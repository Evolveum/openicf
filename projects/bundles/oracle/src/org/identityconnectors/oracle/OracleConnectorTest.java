/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import java.sql.*;
import java.util.*;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.*;

/**
 * @author kitko
 *
 */
public class OracleConnectorTest {
    
    private static OracleConfiguration testConf;
    private static ConnectorFacade facade;
    private static OracleConnector connector;
    
    private static OracleConnector createTestConnector(){
        OracleConnector oc = new OracleConnector();
        oc.init(testConf);
        return oc;
    }
    

    /**
     * Setup for all tests
     */
    @BeforeClass
    public static void setupClass(){
        testConf = OracleConfigurationTest.createSystemConfiguration();
        facade = createFacade(testConf);
        connector = createTestConnector();
    }
    
    private static ConnectorFacade createFacade(OracleConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(OracleConnector.class, conf);
        return factory.newInstance(apiCfg);
    }
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#checkAlive()}.
     */
    @Test
    public void testCheckAlive() {
        OracleConnector oc = createTestConnector();
        oc.checkAlive();
        oc.dispose();
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#getConfiguration()}.
     */
    @Test
    public void testGetConfiguration() {
        OracleConnector oc = createTestConnector();
        OracleConfiguration cfg2 = oc.getConfiguration();
        assertSame(testConf,cfg2);
        oc.dispose();
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    @Test
    public void testInit() {
        OracleConnector oc = createTestConnector();
        oc.dispose();
        
        oc = new OracleConnector();
        OracleConfiguration cfg = new OracleConfiguration();
        try{
            oc.init(cfg);
            fail("Init should fail for uncomplete cfg");
        }
        catch(IllegalArgumentException e){
        }
        
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#test()}.
    
    
    @Test
    public void testTest() {
        OracleConnector oc = createTestConnector();
        oc.test();
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#authenticate(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)}.
     */
    @Test
    public void testAuthenticate() {
        String user = TestHelpers.getProperty("authenticate.user", null);
        String password = TestHelpers.getProperty("authenticate.password", null);
        final Uid uid = facade.authenticate(ObjectClass.ACCOUNT, user, new GuardedString(password.toCharArray()), null);
        assertNotNull(uid);
        
        try{
            facade.authenticate(ObjectClass.ACCOUNT , user, new GuardedString("wrongPassword".toCharArray()), null);
            fail("Authenticate must fail for invalid user/password");
        }
        catch(InvalidCredentialException e){}
        
    }
    
    /** Test fail of create groups */
    @Test
    public void testCreateGroups(){
        //test for fail when using groups
        try{
            connector.create(ObjectClass.GROUP, Collections.<Attribute>emptySet(), null);
            fail("Create must fail for group");
        }
        catch(IllegalArgumentException e){}
    }
    
    /**
     * Test method for local create
     */
    @Test
    public void testCreateLocal(){
        String newUser = "newUser";
        if(new OracleUserReader(connector.getAdminConnection()).userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        //Test local authentication
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        assertNotNull(uid);
        assertEquals(newUser, uid.getUidValue());
        UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(newUser);
        assertNotNull(record);
        assertEquals(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        
        try {
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "grant connect to \"" + newUser + "\"");
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        connector.authenticate( ObjectClass.ACCOUNT, "\"" + newUser + "\"", password, null);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(newUser);
        assertNotNull(record);
        assertEquals(newUser, record.userName);
        assertEquals("OPEN",record.status);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
    }
    
    /** Test create of user with external authentication */
    @Test
    public void testCreateExternal(){
        //Test external authentication
        String newUser = "newUser";
        if(new OracleUserReader(connector.getAdminConnection()).userExist(newUser)){
            connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_EXTERNAL);
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        assertNotNull(uid);
        assertEquals(newUser, uid.getUidValue());
        UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(newUser);
        assertNotNull(record);
        assertEquals(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
    }
    
    /** Test create global user   */
    @Test
    public void testCreateGlobal(){
        String newUser = "newUser";
        if(new OracleUserReader(connector.getAdminConnection()).userExist(newUser)){
            connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_GLOBAL);
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute globalName = AttributeBuilder.build(OracleConnector.ORACLE_GLOBAL_ATTR_NAME, "global");
        Uid uid = null;
        try{
            uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute,globalName), null);
        }
        catch(ConnectorException e){
            if(e.getCause() instanceof SQLException){
                if("67000".equals(((SQLException)e.getCause()).getSQLState()) && 439 == ((SQLException)e.getCause()).getErrorCode()){
                }
                else{
                    fail(e.getMessage());
                }
            }
            else{
                fail(e.getMessage());
            }
        }
        if(uid != null){
            assertEquals(newUser, uid.getUidValue());
            UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(newUser);
            assertNotNull(record);
            assertEquals(newUser, record.userName);
            assertNull(record.expireDate);
            assertNull(record.externalName);
            assertEquals("OPEN",record.status);
            connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
    }
    
    /** Test with all possible attributes for create */
    @Test
    public void testCreateTableSpaces(){
        String newUser = "newUser";
        if(new OracleUserReader(connector.getAdminConnection()).userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute passwordExpire = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
        Attribute defaultTs =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME,findDefaultTS(connector.getAdminConnection()));
        Attribute tempTs =  AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME,findTempTS(connector.getAdminConnection()));
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(authentication);
        attributes.add(name);
        attributes.add(passwordAttribute);
        attributes.add(passwordExpire);
        attributes.add(defaultTs);
        attributes.add(tempTs);
        
        Uid uid = connector.create(ObjectClass.ACCOUNT,attributes, null);
        assertNotNull(uid);
        assertEquals(newUser, uid.getUidValue());
        UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(newUser);
        assertNotNull(record);
        assertEquals(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("EXPIRED",record.status);
        assertEquals(AttributeUtil.getStringValue(defaultTs), record.defaultTableSpace);
        assertEquals(AttributeUtil.getStringValue(tempTs), record.temporaryTableSpace);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
    }
    
    @Test
    public void testCreateProfile(){
        
    }
    
    @Test
    public void testCreateExpire(){
        
    }
    
    @Test
    public void testCreateLock(){
        
    }
    
    
    /** 
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#delete(ObjectClass, Uid, OperationOptions)}
     */
    @Test
    public void testDelete(){
        String newUser = "newUser";
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
            try{
                facade.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
                fail("Delete should fail for unexistent user");
            }
            catch(UnknownUidException e){}
        }
        catch(UnknownUidException e){}
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), null);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        try{
            facade.delete(ObjectClass.ACCOUNT, uid, null);
            fail("Delete should fail for unexistent user, previous delete was not succesful");
        }
        catch(UnknownUidException e){}
    }
    
    
    private String findDefaultTS(Connection conn){
        return getTestuser(conn).defaultTableSpace;
    }
    
    private String findTempTS(Connection conn){
        return getTestuser(conn).temporaryTableSpace;
    }
    
    private UserRecord getTestuser(Connection conn){
        String newUser = "testTS";
        if(!new OracleUserReader(conn).userExist("testUser")){
            Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
            Attribute name = new Name(newUser);
            GuardedString password = new GuardedString("hello".toCharArray());
            Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
            connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        }
        UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(newUser);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser),null);
        return record;
    }
    

}
