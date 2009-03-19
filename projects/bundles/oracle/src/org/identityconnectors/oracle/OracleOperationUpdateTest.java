/**
 * 
 */
package org.identityconnectors.oracle;


import java.sql.SQLException;
import java.util.Collections;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.junit.*;
import static org.identityconnectors.oracle.OracleUserAttribute.*;
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
        assertEquals(testConf.getCSSetup().normalizeToken(PROFILE,profileName), record.profile);
        try{
            SQLUtil.executeUpdateStatement(connector.getAdminConnection(), "drop profile " + testConf.getCSSetup().normalizeAndFormatToken(PROFILE,profileName));
        }
        catch(SQLException e){
        }
    }
    
    
    @Test
    public void testDefTSQuota() throws SQLException{
		Attribute defaultTsQuotaAttr =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME,"30k");
		facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(defaultTsQuotaAttr), null);
        Long quota = userReader.readUserDefTSQuota(uid.getUidValue());
        Assert.assertTrue("Quota must be at least 30k",new Long(30000).compareTo(quota) < 0);
        defaultTsQuotaAttr =  AttributeBuilder.build(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(defaultTsQuotaAttr), null);
        quota = userReader.readUserDefTSQuota(uid.getUidValue());
        Assert.assertEquals("Quota must be set to -1 for unlimited quota", new Long(-1), quota);
        
    }
    
    
    @Test
    public void testUpdateDefTS() throws SQLException{
    	//Test that update to same ts works
    	String defaultTableSpace = userReader.readUserRecord(uid.getUidValue()).defaultTableSpace;
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
        	assertEquals(ts, userReader.readUserRecord(uid.getUidValue()).defaultTableSpace);
        }
    }
    
    @Test
    public void testUpdateTempTS() throws SQLException{
    	//Test that update to same ts works
    	String tempTableSpace = userReader.readUserRecord(uid.getUidValue()).temporaryTableSpace;
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
        	assertEquals(ts, userReader.readUserRecord(uid.getUidValue()).temporaryTableSpace);
        }
    }
    
    

    
    
    
    
    
    

}
