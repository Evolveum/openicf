/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.test;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.junit.Ignore;
import org.junit.Test;

public class SolarisConfigurationTest {
    @Test
    public void testGoodConfiguration() {
        SolarisConfiguration config = getConfiguration();
        config.validate();
        // no IllegalArgumentException should be thrown for valid configuration
    }
    
    /* **************** "MISSING" PROPERTY TESTS ***************** */
    @Test(expected = ConfigurationException.class)
    public void testMissingUsername() {
        SolarisConfiguration config = getConfiguration();
        config.setLoginUser(null);
        config.validate();
        Assert.fail("Configuration allowed a null admin username.");
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingPassword() {
        SolarisConfiguration config = getConfiguration();
        config.setPassword(null);
        config.validate();
        Assert.fail("Configuration allowed a null password.");
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingHostname() {
        SolarisConfiguration config = getConfiguration();
        config.setHost(null);
        config.validate();
        Assert.fail("Configuration allowed a null hostname.");
    }
    
    private SolarisConfiguration getConfiguration() {
        return SolarisTestCommon.createConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void testMissingPort() {
        SolarisConfiguration config = getConfiguration();
        config.setPort(-1);
        config.validate();
        Assert.fail("Configuration allowed a null port.");
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingCredentials() {
        SolarisConfiguration config = getConfiguration();
        config.setRootUser("root");
        config.setCredentials(null);
        config.validate();
        Assert.fail("Configuration allowed a null credential when rootUser was defined");
    }
    
    @Test
    public void testIsSuAuthorization() {
        GuardedString dummyPassword = new GuardedString("".toCharArray());
        SolarisConfiguration config = getConfiguration();
        config.setLoginUser("loginuser");
        config.setPassword(dummyPassword);
        Assert.assertFalse(config.isSuAuthorization());
        
        config = getConfiguration();
        config.setLoginUser("loginuser");
        config.setPassword(dummyPassword);
        config.setRootUser("rootUser");
        config.setCredentials(dummyPassword);
        Assert.assertTrue(config.isSuAuthorization());
        
        config = getConfiguration();
        final String loginUser = "sameLoginUser";
        config.setLoginUser(loginUser);
        config.setPassword(dummyPassword);
        config.setRootUser(loginUser);
        config.setCredentials(dummyPassword);
        Assert.assertFalse(config.isSuAuthorization());
    }
    
    @Test @Ignore // TODO
    public void testGetMessageProperty() {
        String result = getConfiguration().getMessage("SOLARIS");
        Assert.assertTrue(result.equals("Solaris"));
    }
}
