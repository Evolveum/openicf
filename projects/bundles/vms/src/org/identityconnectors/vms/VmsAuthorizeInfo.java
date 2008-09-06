/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.vms;

import java.util.LinkedList;
import java.util.List;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.patternparser.ClassTransform;
import org.identityconnectors.patternparser.GroupsTransform;
import org.identityconnectors.patternparser.ListTransform;
import org.identityconnectors.patternparser.SplitTransform;
import org.identityconnectors.patternparser.SubstituteTransform;
import org.identityconnectors.patternparser.Transform;
import org.identityconnectors.patternparser.MapTransform.PatternNode;



/**
 * This class is used to generate a VMS AUTHORIZE output parser.
 */
public class VmsAuthorizeInfo {
    private static final String TO_NEXT                	    = "[ \\t]+([^\\n]*?)[ \\t]*";
    private static final String TO_EOL                      = TO_NEXT+"\\n";    
    private static final String PLUS_INDENTED_LINES         = "\\n[ \\t]{2}?((?:[^\\n]+\\n)(?:[ \\t]{2}[^\\n]+\\n)*)";

    private static final List<PatternNode> _parser = new LinkedList<PatternNode>();
    static {
        try {
            _parser.add(new PatternNode(Name.NAME,           "Username:"+TO_NEXT+"(?=Owner)"));
            _parser.add(new PatternNode("OWNER",             "Owner:"+TO_EOL));        
            _parser.add(new PatternNode("ACCOUNT",           "Account:"+TO_NEXT+"(?=UIC)"));
            _parser.add(new PatternNode("UIC",               "UIC:[ \\t]+(\\[[^]]+\\])[^\\n]*\\n"));        
            _parser.add(new PatternNode("CLI",               "CLI:"+TO_NEXT+"(?=Tables)"));
            _parser.add(new PatternNode("CLITABLES",         "Tables:"+TO_EOL));        
            _parser.add(new PatternNode("default",           "Default:"+TO_EOL));        
            _parser.add(new PatternNode("LGICMD",            "LGICMD:"+TO_EOL));        
            _parser.add(new PatternNode("FLAGS",             "Flags:"+TO_EOL+"(?=Primary days)", false, false, 
                new Transform[] {
                    new ToUpperCaseTransform(),
                    new SplitTransform("[ \\t]+")
            }));
            _parser.add(new PatternNode("PRIMEDAYS",         "Primary days:"+TO_EOL, false, false, 
                new Transform[] {
                    new SplitTransform("[ \\t]+")
            }));
            _parser.add(new PatternNode("Secondary days", "Secondary days:"+TO_EOL, false, false, 
                new Transform[] {
                    new SplitTransform("[ \\t]+")
            }));
            _parser.add(new PatternNode("EXPIRATION",         "Expiration:"+TO_NEXT+"(?=Pwdminimum)"));
            _parser.add(new PatternNode("PWDMINIMUM",         "Pwdminimum:"+TO_NEXT+"(?=Login Fails)"));
            _parser.add(new PatternNode("loginfails",         "Login Fails:"+TO_EOL));
            _parser.add(new PatternNode("PWDLIFETIME",        "Pwdlifetime:"+TO_NEXT+"(?=Pwdchange)"));
            _parser.add(new PatternNode("PWDCHANGE",          "Pwdchange:"+TO_EOL));        
            _parser.add(new PatternNode("lastlogin",          "Last Login:"+TO_EOL));        
            _parser.add(new PatternNode("MAXJOBS",            "Maxjobs:"+TO_NEXT+"(?=Fillm)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("FILLM",              "Fillm:"+TO_NEXT+"(?=Bytlm)", false, false, 
                new Transform[] {
                        new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("BYTLM",              "Bytlm:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("MAXACCTJOBS",        "Maxacctjobs:"+TO_NEXT+"(?=Shrfillm)", false, false, 
                new Transform[] {
                        new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("SHRFILLM",           "Shrfillm:"+TO_NEXT+"(?=Pbytlm)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
             }));
            _parser.add(new PatternNode("PBYTLM",             "Pbytlm:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("MAXDETACH",          "Maxdetach:"+TO_NEXT+"(?=BIOlm)"));
            _parser.add(new PatternNode("BIOLM",              "BIOlm:"+TO_NEXT+"(?=JTquota)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("JTQUOTA",            "JTquota:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("PRCLM",              "Prclm:"+TO_NEXT+"(?=DIOlm)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("DIOLM",              "DIOlm:"+TO_NEXT+"(?=WSdef)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("WSDEFAULT",          "WSdef:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("PRIORITY",           "Prio:"+TO_NEXT+"(?=ASTlm)", false, false, 
                new Transform[] {
                new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("ASTLM",              "ASTlm:"+TO_NEXT+"(?=WSquo)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("WSQUOTA",            "WSquo:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("QUEPRIO",            "Queprio:"+TO_NEXT+"(?=TQElm)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("TQELM",              "TQElm:"+TO_NEXT+"(?=WSextent)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("WSEXTENT",           "WSextent:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("CPUTIME",            "CPU:"+TO_NEXT+"(?=Enqlm)"));
            _parser.add(new PatternNode("ENQLM",              "Enqlm:"+TO_NEXT+"(?=Pgflquo)", false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("PGFLQUOTA",          "Pgflquo:"+TO_EOL, false, false, 
                new Transform[] {
                    new ClassTransform(Integer.class)
            }));
            _parser.add(new PatternNode("PRIVILEGES",         "Authorized Privileges:[ \\t]+"+PLUS_INDENTED_LINES, true, false, 
                new Transform[] {
                    new SubstituteTransform("IMPERSONAT(\\w\\w+)", "IMPERSONATE $1"),
                    new SplitTransform("[ \\t]+"),
            }));
            _parser.add(new PatternNode("DEFPRIVILEGES",      "Default Privileges:[ \\t]+"+PLUS_INDENTED_LINES, true, false, 
                new Transform[] {
                    new SubstituteTransform("IMPERSONAT(\\w\\w+)", "IMPERSONATE $1"),
                    new SplitTransform("[ \\t]+"),
            }));
            _parser.add(new PatternNode("Identifier",         "Identifier[^\\n]*?"+PLUS_INDENTED_LINES, true, false, 
                new Transform[] {
                    new SplitTransform("\\n"),
                    new ListTransform(new GroupsTransform("[ \\t]*([ \\t]+)[ \\t]+([ \\t]+)[ \\t]*([ \\t]+)?$")),
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ToUpperCaseTransform extends Transform {
        @Override
        public Object transform(Object input) throws Exception {
            return input.toString().toUpperCase();
        }
        
    }
    public static List<PatternNode> getInfo() {
        return _parser;
    }
}
