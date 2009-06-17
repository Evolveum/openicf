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

import junit.framework.Assert;

import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SolarisConfigurationTest {
    
    private static SolarisConfiguration config;
    
    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
        config.setConnectorMessages(TestHelpers.createDummyMessages());
        
        SolarisTestCommon.printIPAddress(config);
    }

    @After
    public void tearDown() throws Exception {
        config = null;
    }
    
    @Test
    public void testGoodConfiguration() {
        SolarisConfiguration config = getConfig();
        config.validate();
        // no IllegalArgumentException should be thrown for valid configuration
    }
    
    /* **************** "MISSING" PROPERTY TESTS ***************** */
    @Test(expected = ConfigurationException.class)
    public void testMissingUsername() {
        SolarisConfiguration config = getConfig();
        config.setUserName(null);
        config.validate();
        Assert.fail("Configuration allowed a null admin username.");
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingPassword() {
        SolarisConfiguration config = getConfig();
        config.setPassword(null);
        config.validate();
        Assert.fail("Configuration allowed a null password.");
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingHostname() {
        SolarisConfiguration config = getConfig();
        config.setHostNameOrIpAddr(null);
        config.validate();
        Assert.fail("Configuration allowed a null hostname.");
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingPort() {
        SolarisConfiguration config = getConfig();
        config.setPort(null);
        config.validate();
        Assert.fail("Configuration allowed a null port.");
    }
    
    @Test @Ignore
    public void testGetMessageProperty() {
        String result = getConfig().getMessage("SOLARIS");
        Assert.assertTrue(result.equals("Solaris"));
    }

    /* ************* AUXILIARY METHODS *********** */

    /**
     * create configuration based on Unit test account
     * @return
     */
    private SolarisConfiguration getConfig() {
        return config;
    }
}
