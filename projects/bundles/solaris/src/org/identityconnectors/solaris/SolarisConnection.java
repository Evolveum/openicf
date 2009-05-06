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

import java.io.IOException;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.ExpectUtils;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

/**
 * maps functionality of SSHConnection.java
 * @author David Adam
 *
 */
public class SolarisConnection {
    //TODO might be a configuration property
    private static final String HOST_END_OF_LINE_TERMINATOR = "\n";
    
    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration _configuration;
    private final Log log = Log.getLog(SolarisConnection.class);
    private Expect4j _expect4j;
    private StringBuffer _buffer;

    /*  CONSTRUCTOR */
    public SolarisConnection(SolarisConfiguration configuration) {
        _buffer = new StringBuffer();
        
        if (configuration == null) {
            throw new ConfigurationException(
                    "Cannot create a SolarisConnection on a null configuration.");
        }
        _configuration = configuration;

        // initialize EXPECT4J
        final GuardedString password = _configuration.getPassword();
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                try {
                    final ConnectionType connType = _configuration
                            .getConnectionType();

                    if (connType.equals(ConnectionType.SSH)) {
                        _expect4j = ExpectUtils.SSH(_configuration
                                .getHostNameOrIpAddr(), _configuration
                                .getUserName(), new String(clearChars),
                                _configuration.getPort());
                    } else if (connType.equals(ConnectionType.TELNET)) {
                        throw new UnsupportedOperationException("Telnet access not yet implemented: TODO");
//                        _expect4j = ExpectUtils.telnet(_configuration
//                                .getHostNameOrIpAddr(), _configuration
//                                .getPort());
                    }
                } catch (Exception e) {
                    log.warn("Starting connection: Exception thrown (cause: invalid configuration, etc.)");
                    throw ConnectorException.wrap(e);
                }
            }
        });
    }

    /* *************** METHODS ****************** */
    /**
     * send a command to the resource, no end of line needed.
     * @param string
     */
    public void send(String string) throws IOException {
        log.info("send(''{0}'')", string);
        //System.out.println("Send:"+string);
        _expect4j.send(string + HOST_END_OF_LINE_TERMINATOR);
    }
    
    /**
     * {@see SolarisConnection#send(String)}
     */
    public void send(StringBuffer string) throws IOException {
        send(string.toString());
    }
    
    /**
     * {@see SolarisConnection#waitFor(String, int)}
     */
    public void waitFor(String string) throws Exception{
        waitFor(string, SolarisHelper.DEFAULT_WAIT);
    }
    
    /**
     * Waits for feedback from the resource, respecting given timeout.
     * 
     * @param string is a standard regular expression
     * @param millis time in millis until expect waits for reply
     * @throws MalformedPatternException
     * @throws Exception
     */
    public void waitFor(final String string, int millis) throws MalformedPatternException, Exception {
        log.info("waitFor(''{0}'', {1})", string, millis);
        Match[] matches = {
                new RegExpMatch(string, new Closure() {
                    public void run(ExpectState state) {
                        _buffer.append(state.getBuffer());
                    }
                }),
                new TimeoutMatch(millis,  new Closure() {
                    public void run(ExpectState state) {
                        System.out.println("timeout:"+_buffer);
                        ConnectorException e = new ConnectorException("TIMEOUT_IN_MATCH");
                        log.error(e, "timeout in waitFor");
                        throw e;
                    }
                })
        };
        _expect4j.expect(matches);
    }
    
    public String getStandardOutput() {
        return _buffer.toString();
    }

    public void resetStandardOutput() {
        _buffer.setLength(0);
    }
    
    /** once connection is disposed it won't be used at all. */
    void dispose() {
        log.info("dispose()");
        if (_expect4j != null) {
            _expect4j.close();
        }
    }
    
    /**
     * Try to authenticate with the given configuration
     * If the test fails, an exception is thrown.
     * @param configuration the configuration that should be tested.
     * @throws Exception
     */
    static void test(SolarisConfiguration configuration) throws Exception {
        SolarisConnection connection = new SolarisConnection(configuration);
        
//        if (!testIfUserIsRoot(connection, configuration)) {
//            throw new IllegalArgumentException(
//                    String.format("The Administrative User defined in configuration property '%s'==\"%s\" is not 'root'", 
//                            "SolarisConfiguration#userName", configuration.getUserName()));
//        }
    }

//    /** helper method for evaluating if user has root privileges */
//    private static boolean testIfUserIsRoot(SolarisConnection connection,
//            SolarisConfiguration configuration) {
//        /*
//         * use the whoami script to look for root named account TODO generalize this.
//         */
//        final String command = "whoami";
//        String result = SolarisHelper.executeCommand(configuration, connection, command);
//        if (result.contains("root")) {
//            return true;
//        }
//        return false;
//    }
}
