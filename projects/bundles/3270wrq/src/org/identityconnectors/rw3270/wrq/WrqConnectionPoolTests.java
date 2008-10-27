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
package org.identityconnectors.rw3270.wrq;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.test.TestHelpers;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.rw3270.ConnectionPool;
import org.identityconnectors.rw3270.PoolableConnectionConfiguration;
import org.identityconnectors.rw3270.RW3270Connection;
import org.identityconnectors.rw3270.PoolableConnectionFactory.ConnectionInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class WrqConnectionPoolTests {

    // Connector Configuration information
    //
    private static String           HOST_NAME;
    private static String           SYSTEM_PASSWORD;
    private static String           SYSTEM_USER;

    private static final int        HOST_TELNET_PORT    = 23;
    private static final Boolean    USE_SSL             = Boolean.FALSE;
    private static final int        SHORT_WAIT          = 5000;
    private static final String  READY               = "\\sREADY\\s{74}";
    private static final String  CONTINUE            = "\\s\\*\\*\\*\\s{76}";


    @BeforeClass
    public static void before() {
        HOST_NAME         = TestHelpers.getProperty("HOST_NAME", null);
        SYSTEM_PASSWORD   = TestHelpers.getProperty("SYSTEM_PASSWORD", null);
        SYSTEM_USER       = TestHelpers.getProperty("SYSTEM_USER", null);
        System.out.println("HOST_NAME="+HOST_NAME);
        System.out.println("SYSTEM_USER="+SYSTEM_USER);
        Assert.assertNotNull("HOST_NAME must be specified", HOST_NAME);
        Assert.assertNotNull("SYSTEM_PASSWORD must be specified", SYSTEM_PASSWORD);
    }

    @Test
    public void testTelnetConnectionViaPool() {
        OurConfiguration configuration = createConfiguration();
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("TODO");
            RW3270Connection connection = info.getConnection();
            try {
                // Now, display a user
                //
                String command = "LISTUSER IDM03";
                String line = executeCommand(connection, command);
                Assert.assertTrue(line.contains("USER=IDM03"));
                System.out.println(line);
                pool.returnObject("TODO", info);
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    private static MapTransform fillInPatternNodes(String parserString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader((parserString))));
        NodeList elements = document.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
            if (elements.item(i) instanceof Element) {
                return new MapTransform((Element) elements.item(i));
            }
        return null;
    }

    /*
     *  <opt><apMatch><t offset='-1'> UID= </t></apMatch><AttrParse><int name='OMVS.UID' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> HOME= </t></apMatch><AttrParse><str name='OMVS.HOME' len='73' trim='true'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> PROGRAM= </t></apMatch><AttrParse><str name='OMVS.PROGRAM' len='70' trim='true'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> CPUTIMEMAX= </t></apMatch><AttrParse><int name='OMVS.CPUTIMEMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> ASSIZEMAX= </t></apMatch><AttrParse><int name='OMVS.ASSIZEMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> FILEPROCMAX= </t></apMatch><AttrParse><int name='OMVS.FILEPROCMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> PROCUSERMAX= </t></apMatch><AttrParse><int name='OMVS.PROCUSERMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> THREADSMAX= </t></apMatch><AttrParse><int name='OMVS.THREADSMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> MMAPAREAMAX= </t></apMatch><AttrParse><int name='OMVS.MMAPAREAMAX' noval='NONE' /></AttrParse></opt>
     */
    @Test
    public void testOmvsParser() {
        String omvsParser =
            "<MapTransform>" +
            "  <PatternNode key='OMVS.HOME' pattern='HOME= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.PROGRAM' pattern='PROGRAM= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.CPUTIMEMAX' pattern='CPUTIMEMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.ASSIZEMAX' pattern='ASSIZEMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.FILEPROCMAX' pattern='FILEPROCMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.PROCUSERMAX' pattern='PROCUSERMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.THREADSMAX' pattern='THREADSMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.MMAPAREAMAX' pattern='MMAPAREAMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "</MapTransform>";
        
        OurConfiguration configuration = createConfiguration();
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("TODO");
            RW3270Connection connection = info.getConnection();
            try {
                // Now, display a user's OMVS info
                //
                String command = "LISTUSER "+SYSTEM_USER+" NORACF OMVS";
                String line = executeCommand(connection, command);
                if (line.contains("NO OMVS INFO")) {
                    // Oh, well
                } else {
                    MapTransform transform = fillInPatternNodes(omvsParser);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                    Assert.assertNotNull(attributes.get("TSO.MAXSIZE"));
                    Assert.assertNotNull(attributes.get("TSO.USERDATA"));
                    Assert.assertNotNull(attributes.get("TSO.JOBCLASS"));
                }
                pool.returnObject("TODO", info);
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testCicsParser() {
        OurConfiguration configuration = createConfiguration();

        String cicsParser =
            "<MapTransform>" +
            "  <PatternNode key='CICS.OPCLASS' pattern='OPCLASS= (.*?\\s*\\n(?:\\s{10}[^\\n]*\\n)*)' optional='true' reset='false'>" +
            "     <SplitTransform splitPattern='\\s+'/>" +
            "  </PatternNode>" +
            "  <PatternNode key='CICS.OPIDENT' pattern='OPIDENT= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='CICS.OPPRTY'  pattern='OPPRTY= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='CICS.TIMEOUT' pattern='TIMEOUT= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='CICS.XRFSOFF' pattern='XRFSOFF= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "</MapTransform>";
        
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("TODO");
            RW3270Connection connection = info.getConnection();
            try {
                // Now, display a user's CICS info
                //
                String command = "LISTUSER CICSUSER NORACF CICS";
                String line = executeCommand(connection, command);
                MapTransform transform = fillInPatternNodes(cicsParser);
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                Assert.assertNotNull(attributes.get("CICS.XRFSOFF"));
                Assert.assertTrue(attributes.get("CICS.OPCLASS") instanceof List);
                pool.returnObject("TODO", info);
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    private String executeCommand(RW3270Connection connection, String command) {
        connection.send("[clear]"+command+"[enter]");
        connection.waitFor(CONTINUE, READY, SHORT_WAIT);
        String line = connection.getStandardOutput();
        line = line.substring(0, line.lastIndexOf(" READY"));
        // break into lines
        //
        int index = line.indexOf(command);
        System.out.println("index="+index);
        if (index>-1)
            line = line.substring(index+80);
        line = line.replaceAll("(.{80})", "$1\n");
        return line;
    }
    
    @Test
    public void testTsoParser() {
        OurConfiguration configuration = createConfiguration();

        String tsoParser =
            "<MapTransform>" +
            "  <PatternNode key='TSO.ACCTNUM' pattern='ACCTNUM= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.HOLDCLASS' pattern='HOLDCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.JOBCLASS' pattern='JOBCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.MSGCLASS' pattern='MSGCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.PROC' pattern='PROC= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.SIZE' pattern='SIZE= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.MAXSIZE' pattern='MAXSIZE= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.SYSOUTCLASS' pattern='SYSOUTCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.PROC' pattern='PROC= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.UNIT' pattern='UNIT= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.USERDATA' pattern='USERDATA= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.COMMAND' pattern='COMMAND= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "</MapTransform>";
        
        try {
            ConnectionPool pool = new ConnectionPool(configuration);
            ConnectionInfo info = (ConnectionInfo)pool.borrowObject("TODO");
            RW3270Connection connection = info.getConnection();
            try {
                // Now, display a user's TSO info
                //
                connection.resetStandardOutput();
                String command = "LISTUSER "+SYSTEM_USER+" NORACF TSO";
                String line = executeCommand(connection, command);
                System.out.println(line);
                MapTransform transform = fillInPatternNodes(tsoParser);
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                Assert.assertNotNull(attributes.get("TSO.MAXSIZE"));
                Assert.assertNotNull(attributes.get("TSO.USERDATA"));
                Assert.assertNotNull(attributes.get("TSO.JOBCLASS"));
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
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
        config.setPoolNames(new String[] { "TODO" });
        config.setEvictionInterval(60000);
        config.setConnectionClassName(WrqConnection.class.getName());

        OurConnectorMessages messages = new OurConnectorMessages();
        Map<Locale, Map<String, String>> catalogs = new HashMap<Locale, Map<String,String>>();
        Map<String, String> foo = new HashMap<String, String>();
        for (String bundleName : new String[] { "org.identityconnectors.rw3270.Messages" }) {
	        ResourceBundle messagesBundle = ResourceBundle.getBundle(bundleName);
	        Enumeration<String> enumeration = messagesBundle.getKeys();
	        while (enumeration.hasMoreElements()) {
	            String key = enumeration.nextElement();
	            foo.put(key, messagesBundle.getString(key));
	        }
        }

        catalogs.put(Locale.getDefault(), foo);
        messages.setCatalogs(catalogs);
        config.setConnectorMessages(messages);        return config;
    }
    
    private String getLoginScript() {
        String script =
            "connection.connect();\n" +
            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
            "connection.send(\"TSO[enter]\");\n" +
            "connection.waitFor(\"ENTER USERID -\", SHORT_WAIT);\n" +
            "connection.send(USERNAME+\"[enter]\");\n" +
            "connection.waitFor(\"Password  ===>\", SHORT_WAIT);\n" +
            "connection.send(PASSWORD);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"\\\\*\\\\*\\\\*\", SHORT_WAIT);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"Option ===>\", SHORT_WAIT);\n" +
            "connection.send(\"[pf3]\");\n" +
            "connection.waitFor(\"READY\\\\s{74}\", SHORT_WAIT);";
        return script;
    }

    private String getLogoffScript() {
        String script =
            "connection.send(\"LOGOFF[enter]\");\n" +
            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
            "connection.dispose();\n";
        return script;
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
