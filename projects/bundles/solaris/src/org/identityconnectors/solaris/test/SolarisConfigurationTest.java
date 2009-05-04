package org.identityconnectors.solaris.test;

import junit.framework.Assert;

import org.identityconnectors.solaris.SolarisConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisConfigurationTest {
    
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
    
    @Test
    public void testGoodConfiguration() {
        try {
            SolarisConfiguration config = getConfig();
            // no IllegalArgumentException should be thrown
            config.validate();
        } catch (IllegalArgumentException ex) {
            Assert
                    .fail("no IllegalArgumentException should be thrown for valid configuration: "
                            + ex.getMessage());
        }
    }
    
    /* **************** "MISSING" PROPERTY TESTS ***************** */
    @Test(expected = IllegalArgumentException.class)
    public void testMissingUsername() {
        SolarisConfiguration config = getConfig();
        config.setUserName(null);
        config.validate();
        Assert.fail("Configuration allowed a null admin username.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingPassword() {
        SolarisConfiguration config = getConfig();
        config.setPassword(null);
        config.validate();
        Assert.fail("Configuration allowed a null password.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingHostname() {
        SolarisConfiguration config = getConfig();
        config.setHostNameOrIpAddr(null);
        config.validate();
        Assert.fail("Configuration allowed a null hostname.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingPort() {
        SolarisConfiguration config = getConfig();
        config.setPort(null);
        config.validate();
        Assert.fail("Configuration allowed a null port.");
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
