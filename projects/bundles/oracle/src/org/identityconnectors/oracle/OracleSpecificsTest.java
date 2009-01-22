package org.identityconnectors.oracle;

import static org.junit.Assert.fail;

import java.sql.*;
import java.text.MessageFormat;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.test.TestHelpers;
import org.identityconnectors.oracle.OracleDriverConnectionInfo.OracleDriverConnectionInfoBuilder;
import org.junit.*;

/**
 * Tests for OracleSpecifics
 * @author kitko
 *
 */
public class OracleSpecificsTest {
    
    private Connection createThinDriverConnection(String user,GuardedString password){
        String host = TestHelpers.getProperty("thin.host",null);
        String port = TestHelpers.getProperty("thin.port",null);
        String database = TestHelpers.getProperty("thin.database", null);
        Connection conn = 
            OracleSpecifics.createThinDriverConnection(new OracleDriverConnectionInfoBuilder().
                    setUser(user).setPassword(password).
                    setHost(host).setPort(port).setDatabase(database).
                    build());
        return conn;
    }
    
    private Connection createTestThinDriverConnection(){
        String user = TestHelpers.getProperty("thin.user",null);
        String passwordString = TestHelpers.getProperty("thin.password", null);
        return createThinDriverConnection(user, new GuardedString(passwordString.toCharArray()));
    }
    
    private Connection createSystemThinDriverConnection(){
        String user = TestHelpers.getProperty("thin.systemUser",null);
        String passwordString = TestHelpers.getProperty("thin.systemPassword", null);
        return createThinDriverConnection(user, new GuardedString(passwordString.toCharArray()));
    }
    
    private Connection createOciDriverConnection(String user,GuardedString password){
        String host = TestHelpers.getProperty("oci.host",null);
        String port = TestHelpers.getProperty("oci.port",null);
        String database = TestHelpers.getProperty("oci.database", null);
        Connection conn = 
            OracleSpecifics.createOciDriverConnection(new OracleDriverConnectionInfoBuilder().
                    setUser(user).setPassword(password).
                    setHost(host).setPort(port).setDatabase(database).
                    build());
        return conn;
    }
    
    
    private Connection createTestOciDriverConnection(){
        String user = TestHelpers.getProperty("oci.user",null);
        String passwordString = TestHelpers.getProperty("oci.password", null);
        GuardedString password = new GuardedString(passwordString.toCharArray());
        return createOciDriverConnection(user, password);
    }
    
    private Connection createSystemOciDriverConnection(){
        String user = TestHelpers.getProperty("oci.systemUser",null);
        String passwordString = TestHelpers.getProperty("oci.systemPassword", null);
        GuardedString password = new GuardedString(passwordString.toCharArray());
        return createOciDriverConnection(user, password);
    }
    
    private void testStaleConnection(Connection systemConn,Connection testConn) throws SQLException{
        Object sid = SQLUtil.selectFirstRowFirstValue(testConn, "SELECT USERENV('SID') FROM DUAL");
        Object serialNumber = SQLUtil.selectFirstRowFirstValue(systemConn, "select serial# from v$session where SID  = " +  sid);
        String killSql = MessageFormat.format("ALTER SYSTEM KILL SESSION {0} ", "'" + sid + "," + serialNumber + "'");
        SQLUtil.executeUpdateStatement(systemConn, killSql);
        //Now testConn is staled
        try{
            OracleSpecifics.testConnection(testConn);
            fail("Session is killed, test should fail");
        }
        catch(Exception e){
        }
        SQLUtil.closeQuietly(systemConn);
        SQLUtil.closeQuietly(testConn);
    }
    
    
    /** Test create new thin driver connection */
    @Test
    public void testCreateThinDriverConnection(){
        Connection conn = createTestThinDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);
        conn = createSystemThinDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);
        
        //try connection without host 
        String database = TestHelpers.getProperty("thin.database",null);
        String user = TestHelpers.getProperty("thin.user",null);
        String password = TestHelpers.getProperty("thin.password", null);
        conn = OracleSpecifics
                .createThinDriverConnection(new OracleDriverConnectionInfoBuilder()
                        .setDatabase(database).setUser(user).setPassword(new GuardedString(password.toCharArray())).build());
        Assert.assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }
    
    /** Test create of oci connection */
    @Test
    public void testCreateOciDriverConnection(){
        Connection conn = createTestOciDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);
        conn = createSystemOciDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);
        
        //try connection without host 
        String database = TestHelpers.getProperty("oci.database",null);
        String user = TestHelpers.getProperty("oci.user",null);
        String password = TestHelpers.getProperty("oci.password", null);
        conn = OracleSpecifics
                .createOciDriverConnection(new OracleDriverConnectionInfoBuilder()
                        .setDatabase(database).setUser(user).setPassword(new GuardedString(password.toCharArray())).build());
        Assert.assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }
    
    /** Test creation of connection from custom driver */
    @Test
    public void testCustomDriverConnection(){
        String user = TestHelpers.getProperty("customDriver.user", null);
        String password = TestHelpers.getProperty("customDriver.password", null);
        String url = TestHelpers.getProperty("customDriver.url", null);
        String driver = TestHelpers.getProperty("customDriver.driverClassName", null);
        Connection conn = OracleSpecifics
                .createDriverConnection(new OracleDriverConnectionInfoBuilder()
                        .setUser(user).setPassword(
                                new GuardedString(password.toCharArray()))
                        .setUrl(url).setDriver(driver).build());
        Assert.assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }
    
    
    /**
     * Test wheather we properly detect stale thin connection
     * @throws SQLException
     */
    @Test
    public void testStaleThinConnection() throws SQLException{
        Connection systemConn = createSystemThinDriverConnection();
        Connection testConn = createTestThinDriverConnection();
        testStaleConnection(systemConn, testConn);
    }
    
    /**
     * Test wheather we properly detect stale oci connection
     * @throws SQLException
     */
    @Test
    public void testStaleOciConnection() throws SQLException{
        Connection systemConn = createSystemOciDriverConnection();
        Connection testConn = createTestOciDriverConnection();
        testStaleConnection(systemConn, testConn);
    }
    
    
}
