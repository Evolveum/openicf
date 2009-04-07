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
package org.identityconnectors.mysqluser;

import static org.identityconnectors.mysqluser.MySQLUserConstants.*;
import java.text.MessageFormat;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


/**
 * Implements the {@link Configuration} interface to provide all the necessary parameters to initialize the JDBC
 * Connector.
 * 
 * @version $Revision $
 * @since 1.0
 */
public class MySQLUserConfiguration extends AbstractConfiguration {

    /**
     * The datasource name is used to connect to database in server environment.
     */
    private String datasource = EMPTY_STR;

    /**
     * Return the datasource name
     * @return datasource name value
     */
    @ConfigurationProperty(order = 1, helpMessageKey = "MYSQL_DATASOURCE_HELP", displayMessageKey = "MYSQL_DATASOURCE_DISLPAY")
    public String getDatasource() {
        return datasource;
    }

    /**
     * Set the datasource name
     * @param datasource name value
     */
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }
    
    
    /**
     * The jndiFactory array of properties setting is used to connect to database in server environment.
     */
    private String[] jndiProperties;

    /**
     * Return the jndiFactoryProperties settings array
     * @return The array of the jndiFactoryProperties values
     */
    @ConfigurationProperty(order = 2, helpMessageKey = "MYSQL_JNDI_PROPERTIES_HELP", displayMessageKey = "MYSQL_JNDI_PROPERTIES_DISPLAY")
    public String[] getJndiProperties() {
        return jndiProperties;
    }

    /**
     * Set the factory settings array
     * @param jndiProperties an array of <CODE>String</CODE>'s JndiProperties values
     */
    public void setJndiProperties(String[] jndiProperties) {
        this.jndiProperties = jndiProperties;
    }

    /**
     * the jdbc driver class name
     */
    private String driver = DEFAULT_DRIVER; // Driver

    /**
     * The getter for jdbc driver class name 
     * @return the  <CODE>String</CODE> jdbc driver class name value
     */
    @ConfigurationProperty(order = 4, helpMessageKey = "MYSQL_DRIVER_HELP", displayMessageKey = "MYSQL_DRIVER_DISPLAY")
    public String getDriver() {
        return this.driver;
    }
        
    /**
     * Sets the jdbc driver class name
     * @param driver the <CODE>String</CODE> jdbc class name value
     */
    public void setDriver(final String driver) {
        this.driver = driver;
    }

    /**
     * MySQL database host name
     */
    private String host = EMPTY_STR; // Host
    
    /**
     * Getter for MySQL database host name
     * @return the  <CODE>String</CODE> host name value
     */
    @ConfigurationProperty(order = 5, helpMessageKey = "MYSQL_HOST_HELP", displayMessageKey = "MYSQL_HOST_DISPLAY")
    public String getHost() {
        return host;
    }    

    /**
     * Setter for Mysql database host name
     * @param host name a <CODE>String</CODE> value
     */
    public void setHost(String host) {
        this.host = host;
    }    

    /**
     * MySQL user (admin user) name able to manage users
     */
    private String user = EMPTY_STR; // Login

    /**
     * The user name getter method 
     * @return The  <CODE>String</CODE> user name value
     */
    @ConfigurationProperty(order = 6, helpMessageKey = "MYSQL_USER_HELP", displayMessageKey = "MYSQL_USER_DISPLAY")
    public String getUser() {
        return this.user;
    }

    /**
     * Setter for user name 
     * @param user name value
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * password
     */
    private GuardedString password; // Password


    /**
     * The getter method 
     * @return The <CODE>GuardedString</CODE> passport object
     */
    @ConfigurationProperty(order = 7, helpMessageKey = "MYSQL_PWD_HELP", displayMessageKey = "MYSQL_PWD_DISPLAY", confidential = true)
    public GuardedString getPassword() {
        return this.password;
    }

    /**
     * Setter for password
     * @param password a <CODE>GuardedString</CODE> passport object
     */
    public void setPassword(final GuardedString password) {
        this.password = password;
    }
    
    /**
     * port on witch the MySQL database is listenning
     */
    private String port = DEFAULT_PORT; // Port

    /**
     * The getter  
     * @return the database port number value
     */
    @ConfigurationProperty(order = 8, helpMessageKey = "MYSQL_PORT_HELP", displayMessageKey = "MYSQL_PORT_DISPLAY")
    public String getPort() {
        return port;
    }

    /**
     * The port setter 
     * @param port value 
     */
    public void setPort(String port) {
        this.port = port;
    }    
    
    /**
     * user model, from with the new user are duplicated
     */
    private String usermodel = DEFAULT_USER_MODEL; // Default User


    /**
     * Getter the usermodel 
     * @return the usermodel name
     */
    @ConfigurationProperty(order = 9, required = true, helpMessageKey = "MYSQL_USERMODEL_HELP", displayMessageKey = "MYSQL_USERMODEL_DISPLAY")
    public String getUsermodel() {
        return usermodel;
    }

    /**
     * Setter for usermodel
     * @param usermodel name valueS
     */
    public void setUsermodel(String usermodel) {
        this.usermodel = usermodel;
    }

    /**
     * Attempt to validate the arguments added to the Configuration.
     * 
     * @see org.identityconnectors.framework.Configuration#validate()
     */
    @Override
    public void validate() {

        Assertions.blankCheck(getUsermodel(), "usermodel");        
        if (StringUtil.isBlank(getUsermodel())) {
            throw new IllegalArgumentException(getMessage( MSG_USER_MODEL_BLANK));
         }
        
        // check that there is not a datasource
        if(StringUtil.isBlank(getDatasource())){ 
            // determine if you can get a connection to the database..
            if (StringUtil.isBlank(getUser())) {
                throw new IllegalArgumentException(getMessage(MSG_USER_BLANK));
             }
            // check that there is a pwd to query..
            if (getPassword() == null) {
                throw new IllegalArgumentException(getMessage(MSG_PASSWORD_BLANK));
             }

            if (StringUtil.isBlank(getHost())) {
                throw new IllegalArgumentException(getMessage(MSG_HOST_BLANK));
            }

            // port required
            if (StringUtil.isBlank(getPort())) {
                throw new IllegalArgumentException(getMessage(MSG_PORT_BLANK));
            }           

            // check that there is a driver..
            if (StringUtil.isBlank(getDriver())) {
                throw new IllegalArgumentException(getMessage(MSG_JDBC_DRIVER_BLANK));
            }   
            // make sure the driver is in the class path..
            try {
                Class.forName(getDriver());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(getMessage(MSG_JDBC_DRIVER_NOT_FOUND));
            }            
            
        } else {
            //Validate the JNDI properties
            JNDIUtil.arrayToHashtable(getJndiProperties(), getConnectorMessages());
        }
    }
    
    /**
     * Format the connector message
     * @param key key of the message
     * @return return the formated message
     */
    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }
    
    /**
     * Format message with arguments 
     * @param key key of the message
     * @param objects arguments
     * @return the localized message string
     */
    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }        
    
    /**
     * The url string
     * @return the url string of the database
     */
    public String getUrlString() {
        final String URL_TEMPLATE = "jdbc:mysql://{0}:{1}/mysql";
        // create the connection base on the configuration..
        String url = null;
        try {
            // get the database URL..
            url = MessageFormat.format(URL_TEMPLATE, getHost(), getPort());
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        return url;
    }
      
}
