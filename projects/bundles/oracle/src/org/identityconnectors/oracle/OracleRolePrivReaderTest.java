package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import java.sql.*;
import java.util.List;

import org.identityconnectors.dbcommon.SQLUtil;
import org.junit.*;

/**
 * Test for OracleRolePrivReader
 * @author kitko
 *
 */
public class OracleRolePrivReaderTest {
    private static Connection conn;
    private static OracleRolePrivReader privReader;
    private static OracleUserReader userReader;
    
    /**
     * Setup connection
     */
    @BeforeClass
    public static void beforeClass(){
        final OracleConfiguration cfg = OracleConfigurationTest.createSystemConfiguration();
        conn = cfg.createAdminConnection();
        privReader = new OracleRolePrivReader(conn);
        userReader = new OracleUserReader(conn);
    }

    /** Test reading user roles 
     * @throws SQLException */
    @Test
    public void testReadRoles() throws SQLException{
        String user = "user1";
        if(!userReader.userExist(user)){
            SQLUtil.executeUpdateStatement(conn,"create user \"" + user + "\" identified by password");
        }
        try{
            SQLUtil.executeUpdateStatement(conn, "drop role \"testrole\"");
        }catch(SQLException e){}
        SQLUtil.executeUpdateStatement(conn, "create role \"testrole\"");
        SQLUtil.executeUpdateStatement(conn, "grant \"testrole\" to \"" + user + "\"");
        final List<String> roles = privReader.readRoles(user);
        assertTrue("User should be granteded testrole",roles.contains("testrole"));
        SQLUtil.rollbackQuietly(conn);
        
    }

    /** Test reading user privileges */
    @Test
    public void testReadPrivileges() {
    }

}
