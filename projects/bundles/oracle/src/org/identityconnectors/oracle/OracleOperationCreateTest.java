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
import static org.identityconnectors.oracle.OracleConstants.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
import org.identityconnectors.test.common.TestHelpers;
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
    
    
    /** Deletes before create  */
    @Before
    public void before(){
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER.toUpperCase()), null);
        }
        catch(UnknownUidException e){
        	
        }
    }
    
    /** Deletes after create */
    @After
    public void after(){
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER.toUpperCase()), null);
        }
        catch(UnknownUidException e){}
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
     * @throws SQLException 
     */
    @Test
    public void testCreateUserLocal() throws SQLException{
        //Test local authentication
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertNull(record.getExternalName());
        assertEquals("OPEN",record.getStatus());
        
        try {
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "grant connect to " + testConf.getCSSetup().formatToken(USER,uid.getUidValue()));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        facade.authenticate( ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null);
        record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertEquals("OPEN",record.getStatus());
    }
    
    /** Test create of user with external authentication 
     * @throws SQLException */
    @Test
    public void testCreateUserExternal() throws SQLException{
        //Test external authentication
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_EXTERNAL);
        try{
        	facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,AttributeBuilder.buildPassword(password)), null);
        	fail("Cannot set password for external authentication");
        }catch(RuntimeException e){}
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertNull(record.getExpireDate());
        assertNull(record.getExternalName());
        assertEquals("OPEN",record.getStatus());
        assertEquals("EXTERNAL",record.getPassword());
    }
    
    /** Test create global user   
     * @throws SQLException */
    @Test
    public void testCreateUserGlobal() throws SQLException{
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_GLOBAL);
        Attribute globalName = AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME, "global");
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
            assertEquals(uid.getUidValue(), record.getUserName());
            assertNull(record.getExpireDate());
            assertNotNull(record.getExternalName());
            assertEquals("OPEN",record.getStatus());
        }
    }
    
    /** Test with default tablespace 
     * @throws SQLException */
    @Test
    public void testCreateUserWithDefTableSpace() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute defaultTs =  AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,findDefDefaultTS(connector.getAdminConnection()));
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTs), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertEquals("OPEN",record.getStatus());
        assertEquals(AttributeUtil.getStringValue(defaultTs), record.getDefaultTableSpace());
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        
        //Now try to create with other tablespaces, if create is successfull, check whether it is correctly set
        for(String ts : findAllDefTS(connector.getAdminConnection())){
        	defaultTs =  AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,ts);
        	try{
        		uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTs), null);
        	}
        	catch(Exception e){
        		//For any reason , when tablespace cannot be used for user
        		continue;
        	}
        	record = userReader.readUserRecord(uid.getUidValue());
        	assertEquals(ts, record.getDefaultTableSpace());
        	facade.delete(ObjectClass.ACCOUNT, uid, null);
        }
        
    }
    
    /** Test with temp tablespace 
     * @throws SQLException */
    @Test
    public void testCreateUserWithTempTableSpace() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute tempTs =  AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,findDefTempTS(connector.getAdminConnection()));
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,tempTs), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertEquals("OPEN",record.getStatus());
        assertEquals(AttributeUtil.getStringValue(tempTs), record.getTemporaryTableSpace());
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        
        //Now try to create with other temp tablespaces, if create is successfull, check whether it is correctly set
        for(String ts : findAllTempTS(connector.getAdminConnection())){
        	tempTs =  AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,ts);
        	try{
        		uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,tempTs), null);
        	}
        	catch(Exception e){
        		//For any reason , when tablespace cannot be used for user
        		continue;
        	}
        	record = userReader.readUserRecord(uid.getUidValue());
        	assertEquals(ts, record.getTemporaryTableSpace());
        	facade.delete(ObjectClass.ACCOUNT, uid, null);
        }
        
    }
    
    
    /** Test setting of quota */
    @Test
    public void testCreateUserWithQuota() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        String defTS = findDefDefaultTS(connector.getAdminConnection());
		Attribute defaultTsAttr =  AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,defTS);
        
		
    	
        //Add quotas for default ts
        Attribute quota = AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME, "66666");
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,defaultTsAttr, quota), null);
        //Look at dba_ts_quotas table
        Long bytes = userReader.readUserTSQuota(uid.getUidValue(), defTS); 
        //Oracle will set it to multiply of table space block size, but we will rather not do any mathematic here in test, 
        //other oracle versions can adjust real maxsize other way
        assertTrue("Max bytes must be greater than quota set",new Long("66666").compareTo(bytes) < 0);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        quota = AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"-1");
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
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute profile = AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME, profileName);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,profile), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record =userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertEquals("OPEN",record.getStatus());
        assertEqualsIgnoreCase(profileName,record.getProfile());
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
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,expirePassword), null);
        assertNotNull(uid);
        assertEqualsIgnoreCase(TEST_USER, uid.getUidValue());
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertNull(record.getExpireDate());
        assertEquals("EXPIRED",record.getStatus());
        //Even if no password is set, default password will be set
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, expirePassword), null);
        record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEquals("EXPIRED",record.getStatus());
    }
    
    /** Test Create user locked/unlocked 
     * @throws SQLException */
    @Test
    public void testCreateUserLocked() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.TRUE);
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,enabled), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertEquals("OPEN",record.getStatus());
        assertNull(record.getLockDate());
        facade.delete(ObjectClass.ACCOUNT, uid , null);
        enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.FALSE);
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,enabled), null);
        record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        assertEquals("LOCKED",record.getStatus());
        assertNotNull(record.getLockDate());
    }
    
    /**
     * Test create user with roles
     * @throws SQLException
     */
    @Test
    public void testCreateWithRoles() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
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
        Attribute roles = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME, Arrays.asList(role));
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute, roles), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
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
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE");
        }
        catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table mytable(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE");
        final Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication, name, passwordAttribute,privileges), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertNotNull(record);
        assertEqualsIgnoreCase(TEST_USER, record.getUserName());
        OracleRolePrivReader roleReader = new OracleRolePrivReader(connector.getAdminConnection());
        final List<String> privRead = roleReader.readPrivileges(uid.getUidValue());
        Assert.assertThat(privRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE"));
    }
	
	/** Test that create will fail on any not known attribute 
	 * @throws SQLException */
	@Test
	public void testValidCreateAttributes() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION");
        Attribute enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.TRUE);
        Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
        Set<Attribute> attrs = new HashSet<Attribute>(Arrays.asList(authentication,name,passwordAttribute,
        		privileges,enabled,expirePassword));
		Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
		facade.delete(ObjectClass.ACCOUNT, uid, null);
		//Now add some dummy attribute
		Set<Attribute> badAttrs = new HashSet<Attribute>(attrs); 
		badAttrs.add(AttributeBuilder.buildDisableDate(0));
		try{
			facade.create(ObjectClass.ACCOUNT, badAttrs, null);
			fail("Disabled date should not be supported");
		}
		catch(IllegalArgumentException e){}
		badAttrs = new HashSet<Attribute>(attrs); 
		badAttrs.add(AttributeBuilder.buildPasswordExpirationDate(0));
		try{
			facade.create(ObjectClass.ACCOUNT, badAttrs, null);
			fail("Expired password date should not be supported");
		}
		catch(IllegalArgumentException e){}
		badAttrs = new HashSet<Attribute>(attrs); 
		badAttrs.add(AttributeBuilder.build("dummy","dummyValue"));
		try{
			facade.create(ObjectClass.ACCOUNT, badAttrs, null);
			fail("Dummy attribute should not be supported");
		}
		catch(IllegalArgumentException e){}
		
		//Test create no attributes
		try{
			facade.create(ObjectClass.ACCOUNT, Collections.<Attribute>emptySet(), null);
			fail("Create must fail for no attributes");
		}catch(RuntimeException e){}
		
		//try other case of attributes
        authentication = AttributeBuilder.build(ORACLE_AUTHENTICATION_ATTR_NAME.toUpperCase(), OracleConstants.ORACLE_AUTH_LOCAL);
        name = new Name(TEST_USER);
        password = new GuardedString("hello".toCharArray());
        passwordAttribute = AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME.toUpperCase(),password);
        privileges = AttributeBuilder.build(ORACLE_PRIVS_ATTR_NAME.toUpperCase(),"CREATE SESSION");
        enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME.toUpperCase(),Boolean.TRUE);
        expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME.toUpperCase(),Boolean.FALSE);
        attrs = new HashSet<Attribute>(Arrays.asList(authentication,name,passwordAttribute,
        		privileges,enabled,expirePassword));
		uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
		UserRecord record = new OracleUserReader(connector.getAdminConnection(),TestHelpers.createDummyMessages()).readUserRecord(uid.getUidValue());
		assertNotNull(record);
		assertEquals(uid.getUidValue(), record.getUserName());
		facade.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
	}
	
	/** Test that create will fail 
	 * @throws SQLException */
	@Test
	public void testCreateFail() throws SQLException{
		OracleConnectorImpl testConnector = createTestConnector();
		try{
			testConnector.delete(ObjectClass.ACCOUNT, new Uid(TEST_USER), null);
		}
		catch(UnknownUidException e){}
		Uid uid = testConnector.create(ObjectClass.ACCOUNT, Collections.<Attribute>singleton(new Name(TEST_USER)), null);
		testConnector.delete(ObjectClass.ACCOUNT, uid, null);
		OracleSpecificsTest.killConnection(connector.getAdminConnection(), testConnector.getAdminConnection());
		try{
			uid = testConnector.create(ObjectClass.ACCOUNT, Collections.<Attribute>singleton(new Name(TEST_USER)), null);
			fail("Create must fail for killed connection");
		}catch(RuntimeException e){}
		testConnector.dispose();
	}
	
    /** Test setting lockout parameter 
     * @throws SQLException */
	@Test
    public void testUpdateLockOut() throws SQLException{
		Uid uid = facade.create(ObjectClass.ACCOUNT, Collections.<Attribute>singleton(new Name(TEST_USER)), null);
    	assertEquals("OPEN",userReader.readUserRecord(uid.getUidValue()).getStatus());
    	facade.delete(ObjectClass.ACCOUNT, uid, null);
    	uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name(TEST_USER), AttributeBuilder.buildLockOut(true)), null);
    	assertEquals("LOCKED",userReader.readUserRecord(uid.getUidValue()).getStatus());
    	facade.delete(ObjectClass.ACCOUNT, uid, null);
    	uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name(TEST_USER), AttributeBuilder.buildLockOut(false)), null);
    	assertEquals("OPEN",userReader.readUserRecord(uid.getUidValue()).getStatus());
    	facade.delete(ObjectClass.ACCOUNT, uid, null);
    }
	
}
