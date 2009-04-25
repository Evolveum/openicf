package org.identityconnectors.solaris.test;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisTestAuthenticate {
    
    private SolarisConfiguration config;

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
    
    @Test
    public void testAuthenticateApiOp() {
        SolarisConnector connector = SolarisTestCommon.createConnector(config);
        GuardedString password = config.getPassword();
        String username = config.getUserName();
        connector.authenticate(null, username, password, null);
    }
    
    /**
     * test to authenticate with invalid credentials.
     */
    @Test (expected=ConnectorException.class)
    public void testAuthenticateApiOpInvalidCredentials() {
        SolarisConnector connector = SolarisTestCommon.createConnector(config);
        GuardedString password = new GuardedString(
                "WRONG_PASSWORD_FOOBAR2135465".toCharArray());
        String username = config.getUserName();
        connector.authenticate(null, username, password, null);
    }
}
