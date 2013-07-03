package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.SQLException;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.oracle.OracleDriverConnectionInfo.Builder;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Tests for OracleSpecifics
 *
 * @author kitko
 *
 */
@Test(groups = { "integration" })
public class OracleSpecificsTest {

    private static final PropertyBag TEST_PROPS = TestHelpers.getProperties(OracleConnector.class);

    private Connection createThinDriverConnection(String user, GuardedString password) {
        String host = TEST_PROPS.getStringProperty("thin.host");
        String port = TEST_PROPS.getStringProperty("thin.port");
        String database = TEST_PROPS.getStringProperty("thin.database");
        Connection conn =
                OracleSpecifics.createThinDriverConnection(new Builder().setUser(user).setPassword(
                        password).setHost(host).setPort(port).setDatabase(database).build(),
                        TestHelpers.createDummyMessages());
        return conn;
    }

    private Connection createTestThinDriverConnection() {
        String user = TEST_PROPS.getStringProperty("thin.user");
        String passwordString = TEST_PROPS.getStringProperty("thin.password");
        return createThinDriverConnection(user, new GuardedString(passwordString.toCharArray()));
    }

    private Connection createSystemThinDriverConnection() {
        String user = TEST_PROPS.getStringProperty("thin.user");
        String passwordString = TEST_PROPS.getStringProperty("thin.password");
        return createThinDriverConnection(user, new GuardedString(passwordString.toCharArray()));
    }

    private Connection createOciDriverConnection(String user, GuardedString password) {
        String host = TEST_PROPS.getStringProperty("oci.host");
        String port = TEST_PROPS.getStringProperty("oci.port");
        String database = TEST_PROPS.getStringProperty("oci.database");
        Connection conn =
                OracleSpecifics.createOciDriverConnection(new Builder().setUser(user).setPassword(
                        password).setHost(host).setPort(port).setDatabase(database).build(),
                        TestHelpers.createDummyMessages());
        return conn;
    }

    private Connection createTestOciDriverConnection() {
        String user = TEST_PROPS.getStringProperty("oci.user");
        String passwordString = TEST_PROPS.getStringProperty("oci.password");
        GuardedString password = new GuardedString(passwordString.toCharArray());
        return createOciDriverConnection(user, password);
    }

    private Connection createSystemOciDriverConnection() {
        String user = TEST_PROPS.getStringProperty("oci.user");
        String passwordString = TEST_PROPS.getStringProperty("oci.password");
        GuardedString password = new GuardedString(passwordString.toCharArray());
        return createOciDriverConnection(user, password);
    }

    @Test
    private void testStaleConnection(Connection systemConn, Connection testConn)
            throws SQLException {
        // Here connection should be ok
        OracleSpecifics.testConnection(testConn);
        OracleSpecifics.killConnection(systemConn, testConn);
        // Here testConn is staled
        try {
            OracleSpecifics.testConnection(testConn);
            Assert.fail("Session is killed, test should fail");
        } catch (Exception e) {
        }
        SQLUtil.closeQuietly(systemConn);
        SQLUtil.closeQuietly(testConn);
    }

    /** Test create new thin driver connection */
    @Test
    public void testCreateThinDriverConnection() {
        Connection conn = createTestThinDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);
        conn = createSystemThinDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);

        // try connection without host
        String database = TEST_PROPS.getStringProperty("thin.database");
        String user = TEST_PROPS.getStringProperty("thin.user");
        String password = TEST_PROPS.getStringProperty("thin.password");
        String host = TEST_PROPS.getStringProperty("thin.host");
        String port = TEST_PROPS.getStringProperty("thin.port");
        conn =
                OracleSpecifics.createThinDriverConnection(new Builder().setHost(host)
                        .setPort(port).setDatabase(database).setUser(user).setPassword(
                                new GuardedString(password.toCharArray())).build(), TestHelpers
                        .createDummyMessages());
        Assert.assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }

    /** Test create of oci connection */
    @Test
    public void testCreateOciDriverConnection() {
        Connection conn = null;
        try {
            conn = createTestOciDriverConnection();
        } catch (UnsatisfiedLinkError e) {
            return;
        }
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);
        conn = createSystemOciDriverConnection();
        Assert.assertNotNull(conn);
        OracleSpecifics.testConnection(conn);
        SQLUtil.closeQuietly(conn);

        // try connection without host
        String database = TEST_PROPS.getStringProperty("oci.database");
        String user = TEST_PROPS.getStringProperty("oci.user");
        String password = TEST_PROPS.getStringProperty("oci.password");
        conn =
                OracleSpecifics.createOciDriverConnection(new Builder().setDatabase(database)
                        .setUser(user).setPassword(new GuardedString(password.toCharArray()))
                        .build(), TestHelpers.createDummyMessages());
        Assert.assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }

    /** Test creation of connection from custom driver */
    @Test
    public void testCustomDriverConnection() {
        String user = TEST_PROPS.getStringProperty("customDriver.user");
        String password = TEST_PROPS.getStringProperty("customDriver.password");
        String url = TEST_PROPS.getStringProperty("customDriver.url");
        String driver = TEST_PROPS.getStringProperty("customDriver.driverClassName");
        Connection conn =
                OracleSpecifics.createCustomDriverConnection(new Builder().setUser(user)
                        .setPassword(new GuardedString(password.toCharArray())).setUrl(url)
                        .setDriver(driver).build(), TestHelpers.createDummyMessages());
        Assert.assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }

    /**
     * Test wheather we properly detect stale thin connection
     *
     * @throws SQLException
     */
    @Test
    public void testStaleThinConnection() throws SQLException {
        Connection systemConn = createSystemThinDriverConnection();
        Connection testConn = createTestThinDriverConnection();
        testStaleConnection(systemConn, testConn);
    }

    /**
     * Test wheather we properly detect stale oci connection
     *
     * @throws SQLException
     */
    @Test
    public void testStaleOciConnection() throws SQLException {
        try {
            Connection systemConn = createSystemOciDriverConnection();
            Connection testConn = createTestOciDriverConnection();
            testStaleConnection(systemConn, testConn);
        } catch (UnsatisfiedLinkError e) {
        }
    }

    @Test
    public void testParseConnectionInfo() throws SQLException {
        Connection conn = createSystemThinDriverConnection();
        OracleDriverConnectionInfo info =
                OracleSpecifics.parseConnectionInfo(conn, TestHelpers.createDummyMessages());
        AssertJUnit.assertNotNull(info);
        String user = TEST_PROPS.getStringProperty("thin.user");
        String passwordString = TEST_PROPS.getStringProperty("thin.password");
        OracleDriverConnectionInfo newInfo =
                new OracleDriverConnectionInfo.Builder().setvalues(info).setUser(user).setPassword(
                        new GuardedString(passwordString.toCharArray())).build();
        Connection conn1 =
                OracleSpecifics.createThinDriverConnection(newInfo, TestHelpers
                        .createDummyMessages());
        Assert.assertNotNull(conn1);
        conn1.close();
        conn.close();
    }

}
