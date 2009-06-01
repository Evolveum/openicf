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
public final class SolarisConnection {
    //TODO externalize this into configuration
    //private static final int SHORT_WAIT = 60000;
    //private static final int LONG_WAIT = 120000;
    private static final int VERY_LONG_WAIT = 1200000;
    /** set the timeout for waiting on reply. */
    public static final int DEFAULT_WAIT = VERY_LONG_WAIT;

    
    //TODO might be a configuration property
    private static final String HOST_END_OF_LINE_TERMINATOR = "\n";
    
    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration _configuration;
    private final Log log = Log.getLog(SolarisConnection.class);
    private Expect4j _expect4j;

    /*  CONSTRUCTOR */
    public SolarisConnection(SolarisConfiguration configuration) {
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
                    final ConnectionType connType = ConnectionType
                            .toConnectionType(_configuration
                                    .getConnectionType());

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
        waitFor(string, DEFAULT_WAIT);
    }
    
    /**
     * Waits for feedback from the resource, respecting given timeout.
     * 
     * @param string is a standard regular expression
     * @param millis time in millis until expect waits for reply
     * @return 
     * @throws MalformedPatternException
     * @throws Exception
     */
    public String waitFor(final String string, int millis) throws MalformedPatternException, Exception {
        log.info("waitFor(''{0}'', {1})", string, millis);
        
        final Holder buffer = new Holder();
        
        Match[] matches = {
                new RegExpMatch(string, new Closure() {
                    public void run(ExpectState state) {
                        buffer.setS(state.getBuffer());
                    }
                }),
                new TimeoutMatch(millis,  new Closure() {
                    public void run(ExpectState state) {
                        ConnectorException e = new ConnectorException("TIMEOUT_IN_MATCH");
                        log.error(e, "timeout in waitFor");
                        throw e;
                    }
                })
        };
        _expect4j.expect(matches);
        
        return buffer.getS();
    }
    
    /** 
     * Execute a issue a command on the resource specified by the configuration 
     */
    public static String executeCommand(SolarisConfiguration configuration,
            SolarisConnection connection, String command) {
        String output = null;
        try {
            connection.send(command);
            output = connection.waitFor(configuration.getRootShellPrompt(), VERY_LONG_WAIT); 
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        int index = output.lastIndexOf(configuration.getRootShellPrompt());
        if (index!=-1)
            output = output.substring(0, index);
        
        String terminator = "\n";
        // trim off starting or ending \n
        //
        if (output.startsWith(terminator)) {
            output = output.substring(terminator.length());
        }
        if (output.endsWith(terminator)) {
            output = output.substring(0, output.length()-terminator.length());
        }
        return output;
    }

    /** once connection is disposed it won't be used at all. */
    void dispose() {
        log.info("dispose()");
        if (_expect4j != null) {
            _expect4j.close();
            _expect4j = null;
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
        connection.dispose();
    }
}

class Holder {
    private String s;

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }
}
