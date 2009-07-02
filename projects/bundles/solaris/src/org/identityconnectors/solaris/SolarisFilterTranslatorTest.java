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
package org.identityconnectors.solaris;

import junit.framework.Assert;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisFilterTranslatorTest {
 
    private SolarisFilterTranslator sft;

    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        sft = new SolarisFilterTranslator();
    }

    @After
    public void tearDown() throws Exception {
        sft = null;
    }
    
    /** AND, OR op. tests */
    @Test
    public void testBasicTranslator() {
        String exp = sft.createAndExpression("abc", "def");
        Assert.assertEquals(exp, "abc&def");
        
        exp = sft.createOrExpression("(abc)", "(def)");
        matches("abc", exp);
    }
    
    /** @param string the string for matching
     *  @ param exp regular expression to match */
    private void matches(String string, String exp) {
        Assertions.nullCheck(string, "string");
        Assertions.nullCheck(exp, "exp");
        String msg = String.format("String '%s' does not match regexp '%s'.", string, exp);
        Assert.assertTrue(msg, string.matches(exp));
    }

    /** the rest of the ops. */
    @Test
    public void testAdvTranslator() {
        Attribute attr = AttributeBuilder.build(Name.NAME, "sarahsmith");
        String exp = sft.createStartsWithExpression(new StartsWithFilter(attr), false);
        matches("sarahsmithBooBarFoo", exp);
        
        attr = AttributeBuilder.build(Name.NAME, "sarahsmith");
        exp = sft.createEndsWithExpression(new EndsWithFilter(attr), false);
        matches("BooBarFoosarahsmith", exp);
        
        attr = AttributeBuilder.build(Name.NAME, "sarahsmith");
        exp = sft.createContainsExpression(new ContainsFilter(attr), false);
        matches("BooBarsarahsmithBazFoo", exp);
    }
    
    @Test
    public void combinedTranslate() {
        String[] leftRighExpression = prepareExpressions();
        
        String exp = sft.createAndExpression(leftRighExpression[0], leftRighExpression[1]);
        String expected = "sarahsmithFooBarlilianBazBoo";
        Assert.assertTrue(String.format("String '%s' doesn't align with regexp: '%s'", expected, exp), evaluateAndExpression(expected, exp));
        
        expected = "sarahsmithFooBarBazBoo";
        Assert.assertFalse(evaluateAndExpression(expected, exp));
        
        expected = "sarahFooBarlilianBazBoo";
        Assert.assertFalse(evaluateAndExpression(expected, exp));
    }

    private boolean evaluateAndExpression(String string, String exp) {
        String[] regExpParts = exp.split("&");
        boolean res = true;
        for (String regExp : regExpParts) {
            res &= string.matches(regExp);
            if (!res) 
                break;
        }
        return res;
    }

    @Test
    public void combinedTranslate2() {
        String[] leftRighExpression = prepareExpressions();
        
        String exp = sft.createOrExpression(leftRighExpression[0], leftRighExpression[1]);
        matches("sarahsmithBooBar", exp);
        matches("BazBoosarahsmithBoolilianBar", exp);
    }
    
    /**
     * create two regular expressions:
     * <ul>
     * <li>sarahsmith* (starts with)</li>
     * <li>*lilian* (contains)</li>
     * </ul>
     * @return
     */
    private String[] prepareExpressions() {
        Attribute attr = AttributeBuilder.build(Name.NAME, "sarahsmith");
        String leftExpression = sft.createStartsWithExpression(new StartsWithFilter(attr), false);
        matches("sarahsmithFooBar", leftExpression);
        
        attr = AttributeBuilder.build(Name.NAME, "lilian");
        String rightExpression = sft.createContainsExpression(new ContainsFilter(attr), false);
        matches("BazBoolilianFooBar", rightExpression);
        
        String[] arr = {leftExpression, rightExpression};
        return arr;
    }
}
