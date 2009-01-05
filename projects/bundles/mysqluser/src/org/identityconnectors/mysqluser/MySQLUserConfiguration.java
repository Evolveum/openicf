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
package org.identityconnectors.mysqluser;

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
     * User, Id, Key field
     */
    public static final String MYSQL_USER = "User";
    
    /**
     * table name
     */
    public static final String MYSQL_USER_TABLE = "mysql.user";
    
    
    
    /**
     * The datasource name is used to connect to database.
     */
    private String datasource;

    /**
     * Return the datasource 
     * @return datasource value
     */
    @ConfigurationProperty(order = 1, helpMessageKey = "mysqluser.datasource.help", displayMessageKey = "mysqluser.datasource.display")
    public String getDatasource() {
        return datasource;
    }

    /**
     * @param value
     */
    public void setDatasource(String value) {
        this.datasource = value;
    }
    
    
    /**
     * The jndiFactory name is used to connect to database.
     */
    private String[] jndiProperties;

    /**
     * Return the jndiFactory 
     * @return jndiFactory value
     */
    @ConfigurationProperty(order = 2, helpMessageKey = "mysqluser.jndiProperties.help", displayMessageKey = "mysqluser.jndiProperties.display")
    public String[] getJndiProperties() {
        return jndiProperties;
    }

    /**
     * @param value
     */
    public void setJndiProperties(String[] value) {
        this.jndiProperties = value;
    }

    /**
     * Host
     */
    public static final String HOST = "Host";



    /**
     * driver
     */
    private String driver = "com.mysql.jdbc.Driver"; // Driver

    /**
     * The setter method 
     * @return a driver value
     */
    @ConfigurationProperty(order = 4, helpMessageKey = "mysqluser.driver.help", displayMessageKey = "mysqluser.driver.display")
    public String getDriver() {
        return this.driver;
    }
        
    /**
     * @param value
     */
    public void setDriver(final String value) {
        this.driver = value;
    }

    /**
     * host
     */
    private String host = ""; // Host
    
    /**
     * @return the host
     */
    @ConfigurationProperty(order = 5, helpMessageKey = "mysqluser.host.help", displayMessageKey = "mysqluser.host.display")
    public String getHost() {
        return host;
    }    

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }    

    /**
     * user
     */
    private String user = ""; // Login

    /**
     * The setter method 
     * @return user value
     */
    @ConfigurationProperty(order = 6, helpMessageKey = "mysqluser.login.help", displayMessageKey = "mysqluser.login.display")
    public String getUser() {
        return this.user;
    }

    /**
     * @param value
     */
    public void setUser(final String value) {
        this.user = value;
    }

    /**
     * password
     */
    private GuardedString password; // Password


    /**
     * The setter method 
     * @return passport value
     */
    @ConfigurationProperty(order = 7, helpMessageKey = "mysqluser.pwd.help", displayMessageKey = "mysqluser.pwd.display", confidential = true)
    public GuardedString getPassword() {
        return this.password;
    }

    /**
     * @param value
     */
    public void setPassword(final GuardedString value) {
        this.password = value;
    }
    
    /**
     * port
     */
    private String port = "3306"; // Port

    /**
     * @return the port
     */
    @ConfigurationProperty(order = 8, helpMessageKey = "mysqluser.port.help", displayMessageKey = "mysqluser.port.display")
    public String getPort() {
        return port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }    
    
    /**
     * user model
     */
    private String usermodel = "idm"; // Default User


    /**
     * @return the usermodel
     */
    @ConfigurationProperty(order = 9, required = true, helpMessageKey = "mysqluser.model.help", displayMessageKey = "mysqluser.model.display")
    public String getUsermodel() {
        return usermodel;
    }

    /**
     * @param usermodel
     *            the usermodel to set
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
        
        // check that there is not a datasource
        if(StringUtil.isBlank(getDatasource())){ 
            // determine if you can get a connection to the database..
            Assertions.blankCheck(getUser(), "user");
            // check that there is a table to query..
            Assertions.nullCheck(getPassword(), "password");

            // check that there is a driver..
            Assertions.blankCheck(getDriver(), "driver");

            Assertions.blankCheck(getHost(), "host");

            Assertions.blankCheck(getPort(), "port");

            // make sure the driver is in the class path..
            try {
                Class.forName(getDriver());
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            //Validate the JNDI properties
            JNDIUtil.arrayToHashtable(getJndiProperties(), getConnectorMessages());
        }
    }
    
    /**
     * The url string
     * @return the url string of the database
     */
    public String getUrlString() {
        final String URL_MASK = "jdbc:mysql://{0}:{1}/mysql";
        // create the connection base on the configuration..
        String url = null;
        try {
            // get the database URL..
            url = MessageFormat.format(URL_MASK, getHost(), getPort());
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        return url;
    }
      
}
