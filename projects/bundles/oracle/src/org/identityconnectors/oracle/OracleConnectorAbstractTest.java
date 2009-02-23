package org.identityconnectors.oracle;

import org.identityconnectors.framework.api.*;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.BeforeClass;

/** Abstract test class for Oracle connector and its operations */
public class OracleConnectorAbstractTest {
    protected static OracleConfiguration testConf;
    protected static ConnectorFacade facade;
    protected static OracleConnector connector;

    /**
     * Setup for all tests
     */
    @BeforeClass
    public static void setupClass(){
        testConf = OracleConfigurationTest.createSystemConfiguration();
        facade = createFacade(testConf);
        connector = createTestConnector();
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
    

}
