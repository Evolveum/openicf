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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.mysqluser;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Attempts to test the Connector with the framework.
 */
@Test(groups = { "integration" })
public class MySQLUserConnectorTests extends MySQLTestBase {

    static boolean modelUserCreated = false;

    /**
     * Create the test suite
     *
     * @throws Exception
     *             a resource exception
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        PropertyBag testProps = TestHelpers.getProperties(MySQLUserConnector.class);

        idmHost = testProps.getStringProperty(HOST);
        AssertJUnit.assertNotNull(HOST + MSG, idmHost);

        idmUser = testProps.getStringProperty(USER);
        AssertJUnit.assertNotNull(USER + MSG, idmUser);

        idmPassword =
                new GuardedString(testProps.getProperty(PASSWD, String.class, "").toCharArray());
        AssertJUnit.assertNotNull(PASSWD + MSG, idmPassword);

        idmPort = testProps.getStringProperty(PORT);
        AssertJUnit.assertNotNull(PORT + MSG, idmPort);

        idmDriver = testProps.getStringProperty(DRIVER);
        AssertJUnit.assertNotNull(DRIVER + MSG, idmDriver);

        idmModelUser = testProps.getStringProperty(USER_MODEL);
        AssertJUnit.assertNotNull(USER_MODEL + MSG, idmModelUser);

        final String passwd = testProps.getStringProperty(TEST_PASSWD);
        AssertJUnit.assertNotNull(TEST_PASSWD + MSG, passwd);
        testPassword = new GuardedString(passwd.toCharArray());
    }

    /**
     * Clean up the test suite
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        ConnectorFacadeFactory.getInstance().dispose();
    }

    /**
     * Setup the test
     *
     * @throws Exception
     */
    @BeforeMethod
    public void setup() throws Exception {
        // attempt to create the database in the directory..
        config = newConfiguration();
        facade = getFacade();
        // quitellyDeleteUser(idmModelUser);
        // Create model test user
        if (!modelUserCreated) {
            createTestModelUser(idmModelUser, testPassword);
            modelUserCreated = true;
        }
    }

    /**
     * @throws Exception
     */
    @AfterMethod
    public void teardown() throws Exception {
        config = null;
    }

    /*
     * (non-Javadoc)
     *
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
     *
     * @throws Exception
     */
    @Test()
    public void testConfiguration() throws Exception {

        AssertJUnit.assertNotNull("tstDriver", config.getDriver());
        AssertJUnit.assertNotNull("tstHost", config.getHost());
        AssertJUnit.assertNotNull("tstLogin", config.getUser());
        AssertJUnit.assertNotNull("tstPassword", config.getPassword());
        AssertJUnit.assertNotNull("tstPort", config.getPort());
        AssertJUnit.assertNotNull("usermodel", config.getUsermodel());

        AssertJUnit.assertEquals("tstDriver", idmDriver, config.getDriver());
        AssertJUnit.assertEquals("tstHost", idmHost, config.getHost());
        AssertJUnit.assertEquals("tstLogin", idmUser, config.getUser());
        AssertJUnit.assertEquals("tstPassword", idmPassword, config.getPassword());
        AssertJUnit.assertEquals("tstPort", idmPort, config.getPort());
        AssertJUnit.assertEquals("usermodel", idmModelUser, config.getUsermodel());

    }

    /**
     * Test method.
     *
     * @throws Exception
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidConfigurationHost() throws Exception {
        config.setHost("");
        config.validate();
    }

    /**
     * Test method.
     *
     * @throws Exception
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidConfigurationDriver() throws Exception {
        config.setDriver("");
        config.validate();
    }

    /**
     * Test method.
     *
     * @throws Exception
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidConfigurationLogin() throws Exception {
        config.setUser("");
        config.validate();
    }

    /**
     * Test method.
     *
     * @throws Exception
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidConfigurationPort() throws Exception {
        config.setPort("");
        config.validate();
    }
}
