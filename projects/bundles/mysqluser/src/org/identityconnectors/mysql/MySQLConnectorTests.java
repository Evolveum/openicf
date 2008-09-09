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
package org.identityconnectors.mysql;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
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
public class MySQLConnectorTests {

    private static String idmDriver = null;
    private static String idmHost = null;

    private static String idmLogin = null;
    private static GuardedString testPassword = null;
    private static String idmModelUser = null;
    private static String idmPassword = null;
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
        
        idmHost = TestHelpers.getProperty("host.connector.string", null);
        idmLogin = TestHelpers.getProperty("login.connector.string", null);
        idmPassword = TestHelpers.getProperty("password.connector.string", null);
        idmPort = TestHelpers.getProperty("port.connector.string", null);
        idmDriver = TestHelpers.getProperty("driver.connector.string", null); 
        idmModelUser = TestHelpers.getProperty("usermodel.connector.string", null);        
        testPassword = new GuardedString(TestHelpers.getProperty("testpassword.connector.string", null).toCharArray());
        
        assertNotNull("Host must be configured for test", idmHost);
        assertNotNull("Login must be c  onfigured for test", idmLogin);
        assertNotNull("Password must be configured for test", idmPassword);
        assertNotNull("Port must be configured for test", idmPort);
        assertNotNull("Driver must be configured for test", idmDriver);
        assertNotNull("ModelUser must be configured for test", idmModelUser);
        assertNotNull("ModelPassword must be configured for test", testPassword);

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
    private static MySQLConfiguration newConfiguration() {
        MySQLConfiguration config = new MySQLConfiguration();
        config.setDriver(idmDriver);
        config.setHost(idmHost);
        config.setLogin(idmLogin);
        config.setPassword(idmPassword);
        config.setPort(idmPort);
        config.setUsermodel(idmModelUser);
        return config;
    }
    
    // Test config
    private MySQLConfiguration config = null;

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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#create(ObjectClass, Set, OperationOptions)}.
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#create(ObjectClass, Set, OperationOptions)}.
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#update(ObjectClass, Set, OperationOptions)}.
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
        ConnectorObject co = testUserFound(userName, true);
        
        Uid uid = co.getUid();
        
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
        ConnectorObject coAfterUpdate = testUserFound(newName, true);
        assertEquals(coBeforeUpdate.getName().getNameValue(), coAfterUpdate.getName().getNameValue());
        
        quitellyDeleteUser(TST_USER1); 
        quitellyDeleteUser(TST_USER3);   
    }

    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test(expected=IllegalStateException.class)
    public void testUpdateUnsupported() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName);    
        // retrieve the object
        ConnectorObject co = testUserFound(userName, true);
        
        Uid uid = co.getUid();
        
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#delete(ObjectClass, Uid, OperationOptions)}.
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#delete(ObjectClass, Uid, OperationOptions)}.
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#newConnection()}.
     * @throws Exception
     */
    @Test()
    public void testFacade() throws Exception {
        assertNotNull(facade);
        //facade.test();
    }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindJustOneObject() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindObjectHandler handler = new FindObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final List<ConnectorObject> actual = handler.getConnectorObjects();
        assertNotNull(actual);
        ConnectorObject co = actual.get(0);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(co.getName()));
        assertEquals("The model user is not mapped to one object", 1, actual.size());        
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByContains() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindObjectHandler handler = new FindObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new ContainsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final List<ConnectorObject> actual = handler.getConnectorObjects();
        assertNotNull(actual);
        ConnectorObject co = actual.get(0);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(co.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByEndWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindObjectHandler handler = new FindObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EndsWithFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final List<ConnectorObject> actual = handler.getConnectorObjects();
        assertNotNull(actual);
        ConnectorObject co = actual.get(0);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(co.getName()));
     }


    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByStartWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindObjectHandler handler = new FindObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new StartsWithFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final List<ConnectorObject> actual = handler.getConnectorObjects();
        assertNotNull(actual);
        ConnectorObject co = actual.get(0);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(co.getName()));
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationHost() throws Exception {
        config.setHost("");
        config.validate();
    }

    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationDriver() throws Exception {
        config.setDriver("");
        config.validate();
    }

    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationLogin() throws Exception {
        config.setLogin("");
        config.validate();
    }
    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationPort() throws Exception {
        config.setPort("");
        config.validate();
    }

    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationUsermodel() throws Exception {
        config.setUsermodel("");
        config.validate();
    }
    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationPassword() throws Exception {
        config.setPassword("");
        config.validate();
    }
    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     * @throws Exception
     */
    @Test()
    public void testValidateConfiguration() throws Exception {
        config.validate();
    }    
    
    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#authenticate(username, password, options)}.
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
        
        facade.authenticate(userName, testPassword, null);
        
        quitellyDeleteUser(userName); 
    }
   
    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#authenticate(username, password, options)}.
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
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#update(ObjectClass, Set, OperationOptions)}.
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
        ConnectorObject co = testUserFound(userName, true);        
        Uid uid = co.getUid();
        
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
        APIConfiguration impl = TestHelpers.createTestConfiguration(MySQLConnector.class, config);
        return factory.newInstance(impl);
    }
    
    /**
     * Create not created user and test it was created
     */
    private void quitellyCreateUser(String userName) {
        try {
            createUser(userName, testPassword, TST_HOST);
        } catch (ConnectorException e) {
            // expected
        } finally {
            testUserFound(userName, true);
        }
    }

    /**
     * Delete not deleted User and test it was deleted
     */
    private void quitellyDeleteUser(String userName) {
        try {
            facade.delete(ObjectClass.ACCOUNT, new Uid(userName), null);
        } catch (ConnectorException expected) {
            //expected
        } finally {
            testUserFound(userName, false);  
        }
    }
    
    /**
     * Test method for {@link org.identityconnectors.mysql.MySQLConnector#schema()}.
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
        ret.add(AttributeBuilder.build(MySQLConfiguration.HOST, tstHost));
        return ret;
    }
    
    /**
     * @param useName
     */
    @SuppressWarnings("synthetic-access")
    private ConnectorObject testUserFound(String useName, boolean found) {
        Uid uid = new Uid(useName);
        FindUidObjectHandler handler = new FindUidObjectHandler(uid);
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(uid), handler, null);
        if(found) {
            assertTrue("The object for "+useName+" was not found",handler.found);
            assertNotNull("The object for "+useName+" is null", handler.getConnectorObject());
            return handler.getConnectorObject();
        } else {
            assertFalse("The object for "+useName+" was found",handler.found);
            assertNull("The object for "+useName+" is not null", handler.getConnectorObject());
            return null;
        }        
    }  
    
    /**
     * Test internal implementation for finding the objects
     */
    static class FindObjectHandler implements ResultsHandler {

        private final Attribute attribute;
        
        private List<ConnectorObject> connectorObjects = new ArrayList<ConnectorObject>();
        
        private boolean found = false;
        
        /**
         * @param attribute
         */
        public FindObjectHandler(Attribute attribute) {
            this.attribute = attribute;
        }
        
        /**
         * @return
         */
        public List<ConnectorObject> getConnectorObjects() {
            return connectorObjects;
        }
        
        /**
         * @return
         */
        public List<ConnectorObject> getObjects() {
            return connectorObjects;
        }


        public boolean handle(ConnectorObject obj) {
            System.out.println("Object: " + obj);
            if (obj.getName().is(attribute.getName())) {
                found = true;
                connectorObjects.add(obj);
            }
            return true;
        }

 
        /**
         * @return
         */
        public boolean isFound() {
            return found;
        }


        /**
         * @param found
         */
        public void setFound(boolean found) {
            this.found = found;
        }

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
         * @return
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
