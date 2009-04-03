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

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
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

    public void validate() {
        if (_suffix==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.SUFFIX_NULL));
        if (_hostNameOrIpAddr==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.HOST_NULL));
        if (_hostLdapPortNumber==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.PORT_NULL));
        if (_ldapUserName==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAME_NULL));
        if (_ldapPassword==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORD_NULL));
        if (_userName==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAMES_NULL));
        if (_password==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORDS_NULL));
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
