/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;
import static org.identityconnectors.oracle.OracleConnectorAbstractTest.*;

import java.sql.*;
import java.util.*;
import static org.identityconnectors.oracle.OracleUserAttribute.*;

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
        if(!userReader.userExist("user1")){
            SQLUtil.executeUpdateStatement(conn,"create user " + cs.formatToken(USER_NAME,"user1") + " identified by password");
        }
        if(!userReader.userExist("user2")){
            SQLUtil.executeUpdateStatement(conn,"create user " + cs.formatToken(USER_NAME,"user2") + " identified by password");
        }
        final Collection<UserRecord> records = userReader.readUserRecords(Arrays.asList("user1","user2","user3"));
        assertEquals("Read should return 2 users",2,records.size());
        
        
        UserRecord record1 = userReader.readUserRecord("user1");
        assertNotNull(record1);
        assertEqualsIgnoreCase("user1",record1.userName);
        assertNotNull(record1.userId);
        assertNotNull(record1.defaultTableSpace);
        assertNotNull(record1.temporaryTableSpace);
        assertNotNull(record1.createdDate);
        assertEquals("OPEN",record1.status);
        
        SQLUtil.rollbackQuietly(conn);
        
        
    }


}
