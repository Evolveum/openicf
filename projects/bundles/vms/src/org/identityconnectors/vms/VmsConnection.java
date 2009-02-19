/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.vms;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.ExpectUtils;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

public class VmsConnection {
    private Log                 log = Log.getLog(VmsConnection.class);
    private Expect4j	        _expect4j;
    private VmsConfiguration    _configuration;
    private int                 _wait;
    private StringBuffer        _buffer;
    private ScriptExecutorFactory _scriptFactory;


    public VmsConnection(VmsConfiguration configuration, int wait) throws Exception {
        _wait = wait;
        _configuration = configuration;
        _buffer = new StringBuffer();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("connection", this);
        parameters.put("prompt", configuration.getHostShellPrompt());
        parameters.put("username", configuration.getUserName());
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        configuration.getPassword().access(accessor);
        char[] passwordArray = accessor.getArray();
        parameters.put("password", new String(passwordArray));
        parameters.put("host", configuration.getHostNameOrIpAddr());
        parameters.put("port", configuration.getHostPortNumber());
        try {
            //TODO: ExpectUtils needs a clear text password
            String password = new String(passwordArray);
            if (_configuration.getSSH())
                _expect4j = ExpectUtils.SSH(configuration.getHostNameOrIpAddr(), configuration.getUserName(), password, configuration.getHostPortNumber());
            else
                _expect4j = ExpectUtils.telnet(configuration.getHostNameOrIpAddr(), configuration.getHostPortNumber());
            _scriptFactory = ScriptExecutorFactory.newInstance(configuration.getScriptingLanguage());
            ScriptExecutor executor = _scriptFactory.newScriptExecutor(getClass().getClassLoader(), configuration.getConnectScript(), false);
            log.info("logging in connection, system:{0}, port {1}, user:{2}", configuration.getHostNameOrIpAddr(), configuration.getHostPortNumber(), configuration.getUserName());
            executor.execute(parameters);
            Arrays.fill(passwordArray, 0, passwordArray.length, ' ');
        } catch (Exception e) {
            Arrays.fill(passwordArray, 0, passwordArray.length, ' ');
            throw e;
        }
        send("SET TERM/NOECHO");
        waitFor(_configuration.getHostShellPrompt());
        send("SET PROMPT=\""+_configuration.getLocalHostShellPrompt()+"\"");
        waitFor(_configuration.getLocalHostShellPrompt());
    }

    public void dispose() {
        log.info("dispose()");
        _expect4j.close();
    }

    public void test() {
        try {
            resetStandardOutput();
            send("WRITE SYS$OUTPUT \"Hello, World\"");
            waitFor(_configuration.getLocalHostShellPrompt(), _wait);
            String result = getStandardOutput();
            if (!result.contains("Hello, World"))
                throw new ConnectorException(_configuration.getMessage(VmsMessages.TEST_FAILED));
        } catch (Exception e) {
            throw new ConnectorException(e);
        }
    }

    public Expect4j getExpect() {
        return _expect4j;
    }

    public void send(String string) throws IOException {
        log.info("send(''{0}'')", string);
        _expect4j.send(string+_configuration.getRealHostLineTerminator());
    }

    public void send(char[] string) throws IOException {
        //TODO: security -- Expect4J doesn't support send(char[])
        // so, we have to coerce back to a string, but, if this is updated
        // we'll be ready.
        log.info("send(encoded data)");
        _expect4j.send(new String(string)+_configuration.getRealHostLineTerminator());
    }

    public void waitFor(String string) throws Exception{
        log.info("waitFor(''{0}'')", string);
        _expect4j.expect(string, new Closure() {
            public void run(ExpectState state) {
                _buffer.append(state.getBuffer());
            }
        });
    }

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
                        ConnectorException e = new ConnectorException(_configuration.getMessage(VmsMessages.TIMEOUT_IN_MATCH, string));
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
}
