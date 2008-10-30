/**
 * 
 */
package org.identityconnectors.db2;

import java.sql.Connection;
import java.util.*;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test for {@link DB2Connector}
 * @author kitko
 *
 */
public class DB2ConnectorTest {
	private static DB2Configuration testConf;
	
	
	@BeforeClass
	public static void setupClass(){
		testConf = DB2ConfigurationTest.createTestConfiguration();
	}
	
	@Test
	public void testAuthenticateSuc(){
		DB2Connector connector = new DB2Connector();
		connector.init(testConf);
		String username = getTestRequiredProperty("testUser");
		String password = getTestRequiredProperty("testPassword");
		Map<String, Object> emptyMap = Collections.emptyMap();
		connector.authenticate(username, new GuardedString(password.toCharArray()),new OperationOptions(emptyMap));
	}
	
	@Test(expected=RuntimeException.class)
	public void testAuthenticateFail(){
		DB2Connector connector = new DB2Connector();
		connector.init(testConf);
		String username = "undefined";
		String password = "testPassword";
		Map<String, Object> emptyMap = Collections.emptyMap();
		connector.authenticate(username, new GuardedString(password.toCharArray()),new OperationOptions(emptyMap));
	}

	static Connection createTestConnection() throws Exception{
		DB2Configuration conf = DB2ConfigurationTest.createTestConfiguration();
		return DB2Specifics.createDB2Connection(DB2Specifics.APP_DRIVER,
				conf.getHost(), conf.getPort(), "db2", conf.getDatabaseName(),conf.getAdminAccount(),
				conf.getAdminPassword());
	}

	static String getTestRequiredProperty(String name){
	    String value = TestHelpers.getProperty(name,null);
	    if(value == null){
	        throw new IllegalArgumentException("Property named [" + name + "] is not defined");
	    }
	    return value;
	}
	
	@Test
	public void testCreate(){
		DB2Connector connector = new DB2Connector();
		connector.init(testConf);
		String username = getTestRequiredProperty("testUser");
		Map<String, Object> emptyMap = Collections.emptyMap();
		Set<Attribute> attributes = new HashSet<Attribute>();
		attributes.add(new Name(username));
		attributes.add(AttributeBuilder.buildPassword(new char[]{'a','b','c'}));
		connector.create(ObjectClass.ACCOUNT, attributes, new OperationOptions(emptyMap));
		
	}
	
	

	
}
