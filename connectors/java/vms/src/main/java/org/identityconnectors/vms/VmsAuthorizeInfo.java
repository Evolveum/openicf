/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.vms;

import static org.identityconnectors.vms.VmsConstants.ATTR_ACCOUNT;
import static org.identityconnectors.vms.VmsConstants.ATTR_ASTLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_BIOLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_BYTLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_CLI;
import static org.identityconnectors.vms.VmsConstants.ATTR_CLITABLES;
import static org.identityconnectors.vms.VmsConstants.ATTR_CPUTIME;
import static org.identityconnectors.vms.VmsConstants.ATTR_DEFAULT;
import static org.identityconnectors.vms.VmsConstants.ATTR_DEFPRIVILEGES;
import static org.identityconnectors.vms.VmsConstants.ATTR_DIOLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_ENQLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_FILLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_FLAGS;
import static org.identityconnectors.vms.VmsConstants.ATTR_GRANT_IDS;
import static org.identityconnectors.vms.VmsConstants.ATTR_JTQUOTA;
import static org.identityconnectors.vms.VmsConstants.ATTR_LGICMD;
import static org.identityconnectors.vms.VmsConstants.ATTR_LOGIN_FAILS;
import static org.identityconnectors.vms.VmsConstants.ATTR_MAXACCTJOBS;
import static org.identityconnectors.vms.VmsConstants.ATTR_MAXDETACH;
import static org.identityconnectors.vms.VmsConstants.ATTR_MAXJOBS;
import static org.identityconnectors.vms.VmsConstants.ATTR_OWNER;
import static org.identityconnectors.vms.VmsConstants.ATTR_PBYTLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_PGFLQUOTA;
import static org.identityconnectors.vms.VmsConstants.ATTR_PRCLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_PRIMEDAYS;
import static org.identityconnectors.vms.VmsConstants.ATTR_PRIORITY;
import static org.identityconnectors.vms.VmsConstants.ATTR_PRIVILEGES;
import static org.identityconnectors.vms.VmsConstants.ATTR_PWDMINIMUM;
import static org.identityconnectors.vms.VmsConstants.ATTR_QUEPRIO;
import static org.identityconnectors.vms.VmsConstants.ATTR_SHRFILLM;
import static org.identityconnectors.vms.VmsConstants.ATTR_TQELM;
import static org.identityconnectors.vms.VmsConstants.ATTR_UIC;
import static org.identityconnectors.vms.VmsConstants.ATTR_WSDEFAULT;
import static org.identityconnectors.vms.VmsConstants.ATTR_WSEXTENT;
import static org.identityconnectors.vms.VmsConstants.ATTR_WSQUOTA;

import java.util.LinkedList;
import java.util.List;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.patternparser.ClassTransform;
import org.identityconnectors.patternparser.MapTransform.PatternNode;
import org.identityconnectors.patternparser.SplitTransform;
import org.identityconnectors.patternparser.SubstituteTransform;
import org.identityconnectors.patternparser.Transform;

/**
 * This class is used to generate a VMS AUTHORIZE output parser.
 */
public final class VmsAuthorizeInfo {
    private static final String TO_NEXT = "[ \\t]+([^\\n]*?)[ \\t]*";
    private static final String TO_EOL = TO_NEXT + "\\n";
    private static final String PLUS_INDENTED_LINES =
            "\\n[ \\t]{2}?((?:[^\\n]+\\n)(?:[ \\t]{2}[^\\n]+\\n)*)";

    private static final List<PatternNode> PATTERN_NODES = new LinkedList<PatternNode>();
    static {
        try {
            /* @formatter:off */
            PATTERN_NODES.add(new PatternNode(Name.NAME,              "Username:"+TO_NEXT+"(?=Owner)"));
            PATTERN_NODES.add(new PatternNode(ATTR_OWNER,             "Owner:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(ATTR_ACCOUNT,           "Account:"+TO_NEXT+"(?=UIC)"));
            PATTERN_NODES.add(new PatternNode(ATTR_UIC,               "UIC:[ \\t]+(\\[[^]]+\\])[^\\n]*\\n"));
            PATTERN_NODES.add(new PatternNode(ATTR_CLI,               "CLI:"+TO_NEXT+"(?=Tables)"));
            PATTERN_NODES.add(new PatternNode(ATTR_CLITABLES,         "Tables:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(ATTR_DEFAULT,           "Default:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(ATTR_LGICMD,            "LGICMD:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(ATTR_FLAGS,             "Flags:((?:[^\\n]+\\n)+)(?=Primary days)", false, false,
                new Transform[] {
                    new SubstituteTransform("(.*?)\\s+$", "$1"),
                    new SubstituteTransform("^\\s+(.*?)", "$1"),
                    new ToUpperCaseTransform(),
                    new SplitTransform("[ \\t]+")
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_PRIMEDAYS,         "Primary days:"+TO_EOL, false, false,
                new Transform[] {
                    new SplitTransform("[ \\t]+")
            }));
            PATTERN_NODES.add(new PatternNode("Secondary days", "Secondary days:"+TO_EOL, false, false,
                new Transform[] {
                    new SplitTransform("[ \\t]+")
            }));
            PATTERN_NODES.add(new PatternNode("Access Restrictions",   "(No\\saccess\\srestrictions|Primary([^\\n]*\\n){7})"));
            PATTERN_NODES.add(new PatternNode(OperationalAttributes.DISABLE_DATE_NAME,
                                                                 "Expiration:"+TO_NEXT+"(?=Pwdminimum)"));
            PATTERN_NODES.add(new PatternNode(ATTR_PWDMINIMUM,         "Pwdminimum:"+TO_NEXT+"(?=Login Fails)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_LOGIN_FAILS,        "Login Fails:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME,
                                                                 "Pwdlifetime:"+TO_NEXT+"(?=Pwdchange)"));
            PATTERN_NODES.add(new PatternNode(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME,
                                                                 "Pwdchange:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(PredefinedAttributes.LAST_LOGIN_DATE_NAME,
                                                                 "Last Login:"+TO_EOL));
            PATTERN_NODES.add(new PatternNode(ATTR_MAXJOBS,            "Maxjobs:"+TO_NEXT+"(?=Fillm)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_FILLM,              "Fillm:"+TO_NEXT+"(?=Bytlm)", false, false,
                new Transform[] {
                        new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_BYTLM,              "Bytlm:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_MAXACCTJOBS,        "Maxacctjobs:"+TO_NEXT+"(?=Shrfillm)", false, false,
                new Transform[] {
                        new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_SHRFILLM,           "Shrfillm:"+TO_NEXT+"(?=Pbytlm)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
             }));
            PATTERN_NODES.add(new PatternNode(ATTR_PBYTLM,             "Pbytlm:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_MAXDETACH,          "Maxdetach:"+TO_NEXT+"(?=BIOlm)"));
            PATTERN_NODES.add(new PatternNode(ATTR_BIOLM,              "BIOlm:"+TO_NEXT+"(?=JTquota)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_JTQUOTA,            "JTquota:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_PRCLM,              "Prclm:"+TO_NEXT+"(?=DIOlm)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_DIOLM,              "DIOlm:"+TO_NEXT+"(?=WSdef)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_WSDEFAULT,          "WSdef:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_PRIORITY,           "Prio:"+TO_NEXT+"(?=ASTlm)", false, false,
                new Transform[] {
                new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_ASTLM,              "ASTlm:"+TO_NEXT+"(?=WSquo)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_WSQUOTA,            "WSquo:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_QUEPRIO,            "Queprio:"+TO_NEXT+"(?=TQElm)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_TQELM,              "TQElm:"+TO_NEXT+"(?=WSextent)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_WSEXTENT,           "WSextent:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_CPUTIME,            "CPU:"+TO_NEXT+"(?=Enqlm)"));
            PATTERN_NODES.add(new PatternNode(ATTR_ENQLM,              "Enqlm:"+TO_NEXT+"(?=Pgflquo)", false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_PGFLQUOTA,          "Pgflquo:"+TO_EOL, false, false,
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_PRIVILEGES,         "Authorized Privileges:[ \\t]+"+PLUS_INDENTED_LINES, true, false,
                new Transform[] {
                    new SubstituteTransform("IMPERSONAT(\\w\\w+)", "IMPERSONATE $1"),
                    new SubstituteTransform("(.*?)\\nUAF>.*$", "$1"),
                    new SubstituteTransform("(.*?)\\s+$", "$1"),
                    new SplitTransform("[ \\t]+"),
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_DEFPRIVILEGES,      "Default Privileges:[ \\t]+"+PLUS_INDENTED_LINES, true, false,
                new Transform[] {
                    new SubstituteTransform("IMPERSONAT(\\w\\w+)", "IMPERSONATE $1"),
                    new SubstituteTransform("(.*?)\\s+$", "$1"),
                    new SplitTransform("[ \\t]+"),
            }));
            PATTERN_NODES.add(new PatternNode(ATTR_GRANT_IDS,          "Identifier[^\\n]*?"+PLUS_INDENTED_LINES, true, false,
                new Transform[] {
                    new SubstituteTransform("(\\S+)[^\\n]+\\n", "$1"),
                    new SplitTransform("\\s+"),
                    //new ListTransform(new GroupsTransform("[ \\t]*([ \\t]+)[ \\t]+([ \\t]+)[ \\t]*([ \\t]+)?$")),
            }));
            /* @formatter:on */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VmsAuthorizeInfo() {
    }

    public static class ToUpperCaseTransform extends Transform {
        @Override
        public Object transform(Object input) throws Exception {
            return input.toString().toUpperCase();
        }

    }

    public static List<PatternNode> getInfo() {
        return PATTERN_NODES;
    }
}
