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
package org.identityconnectors.racf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.rw3270.RW3270Configuration;

public class RacfConfiguration extends AbstractConfiguration implements RW3270Configuration {
    private String         _ldapUserName;
    private GuardedString  _ldapPassword;
    private String         _suffix;

    private Boolean        _isUseSsl;
    private String         _hostNameOrIpAddr;
    private Integer        _hostLdapPortNumber;
    private Integer        _hostTelnetPortNumber;

    private ResourceBundle _bundle = null; 
    private Locale         _lastLocale = null; 
    
    private String[]       _segmentNames;
    private String[]       _segmentParsers;
    private String         _userName;
    private GuardedString  _password;

    private String         _scriptingLanguage;
    private String         _connectScript;
    private String         _disconnectScript;
    private String         _connectionClassName;
    
    private static final String CATALOG = "org.identityconnectors.racf.RacfMessages";

    private ResourceBundle getBundle() {
        if (_bundle==null || CurrentLocale.get()!=_lastLocale) {
            _lastLocale = CurrentLocale.get();
            if (_lastLocale==null)
                _lastLocale = Locale.getDefault();
            _bundle = ResourceBundle.getBundle(CATALOG, _lastLocale); 
        }
        return _bundle;
    }

    String getMessage(String key) {
        return getBundle().getString(key);
    }

    String getMessage(String key, Object... objects) {
        return MessageFormat.format(getBundle().getString(key), objects);
    }
    
    private static final String GROUP_RACF_PARSER   = "org/identityconnectors/racf/GroupRacfSegmentParser.xml";
    private static final String RACF_PARSER         = "org/identityconnectors/racf/RacfSegmentParser.xml";
    private static final String CICS_PARSER         = "org/identityconnectors/racf/CicsSegmentParser.xml";
    private static final String OMVS_PARSER         = "org/identityconnectors/racf/OmvsSegmentParser.xml";
    private static final String TSO_PARSER          = "org/identityconnectors/racf/TsoSegmentParser.xml";
    private static final String NETVIEW_PARSER      = "org/identityconnectors/racf/NetviewSegmentParser.xml";
    private static final String CATALOG_PARSER      = "org/identityconnectors/racf/CatalogParser.xml";

    public RacfConfiguration() {
        setConnectScript(getLoginScript());
        setDisconnectScript(getLogoffScript());
        setSegmentNames(new String[] { 
                "ACCOUNT.RACF",                     "ACCOUNT.TSO",                  "ACCOUNT.NETVIEW",
                "ACCOUNT.CICS",                     "ACCOUNT.OMVS",                 "ACCOUNT.CATALOG", 
                "ACCOUNT.OMVS",                     "GROUP.RACF" });
        try {
        setSegmentParsers(new String[] { 
                loadParserFromFile(RACF_PARSER),    loadParserFromFile(TSO_PARSER), loadParserFromFile(NETVIEW_PARSER), 
                loadParserFromFile(CICS_PARSER),    loadParserFromFile(OMVS_PARSER), loadParserFromFile(CATALOG_PARSER), 
                loadParserFromFile(OMVS_PARSER),    loadParserFromFile(GROUP_RACF_PARSER) });
        } catch (IOException ioe) {
            throw ConnectorException.wrap(ioe);
        }
    }
    
    private String loadParserFromFile(String fileName) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName)));
        StringBuffer tsoParser = new StringBuffer();
        String line = null;
        while ((line=is.readLine())!=null) {
            tsoParser.append(line);
        }
        return tsoParser.toString();
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
            "connection.waitFor(\" \\\\*\\\\*\\\\* \", SHORT_WAIT);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"Option ===>\", SHORT_WAIT);\n" +
            "connection.send(\"[pf3]\");\n" +
            "connection.waitFor(\"READY\\\\s{74}\", SHORT_WAIT);";
        return script;
    }

    private String getLogoffScript() {
        String script = "connection.send(\"LOGOFF[enter]\");\n";
//            "connection.send(\"LOGOFF[enter]\");\n" +
//            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
//            "connection.dispose();\n";
        return script;
    }

    public void validate() {
        // It's OK for all LDAP or all CommandLine connection info to be missing
        // but not both
        //
        boolean noLdap = (StringUtil.isBlank(_suffix) && _hostLdapPortNumber==null && isBlank(_ldapPassword) && StringUtil.isBlank(_ldapUserName));
        boolean noCommandLine = StringUtil.isBlank(_userName) && isBlank(_password);
        
        if (noLdap && noCommandLine)
            throw new IllegalArgumentException(getMessage(RacfMessages.SUFFIX_NULL)); //TODO
        
        if (StringUtil.isBlank(_hostNameOrIpAddr))
            throw new IllegalArgumentException(getMessage(RacfMessages.HOST_NULL));

        if (!noLdap && StringUtil.isBlank(_suffix))
            throw new IllegalArgumentException(getMessage(RacfMessages.SUFFIX_NULL));
        if (!noLdap && _hostLdapPortNumber==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.PORT_NULL));
        if (!noLdap && StringUtil.isBlank(_ldapUserName))
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAME_NULL));
        if (!noLdap && isBlank(_ldapPassword))
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORD_NULL));
        
        if (!noCommandLine && StringUtil.isBlank(_userName))
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAMES_NULL));
        if (!noCommandLine && isBlank(_password))
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORDS_NULL));
    }
    
    boolean isBlank(GuardedString string) {
        if (string==null)
            return true;
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        string.access(accessor);
        boolean isBlank = accessor.getArray().length==0;
        accessor.clear();
        return isBlank;
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
    /**
     * Return LDAP suffix, such as cn=foo
     * @return RACF suffix (such as sysplex)
     */
    @ConfigurationProperty(order=3, displayMessageKey="Suffix", helpMessageKey="SuffixHelp")
    public String getSuffix() {
        return _suffix;
    }

    /**
     * Set the LDAP Suffix.
     * @param suffix
     */
    public void setSuffix(String suffix) {
        _suffix = suffix;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=7, displayMessageKey="SSL", helpMessageKey="SSLHelp")
    public Boolean getUseSsl() {
        return _isUseSsl;
    }

    /**
     * {@inheritDoc}
     */
    public void setUseSsl(Boolean isUseSsl) {
        _isUseSsl = isUseSsl;
    }

    /**
     * Get the user name used for the LDAP connection.
     * @return LDAP user name
     */
    @ConfigurationProperty(order=4, displayMessageKey="UserName", helpMessageKey="UserNameHelp")
    public String getLdapUserName() {
        return _ldapUserName;
    }

    /**
     * Set the user name for the LDAP connection.
     * <p>
     * Must be of the form
     * <code>
     * racfid=<b>name</b>,profileType=User,<b>suffix</b>
     * </code>
     * 
     * @param userName -- user name
     */
    public void setLdapUserName(String userName) {
        _ldapUserName = userName;
    }

    /**
     * Get the password for the LDAP connection
     * @return LDAP password
     */
    @ConfigurationProperty(order=5, displayMessageKey="Password", helpMessageKey="PasswordHelp", confidential=true)
    public GuardedString getLdapPassword() {
        return _ldapPassword;
    }

    /**
     * Set the password for the LDAP connection
     * @param password -- LDAP password
     */
    public void setLdapPassword(GuardedString password) {
        _ldapPassword = password;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=1, displayMessageKey="HostName", helpMessageKey="HostNameHelp")
    public String getHostNameOrIpAddr() {
        return _hostNameOrIpAddr;
    }

    /**
     * {@inheritDoc}
     */
    public void setHostNameOrIpAddr(String hostNameOrIpAddr) {
        _hostNameOrIpAddr = hostNameOrIpAddr;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=6, displayMessageKey="HostTelnetPort", helpMessageKey="HostTelnetPortHelp")
    public Integer getHostTelnetPortNumber() {
        return _hostTelnetPortNumber;
    }

    /**
     * {@inheritDoc}
     */
    public void setHostTelnetPortNumber(Integer hostPortNumber) {
        _hostTelnetPortNumber = hostPortNumber;
    }

    /**
     * Get the port number for the LDAP connection
     * @return LDAP port number
     */
    @ConfigurationProperty(order=2, displayMessageKey="HostLdapPort", helpMessageKey="HostLdapPortHelp")
    public Integer getHostPortNumber() {
        return _hostLdapPortNumber;
    }

    /**
     * Set the port number for the LDAP connection
     * @param hostPortNumber -- LDAP port number
     */
    public void setHostPortNumber(Integer hostPortNumber) {
        _hostLdapPortNumber = hostPortNumber;
    }

    private String escape(String string) {
        return string.replaceAll("#", "\\#");
    }

    @ConfigurationProperty(order=8)
    public String getUserName() {
        return _userName;
    }

    public void setUserName(String name) {
        _userName = name;
    }

    @ConfigurationProperty(order=9, confidential=true)
    public GuardedString getPassword() {
        return _password;
    }

    public void setPassword(GuardedString password) {
        _password = password;
    }

    @ConfigurationProperty(order=11)
    public String[] getSegmentNames() {
        return arrayCopy(_segmentNames);
    }

    public void setSegmentNames(String[] names) {
        _segmentNames = arrayCopy(names);
    }

    @ConfigurationProperty(order=14)
    public String[] getSegmentParsers() {
        return arrayCopy(_segmentParsers);
    }

    public void setSegmentParsers(String[] segmentParsers) {
        _segmentParsers = arrayCopy(segmentParsers);
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=16)
    public String getConnectScript() {
        return _connectScript;
    }

    /**
     * {@inheritDoc}
     */
    public void setConnectScript(String script) {
        _connectScript = script;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=16)
    public String getDisconnectScript() {
        return _disconnectScript;
    }

    /**
     * {@inheritDoc}
     */
    public void setDisconnectScript(String script) {
        _disconnectScript = script;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=13)
    public String getConnectionClassName() {
        return _connectionClassName;
    }

    /**
     * {@inheritDoc}
     */
    public void setConnectionClassName(String className) {
        _connectionClassName = className;
    }

    private <T> T[] arrayCopy(T[] array) {
        if (array==null)
            return null;
        T [] result = (T[])Array.newInstance(array.getClass().getComponentType(), array.length);
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }

    public String getScriptingLanguage() {
        return _scriptingLanguage;
    }

    public void setScriptingLanguage(String language) {
        _scriptingLanguage = language;
    }
}
