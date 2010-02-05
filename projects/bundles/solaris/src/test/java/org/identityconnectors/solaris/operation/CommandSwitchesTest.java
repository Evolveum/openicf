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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.junit.Test;

public class CommandSwitchesTest extends SolarisTestBase {
    private static final String COMMAND_LINE_SWITCH_MARKER = "-";
    private static final String VALUE_MARKER = "V_";

    @Test
    public void testCommandSwitchesFormat() {
        SolarisEntry.Builder bldr = new SolarisEntry.Builder("foo");
        Map<NativeAttribute, String> switches = new HashMap<NativeAttribute, String>();
        
        for (NativeAttribute attr : NativeAttribute.values()) {
            bldr.addAttr(attr, VALUE_MARKER + attr.getName());
            switches.put(attr, COMMAND_LINE_SWITCH_MARKER + attr.getName());
        }
        SolarisEntry entry = bldr.build();

        for (NativeAttribute attr : NativeAttribute.values()) {
            // this is a contract of attribute formatting
            String regexp = null;
            String attrName = attr.getName();
            switch (attr) {
            case LOCK:
            case PWSTAT:
                regexp = COMMAND_LINE_SWITCH_MARKER + attrName + "([\\s][^\"]|[\\s]*$|[\\s]+[" + COMMAND_LINE_SWITCH_MARKER + "])";
                //assert '-ATTR -ATTR' =~ /-ATTR([\s][^"]|[\s]*$|[\s]+[-])/
                break;                
            default:
                regexp = COMMAND_LINE_SWITCH_MARKER + attrName + "[\\s]+\"" + VALUE_MARKER + attrName + "\"";
                //assert '-ATTR "V_ATTR"' =~ /-ATTR[\s]+"V_ATTR"/
                break;
            }
            // see the contract in CommandSwitches#formatCommandSwitches
            String generatedCommandLineSwitches = CommandSwitches.formatCommandSwitches(entry, getConnection(), switches);
            String msg = String.format("Invalid command line formatter: output: <%s> doesn't match regexp: <%s>", generatedCommandLineSwitches, regexp);
            Pattern p = Pattern.compile(regexp);            
            Matcher m = p.matcher(generatedCommandLineSwitches);
            Assert.assertTrue(msg, m.find());
        }
    }

    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
}
