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
import java.util.List;
import java.util.Set;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.command.ClosureFactory;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.command.ClosureFactory.CaptureClosure;
import org.identityconnectors.solaris.command.ClosureFactory.ConnectorExceptionClosure;
import org.identityconnectors.solaris.operation.OpCreateImpl;
import org.identityconnectors.solaris.operation.OpUpdateImpl;

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
                waitFor("login");
                send(username.trim());
                waitForCaseInsensitive("assword");
                SolarisUtil.sendPassword(password, Collections.<String>emptySet(), this);
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
    public void send(String string) throws IOException {
        _expect4j.send(string + HOST_END_OF_LINE_TERMINATOR);
    }
    
    /**
     * {@see SolarisConnection#waitFor(String, int)}
     */
    public String waitFor(final String string) throws Exception {
        return waitFor(string, WAIT);
    }
    
    /** do case insensitive match and wait for */
    public String waitForCaseInsensitive(final String string) throws Exception {
        return waitForImpl(string, WAIT, true);
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
    
    private String waitForImpl(final String string, final int millis, boolean caseInsensitive) throws MalformedPatternException, Exception {
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
        builder.addTimeoutMatch(millis, new Closure() {
            public void run(ExpectState state) throws Exception {
                String msg = String.format("Timeout in waitFor('%s', %s) buffer: <%s>",
                        string, Integer.toString(millis), state.getBuffer());
                throw new ConnectorException(msg);
            }
        });
        
        _expect4j.expect(builder.build());
        
        return buffer.toString();
    }
    
    /** 
     * {@see SolarisConnection#executeCommand(String, Set, Set)} 
     */
    public String executeCommand(String command) {
        return executeCommand(command, Collections.<String>emptySet());
    }
    
    /**
     * {@see SolarisConnection#executeCommand(String, Set, Set)}
     */
    public String executeCommand(String command, Set<String> rejects) {
        return executeCommand(command, rejects, Collections.<String>emptySet());
    }

    /**
     * Execute a issue a command on the resource. Return the match of feedback
     * up to the root shell prompt
     * {@link SolarisConnection#getRootShellPrompt()}.
     * 
     * @param command
     *            the executed command
     * @param rejects
     *            the error messages that can occur. If they are found, a
     *            {@link ConnectorException} is thrown.
     * @param accepts
     *            these are accepting strings, if they are found the result is
     *            returned. If empty set is given, the default accept is
     *            {@link SolarisConnection#getRootShellPrompt()}.
     */
    public String executeCommand(String command, Set<String> rejects, Set<String> accepts) {
        try {
            if (command != null) {
                send(command);
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
        final List<ConnectorExceptionClosure> cecList = new ArrayList<ConnectorExceptionClosure>();
        for (String rej : rejects) {
            ConnectorExceptionClosure cec = ClosureFactory.newConnectorException();
            cecList.add(cec);
            builder.addRegExpMatch(rej, cec);
        }
        
        // #2 Adding RootShellPrompt matcher or other acceptance matchers if given
        List<CaptureClosure> captureClosures = null;
        if (accepts.size() > 0) {
            captureClosures = CollectionUtil.newList();
            for (String acc : accepts) {
                CaptureClosure closure = ClosureFactory.newCaptureClosure();
                captureClosures.add(closure);
                builder.addRegExpMatch(acc, closure);
            }
        } else {
            CaptureClosure closure = ClosureFactory.newCaptureClosure();
            captureClosures = CollectionUtil.newList(closure);
            builder.addRegExpMatch(getRootShellPrompt(), closure);
        }
        
        // #3 set the timeout for matching too
        final boolean[] isTimeoutMatched = new boolean[1];
        builder.addTimeoutMatch(WAIT, new Closure() {
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
        for (CaptureClosure cl : captureClosures) {
            if (cl.isMatched()) {
                // get regular output matched by root shell prompt.
                output = cl.getMsg();
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

    private void handleRejects(List<ConnectorExceptionClosure> cecList) {
        for (ConnectorExceptionClosure connectorExceptionClosure : cecList) {
            if (connectorExceptionClosure.isMatched()) {
                String out = connectorExceptionClosure.getErrMsg();

                out = waitForInput(out);

                throw new ConnectorException("ERROR OUTPUT: " + out);
            }
        }
    }
    
    private String waitForInput(final String out) {
        StringBuilder buffer = new StringBuilder(out);
        long start = System.nanoTime();
        while (System.nanoTime() - start <= ERROR_WAIT) {
            System.out.println("trying ...");
            String tmp = null;
            try {
                tmp = waitFor(".+", WAITFOR_TIMEOUT_FOR_ERROR);
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
        String mutexOut = executeCommand(getAcquireMutexScript());
        if (mutexOut.contains("ERROR")) {
            throw new ConnectorException("error when acquiring mutex. Buffer content: <" + mutexOut + ">");
        }
    }
    
    public void executeMutexAcquireScript(String uidMutexFile, String tmpUidMutexFile, String pidFoundFile) {
        String mutexOut = executeCommand(getAcquireMutexScript(uidMutexFile, tmpUidMutexFile, pidFoundFile));
        if (mutexOut.contains("ERROR")) {
            throw new ConnectorException("error when acquiring mutex. Buffer content: <" + mutexOut + ">");
        }
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
                send(SUDO_RESET_COMMAND); 

                // 2) send sudo start command
                send(SUDO_START_COMMAND); 
                waitForCaseInsensitive("assword:");
                // TODO evaluate which password should be used:
                GuardedString passwd = config.getPassword();
                SolarisUtil.sendPassword(passwd, CollectionUtil.newSet("may not run", "not allowed to execute"), this);
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
    
    /*
     * there is only one usage of this in {@link OpAuthenticateImpl}
     * It should be avoided FIXME.
     * @param matches
     */
    @Deprecated
    public void expect(Match[] matches) {
        try {
            _expect4j.expect(matches);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

}
