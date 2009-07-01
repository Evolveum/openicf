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
        Assert.assertTrue("abc & def".equals(exp));
        
        exp = sft.createOrExpression("abc", "def");
        Assert.assertTrue("abc | def".equals(exp));
    }
    
    /** the rest of the ops. */
    @Test
    public void testAdvTranslator() {
        Attribute attr = AttributeBuilder.build(Name.NAME, "johnsmith");
        String exp = sft.createStartsWithExpression(new StartsWithFilter(attr), false);
        Assert.assertTrue("johnsmith*".equals(exp));
        
        attr = AttributeBuilder.build(Name.NAME, "johnsmith");
        exp = sft.createEndsWithExpression(new EndsWithFilter(attr), false);
        Assert.assertTrue("*johnsmith".equals(exp));
        
        attr = AttributeBuilder.build(Name.NAME, "johnsmith");
        exp = sft.createContainsExpression(new ContainsFilter(attr), false);
        Assert.assertTrue("*johnsmith*".equals(exp));
    }
}
