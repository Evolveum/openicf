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
package org.identityconnectors.racf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.rw3270.RW3270Configuration;

public class RacfConfiguration extends AbstractConfiguration {
    
    //TODO _ldapUserName, _ldapPassword, _suffix, _isUseSsl, _hostLdapPortNumber seems to be LDAP specific
    private String         _ldapUserName;
    private GuardedString  _ldapPassword;
    private String         _suffix;

    private Boolean        _isUseSsl;
    private String         _hostNameOrIpAddr;
    private Integer        _hostLdapPortNumber;
    private Integer        _hostTelnetPortNumber;
    private Integer        _commandTimeout;
    private Integer        _reaperMaximumIdle;
    private String[]       _userQueries;

    private String[]       _userObjectClasses;
    private String[]       _groupObjectClasses;

    private String[]       _segmentNames;
    private String[]       _segmentParsers;
    private String         _parserFactory;

    private String[]       _userNames;
    private GuardedString[]_passwords;

    private Script         _connectScript;
    private Script         _disconnectScript;
    private String         _connectionClassName;
    private String[]       _connectionProperties;

    private Boolean        _asResetToday;
    private Boolean        _asFilterUseOrSearch;
    private Boolean        _asRemoveOCFromFilter;
    private String         _asBlockSize;
    private String         _asDecryptorClass;
    private String[]       _asCertificate;
    private String[]       _asPrivateKey;
    private String[]       _asFilterChangesBy;

    public RW3270Configuration getRW3270Configuration(int index) {
        return new RW3270ConfigurationProxy(this, index);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.MessagesInterfac#getMessage(java.lang.String)
     */
    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.racf.MessagesInterfac#getMessage(java.lang.String, java.lang.Object)
     */
    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }

    private static final String GROUP_RACF_PARSER   = "org/identityconnectors/racf/GroupRacfSegmentParser.xml";
    private static final String RACF_PARSER         = "org/identityconnectors/racf/RacfSegmentParser.xml";
    private static final String CICS_PARSER         = "org/identityconnectors/racf/CicsSegmentParser.xml";
    private static final String OMVS_PARSER         = "org/identityconnectors/racf/OmvsSegmentParser.xml";
    private static final String TSO_PARSER          = "org/identityconnectors/racf/TsoSegmentParser.xml";
    private static final String NETVIEW_PARSER      = "org/identityconnectors/racf/NetviewSegmentParser.xml";
    private static final String CATALOG_PARSER      = "org/identityconnectors/racf/CatalogParser.xml";

    public RacfConfiguration() {
        setUserObjectClasses(new String[] { 
                "racfUser",
                "racfCicsSegment",
                "racfDCESegment",
                "SAFDfpSegment",
                "racfKerberosInfo",
                "racfLanguageSegment",
                "racfLNotesSegment",
                "racfNDSSegment",
                "racfNetviewSegment",
                "racfUserOmvsSegment",
                "racfOperparmSegment",
                "racfUserOvmSegment",
                "racfProxySegment",
                "SAFTsoSegment",
                "racfWorkAttrSegment"});
        setGroupObjectClasses(new String[] { 
                "racfGroup",
                "racfGroupOvmSegment",
                "racfGroupOmvsSegment",
                "SAFDfpSegment"});
        //setConnectScript(getLoginScript());
        //setDisconnectScript(getLogoffScript());
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
        try {
            StringBuffer parser = new StringBuffer();
            String line = null;
            while ((line=is.readLine())!=null) {
                parser.append(line+"\n");
            }
            return parser.toString();
        } finally {
            is.close();
        }
    }

//    private Script getLoginScript() {
//        String script =
//            "connection.connect();\n" +
//            "connection.waitFor(\"PRESS THE ENTER KEY\", SHORT_WAIT);\n" +
//            "connection.send(\"TSO[enter]\");\n" +
//            "connection.waitFor(\"ENTER USERID -\", SHORT_WAIT);\n" +
//            "connection.send(USERNAME+\"[enter]\");\n" +
//            "connection.waitFor(\"Password  ===>\", SHORT_WAIT);\n" +
//            "connection.send(PASSWORD);\n" +
//            "connection.send(\"[enter]\");\n" +
//            "connection.waitFor(\"\\\\*\\\\*\\\\*\", SHORT_WAIT);\n" +
//            "connection.send(\"[enter]\");\n" +
//            "connection.waitFor(\"Option ===>\", SHORT_WAIT);\n" +
//            "connection.send(\"[pf3]\");\n" +
//            "connection.waitFor(\" READY\\\\s{74}\", SHORT_WAIT);";
//        ScriptBuilder builder = new ScriptBuilder();
//        builder.setScriptLanguage("GROOVY");
//        builder.setScriptText(script);
//        return builder.build();
//    }
//
//    private Script getLogoffScript() {
//        String script = "connection.send(\"LOGOFF[enter]\");\n";
//        //            "connection.send(\"LOGOFF[enter]\");\n" +
//        //            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
//        //            "connection.dispose();\n";
//        ScriptBuilder builder = new ScriptBuilder();
//        builder.setScriptLanguage("GROOVY");
//        builder.setScriptText(script);
//        return builder.build();
//    }
    
    boolean isNoLdap() {
        return (StringUtil.isBlank(_suffix) || _hostLdapPortNumber==null || isBlank(_ldapPassword) || StringUtil.isBlank(_ldapUserName));
    }

    boolean isNoCommandLine() {
        return isBlank(_userNames) || isBlank(_passwords);
    }
    
    public void validate() {
        // It's OK for all LDAP or all CommandLine connection info to be missing
        // but not both
        //
        boolean noLdap = isNoLdap();
        boolean noCommandLine = isNoCommandLine();

        if (noLdap && noCommandLine)
            throw new IllegalArgumentException(getMessage(RacfMessages.BAD_CONNECTION_INFO));

        if (StringUtil.isBlank(_hostNameOrIpAddr))
            throw new IllegalArgumentException(getMessage(RacfMessages.HOST_NULL));

        if (!noLdap && StringUtil.isBlank(_suffix))
            throw new IllegalArgumentException(getMessage(RacfMessages.SUFFIX_NULL));
        if (!noLdap && _hostLdapPortNumber==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.LDAP_PORT_NULL));
        if (_hostLdapPortNumber!=null && (_hostLdapPortNumber<1 || _hostLdapPortNumber>65536))
            throw new IllegalArgumentException(getMessage(RacfMessages.ILLEGAL_LDAP_PORT, _hostLdapPortNumber));
        if (!noLdap && StringUtil.isBlank(_ldapUserName))
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAME_NULL));
        if (!noLdap && isBlank(_ldapPassword))
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORD_NULL));
        if (!noLdap && _isUseSsl==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.SSL_NULL));
        
        if (!noCommandLine && _hostTelnetPortNumber==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.TELNET_PORT_NULL));
        if (!noCommandLine && _commandTimeout==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.COMMAND_TIMEOUT_NULL));
        if (!noCommandLine && _reaperMaximumIdle==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.REAPER_MAX_IDLE_NULL));
        if (_hostTelnetPortNumber!=null && (_hostTelnetPortNumber<1 || _hostTelnetPortNumber>65536))
            throw new IllegalArgumentException(getMessage(RacfMessages.ILLEGAL_TELNET_PORT, _hostTelnetPortNumber));
        if (!noCommandLine && isBlank(_userNames))
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAMES_NULL));
        if (!noCommandLine && isBlank(_passwords))
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORDS_NULL));
        if (!noCommandLine && StringUtil.isBlank(_connectionClassName))
            throw new IllegalArgumentException(getMessage(RacfMessages.CONNECTION_CLASS_NULL));
        if (!noCommandLine && isBlank(_disconnectScript))
            throw new IllegalArgumentException(getMessage(RacfMessages.DISCONNECT_SCRIPT_NULL));
        if (!noCommandLine && isBlank(_connectScript))
            throw new IllegalArgumentException(getMessage(RacfMessages.CONNECT_SCRIPT_NULL));

    }

    boolean isBlank(Script script) {
        if (script==null)
            return true;
        return StringUtil.isBlank(script.getScriptText());
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

    boolean isBlank(GuardedString[] strings) {
        if (strings.length==0)
            return true;
        boolean isBlank = false;
        for (GuardedString string : strings) {
            isBlank |= isBlank(string);
        }
        return isBlank;
    }

    boolean isBlank(String[] strings) {
        if (strings==null || strings.length==0)
            return true;
        boolean isBlank = false;
        for (String string : strings) {
            if (string==null)
                return true;
            isBlank |= string.length()==0;
        }
        return isBlank;
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
     * Get the user object class names
     * @return an array of object class names
     */
    @ConfigurationProperty(order=5, displayMessageKey="UserObjectClasses", helpMessageKey="UserObjectClassesHelp")
    public String[] getUserObjectClasses() {
        return arrayCopy(_userObjectClasses);
    }

    /**
     * Set the supported user object classes
     * @param userObjectClasses -- an array of object class names
     */
    public void setUserObjectClasses(String[] userObjectClasses) {
        _userObjectClasses = arrayCopy(userObjectClasses);
    }

    /**
     * Get the user object class names
     * @return an array of object class names
     */
    @ConfigurationProperty(order=5, displayMessageKey="GroupObjectClasses", helpMessageKey="GroupObjectClassesHelp")
    public String[] getGroupObjectClasses() {
        return arrayCopy(_groupObjectClasses);
    }

    /**
     * Set the supported group object classes
     * @param groupObjectClasses -- an array of object class names
     */
    public void setGroupObjectClasses(String[] groupObjectClasses) {
        _groupObjectClasses = arrayCopy(groupObjectClasses);
    }

    /**
     * Get the user queries
     * @return an array of query strings that can fetch all users
     */
    @ConfigurationProperty(order=5, displayMessageKey="UserQueries", helpMessageKey="UserQueriesHelp")
    public String[] getUserQueries() {
        return arrayCopy(_userQueries);
    }

    /**
     * Set the query strings that can fetch all users
     * @param userObjectClasses -- an array of query strings that can fetch all users
     */
    public void setUserQueries(String[] userQueries) {
        _userQueries = arrayCopy(userQueries);
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

//    private String escape(String string) {
//        return string.replaceAll("#", "\\#");
//    }

    @ConfigurationProperty(order=8)
    public String[] getUserNames() {
        return arrayCopy(_userNames);
    }

    public void setUserNames(String[] names) {
        _userNames = arrayCopy(names);
    }

    @ConfigurationProperty(order=9, confidential=true)
    public GuardedString[] getPasswords() {
        return arrayCopy(_passwords);
    }

    public void setPasswords(GuardedString[] passwords) {
        _passwords = arrayCopy(passwords);
    }

    @ConfigurationProperty(order=19, confidential=true)
    public String[] getConnectionProperties() {
        return arrayCopy(_connectionProperties);
    }

    public void setConnectionProperties(String[] properties) {
        _connectionProperties = arrayCopy(properties);
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

    @ConfigurationProperty(order=14)
    public String getParserFactory() {
        return _parserFactory;
    }

    public void setParserFactory(String parserFactory) {
        _parserFactory = parserFactory;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=16)
    public Script getConnectScript() {
        return _connectScript;
    }

    /**
     * {@inheritDoc}
     */
    public void setConnectScript(Script script) {
        _connectScript = script;
    }

    /**
     * {@inheritDoc}
     */
    @ConfigurationProperty(order=16)
    public Script getDisconnectScript() {
        return _disconnectScript;
    }

    /**
     * {@inheritDoc}
     */
    public void setDisconnectScript(Script script) {
        _disconnectScript = script;
    }

    /**
     * {@inheritDoc}
     */

    @ConfigurationProperty(order=14)
    public Integer getCommandTimeout() {
        return _commandTimeout;
    }
    /**
     * {@inheritDoc}
     */
    public void setCommandTimeout(Integer commandTimeout) {
        _commandTimeout = commandTimeout;
    }

    /**
     * {@inheritDoc}
     */

    @ConfigurationProperty(order=14)
    public Integer getReaperMaximumIdleTime() {
        return _reaperMaximumIdle;
    }
    /**
     * {@inheritDoc}
     */
    public void setReaperMaximumIdleTime(Integer reaperMaximumIdle) {
        _reaperMaximumIdle = reaperMaximumIdle;
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

    @ConfigurationProperty
    public String[] getActiveSyncCertificate() {
        return arrayCopy(_asCertificate);
    }

    public void setActiveSyncCertificate(String[] certificate) {
        _asCertificate = arrayCopy(certificate);
    }

    @ConfigurationProperty
    public String[] getActiveSyncPrivateKey() {
        return arrayCopy(_asPrivateKey);
    }

    public void setActiveSyncPrivateKey(String[] privateKey) {
        _asPrivateKey = arrayCopy(privateKey);
    }

    @ConfigurationProperty
    public String[] getActiveSyncFilterChangesBy() {
        return arrayCopy(_asFilterChangesBy);
    }

    public void setActiveSyncFilterChangesBy(String[] filterChangesBy) {
        _asFilterChangesBy = arrayCopy(filterChangesBy);
    }

    @ConfigurationProperty
    public String getActiveSyncBlocksize() {
        return _asBlockSize;
    }

    public void setActiveSyncBlocksize(String blockSize) {
        _asBlockSize = blockSize;
    }

    @ConfigurationProperty
    public Boolean getActiveSyncResetToToday() {
        return _asResetToday;
    }

    public void setActiveSyncResetToToday(Boolean asResetToday) {
        _asResetToday = asResetToday;
    }

    @ConfigurationProperty
    public Boolean getActiveSyncFilterUseOrSearch() {
        return _asFilterUseOrSearch;
    }

    public void setActiveSyncFilterUseOrSearch(Boolean useOrSearch) {
        _asFilterUseOrSearch = useOrSearch;
    }

    @ConfigurationProperty
    public Boolean getActiveSyncRemoveOCFromFilter() {
        return _asRemoveOCFromFilter;
    }

    public void setActiveSyncRemoveOCFromFilter(Boolean removeOCFromFilter) {
        _asRemoveOCFromFilter = removeOCFromFilter;
    }

    @ConfigurationProperty
    public String getActiveSyncPasswordDecryptorClass() {
        return _asDecryptorClass;
    }

    public void setActiveSyncPasswordDecryptorClass(String decryptorClass) {
        _asDecryptorClass = decryptorClass;
    }

    @SuppressWarnings("unchecked")
    private <T> T[] arrayCopy(T[] array) {
        if (array==null)
            return null;
        T [] result = (T[])Array.newInstance(array.getClass().getComponentType(), array.length);
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }
}
