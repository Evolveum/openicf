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
package org.identityconnectors.mysqluser;

import static org.junit.Assert.*;


import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseConnection;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Attempts to test the Connector with the framework.
 */
public class MySQLUserConnectorTests extends MySQLTestBase {
    /**
     * Setup logging for the {@link DatabaseConnection}.
     */
    static final Log log = Log.getLog(DatabaseConnection.class);
    static boolean modelUserCreated = false;
   
    /**
     * Create the test suite
     * @throws Exception a resource exception
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        final String MSG = " must be configured for running unit test";
        final String HOST = "connector.host";
        idmHost = TestHelpers.getProperty(HOST, null);
        assertNotNull(HOST + MSG, idmHost);

        final String USER = "connector.user";
        idmUser = TestHelpers.getProperty(USER, null);
        assertNotNull(USER + MSG, idmUser);

        final String PASSWD = "connector.password";
        idmPassword = new GuardedString(TestHelpers.getProperty(PASSWD, null).toCharArray());
        assertNotNull(PASSWD + MSG, idmPassword);

        final String PORT = "connector.port";
        idmPort = TestHelpers.getProperty(PORT, null);
        assertNotNull(PORT + MSG, idmPort);

        final String DRIVER = "connector.driver";
        idmDriver = TestHelpers.getProperty(DRIVER, null);
        assertNotNull(DRIVER + MSG, idmDriver);

        final String USER_MODEL = "connector.usermodel";
        idmModelUser = TestHelpers.getProperty(USER_MODEL, null);
        assertNotNull(USER_MODEL + MSG, idmModelUser);

        final String TEST_PASSWD = "connector.testpassword";
        final String passwd = TestHelpers.getProperty(TEST_PASSWD, null);
        assertNotNull(TEST_PASSWD + MSG, passwd);
        testPassword = new GuardedString(passwd.toCharArray());       
    }

    /**
     * Clean up the test suite
     * @throws Exception
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        ConnectorFacadeFactory.getInstance().dispose();
    }

    /**
     * Setup  the test
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        // attempt to create the database in the directory..
        config = newConfiguration();
        facade = getFacade();
        //quitellyDeleteUser(idmModelUser);
        //Create model test user
        if ( !modelUserCreated ) {
            createTestModelUser(idmModelUser, testPassword);
            modelUserCreated = true;
        }
    }

    /**
     * @throws Exception
     */
    @After
    public void teardown() throws Exception {     
        config = null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.mysqluser.MySQLTestBase#newConfiguration()
     */
    @Override
    public MySQLUserConfiguration newConfiguration() {
        MySQLUserConfiguration config = new MySQLUserConfiguration();
        config.setDriver(idmDriver);
        config.setHost(idmHost);
        config.setUser(idmUser);
        config.setPassword(idmPassword);
        config.setPort(idmPort);
        config.setUsermodel(idmModelUser);
        config.setConnectorMessages(TestHelpers.createDummyMessages());
        return config;
    }    
    
    /**
     * Test the configuration
     * @throws Exception
     */
    @Test()
    public void testConfiguration() throws Exception {
    
        assertNotNull("tstDriver", config.getDriver());
        assertNotNull("tstHost", config.getHost());
        assertNotNull("tstLogin", config.getUser());
        assertNotNull("tstPassword", config.getPassword());
        assertNotNull("tstPort", config.getPort());
        assertNotNull("usermodel", config.getUsermodel());
    
        assertEquals("tstDriver", idmDriver, config.getDriver());
        assertEquals("tstHost", idmHost, config.getHost());
        assertEquals("tstLogin", idmUser, config.getUser());
        assertEquals("tstPassword", idmPassword, config.getPassword());
        assertEquals("tstPort", idmPort, config.getPort());
        assertEquals("usermodel", idmModelUser, config.getUsermodel());
    
    }
    

    /**
     * Test method 
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationHost() throws Exception {
        config.setHost("");
        config.validate();
    }    
    

    /**
     * Test method 
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationDriver() throws Exception {
        config.setDriver("");
        config.validate();
    }
    

    /**
     * Test method 
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationLogin() throws Exception {
        config.setUser("");
        config.validate();
    }
    

    /**
     * Test method 
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationPort() throws Exception {
        config.setPort("");
        config.validate();
    }
}
