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
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.constants.ConnectionType;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.ExpectUtils;
import expect4j.matches.Match;

/**
 * maps functionality of SSHConnection.java
 * @author David Adam
 *
 */
public class SolarisConnection {
    /** set the timeout for waiting on reply. */
    public static final int WAIT = 12000;

    
    //TODO might be a configuration property
    private static final String HOST_END_OF_LINE_TERMINATOR = "\n";
    
    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration _configuration;
    private final Log log = Log.getLog(SolarisConnection.class);
    private Expect4j _expect4j;

    /** default constructor */
    public SolarisConnection(SolarisConfiguration configuration) {
        this(configuration, configuration.getUserName(), configuration.getPassword());
    }
    
    /**
     * Specific constructor used by OpAuthenticateImpl. In most cases consider
     * using {@link SolarisConnection#SolarisConnection(SolarisConfiguration)}
     */
    public SolarisConnection(SolarisConfiguration configuration,
            final String username, final GuardedString password) {
        if (configuration == null) {
            throw new ConfigurationException(
                    "Cannot create a SolarisConnection on a null configuration.");
        }
        _configuration = configuration;

        final ConnectionType connType = ConnectionType
                .toConnectionType(_configuration.getConnectionType());
        
        switch (connType) {
        case SSH:
            _expect4j = createSSHConn(username, password);
            break;
        case SSH_PUB_KEY:
            _expect4j = createSSHPubKeyConn(username, password);
            break;
        case TELNET:
            throw new UnsupportedOperationException("Telnet access not yet implemented: TODO");
            // _expect4j = ExpectUtils.telnet(_configuration
            // .getHostNameOrIpAddr(), _configuration
            // .getPort());
            //break;
        }
        
        try {
            waitFor(_configuration.getRootShellPrompt());
            /*
             * turn off the echoing of keyboard input on the resource.
             * Saves bandwith too.
             */
            send("stty -echo");
            waitFor(_configuration.getRootShellPrompt());
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * this piece of code is a combination of the adapter's
     * SSHPubKeyConnection#OpenSession() method and ExpectUtils#SSH()
     * 
     * @param username
     * @param password
     * @return
     */
    private Expect4j createSSHPubKeyConn(final String username, GuardedString password) {
//        JSch jsch=new JSch();
//        
//        ///////////////
//        byte[] pubKey = "".getBytes(); // TODO fill in password
//        jsch.addIdentity(name, prvkey, pubkey, passphrase);
//        ///////////////
//        
//        Session session=jsch.getSession(username, hostname, port);
//        
//        Hashtable<String, String> config = new Hashtable<String, String>();
//        config.put("StrictHostKeyChecking", "no");
//        session.setConfig(config);
//        session.setDaemonThread(true);
//        session.connect(3 * 1000); //making a connection with timeout.
//        
//        ChannelShell channel = (ChannelShell) session.openChannel("shell");
//        
//        channel.setPtyType("vt102");
//        
//        Hashtable env=new Hashtable();
//        channel.setEnv(env);
//        
//        Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
//        channel.connect(5*1000);
//
//        return expect;
        return null;
    }

    private Expect4j createSSHConn(final String username, GuardedString password) {
        final Expect4j[] result = new Expect4j[1];
        
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                try {
                    result[0] = ExpectUtils.SSH(_configuration.getHostNameOrIpAddr(), 
                            username, new String(clearChars), _configuration.getPort());
                } catch (Exception e) {
                    throw ConnectorException.wrap(e);
                }
            }
        });

        return result[0];
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
     * {@see SolarisConnection#waitFor(String, int)}
     */
    public String waitFor(final String string) throws Exception {
        return waitFor(string, WAIT);
    }
    
    /** {@see SolarisConnection#waitForCaseInsensitive(String, int)}*/
    public String waitForCaseInsensitive(final String string) throws Exception {
        return waitForCaseInsensitive(string, WAIT);
    }
    
    /** do case insensitive match and wait for
     * {@see SolarisConnection#waitFor(String, int)} 
     */
    public String waitForCaseInsensitive(final String string, int millis) throws Exception {
        return waitForImpl(string, millis, true);
    }

    /** Match a sequence of expected patterns, and invoke the respective actions on them. 
     * Implementation note: internally uses Expect4j#expect() method. 
     */
    public void expect(Match[] matches) throws MalformedPatternException, Exception {
        _expect4j.expect(matches);
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
        return waitForImpl(string, millis, false);
    }
    
    private String waitForImpl(final String string, int millis, boolean caseInsensitive) throws MalformedPatternException, Exception {
        log.info("waitFor(''{0}'', {1}, {2})", string, millis, Boolean.toString(caseInsensitive));
        /** internal buffer for the Solaris resource's output */
        final StringBuilder buffer = new StringBuilder();
        
        // build the matchers
        /** in case of successful match this closure is called */
        Closure successClosure = new Closure() {
            public void run(ExpectState state) {
                // save the content of buffer (the response from Solaris resource)
                buffer.append(state.getBuffer());
            }
        };
        MatchBuilder builder = new MatchBuilder();
        if (caseInsensitive) {
            builder.addCaseInsensitiveRegExpMatch(string, successClosure);
        } else {
            builder.addRegExpMatch(string, successClosure);
        }
        builder.addTimeoutMatch(millis, String.format("Timeout in waitFor('%s', %s)", string, Integer.toString(millis)));
        
        _expect4j.expect(builder.build());
        
        return buffer.toString();
    }
    
    /** 
     * Execute a issue a command on the resource specified by the configuration 
     */
    public String executeCommand(String command) {
        String output = null;
        try {
            
            send(command);
            output = waitFor(_configuration.getRootShellPrompt(), WAIT); 
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        int index = output.lastIndexOf(_configuration.getRootShellPrompt());
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
    public void dispose() {
        try {
            send("exit");
        } catch (IOException e) {
            // OK
        }
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
