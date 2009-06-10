/**
 * 
 */
package org.identityconnectors.oracle;


import static org.identityconnectors.oracle.OracleUserAttribute.PROFILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.UpdateOp;
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
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(TEST_USER);
        password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME, "create session");
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
        
        //If password is present, for external auth we must fail
    	try{
	        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_EXTERNAL),
							AttributeBuilder.buildPassword("password".toCharArray())
							),
					null);
	        fail("Update must fail with external authntication and password");
    	}catch(ConnectorException e){}
    	
    	//Now set ignore extra attributes
        OracleConfiguration newConf = OracleConfigurationTest.createSystemConfiguration();
        newConf.validate();
        newConf
				.setExtraAttributesPolicySetup(new ExtraAttributesPolicySetupBuilder(
						TestHelpers.createDummyMessages()).definePolicy(
						OracleUserAttribute.PASSWORD, UpdateOp.class,
						ExtraAttributesPolicy.IGNORE).build());
        OracleConnector testConnector = new OracleConnector();
        testConnector.init(newConf);
        testConnector.update(ObjectClass.ACCOUNT, uid, CollectionUtil
				.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_EXTERNAL),
						AttributeBuilder.buildPassword("password".toCharArray())
						),
				null);
        testConnector.dispose();
        
    }
    
    /** Test updating authentication 
     * @throws SQLException */
    @Test
    public void testUpdateAuthentication() throws SQLException{
    	//Update to local
    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL)), null);
    	UserRecord record = userReader.readUserRecord(uid.getUidValue());
    	assertEquals(OracleAuthentication.LOCAL, OracleUserReader.resolveAuthentication(record));

    	//Update to external
    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_EXTERNAL)), null);
    	record = userReader.readUserRecord(uid.getUidValue());
    	assertEquals(OracleAuthentication.EXTERNAL, OracleUserReader.resolveAuthentication(record));
    	
    	//Update to global
    	try{
	    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(
							OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,
							OracleConstants.ORACLE_AUTH_GLOBAL),
							AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"anyGlobal")
					), null);
	    	record = userReader.readUserRecord(uid.getUidValue());
	    	assertEquals(OracleAuthentication.GLOBAL, OracleUserReader.resolveAuthentication(record));
	    	assertEquals("anyGlobal", record.getExternalName());
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
    
    /** Test update of profile 
     * @throws SQLException */
    @Test
    public void testUpdateProfile() throws SQLException{
        String profileName = "myProfile";
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "create profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName) + "limit password_lock_time 6");
            connector.getOrCreateAdminConnection().commit();
        }
        catch(SQLException e){
            fail(e.getMessage());
        }
        Attribute profileAttr = AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME, profileName);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(profileAttr), null);
        final UserRecord record = userReader.readUserRecord(uid.getUidValue());
        assertEquals(testConf.getCSSetup().normalizeToken(PROFILE,profileName), record.getProfile());
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
    }
    
    
    @Test
    public void testUpdateDefTSQuota() throws SQLException{
		Attribute defaultTsQuotaAttr =  AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"30k");
		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(defaultTsQuotaAttr), null);
        Long quota = userReader.readUserDefTSQuota(uid.getUidValue());
        Assert.assertTrue("Quota must be at least 30k",new Long(30000).compareTo(quota) < 0);
        defaultTsQuotaAttr =  AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"-1");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(defaultTsQuotaAttr), null);
        quota = userReader.readUserDefTSQuota(uid.getUidValue());
        Assert.assertEquals("Quota must be set to -1 for unlimited quota", new Long(-1), quota);
        
    }
    
    
    @Test
    public void testUpdateDefTS() throws SQLException{
    	//Test that update to same ts works
    	String defaultTableSpace = userReader.readUserRecord(uid.getUidValue()).getDefaultTableSpace();
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME, defaultTableSpace)), null);
        //Now try to update with other tablespaces, if update is successfull, check whether it is correctly set
        for(String ts : findAllDefTS(connector.getOrCreateAdminConnection())){
        	try{
        		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME, ts)), null);
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
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, tempTableSpace)), null);
        //Now try to update with other tablespaces, if update is successfull, check whether it is correctly set
        for(String ts : findAllTempTS(connector.getOrCreateAdminConnection())){
        	try{
        		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, ts)), null);
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
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop role " + cs.normalizeAndFormatToken(OracleUserAttribute.ROLE, role));
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "create role " + cs.normalizeAndFormatToken(OracleUserAttribute.ROLE,role));
        Attribute roles = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME, Arrays.asList(role));
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(roles), null);
        List<String> rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem(cs.normalizeToken(OracleUserAttribute.ROLE,role)));
        
        //If sending null or empty roles attribute, all roles must get revoked
        roles = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(roles), null);
        rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat("All roles must get revoked when sending null roles attribute",rolesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        roles = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,Collections.emptyList());
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(roles), null);
        rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat("All roles must get revoked when sending empty list roles attribute",rolesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop role " + cs.normalizeAndFormatToken(OracleUserAttribute.ROLE, role));
}
    
	@Test
    public void testUpdatePrivileges() throws SQLException{
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE");
        }
        catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create table mytable(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        List<String> privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        
        //If sending null or empty privileges attribute, all roles must get revoked
        privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat("All privileges must get revoked when sending null privileges attribute",privilegesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,Collections.emptyList());
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat("All privileges must get revoked when sending empty list privileges attribute",privilegesRead, new IsEqual<List<String>>(Collections.<String>emptyList()));
        
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table mytable");
    }
    
    @Test
    public void testUpdateEnable() throws SQLException{
    	//new created user will be enabled
    	UserRecord record = userReader.readUserRecord(uid.getUidValue());
    	assertEquals("OPEN",record.getStatus());
    	Attribute enable = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.FALSE);
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(enable), null);
    	record = userReader.readUserRecord(uid.getUidValue());
    	assertNotNull(record);
    	assertEquals("LOCKED",record.getStatus());
    	enable = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(enable), null);
    	record = userReader.readUserRecord(uid.getUidValue());
    	assertNotNull(record);
    	assertEquals("OPEN",record.getStatus());
    }
    
    @Test
    public void testUpdateExpirePasword() throws SQLException{
    	Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(expirePassword), null);
    	UserRecord record = userReader.readUserRecord(uid.getUidValue());
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
        record = userReader.readUserRecord(uid.getUidValue());
    	assertNotNull(record);
        assertEquals("OPEN",record.getStatus());
        
        //expire must fail for not local authentication
        //Update to external
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_EXTERNAL)), null);
        try{
        	//Try expire
	        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
	        fail("Update with expiredPassword for not local authentication must fail");
        }catch(RuntimeException e){
        	if(e.getCause() instanceof SQLException){
        		fail("Update with expiredPassword for not local authentication should not fail with SQLException");
        	}
        }
        //Update back to local
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_LOCAL)), null);
        try{
        	//Try update to external with expire in one call
	        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(
					AttributeBuilder.buildPasswordExpired(true),
					AttributeBuilder.build(
							OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,
							OracleConstants.ORACLE_AUTH_EXTERNAL)), null);
	        fail("Update with expiredPassword for not local authentication must fail");
        }catch(RuntimeException e){
        	if(e.getCause() instanceof SQLException){
        		fail("Update with expiredPassword for not local authentication should not fail with SQLException");
        	}
        }
    	
    	//Now set ignore extra attributes
        OracleConfiguration newConf = OracleConfigurationTest.createSystemConfiguration();
        newConf.validate();
        newConf
				.setExtraAttributesPolicySetup(new ExtraAttributesPolicySetupBuilder(
						TestHelpers.createDummyMessages()).definePolicy(
						OracleUserAttribute.PASSWORD, UpdateOp.class,
						ExtraAttributesPolicy.IGNORE).build());
        OracleConnector testConnector = new OracleConnector();
        testConnector.init(newConf);
        testConnector.update(ObjectClass.ACCOUNT, uid, CollectionUtil
				.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_EXTERNAL),
						AttributeBuilder.buildPassword("password".toCharArray())
						),
				null);
        testConnector.dispose();
        
    }
    
    @Test
    public void testAddAttributeValuesPrivileges() throws SQLException{
    	try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create table MYTABLE1(id number)");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create table MYTABLE2(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE1");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        List<String> privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privilegesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("SELECT ON " + testConf.getUser() + ".MYTABLE2")));
        privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"SELECT ON MYTABLE2");
        facade.addAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE2"));
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE1");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE2");
    }
    
    @Test
    public void testAddAttributeValuesRoles() throws SQLException{
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop role ROLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop role ROLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create role ROLE1");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create role ROLE2");
        Attribute rolesAttr = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"ROLE1");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        List<String> rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE1"));
        Assert.assertThat(rolesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("ROLE2")));
        rolesAttr = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"ROLE2");
        facade.addAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE1"));
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE2"));
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop role role1");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop role role2");
    }
    
    @Test
    public void testRemoveAttributeValuesPrivileges() throws SQLException{
    	try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create table MYTABLE1(id number)");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create table MYTABLE2(id number)");
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","SELECT ON MYTABLE1","SELECT ON MYTABLE2");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        List<String> privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE2"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"SELECT ON MYTABLE1");
        facade.removeAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(privileges), null);
        privilegesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readPrivileges(uid.getUidValue());
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE2"));
        Assert.assertThat(privilegesRead, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(privilegesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("SELECT ON " + testConf.getUserOwner() + ".MYTABLE1")));
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE1");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop table MYTABLE2");
    	
    }
    
    @Test
    public void testRemoveAttributeValuesRoles() throws SQLException{
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop role ROLE1");
        }catch(SQLException e){}
        try{
            SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(), "drop role ROLE2");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create role ROLE1");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"create role ROLE2");
        Attribute rolesAttr = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"ROLE1","ROLE2");
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        List<String> rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE1"));
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE2"));
        rolesAttr = AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"ROLE1");
        facade.removeAttributeValues(ObjectClass.ACCOUNT, uid, Collections.singleton(rolesAttr), null);
        rolesRead = new OracleRolePrivReader(connector.getOrCreateAdminConnection()).readRoles(uid.getUidValue());
        Assert.assertThat(rolesRead, JUnitMatchers.hasItem("ROLE2"));
        Assert.assertThat(rolesRead, new IsNot<Iterable<String>>(JUnitMatchers.hasItem("ROLE1")));
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop role role1");
        SQLUtil.executeUpdateStatement(connector.getOrCreateAdminConnection(),"drop role role2");
    }
    
    @Test
    public void testUpdateExternal() throws SQLException{
        //Test external authentication
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_EXTERNAL)), null);
        UserRecord record = userReader.readUserRecord(uid.getUidValue());
    	assertNotNull(record);
    	assertEquals(OracleAuthentication.EXTERNAL, OracleUserReader.resolveAuthentication(record));
    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
				.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_LOCAL),
						AttributeBuilder.buildPassword("password".toCharArray())
						),
				null);
    	record = userReader.readUserRecord(uid.getUidValue());
    	assertNotNull(record);
        assertEquals(OracleAuthentication.LOCAL, OracleUserReader.resolveAuthentication(record));
    }
    
    
    @Test
    public void testUpdateUserGlobal() throws SQLException{
    	try{
    		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_GLOBAL)), null);
    		fail("Must require global name");
    	}catch(RuntimeException e){}
    	try{
    		facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,""),AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_GLOBAL)), null);
    		fail("Global name cannot be blank");
    	}catch(RuntimeException e){}
    	try{
    		facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME),AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_GLOBAL)), null);
    		fail("Global name cannot be null");
    	}catch(RuntimeException e){}
    	try{
	    	facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(
							OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,
							OracleConstants.ORACLE_AUTH_GLOBAL),
							AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"anyGlobal")
					), null);
	        UserRecord record = userReader.readUserRecord(uid.getUidValue());
	    	assertNotNull(record);
	        assertEquals(OracleAuthentication.GLOBAL, OracleUserReader.resolveAuthentication(record));
	        assertEquals("anyGlobal",record.getExternalName());

	    	//update to newGlobal and check it
	        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"newGlobal")
					), null);
	    	record = userReader.readUserRecord(uid.getUidValue());
	    	assertNotNull(record);
	        assertEquals(OracleAuthentication.GLOBAL, OracleUserReader.resolveAuthentication(record));
	        assertEquals("newGlobal",record.getExternalName());
	        
	    	//Set back to local and check it
	        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
					.newSet(AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME,OracleConstants.ORACLE_AUTH_LOCAL),
							AttributeBuilder.buildPassword("password".toCharArray())
							),
					null);
	    	record = userReader.readUserRecord(uid.getUidValue());
	    	assertNotNull(record);
	        assertEquals(OracleAuthentication.LOCAL, OracleUserReader.resolveAuthentication(record));
	        //Now try to set Global name, which should fail
	        try{
		        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil
						.newSet(AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"newGlobal")
						), null);
		        fail("Update with global name must fail for local authentication");
	        }catch(ConnectorException e){}
	        
	    	
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
    
    
	/** Test that update will fail on any not known attribute */
	@Test
	public void testValidUpdateAttributes(){
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        GuardedString password = new GuardedString("hello".toCharArray());
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION");
        Attribute enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,Boolean.TRUE);
        Attribute expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME,Boolean.TRUE);
        Set<Attribute> attrs = new HashSet<Attribute>(Arrays.asList(authentication,passwordAttribute,
        		privileges,enabled,expirePassword));
		facade.update(ObjectClass.ACCOUNT,uid, attrs, null);
		//Now add some dummy attribute
		Set<Attribute> badAttrs = new HashSet<Attribute>(attrs); 
		badAttrs.add(AttributeBuilder.buildDisableDate(0));
		try{
			facade.update(ObjectClass.ACCOUNT,uid, badAttrs, null);
			fail("Disabled date should not be supported");
		}
		catch(IllegalArgumentException e){}
		badAttrs = new HashSet<Attribute>(attrs); 
		badAttrs.add(AttributeBuilder.buildPasswordExpirationDate(0));
		try{
			facade.update(ObjectClass.ACCOUNT,uid, badAttrs, null);
			fail("Expired password date should not be supported");
		}
		catch(IllegalArgumentException e){}
		badAttrs = new HashSet<Attribute>(attrs); 
		badAttrs.add(AttributeBuilder.build("dummy","dummyValue"));
		try{
			facade.update(ObjectClass.ACCOUNT,uid, badAttrs, null);
			fail("Dummy attribute should not be supported");
		}
		catch(IllegalArgumentException e){}
		
		//Call with no attributes, this must fail
		try{
			facade.update(ObjectClass.ACCOUNT,uid, Collections.<Attribute>emptySet(), null);
			fail("Update with no attributes should fail");
		}
		catch(IllegalArgumentException e){}
		
		try{
			facade.addAttributeValues(ObjectClass.ACCOUNT,uid, Collections.<Attribute>emptySet(), null);
			fail("AddAttributeValues with no attributes should fail");
		}
		catch(IllegalArgumentException e){}

		try{
			facade.removeAttributeValues(ObjectClass.ACCOUNT,uid, Collections.<Attribute>emptySet(), null);
			fail("RemoveAttributeValues with no attributes should fail");
		}
		catch(IllegalArgumentException e){}
		
		//try other case of attributes
        authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME.toUpperCase(), OracleConstants.ORACLE_AUTH_LOCAL);
        password = new GuardedString("hello".toCharArray());
        passwordAttribute = AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME.toUpperCase(),password);
        privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME.toUpperCase(),"CREATE SESSION");
        enabled = AttributeBuilder.build(OperationalAttributes.ENABLE_NAME.toUpperCase(),Boolean.TRUE);
        expirePassword = AttributeBuilder.build(OperationalAttributes.PASSWORD_EXPIRED_NAME.toUpperCase(),Boolean.TRUE);
        attrs = new HashSet<Attribute>(Arrays.asList(authentication,passwordAttribute,
        		privileges,enabled,expirePassword));

        facade.update(ObjectClass.ACCOUNT,uid, attrs, null);
        //We do not need to verify update effect, we test that invalid attributes are rejected 
	}
	
	/** Test that update will fail for sql error, e.g killed connection 
	 * @throws SQLException */ 
	@Test
	public void testUpdateFail() throws SQLException{
		OracleConnectorImpl testConnector = createTestConnector();
		testConnector.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildPassword("newpassword".toCharArray())) , null);
		//Now kill connection
		OracleSpecificsTest.killConnection(connector.getOrCreateAdminConnection(), testConnector.getOrCreateAdminConnection());
		//now update should fail
		try{
			testConnector.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildPassword("newpassword".toCharArray())) , null);
			fail("Update must fail for killed connection");
		}catch(RuntimeException e){
		}
		testConnector.dispose();
	}

    
    /** Test setting lockout parameter 
     * @throws SQLException */
	@Test
    public void testUpdateLockOut() throws SQLException{
    	assertEquals("OPEN",userReader.readUserRecord(uid.getUidValue()).getStatus());
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildLockOut(true)), null);
    	assertEquals("LOCKED",userReader.readUserRecord(uid.getUidValue()).getStatus());
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildPassword("newPassword".toCharArray())), null);
    	assertEquals("LOCKED",userReader.readUserRecord(uid.getUidValue()).getStatus());
    	facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildLockOut(false)), null);
    	assertEquals("OPEN",userReader.readUserRecord(uid.getUidValue()).getStatus());
    }
    
    
    
    

}
