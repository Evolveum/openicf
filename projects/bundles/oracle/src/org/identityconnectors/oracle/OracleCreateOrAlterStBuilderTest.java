/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OracleCreateOrAlterStBuilderTest {

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleCreateOrAlterStBuilder#buildCreateUserSt(java.lang.StringBuilder, org.identityconnectors.oracle.OracleUserAttributes)}.
     */
    @Test
    public void testCreateLocalUserSt() {
        try{
            createDefaultBuilder().buildCreateUserSt(new OracleUserAttributes());
            fail("Not enough info, create must fail");
        }catch(Exception e){}
        OracleUserAttributes local = new OracleUserAttributes();
        local.userName = "user1";
        local.auth = OracleAuthentication.LOCAL;        
        assertEquals("create user \"user1\" identified by \"user1\"", createDefaultBuilder().buildCreateUserSt(local).toString());
        local.password = new GuardedString("password".toCharArray());
        assertEquals("create user \"user1\" identified by \"password\"", createDefaultBuilder().buildCreateUserSt(local).toString());
        
    }
    
    
    /** Test create table space */
    @Test
    public void testCreateTableSpace(){
        OracleUserAttributes attributes = new OracleUserAttributes();
        attributes.userName = "user1";
        attributes.defaultTableSpace = "users";
        attributes.tempTableSpace = "temp";
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.password = new GuardedString("password".toCharArray());
        assertEquals("create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\"", createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.profile = "MyProfile";
        assertEquals("create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" profile \"MyProfile\"", createDefaultBuilder().buildCreateUserSt(attributes).toString());
    }
    
    /** Test quotas */
    @Test
    public void testCreateQuota(){
        OracleUserAttributes attributes = new OracleUserAttributes();
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.userName = "user1";
        attributes.defaultTableSpace = "users";
        attributes.tempTableSpace = "temp";
        attributes.password = new GuardedString("password".toCharArray());
        attributes.defaultTSQuota = "-1";
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota unlimited on \"users\"",
                createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.defaultTSQuota = "32K";
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota 32K on \"users\"",
                createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.defaultTSQuota = null;
        attributes.tempTSQuota = "-1";
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota unlimited on \"temp\"",
                createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.tempTSQuota = "32M";
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota 32M on \"temp\"",
                createDefaultBuilder().buildCreateUserSt(attributes).toString());
        
    }

    private OracleCreateOrAlterStBuilder createDefaultBuilder() {
        return new OracleCreateOrAlterStBuilder(new OracleCaseSensitivityBuilder().build(),TestHelpers.createDummyMessages());
    }
    
    /** Test create external */
    @Test
    public void testCreateExternallUserSt() {
        OracleUserAttributes external = new OracleUserAttributes();
        external.userName = "user1";
        external.auth = OracleAuthentication.EXTERNAL;
        assertEquals("create user \"user1\" identified externally", createDefaultBuilder().buildCreateUserSt(external).toString());
    }
    
    /** Test create global */
    @Test
    public void testCreateGlobalUserSt() {
        OracleUserAttributes global = new OracleUserAttributes();
        global.userName = "user1";
        global.auth = OracleAuthentication.GLOBAL;
        try{
            createDefaultBuilder().buildCreateUserSt(global);
            fail("GlobalName should be missed");
        }catch(Exception e){}
        global.globalName = "global";
        assertEquals("create user \"user1\" identified globally as 'global'", createDefaultBuilder().buildCreateUserSt(global).toString());
    }
    
    /** Test expire password  */
    @Test
    public void testCreateExpirePassword() {
        OracleUserAttributes attributes = new OracleUserAttributes();
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.userName = "user1";
        attributes.expirePassword = true;
        assertEquals("create user \"user1\" identified by \"user1\" password expire", createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.expirePassword = false;
    	createDefaultBuilder().buildCreateUserSt(attributes);
        assertEquals("create user \"user1\" identified by \"user1\"", createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.expirePassword = null;
        assertEquals("create user \"user1\" identified by \"user1\"", createDefaultBuilder().buildCreateUserSt(attributes).toString());
    }
    
    /** Test  lock/unlock */
    @Test
    public void testCreateLock() {
        OracleUserAttributes attributes = new OracleUserAttributes();
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.userName = "user1";
        attributes.enable = true;
        assertEquals("create user \"user1\" identified by \"user1\" account unlock", createDefaultBuilder().buildCreateUserSt(attributes).toString());
        attributes.enable = false;
        assertEquals("create user \"user1\" identified by \"user1\" account lock", createDefaultBuilder().buildCreateUserSt(attributes).toString());
    }

    
    /** Test alter user */
    @Test
    public void testAlterUser() {
        OracleUserAttributes attributes = new OracleUserAttributes();
        attributes.auth = null;
        attributes.userName = "user1";
        attributes.expirePassword = true;
        attributes.enable = true;
        attributes.defaultTSQuota = "-1";
        UserRecord record = new UserRecord.Builder().setDefaultTableSpace("users").build();
        assertEquals("alter user \"user1\" quota unlimited on \"users\" password expire account unlock", createDefaultBuilder().buildAlterUserSt(attributes, record).toString());
        
        attributes.enable = null;
        attributes.expirePassword = null;
        attributes.tempTableSpace = "tempTS";
        record = new UserRecord.Builder().setDefaultTableSpace("defTS").build();
        assertEquals("alter user \"user1\" temporary tablespace \"tempTS\" quota unlimited on \"defTS\"", createDefaultBuilder().buildAlterUserSt(attributes, record).toString());
        
        attributes = new OracleUserAttributes();
        attributes.userName = "user1";
        attributes.expirePassword = true;
        assertEquals("alter user \"user1\" password expire", createDefaultBuilder().buildAlterUserSt(attributes, record).toString());
        
        
        attributes = new OracleUserAttributes();
        attributes.userName = "user1";
       	Assert.assertNull(createDefaultBuilder().buildAlterUserSt(attributes, record));
       	
       	//try to unexpire password. That means just set any new password
        attributes = new OracleUserAttributes();
        attributes.userName = "user1";
        attributes.expirePassword = false;
        attributes.password = new GuardedString("newPassword".toCharArray());
        assertEquals("alter user \"user1\" identified by \"newPassword\"", createDefaultBuilder().buildAlterUserSt(attributes, record).toString());
        attributes.password = null;
        try{
        	createDefaultBuilder().buildAlterUserSt(attributes, record);
        	fail("Must require password for unexpire");
        }catch(RuntimeException e){}
    }
    
    /** Test that builder properly formats tokens */
    @Test
    public void testCaseSensitivity(){
    	OracleCaseSensitivitySetup ocs = new OracleCaseSensitivityBuilder()
				.defineFormatters(
						CSTokenFormatter.build(OracleUserAttributeCS.USER_NAME,""),
						CSTokenFormatter.build(OracleUserAttributeCS.PROFILE,"'"))
						.build();
    	OracleCreateOrAlterStBuilder builder = new OracleCreateOrAlterStBuilder(ocs,TestHelpers.createDummyMessages());
        OracleUserAttributes attributes = new OracleUserAttributes();
        attributes.auth = null;
        attributes.userName = "user1";
        attributes.profile = "profile";
        Assert.assertEquals("create user user1 identified by \"user1\" profile 'profile'", builder.buildCreateUserSt(attributes));
        UserRecord record = new UserRecord.Builder().build();
        Assert.assertEquals("alter user user1 profile 'profile'", builder.buildAlterUserSt(attributes, record));

    }

}
