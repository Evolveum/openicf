package org.identityconnectors.oracle;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.oracle.OracleConfiguration.ConnectionType;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link OracleConfiguration}
 * @author kitko
 *
 */
public class OracleConfigurationTest {
    private final static PropertyBag testProps = TestHelpers.getProperties(OracleConnector.class);
    private final static String WRONG_DATASOURCE = "wrongDatasource";
    private static ThreadLocal<OracleConfiguration> threadCfg = new ThreadLocal<OracleConfiguration>();

    @After
    public void tearDown() throws SQLException{
    	tearDownClass();
    }
    
    @AfterClass
    public static void tearDownClass() throws SQLException{
    	threadCfg.set(null);
    }

    @Test
    public void testDefaultValues(){
    	OracleConfiguration cfg = createEmptyCfg();
    	Assert.assertEquals("default", cfg.getCaseSensitivityString());
    	Assert.assertTrue(cfg.isDropCascade());
    	Assert.assertEquals(OracleNormalizerName.INPUT.name(), cfg.getNormalizerString());
    }

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
        
        cfg = createDataSourceConfiguration();
        cfg.validate();
        cfg.setUseDriverForAuthentication(true);
        cfg.validate();
        
        cfg = createOciConfiguration();
        cfg.validate();
        cfg.setUseDriverForAuthentication(true);
        assertValidateFail(cfg, "validate must fail for oci cfg and useDriverForAuthentication=true");
        
        cfg = createThinConfiguration();
        cfg.validate();
        cfg.setUseDriverForAuthentication(true);
        assertValidateFail(cfg, "validate must fail for thin cfg and useDriverForAuthentication=true");

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
        
        String url = testProps.getStringProperty("thin.url");
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
        String user = testProps.getStringProperty("customDriver.user");
        String password = testProps.getStringProperty("customDriver.password");
        String url = testProps.getStringProperty("customDriver.url");
        String driver = testProps.getStringProperty("customDriver.driverClassName");
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
        String user = testProps.getStringProperty("thin.user");
        String passwordString = testProps.getStringProperty("thin.password");
        GuardedString password = new GuardedString(passwordString.toCharArray());
        String driver = OracleSpecifics.THIN_DRIVER;
        String host = testProps.getStringProperty("thin.host");
        String port = testProps.getProperty("thin.port",String.class,OracleSpecifics.LISTENER_DEFAULT_PORT);
        String database = testProps.getStringProperty("thin.database");
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
        String user = testProps.getStringProperty("oci.user");
        String passwordString = testProps.getStringProperty("oci.password");
        GuardedString password = new GuardedString(passwordString.toCharArray());
        String database = testProps.getStringProperty("oci.database");
        String driver = OracleSpecifics.OCI_DRIVER;
        String host = testProps.getStringProperty("oci.host");
        String port = testProps.getProperty("oci.port",String.class,OracleSpecifics.LISTENER_DEFAULT_PORT);
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
    
    static OracleConfiguration createDataSourceConfiguration(){
        OracleConfiguration conf = new OracleConfiguration();
        conf.setConnectorMessages(TestHelpers.createDummyMessages());
        conf.setDataSource("testDS");
        conf.setUser("user");
        conf.setPassword(new GuardedString("myPassword".toCharArray()));
        conf.setDsJNDIEnv(dsJNDIEnv);
        conf.setPort(null);
        conf.setDriver(null);
        threadCfg.set(conf);
        return conf;
    }
    
    
    /**
     * Test getting Connection from DS
     * @throws SQLException 
     */
    @Test
    public void testDataSourceConfiguration() throws SQLException{
        OracleConfiguration dsConf = createDataSourceConfiguration();
        //set to thread local
        assertArrayEquals(dsConf.getDsJNDIEnv(), dsJNDIEnv);
        dsConf.validate();
        Connection conn = dsConf.createAdminConnection();
        assertNotNull(conn);
        conn.close();
        dsConf.setUser(null);
        dsConf.setPassword(null);
        dsConf.validate();
        conn = dsConf.createAdminConnection();
        assertNotNull(conn);
        conn.close();
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
        conf.setCaseSensitivityString("default");
        assertNotNull("CaseSensitivity should not be null",conf.getCSSetup());
        assertEquals("Default normalizer should have value from attribute be toupper",OracleUserAttribute.USER.getFormatting().isToUpper(),conf.getCSSetup().getAttributeFormatterAndNormalizer(OracleUserAttribute.USER).isToUpper());
        assertEquals("Default formatter should use quates from attribute",OracleUserAttribute.USER.getFormatting().getQuatesChar(),conf.getCSSetup().getAttributeFormatterAndNormalizer(OracleUserAttribute.USER).getQuatesChar());
    }
    
    @Test
    public void testDependentValues(){
    	OracleConfiguration cfg = createSystemConfiguration();
    	OracleConfiguration copy = cfg.clone();
    	cfg.validate();
        assertNotNull(cfg.getCSSetup());
        assertNotNull(cfg.getExtraAttributesPolicySetup());
        assertNotNull(cfg.getNormalizerName());
        
        cfg.setCaseSensitivityString("Invalid");
        try{
        	cfg.validate();
        	fail("Validate must fail for invalid caseSensitivityString");
        }
        catch(RuntimeException e){}
        
        cfg = copy;
        cfg.setExtraAttributesPolicyString("PASSWORD={CREATE=FAIL,UPDATE=IGNORE},PASSWORD_EXPIRE={CREATE=IGNORE,UPDATE=FAIL}");
        cfg.validate();
        assertNotNull(cfg.getExtraAttributesPolicySetup());
        assertEquals(ExtraAttributesPolicy.IGNORE,cfg.getExtraAttributesPolicySetup().getPolicy(OracleUserAttribute.PASSWORD, UpdateOp.class));
        assertEquals(ExtraAttributesPolicy.FAIL,cfg.getExtraAttributesPolicySetup().getPolicy(OracleUserAttribute.PASSWORD, CreateOp.class));
        
        cfg = copy;
        cfg.setNormalizerString(OracleNormalizerName.FULL.name());
        cfg.validate();
        assertEquals(OracleNormalizerName.FULL, cfg.getNormalizerName());
        
        cfg.setNormalizerString(OracleNormalizerName.INPUT_AUTH.name());
        cfg.validate();
        assertEquals(OracleNormalizerName.INPUT_AUTH, cfg.getNormalizerName());
        
    }
    
    
    /**
     * Mock for {@link InitialContextFactory}
     * Must be public, used by jndi
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
                if(threadCfg.get().getUser() == null){
                    Assert.assertEquals("getConnection must be called without user and password",0,method.getParameterTypes().length);
                    return createThinConfiguration().createAdminConnection();
                }
                else{
                    Assert.assertEquals("getConnection must be called with user and password",2,method.getParameterTypes().length);
                    return createThinConfiguration().createAdminConnection();
                }
            }
            throw new IllegalArgumentException("Invalid method");
        }
    }
}
