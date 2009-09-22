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

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.CommandUtil;
import org.junit.Test;

public class CommandUtilTest {
    @Test
    public void test() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 350; i++) {
            input.append("a");
        }
        String resultStr = CommandUtil.limitString(input);
        Assert.assertTrue(resultStr.contains("\\"));
        String[] strs = resultStr.split("\n");
        for (String string : strs) {
            
            final int limit = CommandUtil.DEFAULT_LIMIT + 1;
            final int trimmedStringLength = string.trim().length();
            
            String msg = String.format("String exceeds the maximal limit '%s', as it is: '%s'", limit , trimmedStringLength);
            Assert.assertTrue(msg, trimmedStringLength <= limit);
        }
    }
    
    @Test
    public void testPrepareCommand() {
        final String attrValue = "dummy";
        final NativeAttribute attrName = NativeAttribute.DIR;
        Set<NativePair> replaceAttributes = buildPair(attrName, attrValue);
        String cmd = CommandUtil.prepareCommand(replaceAttributes);
        String expected = String.format("%s \"%s\"", attrName.getCmdSwitch(), attrValue);
        Assert.assertEquals(expected, cmd.trim());
        
        // try multiple commands
        final String attrValue2 = "foobar";
        final NativeAttribute attrName2 = NativeAttribute.GROUP_PRIM;
        replaceAttributes = buildPair(attrName, attrValue, attrName2, attrValue2);
        expected = String.format("%s \"%s\" %s \"%s\"", attrName.getCmdSwitch(), attrValue, attrName2.getCmdSwitch(), attrValue2);
        cmd = CommandUtil.prepareCommand(replaceAttributes);
        Assert.assertTrue(cmd.split(" ").length == 4);
        
        
        String[] tokens = cmd.split(" ");
        controlCmdParam(attrValue, tokens, attrName.getCmdSwitch());
        controlCmdParam(attrValue2, tokens, attrName2.getCmdSwitch());
    }

    private void controlCmdParam(final String attrValue, String[] tokens,
            String cmdSwitchParam) {
        boolean found = false;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(cmdSwitchParam)) {
                Assert.assertTrue("argument missing: " + attrValue, i + 1 < tokens.length);
                Assert.assertEquals(quote(attrValue), tokens[i + 1]);
                found = true;
                break;
            }
        }
        Assert.assertTrue("command switch missing: " + cmdSwitchParam, found);
    }

    private Object quote(String string) {
        return String.format("\"%s\"", string);
    }
    
    private static Set<NativePair> buildPair(NativeAttribute attr, String value) {
        Set<NativePair> set = new HashSet<NativePair>();
        set.add(new NativePair(attr, value));
        return set;
    }
    
    private static Set<NativePair> buildPair(NativeAttribute attr, String value, NativeAttribute attr2, String value2) {
        Set<NativePair> set = buildPair(attr, value);
        Set<NativePair> set2 = buildPair(attr2, value2);
        set.addAll(set2);
        return set;
    }
}
