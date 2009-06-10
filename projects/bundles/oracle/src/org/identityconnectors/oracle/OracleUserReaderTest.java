/**
 * 
 */
package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnectorAbstractTest.assertEqualsIgnoreCase;
import static org.identityconnectors.oracle.OracleUserAttribute.USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests related to OracleUserReader
 * @author kitko
 *
 */
public class OracleUserReaderTest {
    private static Connection conn;
    private static OracleUserReader userReader;
    private static OracleConfiguration cfg;
    
    /**
     * Setup connection
     */
    @BeforeClass
    public static void beforeClass(){
        cfg = OracleConfigurationTest.createSystemConfiguration();
        conn = cfg.createAdminConnection();
        userReader = new OracleUserReader(conn,TestHelpers.createDummyMessages());
    }
    
    /**
     * Close connection 
     */
    @AfterClass
    public static void afterClass(){
        SQLUtil.closeQuietly(conn);
    }
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorHelper#userExist(java.sql.Connection, java.lang.String)}.
     * @throws SQLException 
     */
    @Test
    public void testUserExist() throws SQLException {
        String user = "testUser";
        boolean userExist = userReader.userExist(user);
        final String formatUserName = cfg.getCSSetup().formatToken(USER, user);
        if(userExist){
            SQLUtil.executeUpdateStatement(conn, "drop user " + formatUserName + " cascade");
            assertFalse("User should not exist after delete", userReader.userExist(user));
        }
        else{
            SQLUtil.executeUpdateStatement(conn,"create user " + formatUserName + " identified by password");
            assertTrue("User should exist after create", userReader.userExist(user));
            SQLUtil.executeUpdateStatement(conn, "drop user " + formatUserName + " cascade");
            assertFalse("User should not exist after delete", userReader.userExist(user));
        }
    }

    @Test
    public void testReadUserRecord() throws SQLException {
        final OracleCaseSensitivitySetup cs = cfg.getCSSetup();
        try{
        	SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER,"user1"));
        }catch(SQLException e){}
        try{
        	SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER,"user2"));
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(conn,"create user " + cs.formatToken(USER,"user1") + " identified by password");
        SQLUtil.executeUpdateStatement(conn,"create user " + cs.formatToken(USER,"user2") + " identified by password");
        
        
        UserRecord record1 = userReader.readUserRecord("user1");
        assertNotNull(record1);
        assertEqualsIgnoreCase("user1",record1.getUserName());
        assertNotNull(record1.getUserId());
        assertNotNull(record1.getDefaultTableSpace());
        assertNotNull(record1.getTemporaryTableSpace());
        assertNotNull(record1.getCreatedDate());
        assertNull(record1.getLockDate());
        assertEquals("OPEN",record1.getStatus());
        
        SQLUtil.executeUpdateStatement(conn,"alter user " + cs.formatToken(USER,"user1") + " password expire account lock");
        record1 = userReader.readUserRecord("user1");
        assertNotNull(record1);
        assertNotNull(record1.getExpireDate());
        assertNotNull(record1.getLockDate());
        assertEquals("EXPIRED & LOCKED",record1.getStatus());
        
        SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER,"user1"));
        SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER,"user2"));
        assertNull(userReader.readUserRecord("dummyUser"));
    }
    
    @Test
    public void testReadUserQuota() throws SQLException{
    	final OracleCaseSensitivitySetup cs = cfg.getCSSetup();
    	String user = "user1";
        if(!userReader.userExist(cs.normalizeToken(USER,user))){
            SQLUtil.executeUpdateStatement(conn,"create user " + cs.normalizeAndFormatToken(USER,user) + " identified by password");
        }
        UserRecord readUserRecord = userReader.readUserRecord(cs.normalizeToken(USER,user));
        SQLUtil.executeUpdateStatement(conn, "alter user " + cs.normalizeAndFormatToken(USER,user) + " quota 30k on " + readUserRecord.getDefaultTableSpace());
        Long quota = userReader.readUserDefTSQuota(cs.normalizeToken(USER,user));
        assertTrue("Quota must be set at least to 30k",new Long(30000).compareTo(quota) < 0);
        try{
        	userReader.readUserDefTSQuota("dummyUser");
        	fail("Should not be able to return quota for unknown user");
        }
        catch(IllegalArgumentException e){}
        //For 10.2 , not working
        //quota = userReader.readUserTempTSQuota(cs.normalizeToken(USER_NAME,user));
        SQLUtil.executeUpdateStatement(conn,"drop user " + cs.normalizeAndFormatToken(USER,"user1"));
    }


}
