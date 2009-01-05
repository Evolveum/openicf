package org.identityconnectors.solaris;


import junit.framework.Assert;

import org.identityconnectors.framework.test.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisConnectorTest {

    private static String HOST_NAME;
    private static String SYSTEM_PASSWORD;
    private static String SYSTEM_USER;
    private static String HOST_PORT;
    
    @Before
    public void setUp() throws Exception {
        HOST_NAME = TestHelpers.getProperty("host", null);
        SYSTEM_PASSWORD = TestHelpers.getProperty("password", null);
        SYSTEM_USER = TestHelpers.getProperty("user", null);
        HOST_PORT = TestHelpers.getProperty("port", null);
        
        Assert.assertNotNull(HOST_NAME);
        Assert.assertNotNull(SYSTEM_PASSWORD);
        Assert.assertNotNull(SYSTEM_USER);
        Assert.assertNotNull(HOST_PORT);
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testConnection() {
        
    }
    
    public SolarisConfiguration createConfig() {
        
        return null;
        
    }
    
    

}
