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
 * Portions Copyrighted 2013-2022 Evolveum
 */
package org.identityconnectors.databasetable;

import static org.identityconnectors.databasetable.DatabaseTableConstants.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.operations.DiscoverConfigurationOp;
import org.identityconnectors.framework.spi.operations.SyncOp;

/**
 * Implements the {@link Configuration} interface to provide all the necessary
 * parameters to initialize the JDBC Connector.
 */
public class DatabaseTableConfiguration extends AbstractConfiguration {

    /**
     * Setup logging for the {@link DatabaseTableConfiguration}.
     */
    static Log log = Log.getLog(DatabaseTableConfiguration.class);

    enum Validation {
        FULL,
        BASIC
    }

    /**
     * Type of validation.
     * BASIC - validation of configuration for connection
     * FULL - validation of configuration for connection and basic elements for table
     */
    private Validation validation = Validation.FULL;

    void setValidationOnlyConnection() {
        this.validation = Validation.BASIC;
    }

    void setValidationFull() {
        this.validation = Validation.FULL;
    }

    // =======================================================================
    // DatabaseTableConfiguration
    // =======================================================================

    /**
     * How to quote a column in SQL statements. Possible values can be NONE, SINGLE, DOUBLE, BRACKETS, BACKSLASH
     */
    private String quoting = EMPTY_STR;

    /**
     * NameQoute getter
     *
     * @return quoting value
     */
    @ConfigurationProperty(order = 1,
            displayMessageKey = "QUOTING_DISPLAY",
            helpMessageKey = "QUOTING_HELP")
    public String getQuoting() {
        return this.quoting;
    }

    /**
     * NameQuote Setter
     */
    public void setQuoting(String value) {
        this.quoting = value;
    }

    /**
     * The host value
     */
    private String host = EMPTY_STR;

    /**
     * NameQoute getter
     *
     * @return quoting value
     */
    @ConfigurationProperty(order = 2,
            displayMessageKey = "HOST_DISPLAY",
            helpMessageKey = "HOST_HELP")
    public String getHost() {
        return this.host;
    }

    /**
     * NameQuote Setter
     */
    public void setHost(String value) {
        this.host = value;
    }

    /**
     * The port value
     */
    private String port = EMPTY_STR;

    /**
     * NameQoute getter
     *
     * @return quoting value
     */
    @ConfigurationProperty(order = 3,
            displayMessageKey = "PORT_DISPLAY",
            helpMessageKey = "PORT_HELP")
    public String getPort() {
        return this.port;
    }

    /**
     * NameQuote Setter
     */
    public void setPort(String value) {
        this.port = value;
    }

    /**
     * Database Login User name. This user name is used to connect to database. The provided user name and password
     * should have rights to insert/update/delete the rows in the configured identity holder table.
     * Required configuration property, and should be validated
     */
    private String user = EMPTY_STR;

    /**
     * @return user value
     */
    @ConfigurationProperty(order = 4,
            displayMessageKey = "USER_DISPLAY",
            helpMessageKey = "USER_HELP",
            operations = { DiscoverConfigurationOp.class })
    public String getUser() {
        return this.user;
    }

    public void setUser(String value) {
        this.user = value;
    }

    /**
     * Database access Password. This password is used to connect to database. The provided user name and password
     * should have rights to insert/update/delete the rows in the configured identity holder table.
     * Required configuration property, and should be validated
     */
    private GuardedString password;

    /**
     * @return password value
     */
    @ConfigurationProperty(order = 5, confidential = true,
            displayMessageKey = "PASSWORD_DISPLAY",
            helpMessageKey = "PASSWORD_HELP")
    public GuardedString getPassword() {
        return this.password;
    }

    public void setPassword(GuardedString value) {
        this.password = value;
    }

    /**
     * Database name.
     */
    private String database = EMPTY_STR;

    /**
     * @return user value
     */
    @ConfigurationProperty(order = 6,
            displayMessageKey = "DATABASE_DISPLAY",
            helpMessageKey = "DATABASE_HELP")
    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String value) {
        this.database = value;
    }

    /**
     * Database Table name. The name of the identity holder table (Integration table).
     */
    private String table = EMPTY_STR;

    /**
     * The table name
     *
     * @return the user account table name
     * Please notice, there are used non default message keys
     */
    @ConfigurationProperty(order = 7,
            displayMessageKey = "TABLE_DISPLAY",
            helpMessageKey = "TABLE_HELP")
    public String getTable() {
        return this.table;
    }

    /**
     * Table setter
     *
     * @param table name value
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Key Column, The name of the key column is required
     * This non empty value must be validated
     */
    private String keyColumn = EMPTY_STR;

    /**
     * Key Column getter
     *
     * @return keyColumn value
     */
    @ConfigurationProperty(order = 8,
            displayMessageKey = "KEY_COLUMN_DISPLAY",
            helpMessageKey = "KEY_COLUMN_HELP")
    public String getKeyColumn() {
        return this.keyColumn;
    }

    /**
     * Key Column setter
     *
     * @param keyColumn value
     */
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    /**
     * Password Column. If non empty, password is supported in the schema
     * empty password column means, the password is not supported and also should not be in the schema
     */
    private String passwordColumn = EMPTY_STR;

    /**
     * Password Column getter
     *
     * @return passwordColumn value
     */
    @ConfigurationProperty(order = 9,
            displayMessageKey = "PASSWORD_COLUMN_DISPLAY",
            helpMessageKey = "PASSWORD_COLUMN_HELP")
    public String getPasswordColumn() {
        return this.passwordColumn;
    }

    /**
     * Password Column setter
     */
    public void setPasswordColumn(String value) {
        this.passwordColumn = value;
    }

    /**
     * The Driver class. The jdbcDriver is located by connector framework to connect to database.
     * Required configuration property, and should be validated
     */
    private String jdbcDriver = EMPTY_STR;

    /**
     * @return jdbcDriver value
     */
    @ConfigurationProperty(order = 10,
            displayMessageKey = "JDBC_DRIVER_DISPLAY",
            helpMessageKey = "JDBC_DRIVER_HELP")
    public String getJdbcDriver() {
        return this.jdbcDriver;
    }

    public void setJdbcDriver(String value) {
        this.jdbcDriver = value;
    }

    /**
     * Database connection URL. The url is used to connect to database.
     * Required configuration property, and should be validated
     */
    private String jdbcUrlTemplate = EMPTY_STR;

    /**
     * Return the jdbcUrlTemplate
     *
     * @return url value
     */
    @ConfigurationProperty(order = 11,
            displayMessageKey = "URL_TEMPLATE_DISPLAY",
            helpMessageKey = "URL_TEMPLATE_HELP")
    public String getJdbcUrlTemplate() {
        if (StringUtil.isNotEmpty(jdbcUrlTemplate)) {
            return jdbcUrlTemplate;
        }

        String driver = getJdbcDriver();
        if ("oracle.jdbc.driver.OracleDriver".equals(driver) || "org.apache.derby.jdbc.EmbeddedDriver".equals(driver)) {
            return "jdbc:oracle:thin:@%h:%p:%d";
        } else if ("com.mysql.cj.jdbc.Driver".equals(driver) || "com.mysql.jdbc.Driver".equals(driver)) {
            return "jdbc:mysql://%h:%p/%d";
        } else if ("org.postgresql.Driver".equals(driver)) {
            return "jdbc:postgresql://%h:%p/%d";
        } else if ("com.microsoft.sqlserver.jdbc.SQLServerDriver".equals(driver)) {
            return "jdbc:sqlserver://%h:%p;databaseName=%d;";
        }

        return jdbcUrlTemplate;
    }

    public void setJdbcUrlTemplate(String value) {
        this.jdbcUrlTemplate = value;
    }

    /**
     * The empty string setting
     * allow conversion of a null into an empty string for not-null char columns
     */
    public boolean enableEmptyString = false;

    /**
     * Accessor for the enableEmptyString property
     *
     * @return the enableEmptyString
     */
    @ConfigurationProperty(order = 12,
            displayMessageKey = "ENABLE_EMPTY_STRING_DISPLAY",
            helpMessageKey = "ENABLE_EMPTY_STRING_HELP")
    public boolean isEnableEmptyString() {
        return enableEmptyString;
    }

    /**
     * Setter for the enableEmptyString property.
     *
     * @param enableEmptyString the enableEmptyString to set
     */
    public void setEnableEmptyString(boolean enableEmptyString) {
        this.enableEmptyString = enableEmptyString;
    }

    /**
     * Some database drivers will throw the SQLError when setting the
     * parameters to the statement with zero ErrorCode. This mean no error.
     * This switch allow to switch off ignoring this SQLError
     */
    public boolean rethrowAllSQLExceptions = true;

    /**
     * Accessor for the rethrowAllSQLExceptions property
     *
     * @return the rethrowAllSQLExceptions
     */
    @ConfigurationProperty(order = 14,
            displayMessageKey = "RETHROW_ALL_SQLEXCEPTIONS_DISPLAY",
            helpMessageKey = "RETHROW_ALL_SQLEXCEPTIONS_HELP")
    public boolean isRethrowAllSQLExceptions() {
        return rethrowAllSQLExceptions;
    }

    /**
     * Setter for the rethrowAllSQLExceptions property.
     *
     * @param rethrowAllSQLExceptions the rethrowAllSQLExceptions to set
     */
    public void setRethrowAllSQLExceptions(boolean rethrowAllSQLExceptions) {
        this.rethrowAllSQLExceptions = rethrowAllSQLExceptions;
    }

    /**
     * Some JDBC drivers (ex: Oracle) may not be able to get correct string representation of
     * TIMESTAMP data type of the column from the database table.
     * To get correct value , one needs to use rs.getTimestamp() rather rs.getString().
     */
    public boolean nativeTimestamps = false;

    /**
     * Accessor for the nativeTimestamps property
     *
     * @return the nativeTimestamps
     */
    @ConfigurationProperty(order = 15,
            displayMessageKey = "NATIVE_TIMESTAMPS_DISPLAY",
            helpMessageKey = "NATIVE_TIMESTAMPS_HELP")
    public boolean isNativeTimestamps() {
        return nativeTimestamps;
    }

    /**
     * Setter for the nativeTimestamps property.
     *
     * @param nativeTimestamps the nativeTimestamps to set
     */
    public void setNativeTimestamps(boolean nativeTimestamps) {
        this.nativeTimestamps = nativeTimestamps;
    }

    /**
     * Some JDBC drivers (ex: DerbyDB) may need to access all the datatypes with native types
     * to get correct value.
     */
    public boolean allNative = false;

    /**
     * Accessor for the allNativeproperty
     *
     * @return the allNative
     */
    @ConfigurationProperty(order = 16,
            displayMessageKey = "ALL_NATIVE_DISPLAY",
            helpMessageKey = "ALL_NATIVE_HELP")
    public boolean isAllNative() {
        return allNative;
    }

    /**
     * Setter for the allNative property.
     *
     * @param allNative the allNative to set
     */
    public void setAllNative(boolean allNative) {
        this.allNative = allNative;
    }

    /**
     * The new connection validation query. The query can be empty. Then the default driver's validate method
     * will be used with possible fallback to the auto commit true/false switch if driver doesn't support own
     * validate method.
     * Auto commit true/false switch can be insufficient on some database drivers because of caching.
     * Then the validation query is required.
     */
    private String validConnectionQuery;

    /**
     * connection validation query getter
     *
     * @return validConnectionQuery value
     */
    @ConfigurationProperty(order = 17,
            displayMessageKey = "VALID_CONNECTION_QUERY_DISPLAY",
            helpMessageKey = "VALID_CONNECTION_QUERY_HELP")
    public String getValidConnectionQuery() {
        return this.validConnectionQuery;
    }

    /**
     * Connection validation query setter
     */
    public void setValidConnectionQuery(String value) {
        this.validConnectionQuery = value;
    }

    /**
     * Timeout in seconds for connection validation using driver's own validation method.
     * It's used only if validConnectionQuery is empty.
     */
    private int validConnectionTimeout = DEFAULT_CONNECTION_VALIDATION_TIMEOUT;

    /**
     * connection validation timeout getter
     *
     * @return validConnectionTimeout value
     */
    @ConfigurationProperty(order = 18,
            displayMessageKey = "VALID_CONNECTION_TIMEOUT_DISPLAY",
            helpMessageKey = "VALID_CONNECTION_TIMEOUT_HELP")
    public int getValidConnectionTimeout() {
        return this.validConnectionTimeout;
    }

    /**
     * Connection validation timeout setter
     */
    public void setValidConnectionTimeout(int value) {
        this.validConnectionTimeout = value;
    }

    /**
     * Change Log Column (should automatically add ORDER BY)
     * If the value is non-empty, the SyncOp should be supported
     * It could be nativeTimestamps.
     */
    private String changeLogColumn = EMPTY_STR;

    /**
     * Log Column is required be SyncOp
     *
     * @return Log Column
     */
    @ConfigurationProperty(order = 20, operations = SyncOp.class,
            displayMessageKey = "CHANGE_LOG_COLUMN_DISPLAY",
            helpMessageKey = "CHANGE_LOG_COLUMN_HELP")
    public String getChangeLogColumn() {
        return this.changeLogColumn;
    }

    public void setChangeLogColumn(String value) {
        this.changeLogColumn = value;
    }

    /**
     * LiveSync Order Column (overrides implicit ORDER BY changeLogColumn)
     * If the value is empty, sync data are ordered by changeLogColumn
     */
    private String syncOrderColumn = EMPTY_STR;

    @ConfigurationProperty(order = 21, operations = SyncOp.class,
            displayMessageKey = "SYNC_ORDER_COLUMN_DISPLAY",
            helpMessageKey = "SYNC_ORDER_COLUMN_HELP")
    public String getSyncOrderColumn() {
        return this.syncOrderColumn;
    }

    public void setSyncOrderColumn(String value) {
        this.syncOrderColumn = value;
    }

    /**
     * LiveSync Order Asc
     * If the value is empty, sync data are ordered by ASC
     */
    private Boolean syncOrderAsc = true;

    @ConfigurationProperty(order = 22, operations = SyncOp.class,
            displayMessageKey = "SYNC_ORDER_ASC_DISPLAY",
            helpMessageKey = "SYNC_ORDER_ASC_HELP")
    public Boolean getSyncOrderAsc() {
        return this.syncOrderAsc;
    }

    public void setSyncOrderAsc(Boolean value) {
        this.syncOrderAsc = value;
    }

    // =======================================================================
    // DataSource
    // =======================================================================

    /**
     * The datasource name is used to connect to database.
     */
    private String datasource = EMPTY_STR;

    /**
     * Return the datasource
     *
     * @return datasource value
     */
    @ConfigurationProperty(order = 23,
            displayMessageKey = "DATASOURCE_DISPLAY",
            helpMessageKey = "DATASOURCE_HELP")
    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String value) {
        this.datasource = value;
    }

    /**
     * The jndiFactory name is used to connect to database.
     */
    private String[] jndiProperties;

    /**
     * Return the jndiFactory
     *
     * @return jndiFactory value
     */
    @ConfigurationProperty(order = 24,
            displayMessageKey = "JNDI_PROPERTIES_DISPLAY",
            helpMessageKey = "JNDI_PROPERTIES_HELP")
    public String[] getJndiProperties() {
        return jndiProperties;
    }

    public void setJndiProperties(String[] value) {
        this.jndiProperties = value;
    }

    private boolean suppressPassword = true;

    /**
     * If set to true then the password will not be returned. Never. Even though it is explicitly requested.
     * If set to false then the password will be returned if it is explicitly requested.
     */
    @ConfigurationProperty(order = 25,
            displayMessageKey = "SUPPRESS_PASSWORD_DISPLAY",
            helpMessageKey = "SUPPRESS_PASSWORD_HELP")
    public boolean getSuppressPassword() {
        return suppressPassword;
    }

    public void setSuppressPassword(boolean suppressPassword) {
        this.suppressPassword = suppressPassword;
    }

    private String alreadyExistMessages;

    @ConfigurationProperty(order = 26,
            displayMessageKey = "ALREADY_EXISTS_MESSAGES_DISPLAY",
            helpMessageKey = "ALREADY_EXISTS_MESSAGES_HELP")
    public String getAlreadyExistMessages() {
        return alreadyExistMessages;
    }

    public void setAlreadyExistMessages(String alreadyExistMessages) {
        this.alreadyExistMessages = alreadyExistMessages;
    }

    /**
     * SQLState based error handling
     * handling some sql exceptions based on the SQLState SQL Exception parameter
     */
    public boolean sqlStateExceptionHandling = false;

    @ConfigurationProperty(order = 27,
            displayMessageKey = "SQL_STATE_EXCEPTION_HANDLING_DISPLAY",
            helpMessageKey = "SQL_STATE_EXCEPTION_HANDLING_HELP")
    public boolean getSQLStateExceptionHandling() {
        return sqlStateExceptionHandling;
    }

    public void setSQLStateExceptionHandling(boolean sqlStateExceptionHandling) {
        this.sqlStateExceptionHandling = sqlStateExceptionHandling;
    }

    /**
     * SQLState based error handling AlreadyExist codes.
     * SQLState values which can be interpreted as codes for "alreadyExistException" error handling.
     */
    public String[] sqlStateAlreadyExists;

    @ConfigurationProperty(order = 28,
            displayMessageKey = "SQL_STATE_ALREADY_EXIST_DISPLAY",
            helpMessageKey = "SQL_STATE_ALREADY_EXIST_HELP")
    public String[] getSQLStateAlreadyExists() {
        return sqlStateAlreadyExists;
    }

    public void setSQLStateAlreadyExists(String[] sqlStateAlreadyExists) {
        this.sqlStateAlreadyExists = sqlStateAlreadyExists;
    }

    /**
     * SQLState based error handling ConnectionFailed codes.
     * SQLState values which can be interpreted as codes for "ConnectionFailedException" error handling.
     */
    public String[] sqlStateConnectionFailed;

    @ConfigurationProperty(order = 29,
            displayMessageKey = "SQL_STATE_CONNECTION_FAILED_DISPLAY",
            helpMessageKey = "SQL_STATE_CONNECTION_FAILED_HELP")
    public String[] getSQLStateConnectionFailed() {
        return sqlStateConnectionFailed;
    }

    public void setSQLStateConnectionFailed(String[] sqlStateConnectionFailed) {
        this.sqlStateConnectionFailed = sqlStateConnectionFailed;
    }

    /**
     * SQLState based error handling Invalid Attribute Value codes.
     * SQLState values which can be interpreted as codes for "InvalidAttributeValue" exception error handling.
     */
    public String[] sqlStateInvalidAttributeValue;

    @ConfigurationProperty(order = 30,
            displayMessageKey = "SQL_STATE_INVALID_ATTRIBUTE_VALUE_DISPLAY",
            helpMessageKey = "SQL_STATE_INVALID_ATTRIBUTE_VALUE_HELP")
    public String[] getSQLStateInvalidAttributeValue() {
        return sqlStateInvalidAttributeValue;
    }

    public void setSQLStateInvalidAttributeValue(String[] sqlStateInvalidAttributeValue) {
        this.sqlStateInvalidAttributeValue = sqlStateInvalidAttributeValue;
    }

    /**
     * SQLState based error handling Configuration Exception codes.
     * SQLState values which can be interpreted as codes for "ConfigurationException" exception error handling.
     */
    public String[] sqlStateConfigurationException;

    @ConfigurationProperty(order = 31,
            displayMessageKey = "SQL_STATE_CONFIGURATION_EXCEPTION_DISPLAY",
            helpMessageKey = "SQL_STATE_CONFIGURATION_EXCEPTION_HELP")
    public String[] getSQLStateConfigurationException() {
        return sqlStateConfigurationException;
    }

    public void setSQLStateConfigurationException(String[] sqlStateConfigurationException) {
        this.sqlStateConfigurationException = sqlStateConfigurationException;
    }

    /**
     * Column which describes last login date. If empty, the last login date is not supported.
     */
    private String lastLoginDateColumn = EMPTY_STR;

    @ConfigurationProperty(order = 32,
            displayMessageKey = "LAST_LOGIN_DATE_COLUMN_DISPLAY",
            helpMessageKey = "LAST_LOGIN_DATE_COLUMN_HELP")
    public String getLastLoginDateColumn() {
        return lastLoginDateColumn;
    }

    public void setLastLoginDateColumn(String lastLoginDateColumn) {
        this.lastLoginDateColumn = lastLoginDateColumn;
    }

    // =======================================================================
    // Configuration Interface
    // =======================================================================

    /**
     * Attempt to validate the arguments added to the Configuration.
     */
    @Override
    public void validate() {
        log.info("Validate DatabaseTableConfiguration");
        // check the url is configured
        if (StringUtil.isBlank(getJdbcUrlTemplate())) {
            throw new IllegalArgumentException(getMessage(MSG_JDBC_TEMPLATE_BLANK));
        }
        // check that there is not a datasource
        if (StringUtil.isBlank(getDatasource())) {
            log.info("Validate driver configuration.");

            // determine if you can get a connection to the database..
            if (getUser() == null) {
                throw new IllegalArgumentException(getMessage(MSG_USER_BLANK));
            }
            // check that there is a pwd to query..
            if (getPassword() == null) {
                throw new IllegalArgumentException(getMessage(MSG_PASSWORD_BLANK));
            }

            // host required
            if (getJdbcUrlTemplate().contains("%h")) {
                if (StringUtil.isBlank(getHost())) {
                    throw new IllegalArgumentException(getMessage(MSG_HOST_BLANK));
                }
            }
            // port required
            if (getJdbcUrlTemplate().contains("%p")) {
                if (StringUtil.isBlank(getPort())) {
                    throw new IllegalArgumentException(getMessage(MSG_PORT_BLANK));
                }
            }
            // database required            
            if (getJdbcUrlTemplate().contains("%d")) {
                if (StringUtil.isBlank(getDatabase())) {
                    throw new IllegalArgumentException(getMessage(MSG_DATABASE_BLANK));
                }
            }
            // make sure the jdbcDriver is in the class path..
            if (StringUtil.isBlank(getJdbcDriver())) {
                throw new IllegalArgumentException(getMessage(MSG_JDBC_DRIVER_BLANK));
            }
            try {
                Class.forName(getJdbcDriver());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(getMessage(MSG_JDBC_DRIVER_NOT_FOUND));
            }
            log.ok("driver configuration is ok");
        } else {
            log.info("Validate datasource configuration");
            //Validate the JNDI properties
            JNDIUtil.arrayToHashtable(getJndiProperties(), getConnectorMessages());
            log.ok("datasource configuration is ok");
        }

        if (Validation.FULL.equals(validation)) {
            validateConfigurationForTable();
        }

        log.ok("Configuration is valid");
    }

    private void validateConfigurationForTable() {
        // check that there is a table to query.
        if (StringUtil.isBlank(getTable())) {
            throw new IllegalArgumentException(getMessage(MSG_TABLE_BLANK));
        }
        // determine if you can get a key column
        if (StringUtil.isBlank(getKeyColumn())) {
            throw new IllegalArgumentException(getMessage(MSG_KEY_COLUMN_BLANK));
        } else {
            if (getKeyColumn().equalsIgnoreCase(getChangeLogColumn())) {
                throw new IllegalArgumentException(getMessage(MSG_KEY_COLUMN_EQ_CHANGE_LOG_COLUMN));
            }
        }
        // key column, password column
        if (StringUtil.isNotBlank(getPasswordColumn())) {
            if (getPasswordColumn().equalsIgnoreCase(getKeyColumn())) {
                throw new IllegalArgumentException(getMessage(MSG_PASSWD_COLUMN_EQ_KEY_COLUMN));
            }

            if (getPasswordColumn().equalsIgnoreCase(getChangeLogColumn())) {
                throw new IllegalArgumentException(getMessage(MSG_PASSWD_COLUMN_EQ_CHANGE_LOG_COLUMN));
            }
        }

        try {
            DatabaseTableSQLUtil.quoteName(getQuoting(), "test");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(getMessage(MSG_INVALID_QUOTING, getQuoting()));
        }
    }

    /**
     * Format a URL given a template. Recognized template characters are:
     * % literal % h host p port d database
     *
     * @return the database url
     */
    public String formatUrlTemplate() {
        log.info("format UrlTemplate");
        final StringBuilder sb = new StringBuilder();
        final String url = getJdbcUrlTemplate();
        final int len = url.length();
        for (int i = 0; i < len; i++) {
            char ch = url.charAt(i);
            if (ch != '%') {
                sb.append(ch);
            } else if (i + 1 < len) {
                i++;
                ch = url.charAt(i);
                if (ch == '%') {
                    sb.append(ch);
                } else if (ch == 'h') {
                    sb.append(getHost());
                } else if (ch == 'p') {
                    sb.append(getPort());
                } else if (ch == 'd') {
                    sb.append(getDatabase());
                }
            }
        }
        String formattedURL = sb.toString();
        log.ok("UrlTemplate is formatted to {0}", formattedURL);
        return formattedURL;
    }

    /**
     * Format the connector message
     *
     * @param key key of the message
     * @return return the formatted message
     */
    public String getMessage(String key) {
        final String fmt = getConnectorMessages().format(key, key);
        log.ok("Get for a key {0} connector message {1}", key, fmt);
        return fmt;
    }

    /**
     * Format message with arguments
     *
     * @param key key of the message
     * @param args arguments
     * @return the localized message string
     */
    public String getMessage(String key, Object... args) {

        if (args != null) {

            final String fmt = getConnectorMessages().format(key, key, args);
            log.ok("Get for a key {0} connector message {1}", key, fmt);

            return fmt;
        } else {

            return getMessage(key);
        }
    }
}
