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

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.PropertiesResolver;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.*;

/**
 * Tests for {@link DB2Configuration}
 * @author kitko
 *
 */
public class DB2ConfigurationTest {
	private static ThreadLocal<DB2Configuration> cfg = new ThreadLocal<DB2Configuration>();
	private final static Log log = Log.getLog(DB2ConfigurationTest.class);
	
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
			conf = createTestType2Configuration();
		}
		else{
			throw new IllegalArgumentException("Ilegall connType " + connType);
		}
		if(conf == null){
			throw new IllegalStateException("Configuration not created");
		}
		conf.validate();
		return conf;
	}
	
	private static DB2Configuration createTestType4Configuration(){
		DB2Configuration conf = new DB2Configuration();
		Properties properties = TestHelpers.getProperties();
		properties = PropertiesResolver.resolveProperties(properties);
		String databaseName = properties.getProperty("type4.databaseName",null);
		String adminAcoount = properties.getProperty("type4.adminAccount",null);
		String adminPassword = properties.getProperty("type4.adminPassword",null);
		String host = properties.getProperty("type4.host");
		String port = properties.getProperty("type4.port");
		conf.setDatabaseName(databaseName);
		conf.setAdminAccount(adminAcoount);
		conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
		conf.setHost(host);
		conf.setPort(port);
		conf.setJdbcDriver(DB2Specifics.JCC_DRIVER);
		return conf;
	}
	
	/**
	 * Validates and create connection using type4 driver
	 */
	@Test
	public void testType4Configuration(){
		DB2Configuration conf = createTestType4Configuration();
		conf.validate();
		Connection conn = conf.createAdminConnection();
		assertNotNull(conn);
		conf.setAliasName("sample");
		try{
			conf.validate();
			fail("Cannot set alias , when having enough info to connect using type4 driver");
		}
		catch(Exception e){}
	}
	
	
	private static DB2Configuration createTestType2Configuration(){
		Properties properties = TestHelpers.getProperties();
		properties = PropertiesResolver.resolveProperties(properties);
		String alias = properties.getProperty("type2.alias");
		if(alias == null){
			return null;
		}
		DB2Configuration conf = new DB2Configuration();
		String adminAccount = properties.getProperty("type2.adminAccount");
		String adminPassword = properties.getProperty("type2.adminPassword");
		conf.setAliasName(alias);
		conf.setAdminAccount(adminAccount);
		conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
		conf.setJdbcDriver(DB2Specifics.APP_DRIVER);
		conf.setPort(null);
		return conf;
	}

	
	/**
	 * Validates and create connection using type2 driver
	 */
	@Test
	public void testType2Configuration(){
		DB2Configuration conf = createTestType2Configuration();
		//we need having alias on local machine, so we will test only when type2.alias property is set
		if(conf == null){
			conf = new DB2Configuration();
			conf.setAliasName("myDBAlias");
			conf.setAdminAccount("dummy");
			conf.setAdminPassword(new GuardedString());
			conf.setJdbcDriver(DB2Specifics.APP_DRIVER);
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
		DB2Configuration conf = new DB2Configuration();
		conf.setDataSource("testDS");
		conf.setAdminAccount("user");
		conf.setAdminPassword(new GuardedString(new char[]{'t'}));
		conf.setDsJNDIEnv(dsJNDIEnv);
		conf.setPort(null);
		conf.setJdbcDriver(null);
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
