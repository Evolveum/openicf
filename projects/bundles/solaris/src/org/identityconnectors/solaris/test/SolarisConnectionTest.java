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
