package org.identityconnectors.solaris;


import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisConnectorTest {

    private static String HOST_NAME = null;
    private static String SYSTEM_PASSWORD = null;
    private static String SYSTEM_USER = null;
    private static String HOST_PORT = null;
    
    @Before
    public void setUp() throws Exception {
        HOST_NAME = TestHelpers.getProperty("host", null);
        SYSTEM_PASSWORD = TestHelpers.getProperty("pass", null);
        SYSTEM_USER = TestHelpers.getProperty("user", null);
        HOST_PORT = TestHelpers.getProperty("port", null);
        
        String msg = "%s must be provided in build.groovy";
        Assert.assertNotNull(String.format(msg, "HOST_NAME"), HOST_NAME);
        Assert.assertNotNull(String.format(msg, "SYSTEM_PASSWORD"), SYSTEM_PASSWORD);
        Assert.assertNotNull(String.format(msg, "SYSTEM_USER"), SYSTEM_USER);
        Assert.assertNotNull(String.format(msg, "HOST_PORT"), HOST_PORT);
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testConnection() {
        SolarisConfiguration config = createConfig();
        SolarisConnector info = createConnector(config);
        try {
            info.checkAlive();
        } finally {
            info.dispose();
        }
    }
    
    private SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        return conn;
    }

    private SolarisConfiguration createConfig() {
        SolarisConfiguration config = new SolarisConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setPort(HOST_PORT);
        config.setUserName(SYSTEM_USER);
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        
        return config;
    }
}
