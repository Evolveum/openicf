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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SolarisConnectionTest extends SolarisTestBase {
    
    private static final String TIMEOUT_BETWEEN_MSGS = "0.5";
    private static final String LAST_ECHOED_INFO = "sausage.";
    private final Log log = Log.getLog(SolarisConnectionTest.class);
    
    /** test connection to the configuration given by default credentials (build.groovy) */
    @Test
    public void testGoodConnection() {
        SolarisConnector connector = new SolarisConnector();
        connector.init(getConfiguration());
        
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
        Set<String> rejects = new HashSet<String>();
        final String ERROR = "ERROR";
        rejects.add(ERROR);
        try {
            getConnection().executeCommand(
                    String.format("echo \"%s: ahoj ship\"", ERROR), 
                    rejects);
            Assert.fail("no exception thrown, when error found.");
        } catch (ConnectorException e) {
            // OK
        }
    }
    
    @Test @Ignore
    public void testErrorReplyScenarioWithTimeout() {
        Set<String> rejects = new HashSet<String>();
        final String ERROR_MARKER = "ERROR";
        rejects.add(ERROR_MARKER);
        try {
            // Tougher test (it demands setting a long timeout on the connection.)
            //conn.executeCommand(String.format("export timeout=\"%s\" && echo  \"%s: ahoj\" && sleep \"$timeout\" && echo  \"ship\" && sleep \"$timeout\" && echo  \"egg\" && sleep \"$timeout\" && echo  \"spam\" && sleep \"$timeout\" && echo  \"%s\"", TIMEOUT_BETWEEN_MSGS, ERROR_MARKER, LAST_ECHOED_INFO), rejects);
            // Weaker test
            getConnection().executeCommand(String.format("export timeout=\"%s\" && echo  \"%s: ahoj\" && sleep \"$timeout\" && echo \"%s\"", TIMEOUT_BETWEEN_MSGS, ERROR_MARKER, LAST_ECHOED_INFO), rejects);
            Assert.fail("no exception thrown, when error found.");
        } catch (ConnectorException e) {
            final String exMsg = e.getMessage();
            String msg = String.format("Buffer <%s> doesn't containt the last echoed info: '%s'.", exMsg, LAST_ECHOED_INFO);
            //System.out.println("TEST: found: <"  exMsg  ">");
            Assert.assertTrue(msg, exMsg.contains(LAST_ECHOED_INFO));
        }
    }
    
    @Test
    public void testSpecialAccepts() {
        final String MSG = "AHOJ";
        final String errMarker = "ERROR";
        
        getConnection().executeCommand(String.format("echo \"%s\"", MSG), Collections.<String>emptySet(), CollectionUtil.newSet(MSG));

        try {
            getConnection().executeCommand(String.format("echo \"%s %s\"", errMarker, MSG), CollectionUtil.newSet(errMarker), CollectionUtil.newSet(MSG));
            Assert.fail("no exception thrown when error should be found in the output.");
        } catch (ConnectorException ex) {
            //OK
        }
    }

    @Test
    public void testTelnetConnection() {
        if (!SolarisTestCommon.getProperty("unitTests.SolarisConnection.testTelnetMode", Boolean.class)) {
            log.info("skipping testTelnetConnection test, because the resource doesn't support it.");
            return;
        }
        
        // connection is recreated after every test method call so we are free to modify it.
        SolarisConfiguration config = getConnection().getConfiguration();
        config.setPort(23);
        config.setConnectionType(ConnectionType.TELNET.toString());
        SolarisConnection conn = new SolarisConnection(config);
        
        String out = conn.executeCommand("echo 'ahoj ship'");
        Assert.assertTrue(out.contains("ahoj ship"));
        conn.dispose();
    }

    @Test 
    public void testSSHPubKeyConnection() {
        if (!SolarisTestCommon.getProperty("unitTests.SolarisConnection.testSSHPubkeyMode", Boolean.class)) {
            log.info("skipping testSSHPubKeyConnection test, because the resource doesn't support it.");
            return;
        }
        
        // connection is recreated after every test method call so we are free to modify it.
        SolarisConfiguration config = getConnection().getConfiguration();
        config.setPassphrase(SolarisTestCommon.getProperty("rootPassphrase", GuardedString.class));
        config.setPrivateKey(SolarisTestCommon.getProperty("rootPrivateKey", GuardedString.class));
        config.setConnectionType(ConnectionType.SSHPUBKEY.toString());
        SolarisConnection conn = new SolarisConnection(config);
        
        String out = conn.executeCommand("echo 'ahoj ship'");
        Assert.assertTrue(out.contains("ahoj ship"));
        conn.dispose();
    }
    
    @Test
    public void testSudoAuthorization() {
        if (!SolarisTestCommon.getProperty("unitTests.SolarisConnection.testsudoAuthorization", Boolean.class)) {
            log.info("skipping testSSHPubKeyConnection test, because the resource doesn't support it.");
            return;
        }
        
        // connection is recreated after every test method call so we are free to modify it.
        SolarisConfiguration config = getConnection().getConfiguration();
        config.setSudoAuthorization(true);
        config.setLoginUser("david");
        config.setLoginShellPrompt("\\$");
        config.setPassword(SolarisTestCommon.getProperty("pass", GuardedString.class));
        config.setCredentials(SolarisTestCommon.getProperty("pass", GuardedString.class));
        
        SolarisConnection conn = new SolarisConnection(config);
        
        String out = conn.executeCommand("echo 'ahoj ship'");
        Assert.assertTrue(out.contains("ahoj ship"));
        conn.dispose();        
    }
    
    @Test
    public void checkAliveTest() {
        try {
            getConnection().checkAlive();
        } catch (Exception ex) {
            Assert.fail("no exception is expected.");
        }
        getConnection().dispose();
        try {
            getConnection().checkAlive();
            Assert.fail("exception should be thrown");
        } catch (Exception ex) {
            // OK
        }
    }

    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
}
