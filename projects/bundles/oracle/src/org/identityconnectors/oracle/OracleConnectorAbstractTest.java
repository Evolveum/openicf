package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;

/** Abstract test class for Oracle connector and its operations */
@Ignore
public abstract class OracleConnectorAbstractTest {
    protected static OracleConfiguration testConf;
    protected static ConnectorFacade facade;
    protected static OracleConnectorImpl connector;
    protected static OracleUserReader userReader;

    /**
     * Setup for all tests
     */
    @BeforeClass
    public static void setupClass(){
        testConf = OracleConfigurationTest.createSystemConfiguration();
        facade = createFacade(testConf);
        connector = createTestConnector();
        userReader = new OracleUserReader(connector.getAdminConnection(),TestHelpers.createDummyMessages());
    }
    
    @AfterClass
    public static void afterClass(){
    	connector.dispose();
    }
    
    private static ConnectorFacade createFacade(OracleConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(OracleConnector.class, conf);
        return factory.newInstance(apiCfg);
    }
    
    protected static OracleConnectorImpl createTestConnector(){
        OracleConnectorImpl oc = new OracleConnectorImpl();
        oc.init(testConf);
        return oc;
    }
    
    protected static void assertEqualsIgnoreCase(String expected,String actual){
        Assert.assertEquals(expected.toUpperCase(), actual.toUpperCase());
    }
    
    protected static String findDefDefaultTS(Connection conn) throws SQLException{
        return getTestUserRecord(conn).getDefaultTableSpace();
    }
    
    protected static String findDefTempTS(Connection conn) throws SQLException{
        return getTestUserRecord(conn).getTemporaryTableSpace();
    }
    
    private static UserRecord getTestUserRecord(Connection conn) throws SQLException{
        String testUser = "TEST_TS";
        if(!userReader.userExist(testUser)){
            Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
            Attribute name = new Name(testUser);
            GuardedString password = new GuardedString("hello".toCharArray());
            Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
            testUser = connector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute), null).getUidValue();
        }
        UserRecord record = userReader.readUserRecord(testUser);
        connector.delete(ObjectClass.ACCOUNT, new Uid(testUser),null);
        return record;
    }
    
    protected static List<String> findAllDefTS(Connection conn) throws SQLException{
    	List<Object[]> rows = SQLUtil.selectRows(conn, "select TABLESPACE_NAME from DBA_TABLESPACES where CONTENTS='PERMANENT'");
    	List<String> results = new ArrayList<String>();
    	for(Object[] row : rows){
    		String tabSpace = (String) row[0]; 
    		results.add(tabSpace);
    	}
    	return results;
    }
    
    protected static List<String> findAllTempTS(Connection conn) throws SQLException{
    	List<Object[]> rows = SQLUtil.selectRows(conn, "select TABLESPACE_NAME from DBA_TABLESPACES where CONTENTS='TEMPORARY'");
    	List<String> results = new ArrayList<String>();
    	for(Object[] row : rows){
    		String tabSpace = (String) row[0]; 
    		results.add(tabSpace);
    	}
    	return results;
    }
    

}
