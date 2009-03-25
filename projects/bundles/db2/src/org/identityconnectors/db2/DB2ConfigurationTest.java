/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.db2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DB2Configuration}
 * @author kitko
 *
 */
public class DB2ConfigurationTest {
	private static ThreadLocal<DB2Configuration> cfg = new ThreadLocal<DB2Configuration>();
	private final static Log log = Log.getLog(DB2ConfigurationTest.class);
	private final static String WRONG_ADMIN = "wrongAdmin";
	private final static GuardedString WRONG_PASSWORD = new GuardedString(new char[]{'w','r','o','n','g','P','a','s','s','w','o','r','d'});
	private final static String WRONG_DATABASE = "wrongDatabase";
	private final static String WRONG_HOST = "wrongHost";
	private final static String WRONG_PORT = "666";
	private final static String WRONG_ALIAS = "wrgAls";
    private final static String WRONG_DATASOURCE = "wrongDatasource";
	
	/**
	 * Test validation
	 */
	@Test
	public void testValidateSuc(){
		DB2Configuration testee = createTestConfiguration();
		testee.validate();
	}
	
	static DB2Configuration createTestConfiguration(){
		DB2Configuration conf = null;
		String connType = TestHelpers.getProperty("connType",null);
		if("type4".equals(connType)){
			conf = createTestType4Configuration();
		}
		else if("type2".equals(connType)){
			conf = createTestType2Configuration(DB2Specifics.JCC_DRIVER);
		}
		else{
			throw new IllegalArgumentException("Illegal connType : " + connType);
		}
		if(conf == null){
			throw new IllegalStateException("Configuration not created");
		}
		conf.validate();
		return conf;
	}
	
	private static DB2Configuration createTestType4Configuration(){
		DB2Configuration conf = createDB2Configuration();
		String databaseName = TestHelpers.getProperty("type4.databaseName",null);
		String adminAcoount = TestHelpers.getProperty("type4.adminAccount",null);
		String adminPassword = TestHelpers.getProperty("type4.adminPassword",null);
		String host = TestHelpers.getProperty("type4.host",null);
		String port = TestHelpers.getProperty("type4.port",null);
		conf.setDatabaseName(databaseName);
		conf.setAdminAccount(adminAcoount);
		conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
		conf.setHost(host);
		conf.setPort(port);
		conf.setJdbcDriver(DB2Specifics.JCC_DRIVER);
		return conf;
	}
	
	private static DB2Configuration createTestTypeURLConfiguration(){
		DB2Configuration conf = createDB2Configuration();
		String url = TestHelpers.getProperty("typeURL.url",null);
		String adminAcoount = TestHelpers.getProperty("typeURL.adminAccount",null);
		String adminPassword = TestHelpers.getProperty("typeURL.adminPassword",null);
		String jdbcDriver = TestHelpers.getProperty("typeURL.jdbcDriver",null);
		conf.setURL(url);
		conf.setAdminAccount(adminAcoount);
		conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
		conf.setJdbcDriver(jdbcDriver);
		conf.setPort(null);
		conf.setJdbcSubProtocol(null);
		return conf;
	}
	
	
	/**
	 * Validates and create connection using type4 driver
	 */
	@Test
	public void testType4Configuration(){
		DB2Configuration okConf = createTestType4Configuration();
		okConf.validate();
		Connection conn = okConf.createAdminConnection();
		assertNotNull(conn);
		DB2Configuration failConf = null;
		
		//test fail with wrong admin account
		failConf = okConf.clone();
		failConf.setAdminAccount(null);
		assertValidateFail(failConf,"Validate should fail with null admin account");
		failConf.setAdminAccount(WRONG_ADMIN);
		assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong admin account");
		
		//test fail with wrong admin password
		failConf = okConf.clone();
		failConf.setAdminPassword(null);
        assertValidateFail(failConf,"Validate should fail with null admin password");
        failConf.setAdminPassword(WRONG_PASSWORD);
		assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong admin password");
		
        //test fail with wrong database
		failConf = okConf.clone();
		failConf.setDatabaseName(null);
        assertValidateFail(failConf,"Validate should fail with null database");
        failConf.setDatabaseName(WRONG_DATABASE);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong database");
        
        //test fail with wrong host
        failConf = okConf.clone();
        failConf.setHost(null);
        assertValidateFail(failConf, "Validate should fail with null host");
        failConf.setHost(WRONG_HOST);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong host");
        
        //test fail with wrong port
        failConf = okConf.clone();
        failConf.setPort(null);
        assertValidateFail(failConf, "Validate should fail with null port");
        failConf.setPort(WRONG_PORT);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong port");
		
	}
	
	/**
	 * Validates and create connection using concrete URL
	 */
	@Test
	public void testTypeURLConfiguration(){
		DB2Configuration okConf = createTestTypeURLConfiguration();
		okConf.validate();
		Connection conn = okConf.createAdminConnection();
		assertNotNull(conn);
		DB2Configuration failConf = okConf.clone();
		failConf.setAdminAccount(null);
		assertValidateFail(failConf,"Validate should fail with null admin account");
		failConf = okConf.clone();
		failConf.setDatabaseName("sample");
		assertValidateFail(failConf,"Validate should fail with sample database");
	}
	
	
	
	private void assertValidateFail(DB2Configuration conf,String failMsg){
        try{
            conf.validate();
            fail(failMsg);
        }
        catch(RuntimeException e){
        }
	}
	
	private void assertCreateAdminConnFail(DB2Configuration conf,String failMsg){
        conf.validate();
        try{
            conf.createAdminConnection();
            fail(failMsg);
        }
        catch(RuntimeException e){
        }
	}
	
	
    private static DB2Configuration createTestType2Configuration(String driver){
        String alias = TestHelpers.getProperty("type2.alias",null);
        if(alias == null){
            return null;
        }
        DB2Configuration conf = createDB2Configuration();
        String adminAccount = TestHelpers.getProperty("type2.adminAccount",null);
        String adminPassword = TestHelpers.getProperty("type2.adminPassword",null);
        conf.setDatabaseName(alias);
        conf.setAdminAccount(adminAccount);
        conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
        conf.setJdbcDriver(driver);
        conf.setPort(null);
        return conf;
    }
	
	

	
	/**
	 * Validates and creates connection using type2 driver
	 */
	@Test
	public void testType2Configuration(){
		DB2Configuration conf = createTestType2Configuration(DB2Specifics.JCC_DRIVER);
		//we need having alias on local machine, so we will test only when type2.alias property is set
		if(conf == null){
			conf = createDB2Configuration();
			conf.setDatabaseName("myDBAlias");
			conf.setAdminAccount("dummy");
			conf.setAdminPassword(new GuardedString());
			conf.setJdbcDriver(DB2Specifics.JCC_DRIVER);
			conf.setPort(null);
			try{
				conf.validate();
			}
			catch(Exception e){
				handleType2Exception(e);
			}
		}
		else{
			try{
				conf.validate();
				final Connection conn = conf.createAdminConnection();
				assertNotNull(conn);
			}
			catch(Exception e){
				handleType2Exception(e);
			}
		}
		DB2Configuration failConf = conf.clone();
		
		//Test wrong user name
        failConf = conf.clone();
        failConf.setAdminAccount(null);
        assertValidateFail(failConf,"Validate should fail with null admin account");
        failConf.setAdminAccount(WRONG_ADMIN);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong admin account");
        
        //test fail with wrong admin password
        failConf = conf.clone();
        failConf.setAdminPassword(null);
        assertValidateFail(failConf,"Validate should fail with null admin password");
        failConf.setAdminPassword(WRONG_PASSWORD);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong admin password");
        
        //Test wrong alias name
        failConf = conf.clone();
        failConf.setDatabaseName(null);
        assertValidateFail(failConf,"Validate should fail with null aliasname");
        failConf.setDatabaseName(WRONG_ALIAS);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong alias name");
		
	}
	
    /**
     * Validates and create connection using type2 legacy driver
     */
    @Test
    public void testType2LegacyConfiguration(){
        DB2Configuration conf = createTestType2Configuration(DB2Specifics.CLI_LEGACY_DRIVER);
        //we need having alias on local machine, so we will test only when type2.alias property is set
        if(conf == null){
            conf = createDB2Configuration();
            conf.setDatabaseName("myDBAlias");
            conf.setAdminAccount("dummy");
            conf.setAdminPassword(new GuardedString());
            conf.setJdbcDriver(DB2Specifics.CLI_LEGACY_DRIVER);
            conf.setPort(null);
            try{
                conf.validate();
            }
            catch(Exception e){
                handleType2Exception(e);
            }
        }
        else{
            try{
                conf.validate();
                final Connection conn = conf.createAdminConnection();
                assertNotNull(conn);
            }
            catch(Exception e){
                handleType2Exception(e);
            }
        }
        DB2Configuration failConf = conf.clone();
        
        //Test wrong user name
        failConf = conf.clone();
        failConf.setAdminAccount(null);
        assertValidateFail(failConf,"Validate should fail with null admin account");
        failConf.setAdminAccount(WRONG_ADMIN);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong admin account");
        
        //test fail with wrong admin password
        failConf = conf.clone();
        failConf.setAdminPassword(null);
        assertValidateFail(failConf,"Validate should fail with null admin password");
        failConf.setAdminPassword(WRONG_PASSWORD);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong admin password");
        
        //Test wrong alias name
        failConf = conf.clone();
        failConf.setDatabaseName(null);
        assertValidateFail(failConf,"Validate should fail with null aliasname");
        failConf.setDatabaseName(WRONG_ALIAS);
        assertCreateAdminConnFail(failConf, "CreateAdminConnection should fail on wrong alias name");
        
    }
	
	
	
	private void handleType2Exception(Exception e){
		boolean rethrow = true;
		if(e.getCause() instanceof SQLException){
			if(((SQLException)e.getCause()).getErrorCode() == -4472){
				//UnsatisfiedLinkError is not cause of SQLException , why db2 guys ???
				if(e.getCause().toString().contains("UnsatisfiedLinkError")){
					//This will happen when having driver on classpath, but db2 client is not installed
					rethrow = false;
					log.warn(e,"Cannot load db2 type2 driver, probably db2client not installed");
				}
			}
		}
		if(rethrow){
			throw ConnectorException.wrap(e);
		}
		
	}
	
	private final String[] dsJNDIEnv = new String[]{"java.naming.factory.initial=" + MockContextFactory.class.getName()};
	
	private DB2Configuration createDataSourceConfiguration(){
		DB2Configuration conf = createDB2Configuration();
		conf.setDataSource("testDS");
		conf.setAdminAccount("user");
		conf.setAdminPassword(new GuardedString(new char[]{'t'}));
		conf.setDsJNDIEnv(dsJNDIEnv);
		conf.setPort(null);
		conf.setJdbcDriver(null);
		conf.setJdbcSubProtocol(null);
		return conf;
	}
	

	/**
	 * Test getting Connection from DS
	 */
	@Test
	public void testDataSourceConfiguration(){
		DB2Configuration conf = createDataSourceConfiguration();
		//set to thread local
		cfg.set(conf);
		assertArrayEquals(conf.getDsJNDIEnv(), dsJNDIEnv);
		conf.validate();
		Connection conn = conf.createAdminConnection();
		conf.setAdminAccount(null);
		conf.setAdminPassword(null);
		conf.validate();
		conn = conf.createAdminConnection();
		assertNotNull(conn);
		
		DB2Configuration failConf = conf.clone();
		failConf.setDataSource(null);
		assertValidateFail(failConf, "Validate should fail for null datasource");
		failConf.setDataSource(WRONG_DATASOURCE);
		assertCreateAdminConnFail(failConf, "CreateAdminConnection with wrong datasource should fail");
	}
	
	/**
	 * Simple test of clone 
	 */
	@Test
	public void testClone(){
	    DB2Configuration cfg = createTestType4Configuration();
	    DB2Configuration clone = cfg.clone();
	    assertNotSame(cfg,clone);
	    assertEquals(cfg.getAdminAccount(), clone.getAdminAccount());
	}
	
	private static DB2Configuration createDB2Configuration(){
	    DB2Configuration cfg = new DB2Configuration();
	    cfg.setConnectorMessages(TestHelpers.createDummyMessages());
	    return cfg;
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
				if(cfg.get().getAdminAccount() == null){
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
