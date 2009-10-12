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
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.text.MessageFormat;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Implements the {@link Configuration} interface to provide all the necessary
 * parameters to initialize the OracleErp Connector.
 *
 * @version 1.0
 * @since 1.0
 */
final public class OracleERPConfiguration extends AbstractConfiguration implements Messages {

    /**
     * Setup logging.
     */
    private static final Log log = Log.getLog(OracleERPConfiguration.class);

    /** Oracle thin url pattern, used when formating the url from components */
    static final String ORACLE_THIN_CONN_URL = "java:oracle:thin:@{0}:{1}:{2}";

    /** Oracle user friendly url default, when left unchanged, the components are considered */
    static final String DEFAULT_CONN_URL = "java:oracle:thin:@HOSTNAME:PORT:DB";    

    /** Oracle default port */
    static final String DEFAULT_PORT = "1521";

    /** Predefined encryption type */
    static final String DEFAULT_ENCRYPTION_TYPE = "RC4_128";
 
    /** Predefined encryption level*/
    static final String DEFAULT_ENCRYPTION_LEVEL = "ACCEPTED";
    
    /**
     * Datasource attributed
     * The attribute has precedence over other database connection related attributes.
     *
     * imported adapter attribute
     * name="dataSource" type="string" multi="false" value="jdbc/SampleDataSourceName"
     * displayName="DATA_SOURCE_NAME" description="HELP_393"
     */
    private String dataSource="";

    /**
     * Getter for the driver dataSource.
     * @return dataSource
     * add support for dataSource in the future
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Setter for the dataSource attribute.
     * @param dataSource
     */
    @ConfigurationProperty(order=1 ,displayMessageKey="DATA_SOURCE_DISPLAY", helpMessageKey="DATA_SOURCE_DISPLAY")
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }


    /**
     * The jndiFactory name is used to connect to database.
     */
    private String[] jndiProperties;

    /**
     * Return the jndiFactory
     * @return jndiFactory value
     */
    public String[] getJndiProperties() {
        return jndiProperties;
    }

    /**
     * @param value
     */
    @ConfigurationProperty(order = 2,displayMessageKey="JNDI_PROPERTIES_DISPLAY", helpMessageKey="JNDI_PROPERTIES_DISPLAY")
    public void setJndiProperties(final String[] value) {
        this.jndiProperties = value;
    }

    /**
     * Driver attribute,
     * Ignored if <b>dataSource</b> attribute or <b>url</b> attribute is specified
     *
     * imported adapter attribute
     * name="driver" type="string" multi="false" value="oracle.jdbc.driver.OracleDriver"
     * displayName="DRIVER" description="HELP_369"
     */
    private String driver = DEFAULT_DRIVER;

    /**
     * Getter for the driver attribute.
     * @return driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Setter for the driver attribute.
     * @param driver
     */
    @ConfigurationProperty(order=3 ,displayMessageKey="DRIVER_DISPLAY", helpMessageKey="DRIVER_HELP")
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Database connection url
     * Ignored if <b>dataSource</b> attribute is specified
     *
     * imported adapter attribute
     * name="url" type="string" multi="false" value="java:oracle:thin:@HOSTNAME:PORT:DB"
     * displayName="CONN_URL" description="HELP_394"
     */
    private String url = DEFAULT_CONN_URL;


    /**
     * Getter for the url attribute.
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Setter for the url attribute.
     * @param url
     */
    @ConfigurationProperty(order=4 ,displayMessageKey="CONN_URL_DISPLAY", helpMessageKey="CONN_URL_HELP")
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Host attribute
     * Ignored if <b>dataSource</b> attribute or <b>url</b> attribute is specified
     *
     * imported adapter attribute
     * name="host" type="string" multi="false"
     * displayName="HOST"  description="HELP_239"
     */
    private String host;

    /**
     * Getter for the url attribute.
     * @return url
     */
    public String getHost() {
        return host;
    }

    /**
     * Setter for the host attribute.
     * @param host attribute.
     */
    @ConfigurationProperty(order=5 ,displayMessageKey="HOST_NAME_DISPLAY", helpMessageKey="HOST_NAME_HELP")
    public void setHost(String host) {
        this.host = host;
    }


    /**
     * Port attribute
     * Ignored if <b>dataSource</b> attribute or <b>url</b> attribute is specified
     *
     * imported adapter attribute
     * name="port" type="string" multi="false" value="1521"
     * displayName="PORT" description="HELP_269" +
     */
    private String port = DEFAULT_PORT;

    /**
     * Getter for the port attribute.
     * @return port
     */
     public String getPort() {
        return port;
    }

    /**
     * Setter for the port attribute.
     * @param port attribute.
     */
    @ConfigurationProperty(order=6 ,displayMessageKey="PORT_DISPLAY", helpMessageKey="PORT_HELP")
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Database attribute
     * Ignored if <b>dataSource</b> attribute or <b>url</b> attribute is specified
     *
     * imported adapter attribute
     * name="database" type="string" multi="false"
     * displayName="DATABASE" description="HELP_80"
     */
    private String database;

    /**
     * Getter for the database attribute.
     * @return database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Setter for the database attribute.
     * @param database attribute.
     */
    @ConfigurationProperty(order=7 ,displayMessageKey="DATABASE_NAME_DISPLAY", helpMessageKey="DATABASE_NAME_HELP")
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * User attribute
     *
     * imported adapter attribute
     * name="user" displayName="USER" type="string" multi="false"
     */
    private String user = DEFAULT_USER_NAME;

    /**
     * Getter for the user attribute.
     * @return user
     */
    public String getUser() {
        return user;
    }

    /**
     * Setter for the user attribute.
     * @param user attribute.
     */
    @ConfigurationProperty(order=8 ,displayMessageKey="USER_DISPLAY", helpMessageKey="USER_HELP")
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Password attribute
     *
     * imported adapter attribute
     * name="password" type="encrypted" multi="false"
     * displayName="PASSWORD" description="HELP_262"
     */
    private GuardedString password;

    /**
     * Getter for the user attribute.
     * @return user
     */
    public GuardedString getPassword() {
        return password;
    }

    /**
     * Setter for the password attribute.
     * @param password attribute.
     */
    @ConfigurationProperty(order=9 ,displayMessageKey="PASSWORD_DISPLAY", helpMessageKey="PASSWORD_HELP", confidential=true)
    public void setPassword(GuardedString password) {
        this.password = password;
    }

    /**
     * Audit Responsibility attribute,  desired Oracle ERP responsibility
     *
     * imported adapter attribute
     * name="auditResponsibility"  type="string" multi="false"
     * displayName="AUDIT_RESPONSIBILITY" description="HELP_ORACLE_ERP_ADMIN_USER_RESPONSIBILITY"
     */
    private String auditResponsibility="";

    /**
     * Getter for the user attribute.
     * @return user
     */
    public String getAuditResponsibility() {
        return auditResponsibility;
    }

    /**
     * Setter for the auditResponsibility attribute.
     * @param auditResponsibility attribute.
     */
    @ConfigurationProperty(order=10 ,displayMessageKey="AUDIT_RESPONSIBILITY_DISPLAY", helpMessageKey="AUDIT_RESPONSIBILITY_HELP")
    public void setAuditResponsibility(String auditResponsibility) {
        this.auditResponsibility = auditResponsibility;
    }

    /**
     * Manage Securing Attributes attribute
     * TRUE to manage securing attributes, or FALSE to ignore the schema attribute
     *
     * imported adapter attribute
     * name="manageSecuringAttrs" type="string" multi="false" value="TRUE"
     * displayName="MANAGE_SECURING_ATTRS" description="HELP_ORACLE_ERP_MANAGE_SECURING_ATTRS"
     */
    private boolean manageSecuringAttrs=true;

    /**
     * Getter for the manageSecuringAttrs attribute.
     * @return manageSecuringAttrs attribute
     */
    public boolean isManageSecuringAttrs() {
        return manageSecuringAttrs;
    }

    /**
     * Setter for the manageSecuringAttrs attribute.
     * @param manageSecuringAttrs attribute.
     */
    @ConfigurationProperty(order=11 ,displayMessageKey="MANAGE_SECURING_ATTRS_DISPLAY", helpMessageKey="MANAGE_SECURING_ATTRS_HELP")
    public void setManageSecuringAttrs(boolean manageSecuringAttrs) {
        this.manageSecuringAttrs = manageSecuringAttrs;
    }

    /**
     * Return the Set of Books and/or Organization associated with auditor responsibility
     * false will increase performance
     *
     * imported adapter attribute
     * name="returnSobOrgAttrs" type="string" multi="false" value="FALSE"
     * displayName="RETURN_SOB_AND_ORG" description="HELP_ORACLE_ERP_RETURN_SOB_AND_ORG_ATTRS"
     */
    private boolean returnSobOrgAttrs = false;

    /**
     * Getter for the returnSobOrgAttrs attribute.
     * @return returnSobOrgAttrs attribute
     */
    public boolean isReturnSobOrgAttrs() {
        return returnSobOrgAttrs;
    }

    /**
     * Setter for the returnSobOrgAttrs attribute.
     * @param returnSobOrgAttrs attribute.
     */
    @ConfigurationProperty(order=12 ,displayMessageKey="RETURN_SOB_AND_ORG_DISPLAY", helpMessageKey="RETURN_SOB_AND_ORG_HELP")
    public void setReturnSobOrgAttrs(boolean returnSobOrgAttrs) {
        this.returnSobOrgAttrs = returnSobOrgAttrs;
    }

    /**
     * Set to a value to limit accounts returned
     * If true, then only accounts with START_DATE and END_DATE spanning SYSDATE are returned.
     * The default value is false; in this case, all accounts on the resource are returned.
     *
     * imported adapter attribute
     * name="activeAccountsOnly"  type="string" multi="false" value="FALSE"
     * displayName="ORACLE_ERP_ACTIVE_ACCOUNTS_ONLY" description="HELP_ORACLE_ERP_ACTIVE_ACCOUNTS_ONLY"
     */
    private boolean activeAccountsOnly=false;

    /**
     * Getter for the activeAccountsOnly attribute.
     * @return activeAccountsOnly attribute
     */
    public boolean isActiveAccountsOnly() {
        return activeAccountsOnly;
    }

    /**
     * Setter for the activeAccountsOnly attribute.
     * @param activeAccountsOnly attribute.
     */
    @ConfigurationProperty(order=13 ,displayMessageKey="ACTIVE_ACCOUNTS_ONLY_DISPLAY", helpMessageKey="ACTIVE_ACCOUNTS_ONLY_HELP")
    public void setActiveAccountsOnly(boolean activeAccountsOnly) {
        this.activeAccountsOnly = activeAccountsOnly;
    }

    /**
     * Parameter indicate to limit which accounts to be managed by IDM.
     * The limitation is by adding WHERE clause
     * If enabled, 'Active Accounts Only' parameterName is ignored.
     * Default value is false
     *
     * imported adapter attribute
     * name="ACCOUNTS_INCLUDED" type="string" multi="false"
     * displayName="ORACLE_ERP_ACCOUNTS_INCLUDED" description="ORACLE_ERP_ACCOUNTS_INCLUDED_HELP"
     */
    private String accountsIncluded="";

    /**
     * Getter for the accountsIncluded attribute.
     * @return accountsIncluded attribute
     */
    public String getAccountsIncluded() {
        return accountsIncluded;
    }

    /**
     * Setter for the accountsIncluded attribute.
     * @param accountsIncluded attribute.
     */
    @ConfigurationProperty(order=14 ,displayMessageKey="ACCOUNTS_INCLUDED_DISPLAY", helpMessageKey="ACCOUNTS_INCLUDED_HELP")
    public void setAccountsIncluded(String accountsIncluded) {
        this.accountsIncluded = accountsIncluded;
    }

    /**
     * Enter the name of the resource action that contains the script
     * used to retrieve additional custom account attributes
     * for a user from this resource
     *
     * imported adapter attribute
     * name="GetUser Actions"  type="string" multi="false" required="false"
     * displayName="GETUSER_AFTER_ACTION" description="GETUSER_AFTER_ACTION_HELP"
     */
    private Script userAfterActionScript = null;

    /**
     * Getter for the userAfterActions attribute.
     * @return userAfterActions attribute
     */
    public Script getUserAfterActionScript() {
        return userAfterActionScript;
    }

    /**
     * Setter for the userAfterActions attribute.
     * @param userAfterActionScript attribute.
     */
    @ConfigurationProperty(order=15 ,displayMessageKey="USER_AFTER_ACTION_SCRIPT_DISPLAY", helpMessageKey="USER_AFTER_ACTION_SCRIPT_HELP")
    public void setUserAfterActionScript(Script userAfterActionScript) {
        this.userAfterActionScript = userAfterActionScript;
    }   
    
    /**
     * When true, the schema identifier will not be prefixed to table names.
     * When false, a schema identifier is prefixed to tables names.
     * Defaults to false.
     *
     * imported adapter attribute
     * name='noSchemaId' type='string' multi='false' required='false'
     * description='HELP_NO_SCHEMA_ID" value='FALSE'
     */
    private boolean noSchemaId = false;


    /**
     * Getter for the noSchemaId attribute.
     * @return the noSchemaId value
     */
    public boolean isNoSchemaId() {
        return noSchemaId;
    }

    /**
     * Setter for the noSchemaId attribute.
     * @param noSchemaId
     */
    @ConfigurationProperty(order=17 ,displayMessageKey="NO_SCHEMA_ID_DISPLAY", helpMessageKey="NO_SCHEMA_ID_HELP")
    public void setNoSchemaId(boolean noSchemaId) {
        this.noSchemaId = noSchemaId;
    }
    

    /*
     * implemented by framework, left as comment for the reference
     * name="encryptionTypesClient"  type="string" multi="false"  value="RC4_128"
     * displayName="ENCRYPTION_TYPES_CLIENT" description="HELP_ORACLE_ERP_CLIENT_ENCRYPTION_ALGORITHMS"
     * 
     * CLIENT_ENCRYPTION_ALGORITHMS
     * old name = encryptionTypesClient, oracle.net.encryption_types_client
     */
    private String clientEncryptionType = DEFAULT_ENCRYPTION_TYPE;
    
    /**
     * Getter
     * @return clientEncryptionType value
     */
    public String getClientEncryptionType() {
        return clientEncryptionType;
    }

    /**
     * Setter
     * @param clientEncryptionType
     */
    @ConfigurationProperty(order=18 ,displayMessageKey="CLIENT_ENCRYPTION_ALGORITHMS_DISPLAY", helpMessageKey="CLIENT_ENCRYPTION_ALGORITHMS_HELP")
    public void setClientEncryptionType(String clientEncryptionType) {
        this.clientEncryptionType = clientEncryptionType;
    }

    /**
    /*
     * implemented by framework, left as comment for the reference
     * name="encryptionClient" type="string" multi="false" value="DEFAULT_ENCRYPTION_LEVEL"
     * displayName="ENCRYPTION_CLIENT" description="HELP_ORACLE_ERP_CLIENT_ENCRYPTION_LEVEL"
     * 
     * CLIENT_ENCRYPTION_LEVEL
     * old name = encryptionClient, oracle.net.encryption_client
     */
    private String clientEncryptionLevel = DEFAULT_ENCRYPTION_LEVEL;    

    /**
     * Getter
     * @return clientEncryptionLevel value
     */
    public String getClientEncryptionLevel() {
        return clientEncryptionLevel;
    }

    /**
     * Setter
     * @param clientEncryptionLevel
     */
    @ConfigurationProperty(order=19 ,displayMessageKey="CLIENT_ENCRYPTION_LEVEL_DISPLAY", helpMessageKey="CLIENT_ENCRYPTION_LEVEL_HELP")
    public void setClientEncryptionLevel(String clientEncryptionLevel) {
        this.clientEncryptionLevel = clientEncryptionLevel;
    }

    /**
     * Responsibility Id
     */
    private String respId = "";

    /**
     * Accessor for the respId property
     *
     * @return the respId
     */
    String getRespId() {
        return respId;
    }

    /**
     * Setter for the respId property.
     * @param respId the respId to set
     */
    void setRespId(String respId) {
        this.respId = respId;
    }

    /**
     * Responsibility Application Id
     */
    private String respApplId = "";

    /**
     * Accessor for the respApplId property
     *
     * @return the respApplId
     */
    String getRespApplId() {
        return respApplId;
    }


    /**
     * Setter for the respApplId property.
     * @param respApplId the respApplId to set
     */
    void setRespApplId(String respApplId) {
        this.respApplId = respApplId;
    }

    /**
     * If 12, determine if description field exists in responsibility views. Default to true
     */
    private boolean descrExists = true;

    /**
     * Accessor for the descrExists property
     *
     * @return the descrExists
     */
    public boolean isDescrExists() {
        return descrExists;
    }

    /**
     * Setter for the descrExists property.
     * @param descrExists the descrExists to set
     */
    void setDescrExists(boolean descrExists) {
        this.descrExists = descrExists;
    }


    /**
     * Check to see which responsibility account attribute is sent. Version 11.5.9 only supports responsibilities, and
     * 11.5.10 only supports directResponsibilities and indirectResponsibilities Default to false If 11.5.10, determine
     * if description field exists in responsibility views.
     */
    private boolean newResponsibilityViews = false;

    /**
     * Accessor for the newResponsibilityViews property
     *
     * @return the newResponsibilityViews
     */
    public boolean isNewResponsibilityViews() {
        return newResponsibilityViews;
    }

    /**
     * Setter for the newResponsibilityViews property.
     * @param newResponsibilityViews the newResponsibilityViews to set
     */
    void setNewResponsibilityViews(boolean newResponsibilityViews) {
        this.newResponsibilityViews = newResponsibilityViews;
    }


    /**
     * User id from cfg.User
     */
    private String userId = "";

    /**
     * Accessor for the userId property
     * @return the userId
     */
    String getUserId() {
        return userId;
    }

    /**
     * @param userId
     */
    void setUserId(String userId) {
       this.userId = userId;
    }

    /**
     * Accessor for the adminUserId property
     * @return the adminUserId
     */
    int getAdminUserId() {
        try {
            log.ok("The adminUserId is : {0} ", userId);
            return new Integer(userId).intValue();
        } catch (Exception ex) {
            log.error(ex, "The User Id String {0} is not a number", userId);
            return 0;
        }
    }

    /**
     * The cached schema
     */
    private Schema schema;

    /**
     * Accessor for the schema property
     * @return the schema
     */
    Schema getSchema() {
        return schema;
    }

    /**
     * Setter for the schema property.
     * @param schema the schema to set
     */
    void setSchema(Schema schema) {
        this.schema = schema;
    }

    /**
     * Constructor
     */
    public OracleERPConfiguration() {
        //empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        if (StringUtil.isBlank(dataSource)) {
            if(getPassword()==null){
                log.info("validate Password");
                throw new IllegalArgumentException(getMessage(MSG_PASSWORD_BLANK));
            }
            if(StringUtil.isBlank(user)){
                log.info("validate user");
                throw new IllegalArgumentException(getMessage(MSG_USER_BLANK));
            }
            if(StringUtil.isBlank(driver)){
                log.info("validate driver");
                throw new IllegalArgumentException(getMessage(MSG_DRIVER_BLANK));
            }
            try {
                log.info("validate driver forName");
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(getMessage(MSG_DRIVER_NOT_FOUND));
            }
            if(StringUtil.isBlank(url)) {
                if(StringUtil.isBlank(host)){
                    log.info("validate driver host");
                    throw new IllegalArgumentException(getMessage(MSG_HOST_BLANK));
                }
                if(StringUtil.isBlank(port)){
                    log.info("validate driver port");
                    throw new IllegalArgumentException(getMessage(MSG_PORT_BLANK));
                }
                if(StringUtil.isBlank(database)){
                    log.info("validate driver database");
                    throw new IllegalArgumentException(getMessage(MSG_DATABASE_BLANK));
                }
            }
            log.ok("driver configuration is ok");
        } else {
            log.info("validate dataSource");
            //Validate the JNDI properties
            JNDIUtil.arrayToHashtable(jndiProperties, getConnectorMessages());
            log.ok("dataSource configuration is ok");
        }
    }


    /**
     * The connection url constructed from host, port and database name, or from connection urlS
     *
     * @return The user and dot et the end.
     */
    public String getConnectionUrl() {
        if (StringUtil.isBlank(url) || DEFAULT_CONN_URL.equals(url)) {
            if(StringUtil.isNotBlank(getHost()) && StringUtil.isNotBlank(getPort()) && StringUtil.isNotBlank(getDatabase()) ) {
                url = MessageFormat.format(ORACLE_THIN_CONN_URL, getHost(), getPort(), getDatabase());
            }
        }
        return url;
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.oracleerp.Messages#getMessage(java.lang.String)
     */
    public String getMessage(String key) {
        String fmt = key;
        try {
            fmt = getConnectorMessages().format(key, key);
        } catch (Exception e) {
           log.error(e, "getMessage error");
        }
        return fmt;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.oracleerp.Messages#getMessage(java.lang.String, java.lang.Object)
     */
    public String getMessage(String key, Object... objects) {
        String fmt = key;
        try {
            fmt = getConnectorMessages().format(key, key, objects);
        } catch (Exception e) {
           log.error(e, "getMessage error");
        }
        return fmt;
    }

    /** application prefix cache */
    private String app;
    
    /**
     * The application id from the user
     * see the bug id. 19352
     * @return The "APPL." or empty, if noSchemaId is true
     */
    String app() {
        if(isNoSchemaId()) return "";
        if(StringUtil.isNotBlank(app)) {
            return app;
        }
        app = getOraUserName()+".";
        return app;
    }

    /**
     * OraUser name is a user if not empty, or DEFAULT_USER_NAME
     * @return the ora user name
     */
    String getOraUserName() {
        String userName = getUser();
        if (StringUtil.isBlank(userName)) {
            userName = DEFAULT_USER_NAME;
        } 
        return userName.trim().toUpperCase();
    }
}
