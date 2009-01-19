package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import java.sql.*;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.Test;

/**
 * Tests for {@link OracleConfiguration}
 * @author kitko
 *
 */
public class OracleConfigurationTest {

    /** Test validation of cfg */
    @Test
    public void testValidate() {
        OracleConfiguration cfg = new OracleConfiguration();
        assertValidateFail(cfg,"Validate for empty cfg should fail");
        //Try datasource validate
        cfg.setDataSource("myDS");
        cfg.validate();
        
        //Try thin driver
        cfg = new OracleConfiguration();
        cfg.setDriver(OracleSpecifics.THIN_DRIVER);
        assertValidateFail(cfg,"Validate cfg should fail, not enaough info");
        cfg.setHost("localhost");
        cfg.setDatabase("XE");
        cfg.setUser("user");
        cfg.setPassword(new GuardedString(new char[]{'p'}));
        cfg.validate();
        
        //Try oci driver
        cfg = new OracleConfiguration();
        cfg.setDriver(OracleSpecifics.OCI_DRIVER);
        assertValidateFail(cfg,"Validate cfg should fail, not enaough info");
        cfg.setDatabase("XE");
        cfg.setUser("user");
        cfg.setPassword(new GuardedString(new char[]{'p'}));
        cfg.validate();
        
        //Try custom driver
        cfg.setDriver("invalidClass");
        assertValidateFail(cfg,"Validate cfg should fail, invalid driver class");
        cfg.setDriver(Long.class.getName());
        assertValidateFail(cfg,"Validate cfg should fail, invalid driver class");
        cfg.setDriver(Driver.class.getName());
        assertValidateFail(cfg,"Validate cfg should fail, not enough info");
        cfg.setUrl("java:jdbc:customUrl");
        cfg.validate();
        
    }
    
    private void assertValidateFail(OracleConfiguration cfg,String failMsg){
        try{
            cfg.validate();
            fail(failMsg);
        }
        catch(Exception e){}
    }
    
    private void assertCreateAdminConnectionFail(OracleConfiguration cfg,String failMsg){
        try{
            cfg.createAdminConnection();
            fail(failMsg);
        }
        catch(Exception e){}
    }

    /** Test creation of connection from thin driver */
    @Test
    public void testThinConfiguration() {
        OracleConfiguration cfg = createThinConfiguration();
        cfg.validate();
        Connection conn = cfg.createAdminConnection();
        assertNotNull(conn);
        OracleConfiguration failCfg = cfg.clone();
        failCfg.setUser(null);
        assertValidateFail(failCfg, "Validate for null user must fail");
        failCfg = cfg.clone();
        failCfg.setUser("invalidUser");
        failCfg.validate();
        assertCreateAdminConnectionFail(failCfg, "Create connection must fail on invalid user");
        failCfg = cfg.clone();
        failCfg.setDriver(OracleSpecifics.THIN_DRIVER);
        failCfg.validate();
        failCfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
        failCfg.setDriver("invalidDriver");
        assertValidateFail(failCfg, "Validate for invalid driver must fail");
        failCfg = cfg.clone();
        failCfg.setDatabase(null);
        assertValidateFail(failCfg,"Validate must fail for null database");
        failCfg = cfg.clone();
        failCfg.setDriver(null);
        assertValidateFail(failCfg,"Validate must fail for null driver");
        failCfg = cfg.clone();
        failCfg.setHost(null);
        assertValidateFail(failCfg,"Validate must fail for null host");
        failCfg = cfg.clone();
        failCfg.setDriver(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
        assertValidateFail(failCfg, "Must fail, url must be specified when driver classname is specified");
        failCfg.setUrl("invalidUrl");
        failCfg.validate();
        assertCreateAdminConnectionFail(failCfg, "Create connection must fail on invalid url");
        String url = TestHelpers.getProperty("thin.url", null);
        assertNotNull(url);
        failCfg.setUrl(url);
        failCfg.validate();
        SQLUtil.closeQuietly(failCfg.createAdminConnection());
    }
    
    /** Test OCI driver configuration */
    @Test
    public void testOciConfiguration(){
        OracleConfiguration cfg = createOciConfiguration();
        cfg.validate();
        Connection conn = cfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
        OracleConfiguration failCfg = cfg.clone();
        failCfg.setDatabase(null);
        assertValidateFail(failCfg, "Validate must fail for null database");
        failCfg = cfg.clone();
        failCfg.setDriver(null);
        assertValidateFail(failCfg,"Validate must fail for null driver");
        failCfg = cfg.clone();
        failCfg.setHost(null);
        failCfg.validate();
        SQLUtil.closeQuietly(failCfg.createAdminConnection());
        
    }
    
    
    /** Test of clone */
    @Test
    public void testClone(){
        OracleConfiguration cfg = createThinConfiguration();
        OracleConfiguration clone = cfg.clone();
        assertNotSame(cfg,clone);
        assertEquals(cfg.getUser(), cfg.getUser());
    }
    
    private OracleConfiguration createThinConfiguration(){
        String user = TestHelpers.getProperty("thin.user",null);
        String passwordString = TestHelpers.getProperty("thin.password", null);
        GuardedString password = new GuardedString(passwordString.toCharArray());
        String driver = OracleSpecifics.THIN_DRIVER;
        String host = TestHelpers.getProperty("thin.host",null);
        String port = TestHelpers.getProperty("thin.port",OracleSpecifics.LISTENER_DEFAULT_PORT);
        String database = TestHelpers.getProperty("thin.database", null);
        OracleConfiguration cfg = new OracleConfiguration();
        cfg.setUser(user);
        cfg.setDatabase(database);
        cfg.setDriver(driver);
        cfg.setHost(host);
        cfg.setPassword(password);
        cfg.setPort(port);
        return  cfg;
    }
    
    private OracleConfiguration createOciConfiguration(){
        String user = TestHelpers.getProperty("oci.user", null);
        String passwordString = TestHelpers.getProperty("oci.password", null);
        GuardedString password = new GuardedString(passwordString.toCharArray());
        String database = TestHelpers.getProperty("oci.database", null);
        String driver = OracleSpecifics.OCI_DRIVER;
        String host = TestHelpers.getProperty("oci.host",null);
        String port = TestHelpers.getProperty("oci.port",OracleSpecifics.LISTENER_DEFAULT_PORT);
        OracleConfiguration cfg = new OracleConfiguration();
        cfg.setUser(user);
        cfg.setDatabase(database);
        cfg.setDriver(driver);
        cfg.setHost(host);
        cfg.setPassword(password);
        cfg.setPort(port);
        return  cfg;
        
    }

}
