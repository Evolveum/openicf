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
    
//    @Test
//    public void testBasicTranslator() {
//        SolarisFilter expFlt = sft.createOrExpression(createFilter("(abc)"), createFilter("(def)"));
//        String exp = expFlt.getRegExp();
//        matches("abc", exp);
//        matches("def", exp);
//        matches("xef", exp, false);
//        matches("abef", exp, false);
//    }
//    
//    private SolarisFilter createFilter(String string) {
//        return new SolarisFilter(Name.NAME, string);
//    }
//
//    /** @param string the string for matching
//     *  @ param exp regular expression to match */
//    private void matches(String string, String exp) {
//        matches(string, exp, true);
//    }
//    
//    private void matches(String string, String exp, boolean expectedMatch) {
//        Assertions.nullCheck(string, "string");
//        Assertions.nullCheck(exp, "exp");
//        String msg = String.format(((expectedMatch) ? "String '%s' does not match regexp '%s'."
//                                : "String '%s' matched regexp '%s', but it should *not*."), string, exp);
//        if (expectedMatch) {
//            Assert.assertTrue(msg, string.matches(exp));
//        } else {
//            Assert.assertFalse(msg, string.matches(exp));
//        }
//        
//    }
//
//    /** the rest of the ops. */
//    @Test
//    public void testAdvTranslator() {
//        Attribute attr = AttributeBuilder.build(Name.NAME, "sarahsmith");
//        SolarisFilter expFlt = sft.createStartsWithExpression(new StartsWithFilter(attr), false);
//        String exp = expFlt.getRegExp();
//        matches("sarahsmithBooBarFoo", exp);
//        matches("abcsarahsmith", exp, false);
//        
//        expFlt = sft.createEndsWithExpression(new EndsWithFilter(attr), false);
//        exp = expFlt.getRegExp();
//        matches("BooBarFoosarahsmith", exp);
//        matches("sarahsmithx", exp, false);
//        
//        expFlt = sft.createContainsExpression(new ContainsFilter(attr), false);
//        exp = expFlt.getRegExp();
//        matches("BooBarsarahsmithBazFoo", exp);
//        matches("sarahxsmith", exp, false);
//    }
}
