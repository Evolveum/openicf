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

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.naming.NamingException;

import junit.framework.Assert;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.rw3270.PoolableConnectionFactory.ConnectionInfo;
import org.junit.BeforeClass;
import org.junit.Test;


public class RW3270ConnectionPoolTests {

    // Connector Configuration information
    //
    private static String HOST_NAME;
    private static String SYSTEM_PASSWORD;
    private static String SYSTEM_USER;

    private static final int     HOST_TELNET_PORT    = 23;
    private static final Boolean USE_SSL             = Boolean.FALSE;


    @BeforeClass
    public static void before() {
        HOST_NAME         = "HOST";
        SYSTEM_PASSWORD   = "SYSTEM_PASSWORD";
        SYSTEM_USER       = "SYSTEM_USER";
    }

    @Test
    public void testNegativePath() {
        OurConfiguration configuration = createConfiguration();
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("XYZZY");
            final DummyConnection connection = (DummyConnection)info.getConnection();
            connection.setDisplay("Nothing");
            
            try {
                info.getConnection().waitFor("USER=", 100);
                Assert.fail("exception expected");
            } catch (RuntimeException e) {
            }
            try {
                info.getConnection().waitFor("USER=", "C", 100);
                Assert.fail("exception expected");
            } catch (RuntimeException e) {
            }
            Thread thread1 = new Thread() {
                public void run() {
                    try {
                        connection.waitFor("USER=");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread1.start();
            thread1.join(5000);
            Assert.assertTrue(thread1.isAlive());

            Thread thread2 = new Thread() {
                public void run() {
                    try {
                        connection.waitFor("USER=", "X");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread2.start();
            thread2.join(5000);
            Assert.assertTrue(thread2.isAlive());
            pool.clear();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testTelnetConnectionViaPool() {
        OurConfiguration configuration = createConfiguration();
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("XYZZY");
            info.getConnection().send("Login[enter]");
            info.getConnection().send("[clear]");
            info.getConnection().send("[cursor (256)]");
            try {
                info.getConnection().send("[bogus]");
                Assert.fail("no error thrown");
            } catch (IllegalArgumentException e) {
            }
            sendRelease((DummyConnection)info.getConnection(), "USER=IDM03");
            info.getConnection().waitFor("USER=IDM03");
            sendRelease((DummyConnection)info.getConnection(), "USER=", "IDM03");
            info.getConnection().waitFor("USER=", "IDM03");
            sendRelease((DummyConnection)info.getConnection(), "USER=IDM03");
            info.getConnection().waitFor("USER=IDM03", 3000);
            sendRelease((DummyConnection)info.getConnection(), "USER=", "IDM03");
            info.getConnection().waitFor("USER=", "IDM03", 3000);
            pool.returnObject("XYZZY", info);
            pool.clear();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testPaKeys() {
        OurConfiguration configuration = createConfiguration();
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("XYZZY");
            for (int i=1; i<4; i++)
                info.getConnection().send("[pa"+i+"]");
            try {
                info.getConnection().send("[pa0]");
                Assert.fail("bad PA Key not caught");
            } catch (RuntimeException e) {
            }
            try {
                info.getConnection().send("[pa5]");
                Assert.fail("bad PA Key not caught");
            } catch (RuntimeException e) {
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testPfKeys() {
        OurConfiguration configuration = createConfiguration();
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("XYZZY");
            for (int i=1; i<25; i++)
                info.getConnection().send("[pf"+i+"]");
            try {
                info.getConnection().send("[pf0]");
                Assert.fail("bad PFA Key not caught");
            } catch (RuntimeException e) {
            }
            try {
                info.getConnection().send("[pf25]");
                Assert.fail("bad PF Key not caught");
            } catch (RuntimeException e) {
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    private void sendRelease(final DummyConnection connection, final String... display) {
        for (int i=0; i<display.length; i++) {
            final int index = i;
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000*index);
                    } catch (InterruptedException e) {
                    }
                    connection.setDisplay(display[index]);
                    connection.releaseSemaphore();
                }
            }.start();
        }
    }

    private OurConfiguration createConfiguration() {
        OurConfiguration config = new OurConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setHostTelnetPortNumber(HOST_TELNET_PORT);
        config.setUseSsl(USE_SSL);
        config.setConnectScript(getLoginScript());
        config.setDisconnectScript(getLogoffScript());
        config.setUserNames(new String[] { SYSTEM_USER });
        config.setPasswords(new GuardedString[] { new GuardedString(SYSTEM_PASSWORD.toCharArray()) });
        config.setPoolNames(new String[] { "XYZZY" });
        config.setEvictionInterval(60000);
        config.setConnectionClassName(DummyConnection.class.getName());

        OurConnectorMessages messages = new OurConnectorMessages();
        Map<Locale, Map<String, String>> catalogs = new HashMap<Locale, Map<String,String>>();
        ResourceBundle messagesBundle = ResourceBundle.getBundle("org.identityconnectors.rw3270.Messages");
        Map<String, String> foo = new HashMap<String, String>();
        Enumeration<String> enumeration = messagesBundle.getKeys();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            foo.put(key, messagesBundle.getString(key));
        }
        catalogs.put(Locale.getDefault(), foo);
        messages.setCatalogs(catalogs);
        config.setConnectorMessages(messages);
        return config;
    }
    
    private String getLoginScript() {
        String script =
            "";
        return script;
    }

    private String getLogoffScript() {
        String script =
            "";
        return script;
    }

    public static class DummyConnection extends RW3270BaseConnection {
        private StringBuffer sentData = new StringBuffer();
        private String display;
        
        public DummyConnection(PoolableConnectionConfiguration config) throws NamingException {
            super(config);
        }
        
        public void releaseSemaphore() {
            _semaphore.release();
        }

        protected void clearAndUnlock() throws InterruptedException {
        }
        
        protected void setDisplay(String display) {
            this.display = display;
        }

        protected String getDisplay() {
            return display;
        }

        protected void sendEnter() {
            sentData.append("[enter]");
        }

        protected void sendKeys(String keys) {
            sentData.append(keys);
        }

        protected void sendPAKeys(int pa) {
            sentData.append("[pa"+pa+"]");
        }

        protected void sendPFKeys(int pf) {
            sentData.append("[pf"+pf+"]");
        }

        protected void setCursorPos(short pos) {
        }

        protected void waitForUnlock() throws InterruptedException {
        }

        public void connect() {
        }

        public void dispose() {
        }

        public String getStandardOutput() {
            return getDisplay();
        }

        public String getSentData() {
            return sentData.toString();
        }

        public void resetStandardOutput() {
        }

        public void test() {
        }
    }

    public static class TestHandler implements ResultsHandler, Iterable<ConnectorObject> {
        private List<ConnectorObject> objects = new LinkedList<ConnectorObject>();

        public boolean handle(ConnectorObject object) {
            objects.add(object);
            return true;
        }

        public Iterator<ConnectorObject> iterator() {
            return objects.iterator();
        }

        public int size() {
            return objects.size();
        }
    }
    
    public static class OurConfiguration extends AbstractConfiguration implements PoolableConnectionConfiguration {
        private String _connectScript;
        private String _disconnectScript;
        private String _host;
        private Integer _port;
        private GuardedString[] _passwords;
        private String[] _poolNames;
        private String[] _userNames;
        private Integer _evictionInterval;
        private String _connectClass;
        private Boolean _useSsl ;

        public String getConnectScript() {
            return _connectScript;
        }

        public String getConnectionClassName() {
            return _connectClass;
        }

        public String getDisconnectScript() {
            return _disconnectScript;
        }

        public String getHostNameOrIpAddr() {
            return _host;
        }

        public Integer getHostTelnetPortNumber() {
            return _port;
        }

        public GuardedString[] getPasswords() {
            return _passwords;
        }

        public String[] getPoolNames() {
            return _poolNames;
        }

        public Boolean getUseSsl() {
            return _useSsl;
        }

        public String[] getUserNames() {
            return _userNames;
        }

        public void setConnectScript(String script) {
            _connectScript = script;
        }

        public void setConnectionClassName(String clazz) {
            _connectClass = clazz;
        }

        public void setDisconnectScript(String script) {
            _disconnectScript = script;
        }

        public void setHostNameOrIpAddr(String host) {
            _host = host;
        }

        public void setHostTelnetPortNumber(Integer port) {
            _port = port;
        }

        public void setPasswords(GuardedString[] passwords) {
            _passwords = passwords;
        }

        public void setPoolNames(String[] poolNames) {
            _poolNames = poolNames;
        }

        public void setUseSsl(Boolean useSsl) {
            _useSsl = useSsl;
        }

        public void setUserNames(String[] userNames) {
            _userNames = userNames;
        }
        
        public Integer getEvictionInterval() {
            return _evictionInterval;
        }
        
        public void setEvictionInterval(Integer interval) {
            _evictionInterval = interval;
        }
        
        public void validate() {
            
        }
    }
    
    public class OurConnectorMessages implements ConnectorMessages {
        private Map<Locale, Map<String, String>> _catalogs = new HashMap<Locale, Map<String, String>>();

        public String format(String key, String defaultValue, Object... args) {
        	Locale locale = CurrentLocale.isSet()?CurrentLocale.get():Locale.getDefault();
        	Map<String,String> catalog = _catalogs.get(locale);
            String message = catalog.get(key);
            MessageFormat formatter = new MessageFormat(message,locale);
            return formatter.format(args, new StringBuffer(), null).toString();
        }

        public void setCatalogs(Map<Locale,Map<String,String>> catalogs) {
            _catalogs = catalogs;
        }
    }
}
