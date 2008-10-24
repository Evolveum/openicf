/**
 * 
 */
package org.identityconnectors.db2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.Test;

/**
 * Tests for {@link DB2Configuration}
 * @author kitko
 *
 */
public class DB2ConfigurationTest {
	/** Test loading of exclude names from resource */
	@Test
	public void testLoadExcludeNames(){
		final Collection<String> excludeNames = DB2Configuration.getExcludeNames();
		assertNotNull(excludeNames);
		assertTrue("Exclude names must contain COUNT",excludeNames.contains("COUNT"));
		assertTrue("Exclude names must contain LOCAL",excludeNames.contains("LOCAL"));
		assertTrue("Must contain at least 50 exclude names",excludeNames.size() >= 50);
	}
	
	@Test
	public void testValidate(){
		DB2Configuration testee = createTestConfiguration();
		testee.validate();
	}
	
	public static DB2Configuration createTestConfiguration(){
		DB2Configuration conf = new DB2Configuration();
		String host = DB2ConnectorTest.getTestRequiredProperty("host.connector.string");
		String port = DB2ConnectorTest.getTestRequiredProperty("port.connector.integer");
		String databaseName = DB2ConnectorTest.getTestRequiredProperty("databaseName.connector.string");
		String adminAcoount = DB2ConnectorTest.getTestRequiredProperty("adminAccount.connector.string");
		String adminPassword = DB2ConnectorTest.getTestRequiredProperty("adminPassword.connector.string");
		String jdbcDriver = TestHelpers.getProperty("jdbcDriver.connector.string",DB2Connection.APP_DRIVER);
		conf.setHost(host);
		conf.setPort(port);
		conf.setAdminAccount(adminAcoount);
		conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
		conf.setDatabaseName(databaseName);
		conf.setJdbcDriver(jdbcDriver);
		
		return conf;
	}
	
	
}
