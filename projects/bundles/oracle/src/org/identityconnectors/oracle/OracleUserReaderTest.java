/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;
import static org.identityconnectors.oracle.OracleConnectorAbstractTest.*;

import java.sql.*;
import java.util.*;
import static org.identityconnectors.oracle.OracleUserAttributeCS.*;

import org.identityconnectors.dbcommon.SQLUtil;
import org.junit.*;

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
        userReader = new OracleUserReader(conn);
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
        final String formatUserName = cfg.getCSSetup().formatToken(USER_NAME, user);
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

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorHelper#readUserRecords(java.sql.Connection, java.util.List)}.
     * @throws SQLException 
     */
    @Test
    public void testReadUserRecords() throws SQLException {
        final OracleCaseSensitivitySetup cs = cfg.getCSSetup();
        try{
        	SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER_NAME,"user1"));
        }catch(SQLException e){}
        try{
        	SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER_NAME,"user2"));
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(conn,"create user " + cs.formatToken(USER_NAME,"user1") + " identified by password");
        SQLUtil.executeUpdateStatement(conn,"create user " + cs.formatToken(USER_NAME,"user2") + " identified by password");
        final Collection<UserRecord> records = userReader.readUserRecords(Arrays.asList("user1","user2","user3"));
        assertEquals("Read should return 2 users",2,records.size());
        
        
        UserRecord record1 = userReader.readUserRecord("user1");
        assertNotNull(record1);
        assertEqualsIgnoreCase("user1",record1.getUserName());
        assertNotNull(record1.getUserId());
        assertNotNull(record1.getDefaultTableSpace());
        assertNotNull(record1.getTemporaryTableSpace());
        assertNotNull(record1.getCreatedDate());
        assertNull(record1.getLockDate());
        assertEquals("OPEN",record1.getStatus());
        
        SQLUtil.executeUpdateStatement(conn,"alter user " + cs.formatToken(USER_NAME,"user1") + " password expire account lock");
        record1 = userReader.readUserRecord("user1");
        assertNotNull(record1);
        assertNotNull(record1.getExpireDate());
        assertNotNull(record1.getLockDate());
        assertEquals("EXPIRED & LOCKED",record1.getStatus());
        
        SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER_NAME,"user1"));
        SQLUtil.executeUpdateStatement(conn,"drop user " + cs.formatToken(USER_NAME,"user2"));
    }
    
    @Test
    public void testReadUserQuota() throws SQLException{
    	final OracleCaseSensitivitySetup cs = cfg.getCSSetup();
    	String user = "user1";
        if(!userReader.userExist(cs.normalizeToken(USER_NAME,user))){
            SQLUtil.executeUpdateStatement(conn,"create user " + cs.normalizeAndFormatToken(USER_NAME,user) + " identified by password");
        }
        UserRecord readUserRecord = userReader.readUserRecord(cs.normalizeToken(USER_NAME,user));
        SQLUtil.executeUpdateStatement(conn, "alter user " + cs.normalizeAndFormatToken(USER_NAME,user) + " quota 30k on " + readUserRecord.getDefaultTableSpace());
        Long quota = userReader.readUserDefTSQuota(cs.normalizeToken(USER_NAME,user));
        assertTrue("Quota must be set at least to 30k",new Long(30000).compareTo(quota) < 0);
        //For 10.2 , not working
        //quota = userReader.readUserTempTSQuota(cs.normalizeToken(USER_NAME,user));
        SQLUtil.executeUpdateStatement(conn,"drop user " + cs.normalizeAndFormatToken(USER_NAME,"user1"));
    }


}
