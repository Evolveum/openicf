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


import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisConnectorTest {

    private static SolarisConfiguration config;
    
    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // names of properties in the property file (build.groovy)
        final String PROP_HOST = "host";
        final String PROP_SYSTEM_PASSWORD = "pass";
        final String PROP_SYSTEM_USER = "user";
        final String PROP_PORT = "port";
        final String PROP_CONN_TYPE = "connectionType";
        
        // set the credentials
        final String HOST_NAME = TestHelpers.getProperty(PROP_HOST, null);
        final String SYSTEM_PASSWORD = TestHelpers.getProperty(PROP_SYSTEM_PASSWORD, null);
        final String SYSTEM_USER = TestHelpers.getProperty(PROP_SYSTEM_USER, null);
        final String PORT = TestHelpers.getProperty(PROP_PORT, null);
        final String CONN_TYPE = TestHelpers.getProperty(PROP_CONN_TYPE, null);
        
        String msg = "%s must be provided in build.groovy";
        Assert.assertNotNull(String.format(msg, PROP_HOST), HOST_NAME);
        Assert.assertNotNull(String.format(msg, PROP_SYSTEM_PASSWORD), SYSTEM_PASSWORD);
        Assert.assertNotNull(String.format(msg, PROP_SYSTEM_USER), SYSTEM_USER);
        Assert.assertNotNull(String.format(msg, PROP_PORT), PORT);
        Assert.assertNotNull(String.format(msg, PROP_CONN_TYPE), CONN_TYPE);
        
        // save configuration
        config = new SolarisConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setPort(Integer.parseInt(PORT));
        config.setUserName(SYSTEM_USER);
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setConnectionType(CONN_TYPE);
    }

    @After
    public void tearDown() throws Exception {
        config = null;
    }
    
    /* ************ TEST CONNECTOR ************ */
    
    @Test
    public void basicConnectorTests() {
        testGoodConnection();
        basicSchemaTest();
        testGoodConfiguration();
    }
    
    /* ___________ AUTHENTICATE TESTS ___________ */
    @Test
    public void testAuthenticateApiOp() {
        SolarisConnector connector = createConnector(getConfig());
        GuardedString password = getConfig().getPassword();
        String username = getConfig().getUserName();
        connector.authenticate(null, username, password, null);
    }
    
    /**
     * test to authenticate with invalid credentials.
     */
    @Test (expected=ConnectorException.class)
    public void testAuthenticateApiOpInvalidCredentials() {
        SolarisConnector connector = createConnector(getConfig());
        GuardedString password = new GuardedString(
                "WRONG_PASSWORD_FOOBAR2135465".toCharArray());
        String username = getConfig().getUserName();
        connector.authenticate(null, username, password, null);
    }
    
    /* ___________ CREATE TESTS ___________ */
    /**
     * creates a sample user
     */
    @Test
    public void testCreate() {
        SolarisConnector connector = createConnector(getConfig());
        
        Set<Attribute> attrs = initSampleUser();
        connector.create(ObjectClass.ACCOUNT, attrs, null);
    }

    /* ************* TEST CONFIGURATION *********** */
    
    private void testGoodConfiguration() {
        try {
            SolarisConfiguration config = getConfig();
            // no IllegalArgumentException should be thrown
            config.validate();
        } catch (IllegalArgumentException ex) {
            Assert
                    .fail("no IllegalArgumentException should be thrown for valid configuration.\n" + ex.getMessage());
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingUsername() {
        SolarisConfiguration config = getConfig();
        config.setUserName(null);
        config.validate();
        Assert.fail("Configuration allowed a null admin username.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingPassword() {
        SolarisConfiguration config = getConfig();
        config.setPassword(null);
        config.validate();
        Assert.fail("Configuration allowed a null password.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingHostname() {
        SolarisConfiguration config = getConfig();
        config.setHostNameOrIpAddr(null);
        config.validate();
        Assert.fail("Configuration allowed a null hostname.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingPort() {
        SolarisConfiguration config = getConfig();
        config.setPort(null);
        config.validate();
        Assert.fail("Configuration allowed a null port.");
    }
    
    /* ************* AUXILIARY METHODS *********** */
    /**
     * create a new solaris connector and initialize it with the given configuration
     * @param config the configuration to be used.
     */
    private SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        
        return conn;
    }

    /**
     * create configuration based on Unit test account
     * @return
     */
    private SolarisConfiguration getConfig() {
        return config;
    }
    
    /** test connection to the configuration given by default credentials (build.groovy) */
    private void testGoodConnection() {
        SolarisConfiguration config = getConfig();
        SolarisConnector connector = createConnector(config);
        try {
            connector.checkAlive();
        } finally {
            connector.dispose();
        }
    }
    
    /** elementary schema test */
    private void basicSchemaTest() {
        SolarisConnector connector = createConnector(getConfig());
        Schema schema = connector.schema();
        Assert.assertNotNull(schema);
    }
    
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
}
