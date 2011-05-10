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

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.identityconnectors.solaris.test.SolarisTestBase;

public class CommandSwitchesTest extends SolarisTestBase {
    private static final String COMMAND_LINE_SWITCH_MARKER = "-";
    private static final String VALUE_MARKER = "V_";

    @Test
    public void testCommandSwitchesFormat() {
        SolarisEntry.Builder bldr = new SolarisEntry.Builder("foo");
        Map<NativeAttribute, String> switches = new HashMap<NativeAttribute, String>();
        
        for (NativeAttribute attr : NativeAttribute.values()) {
            Object attrValue = VALUE_MARKER + attr.getName();
            // PWSTAT is special attribute, that accepts true only values, so we need to workaround this.
            if (attr.equals(NativeAttribute.PWSTAT)) {
                attrValue = true;
            }
            bldr.addAttr(attr, attrValue);
            switches.put(attr, COMMAND_LINE_SWITCH_MARKER + attr.getName());
        }
        SolarisEntry entry = bldr.build();

        // see the contract in CommandSwitches#formatCommandSwitches
        String generatedCommandLineSwitches = CommandSwitches.formatCommandSwitches(entry, getConnection(), switches);
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
            
            String msg = String.format("Invalid command line formatter: output: <%s> doesn't match regexp: <%s>", generatedCommandLineSwitches, regexp);
            Pattern p = Pattern.compile(regexp);            
            Matcher m = p.matcher(generatedCommandLineSwitches);
            AssertJUnit.assertTrue(msg, m.find());
        }
    }
    
    @Test
    public void testFormatMultiValueSwitches() {
        List<String> values = CollectionUtil.newList("foo", "bar", "baz");
        final String expectedMultivalueArgument = "\"foo,bar,baz\""; 
        
        List<NativeAttribute> multivalueAttributes = CollectionUtil.<NativeAttribute>newList();
        for (NativeAttribute it : NativeAttribute.values()) {
            if (!it.isSingleValue()) {
                multivalueAttributes.add(it);
            }
        }
        
        SolarisEntry.Builder bldr = new SolarisEntry.Builder("foo");
        Map<NativeAttribute, String> switches = new HashMap<NativeAttribute, String>();
        for (NativeAttribute it : multivalueAttributes) {
            bldr.addAttr(it, values);
            switches.put(it, COMMAND_LINE_SWITCH_MARKER + it.getName());
        }
        
        String generatedCommandLineSwitches = CommandSwitches.formatCommandSwitches(bldr.build(), getConnection(), switches);
        String[] switchElements = generatedCommandLineSwitches.split(COMMAND_LINE_SWITCH_MARKER);
        for (NativeAttribute attr : multivalueAttributes) {
            String foundCluster = searchCluster(attr, switchElements);
            AssertJUnit.assertNotNull("attribute '" + attr.getName() + "' is missing", foundCluster);
            AssertJUnit.assertTrue(foundCluster.contains(expectedMultivalueArgument));
        }
    }

    private String searchCluster(NativeAttribute attr, String[] switchElements) {
        for (String it : switchElements) {
            if (it.contains(attr.getName())) {
                return it;
            }
        }
        return null;
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
