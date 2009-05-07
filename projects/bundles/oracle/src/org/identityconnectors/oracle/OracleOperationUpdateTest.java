/**
 * 
 */
package org.identityconnectors.oracle;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.junit.*;
import org.junit.matchers.JUnitMatchers;

import static org.identityconnectors.oracle.OracleUserAttributeCS.*;
import static org.junit.Assert.*;


/**
 * @author kitko
 *
 */
public class OracleOperationUpdateTest extends OracleConnectorAbstractTest{
    private static final String TEST_USER = "testUser";
    private Uid uid;
    private GuardedString password;
    
    /**
     * Creates test user to be used in all test methods
     */
    @Before
    public void createTestUser(){
        uid = new Uid(TEST_USER);
        try{
            facade.delete(ObjectClass.ACCOUNT, uid, null);
        }
        catch(UnknownUidException e){}
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME, "create session");
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute,privileges), null);
    }
    
    /**
     * Deletes test user
     */
    @After
    public void deleteTestUser(){
        facade.delete(ObjectClass.ACCOUNT, uid, null);
    }
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleOperationUpdate#update(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)}.
     */
    @Test
    public void testUpdatePassword() {
        password = new GuardedString("newPassword".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(passwordAttribute),null);
        //now try to authenticate
        facade.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
    }
    
    /** Test updating authentication 
     * @throws SQLException */
    @Test
    public void testUpdateAuthentication() throws SQLException{
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_EXTERNAL);
        facade.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(authentication), null);
        try{
            facade.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
            fail("Authenticate must fail for external authentication");
        }
        catch(RuntimeException e){}
        /* Not enabled for XE
        authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_GLOBAL);
        Attribute globalName = AttributeBuilder.build(OracleConnector.ORACLE_GLOBAL_ATTR_NAME,"myGlobalName");
        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(authentication,globalName), null);
        final UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
        assertEquals("myGlobalName", record.externalName);
        */
    }
    
    /** Test update of profile 
     * @throws SQLException */
    @Test
    public void testUpdateProfile() throws SQLException{
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
        Attribute profileAttr = AttributeBuilder.build(OracleConnector.ORACLE_PROFILE_ATTR_NAME, profileName);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(profileAttr), null);
        final UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
        assertEquals(testConf.getCSSetup().normalizeToken(PROFILE,profileName), record.getProfile());
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
    }
    
    
    @Test
    public void testUpdateDefTSQuota() throws SQLException{
		Attribute defaultTsQuotaAttr =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"30k");
		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(defaultTsQuotaAttr), null);
        Long quota = userReader.readUserDefTSQuota(uid.getUidValue());
        Assert.assertTrue("Quota must be at least 30k",new Long(30000).compareTo(quota) < 0);
        defaultTsQuotaAttr =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"-1");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(defaultTsQuotaAttr), null);
        quota = userReader.readUserDefTSQuota(uid.getUidValue());
        Assert.assertEquals("Quota must be set to -1 for unlimited quota", new Long(-1), quota);
        
    }
    
    
    @Test
    public void testUpdateDefTS() throws SQLException{
    	//Test that update to same ts works
    	String defaultTableSpace = userReader.readUserRecord(uid.getUidValue()).getDefaultTableSpace();
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME, defaultTableSpace)), null);
        //Now try to update with other tablespaces, if update is successfull, check whether it is correctly set
        for(String ts : findAllDefTS(connector.getAdminConnection())){
        	try{
        		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_ATTR_NAME, ts)), null);
        	}
        	catch(Exception e){
        		//For any reason , when tablespace cannot be used for user
        		continue;
        	}
        	assertEquals(ts, userReader.readUserRecord(uid.getUidValue()).getDefaultTableSpace());
        }
    }
    
    @Test
    public void testUpdateTempTS() throws SQLException{
    	//Test that update to same ts works
    	String tempTableSpace = userReader.readUserRecord(uid.getUidValue()).getTemporaryTableSpace();
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME, tempTableSpace)), null);
        //Now try to update with other tablespaces, if update is successfull, check whether it is correctly set
        for(String ts : findAllTempTS(connector.getAdminConnection())){
        	try{
        		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME, ts)), null);
        	}
        	catch(Exception e){
        		//For any reason , when tablespace cannot be used for user
        		continue;
        	}
        	assertEquals(ts, userReader.readUserRecord(uid.getUidValue()).getTemporaryTableSpace());
        }
    }
    
    @Test
    public void testUpdateRoles() throws SQLException{
        String role = "testrole";
        final OracleCaseSensitivitySetup cs = testConf.getCSSetup();
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role " + cs.normalizeAndFormatToken(OracleUserAttributeCS.ROLE, role));
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "create role " + cs.normalizeAndFormatToken(OracleUserAttributeCS.ROLE,role));
        Attribute roles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME, Arrays.asList(role));
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(roles), null);
        List<String> rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem(cs.normalizeToken(OracleUserAttributeCS.ROLE,role)));
        
        //If sending null or empty roles attribute, all roles must get revoked
        roles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(roles), null);
        rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat("All roles must get revoked when sending null roles attribute",rolesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        roles = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME,Collections.emptyList());
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(roles), null);
        rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat("All roles must get revoked when sending empty list roles attribute",rolesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role " + cs.normalizeAndFormatToken(OracleUserAttributeCS.ROLE, role));
}
    
	@Test
    public void testUpdatePrivileges() throws SQLException{
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE");
        }
        catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table mytable(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        List<String> privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        
        //If sending null or empty privileges attribute, all roles must get revoked
        privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat("All privileges must get revoked when sending null privileges attribute",privilegesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,Collections.emptyList());
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat("All privileges must get revoked when sending empty list privileges attribute",privilegesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table mytable");
    }
    
    @Test
    public void testUpdateEnable() throws SQLException{
    	//new created user will be enabled
    	UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
    	assertEquals("OPEN",record.getStatus());
    	Attribute enable = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.FALSE);
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(enable), null);
    	record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
    	assertNotNull(record);
    	assertEquals("LOCKED",record.getStatus());
    	enable = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(enable), null);
    	record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
    	assertNotNull(record);
    	assertEquals("OPEN",record.getStatus());
    }
    
    @Test
    public void testUpdateExpirePasword() throws SQLException{
    	Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(expirePassword), null);
    	UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
    	assertNotNull(record);
        assertEquals("EXPIRED",record.getStatus());
        
        //now unexpire
        expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.FALSE);
        try{
        	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(expirePassword), null);
        	fail("Must require password");
        }catch(RuntimeException e){
        }
        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(expirePassword,AttributeBuilder.buildPassword("newPassword".toCharArray())), null);
        record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
    	assertNotNull(record);
        assertEquals("OPEN",record.getStatus());
    }
    
    @Test
    public void testAddAttributeValuesPrivileges() throws SQLException{
    	try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table MYTABLE1(id number)");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table MYTABLE2(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE1");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        List<String> privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privilegesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("SELECT ON " + testConf.getUser() + ".MYTABLE2")));
        privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"SELECT ON MYTABLE2");
        facade.addAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE2"));
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE1");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE2");
    }
    
    @Test
    public void testAddAttributeValuesRoles() throws SQLException{
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role ROLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role ROLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create role ROLE1");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create role ROLE2");
        Attribute rolesAttr = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME,"ROLE1");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        List<String> rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE1"));
        Assert.assertThat(rolesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("ROLE2")));
        rolesAttr = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME,"ROLE2");
        facade.addAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE1"));
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE2"));
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop role role1");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop role role2");
    }
    
    @Test
    public void testRemoveAttributeValuesPrivileges() throws SQLException{
    	try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table MYTABLE1(id number)");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create table MYTABLE2(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE1","SELECT ON MYTABLE2");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        List<String> privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE2"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        privileges = AttributeBuilder.build(OracleConnector.ORACLE_PRIVS_ATTR_NAME,"SELECT ON MYTABLE1");
        facade.removeAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE2"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privilegesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1")));
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE1");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop table MYTABLE2");
    	
    }
    
    @Test
    public void testRemoveAttributeValuesRoles() throws SQLException{
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role ROLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop role ROLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create role ROLE1");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"create role ROLE2");
        Attribute rolesAttr = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME,"ROLE1","ROLE2");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        List<String> rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE1"));
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE2"));
        rolesAttr = AttributeBuilder.build(OracleConnector.ORACLE_ROLES_ATTR_NAME,"ROLE1");
        facade.removeAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        rolesRead = new OracleRolePrivReader(connector.getAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE2"));
        Assert.assertThat(rolesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("ROLE1")));
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop role role1");
        SQLUtil.executeUpdateStatement(connector.getAdminConnection(),"drop role role2");
    }
    
    @Test
    public void testUpdateUserExternal() throws SQLException{
        //Test external authentication
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConnector.ORACLE_AUTH_EXTERNAL)), null);
        UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
    	assertNotNull(record);
        assertEquals("OPEN",record.getStatus());
        assertEquals("EXTERNAL",record.getPassword());
    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
				.newSet(AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConnector.ORACLE_AUTH_LOCAL),
						AttributeBuilder.buildPassword("password".toCharArray())
						),
				null);
    }
    
    
    @Test
    public void testUpdateUserGlobal() throws SQLException{
        //Test external authentication
    	try{
    		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConnector.ORACLE_AUTH_GLOBAL)), null);
    		fail("Must require global name");
    	}catch(RuntimeException e){}
    	try{
	    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(
							OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME,
							OracleConnector.ORACLE_AUTH_GLOBAL),
							AttributeBuilder.build(OracleConnector.ORACLE_GLOBAL_ATTR_NAME,"anyGlobal")
					), null);
	        UserRecord record = new OracleUserReader(connector.getAdminConnection()).readUserRecord(uid.getUidValue());
	    	assertNotNull(record);
	        assertEquals("OPEN",record.getStatus());
	        assertNotNull(record.getExternalName());
	    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConnector.ORACLE_AUTH_LOCAL),
							AttributeBuilder.buildPassword("password".toCharArray())
							),
					null);
	    	
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
    }
    
    
    

    
    
    
    
    
    

}
