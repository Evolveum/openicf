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
package org.identityconnectors.racf;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.rw3270.PoolableConnectionConfiguration;

public class RacfConfiguration extends AbstractConfiguration implements PoolableConnectionConfiguration {
    private String         _userName;
    private GuardedString  _password;
    private String         _suffix;

    private Boolean        _isUseSsl;
    private String         _hostNameOrIpAddr;
    private Integer        _hostLdapPortNumber;
    private Integer        _hostTelnetPortNumber;

    private ResourceBundle _bundle = null; 
    private Locale         _lastLocale = null; 
    
    private String[]       _segmentNames;
    private String[]       _segmentParsers;
    private String[]       _userNames;
    private GuardedString[] _passwords;
    private String[]       _poolNames;
    private Integer        _evictionInterval;
    private String         _connectScript;
    private String         _disconnectScript;
    private String         _connectionClassName;
    
    private String         _affinityList;
    private String         _segmentParserList;
    
    
    private static final String CATALOG = "org.identityconnectors.racf.RacfMessages";

    private ResourceBundle getBundle() {
        if (_bundle==null || getLocale()!=_lastLocale) {
            _lastLocale = getLocale();
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
        if (_userName==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAME_NULL));
        if (_password==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORD_NULL));
        if (_userNames==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.USERNAMES_NULL));
        if (_passwords==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORDS_NULL));
        if (_poolNames==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.POOLNAMES_NULL));
        if (_evictionInterval==null)
            throw new IllegalArgumentException(getMessage(RacfMessages.INTERVAL_NULL));
        if (_userNames.length!=_passwords.length || _userNames.length!=_poolNames.length)
            throw new IllegalArgumentException(getMessage(RacfMessages.PASSWORDS_LENGTH));
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
    public String getUserName() {
        return _userName;
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
    public void setUserName(String userName) {
        _userName = userName;
    }

    /**
     * Get the password for the LDAP connection
     * @return LDAP password
     */
    @ConfigurationProperty(order=5, displayMessageKey="Password", helpMessageKey="PasswordHelp", confidential=true)
    public GuardedString getPassword() {
        return _password;
    }

    /**
     * Set the password for the LDAP connection
     * @param password -- LDAP password
     */
    public void setPassword(GuardedString password) {
        _password = password;
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

    @ConfigurationProperty(order=10)
    public String[] getPoolNames() {
        return arrayCopy(_poolNames);
    }

    public void setPoolNames(String[] poolNames) {
        _poolNames = arrayCopy(poolNames);
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
    @ConfigurationProperty(order=15)
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
    
    /**
     * {@inheritDoc}
     */
    public Integer getEvictionInterval() {
        return _evictionInterval;
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionInterval(Integer evictionInterval) {
        _evictionInterval = evictionInterval;
    }

    public String getSegmentParserList() {
        return _segmentParserList;
    }

    public void setSegmentParserList(String parserList) {
        _segmentParserList = parserList;
    }

    public String getAffinityList() {
        return _affinityList;
    }

    public void setAffinityList(String affinityList) {
        _affinityList = affinityList;
    }
    
    private <T> T[] arrayCopy(T[] array) {
        if (array==null)
            return null;
        T [] result = (T[])Array.newInstance(array.getClass().getComponentType(), array.length);
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }
}