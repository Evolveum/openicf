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
package org.identityconnectors.rw3270;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.RW3270Connection;

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
    protected PoolableConnectionConfiguration _config;
    protected Expect4j                  _expect4j;
    protected RW3270IOPair              _ioPair;
    protected Pattern                   _commandPattern = Pattern.compile("(?<!\\[)\\[([^]]*)\\]");
    protected Pattern                   _pfPattern      = Pattern.compile("PF(\\d+)", Pattern.CASE_INSENSITIVE);
    protected Pattern                   _paPattern      = Pattern.compile("PA(\\d+)", Pattern.CASE_INSENSITIVE);
    protected Pattern                   _cursorPattern  = Pattern.compile("cursor\\s*\\(\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    protected static final String       CLEAR            = "CLEAR";
    protected static final String       ENTER            = "ENTER";

    public RW3270BaseConnection(PoolableConnectionConfiguration config) throws NamingException {
        _config = config;
        _buffer = new StringBuffer();
        _model = 2;
        _semaphore = new Semaphore(0);
        _expect4j = new Expect4j(_ioPair = new RW3270IOPair(this));
    }

    protected abstract void sendKeys(String keys);
    protected abstract void sendEnter();
    protected abstract void sendPAKeys(int pa);
    protected abstract void sendPFKeys(int pf);
    protected abstract void setCursorPos(short pos);
    protected abstract void waitForUnlock() throws InterruptedException ;
    protected abstract void clearAndUnlock() throws InterruptedException;
    protected abstract String getDisplay();
    
    public void reset() {
        dispose();
        connect();
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
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String)
     */
    public void waitFor(String expression) {
        waitForLocal(expression, null);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, int)
     */
    public void waitFor(final String expression, int timeOut) {
        waitForLocal(expression, new Integer(timeOut));
    }

    private void waitForLocal(String expression, Integer timeout) {
        try {
            List<Match> matches = new LinkedList<Match>();
            matches.add(new RegExpMatch(expression, new Closure() {
                public void run(ExpectState state) throws Exception {
                    state.addVar("timeout", Boolean.FALSE);
                }
            }));
            if (timeout != null)
                matches.add(new TimeoutMatch(timeout, new Closure() {
                    public void run(ExpectState state) throws Exception {
                        _buffer.append(state.getBuffer());
                        state.addVar("timeout", Boolean.TRUE);
                    }
                }));
            _expect4j.expect(matches);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        Boolean isTimeout = (Boolean)_expect4j.getLastState().getVar("timeout");
        if (isTimeout==null || isTimeout.booleanValue())
            throw new ConnectorException(_config.getConnectorMessages().format("IsAlive", "timed out waiting for ''{0}'':''{1}''", expression, getStandardOutput()));
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
    
    private void waitForLocal(final String expression0, final String expression1,
            Integer timeout) {
        try {
            List<Match> matches = new LinkedList<Match>();
            final Pattern pattern = Pattern.compile(expression0);
            matches.add(new RegExpMatch(expression0, new Closure() {
                public void run(ExpectState state) throws Exception {
                    // Need to strip off the match
                    //
                    String data = state.getBuffer();
                    Matcher matcher = pattern.matcher(data);
                    if (matcher.find()) {
                        data = data.substring(0, matcher.start());
                    }
                    _buffer.append(data);
                    clearAndUnlock();
                    sendEnter();
                    state.exp_continue();
                }
            }));
            matches.add(new RegExpMatch(expression1, new Closure() {
                public void run(ExpectState state) throws Exception {
                    String data = state.getBuffer();
                    _buffer.append(data);
                    state.addVar("timeout", Boolean.FALSE);
                }
            }));
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
                    expression1, getStandardOutput()));
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
}
