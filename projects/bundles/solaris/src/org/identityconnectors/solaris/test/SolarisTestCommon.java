package org.identityconnectors.solaris.test;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.test.common.TestHelpers;

/**
 * 
 * Create a solaris test configuration (for unit tests only)
 *
 */
public class SolarisTestCommon {
    
    /**
     * create a new solaris connector and initialize it with the given configuration
     * @param config the configuration to be used.
     */
    static SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        
        return conn;
    }
    
    static SolarisConfiguration createConfiguration() {
        // names of properties in the property file (build.groovy)
        final String PROP_HOST = "host";
        final String PROP_SYSTEM_PASSWORD = "pass";
        final String PROP_SYSTEM_USER = "user";
        final String PROP_PORT = "port";
        final String PROP_CONN_TYPE = "connectionType";
        
        // set the credentials
        final String HOST_NAME = TestHelpers.getProperty(PROP_HOST, null);
        final String SYSTEM_PASSWORD = TestHelpers.getProperty(PROP_SYSTEM_PASSWORD, null);
        final String SYSTEM_USER = TestHelpers.getProperty(PROP_SYSTEM_USER, null);
        final String PORT = TestHelpers.getProperty(PROP_PORT, null);
        final String CONN_TYPE = TestHelpers.getProperty(PROP_CONN_TYPE, null);
        
        String msg = "%s must be provided in build.groovy";
        Assert.assertNotNull(String.format(msg, PROP_HOST), HOST_NAME);
        Assert.assertNotNull(String.format(msg, PROP_SYSTEM_PASSWORD), SYSTEM_PASSWORD);
        Assert.assertNotNull(String.format(msg, PROP_SYSTEM_USER), SYSTEM_USER);
        Assert.assertNotNull(String.format(msg, PROP_PORT), PORT);
        Assert.assertNotNull(String.format(msg, PROP_CONN_TYPE), CONN_TYPE);
        
        // save configuration
        SolarisConfiguration config = new SolarisConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setPort(Integer.parseInt(PORT));
        config.setUserName(SYSTEM_USER);
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setConnectionType(CONN_TYPE);
        
        return config;
    }
}
