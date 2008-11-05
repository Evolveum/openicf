/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.mysqluser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseConnection;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Attempts to test the Connector with the framework.
 */
public class MySQLUserConnectorTests {
    /**
     * Setup logging for the {@link DatabaseConnection}.
     */
    private static final Log log = Log.getLog(DatabaseConnection.class);
   
    private static String idmDriver = null;
    private static String idmHost = null;

    private static String idmLogin = null;
    private static GuardedString testPassword = null;
    private static String idmModelUser = null;
    private static GuardedString idmPassword = null;
    private static String idmPort = null;
    private static final String TST_HOST = "%";
    private static final String TST_USER1 = "test1";

    private static final String TST_USER2 = "test2";
    private static final String TST_USER3 = "test3";

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

        final String LOGIN = "connector.login";
        idmLogin = TestHelpers.getProperty(LOGIN, null);
        assertNotNull(LOGIN + MSG, idmLogin);

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
     * Clen up the test suite
     * @throws Exception
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        ConnectorFacadeFactory.getInstance().dispose();
    }

    /**
     * Get the configuration
     * @return the initialized configuration
     */
    private static MySQLUserConfiguration newConfiguration() {
        MySQLUserConfiguration config = new MySQLUserConfiguration();
        config.setDriver(idmDriver);
        config.setHost(idmHost);
        config.setLogin(idmLogin);
        config.setPassword(idmPassword);
        config.setPort(idmPort);
        config.setUsermodel(idmModelUser);
        return config;
    }
    
    // Test config
    private MySQLUserConfiguration config = null;

    // Test facade
    private ConnectorFacade facade = null;

    /**
     * Setup  the test
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        // attempt to create the database in the directory..
        config = newConfiguration();
        facade = getFacade();
    }

    /**
     * @throws Exception
     */
    @After
    public void teardown() throws Exception {     
        config = null;
    }

    /**
     * Test the configuration
     * @throws Exception
     */
    @Test()
    public void testConfiguration() throws Exception {

        assertNotNull("tstDriver", config.getDriver());
        assertNotNull("tstHost", config.getHost());
        assertNotNull("tstLogin", config.getLogin());
        assertNotNull("tstPassword", config.getPassword());
        assertNotNull("tstPort", config.getPort());
        assertNotNull("usermodel", config.getUsermodel());

        assertEquals("tstDriver", idmDriver, config.getDriver());
        assertEquals("tstHost", idmHost, config.getHost());
        assertEquals("tstLogin", idmLogin, config.getLogin());
        assertEquals("tstPassword", idmPassword, config.getPassword());
        assertEquals("tstPort", idmPort, config.getPort());
        assertEquals("usermodel", idmModelUser, config.getUsermodel());

    }

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testCreate() {
        assertNotNull(facade);
        String userName=TST_USER1;
        quitellyDeleteUser(userName);
        final Uid uid = createUser(userName, testPassword, TST_HOST);
        assertNotNull(uid);
        assertEquals(userName, uid.getUidValue());
        //Delete it at the end
        quitellyDeleteUser(userName);
    }
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testCreateUnsupported() {
        assertNotNull(facade);
        String userName=TST_USER1;
        quitellyDeleteUser(userName);
        
        Set<Attribute> tuas = getUserAttributeSet(TST_USER1, testPassword , TST_USER1);
        assertNotNull(tuas);
        ObjectClass oc = new ObjectClass("UNSUPPORTED");
        facade.create(oc, tuas, null);   
    }    
    
 
    
    /**
     * Test method for {@link MySQLUserConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test
    public void testCreateAndUpdate() {
        String userName = TST_USER1;
        String newName = TST_USER3;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName);    
        quitellyDeleteUser(newName);    
        
        // retrieve the object      
        Uid uid = new Uid(testUserFound(userName, true)); 
        
        // create updated connector object
        ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
        coBuilder.setName(newName); //Going to change name -> id change follows
        coBuilder.setUid(uid);
        coBuilder.setObjectClass(ObjectClass.ACCOUNT);
        ConnectorObject coBeforeUpdate = coBuilder.build();
        
        // do the update
        Set<Attribute> changeSet = CollectionUtil.newSet(coBeforeUpdate.getAttributes());
        final Uid uidUpdate = facade.update(UpdateApiOp.Type.REPLACE, ObjectClass.ACCOUNT, changeSet, null);
        
        // uids should be the same
        assertEquals(newName, uidUpdate.getUidValue());
        
        // retrieve the updated object
        // retrieve the object      
        String actual = testUserFound(newName, true); 
        assertEquals(coBeforeUpdate.getName().getNameValue(), actual);
        
        quitellyDeleteUser(TST_USER1); 
        quitellyDeleteUser(TST_USER3);   
    }

    
    /**
     * Test method for {@link MySQLUserConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test(expected=IllegalStateException.class)
    public void testUpdateUnsupported() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName);    
        // retrieve the object      
        Uid uid = new Uid(testUserFound(userName, true)); 
        // create updated connector object
        ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
        ObjectClass oc = new ObjectClass("UNSUPPORTED");
        coBuilder.setUid(uid);
        coBuilder.setObjectClass(ObjectClass.ACCOUNT);
        ConnectorObject coBeforeUpdate = coBuilder.build();
        
        // do the update
        Set<Attribute> changeSet = CollectionUtil.newSet(coBeforeUpdate.getAttributes());

        facade.update(UpdateApiOp.Type.REPLACE, oc, changeSet, null);
    }    
    
    /**
     * Test method for {@link MySQLUserConnector#delete(ObjectClass, Uid, OperationOptions)}.
     * 
     */
    @Test
    public void testDelete() {
        assertNotNull(facade);
        String userName = TST_USER2;
        //To be sure it is created
        quitellyCreateUser(userName);
        // retrieve the object
        testUserFound(userName, true);  
        
        facade.delete(ObjectClass.ACCOUNT, new Uid(TST_USER2), null);
        
        // retrieve the object
        testUserFound(userName, false);  
    }

    
    /**
     * Test method for {@link MySQLUserConnector#delete(ObjectClass, Uid, OperationOptions)}.
     * 
     */
    @Test(expected=UnknownUidException.class)
    public void testDeleteUnexisting() {
        assertNotNull(facade);
        String userName = TST_USER2;
        //To be sure it is created
        quitellyCreateUser(userName);
        // retrieve the object
        testUserFound(userName, true);  
        
        facade.delete(ObjectClass.ACCOUNT, new Uid("UNKNOWN"), null);  
    }    
    
    /**
     * Test method for {@link MySQLUserConnector#newConnection()}.
     * @throws Exception
     */
    @Test()
    public void testFacade() throws Exception {
        assertNotNull(facade);
        //facade.test();
    }

    /**
     * test method
     */
    @Test
    public void testTestMethod() {
        ConnectorFacade facade = getFacade();
        facade.test();
    }        
    
    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindJustOneObject() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByContains() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new ContainsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByEndWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EndsWithFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }


    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByStartWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new StartsWithFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByUid() {
        final Uid expected = new Uid(config.getUsermodel());
        FindUidObjectHandler handler = new FindUidObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final Uid actual = handler.getUid();
        assertNotNull(actual);
        assertTrue(actual.is(expected.getName()));  
     }


    
    /**
     * Test method for {@link MySQLUserConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationHost() throws Exception {
        config.setHost("");
        config.validate();
    }

    /**
     * Test method for {@link MySQLUserConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationDriver() throws Exception {
        config.setDriver("");
        config.validate();
    }

    /**
     * Test method for {@link MySQLUserConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationLogin() throws Exception {
        config.setLogin("");
        config.validate();
    }
    
    /**
     * Test method for {@link MySQLUserConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationPort() throws Exception {
        config.setPort("");
        config.validate();
    }

    /**
     * Test method for {@link MySQLUserConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationUsermodel() throws Exception {
        config.setUsermodel("");
        config.validate();
    }

    /**
     * Test method for {@link MySQLUserConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test()
    public void testValidateConfiguration() throws Exception {
        config.validate();
    }    
    
    
    /**
     * Test method for {@link MySQLUserConnector#authenticate(username, password, options)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test
    public void testAuthenticateOriginal() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName);    
        
        // test user created
        testUserFound(userName, true);
        
        final Uid uid = facade.authenticate(userName, testPassword, null);
        assertEquals(userName, uid.getUidValue());
        quitellyDeleteUser(userName); 
    }
   
    
    /**
     * Test method for {@link MySQLUserConnector#authenticate(String, GuardedString, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test(expected=InvalidCredentialException.class)
    public void testAuthenticateWrongOriginal() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName);    
        
        // retrieve the object
        testUserFound(userName, true);
        try {
            facade.authenticate(userName, new GuardedString("blaf".toCharArray()), null);
        } finally {
            quitellyDeleteUser(userName);
        }
    }    
        
    /**
     * Test method for {@link MySQLUserConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test
    public void testCreateUpdateAutenticate() {
        final String NEWPWD = "newvalue";        
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName);    
        
        // retrieve the object      
        Uid uid = new Uid(testUserFound(userName, true)); 
        
        // create updated connector object
        ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
        coBuilder.setObjectClass(ObjectClass.ACCOUNT);
        coBuilder.addAttribute(AttributeBuilder.buildPassword(NEWPWD.toCharArray()));
        coBuilder.setName(userName);
        coBuilder.setUid(uid);
        ConnectorObject coUpdate = coBuilder.build();
        
        // do the update
        final Uid uidUpdate = facade.update(UpdateApiOp.Type.REPLACE, coUpdate.getObjectClass() , coUpdate.getAttributes(), null);
        
        // uids should be the same
        assertEquals(userName, uidUpdate.getUidValue());
        
        facade.authenticate(userName, new GuardedString(NEWPWD.toCharArray()), null);
        
        quitellyDeleteUser(TST_USER1);   
    }
   
    private Uid createUser(String user, GuardedString password, String host) {
        Set<Attribute> tuas = getUserAttributeSet(user, password, host);
        assertNotNull(tuas);
        return facade.create(ObjectClass.ACCOUNT, tuas, null);
    }      
    
    private ConnectorFacade getFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(MySQLUserConnector.class, config);
        return factory.newInstance(impl);
    }
    
    /**
     * Create not created user and test it was created
     */
    private void quitellyCreateUser(String userName) {
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        ResultSet result = null;
        final List<Object> values = new ArrayList<Object>();
        values.add(userName);
        values.add(testPassword);
        final String SQL_CREATE_TEMPLATE = "CREATE USER ? IDENTIFIED BY ?";
        log.info("quitelly Create User {0}", userName);
        try {
            conn = MySQLUserConnector.newConnection(newConfiguration());
            ps = conn.prepareStatement(SQL_CREATE_TEMPLATE);
            SQLUtil.setParams(ps, values);
            ps.execute();
            conn.commit();
        } catch (SQLException ex) {
            log.info("quitelly Create User {0} has expected exception {1}", userName, ex.getMessage());
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        testUserFound(userName, true);
        log.ok("quitelly Create User {0}", userName);
    }

    /**
     * Delete not deleted User and test it was deleted
     */
    private void quitellyDeleteUser(String userName) {
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        ResultSet result = null;
        final List<Object> values = new ArrayList<Object>();
        values.add(userName);
        final String SQL_DELETE_TEMPLATE = "DROP USER ?";
        log.info("quitelly Delete User {0}", userName);
        try {
            conn = MySQLUserConnector.newConnection(newConfiguration());
            ps = conn.prepareStatement(SQL_DELETE_TEMPLATE);
            SQLUtil.setParams(ps, values);
            ps.execute();
            conn.commit();
        } catch (SQLException ex) {
            log.info("quitelly Delete User {0} has expected exception {1}", userName, ex.getMessage());
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        testUserFound(userName, false);
        log.ok("quitelly Delete User {0}", userName);
    }
    
    /**
     * Test method for {@link MySQLUserConnector#schema()}.
     */
    @Test
    public void testSchemaApi() {
        Schema schema = facade.schema();
        // Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(1, objectInfos.size());
        ObjectClassInfo objectInfo = (ObjectClassInfo) objectInfos.toArray()[0];
        assertNotNull(objectInfo);
        // the object class has to ACCOUNT_NAME
        assertEquals(ObjectClass.ACCOUNT_NAME, objectInfo.getType());
        // iterate through AttributeInfo Set
        Set<AttributeInfo> attInfos = objectInfo.getAttributeInfo();
        
        assertNotNull(AttributeInfoUtil.find(Name.NAME, attInfos));
        assertNotNull(AttributeInfoUtil.find(OperationalAttributes.PASSWORD_NAME, attInfos));
    }
    
   
    private Set<Attribute> getUserAttributeSet(String tstUser, GuardedString tstPassword, String tstHost) {
        Set<Attribute> ret = new HashSet<Attribute>();
        ret.add(AttributeBuilder.build(Name.NAME, tstUser));
        ret.add(AttributeBuilder.buildPassword(tstPassword));
        ret.add(AttributeBuilder.build(MySQLUserConfiguration.HOST, tstHost));
        return ret;
    }
    
    /**
     * @param userName
     */
    private String testUserFound(String userName, boolean found) {
        String ret = null;

        // update the last change
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        ResultSet result = null;
        final List<Object> values = new ArrayList<Object>();
        values.add(userName);
        final String SQL_SELECT = "SELECT user FROM mysql.user WHERE user = ?";               
        log.info("test User {0} found {1} ", userName, found);   
        try {
            conn  =  MySQLUserConnector.newConnection(newConfiguration());
            ps = conn.prepareStatement(SQL_SELECT);
            SQLUtil.setParams(ps, values);
            result = ps.executeQuery();
            if(result.next()) {
                ret = result.getString(1);
            }
            conn.commit();
        } catch (SQLException ex) {
            log.error(ex,"test User {0} found {1} ", userName, found);   
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        if(found) {
            assertNotNull("The object for "+userName+" is null", ret);
        } else {
            assertNull("The object for "+userName+" is not null", ret);
        }
        log.ok("test User {0} found {1} ", userName, found);   
        return ret;
    }  

    /**
     * Test internal implementation for finding the objects
     * @author Petr Jung
     */
    static class FindUidObjectHandler implements ResultsHandler {

        private ConnectorObject connectorObject = null;
        
        private boolean found = false;
        
        private final Uid uid;
        
        /**
         * @param uid
         */
        public FindUidObjectHandler(Uid uid) {
            this.uid = uid;
        }
        
        /**
         * getter method
         * @return object value
         */
        public ConnectorObject getConnectorObject() {
            return connectorObject;
        }
        
        /**
         * @return the uid
         */
        public Uid getUid() {
            return uid;
        }

        public boolean handle(ConnectorObject obj) {
            System.out.println("Object: " + obj);
            if (obj.getUid().equals(uid)) {
                found = true;
                this.connectorObject = obj;
                return false;
            }
            return true;
        }

        /**
         * @return the found
         */
        public boolean isFound() {
            return found;
        }

        /**
         * @param found the found to set
         */
        public void setFound(boolean found) {
            this.found = found;
        }

    }    
}
