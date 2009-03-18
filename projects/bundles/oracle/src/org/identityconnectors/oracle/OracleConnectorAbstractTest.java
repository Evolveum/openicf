package org.identityconnectors.oracle;

import org.identityconnectors.framework.api.*;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.*;

/** Abstract test class for Oracle connector and its operations */
public class OracleConnectorAbstractTest {
    protected static OracleConfiguration testConf;
    protected static ConnectorFacade facade;
    protected static OracleConnector connector;
    protected static OracleUserReader userReader;

    /**
     * Setup for all tests
     */
    @BeforeClass
    public static void setupClass(){
        testConf = OracleConfigurationTest.createSystemConfiguration();
        facade = createFacade(testConf);
        connector = createTestConnector();
        userReader = new OracleUserReader(connector.getAdminConnection());
    }
    
    private static ConnectorFacade createFacade(OracleConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(OracleConnector.class, conf);
        return factory.newInstance(apiCfg);
    }
    
    protected static OracleConnector createTestConnector(){
        OracleConnector oc = new OracleConnector();
        oc.init(testConf);
        return oc;
    }
    
    protected static void assertEqualsIgnoreCase(String expected,String actual){
        Assert.assertEquals(expected.toUpperCase(), actual.toUpperCase());
    }
    

}
