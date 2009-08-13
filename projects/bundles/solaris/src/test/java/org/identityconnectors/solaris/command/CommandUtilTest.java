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
package org.identityconnectors.solaris.command;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.solaris.constants.AccountAttributes;
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
        Set<Attribute> replaceAttributes = new HashSet<Attribute>();
        final String attrValue = "dummy";
        replaceAttributes.add(AttributeBuilder.build(AccountAttributes.UID.getName(), attrValue));
        final String cmd = CommandUtil.prepareCommand(replaceAttributes, ObjectClass.ACCOUNT);
        final String expected = String.format("%s \"%s\"", AccountAttributes.UID.getCmdSwitch(), attrValue);
        Assert.assertTrue(cmd.equals(expected));
    }
}
