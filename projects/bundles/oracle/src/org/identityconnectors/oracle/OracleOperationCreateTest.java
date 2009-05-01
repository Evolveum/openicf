/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.identityconnectors.oracle.OracleUserAttributeCS.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;

/**
 * @author kitko
 *
 */
public class OracleOperationCreateTest extends OracleConnectorAbstractTest {
    static final String TEST_USER = "testUser";
    
    
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
    
    /** Deletes before create  */
    @Before
    public void before(){
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER), null);
        }
        catch(UnknownUidException e){}
    }
    
    /** Deletes after create */
    @After
    public void after(){
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER), null);
        }
        catch(UnknownUidException e){}
    }
    
        
    /**
     * Test method for local create
     * @throws SQLException 
     */
    @Test
    public void testCreateUserLocal() throws SQLException{
        //Test local authentication
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        
        try {
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "grant connect to " + testConf.getCSSetup().formatToken(USER_NAME,uid.getUidValue()));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        facade.authenticate( ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertEquals("OPEN",record.status);
    }
    
    /** Test create of user with external authentication 
     * @throws SQLException */
    @Test
    public void testCreateUserExternal() throws SQLException{
        //Test external authentication
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_EXTERNAL);
        try{
        	facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,AttributeBuilder.buildPassword(password)), null);
        	fail("Cannot set password for external authentication");
        }catch(RuntimeException e){}
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertNull(record.expireDate);
        assertNull(record.externalName);
        assertEquals("OPEN",record.status);
        assertEquals("EXTERNAL",record.password);
    }
    
    /** Test create global user   
     * @throws SQLException */
    @Test
    public void testCreateUserGlobal() throws SQLException{
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_GLOBAL);
        Attribute globalName = AttributeBuilder.build(OracleConnector.ORACLE_GLOBAL_ATTR_NAME, "global");
        Uid uid = null;
        try{
        	uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,AttributeBuilder.buildPassword(password),globalName), null);
        	fail("Password cannot be provided for global authentication");
        }catch(IllegalArgumentException e){
        }
        try{
            uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,globalName), null);
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
            assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
            UserRecord record = userReader.readUserRecord(uid.getUidValue());
            assertNotNull(record);
            assertEquals(uid.getUidValue(), record.userName);
            assertNull(record.expireDate);
            assertNotNull(record.externalName);
            assertEquals("OPEN",record.status);
        }
    }
    
    /** Test with default tablespace 
     * @throws SQLException */
    @Test
    public void testCreateUserWithDefTableSpace() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute defaultTs =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME,findDefDefaultTS(connector.getAdminConnection()));
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTs), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertEquals("OPEN",record.status);
        assertEquals(AttributeUtil.getStringValue(defaultTs), record.defaultTableSpace);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        
        //Now try to create with other tablespaces, if create is successfull, check whether it is correctly set
        for(String ts : findAllDefTS(connector.getAdminConnection())){
        	defaultTs =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME,ts);
        	try{
        		uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTs), null);
        	}
        	catch(Exception e){
        		//For any reason , when tablespace cannot be used for user
        		continue;
        	}
        	record = userReader.readUserRecord(uid.getUidValue());
        	assertEquals(ts, record.defaultTableSpace);
        	facade.delete(ObjectClass.ACCOUNT, uid, null);
        }
        
    }
    
    /** Test with temp tablespace 
     * @throws SQLException */
    @Test
    public void testCreateUserWithTempTableSpace() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute tempTs =  AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME,findDefTempTS(connector.getAdminConnection()));
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,tempTs), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertEquals("OPEN",record.status);
        assertEquals(AttributeUtil.getStringValue(tempTs), record.temporaryTableSpace);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        
        //Now try to create with other temp tablespaces, if create is successfull, check whether it is correctly set
        for(String ts : findAllTempTS(connector.getAdminConnection())){
        	tempTs =  AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME,ts);
        	try{
        		uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,tempTs), null);
        	}
        	catch(Exception e){
        		//For any reason , when tablespace cannot be used for user
        		continue;
        	}
        	record = userReader.readUserRecord(uid.getUidValue());
        	assertEquals(ts, record.temporaryTableSpace);
        	facade.delete(ObjectClass.ACCOUNT, uid, null);
        }
        
    }
    
    
    /** Test setting of quota */
    @Test
    public void testCreateUserWithQuota() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        String defTS = findDefDefaultTS(connector.getAdminConnection());
		Attribute defaultTsAttr =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME,defTS);
        
		
    	
        //Add quotas for default ts
        Attribute quota = AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME, "66666");
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTsAttr, quota), null);
        //Look at dba_ts_quotas table
        Long bytes = userReader.readUserTSQuota(uid.getUidValue(), defTS); 
        //Oracle will set it to multiply of table space block size, but we will rather not do any mathematic here in test, 
        //other oracle versions can adjust real maxsize other way
        assertTrue("Max bytes must be greater than quota set",new Long("66666").compareTo(bytes) < 0);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        quota = AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"-1");
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTsAttr, quota), null);
        bytes = userReader.readUserTSQuota(uid.getUidValue(), defTS);
        assertEquals("Quata max bytes must be -1 when set to unlimited",new Long("-1"),bytes);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        
        //Add quotas for temp ts
        //This does not work for 10.2, 
        /*
        String tempTS = findTempTS(connector.getAdminConnection()); 
		Attribute tempTsAttr =  AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME,tempTS);
		quota = AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_QUOTA_ATTR_NAME, "3333");
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,tempTsAttr, quota), null);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        */

    }
    
    
    
    /** Test create with custom profile 
     * @throws SQLException */
    @Test
    public void testCreateUserWithProfile() throws SQLException{
        String profileName = "myProfile";
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName) + "limit password_lock_time 6");
            connector.getAdminConnection().commit();
        }
        catch(SQLException e){
            fail(e.getMessage());
        }
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute profile = AttributeBuilder.build(OracleConnector.ORACLE_PROFILE_ATTR_NAME, profileName);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,profile), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record =userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertEquals("OPEN",record.status);
        assertEqualsIgnoreCase(profileName,record.profile);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
    }
    
    /** Test create user with expired password 
     * @throws SQLException */
    @Test
    public void testCreateUserPasswordExpired() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,expirePassword), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertNull(record.expireDate);
        assertEquals("EXPIRED",record.status);
    }
    
    /** Test Create user locked/unlocked 
     * @throws SQLException */
    @Test
    public void testCreateUserLocked() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.TRUE);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,enabled), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertEquals("OPEN",record.status);
        assertNull(record.lockDate);
        facade.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER), null);
        enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.FALSE);
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,enabled), null);
        record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        assertEquals("LOCKED",record.status);
        assertNotNull(record.lockDate);
    }
    
    /**
     * Test create user with roles
     * @throws SQLException
     */
    @Test
    public void testCreateWithRoles() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        String role = "testrole";
        final OracleCaseSensitivitySetup cs = testConf.getCSSetup();
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),
                    "drop role "
                            + cs.normalizeAndFormatToken(OracleUserAttributeCS.ROLE, role));
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create role " + cs.normalizeAndFormatToken(OracleUserAttributeCS.ROLE,role));
        Attribute roles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME, Arrays.asList(role));
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute, roles), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        OracleRolePrivReader roleReader = new OracleRolePrivReader(connector.getAdminConnection());
        final List<String> rolesRead = roleReader.readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem(cs.normalizeToken(OracleUserAttributeCS.ROLE,role)));
    }
    
    /**
     * Test create user with profiles
     * @throws SQLException
     */
	@Test
    public void testCreateWithPrivileges() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE");
        }
        catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table mytable(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE");
        final Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,privileges), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.userName);
        OracleRolePrivReader roleReader = new OracleRolePrivReader(connector.getAdminConnection());
        final List<String> privRead = roleReader.readPrivileges(uid.getUidValue());
        Assert.assertThat(privRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUser() + ".MYTABLE"));
    }
    

}
