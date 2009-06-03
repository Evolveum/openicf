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
import org.identityconnectors.oracle.OracleConfiguration.ConnectionType;
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
        OracleConfiguration cfg = createEmptyCfg();
        assertValidateFail(cfg,"Validate for empty cfg should fail");
        
        //Try datasource validate
        cfg.setDataSource("myDS");
        cfg.setDriver(null);
        cfg.setPort(null);
        cfg.validate();
        Assert.assertEquals(ConnectionType.DATASOURCE,cfg.getConnType());
        
        cfg.setHost("anyHost");
        assertValidateFail(cfg, "validate must fail for datasource configuration when host is not blank");
        cfg.setHost(null);
        cfg.setPort("1234");
        assertValidateFail(cfg, "validate must fail for datasource configuration when port is not blank");
        cfg.setPort(null);
        cfg.setDatabase("myDB");
        assertValidateFail(cfg, "validate must fail for custom datasource configuration when database is not blank");
        cfg.setDatabase(null);
        cfg.setDriver("myDriver");
        assertValidateFail(cfg, "validate must fail for custom datasource configuration when driver is not blank");
        //if user is set, then also password must be set
        cfg = createEmptyCfg();
        cfg.setDataSource("myDS");
        cfg.setDriver(null);
        cfg.setPort(null);
        cfg.validate();
        cfg.setUser("myUser");
        assertValidateFail(cfg, "validate must fail for custom datasource configuration , set user and null password");
        cfg.setUser(null);
        cfg.setPassword(new GuardedString("myPassword".toCharArray()));
        assertValidateFail(cfg, "validate must fail for custom datasource configuration , null user and not null password");
        cfg.setUser("myUser");
        cfg.validate();
        
        //Try thin driver
        cfg = createEmptyCfg();
        cfg.setDriver(OracleSpecifics.THIN_DRIVER);
        assertValidateFail(cfg,"Validate cfg should fail, not enaough info");
        cfg.setHost("localhost");
        cfg.setDatabase("XE");
        cfg.setUser("user");
        cfg.setPassword(new GuardedString(new char[]{'p'}));
        cfg.validate();
        Assert.assertEquals(ConnectionType.THIN,cfg.getConnType());
        
        //Try oci driver
        cfg = createEmptyCfg();
        cfg.setDriver(OracleSpecifics.OCI_DRIVER);
        assertValidateFail(cfg,"Validate cfg should fail, not enaough info");
        cfg.setDatabase("XE");
        cfg.setUser("user");
        cfg.setPassword(new GuardedString(new char[]{'p'}));
        cfg.validate();
        Assert.assertEquals(ConnectionType.OCI,cfg.getConnType());
        
        //Try custom driver
        cfg.setDriver("invalidClass");
        assertValidateFail(cfg,"Validate cfg should fail, invalid driver class");
        cfg.setDriver(Long.class.getName());
        assertValidateFail(cfg,"Validate cfg should fail, invalid driver class");
        cfg.setDriver(Driver.class.getName());
        assertValidateFail(cfg,"Validate cfg should fail, not enough info");
        cfg.setUrl("java:jdbc:customUrl");
        cfg.setDatabase(null);
        cfg.setPort(null);
        cfg.setHost(null);
        cfg.validate();
        Assert.assertEquals(ConnectionType.FULL_URL,cfg.getConnType());
        cfg.setHost("myHost");
        assertValidateFail(cfg, "validate must fail for custom URL configuration when host is not blank");
        cfg.setHost(null);
        cfg.setPort("1234");
        assertValidateFail(cfg, "validate must fail for custom URL configuration when port is not blank");
        cfg.setPort(null);
        cfg.setDatabase("myDB");
        assertValidateFail(cfg, "validate must fail for custom URL configuration when database is not blank");
        
    }
    
    @Test
    public void testExplicitValidation(){
    	//Test with ds
    	OracleConfiguration cfg = createEmptyCfg();
    	cfg.setSourceType(ConnectionType.DATASOURCE.getSourceType());
    	cfg.setDataSource("myDS");
    	cfg.validate();
    	Assert.assertEquals(ConnectionType.DATASOURCE, cfg.getConnType());
    	Assert.assertNull("Driver classname must be null", cfg.getDriverClassName());
    	//We should be able to set host even when ignored
    	cfg.setHost("myHost");
    	cfg.setDataSource(null);
    	assertValidateFail(cfg, "Validate must fail for null ds");
    	
    	//Test with thin driver
    	cfg = createEmptyCfg();
    	cfg.setSourceType(ConnectionType.THIN.getSourceType());
    	cfg.setHost("myHost");
    	cfg.setPort("12345");
    	cfg.setUser("user");
    	cfg.setPassword(new GuardedString("myPassword".toCharArray()));
    	cfg.setDatabase("myDB");
    	cfg.validate();
    	Assert.assertEquals(ConnectionType.THIN, cfg.getConnType());
    	Assert.assertEquals(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME, cfg.getDriverClassName());
    	cfg.setDataSource("ds");
    	cfg.validate();
    	cfg.setHost(null);
    	assertValidateFail(cfg, "Validate must fail for null host");
    	
    	//Test with oci driver
    	cfg = createEmptyCfg();
    	cfg.setSourceType(ConnectionType.OCI.getSourceType());
    	cfg.setUser("user");
    	cfg.setPassword(new GuardedString("myPassword".toCharArray()));
    	cfg.setDatabase("myDB");
    	cfg.validate();
    	Assert.assertEquals(ConnectionType.OCI, cfg.getConnType());
    	cfg.setDataSource("ds");
    	cfg.validate();
    	Assert.assertEquals(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME, cfg.getDriverClassName());
    	cfg.setDatabase(null);
    	assertValidateFail(cfg, "Validate must fail for null database");
    	
    	//Test with custom driver
    	cfg = createEmptyCfg();
		cfg.setSourceType(ConnectionType.FULL_URL.getSourceType());
		cfg.setUrl("myURL");
		cfg.setDriver(Driver.class.getName());
    	cfg.setUser("user");
    	cfg.setPassword(new GuardedString("myPassword".toCharArray()));
    	cfg.validate();
    	Assert.assertEquals(ConnectionType.FULL_URL, cfg.getConnType());
    	cfg.setUrl(null);
    	assertValidateFail(cfg, "Validate must fail for null url");
    }

	private OracleConfiguration createEmptyCfg() {
		OracleConfiguration cfg = new OracleConfiguration();
        cfg.setConnectorMessages(TestHelpers.createDummyMessages());
		return cfg;
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
        OracleConfiguration thinCfg = createThinConfiguration();
        thinCfg.validate();
        Connection conn = thinCfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
        
        OracleConfiguration cfg = thinCfg.clone();
        cfg.setUser(null);
        assertValidateFail(cfg, "Validate for null user must fail");
        
        cfg = thinCfg.clone();
        cfg.setUser("invalidUser");
        cfg.validate();
        assertCreateAdminConnectionFail(cfg, "Create connection must fail on invalid user");
        
        //Set thin driver
        cfg = thinCfg.clone();
        cfg.setDriver(OracleSpecifics.THIN_DRIVER);
        cfg.validate();
        conn = cfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
        
        //Thin and oracle.jdbc.driver.OracleDriver we find as thin driver 
        cfg = thinCfg.clone();
        cfg.setDriver(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
        cfg.validate();
        SQLUtil.closeQuietly(cfg.createAdminConnection());
        
        cfg = thinCfg.clone();
        cfg.setDriver("invalidDriver");
        assertValidateFail(cfg, "Validate for invalid driver must fail");
        
        cfg = thinCfg.clone();
        cfg.setDatabase(null);
        assertValidateFail(cfg,"Validate must fail for null database");
        
        cfg = thinCfg.clone();
        cfg.setDriver(null);
        assertValidateFail(cfg,"Validate must fail for null driver");
        
        cfg = thinCfg.clone();
        cfg.setHost(null);
        assertValidateFail(cfg,"Validate must fail for null host");
        
        cfg = thinCfg.clone();
        cfg.setUrl("invalidUrl");
        cfg.setHost(null);
        cfg.setPort(null);
        cfg.setDatabase(null);
        cfg.validate();
        assertCreateAdminConnectionFail(cfg, "Create connection must fail on invalid url");
        
        String url = TestHelpers.getProperty("thin.url", null);
        assertNotNull(url);
        cfg.setUrl(url);
        cfg.validate();
        SQLUtil.closeQuietly(cfg.createAdminConnection());
    }
    
    /** Test OCI driver configuration */
    @Test
    public void testOciConfiguration(){
        OracleConfiguration ociCfg = createOciConfiguration();
        ociCfg.validate();
        Connection conn = null;
        try{
        	conn = ociCfg.createAdminConnection();
        }
        catch(UnsatisfiedLinkError e){
        	return;
        }
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
        OracleConfiguration cfg = ociCfg.clone();
        cfg.setDatabase(null);
        assertValidateFail(cfg, "Validate must fail for null database");
        cfg = ociCfg.clone();
        cfg.setDriver(null);
        assertValidateFail(cfg,"Validate must fail for null driver");
        cfg = ociCfg.clone();
        cfg.setHost(null);
        cfg.validate();
        conn = cfg.createAdminConnection();
        assertNotNull(conn);
        SQLUtil.closeQuietly(conn);
    }
    
    /**
     * Test cfg with custom driver and url
     */
    @Test
    public void testCustomDriverConfiguration(){
        String user = TestHelpers.getProperty("customDriver.user", null);
        String password = TestHelpers.getProperty("customDriver.password", null);
        String url = TestHelpers.getProperty("customDriver.url", null);
        String driver = TestHelpers.getProperty("customDriver.driverClassName", null);
        OracleConfiguration cfg = createEmptyCfg();
        cfg.setUser(user);
        cfg.setPassword(new GuardedString(password.toCharArray()));
        cfg.setUrl(url);
        cfg.setDriver(driver);
        cfg.setPort(null);
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
        cfg.setConnectorMessages(TestHelpers.createDummyMessages());
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
        cfg.setConnectorMessages(TestHelpers.createDummyMessages());
        return  cfg;
    }
    
    static OracleConfiguration createSystemConfiguration(){
    	//use thin here, no switch
    	return createThinConfiguration();
    }
    
    private static final String[] dsJNDIEnv = new String[]{"java.naming.factory.initial=" + MockContextFactory.class.getName()};
    
    private static OracleConfiguration createDataSourceConfiguration(){
        OracleConfiguration conf = new OracleConfiguration();
        conf.setConnectorMessages(TestHelpers.createDummyMessages());
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
        OracleConfiguration dsConf = createDataSourceConfiguration();
        //set to thread local
        dsCfg.set(dsConf);
        assertArrayEquals(dsConf.getDsJNDIEnv(), dsJNDIEnv);
        dsConf.validate();
        Connection conn = dsConf.createAdminConnection();
        dsConf.setUser(null);
        dsConf.setPassword(null);
        dsConf.validate();
        conn = dsConf.createAdminConnection();
        assertNotNull(conn);
        
        OracleConfiguration cfg = dsConf.clone();
        cfg.setDataSource(null);
        assertValidateFail(cfg, "Validate should fail for null datasource");
        cfg.setDataSource(WRONG_DATASOURCE);
        assertCreateAdminConnectionFail(cfg, "CreateAdminConnection with wrong datasource should fail");
    }
    
    /** Test settings of case sensitivity */
    @Test
    public void testSetCaseSensitivity(){
        OracleConfiguration conf = createEmptyCfg();
        conf.setCaseSensitivity("default");
        assertNotNull("CaseSensitivity should not be null",conf.getCSSetup());
        assertEquals("Default normalizer should have value from attribute be toupper",OracleUserAttributeCS.USER.isDefToUpper(),conf.getCSSetup().getAttributeFormatterAndNormalizer(OracleUserAttributeCS.USER).isToUpper());
        assertEquals("Default formatter should use quates from attribute",OracleUserAttributeCS.USER.getDefQuatesChar(),conf.getCSSetup().getAttributeFormatterAndNormalizer(OracleUserAttributeCS.USER).getQuatesChar());
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
    //Replace with EasyMock after switch to maven
    private static  class ConnectionIH implements InvocationHandler{
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        	if("getAutoCommit".equals(method.getName())){
        		return false;
        	}
        	if("getTransactionIsolation".equals(method.getName())){
        		return Connection.TRANSACTION_READ_COMMITTED;
        	}
        	if("isClosed".equals(method.getName())){
        		return false;
        	}
            return null;
        }
    }
    

}
