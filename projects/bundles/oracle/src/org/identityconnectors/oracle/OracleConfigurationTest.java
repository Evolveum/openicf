package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.sql.*;
import java.util.Hashtable;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.*;

/**
 * Tests for {@link OracleConfiguration}
 * @author kitko
 *
 */
public class OracleConfigurationTest {
    private final static String WRONG_DATASOURCE = "wrongDatasource";
    private static ThreadLocal<OracleConfiguration> dsCfg = new ThreadLocal<OracleConfiguration>();


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
        Connection conn = null;
        try{
        	conn = cfg.createAdminConnection();
        }
        catch(UnsatisfiedLinkError e){
        	return;
        }
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
        conn = failCfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }
    
    /**
     * Test cfg with custom driver and url
     */
    @Test
    public void testCustomDriverConfiguration(){
        String user = TestHelpers.getProperty("customDriver.user", null);
        String password = TestHelpers.getProperty("customDriver.user", null);
        String url = TestHelpers.getProperty("customDriver.url", null);
        String driver = TestHelpers.getProperty("customDriver.driverClassName", null);
        OracleConfiguration cfg = new OracleConfiguration();
        cfg.setUser(user);
        cfg.setPassword(new GuardedString(password.toCharArray()));
        cfg.setUrl(url);
        cfg.setDriver(driver);
        cfg.validate();
        final Connection conn = cfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }
    
    
    /** Test of clone */
    @Test
    public void testClone(){
        OracleConfiguration cfg = createThinConfiguration();
        OracleConfiguration clone = cfg.clone();
        assertNotSame(cfg,clone);
        assertEquals(cfg.getUser(), cfg.getUser());
    }
    
    static OracleConfiguration createThinConfiguration(){
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
    
    static OracleConfiguration createOciConfiguration(){
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
    
    static OracleConfiguration createSystemConfiguration(){
        String user = TestHelpers.getProperty("thin.systemUser", null);
        String passwordString = TestHelpers.getProperty("thin.systemPassword", "missingPassword");
        GuardedString password = new GuardedString(passwordString.toCharArray());
        String database = TestHelpers.getProperty("thin.database", null);
        String driver = OracleSpecifics.THIN_DRIVER;
        String host = TestHelpers.getProperty("thin.host",null);
        String port = TestHelpers.getProperty("thin.port",OracleSpecifics.LISTENER_DEFAULT_PORT);
        OracleConfiguration cfg = new OracleConfiguration();
        cfg.setUser(user);
        cfg.setPassword(password);
        cfg.setDatabase(database);
        cfg.setDriver(driver);
        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setConnectorMessages(TestHelpers.createDummyMessages());
        return cfg;
    }
    
    private static final String[] dsJNDIEnv = new String[]{"java.naming.factory.initial=" + MockContextFactory.class.getName()};
    
    private static OracleConfiguration createDataSourceConfiguration(){
        OracleConfiguration conf = new OracleConfiguration();
        conf.setDataSource("testDS");
        conf.setUser("user");
        conf.setPassword(new GuardedString(new char[]{'t'}));
        conf.setDsJNDIEnv(dsJNDIEnv);
        conf.setPort(null);
        conf.setDriver(null);
        return conf;
    }
    
    
    /**
     * Test getting Connection from DS
     */
    @Test
    public void testDataSourceConfiguration(){
        OracleConfiguration conf = createDataSourceConfiguration();
        //set to thread local
        dsCfg.set(conf);
        assertArrayEquals(conf.getDsJNDIEnv(), dsJNDIEnv);
        conf.validate();
        Connection conn = conf.createAdminConnection();
        conf.setUser(null);
        conf.setPassword(null);
        conf.validate();
        conn = conf.createAdminConnection();
        assertNotNull(conn);
        
        OracleConfiguration failConf = conf.clone();
        failConf.setDataSource(null);
        assertValidateFail(failConf, "Validate should fail for null datasource");
        failConf.setDataSource(WRONG_DATASOURCE);
        assertCreateAdminConnectionFail(failConf, "CreateAdminConnection with wrong datasource should fail");
    }
    
    /** Test settings of case sensitivity */
    @Test
    public void testSetCaseSensitivity(){
        OracleConfiguration conf = new OracleConfiguration();
        conf.setConnectorMessages(TestHelpers.createDummyMessages());
        conf.setCaseSensitivity("default");
        assertNotNull("CaseSensitivity should not be null",conf.getCSSetup());
        assertTrue("Default normalizer should be toupper",conf.getCSSetup().getAttributeNormalizer(OracleUserAttribute.USER_NAME).isToUpper());
        assertEquals("Default formatter should use \" for quates","\"",conf.getCSSetup().getAttributeFormatter(OracleUserAttribute.USER_NAME).getQuatesChar());
    }
    
    
    /**
     * Mock for {@link InitialContextFactory}
     * @author kitko
     *
     */
    public static class MockContextFactory implements InitialContextFactory{
        
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            Context context = (Context)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Context.class}, new ContextIH());
            return context;
        }
    }
    
    private static class ContextIH implements InvocationHandler{

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("lookup")){
                if(WRONG_DATASOURCE.equals(args[0])){
                    throw new NamingException("Cannot lookup wrong datasource");
                }
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class}, new DataSourceIH());
            }
            return null;
        }
    }
    
    private static class DataSourceIH implements InvocationHandler{
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("getConnection")){
                if(dsCfg.get().getUser() == null){
                    Assert.assertEquals("getConnection must be called without user and password",0,method.getParameterTypes().length);
                }
                else{
                    Assert.assertEquals("getConnection must be called with user and password",2,method.getParameterTypes().length);
                }
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class}, new ConnectionIH());
            }
            throw new IllegalArgumentException("Invalid method");
        }
    }
    
    private static  class ConnectionIH implements InvocationHandler{
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
    

}
