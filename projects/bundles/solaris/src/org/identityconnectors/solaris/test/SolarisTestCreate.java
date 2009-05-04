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
package org.identityconnectors.solaris.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisTestCreate {
    
    private SolarisConfiguration config;

    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        config = null;
    }
    
    /**
     * creates a sample user
     */
    @Test
    public void testCreate() {
        SolarisConnector connector = createConnector(config);
        
        Set<Attribute> attrs = initSampleUser();
        
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        Assert.assertNotNull(uid);
    }
    
    /* ************* AUXILIARY METHODS *********** */
    
    /** fill in sample user/password for sample user used in create */
    private Set<Attribute> initSampleUser() {
        String msg = "test property '%s' should not be null";
        
        Set<Attribute> res = new HashSet<Attribute>();
        
        String sampleUser = TestHelpers.getProperty("sampleUser", null);
        Assert.assertNotNull(String.format(msg, "sampleUser"), sampleUser);
        res.add(AttributeBuilder.build(Name.NAME, sampleUser));
        
        String samplePasswd = TestHelpers.getProperty("samplePasswd", null);
        Assert.assertNotNull(String.format(msg, "samplePasswd"), samplePasswd);
        res.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString(samplePasswd.toCharArray())));
        
        return res;
    }
    
    /**
     * create a new solaris connector and initialize it with the given configuration
     * @param config the configuration to be used.
     */
    private SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        
        return conn;
    }
}
