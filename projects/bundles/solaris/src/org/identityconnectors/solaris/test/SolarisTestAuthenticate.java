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
