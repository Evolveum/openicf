/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;
import static org.identityconnectors.oracle.OracleUserAttribute.*;

import java.sql.*;
import java.util.*;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.junit.*;
import org.junit.matchers.JUnitMatchers;

/**
 * @author kitko
 *
 */
public class OracleOperationCreateTest extends OracleConnectorAbstractTest {
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
     * @throws SQLException 
     */
    @Test
    public void testCreateUserLocal() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        //Test local authentication
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(newUser, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        
        try {
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "grant connect to " + testConf.getCSSetup().formatToken(USER_NAME,newUser));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        connector.authenticate( ObjectClass.ACCOUNT, newUser, password, null);
        connector.delete(ObjectClass.ACCOUNT, uid, null);
        uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertEquals("OPEN",record.status);
        connector.delete(ObjectClass.ACCOUNT, uid, null);
    }
    
    /** Test create of user with external authentication 
     * @throws SQLException */
    @Test
    public void testCreateUserExternal() throws SQLException{
        //Test external authentication
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
            connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_EXTERNAL);
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(newUser, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        connector.delete(ObjectClass.ACCOUNT, uid, null);
    }
    
    /** Test create global user   
     * @throws SQLException */
    @Test
    public void testCreateUserGlobal() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
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
            assertEqualsIgnoreCase(newUser, uid.getUidValue());
            UserRecord record = userReader.readUserRecord(newUser);
            assertNotNull(record);
            assertEquals(newUser, record.userName);
            assertNull(record.expireDate);
            assertNull(record.externalName);
            assertEquals("OPEN",record.status);
            connector.delete(ObjectClass.ACCOUNT, uid, null);
        }
    }
    
    /** Test with all possible attributes for create 
     * @throws SQLException */
    @Test
    public void testCreateUserWithTableSpaces() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute defaultTs =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME,findDefaultTS(connector.getAdminConnection()));
        Attribute tempTs =  AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME,findTempTS(connector.getAdminConnection()));
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTs, tempTs), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(newUser, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        assertEquals(AttributeUtil.getStringValue(defaultTs), record.defaultTableSpace);
        assertEquals(AttributeUtil.getStringValue(tempTs), record.temporaryTableSpace);
        connector.delete(ObjectClass.ACCOUNT, uid, null);
    }
    
    /** Test create with custom profile 
     * @throws SQLException */
    @Test
    public void testCreateUserWithProfile() throws SQLException{
        String profileName = "myprofile";
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().formatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create profile " + testConf.getCSSetup().formatToken(PROFILE,profileName) + "limit password_lock_time 6");
            connector.getAdminConnection().commit();
        }
        catch(SQLException e){
            fail(e.getMessage());
        }
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute profile = AttributeBuilder.build(OracleConnector.ORACLE_PROFILE_ATTR_NAME, profileName);
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,profile), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(newUser, uid.getUidValue());
        UserRecord record =userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        assertEqualsIgnoreCase(profileName,record.profile);
        connector.delete(ObjectClass.ACCOUNT, uid, null);
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().formatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
    }
    
    /** Test create user with expired password 
     * @throws SQLException */
    @Test
    public void testCreateUsersExpired() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
        Uid uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,expirePassword), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(newUser, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("EXPIRED",record.status);
        connector.delete(ObjectClass.ACCOUNT, uid, null);
    }
    
    /** Test Create user locked/unlocked 
     * @throws SQLException */
    @Test
    public void testCreateUserLocked() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.TRUE);
        connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,enabled), null);
        UserRecord record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertEquals("OPEN",record.status);
        assertNull(record.lockDate);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.FALSE);
        connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,enabled), null);
        record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEqualsIgnoreCase(newUser, record.userName);
        assertEquals("LOCKED",record.status);
        assertNotNull(record.lockDate);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
    }
    
    /**
     * Test create user with roles
     * @throws SQLException
     */
    @Test
    public void testCreateWithRoles() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        String role = "testrole";
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role " + testConf.getCSSetup().formatToken(ROLE,role));
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create role " + testConf.getCSSetup().formatToken(ROLE,role));
        Attribute roles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME, Arrays.asList(role));
        connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute, roles), null);
        UserRecord record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEquals(newUser, record.userName);
        OracleRolePrivReader roleReader = new OracleRolePrivReader(connector.getAdminConnection());
        final List<String> rolesRead = roleReader.readRoles(newUser);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("testrole"));
    }
    
    /**
     * Test create user with profiles
     * @throws SQLException
     */
    @Test
    public void testCreateWithPrivileges() throws SQLException{
        String newUser = "newUser";
        if(userReader.userExist(newUser)){
              connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE");
        }
        catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table mytable(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE");
        connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,privileges), null);
        UserRecord record = userReader.readUserRecord(newUser);
        assertNotNull(record);
        assertEquals(newUser, record.userName);
        OracleRolePrivReader roleReader = new OracleRolePrivReader(connector.getAdminConnection());
        final List<String> privRead = roleReader.readPrivileges(newUser);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        Assert.assertThat(privRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privRead, JUnitMatchers.hasItem("SELECT ON MYTABLE"));
    }
    
    private String findDefaultTS(Connection conn) throws SQLException{
        return getTestUserRecord(conn).defaultTableSpace;
    }
    
    private String findTempTS(Connection conn) throws SQLException{
        return getTestUserRecord(conn).temporaryTableSpace;
    }
    
    private UserRecord getTestUserRecord(Connection conn) throws SQLException{
        String newUser = "testTS";
        if(!userReader.userExist(newUser)){
            Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
            Attribute name = new Name(newUser);
            GuardedString password = new GuardedString("hello".toCharArray());
            Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
            connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        }
        UserRecord record = userReader.readUserRecord(newUser);
        connector.delete(ObjectClass.ACCOUNT, new Uid(newUser),null);
        return record;
    }
    

}
