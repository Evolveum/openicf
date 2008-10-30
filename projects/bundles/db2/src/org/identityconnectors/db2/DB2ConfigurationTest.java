/**
 * 
 */
package org.identityconnectors.db2;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.Test;

/**
 * Tests for {@link DB2Configuration}
 * @author kitko
 *
 */
public class DB2ConfigurationTest {
	
	@Test
	public void testValidate(){
		DB2Configuration testee = createTestConfiguration();
		testee.validate();
	}
	
	static DB2Configuration createTestConfiguration(){
		DB2Configuration conf = new DB2Configuration();
		String host = DB2ConnectorTest.getTestRequiredProperty("host.connector.string");
		String port = DB2ConnectorTest.getTestRequiredProperty("port.connector.integer");
		String databaseName = DB2ConnectorTest.getTestRequiredProperty("databaseName.connector.string");
		String adminAcoount = DB2ConnectorTest.getTestRequiredProperty("adminAccount.connector.string");
		String adminPassword = DB2ConnectorTest.getTestRequiredProperty("adminPassword.connector.string");
		String jdbcDriver = TestHelpers.getProperty("jdbcDriver.connector.string",DB2Specifics.APP_DRIVER);
		conf.setHost(host);
		conf.setPort(port);
		conf.setAdminAccount(adminAcoount);
		conf.setAdminPassword(new GuardedString(adminPassword.toCharArray()));
		conf.setDatabaseName(databaseName);
		conf.setJdbcDriver(jdbcDriver);
		
		return conf;
	}
	
	
}
