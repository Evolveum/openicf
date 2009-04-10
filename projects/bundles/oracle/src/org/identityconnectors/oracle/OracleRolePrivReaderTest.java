package org.identityconnectors.oracle;

import static org.junit.Assert.assertTrue;

import java.sql.*;
import java.util.List;


import org.identityconnectors.dbcommon.SQLUtil;
import org.junit.*;
import org.junit.matchers.JUnitMatchers;

/**
 * Test for OracleRolePrivReader
 * @author kitko
 *
 */
public class OracleRolePrivReaderTest {
    private static Connection conn;
    private static OracleRolePrivReader privReader;
    private static OracleUserReader userReader;
    private static OracleConfiguration cfg;
    
    /**
     * Setup connection
     */
    @BeforeClass
    public static void beforeClass(){
        cfg = OracleConfigurationTest.createSystemConfiguration();
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

    /** Test reading user privileges 
     * @throws SQLException */
	@Test
    public void testReadPrivileges() throws SQLException {
        String user = "user1";
        if(userReader.userExist(user)){
            //We want to have clean user
            SQLUtil.executeUpdateStatement(conn,"drop user \"" + user + "\" cascade");
        }
        SQLUtil.executeUpdateStatement(conn,"create user \"" + user + "\" identified by password");
        try{
            SQLUtil.executeUpdateStatement(conn,"drop table MYTABLE");
        }
        catch(SQLException e){}
        SQLUtil.executeUpdateStatement(conn,"create table mytable(id number)");
        SQLUtil.executeUpdateStatement(conn,"grant create session to \"" + user + "\"");
        SQLUtil.executeUpdateStatement(conn,"grant select on mytable to \"" + user + "\"");
        final List<String> readPrivileges = privReader.readPrivileges(user);
        Assert.assertThat(readPrivileges, JUnitMatchers.hasItem("CREATE SESSION"));
        Assert.assertThat(readPrivileges, JUnitMatchers.hasItem("SELECT ON " + cfg.getUser() + ".MYTABLE"));
        SQLUtil.rollbackQuietly(conn);
        
    }

}
