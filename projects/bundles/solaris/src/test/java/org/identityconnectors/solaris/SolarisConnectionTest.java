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
package org.identityconnectors.solaris;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SolarisConnectionTest {
    
    private static SolarisConfiguration config;
    private static final String TIMEOUT_BETWEEN_MSGS = "0.5";
    private static final String LAST_ECHOED_INFO = "sausage.";
    
    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
        
        SolarisTestCommon.printIPAddress(config);
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
    
    /**
     * test that if an error occurs in the output, an exception is thrown.
     */
    @Test
    public void testErrorReplyScenario() {
        SolarisConnection conn = new SolarisConnection(SolarisTestCommon.createConfiguration());
        Set<String> rejects = new HashSet<String>();
        final String ERROR = "ERROR";
        rejects.add(ERROR);
        try {
            conn.executeCommand(
                    String.format("echo \"%s: ahoj ship\"", ERROR), 
                    rejects);
            Assert.fail("no exception thrown, when error found.");
        } catch (ConnectorException e) {
            // OK
        }
    }
    
    @Test @Ignore
    public void testErrorReplyScenarioWithTimeout() {
        config = SolarisTestCommon.createConfiguration();
        config.setPort(23);
        config.setConnectionType("telnet");
        SolarisConnection conn = new SolarisConnection(config);
        Set<String> rejects = new HashSet<String>();
        final String ERROR_MARKER = "ERROR";
        rejects.add(ERROR_MARKER);
        try {
            // Tougher test (it demands setting a long timeout on the connection.)
            //conn.executeCommand(String.format("export timeout=\"%s\" && echo  \"%s: ahoj\" && sleep \"$timeout\" && echo  \"ship\" && sleep \"$timeout\" && echo  \"egg\" && sleep \"$timeout\" && echo  \"spam\" && sleep \"$timeout\" && echo  \"%s\"", TIMEOUT_BETWEEN_MSGS, ERROR_MARKER, LAST_ECHOED_INFO), rejects);
            // Weaker test
            conn.executeCommand(String.format("export timeout=\"%s\" && echo  \"%s: ahoj\" && sleep \"$timeout\" && echo \"%s\"", TIMEOUT_BETWEEN_MSGS, ERROR_MARKER, LAST_ECHOED_INFO), rejects);
            Assert.fail("no exception thrown, when error found.");
        } catch (ConnectorException e) {
            final String exMsg = e.getMessage();
            String msg = String.format("Buffer <%s> doesn't containt the last echoed info: '%s'.", exMsg, LAST_ECHOED_INFO);
            //System.out.println("TEST: found: <"  exMsg  ">");
            Assert.assertTrue(msg, exMsg.contains(LAST_ECHOED_INFO));
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
