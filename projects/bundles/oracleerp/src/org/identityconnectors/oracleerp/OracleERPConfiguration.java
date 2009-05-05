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

import java.text.MessageFormat;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
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
public class OracleERPConfiguration extends AbstractConfiguration {

    /**
     * Datasource attributed
     * The attribute has precedence over other database connection related attributes.
     * 
     * imported adapter attribute
     * name="dataSource" type="string" multi="false" value="jdbc/SampleDataSourceName"
     * displayName="DATA_SOURCE_NAME" description="HELP_393"
     */
    String dataSource="";

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
    public void setJndiProperties(String[] value) {
        this.jndiProperties = value;
    }
    
    /** */
    private static final String DEFAULT_DRIVER = "oracle.jdbc.driver.OracleDriver";

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

    /** */
    private static final String ORACLE_THIN_CONN_URL = "java:oracle:thin:@{0}:{1}:{2}";

    /** */
    private static final String DEFAULT_CONN_URL = "java:oracle:thin:@HOSTNAME:PORT:DB";
    
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

    /** */
    private static final String DEFAULT_PORT = "1521";

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
    
    /** */
    private static final String DEFAULT_USER = "APPS";
    
    /**
     * User attribute
     * 
     * imported adapter attribute
     * name="user" displayName="USER" type="string" multi="false"
     * description="HELP_286"  value="APPL"
     */
    private String user = DEFAULT_USER;

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
    
    /*
     * implemented by framework, left as comment for the reference
     * name="useConnectionPool" type="string" multi="false" value="FALSE"
     * displayName="CONN_POOLING" description="HELP_395"
     */ 
    
    /*
     * implemented by framework, left as comment for the reference
     * name="idleTimeout" type="string" multi="false" value="" + IDLE_TIMEOUT
     * displayName="DBPOOL_IDLETIMEOUT" description="DBPOOL_IDLETIMEOUT_HELP"
     */ 
       
    /*
     * implemented by framework, left as comment for the reference
     * name="encryptionTypesClient"  type="string" multi="false"  value="RC4_128"
     * displayName="ENCRYPTION_TYPES_CLIENT" description="HELP_ORACLE_ERP_CLIENT_ENCRYPTION_ALGORITHMS"
     */ 
    
    /*
     * implemented by framework, left as comment for the reference
     * name="encryptionClient" type="string" multi="false" value="ACCEPTED"
     * displayName="ENCRYPTION_CLIENT" description="HELP_ORACLE_ERP_CLIENT_ENCRYPTION_LEVEL"
     */  
    
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
    private String userActions = "";

    /**
     * Getter for the userActions attribute.
     * @return userActions attribute
     */
    public String getUserActions() {
        return userActions;
    }

    /**
     * Setter for the userActions attribute.
     * @param userActions attribute.
     */
    @ConfigurationProperty(order=15 ,displayMessageKey="AFTER_ACTION_DISPLAY", helpMessageKey="AFTER_ACTION_HELP")
    public void setUserActions(String userActions) {
        this.userActions = userActions;
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
    @ConfigurationProperty(order=16 ,displayMessageKey="NO_SCHEMA_ID_DISPLAY", helpMessageKey="NO_SCHEMA_ID_HELP")
    public void setNoSchemaId(boolean noSchemaId) {
        this.noSchemaId = noSchemaId;
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
        Assertions.blankCheck(user, "user");
        Assertions.nullCheck(password, "password");
        if (StringUtil.isBlank(dataSource)) {
            if(StringUtil.isBlank(url)) {
                Assertions.blankCheck(host, "database"); 
                Assertions.blankCheck(port, "database"); 
                Assertions.blankCheck(database, "database"); 
            }
        } else {
            //Validate the JNDI properties
            JNDIUtil.arrayToHashtable(getJndiProperties(), getConnectorMessages());           
        }
    }

    /**
     * The application id from the user
     * see the bug id. 19352
     * @return The "APPL." or empty, if noSchemaId is true
     */
    public String app() {
        if(noSchemaId) return "";
        return user.trim().toUpperCase()+".";
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
    
}
