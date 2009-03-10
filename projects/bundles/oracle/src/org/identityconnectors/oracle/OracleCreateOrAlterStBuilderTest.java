/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.identityconnectors.common.security.GuardedString;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OracleCreateOrAlterStBuilderTest {

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleCreateOrAlterStBuilder#buildCreateUserSt(java.lang.StringBuilder, org.identityconnectors.oracle.CreateAlterAttributes)}.
     */
    @Test
    public void testCreateLocalUserSt() {
        try{
            createSensitiveBuilder().buildCreateUserSt(new CreateAlterAttributes());
            fail("Not enough info, create must fail");
        }catch(Exception e){}
        CreateAlterAttributes local = new CreateAlterAttributes();
        local.userName = "user1";
        try{
            createSensitiveBuilder().buildCreateUserSt(local);
            fail("Not enough info, create must fail");
        }catch(Exception e){}
        local.auth = OracleAuthentication.LOCAL;        
        assertEquals("create user \"user1\" identified by \"user1\"", createSensitiveBuilder().buildCreateUserSt(local).toString());
        local.password = new GuardedString("password".toCharArray());
        assertEquals("create user \"user1\" identified by \"password\"", createSensitiveBuilder().buildCreateUserSt(local).toString());
        
    }
    
    /** Test create table space */
    @Test
    public void testCreateTableSpace(){
        CreateAlterAttributes attributes = new CreateAlterAttributes();
        attributes.userName = "user1";
        attributes.defaultTableSpace = "users";
        attributes.tempTableSpace = "temp";
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.password = new GuardedString("password".toCharArray());
        assertEquals("create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\"", createSensitiveBuilder().buildCreateUserSt(attributes).toString());
        attributes.profile = "MyProfile";
        assertEquals("create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" profile \"MyProfile\"", createSensitiveBuilder().buildCreateUserSt(attributes).toString());
    }
    
    /** Test quotas */
    @Test
    public void testCreateQuota(){
        CreateAlterAttributes attributes = new CreateAlterAttributes();
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.userName = "user1";
        attributes.defaultTableSpace = "users";
        attributes.tempTableSpace = "temp";
        attributes.password = new GuardedString("password".toCharArray());
        attributes.defaultTSQuota = new Quota();
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota unlimited on \"users\"",
                createSensitiveBuilder().buildCreateUserSt(attributes).toString());
        attributes.defaultTSQuota = new Quota("32K");
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota 32K on \"users\"",
                createSensitiveBuilder().buildCreateUserSt(attributes).toString());
        attributes.defaultTSQuota = null;
        attributes.tempTSQuota = new Quota();
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota unlimited on \"temp\"",
                createSensitiveBuilder().buildCreateUserSt(attributes).toString());
        attributes.tempTSQuota = new Quota("32M");
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota 32M on \"temp\"",
                createSensitiveBuilder().buildCreateUserSt(attributes).toString());
        
    }

    private OracleCreateOrAlterStBuilder createSensitiveBuilder() {
        return new OracleCreateOrAlterStBuilder(new OracleCaseSensitivityBuilder().build());
    }
    
    /** Test create external */
    @Test
    public void testCreateExternallUserSt() {
        CreateAlterAttributes external = new CreateAlterAttributes();
        external.userName = "user1";
        external.auth = OracleAuthentication.EXTERNAL;
        assertEquals("create user \"user1\" identified externally", createSensitiveBuilder().buildCreateUserSt(external).toString());
    }
    
    /** Test create global */
    @Test
    public void testCreateGlobalUserSt() {
        CreateAlterAttributes global = new CreateAlterAttributes();
        global.userName = "user1";
        global.auth = OracleAuthentication.GLOBAL;
        try{
            createSensitiveBuilder().buildCreateUserSt(global);
            fail("GlobalName should be missed");
        }catch(Exception e){}
        global.globalName = "global";
        assertEquals("create user \"user1\" identified globally as \"global\"", createSensitiveBuilder().buildCreateUserSt(global).toString());
    }
    
    /** Test expire and lock/unlock */
    @Test
    public void testCreateExpireAndLock() {
        CreateAlterAttributes attributes = new CreateAlterAttributes();
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.userName = "user1";
        attributes.expirePassword = true;
        attributes.enable = true;
        assertEquals("create user \"user1\" identified by \"user1\" password expire account unlock", createSensitiveBuilder().buildCreateUserSt(attributes).toString());
        attributes.expirePassword = false;
        assertEquals("create user \"user1\" identified by \"user1\" account unlock", createSensitiveBuilder().buildCreateUserSt(attributes).toString());
    }
    
    /** Test alter user */
    @Test
    public void testAlterUser() {
        CreateAlterAttributes attributes = new CreateAlterAttributes();
        attributes.auth = OracleAuthentication.LOCAL;
        attributes.userName = "user1";
        attributes.expirePassword = true;
        attributes.enable = true;
        attributes.defaultTSQuota = new Quota();
        UserRecord record = new UserRecord();
        record.defaultTableSpace = "users";
        assertEquals("alter user \"user1\" identified by \"user1\" quota unlimited on \"users\" password expire account unlock", createSensitiveBuilder().buildAlterUserSt(attributes, record).toString());
        
    }
    
    

}
