package org.identityconnectors.solaris.test;

import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisConnectionTest {
    
    private static SolarisConfiguration config;
    
    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        config = null;
    }
    
    /** test connection to the configuration given by default credentials (build.groovy) */
    @Test
    public void testGoodConnection() {
        SolarisConfiguration config = getConfig();
        SolarisConnector connector = SolarisTestCommon.createConnector(config);
        try {
            connector.checkAlive();
        } finally {
            connector.dispose();
        }
    }
    
    /* ************* AUXILIARY METHODS *********** */

    /**
     * create configuration based on Unit test account
     * @return
     */
    private SolarisConfiguration getConfig() {
        return config;
    }
}
