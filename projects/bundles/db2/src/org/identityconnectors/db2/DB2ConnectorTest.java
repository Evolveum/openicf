/**
 * 
 */
package org.identityconnectors.db2;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.*;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.*;


/**
 * Test for {@link DB2Connector}
 * @author kitko
 *
 */
public class DB2ConnectorTest {
	private static DB2Configuration testConf;
    private static ConnectorFacade facade;
    private static String findUser = TestHelpers.getProperty("findUser","TEST");

	
	
	@BeforeClass
	public static void setupClass(){
		testConf = DB2ConfigurationTest.createTestConfiguration();
		facade = getFacade();
	}
	
    private static ConnectorFacade getFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(DB2Connector.class, testConf);
        return factory.newInstance(apiCfg);
    }
    
    @Test
    public void testSchemaApi() {
        Schema schema = facade.schema();
        // Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(1, objectInfos.size());
        ObjectClassInfo objectInfo = (ObjectClassInfo) objectInfos.toArray()[0];
        assertNotNull(objectInfo);
        // the object class has to ACCOUNT_NAME
        assertEquals(ObjectClass.ACCOUNT_NAME, objectInfo.getType());
        // iterate through AttributeInfo Set
        Set<AttributeInfo> attInfos = objectInfo.getAttributeInfo();
        
        assertNotNull(AttributeInfoUtil.find(Name.NAME, attInfos));
        assertNotNull(AttributeInfoUtil.find(OperationalAttributes.PASSWORD_NAME, attInfos));
    }
    
	
	
	@Test
	public void testAuthenticateSuc(){
		String username = getTestRequiredProperty("testUser");
		String password = getTestRequiredProperty("testPassword");
		Map<String, Object> emptyMap = Collections.emptyMap();
		facade.authenticate(username, new GuardedString(password.toCharArray()),new OperationOptions(emptyMap));
	}
	
	@Test(expected=RuntimeException.class)
	public void testAuthenticateFail(){
		String username = "undefined";
		String password = "testPassword";
		Map<String, Object> emptyMap = Collections.emptyMap();
		facade.authenticate(username, new GuardedString(password.toCharArray()),new OperationOptions(emptyMap));
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
		String username = getTestRequiredProperty("testUser");
		Map<String, Object> emptyMap = Collections.emptyMap();
		Set<Attribute> attributes = new HashSet<Attribute>();
		attributes.add(new Name(username));
		attributes.add(AttributeBuilder.buildPassword(new char[]{'a','b','c'}));
		facade.create(ObjectClass.ACCOUNT, attributes, new OperationOptions(emptyMap));
		//find user
		Uid uid = findUser(username);
		assertNotNull(uid);
		
	}
	
	private Uid findUser(String name){
        final Uid expected = new Uid(name);
        FindUidObjectHandler handler = new FindUidObjectHandler(expected);
        final Uid actual = handler.getUid();
        return actual;
	}
	
    @Test
    public void testFindModelUserByUid() {
        final Uid expected = new Uid(findUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(expected), handler, null);
        assertTrue("The testuser was not found", handler.found);
        final Uid actual = handler.getUid();
        assertNotNull(actual);
        assertTrue(actual.is(expected.getName()));  
     }
    
    @Test
    public void testFindModelUserByEndWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, findUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(findUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EndsWithFilter(expected), handler, null);
        assertTrue("The user was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",findUser, AttributeUtil.getAsStringValue(actual.getName()));
     }


    @Test
    public void testFindModelUserByStartWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, findUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(findUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new StartsWithFilter(expected), handler, null);
        assertTrue("The user was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",findUser, AttributeUtil.getAsStringValue(actual.getName()));
     }
    
	
    
    private static class FindUidObjectHandler implements ResultsHandler {
        private ConnectorObject connectorObject = null;
        private boolean found = false;
        private final Uid uid;
        
        /**
         * @param uid
         */
        public FindUidObjectHandler(Uid uid) {
            this.uid = uid;
        }
        
        /**
         * getter method
         * @return object value
         */
        public ConnectorObject getConnectorObject() {
            return connectorObject;
        }
        
        /**
         * @return the uid
         */
        public Uid getUid() {
            return uid;
        }

        public boolean handle(ConnectorObject obj) {
            System.out.println("Object: " + obj);
            if (obj.getUid().equals(uid)) {
                found = true;
                this.connectorObject = obj;
                return false;
            }
            return true;
        }
    }    
    
	
	

	
}
