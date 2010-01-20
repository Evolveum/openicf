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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.command.RegExpCaseInsensitiveMatch;
import org.identityconnectors.solaris.operation.SolarisCreate;
import org.identityconnectors.solaris.operation.SolarisUpdate;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.ExpectUtils;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

/**
 * Connection class provides connection to the Solaris resource. It also creates an 
 * abstraction layer for interpretation of errors.
 * 
 * <p>The connection offers the following authentication types: SSH, Telnet and SSH Pubkey.
 * 
 * @author David Adam
 * 
 * <p><i>Implementation notes:</i> Expect4J is an expect based matching system for analyzing the 
 * responses from a unix resource, and for defining actions to these responses.
 * <p> Expect4j uses internally JSch to provide the SSH connection channel to the resource. (This is 
 * transparent to the user of Expect4J, so it is for {@link SolarisConnection}.
 *
 */
public class SolarisConnection {
    /*
     * Implementation constant: the maximum timeout that the connection 
     * waits before retrying to read an emergency output (after occurrence of ERROR in 
     * output stream from the resource.
     * 
     * Constant's unit: millisecond.
     */
    private static final int WAITFOR_TIMEOUT_FOR_ERROR = 600;
    
    private static final String HOST_END_OF_LINE_TERMINATOR = "\n";
    /* 
     * Root Shell Prompt used by the connector.
     * As Expect uses regular expressions, the pattern should be quoted as a string literal. 
     */
    private static final String CONNECTOR_PROMPT = "~ConnectorPrompt";
    /*
     * Implementation constant: the maximum length of overall timeout that we 
     * wait after discovering ERROR in output stream of the unix resource.
     * 
     * - we are using System.nanotime() for time measuring, so the unit of 
     * this constant is nanosecond.
     */
    private static final double ERROR_WAIT = 100 * Math.pow(10, 6); // 100 mseconds, hard-coded constant in the adapter.
    
    /**
     * default way of handling error messages.
     */
    private static final ErrorHandler defaultErrorHandler =  new ErrorHandler() {
        public void handle(String buffer) {
            throw new ConnectorException("ERROR, buffer content: <" + buffer + ">");
        }
    };
    
    private String loginShellPrompt;
    private Expect4j expect4j;
    
    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration configuration;
    public SolarisConfiguration getConfiguration() {
        return configuration;
    }

    private final Log log = Log.getLog(SolarisConnection.class);
    
    /**
     * Specific constructor used by OpAuthenticateImpl. In most cases consider
     * using {@link SolarisConnection#SolarisConnection(SolarisConfiguration)}
     */
    public SolarisConnection(SolarisConfiguration config) {
        if (config == null) {
            throw new ConfigurationException(
                    "Cannot create a SolarisConnection on a null configuration.");
        }
        configuration = config;
        
        loginShellPrompt = configuration.getLoginShellPrompt();
        
        final String loginUser = configuration.getLoginUser();
        final GuardedString password = configuration.getPassword();

        final ConnectionType connType = ConnectionType.toConnectionType(configuration.getConnectionType());
        switch (connType) {
        case SSH:
            expect4j = createSSHConn(loginUser, password);
            break;
        case SSHPUBKEY:
            expect4j = createSSHPubKeyConn(loginUser);
            break;
        case TELNET:
            expect4j = createTelnetConn(loginUser, password);
            break;
        }
        
        try {
            if (!connType.selfAuthenticates()) {
                /*
                 * telnet doesn't authenticate automatically, so an extra step is needed:
                 */
                executeCommand(null, Collections.<String>emptySet(), CollectionUtil.newSet("login")/* wait for login prompt */);
                executeCommand(loginUser.trim(), Collections.<String>emptySet(), CollectionUtil.newSet("assword"));
                sendPassword(password, this);
            }
            
            waitForRootShellPrompt(CollectionUtil.newSet("incorrect"));
            /*
             * turn off the echoing of keyboard input on the resource.
             * Saves bandwith too.
             */
            executeCommand("stty -echo");
            
            // if the login and root users are different, we will need to su to root here.
            final String rootUser = (!StringUtil.isBlank(configuration.getRootUser())) ? configuration.getRootUser() : loginUser; // rootuser equals to loginUser, if it is not defined in the configuration.
            if (!configuration.isSudoAuthorization() && !loginUser.equals(rootUser)) {
                executeCommand("su " + rootUser, CollectionUtil.newSet("Unknown id", "does not exist"), CollectionUtil.newSet("assword:"));
                
                // we need to change the type of rootShellPrompt here (we used loginUser's up to now)
                final String rootShellPrompt = (!StringUtil.isBlank(configuration.getRootShellPrompt())) ? configuration.getRootShellPrompt() : loginShellPrompt;
                loginShellPrompt = rootShellPrompt;
                
                sendPassword(password, CollectionUtil.newSet("Sorry", "incorrect password"), Collections.<String>emptySet() /* wait for rootShellPrompt */, this);
                executeCommand("stty -echo");
            }
            
            /*
             * Change root shell prompt, for simplier parsing of the output.
             * Revert the changes after the connection is closed.
             */
            loginShellPrompt = CONNECTOR_PROMPT;
            executeCommand("PS1=\"" + CONNECTOR_PROMPT + "\"");
        } catch (Exception e) {
            throw new ConnectorException(String.format("Connection failed to host '%s:%s' for user '%s'", configuration.getHost(), configuration.getPort(), loginUser), e);
        }
    }

    private Expect4j createTelnetConn(String username, GuardedString password) {
        Expect4j expect4j = null;
        try {
            expect4j = ExpectUtils.telnet(configuration.getHost(), configuration.getPort());
        } catch (Exception e1) {
            throw ConnectorException.wrap(e1);
        }
        return expect4j;
    }

    /**
     * Connect to the resource using privateKey + passphrase pair.
     * 
     * @param username
     * @return initialized instance of Expect4J library used for the connection.
     * 
     * Implementational note: this piece of code is a combination of the adapter's
     * SSHPubKeyConnection#OpenSession() method and ExpectUtils#SSH()
     */
    private Expect4j createSSHPubKeyConn(final String username) {
        final JSch jsch=new JSch();
        
        final GuardedString privateKey = getConfiguration().getPrivateKey();
        final GuardedString keyPassphrase = getConfiguration().getPassphrase();
        privateKey.access(new GuardedString.Accessor() {
            public void access(final char[] privateKeyClearText) {
                keyPassphrase.access(new GuardedString.Accessor() {
                    public void access(final char[] keyPassphraseClearText) {
                        try {
                            jsch.addIdentity("IdentityConnector", convertToBytes(privateKeyClearText), null, convertToBytes(keyPassphraseClearText));
                        } catch (JSchException e) {
                            throw ConnectorException.wrap(e);
                        }
                    }

                    private byte[] convertToBytes(char[] text) {
                        byte[] bytes = new byte[text.length];
                        for (int i = 0; i < text.length; i++) {
                            bytes[i] = (byte) text[i];
                        }
                        return bytes;
                    }
                });
            }
        });
        
        Session session = null;
        try {
            session = jsch.getSession(username, getConfiguration().getHost(), getConfiguration().getPort());
        } catch (JSchException e) {
            throw ConnectorException.wrap(e);
        }
        
        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setDaemonThread(true);
        try {
            session.connect(3 * 1000);//making a connection with timeout.
        } catch (JSchException e) {
            throw ConnectorException.wrap(e);
        } 
        
        ChannelShell channel = null;
        try {
            channel = (ChannelShell) session.openChannel("shell");
        } catch (JSchException e) {
            throw ConnectorException.wrap(e);
        }
        
        channel.setPtyType("vt102");
        
        Expect4j expect = null;
        try {
            expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
            channel.connect(5*1000);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }

        return expect;
    }

    private Expect4j createSSHConn(final String username, GuardedString password) {
        final Expect4j[] result = new Expect4j[1];
        
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                try {
                    result[0] = ExpectUtils.SSH(configuration.getHost(), 
                            username, new String(clearChars), configuration.getPort());
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
    private void sendInternal(String string) throws IOException {
        expect4j.send(string + HOST_END_OF_LINE_TERMINATOR);
    }
    
    /**
     * send a password to the resource, and return the response from the
     * resource, if any.
     * 
     * @param passwd
     *            the password to send
     * @param rejects
     *            Optional parameter. {@see
     *            SolarisConnection#executeCommand(String, Set, Set)} contract
     * @param accepts
     *            Optional parameter. {@see
     *            SolarisConnection#executeCommand(String, Set, Set)} contract
     * @param conn
     * @return feedback on the sent password from the resource.
     * 
     * Note on usage of params 'rejects', 'accepts': If none of the parameters are given, we wait for RootShellPrompt
     * Note: compare with {@link SolarisUtil#sendPassword(GuardedString, SolarisConnection)}
     */
    public static String sendPassword(GuardedString passwd, Set<String> rejects, Set<String> accepts, final SolarisConnection conn) {
        sendPasswdImpl(passwd, conn);
        
        return conn.executeCommand(null/* no command is executed here */, rejects, accepts);
    }
    
    /** 
     * just send a password but don't anticipate any response from the resource.
     * Compare with {@link SolarisUtil#sendPassword(GuardedString, Set, Set, SolarisConnection)}
     */
    public static void sendPassword(GuardedString passwd, SolarisConnection conn) {
        sendPasswdImpl(passwd, conn);
    }

    private static void sendPasswdImpl(GuardedString passwd,
            final SolarisConnection conn) {
        passwd.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                try {
                    for (char c : clearChars) {
                        if (Character.isISOControl(c)) {
                            throw new IllegalArgumentException("User password contains one or more control characters.");
                        }
                    }
                    
                    conn.sendInternal(new String(clearChars));
                } catch (IOException e) {
                    throw ConnectorException.wrap(e);
                }
            }
        });
    }
    
    /** 
     * {@see SolarisConnection#executeCommand(String, Set, Set)} 
     */
    public String executeCommand(String command) {
        return executeCommand(command, Collections.<String>emptySet(), Collections.<String>emptySet());
    }
    
    /**
     * {@see SolarisConnection#executeCommand(String, Set, Set)}
     * 
     * @param timeout
     *            the time interval, that we will wait for the response (marked
     *            by {@link SolarisConfiguration#getRootShellPrompt()}
     */
    public String executeCommand(String command, int timeout) {
        return executeCommand(command, Collections.<String>emptySet(), Collections.<String>emptySet(), timeout);
    }

    /**
     * {@see SolarisConnection#executeCommand(String, Set, Set)}
     */
    public String executeCommand(String command, Set<String> rejects) {
        return executeCommand(command, rejects, Collections.<String>emptySet());
    }

    /**
     * This method's contract is similar to
     * {@link SolarisConnection#executeCommand(String, Map, Set)}. The only
     * difference is the 'rejects' parameter.
     * 
     * @param rejects
     *            the error messages that can occur. If they are found, a
     *            {@link ConnectorException} is thrown.
     */
    public String executeCommand(String command, Set<String> rejects, Set<String> accepts) {
        Map<String, ErrorHandler> rejectsMap = new HashMap<String, ErrorHandler>();
        for (String rej : rejects) {
            // by default rejects throw ConnectorException.
            rejectsMap.put(rej, defaultErrorHandler);
        }
        return executeCommand(command, CollectionUtil.asReadOnlyMap(rejectsMap), accepts);
    }
    
    /**
     * {@see SolarisConnection#executeCommand(String, Set, Set)}
     * 
     * @param timeout
     *            the time interval, that we will wait for the response (marked
     *            by {@link SolarisConfiguration#getRootShellPrompt()}
     */
    private String executeCommand(String command, Set<String> rejects, Set<String> accepts, int timeout) {
        Map<String, ErrorHandler> rejectsMap = new HashMap<String, ErrorHandler>();
        for (String rej : rejects) {
            // by default rejects throw ConnectorException.
            rejectsMap.put(rej, defaultErrorHandler);
        }
        return executeCommand(command, CollectionUtil.asReadOnlyMap(rejectsMap), accepts, timeout);
    }

    /**
     * {@link SolarisConnection#executeCommand(String, Map, Set)}
     * @param timeout
     *            the time interval, that we will wait for the response (marked
     *            by {@link SolarisConfiguration#getRootShellPrompt()}
     */
    private String executeCommand(String command, Map<String, ErrorHandler> rejects, Set<String> accepts, int timeout) {
        try {
            if (command != null) {
                sendInternal(command);
            }
        } catch (Exception e) {
            throw new ConnectorException("Error occured in SolarisConnection, during send(). Exception message: " + e.getMessage());
        }
        
        /*
         * IMPLEMENTATION NOTE on 'Ordering of matchers w.r.t. Expect4j'
         * 
         * Expect4j matches the first pattern, that occurs at the minimum index in the input stream.
         * That said, for instance input stream is 
         * '$ foobar $'
         * with List of patterns = { "foobar", "$" }.
         * The matching pattern according to expect is "$".
         * *WHY*?
         * Expect matches the "$" pattern, despite of "foobar" being the first in the list of patterns.
         * In fact, the implementation iterates over the list of matchers, and searches for the index 
         * of matching substring. The first minimal index wins. In other words, if two matchers both 
         * match the input from the start, the matcher, which is the first in the list of patterns, wins.
         * 
         * This explains why "$" was matched (it has index 0), rather then foobar (with larger match index 2).
         */
        
        // according to the previous implementation note, the error matchers should go first!:
        MatchBuilder builder = new MatchBuilder();
        // #1 Adding Error matchers
        final List<ErrorClosure> cecList = new ArrayList<ErrorClosure>();
        for (Map.Entry<String, ErrorHandler> rejEntry : rejects.entrySet()) {
            ErrorClosure cec = new ErrorClosure(rejEntry.getValue());
            cecList.add(cec);
            /*
             * Errors are matched without respect to case. E.g. 'Error' and 'ERROR' is treated the same.
             */
            builder.addCaseInsensitiveRegExpMatch(rejEntry.getKey(), cec);
        }
        
        // #2 Adding RootShellPrompt matcher or other acceptance matchers if given
        List<SolarisClosure> captureClosures = null;
        if (accepts.size() > 0) {
            captureClosures = CollectionUtil.newList();
            for (String acc : accepts) {
                SolarisClosure closure = new SolarisClosure();
                captureClosures.add(closure);
                builder.addCaseInsensitiveRegExpMatch(acc, closure);
            }
        } else {
            // by default rootShellPrompt is added.
            SolarisClosure closure = new SolarisClosure();
            captureClosures = CollectionUtil.newList(closure);
            builder.addRegExpMatch(getRootShellPrompt(), closure);
        }
        
        // #3 set the timeout for matching too
        builder.addTimeoutMatch(timeout, new SolarisClosure() {
            public void run(ExpectState state) throws Exception {
                throw new ConnectorException("executeCommand: timeout occured, and no ERROR matched. Buffer: <" + state.getBuffer() + ">");
            }
        });
        
        try {
            /*
             * Implementation notes on Expect4J (v. 1.0)
             * 
             * 1) What is Expect4J and how it is used in the connector?
             * 
             * Expect4J is a wrapping library over SSH connection (provided by
             * JSch library), that is capable of analyzing the response from the
             * SSH connection. Expect supports various other connection types
             * too: telnet, SSH keypair.
             * 
             * The role of Expect in SolarisConnector is to analyze the feedback
             * from the resource, and to invoke the assigned actions based on
             * the resource's feedback.
             * 
             * The previous paragraph sounds a bit generic, so let's be more
             * specific about the way Expect analyzes the resource's response
             * (referred as "response" further). Expect has two basic commands:
             * -- send(string) -- that sends the string to an SSH connection *
             * -- expect(Match[]) -- waits for response, that will contain the
             *      given matches. A match consists of a [string, Closure] pair. If
             *      the given string is matched, the Closure is executed. 
             * 
             * Expect follows the algorithm: 
             * -- a) read the response from the resource 
             * -- b) scan through matches, and choose a single winner match 
             * that is in the response
             * -- c) if no matches found try to read from the resource, until 
             *         some time is left from timeout.
             *         
             * WARNING: Expect's way of matching patterns is pretty intricate!
             * 
             * Note: a Closure is a callback interface, that is invoked when 
             * the associated matching string is matched.
             * 
             * 
             * 
             * 
             * 
             * 2) How does expect choose which matcher matches the response?
             * 
             * If you'd like to verify the following paragraphs in the code, 
             * look at expect4j.Expect4j.runFirstMatch(List).
             * 
             * Expect receives a list of matchers (String, Closure) pairs.
             * 
             * 
             * 
             * What matters for Expect when choosing the "winner" matching string?
             * 
             * CRITERIA #1
             * -- The order of matcher string *does matter*.
             * 
             * CRITERIA #2
             * -- The position of match in the response for the given 
             *      matcher *does matter*.
             * 
             * 
             * 
             * 
             * == CRITERIA #1 explained -- "order matters" ==
             * 
             *    In general, the most specific matcher strings should come first 
             * in the list of matchers that expect receives. The more generic 
             * should come to the end of List.
             * If we take an extreme example, when we have a matcher list:
             * ["Error Foo specific", "Error"]
             * In case the response is identical to the first matcher, than 
             * it'll be the winner. The second matcher could be matched too, 
             * but it is only coming as second, moreover it has the same 
             * matching position -- see the criteria further.
             * 
             * 
             * 
             * == CRITERIA #2 explained -- "match position matters" ==
             * 
             *    If there are two matchers, one is _prefix_ of another 
             * (for example the previous ["Error Foo specific", "Error"])
             * so we can have cases, when both matchers are matched. 
             * In this case Expect *prefers* the matcher who:
             *  -- is matched as first
             *  -- && match position is closer or equal to the start of the string.
             * 
             * Reflected back onto the example, if the responses are:
             * matcher list: ["Error Foo specific", "Error"]
             * response#1: "bla Error Foo specific bla bla"
             * winner#1: "Error Foo specific" -- both matchers have the same position 
             *            of the first match character (=4), so the one earlier on 
             *            the matching list wins.
             * 
             * response#2: "bla Error bla Error Foo specific bla"
             * winner #2: "Error" -- the earlier match position wins here.
             * 
             * 
             * 
             * 3) What part of the output do I get from expect in case of successful match?
             * 
             * Let's give an example:
             * matcher list = ["Ahoj"]
             * response: "foo bar baz Ahoj ship"
             * returned output from expect: "foo bar baz Ahoj"
             * 
             * In other words, expect returns the response buffer content up to 
             * the first successful match (including characters of this match).
             * 
             * Note: connector has and additional funcionality of altering the output:
             * it deletes the trailing rootShellPrompt (e.g. typicalli $).
             * 
             * 
             * 
             * 4) What happens if the match is unsuccessful?
             * A timeout occurs. This timeout can be registered by a timeout closure, 
             * as it is done in the SolarisConnection too.
             * 
             * 
             * 5) How do we handle connection errors?
             * In case an error string is matched we enter into mode of special 
             * processing, that is inherited from the Solaris resource adapter. 
             * This means, that we try to read as much of error output as it is possible. 
             * (We disregard any terminating characters exceptionally.)
             * This fuctionality is visible in method: 
             * org.identityconnectors.solaris.SolarisConnection.handleRejects(List<ErrorClosure>)
             * .
             * 
             * 6) Can I pass regurlar expressions in matchers? 
             * Yes, Expect4j uses Apache Oro regular expression library. 
             * The regular expressions are matched throughout the whole response.
             * For example:
             * -- regexp: "ship"
             * -- string to match: "ahoj ship"
             * the match is successful, the initial other letters are ignored by the matcher.
             * 
             */
            expect4j.expect(builder.build());
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        String output = null;
        for (SolarisClosure cl : captureClosures) {
            if (cl.isMatched()) {
                // get regular output matched by root shell prompt.
                output = cl.getMatchedBuffer();
                break;
            }
        }
        
        if (output == null) {
            // handle error message processing, throw an exception if error found
            handleRejects(cecList);
        } else {
            output = trimOutput(output);
        }

        return output;
    }
    
    /**
     * Execute a issue a command on the resource. Return the match of feedback
     * up to the root shell prompt
     * {@link SolarisConnection#getRootShellPrompt()}.
     * 
     * @param command
     *            the executed command. In special cases (such as waiting for
     *            the first prompt after login, the command can have {@code
     *            null} value. If {@code command} is {@code null}, then we wait
     *            for root shell prompt without executing any other commands
     *            (given that {@code root shell prompt} was not overriden by
     *            {@code accepts} parameter.
     * 
     * @param rejects
     *            Map that contains error message,
     *            {@link SolarisConnection.ErrorHandler} pairs. If the error
     *            message is found in response from the resource, the error
     *            handler is called. .
     *            <p>
     * 
     * @param accepts
     *            these are accepting strings, if they are found the result is
     *            returned. If empty set is given, the default accept is
     *            {@link SolarisConnection#getRootShellPrompt()}. Caution: in case
     *            <code>accepts</code> parameter is specified, it'll be the last
     *            element in the response from the resource. If we don't respect this 
     *            rule than we will loose precious output of the following commands, that 
     *            are issued. <i>A larger illustration of violation of the previous contract
     *            follows. It is above the basic usage, however we should be aware of 
     *            consequences of violating contract for 'accepts' parameter.</i> 
     *            EXAMPLE: see the following calls / respones from the {@link SolarisConnection}:
     *            <pre>
     *            out = conn.executeCommand("echo 'one'", Collections.<String>emptySet(), CollectionUtil.newSet("one"));
     *            Assert.assertEquals("one", out); // will succeed
     *            out = conn.executeCommand("echo 'two'", Collections.<String>emptySet(), CollectionUtil.newSet("two"));
     *            Assert.assertEquals("two", out); // fail, due to empty output
     *            out = conn.executeCommand("echo 'three'", Collections.<String>emptySet(), CollectionUtil.newSet("three"));
     *            Assert.assertEquals("three", out); // fail, due to empty output
     *            </pre>
     *            If we analyze the previous example we will see the following sequence of request/response going on:
     *            <pre>
     *            >> echo 'one'
     *            << one
     *            >> echo 'two'
     *            << two~ConnectorPrompt // at least this is what we expect, but we'll get an empty output
     *                                   // because of {@link SolarisConnection#trimOutput(String)}, that cuts off everything after root shell prompt.
     *            </pre>
     *            The solution is to wait for rootShellPrompt after every 'accept' parameter, that doesn't terminate the output:
     *            <pre>
     *            out = conn.executeCommand("echo 'one'", Collections.<String>emptySet(), CollectionUtil.newSet("one"));
     *            conn.executeCommand(null) // will cause waiting for the rootShellPrompt.
     *            Assert.assertEquals("one", out); // will succeed
     *            out = conn.executeCommand("echo 'two'", Collections.<String>emptySet(), CollectionUtil.newSet("two"));
     *            conn.executeCommand(null) // will cause waiting for the rootShellPrompt.
     *            Assert.assertEquals("two", out); // will succeed
     *            out = conn.executeCommand("echo 'three'", Collections.<String>emptySet(), CollectionUtil.newSet("three"));
     *            conn.executeCommand(null) // will cause waiting for the rootShellPrompt.
     *            Assert.assertEquals("three", out); // will succeed
     *            </pre>
     * 
     * @return the response from the resource when the command is successful,
     *         free of error messages. Otherwise throw a
     *         {@link ConnectorException}.
     * 
     * @throws ConnectorException
     *             in case a <code>rejects</code> string is found in the
     *             response of the resource.
     */
    public String executeCommand(String command, Map<String, ErrorHandler> rejects, Set<String> accepts) {
        return executeCommand(command, rejects, accepts, getConfiguration().getCommandTimeout());
    }

    private String trimOutput(String output) {
        int index = output.lastIndexOf(getRootShellPrompt());
        if (index != -1) {
            output = output.substring(0, index);
        }
        output = output.trim();
        return output;
    }

    private void handleRejects(List<ErrorClosure> cecList) {
        for (ErrorClosure connectorExceptionClosure : cecList) {
            if (connectorExceptionClosure.isMatched()) {
                String out = connectorExceptionClosure.getMatchedBuffer();

                out = waitForInput(out);

                connectorExceptionClosure.getErrorHandler().handle(out);
            }//fi
        }//for
    }
    
    private String waitForInput(final String out) {
        StringBuilder buffer = new StringBuilder(out);
        long start = System.nanoTime();
        while (System.nanoTime() - start <= ERROR_WAIT) {
            String tmp = null;
            try {
                tmp = waitForImpl(".+", WAITFOR_TIMEOUT_FOR_ERROR, true);
            } catch (Exception ex) {
                // OK
            }
            int lastLength = buffer.length();
            buffer.append((tmp != null) ? tmp : "");
            if (buffer.indexOf(getRootShellPrompt(), lastLength) > -1) {
                break;
            }
        }
        return trimOutput(buffer.toString());
    }

    /**
     * wait for shell prompt
     * 
     * @param rejects
     *            throw {@link ConnectorException} if a reject is matched in the
     *            feedback up to the rootShellPrompt.
     * @throws {@link ConnectorException} in case of timeout in waiting for
     *         rootShellPrompt character.
     */
    public void waitForRootShellPrompt(Set<String> rejects) {
        executeCommand(null, rejects);
    }
    
    /** {@see SolarisConnection#waitForRootShellPrompt(Set)} */
    public void waitForRootShellPrompt() {
        executeCommand(null, Collections.<String>emptySet());
    }
    
    private String waitForImpl(final String string, final int millis, boolean caseInsensitive) throws MalformedPatternException, Exception {
        log.info("waitFor(''{0}'', {1}, {2})", string, millis, Boolean.toString(caseInsensitive));
        /** internal buffer for the Solaris resource's output */
        final StringBuilder buffer = new StringBuilder();
        
        // build the matchers
        /** in case of successful match this closure is called */
        SolarisClosure successClosure = new SolarisClosure() {
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
        builder.addTimeoutMatch(millis, new SolarisClosure() {
            public void run(ExpectState state) throws Exception {
                String msg = String.format("Timeout in waitFor('%s', %s) buffer: <%s>",
                        string, Integer.toString(millis), state.getBuffer());
                throw new ConnectorException(msg);
            }
        });
        
        expect4j.expect(builder.build());
        
        return buffer.toString();
    }

    /**
     * once connection is disposed it won't be used at all. This method performs
     * logoff and assigns null to the internal expect libraries reference.
     */
    public void dispose() {
        try {
            sendInternal("exit");
            final String loginUser = getConfiguration().getLoginUser();
            // rootuser equals to loginUser, if it is not defined in the configuration.
            final String rootUser = (!StringUtil.isBlank(getConfiguration().getRootUser())) ? getConfiguration().getRootUser() : getConfiguration().getLoginUser();
            if (!loginUser.equals(rootUser)) {
                sendInternal("exit");
            }
        } catch (IOException e) {
            // OK
        }
        log.info("dispose()");
        if (expect4j != null) {
            expect4j.close();
            expect4j = null;
        }
    }
    
    /**
     * The method formats the given command, and inserts {@code sudo} prefix in front
     * of the command according to the state of the connection's {@link SolarisConnection#configuration}.
     * 
     * @param command
     *            the command can be a chain of strings separated by spaces. In
     *            case for some reason we want to delegate the chaining to this
     *            builder, we can use the additional arguments parameter.
     * @param arguments
     *            optional parameter for chaining extra arguments at the end of
     *            command.
     * <br>
     * Note: Don't use this method, if you want to have a plain command, <b>withouth</b> 
     * the {@code sudo} prefix. 
     */
    public String buildCommand(String command, CharSequence... arguments) {
        StringBuilder buff = new StringBuilder();
        if (configuration.isSudoAuthorization()) {
            buff.append("sudo ");
        }
        buff.append(command);
        buff.append(" ");// for safety reasons, in case there are no arguments, and the command is used within legacy scrips from adapter.
        
        for (CharSequence string : arguments) {
            buff.append(" ");
            buff.append(string.toString());
        }
        
        return SolarisUtil.limitString(buff);
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

    public String getRootShellPrompt() {
        return loginShellPrompt;
    }
    
    /*
     * MUTEXING
     */
    /** mutex acquire constants */
    private static final String tmpPidMutexFile = "/tmp/WSlockuid.$$";
    private static final String pidMutexFile = "/tmp/WSlockuid";
    private static final String pidFoundFile = "/tmp/WSpidfound.$$";
    /**
     * Mutexing script is used to prevent race conditions when creating
     * multiple users. These conditions are present at {@link SolarisCreate} and
     * {@link SolarisUpdate}. The code is taken from the resource adapter.
     */
    private String getAcquireMutexScript() {
        // This code is from SolarisResouceAdapter
        long timeout = getConfiguration().getMutexAcquireTimeout();
        String rmCmd = buildCommand("rm");
        String catCmd = buildCommand("cat");

        if (timeout < 1) {
            timeout = SolarisConfiguration.DEFAULT_MUTEX_ACQUIRE_TIMEOUT;
        }

        String pidMutexAcquireScript =
            "TIMEOUT=" + timeout + "; " +
            "echo $$ > " + tmpPidMutexFile + "; " +
            "while test 1; " +
            "do " +
              "ln -n " + tmpPidMutexFile + " " + pidMutexFile + " 2>/dev/null; " +
              "rc=$?; " +
              "if [ $rc -eq 0 ]; then\n" +
                "LOCKPID=`" + catCmd + " " +  pidMutexFile + "`; " +
                "if [ \"$LOCKPID\" = \"$$\" ]; then " +
                  rmCmd + " -f " + tmpPidMutexFile + "; " +
                  "break; " +
                "fi; " +
              "fi\n" +
              "if [ -f " + pidMutexFile + " ]; then " +
                "LOCKPID=`" + catCmd + " " + pidMutexFile + "`; " +
                "if [ \"$LOCKPID\" = \"$$\" ]; then " +
                  rmCmd + " -f " + pidMutexFile + "\n" +
                "else " +
                  "ps -ef | while read REPLY\n" +
                  "do " +
                    "TESTPID=`echo $REPLY | awk '{ print $2 }'`; " +
                    "if [ \"$LOCKPID\" = \"$TESTPID\" ]; then " +
                      "touch " + pidFoundFile + "; " +
                      "break; " +
                    "fi\n" +
                  "done\n" +
                  "if [ ! -f " + pidFoundFile + " ]; then " +
                    rmCmd + " -f " + pidMutexFile + "; " +
                  "else " +
                    rmCmd + " -f " + pidFoundFile + "; " +
                  "fi\n" +
                "fi\n" +
              "fi\n" +
              "TIMEOUT=`echo | awk 'BEGIN { n = '$TIMEOUT' } { n -= 1 } END { print n }'`\n" +
              "if [ $TIMEOUT = 0 ]; then " +
                "echo \"ERROR: failed to obtain uid mutex\"; " +
                rmCmd + " -f " + tmpPidMutexFile + "; " +
                "break; " +
              "fi\n" +
              "sleep 1; " +
            "done";

        return pidMutexAcquireScript;
    }
    
    private String getAcquireMutexScript(String uidMutexFile, String tmpUidMutexFile, String pidFoundFile) {
        long timeout = getConfiguration().getMutexAcquireTimeout();
        String rmCmd = buildCommand("rm");
        String catCmd = buildCommand("cat");

        if (timeout < 1) {
            timeout = SolarisConfiguration.DEFAULT_MUTEX_ACQUIRE_TIMEOUT;
        }
        
        String uidMutexAcquireScript =
            "TIMEOUT=" + timeout + "; " +
            "echo $$ > " + tmpUidMutexFile + "; " +
            "while test 1; " +
            "do " +
            "ln -n " + tmpUidMutexFile + " " + uidMutexFile + " 2>/dev/null; " +
            "rc=$?; " +
            "if [ $rc -eq 0 ]; then\n" +
              rmCmd + " -f " + tmpUidMutexFile + "; " +
              "break; " +
            "fi\n" +
            "LOCKPID=`" + catCmd + " " + uidMutexFile + "`; " +
            "if [ \"$LOCKPID\" = \"$$\" ]; then " +
              rmCmd + " -f " + uidMutexFile + "\n" +
            "else " +
              "ps -ef | while read REPLY\n" +
              "do " +
                "TESTPID=`echo $REPLY | awk '{ print $2 }'`; " +
                "if [ \"$LOCKPID\" = \"$TESTPID\" ]; then " +
                  "touch " + pidFoundFile + "; " +
                  "break; " +
                "fi\n" +
              "done\n" +
              "if [ ! -f " + pidFoundFile + " ]; then " +
                rmCmd + " -f " + uidMutexFile + "; " +
              "else " +
                rmCmd + " -f " + pidFoundFile + "; " +
              "fi\n" +
            "fi\n" +
            "if [ -f " + uidMutexFile + " ]; then " +
              "TIMEOUT=`echo | awk 'BEGIN { n = '$TIMEOUT' } { n -= 1 } END { print n }'`\n" +
              "if [ $TIMEOUT = 0 ]; then " +
                "echo \"ERROR: failed to obtain uid mutex\"; " +
                rmCmd + " -f " + tmpUidMutexFile + "; " +
                "break; " +
              "fi\n" +
              "sleep 1; " +
            "fi\n" +
          "done";

        return uidMutexAcquireScript;
    }

    /** Counterpart of {@link SolarisConnection#getAcquireMutexScript()} */
    private String getMutexReleaseScript(String uidMutexFile) {
        String rmCmd = buildCommand("rm");
        String pidMutexReleaseScript =
            "if [ -f " + uidMutexFile + " ]; then " +
              "LOCKPID=`cat " + uidMutexFile + "`; " +
              "if [ \"$LOCKPID\" = \"$$\" ]; then " +
                rmCmd + " -f " + uidMutexFile + "; " +
              "fi; " +
            "fi";
        return pidMutexReleaseScript;
    }
    
    private String getMutexReleaseScript() {
        return getMutexReleaseScript(pidMutexFile);
    }
    
    public void executeMutexAcquireScript() {
        executeCommand(getAcquireMutexScript(), CollectionUtil.newSet("ERROR"));
    }
    
    public void executeMutexAcquireScript(String uidMutexFile, String tmpUidMutexFile, String pidFoundFile) {
        executeCommand(getAcquireMutexScript(uidMutexFile, tmpUidMutexFile, pidFoundFile), CollectionUtil.newSet("ERROR"));
    }
    
    public void executeMutexReleaseScript() {
        executeCommand(getMutexReleaseScript());
    }
    
    public void executeMutexReleaseScript(String uidMutexFile) {
        executeCommand(getMutexReleaseScript(uidMutexFile));
    }
    
    public void checkAlive() {
        String out = executeCommand("echo 'checkAlive'");
        if (StringUtil.isBlank(out) || !out.contains("checkAlive")) {
            throw new RuntimeException("Solaris Connector no longer alive.");
        }
    }
    
    /*
     * SUDO
     */
    private static final String SUDO_START_COMMAND = "sudo -v";
    private static final String SUDO_RESET_COMMAND = "sudo -k";
    
    public void doSudoStart() {
        final SolarisConfiguration config = getConfiguration();
        if (config.isSudoAuthorization()) {
            try {
                // 1) send sudo reset command
                executeCommand(SUDO_RESET_COMMAND, CollectionUtil.newSet("not found"));

                // 2) send sudo start command
                executeCommand(SUDO_START_COMMAND, Collections.<String>emptySet(), CollectionUtil.newSet("assword:")); 

                GuardedString passwd = config.getCredentials();
                sendPassword(passwd, CollectionUtil.newSet("may not run", "not allowed to execute"), Collections.<String>emptySet(), this);
            } catch (Exception e) {
                throw ConnectorException.wrap(e);
            }
        }
    }
    
    public void doSudoReset() {
        final SolarisConfiguration config = getConfiguration();
        if (config.isSudoAuthorization()) {
            // send sudo reset command
            executeCommand(SUDO_RESET_COMMAND);
        }
    }
    
    public boolean isNis() {
        final String sysDB = getConfiguration().getSystemDatabaseType();
        return sysDB != null && sysDB.equalsIgnoreCase("nis");
    }
    
    public boolean isDefaultNisPwdDir() {
        return configuration.getNisPwdDir().equals(SolarisConfiguration.DEFAULT_NISPWDDIR);
    }

    /**
     * Use this class to construct a sequence of matchers.
     * Matchers consists of two parts:
     * <ul>
     *  <li>1) regular expression -- used to detect the match,</li>
     *  <li>2) closure -- call-back interface that is executed upon the match.</li>
     * </ul>
     * @author David Adam
     */
    private final static class MatchBuilder {
        private List<Match> matches;

        public MatchBuilder() {
            matches = new ArrayList<Match>();
        }

        /**
         * adds a case sensitive matcher. Compare with
         * {@link MatchBuilder#addCaseInsensitiveRegExpMatch(String, SolarisClosure)}.
         */
        public void addRegExpMatch(String regExp, SolarisClosure closure) {
            try {
                matches.add(new RegExpMatch(regExp, closure));
            } catch (MalformedPatternException ex) {
                throw ConnectorException.wrap(ex);
            }
        }
        
        /**
         * adds a case *insensitive matcher. Compare with
         * {@link MatchBuilder#addRegExpMatch(String, SolarisClosure)}
         */
        public void addCaseInsensitiveRegExpMatch(String regExp, SolarisClosure closure) {
            try {
                matches.add(new RegExpCaseInsensitiveMatch(regExp, closure));
            } catch (MalformedPatternException ex) {
                throw ConnectorException.wrap(ex);
            }
        }
        
        /** add a timeout match with given 'millis' period. */
        public void addTimeoutMatch(long millis, SolarisClosure closure) {
            matches.add(new TimeoutMatch(millis, closure));
        }

        public Match[] build() {
            return matches.toArray(new Match[matches.size()]);
        }
    }// MatchBuilder
    
    /**
     * internal Closure to hold the buffer state of the resource, plus indicator of match.
     */
    private class SolarisClosure implements Closure {
        private String buffer;
        private boolean isReject;
        
        /** @return the buffer content up to the matched string */
        public String getMatchedBuffer() {
            return buffer;
        }
        
        public boolean isMatched() {
            return isReject;
        }
        
        public void run(ExpectState state) throws Exception {
            buffer = state.getBuffer();
            isReject = true;
        }
    }
    
    private class ErrorClosure extends SolarisClosure {
        private final ErrorHandler errHandler;
        
        public ErrorClosure(ErrorHandler errHandler) {
            this.errHandler = errHandler;
        }

        public ErrorHandler getErrorHandler() {
            return errHandler;
        }
    }
    
    /**
     * Call-back interface for {@link SolarisConnection}. Used for customizable
     * exception messages.
     * 
     * <p>
     * If an error is detected, {@link ErrorHandler#handle(String)} method will
     * be called with the error message received from the resource.
     */
    public interface ErrorHandler {
        /**
         * typically throws a customized ConnectorException, with a message,
         * possibly including buffer's state.
         */
        public void handle(String buffer);
    }
}
