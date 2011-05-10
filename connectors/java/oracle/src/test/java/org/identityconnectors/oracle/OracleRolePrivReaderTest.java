package org.identityconnectors.oracle;

import static org.testng.AssertJUnit.assertTrue;
import static org.fest.assertions.Assertions.assertThat;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.test.common.TestHelpers;
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
        userReader = new OracleUserReader(conn,TestHelpers.createDummyMessages());
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
        SQLUtil.executeUpdateStatement(conn, "drop user \"" + user + "\"");
        SQLUtil.executeUpdateStatement(conn, "drop role \"testrole\"");
    }

    /** Test reading all user privileges 
     * @throws SQLException */
	@Test
    public void testReadAllPrivileges() throws SQLException {
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
        final List<String> readPrivileges = privReader.readAllPrivileges(user);
        assertThat(readPrivileges).contains("CREATE SESSION");
        assertThat(readPrivileges).contains("SELECT ON " + cfg.getUserOwner() + ".MYTABLE");
        SQLUtil.executeUpdateStatement(conn, "drop user \"" + user + "\"");
        SQLUtil.executeUpdateStatement(conn, "drop table MYTABLE");
    }
	
	@Test
	public void testReadAllSystemPrivileges() throws SQLException{
	    String user = "user1";
        if(userReader.userExist(user)){
            //We want to have clean user
            SQLUtil.executeUpdateStatement(conn,"drop user \"" + user + "\" cascade");
        }
        SQLUtil.executeUpdateStatement(conn,"create user \"" + user + "\" identified by password");
	    SQLUtil.executeUpdateStatement(conn,"grant create session to \"" + user + "\"");
	    SQLUtil.executeUpdateStatement(conn,"grant alter session to \"" + user + "\"");
	    SQLUtil.executeUpdateStatement(conn,"grant debug connect session to \"" + user + "\"");
	    final List<String> privileges = privReader.readSystemPrivileges(user);
	    assertThat(privileges).contains("CREATE SESSION","ALTER SESSION","DEBUG CONNECT SESSION");
	    SQLUtil.executeUpdateStatement(conn, "drop user \"" + user + "\"");
	}
	
    @Test
    public void testReadObjectPrivileges() throws SQLException{
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
        
        SQLUtil.executeUpdateStatement(conn,"grant select on mytable to \"" + user + "\"");
        SQLUtil.executeUpdateStatement(conn,"grant delete on mytable to \"" + user + "\"");
        SQLUtil.executeUpdateStatement(conn,"grant update on mytable to \"" + user + "\"");
        final List<String> privileges = privReader.readObjectPrivileges(user);
        SQLUtil.executeUpdateStatement(conn, "drop user \"" + user + "\"");
        SQLUtil.executeUpdateStatement(conn, "drop table MYTABLE");
        AssertJUnit.assertEquals("Must have just 3 object privileges", 3, privileges.size());
        assertThat(privileges).contains("SELECT ON " + cfg.getUserOwner() + ".MYTABLE");
        assertThat(privileges).contains("DELETE ON " + cfg.getUserOwner() + ".MYTABLE");
        assertThat(privileges).contains("UPDATE ON " + cfg.getUserOwner() + ".MYTABLE");
        
    }
	

}
