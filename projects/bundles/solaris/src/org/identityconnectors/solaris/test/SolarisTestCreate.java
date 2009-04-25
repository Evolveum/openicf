package org.identityconnectors.solaris.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisTestCreate {
    
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
    
    /**
     * creates a sample user
     */
    @Test
    public void testCreate() {
        SolarisConnector connector = createConnector(config);
        
        Set<Attribute> attrs = initSampleUser();
        connector.create(ObjectClass.ACCOUNT, attrs, null);
    }
    
    /* ************* AUXILIARY METHODS *********** */
    
    /** fill in sample user/password for sample user used in create */
    private Set<Attribute> initSampleUser() {
        String msg = "test property '%s' should not be null";
        
        Set<Attribute> res = new HashSet<Attribute>();
        
        String sampleUser = TestHelpers.getProperty("sampleUser", null);
        Assert.assertNotNull(String.format(msg, "sampleUser"), sampleUser);
        res.add(AttributeBuilder.build(Name.NAME, sampleUser));
        
        String samplePasswd = TestHelpers.getProperty("samplePasswd", null);
        Assert.assertNotNull(String.format(msg, "samplePasswd"), samplePasswd);
        res.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString(samplePasswd.toCharArray())));
        
        return res;
    }
    
    /**
     * create a new solaris connector and initialize it with the given configuration
     * @param config the configuration to be used.
     */
    private SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        
        return conn;
    }
}
