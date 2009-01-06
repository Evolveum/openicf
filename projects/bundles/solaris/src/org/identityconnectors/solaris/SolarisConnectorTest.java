/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisConnectorTest {

    private static String HOST_NAME;
    private static String SYSTEM_PASSWORD;
    private static String SYSTEM_USER;
    private static String HOST_PORT;
    
    @Before
    public void setUp() throws Exception {
        HOST_NAME = TestHelpers.getProperty("host", null);
        SYSTEM_PASSWORD = TestHelpers.getProperty("pass", null);
        SYSTEM_USER = TestHelpers.getProperty("user", null);
        HOST_PORT = TestHelpers.getProperty("port", null);
        
        String msg = "%s must be provided in build.groovy";
        Assert.assertNotNull(String.format(msg, "HOST_NAME"), HOST_NAME);
        Assert.assertNotNull(String.format(msg, "SYSTEM_PASSWORD"), SYSTEM_PASSWORD);
        Assert.assertNotNull(String.format(msg, "SYSTEM_USER"), SYSTEM_USER);
        Assert.assertNotNull(String.format(msg, "HOST_PORT"), HOST_PORT);
    }

    @After
    public void tearDown() throws Exception {
    }
    
    /* ************ TEST CONNECTOR ************ */
    
    @Test
    public void testGoodConnection() {
        SolarisConfiguration config = createConfig();
        SolarisConnector connector = createConnector(config);
        try {
            connector.checkAlive();
        } finally {
            connector.dispose();
        }
    }
    
    /* ************* TEST CONFIGURATION *********** */
    
    @Test
    public void testGoodConfiguration() {
        try {
            SolarisConfiguration config = createConfig();
            // no IllegalArgumentException should be thrown
            config.validate();
        } catch (IllegalArgumentException ex) {
            Assert
                    .fail("no IllegalArgumentException should be thrown for valid configuration.");
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingUsername() {
        SolarisConfiguration config = createConfig();
        config.setUserName(null);
        config.validate();
        Assert.fail("Configuration allowed a null admin username.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testPassword() {
        SolarisConfiguration config = createConfig();
        config.setPassword(null);
        config.validate();
        Assert.fail("Configuration allowed a null password.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingHostname() {
        SolarisConfiguration config = createConfig();
        config.setHostNameOrIpAddr(null);
        config.validate();
        Assert.fail("Configuration allowed a null hostname.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingPort() {
        SolarisConfiguration config = createConfig();
        config.setPort(null);
        config.validate();
        Assert.fail("Configuration allowed a null port.");
    }
    
    /* ************* AUXILIARY METHODS *********** */
    private SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        conn.test();
        
        return conn;
    }

    private SolarisConfiguration createConfig() {
        SolarisConfiguration config = new SolarisConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setPort(HOST_PORT);
        config.setUserName(SYSTEM_USER);
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        
        return config;
    }
}
