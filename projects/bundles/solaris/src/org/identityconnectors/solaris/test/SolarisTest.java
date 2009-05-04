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


import java.io.IOException;

import junit.framework.Assert;

import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolarisTest {

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
    
    /* ************ TEST CONNECTOR ************ */
    
    /** elementary schema test */
    @Test
    public void basicSchemaTest() {
        SolarisConnector connector = SolarisTestCommon.createConnector(getConfig());
        Schema schema = connector.schema();
        Assert.assertNotNull(schema);
    }
        
    @Test
    public void testEcho() {
        SolarisConfiguration config = getConfig();
        SolarisConnector connector = SolarisTestCommon.createConnector(config);
        final String message = "HeLlO WoRlD";
        String output = connector.executeCommand(connector.getConnection(), String.format("echo \"%s\"", message));
        //System.out.println(s);
        /*
         * the output contains even the optional welcome message on Solaris.
         */
        Assert.assertTrue(output.contains(message));
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
