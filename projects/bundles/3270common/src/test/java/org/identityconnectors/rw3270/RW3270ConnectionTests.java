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
import org.junit.BeforeClass;
import org.junit.Test;


public class RW3270ConnectionTests {

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
            final DummyConnection connection = new DummyConnection(configuration);
            connection.setDisplay("Nothing");
            
            try {
                connection.waitFor("USER=", 100);
                Assert.fail("exception expected");
            } catch (RuntimeException e) {
            }
            try {
                connection.waitFor("USER=", "C", 100);
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
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testTelnetConnectionViaPool() {
        OurConfiguration configuration = createConfiguration();
        try {
            DummyConnection connection = new DummyConnection(configuration);
            connection.send("Login[enter]");
            connection.send("[clear]");
            connection.send("[cursor (256)]");
            try {
                connection.send("[bogus]");
                Assert.fail("no error thrown");
            } catch (IllegalArgumentException e) {
            }
            sendRelease((DummyConnection)connection, "USER=IDM03");
            connection.waitFor("USER=IDM03");
            sendRelease((DummyConnection)connection, "USER=", "IDM03");
            connection.waitFor("USER=", "IDM03");
            sendRelease((DummyConnection)connection, "USER=IDM03");
            connection.waitFor("USER=IDM03", 3000);
            sendRelease((DummyConnection)connection, "USER=", "IDM03");
            connection.waitFor("USER=", "IDM03", 3000);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testPaKeys() {
        OurConfiguration configuration = createConfiguration();
        try {
            DummyConnection connection = new DummyConnection(configuration);
            for (int i=1; i<4; i++)
                connection.send("[pa"+i+"]");
            try {
                connection.send("[pa0]");
                Assert.fail("bad PA Key not caught");
            } catch (RuntimeException e) {
            }
            try {
                connection.send("[pa5]");
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
            DummyConnection connection = new DummyConnection(configuration);
            for (int i=1; i<25; i++)
                connection.send("[pf"+i+"]");
            try {
                connection.send("[pf0]");
                Assert.fail("bad PFA Key not caught");
            } catch (RuntimeException e) {
            }
            try {
                connection.send("[pf25]");
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
        config.setConnectionProperties(null);
        config.setConnectScript(getLoginScript());
        config.setDisconnectScript(getLogoffScript());
        config.setUserName(SYSTEM_USER);
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setScriptingLanguage("GROOVY");
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
        
        public DummyConnection(RW3270Configuration config) throws NamingException {
            super(config);
        }
        
        public void releaseSemaphore() {
            _semaphore.release();
        }

        public void clearAndUnlock() throws InterruptedException {
        }
        
        public void setDisplay(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        public void sendEnter() {
            sentData.append("[enter]");
        }

        public void sendKeys(String keys) {
            sentData.append(keys);
        }

        public void sendPAKeys(int pa) {
            sentData.append("[pa"+pa+"]");
        }

        public void sendPFKeys(int pf) {
            sentData.append("[pf"+pf+"]");
        }

        public void setCursorPos(short pos) {
        }

        public void waitForUnlock() throws InterruptedException {
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
            _ioPair.reset();
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
    
    public static class OurConfiguration extends AbstractConfiguration implements RW3270Configuration {
        private String _connectScript;
        private String _disconnectScript;
        private String _host;
        private Integer _port;
        private GuardedString _password;
        private String _language;
        private String _userName;
        private Integer _evictionInterval;
        private String _connectClass;
        private String[] _connectionProperties;

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

        public GuardedString getPassword() {
            return _password;
        }

        public String getScriptingLanguage() {
            return _language;
        }

        public String[] getConnectionProperties() {
            return _connectionProperties;
        }

        public String getUserName() {
            return _userName;
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

        public void setPassword(GuardedString password) {
            _password = password;
        }

        public void setScriptingLanguage(String language) {
            _language = language;
        }

        public void setConnectionProperties(String[] connectionProperties) {
            _connectionProperties = connectionProperties;
        }

        public void setUserName(String userName) {
            _userName = userName;
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
