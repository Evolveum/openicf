/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.rw3270;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.RW3270Connection;



public abstract class RW3270BaseConnection implements RW3270Connection {
    protected String                    _lastConnError;
    protected int                       _model;
    protected Semaphore                 _semaphore;
    protected StringBuffer              _buffer;
    protected PoolableConnectionConfiguration _config;
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
                        throw new IllegalArgumentException(_config.getConnectorMessages().format(_config.getLocale(), "IllegalPA", "Illegal PA key:{0}", match));
                    sendPAKeys(number);
                    waitForUnlock();
                } else if (pfMatcher.matches()) {
                    int number = Integer.parseInt(pfMatcher.group(1));
                    if (number<1 || number>24)
                        throw new IllegalArgumentException(_config.getConnectorMessages().format(_config.getLocale(), "IllegalPF", "Illegal PF key:{0}", match));
                    sendPFKeys(number);
                    waitForUnlock();
                } else if (cursorMatcher.matches()) {
                    short cursor = Short.parseShort(cursorMatcher.group(1));
                    setCursorPos(cursor);
                } else {
                    throw new IllegalArgumentException(_config.getConnectorMessages().format(_config.getLocale(), "IllegalCommand", "Illegal Command:{0}", match));
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
    public void waitFor(String expression) {
        try {
            while (true) {
                _semaphore.acquire();
                String display = getDisplay();
                if (display.contains(expression)) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, java.lang.String)
     */
    public void waitFor(String expression0, String expression1) {
        try {
            while (true) {
                _semaphore.acquire();
                String display = getDisplay();
                //dump();
                // This is used to signal commands which may continue over
                // multiple screens.
                //  expression[0] signals more data is to come
                //  expression[1] signals all data has been presented
                //
                boolean endsWith0 = display.trim().endsWith(expression0); 
                boolean endsWith1 = display.trim().endsWith(expression1); 
                if (endsWith0) {
                    // Save the existing screen's data
                    // 
                    _buffer.append(display.substring(0, display.lastIndexOf(expression0)));
                    clearAndUnlock();
                    sendEnter();
                } else if (endsWith1) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, int)
     */
    public void waitFor(final String expression, int timeOut) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    waitFor(expression);
                } catch (Exception e) {
                    throw new ConnectorException(e);
                }
            }
        };
        thread.start();
        try {
            thread.join(timeOut);
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (thread.isAlive())
            throw new ConnectorException(_config.getConnectorMessages().format(_config.getLocale(), "IsAlive", "timed out waiting for ''{0}'':''{1}''", expression, getStandardOutput()));
        if (!getStandardOutput().contains(expression))
            throw new ConnectorException(_config.getConnectorMessages().format(_config.getLocale(), "NotFound", "''{0}'' not found; instead had ''{1}''", expression, getStandardOutput()));
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.RW3270Connection#waitFor(java.lang.String, java.lang.String, int)
     */
    public void waitFor(final String expression0, final String expression1, int timeOut) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    waitFor(expression0, expression1);
                } catch (Exception e) {
                    throw new ConnectorException(e);
                }
            }
        };
        thread.start();
        try {
            thread.join(timeOut);
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (thread.isAlive())
            throw new ConnectorException(_config.getConnectorMessages().format(_config.getLocale(), "IsAlive2", "timed out waiting for ''{0}'' or ''{1}'':''{2}''", expression0, expression1, getStandardOutput().trim()));
        if (!getStandardOutput().contains(expression1))
            throw new ConnectorException(_config.getConnectorMessages().format(_config.getLocale(), "NotFound", "''{0}'' not found; instead had ''{1}''", expression1, getStandardOutput()));
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
