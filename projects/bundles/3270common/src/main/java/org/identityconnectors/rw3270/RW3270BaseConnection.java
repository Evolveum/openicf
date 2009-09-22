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
package org.identityconnectors.rw3270;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.oro.text.regex.MalformedPatternException;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

public abstract class RW3270BaseConnection implements RW3270Connection {
    protected String                    _lastConnError;
    protected int                       _model;
    protected Semaphore                 _semaphore;
    protected StringBuffer              _buffer;
    protected RW3270Configuration       _config;
    protected Expect4j                  _expect4j;
    protected RW3270IOPair              _ioPair;
    protected Pattern                   _commandPattern = Pattern.compile("(?<!\\[)\\[([^]]*)\\]");
    protected Pattern                   _pfPattern      = Pattern.compile("PF(\\d+)", Pattern.CASE_INSENSITIVE);
    protected Pattern                   _paPattern      = Pattern.compile("PA(\\d+)", Pattern.CASE_INSENSITIVE);
    protected Pattern                   _cursorPattern  = Pattern.compile("cursor\\s*\\(\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    protected static final String       CLEAR            = "CLEAR";
    protected static final String       ENTER            = "ENTER";
    protected ScriptExecutor            _connectScriptExecutor;
    protected ScriptExecutor            _disconnectScriptExecutor;

    public RW3270BaseConnection(RW3270Configuration config) throws NamingException {
        _config = config;
        _buffer = new StringBuffer();
        _model = 2;
        _semaphore = new Semaphore(0);
        _expect4j = new Expect4j(_ioPair = new RW3270IOPair(this));
        ScriptExecutorFactory factory = ScriptExecutorFactory.newInstance(config.getConnectScript().getScriptLanguage());
        _connectScriptExecutor = factory.newScriptExecutor(getClass().getClassLoader(), config.getConnectScript().getScriptText(), true);
         factory = ScriptExecutorFactory.newInstance(config.getDisconnectScript().getScriptLanguage());
        _disconnectScriptExecutor = factory.newScriptExecutor(getClass().getClassLoader(), config.getDisconnectScript().getScriptText(), true);
    }

    public abstract void sendKeys(String keys);
    public abstract void sendEnter();
    public abstract void sendPAKeys(int pa);
    public abstract void sendPFKeys(int pf);
    public abstract void setCursorPos(short pos);
    public abstract void waitForUnlock() throws InterruptedException ;
    public abstract void clearAndUnlock() throws InterruptedException;
    public abstract String getDisplay();
    
    public void reset() {
        dispose();
        connect();
    }
    
    public RW3270Configuration getConfiguration() {
    	return _config;
    }

    public void resetStandardOutput() {
        _buffer.setLength(0);
        _ioPair.reset();
    }

    public void loginUser() {
        try {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("SHORT_WAIT", 15000);
            arguments.put("USERNAME", _config.getUserName());
            arguments.put("PASSWORD", _config.getPassword());
            arguments.put("connection", this);
            _connectScriptExecutor.execute(arguments);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    public void logoutUser() {
        try {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("SHORT_WAIT", 5000);
            arguments.put("USERNAME", _config.getUserName());
            arguments.put("PASSWORD", _config.getPassword());
            arguments.put("connection", this);
            _disconnectScriptExecutor.execute(arguments);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public int getWidth() {
        switch (_model) {
        case 1 :
            return 40;
        case 2 :
        case 3 :
        case 4 :
            return 80;
        case 5 :
            return 132;
        }
        return 0;
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#send(java.lang.String)
     */
    public void send(GuardedString command) {
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        command.access(accessor);
        char[] string = accessor.getArray();
        try {
            send(string);
        } finally {
            accessor.clear();
            Arrays.fill(string, 0, string.length, ' ');
        }
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#send(java.lang.String)
     */
    public void send(char[] command) {
        send(new String(command));
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#send(java.lang.String)
     */
    public void send(String command) {
        try {
            _expect4j.send(command);
        } catch (IOException e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    public void sendFromIOPair(String command) {
        try {
            waitForUnlock();

            // Loop through the string, extracting any commands
            //
            Matcher matcher = _commandPattern.matcher(command);
            int start = 0;
            while (matcher.find(start)) {
                String prefix = command.substring(start, matcher.start());
                sendKeys(prefix);
                String match = matcher.group(1).trim();
                Matcher paMatcher = _paPattern.matcher(match);
                Matcher pfMatcher = _pfPattern.matcher(match);
                Matcher cursorMatcher = _cursorPattern.matcher(match);

                if (CLEAR.equalsIgnoreCase(match)) {
                    clearAndUnlock();
                } else if (ENTER.equalsIgnoreCase(match)) {
                    sendEnter();
                } else if (paMatcher.matches()) {
                    int number = Integer.parseInt(paMatcher.group(1));
                    if (number<1 || number>3)
                        throw new IllegalArgumentException(_config.getConnectorMessages().format("IllegalPA", "Illegal PA key:{0}", match));
                    sendPAKeys(number);
                    waitForUnlock();
                } else if (pfMatcher.matches()) {
                    int number = Integer.parseInt(pfMatcher.group(1));
                    if (number<1 || number>24)
                        throw new IllegalArgumentException(_config.getConnectorMessages().format("IllegalPF", "Illegal PF key:{0}", match));
                    sendPFKeys(number);
                    waitForUnlock();
                } else if (cursorMatcher.matches()) {
                    short cursor = Short.parseShort(cursorMatcher.group(1));
                    setCursorPos(cursor);
                } else {
                    throw new IllegalArgumentException(_config.getConnectorMessages().format("IllegalCommand", "Illegal Command:{0}", match));
                }
                start = matcher.end();
            }
            sendKeys(command.substring(start));
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        } catch (NumberFormatException e) {
            throw new ConnectorException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String)
     */
    private String _lastDisplay = "";
    public synchronized String waitForInput() {
        try {
            _semaphore.acquire();
            // Eliminate any trailing blank lines
            //
            StringBuffer buffer = new StringBuffer(getDisplay());
            int last = buffer.length();
            while (last>0)
                if (buffer.charAt(--last)!=' ')
                    break;
            last += getWidth()-(last%getWidth());
            String value = "";
            buffer.setLength(last);
            String display = buffer.toString();
            if (display.startsWith(_lastDisplay)) {
                value = display.substring(_lastDisplay.length());
            } else {
                value = display;
            }
            _lastDisplay = display;
            return value;
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(expect4j.matches.Match[])
     */
    public void waitFor(Match[] matches) {
        try {
            _expect4j.expect(matches);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String)
     */
    public void waitFor(String expression) {
        waitForLocal(null, expression, null);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, int)
     */
    public void waitFor(final String expression, int timeOut) {
        waitForLocal(null, expression, new Integer(timeOut));
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, java.lang.String)
     */
    public void waitFor(String expression0, String expression1) {
        waitForLocal(expression0, expression1, null);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, java.lang.String, int)
     */
    public void waitFor(String expression0, String expression1, int timeOut) {
        waitForLocal(expression0, expression1, new Integer(timeOut));
    }
    
    private int count = 0;
    private void waitForLocal(final String continue_regexp, final String complete_regexp, Integer timeout) {
        try {
            List<Match> matches = new LinkedList<Match>();
            
            // Match the continue expression, so
            //  save the partial output
            //  ask for more output
            //
            if (continue_regexp!=null) {
                matches.add(new RegExpMatch(continue_regexp, new Closure() {
                    public void run(ExpectState state) throws Exception {
                        // Need to strip off the match
                        //
                        String data = state.getBuffer();
                        
                        data = data.substring(0, state.getMatchedWhere());
                        _buffer.append(data);
                        //System.out.println("+++continue("+count+++")\n:"+_buffer.toString().replaceAll("(.{80})", "$1\n"));
                        clearAndUnlock();
                        sendEnter();
                        state.exp_continue();
                    }
                }));
            }
            
            // Match the command complete expression, so
            //  if there was an error,
            //      throw exception
            //  else
            //      save the final output
            matches.add(new RegExpMatch(complete_regexp, new Closure() {
                public void run(ExpectState state) throws Exception {
                    String data = state.getBuffer();
                    _buffer.append(data);
                    //System.out.println("+++complete("+count+++")\n:"+_buffer.toString().replaceAll("(.{80})", "$1\n"));
                    Object errorDetected = state.getVar("errorDetected");
                    state.addVar("timeout", Boolean.FALSE);
                    state.addVar("errorDetected", null);
                    //if (errorDetected!=null)
                    //    throw new XXX();;
                }
            }));
            
            // Match the error expression, so
            //  send the abort command
            //  continue execution, to see if we can recover
            //
            /*
            matches.add(new RegExpMatch(expression2, new Closure() {
                public void run(ExpectState state) throws Exception {
                    state.addVar("errorDetected", state.getBuffer());
                    // Need to strip off the match
                    //
                    String data = state.getBuffer();
                    Matcher matcher = pattern.matcher(data);
                    if (matcher.find()) {
                        data = data.substring(0, matcher.start());
                    }
                    _buffer.append(data);
                    clearAndUnlock();
                    sendPAKeys(1);
                    state.exp_continue();
                }
            }));
            */
            if (timeout != null)
                matches.add(new TimeoutMatch(timeout, new Closure() {
                    public void run(ExpectState state) throws Exception {
                        state.addVar("timeout", Boolean.TRUE);
                    }
                }));
            _expect4j.expect(matches);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        Boolean isTimeout = (Boolean)_expect4j.getLastState().getVar("timeout"); 
        if (isTimeout==null || isTimeout.booleanValue())
            throw new ConnectorException(_config.getConnectorMessages().format(
                    "IsAlive", "timed out waiting for ''{0}'':''{1}''",
                    complete_regexp, getStandardOutput()));
    }
    
    private static class GuardedStringAccessor implements GuardedString.Accessor {
        private char[] _array;
        
        public void access(char[] clearChars) {
            _array = new char[clearChars.length];
            System.arraycopy(clearChars, 0, _array, 0, _array.length);
        }
        
        public char[] getArray() {
            return _array;
        }

        public void clear() {
            Arrays.fill(_array, 0, _array.length, ' ');
        }
    }
    
    protected Properties asProperties(String[] array) {
        if (array==null)
            return null;
        Properties properties = new Properties();
        for (int i=0; i<array.length; i+=2)
            properties.put(array[i], array[i+1]);
        return properties;
    }
}
