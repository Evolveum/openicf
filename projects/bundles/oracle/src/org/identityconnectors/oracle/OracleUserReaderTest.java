/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.*;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.objects.*;
import org.junit.*;

/**
 * @author kitko
 *
 */
public class OracleUserReaderTest {
    private static Connection conn;
    private static OracleConnector connector;
    private static OracleUserReader userReader;
    
    /**
     * Setup connection
     */
    @BeforeClass
    public static void beforeClass(){
        final OracleConfiguration cfg = OracleConfigurationTest.createSystemConfiguration();
        conn = cfg.createAdminConnection();
        connector = new OracleConnector();
        userReader = new OracleUserReader(conn);
        connector.init(cfg);
    }
    
    /**
     * Close connection 
     */
    @AfterClass
    public static void afterClass(){
        SQLUtil.closeQuietly(conn);
    }
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorHelper#userExist(java.sql.Connection, java.lang.String)}.
     */
    @Test
    public void testUserExist() {
        String user = "testUser";
        boolean userExist = userReader.userExist(user);
        if(userExist){
            connector.delete(ObjectClass.ACCOUNT, new Uid(user), new OperationOptionsBuilder().build());
            assertFalse("User should not exist after delete", userReader.userExist(user));
        }
        else{
            Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
            Attribute name = new Name(user);
            Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
            connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), new OperationOptionsBuilder().build());
            assertTrue("User should exist after create", userReader.userExist(user));
            connector.delete(ObjectClass.ACCOUNT, new Uid(user), new OperationOptionsBuilder().build());
            assertFalse("User should not exist after delete", userReader.userExist(user));
        }
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorHelper#readUserRecords(java.sql.Connection, java.util.List)}.
     */
    @Test
    public void testReadUserRecords() {
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
        if(!userReader.userExist("user1")){
            connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,new Name("user1"),password), new OperationOptionsBuilder().build());
        }
        if(!userReader.userExist("user2")){
            connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,new Name("user2"),password), new OperationOptionsBuilder().build());
        }
        final Collection<UserRecord> records = userReader.readUserRecords(Arrays.asList("user1","user2","user3"));
        assertEquals("Read should return 2 users",2,records.size());
        
        UserRecord record1 = OracleUserReader.createUserRecordMap(records).get("user1");
        assertNotNull(record1);
        assertEquals("user1",record1.userName);
        assertNotNull(record1.userId);
        assertNotNull(record1.defaultTableSpace);
        assertNotNull(record1.temporaryTableSpace);
        assertNotNull(record1.createdDate);
        assertEquals("OPEN",record1.status);
        
        UserRecord record2 = OracleUserReader.createUserRecordMap(records).get("user2");
        assertNotNull(record2);
        assertEquals("user2",record2.userName);
        assertNotNull(record2.userId);
        assertNotNull(record2.defaultTableSpace);
        assertNotNull(record2.temporaryTableSpace);
        assertNotNull(record2.createdDate);
        assertEquals("OPEN",record2.status);
        
        record1 = userReader.readUserRecord("user1");
        assertNotNull(record1);
        assertEquals("user1",record1.userName);
        assertNotNull(record1.userId);
        assertNotNull(record1.defaultTableSpace);
        assertNotNull(record1.temporaryTableSpace);
        assertNotNull(record1.createdDate);
        assertEquals("OPEN",record1.status);
        
        connector.delete(ObjectClass.ACCOUNT, new Uid("user1"), null);
        connector.delete(ObjectClass.ACCOUNT, new Uid("user2"), null);
        
        
    }


}
