/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openicf.connectors.rsaauthenticationmanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the RSAAuthenticationManager8 Connector.
 *
 * The RSA AM 8 Connector uses the "config.properties" file to store its connectivity 
 * properties, as defined in the "CommandClientAppContextOverrides.xml" bean definition file.
 * The RSAAuthenticationManager8Configuration object will check for the existence of this file, and if it not there,
 * it will require its configuration propeties to be populated.
 * 
 * @author Alex Babeanu (ababeanu@nulli.com)
 * www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1
 * @since 1.0
 */
public class RSAAuthenticationManager8Configuration extends AbstractConfiguration {

    // Constants
    public static final String RSA_DOMAIN = "SystemDomain";
    public static final String CONFIG_PROPERTIES_FILE = "config.properties";
    public static final String DATE_FORMAT = "yyyy/MM/dd";
    // Token Operations
    public static final Integer TOKEN_OP_OPTION_ASSIGN = 1;
    public static final String TOKEN_OP_OPTION_ASSIGN_NAME = "Assign";
    public static final Integer TOKEN_OP_OPTION_REVOKE = 2;
    public static final String TOKEN_OP_OPTION_REVOKE_NAME = "Revoke";
    public static final Integer TOKEN_OP_OPTION_DISABLE = 3;
    public static final String TOKEN_OP_OPTION_DISABLE_NAME = "Disable";
    public static final Integer TOKEN_OP_OPTION_ENABLE = 4;
    public static final String TOKEN_OP_OPTION_ENABLE_NAME = "Enable";
    public static final String TOKEN_OBJECTCLASS = "__TOKEN__";
    // Group operations
    public static final Integer GROUP_OP_OPTION_LINK = 1;
    public static final String GROUP_OP_OPTION_LINK_NAME = "Link";
    public static final Integer GROUP_OP_OPTION_UNLINK = 2;
    public static final String GROUP_OP_OPTION_UNLINK_NAME = "Unlink";
    // Search Options
    public static final String SEARCH_LIMIT_NAME = "SearchLimit";
    public static final Integer SEARCH_LIMIT_DEFAULT = 100000;
    // CUSTOM Attributes
    public static final String CUSTOM_ATTR_EMPLOYEE_NB = "Employee Number";
    public static final String CUSTOM_ATTR_TOKEN_SN_LIST = "TokensList";
    public static final String CUSTOM_ATTR_GROUP_NAME = "GroupName";
    // For defaultShell use: com.rsa.authmgr.common.AdminResource.AdminResource.DEFAULTSHELL
    public static final String CUSTOM_ATTR_SHELL_ALLOWED = "shellAllowed";
    public static final String CUSTOM_ATTR_GROUPS = "groups";
    public static final String CUSTOM_ATTR_ALLOWED_TO_CREATE_PIN = "allowedToCreatePin";
    public static final String CUSTOM_ATTR_REQUIRED_TO_CREATE_PIN = "requiredToCreatePin";
    public static final String CUSTOM_ATTR_TEMPUSER = "tempUser";
    public static final String CUSTOM_ATTR_TEMP_START_DATE = "tempStartDate";
    public static final String CUSTOM_ATTR_TEMP_START_HOUR = "tempStartHour";
    public static final String CUSTOM_ATTR_TEMP_END_DATE = "tempEndDate";
    public static final String CUSTOM_ATTR_TEMP_END_HOUR = "tempEndHour";
    public static final String CUSTOM_ATTR_SECRET_WORD = "Secret Word";
    // Token Custom Attribs
    // Token 1
    public static final String CUSTOM_ATTR_TOKEN1_SN = "token1SerialNumber";
    public static final String CUSTOM_ATTR_TOKEN1_PIN = "token1Pin";
    public static final String CUSTOM_ATTR_TOKEN1_RESYNC = "token1Resync";
    public static final String CUSTOM_ATTR_TOKEN1_FIRST_SEQ ="token1FirstSequence";
    public static final String CUSTOM_ATTR_TOKEN1_NEXT_SEQ = "token1NextSequence";
    public static final String CUSTOM_ATTR_TOKEN1_DISABLED = "token1Disabled";
    public static final String CUSTOM_ATTR_TOKEN1_LOST = "token1Lost";
    public static final String CUSTOM_ATTR_TOKEN1_NEW_PIN_MODE = "token1NewPinMode";
    public static final String CUSTOM_ATTR_TOKEN1_CLEAR_PIN = "token1ClearPin";
    public static final String CUSTOM_ATTR_TOKEN1_UNASSIGN = "token1Unassign";
    // Token 2
    public static final String CUSTOM_ATTR_TOKEN2_SN = "token2SerialNumber";
    public static final String CUSTOM_ATTR_TOKEN2_PIN = "token2Pin";
    public static final String CUSTOM_ATTR_TOKEN2_RESYNC = "token2Resync";
    public static final String CUSTOM_ATTR_TOKEN2_FIRST_SEQ ="token2FirstSequence";
    public static final String CUSTOM_ATTR_TOKEN2_NEXT_SEQ = "token2NextSequence";
    public static final String CUSTOM_ATTR_TOKEN2_DISABLED = "token2Disabled";
    public static final String CUSTOM_ATTR_TOKEN2_LOST = "token2Lost";
    public static final String CUSTOM_ATTR_TOKEN2_NEW_PIN_MODE = "token2NewPinMode";
    public static final String CUSTOM_ATTR_TOKEN2_CLEAR_PIN = "token2ClearPin";
    public static final String CUSTOM_ATTR_TOKEN2_UNASSIGN = "token2Unassign";
    // Token 3
    public static final String CUSTOM_ATTR_TOKEN3_SN = "token3SerialNumber";
    public static final String CUSTOM_ATTR_TOKEN3_PIN = "token3Pin";
    public static final String CUSTOM_ATTR_TOKEN3_RESYNC = "token3Resync";
    public static final String CUSTOM_ATTR_TOKEN3_FIRST_SEQ ="token3FirstSequence";
    public static final String CUSTOM_ATTR_TOKEN3_NEXT_SEQ = "token3NextSequence";
    public static final String CUSTOM_ATTR_TOKEN3_DISABLED = "token3Disabled";
    public static final String CUSTOM_ATTR_TOKEN3_LOST = "token3Lost";
    public static final String CUSTOM_ATTR_TOKEN3_NEW_PIN_MODE = "token3NewPinMode";
    public static final String CUSTOM_ATTR_TOKEN3_CLEAR_PIN = "token3ClearPin";
    public static final String CUSTOM_ATTR_TOKEN3_UNASSIGN = "token3Unassign";
    
    // Use for the AM extension only, uncomment if needed:
    /*
    public static final String ATTR_DEFAULT_SHELL = "DefaultSHell";
    public static final String ATTR_WIN_PWD = "WindowsPwd";
    public static final String ATTR_SHELL_ALLOWED = "ShellAllowed";
    */

    // Exposed configuration properties.
    /**
     * Setup logging for the {@link RSAAuthenticationManager8Connection}.
     */
    private static final Log logger = Log.getLog(RSAAuthenticationManager8Configuration.class);
    /**
     * JNDI factory class.
     * Default = "weblogic.jndi.WLInitialContextFactory" .
     */
    private final String InitialNamingFactory = "weblogic.jndi.WLInitialContextFactory";
    /**
     * Server URL. 
     * */
    private String NamingProviderUrl = null;  //e.g., "t3s://local1:7002"

    /** 
     * User ID for process-level Authentication.
     */ 
    private String CmdclientUser = null;

    /** 
     * Password for process-level Authentication
     */ 
    private GuardedString CmdClientUserPwd = null;

    /**
     * Password for Two-Way SSL client identity keystore
     */ 
    private GuardedString RsaSslClientIdStorePwd = null;

    /**
     * Password for Two-Way SSL client identity private key
     */ 
    private GuardedString RsaSslClientIdKeyPwd = null;

    /**
     * Provider URL for Two-Way SSL client Authentication
     */
    private String ImsSslClientProviderUrl = null;  // e.g., t3s://local1:7022

    /**
     * Identity keystore for Two-Way SSL client Authentication
     */ 
    private String ImsSslClientIdentityKeystoreFilename = null; // e.g., "client-identity.jks"

    /**
     * Identity keystore private key alias for Two-Way SSL client Authentication.
     * Default = "client-identity".
     */ 
    private String ImsSslClientIdentityKeyAlias = "client-identity";

    /**
     * Identity keystore trusted root CA certificate alias.
     * Default = "root-ca".
     */ 
    private String ImsSslClientRootCaAlias = "root-ca";

    /**
     * SOAPCommandTargetBasicAuth provider URL
     */ 
    private String ImsSoapClientProviderUrl = null; // e.g., "https://local1:7002/ims-ws/services/CommandServer"

    /**
     * HttpInvokerCommandTargetBasicAuth provider URL
     */ 
    private String ImsHttpinvokerClientProviderUrl = null; // e.g., "https://local1:7002/ims-ws/httpinvoker/CommandServer"
    
    /**
     * Boolean Flag to easily determine if a configuration properties file exists already for the RSA connectivity 
     * configuration
     */
    private Boolean ConfigFileExists = true;
    
    /**
     * The User ID the connector uses to connect to the RSA AM server. This User Principal must exist in RSA AM
     * and have the Auth Mgr User Admin administrative role.
     */
    private String UserMgrPrincipal = "openicf";
    
    /**
     * The Password of the User Manager
     */
    private GuardedString UserMgrPwd = null;
    
    /**
     * The RSA Security Domain to use with this Connector instance
     */
    private String SecurityDomain = null;

    /**
     * Constructor.
     */
    public RSAAuthenticationManager8Configuration() {
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "NamingProviderUrl.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "NamingProviderUrl.help",
            required = false, confidential = false)
    public String getNamingProviderUrl() {
        return NamingProviderUrl;
    }
    public void setNamingProviderUrl(String NamingProviderUrl) {
        this.NamingProviderUrl = NamingProviderUrl;
    }
    
    @ConfigurationProperty(order = 1, displayMessageKey = "CmdclientUser.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "CmdclientUser.help",
            required = false, confidential = false)
    public String getCmdclientUser() {
        return CmdclientUser;
    }
    public void setCmdclientUser(String CmdclientUser) {
        this.CmdclientUser = CmdclientUser;
    }  

    @ConfigurationProperty(order = 1, displayMessageKey = "CmdClientUserPwd.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "CmdClientUserPwd.help",
            required = false, confidential = true)
    public GuardedString getCmdClientUserPwd() {
        return CmdClientUserPwd;
    }
    public void setCmdClientUserPwd(GuardedString CmdClientUserPwd) {
        this.CmdClientUserPwd = CmdClientUserPwd;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "RsaSslClientIdStorePwd.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "RsaSslClientIdStorePwd.help",
            required = false, confidential = true)
    public GuardedString getRsaSslClientIdStorePwd() {
        return RsaSslClientIdStorePwd;
    }
    public void setRsaSslClientIdStorePwd(GuardedString RsaSslClientIdStorePwd) {
        this.RsaSslClientIdStorePwd = RsaSslClientIdStorePwd;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "RsaSslClientIdKeyPwd.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "RsaSslClientIdKeyPwd.help",
            required = false, confidential = true)
    public GuardedString getRsaSslClientIdKeyPwd() {
        return RsaSslClientIdKeyPwd;
    }
    public void setRsaSslClientIdKeyPwd(GuardedString RsaSslClientIdKeyPwd) {
        this.RsaSslClientIdKeyPwd = RsaSslClientIdKeyPwd;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "ImsSslClientProviderUrl.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ImsSslClientProviderUrl.help",
            required = false, confidential = false)
    public String getImsSslClientProviderUrl() {
        return ImsSslClientProviderUrl;
    }
    public void setImsSslClientProviderUrl(String ImsSslClientProviderUrl) {
        this.ImsSslClientProviderUrl = ImsSslClientProviderUrl;
    }
    
    @ConfigurationProperty(order = 1, displayMessageKey = "ImsSslClientIdentityKeystoreFilename.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ImsSslClientIdentityKeystoreFilename.help",
            required = false, confidential = false)
    public String getImsSslClientIdentityKeystoreFilename() {
        return ImsSslClientIdentityKeystoreFilename;
    }
    public void setImsSslClientIdentityKeystoreFilename(String ImsSslClientIdentityKeystoreFilename) {
        this.ImsSslClientIdentityKeystoreFilename = ImsSslClientIdentityKeystoreFilename;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "ImsSslClientIdentityKeyAlias.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ImsSslClientIdentityKeyAlias.help",
            required = false, confidential = false)
    public String getImsSslClientIdentityKeyAlias() {
        return ImsSslClientIdentityKeyAlias;
    }

    public void setImsSslClientIdentityKeyAlias(String ImsSslClientIdentityKeyAlias) {
        this.ImsSslClientIdentityKeyAlias = ImsSslClientIdentityKeyAlias;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "ImsSslClientRootCaAlias.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ImsSslClientRootCaAlias.help",
            required = false, confidential = false)
    public String getImsSslClientRootCaAlias() {
        return ImsSslClientRootCaAlias;
    }
    public void setImsSslClientRootCaAlias(String ImsSslClientRootCaAlias) {
        this.ImsSslClientRootCaAlias = ImsSslClientRootCaAlias;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "ImsSoapClientProviderUrl.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ImsSoapClientProviderUrl.help",
            required = false, confidential = false)
    public String getImsSoapClientProviderUrl() {
        return ImsSoapClientProviderUrl;
    }
    public void setImsSoapClientProviderUrl(String ImsSoapClientProviderUrl) {
        this.ImsSoapClientProviderUrl = ImsSoapClientProviderUrl;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "ImsHttpinvokerClientProviderUrl.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ImsHttpinvokerClientProviderUrl.help",
            required = false, confidential = false)
    public String getImsHttpinvokerClientProviderUrl() {
        return ImsHttpinvokerClientProviderUrl;
    }
    public void setImsHttpinvokerClientProviderUrl(String ImsHttpinvokerClientProviderUrl) {
        this.ImsHttpinvokerClientProviderUrl = ImsHttpinvokerClientProviderUrl;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "InitialNamingFactory.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "InitialNamingFactory.help",
            required = false, confidential = false)
    public String getInitialNamingFactory() {
        return InitialNamingFactory;
    }
    @ConfigurationProperty(order = 1, displayMessageKey = "ConfigFileExists.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "ConfigFileExists.help",
            required = false, confidential = false)
    public Boolean getConfigFileExists() {
        return ConfigFileExists;
    }
    public void setConfigFileExists(Boolean ConfigFileExists) {
        this.ConfigFileExists = ConfigFileExists;
    }
    
    @ConfigurationProperty(order = 1, displayMessageKey = "UserMgrPrincipal.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "UserMgrPrincipal.help",
            required = false, confidential = false)
    public String getUserMgrPrincipal() {
        return UserMgrPrincipal;
    }
    public void setUserMgrPrincipal(String UserMgrPrincipal) {
        this.UserMgrPrincipal = UserMgrPrincipal;
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "UserMgrPwd.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "UserMgrPwd.help",
            required = false, confidential = false)
    public GuardedString getUserMgrPwd() {
        return UserMgrPwd;
    }
    public void setUserMgrPwd(GuardedString UserMgrPwd) {
        this.UserMgrPwd = UserMgrPwd;
    }

    /* @ConfigurationProperty(order = 1, displayMessageKey = "SecurityDomain.display",
            //groupMessageKey = "basic.group",
            helpMessageKey = "SecurityDomain.help",
            required = false, confidential = false)
    */
    public String getSecurityDomain() {
        return SecurityDomain;
    }
    public void setSecurityDomain(String SecurityDomain) {
        this.SecurityDomain = SecurityDomain;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        // Check if the CONFIG_PROPERTIES_FILE file exists. If it does, then the connector will just use that.
        // If not, the connector requires the following properties to be supplied through setters.
        
        // 1- Read Prop file
        Properties prop = new Properties();

        try {
            //load a properties file
            prop.load(new FileInputStream(CONFIG_PROPERTIES_FILE));
            logger.info("A configuration file exists, the Connector will use it.");
            this.ConfigFileExists = true;
            
        } catch (IOException ex) {
            
            logger.warn("A configuration file doesn't exist. The connection configuration expects its properties to have been set manually.");
            this.ConfigFileExists = false;
            
            //2 - Only check if the properties have been supplied if the file doesn't exist
            if (StringUtil.isBlank(CmdclientUser)) {
                throw new IllegalArgumentException("Command Client User cannot be null or empty.");
            }
            if (StringUtil.isBlank(NamingProviderUrl)) {
                throw new IllegalArgumentException("Naming Provider URL cannot be null or empty.");
            }
            if (StringUtil.isBlank(ImsSslClientProviderUrl)) {
                throw new IllegalArgumentException("IMS SSL Client Provider URL cannot be null or empty.");
            }
            if (StringUtil.isBlank(ImsSslClientIdentityKeystoreFilename)) {
                throw new IllegalArgumentException("IMS SSL Client Identity Keystore File Name cannot be null or empty.");
            }
            if (StringUtil.isBlank(ImsSslClientIdentityKeyAlias)) {
                throw new IllegalArgumentException("IMS SSL keystore alias for the private key cannot be null or empty.");
            }
            if (StringUtil.isBlank(UserMgrPrincipal)) {
                throw new IllegalArgumentException("User Manager Principal cannot be null or empty.");
            }
        }
    }

}
