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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.command.RegExpCaseInsensitiveMatch;
import org.identityconnectors.solaris.operation.OpCreateImpl;
import org.identityconnectors.solaris.operation.OpUpdateImpl;

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
 * The connection offers the following authentication types: SSH, Telnet and SSH Pubkey.
 * 
 * @author David Adam
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

    /** set the timeout for waiting on reply. */
    public static final int WAIT = 24000;
    
    //TODO might be a configuration property
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
    
    private String _rootShellPrompt;
    private Expect4j _expect4j;
    
    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration _configuration;
    public SolarisConfiguration getConfiguration() {
        return _configuration;
    }

    private final Log log = Log.getLog(SolarisConnection.class);
    

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
        
        _rootShellPrompt = _configuration.getRootShellPrompt();

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
            // throw new
            // UnsupportedOperationException("Telnet access not yet implemented: TODO");
            _expect4j = createTelnetConn(username, password);
            break;
        }
        
        try {
            if (!connType.selfAuthenticates()) {
                /*
                 * telnet doesn't authenticate automatically, so an extra step is needed:
                 */
                executeCommand(null, Collections.<String>emptySet(), CollectionUtil.newSet("login"));
                executeCommand(username.trim(), Collections.<String>emptySet(), CollectionUtil.newSet("assword"));
                sendPassword(password, this);
            }
            
            executeCommand(null/* no command sent here */, CollectionUtil.newSet("incorrect"));
            /*
             * turn off the echoing of keyboard input on the resource.
             * Saves bandwith too.
             */
            executeCommand("stty -echo");
            
            /*
             * Change root shell prompt, for simplier parsing of the output.
             * Revert the changes after the connection is closed.
             */
            _rootShellPrompt = CONNECTOR_PROMPT;
            executeCommand("PS1=\"" + CONNECTOR_PROMPT + "\"");
        } catch (Exception e) {
            throw new ConnectorException(String.format("Connection failed to host '%s:%s' for user '%s'", _configuration.getHostNameOrIpAddr(), _configuration.getPort(), username), e);
        }
    }

    private Expect4j createTelnetConn(String username, GuardedString password) {
        Expect4j expect4j = null;
        try {
            expect4j = ExpectUtils.telnet(_configuration
                    .getHostNameOrIpAddr(), _configuration.getPort());
        } catch (Exception e1) {
            throw ConnectorException.wrap(e1);
        }
        return expect4j;
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
    private void sendInternal(String string) throws IOException {
        _expect4j.send(string + HOST_END_OF_LINE_TERMINATOR);
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
                    // send the password
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
     * Execute a issue a command on the resource. Return the match of feedback
     * up to the root shell prompt
     * {@link SolarisConnection#getRootShellPrompt()}.
     * 
     * @param command
     *            the executed command
     * 
     * @param rejects
     *            Map that contains error message, {@link SolarisConnection.ErrorHandler} pairs.
     *            If the error message is found in response from the resource,
     *            the error handler is called. .
     *            <p>
     * 
     * @param accepts
     *            these are accepting strings, if they are found the result is
     *            returned. If empty set is given, the default accept is
     *            {@link SolarisConnection#getRootShellPrompt()}. Note: in case
     *            <code>accepts</code> parameter is specified, it'll be the last
     *            element in the response from the resource.
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
        try {
            if (command != null) {
                sendInternal(command);
            }
        } catch (Exception e) {
            // TODO finish ex...
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
            builder.addCaseInsensitiveRegExpMatch(rejEntry.getKey(), cec);
        }
        
        // #2 Adding RootShellPrompt matcher or other acceptance matchers if given
        List<SolarisClosure> captureClosures = null;
        if (accepts.size() > 0) {
            captureClosures = CollectionUtil.newList();
            for (String acc : accepts) {
                SolarisClosure closure = new SolarisClosure();
                captureClosures.add(closure);
                builder.addRegExpMatch(acc, closure);
            }
        } else {
            // by default rootShellPrompt is added.
            SolarisClosure closure = new SolarisClosure();
            captureClosures = CollectionUtil.newList(closure);
            builder.addRegExpMatch(getRootShellPrompt(), closure);
        }
        
        // #3 set the timeout for matching too
        final boolean[] isTimeoutMatched = new boolean[1];
        builder.addTimeoutMatch(WAIT, new SolarisClosure() {
            public void run(ExpectState state) throws Exception {
                isTimeoutMatched[0] = true;
            }
        });
        
        try {
            _expect4j.expect(builder.build());
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
            
            // no reject thrown and exception, so timeout occurred.
            if (isTimeoutMatched[0]) {
                throw new ConnectorException("executeCommand: timeout occured, and no ERROR or useful message matched.");
            }
        }
        
        output = trimOutput(output);

        return output;
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
            System.out.println("trying ...");
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
     * @throws {@link ConnectorException} in case of timeout in waiting for rootShellPrompt character.
     */
    public void waitForRootShellPrompt() {
        try {
            waitForImpl(getRootShellPrompt(), WAIT, false);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
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
        
        _expect4j.expect(builder.build());
        
        return buffer.toString();
    }

    /** once connection is disposed it won't be used at all. */
    public void dispose() {
        try {
            sendInternal("exit");
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
     * @param command
     *            the command can be a chain of strings separated by spaces. In
     *            case for some reason we want to delegate the chaining to this
     *            builder, we can use the additional arguments parameter.
     * @param arguments
     *            optional parameter for chaining extra arguments at the end of
     *            command.
     */
    public String buildCommand(String command, CharSequence... arguments) {
        StringBuilder buff = new StringBuilder();
        if (_configuration.isSudoAuth()) {
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
        return _rootShellPrompt;
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
     * multiple users. These conditions are present at {@link OpCreateImpl} and
     * {@link OpUpdateImpl}. The code is taken from the resource adapter.
     */
    private String getAcquireMutexScript() {
        // This code is from SolarisResouceAdapter
        Long timeout = getConfiguration().getMutexAcquireTimeout();
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
        Long timeout = getConfiguration().getMutexAcquireTimeout();
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
    
    /*
     * SUDO
     */
    private static final String SUDO_START_COMMAND = "sudo -v";
    private static final String SUDO_RESET_COMMAND = "sudo -k";
    
    // purely based on RA, TODO test 
    public void doSudoStart() {
        final SolarisConfiguration config = getConfiguration();
        if (config.isSudoAuth()) {
            try {
                // 1) send sudo reset command
                executeCommand(SUDO_RESET_COMMAND, CollectionUtil.newSet("not found"));

                // 2) send sudo start command
                executeCommand(SUDO_START_COMMAND, Collections.<String>emptySet(), CollectionUtil.newSet("assword:")); 

                // TODO evaluate which password should be used:
                GuardedString passwd = config.getPassword();
                sendPassword(passwd, CollectionUtil.newSet("may not run", "not allowed to execute"), Collections.<String>emptySet(), this);
            } catch (Exception e) {
                throw ConnectorException.wrap(e);
            }
        }
    }
    
    // purely based on RA, TODO test 
    public void doSudoReset() {
        final SolarisConfiguration config = getConfiguration();
        if (config.isSudoAuth()) {
            // send sudo reset command
            executeCommand(SUDO_RESET_COMMAND);
        }
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
    private final class MatchBuilder {
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
