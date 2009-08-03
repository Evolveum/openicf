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
package org.identityconnectors.solaris.operation.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;

public class PatternBuilderTest {

    @Test
    public void testSimple() {
        String token = "[^:]*";
        String pattern = String.format("%s:(%s):%s:%s", token, token, token,
                token);
        String result = PatternBuilder.buildPattern(4, 2);
        Assert.assertEquals(pattern, result);
    }

    @Test
    public void testNegative() {
        try {
            int[] arrOfColumns = { 1, 6, 3, 9 };
            PatternBuilder.buildPattern(4, arrOfColumns);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // OK
        }
    }

    @Test
    public void testMulti() {
        basicTest();
    }

    private String basicTest() {
        String token = "[^:]*";
        String pattern = String.format("%s:(%s):%s:(%s):%s", token, token,
                token, token, token);
        String result = PatternBuilder.buildPattern(5, 2, 4);
        Assert.assertEquals(pattern, result);
        return result;
    }

    @Test
    public void testMatch() {
        String result = basicTest();
        String input = "a:b:c:d:e:f";
        String[] b = matches(result, input);
        String msg = String.format("no matching should be here between matcher pattern: '%s' and input: '%s'", result, input);
        Assert.assertNull(msg, b);
        
        input = "a:b:c:d:e";
        b = matches(result, input);
        msg = String.format("matching should be between matcher pattern: '%s' and input: '%s'", result, input);
        Assert.assertNotNull(b);
        Assert.assertTrue(b.length == 2);
        /*
         * this is accorind to {@link PatternBuilderTest#basicTest}, (5, 2, 4) method call.
         */
        Assert.assertTrue(b[0].equals("b")); 
        Assert.assertTrue(b[1].equals("d"));
    }

    /**
     * @param result
     *            the regular expression for matching
     * @param input
     *            the input text to match
     * @return true if the match is correct, false otherwise
     */
    private String[] matches(String result, String input) {
        Pattern p = Pattern.compile(result);

        Matcher matcher = p.matcher(input);
        List<String> res = new ArrayList<String>();
        if(matcher.matches()){
            for (int i = 1; i <= matcher.groupCount(); i++) {
                res.add(matcher.group(i));
            }
            return res.toArray(new String[0]);
        }
        return null;
    }
}
