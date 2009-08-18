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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.*;


/**
 * Attempts to test the Connector with the framework.
 */
public class MySQLUserConnectorDSTests extends MySQLTestBase {
    static final Log log = Log.getLog(MySQLUserConnectorDSTests.class);
    
    /**
     * Derby's embedded ds.
     */
    static final String TEST_DS="testDS";
    
    /**
     * The model user should be created once for all tests, lazy creation check 
     */
    static boolean modelUserCreated = false;

    //jndi for datasource
    static final String[] JNDI_PROPERTIES = new String[]{"java.naming.factory.initial=" + MockContextFactory.class.getName()};    
    
    /**
     * Create the test suite
     * @throws Exception a resource exception
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        idmHost = TestHelpers.getProperty(HOST, null);
        assertNotNull(HOST + MSG, idmHost);

        idmUser = TestHelpers.getProperty(USER, null);
        assertNotNull(USER + MSG, idmUser);

        idmPassword = new GuardedString(TestHelpers.getProperty(PASSWD, "").toCharArray());
        assertNotNull(PASSWD + MSG, idmPassword);

        idmPort = TestHelpers.getProperty(PORT, null);
        assertNotNull(PORT + MSG, idmPort);

        idmDriver = TestHelpers.getProperty(DRIVER, null);
        assertNotNull(DRIVER + MSG, idmDriver);

        idmModelUser = TestHelpers.getProperty(USER_MODEL, null);
        assertNotNull(USER_MODEL + MSG, idmModelUser);

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
        config.setDatasource(TEST_DS);
        config.setJndiProperties(JNDI_PROPERTIES);
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
        
        assertEquals("tstDatasource", TEST_DS, config.getDatasource());
        assertEquals("tstJndiProperties", Arrays.asList(JNDI_PROPERTIES), Arrays.asList(config.getJndiProperties()));
    
    }

    /**
     * Context is set in jndiProperties
     */
    public static class MockContextFactory implements InitialContextFactory {

        @SuppressWarnings("unchecked")
        public Context getInitialContext(Hashtable environment) throws NamingException {
            Context context = (Context) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { Context.class }, new ContextIH());
            return context;
        }
    }
   
    /**
     *  MockContextFactory create the ContextIH
     *  The looup method will return DataSourceIH
     */
    static class ContextIH implements InvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("lookup")) {
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { DataSource.class },
                        new DataSourceIH());
            }
            return null;
        }
    }

    /**
     * ContextIH create DataSourceIH
     * The getConnection method will return ConnectionIH
     */
    static class DataSourceIH implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getConnection")) {
                log.info("DataSource "+TEST_DS+" getConnection");
                return SQLUtil.getDriverMangerConnection(
                        idmDriver, 
                        MySQLUserConfiguration.getUrlString(idmHost, idmPort), 
                        idmUser, 
                        idmPassword);
            }
            throw new IllegalArgumentException("DataSource, invalid method:"+method.getName());            
        }
    }          
}
