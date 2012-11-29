/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
package org.forgerock.openicf.connectors.scriptedsql;

import java.io.File;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the ScriptedJDBC Connector.
 *
 * @author gael
 * @version 1.0
 * @since 1.0
 */
public class ScriptedSQLConfiguration extends AbstractConfiguration {

    /**
     * Setup logging for the {@link ScriptedSQLConfiguration}.
     */
    static Log log = Log.getLog(ScriptedSQLConfiguration.class);
    public static final String EMPTY_STR = "";

    /**
     * Constructor
     */
    public ScriptedSQLConfiguration() {
    }
    // =======================================================================
    // DatabaseTableConfiguration
    // =======================================================================
    /**
     * How to quote a column in SQL statements. Possible values can be NONE,
     * SINGLE, DOUBLE, BRACKETS, BACKSLASH
     */
    private String quoting = EMPTY_STR;  // GAEL check with DBTAble connector constantsEMPTY_STR;

    /**
     * NameQuote getter
     *
     * @return quoting value
     */
    @ConfigurationProperty(displayMessageKey = "QUOTING_DISPLAY", helpMessageKey = "QUOTING_HELP")
    public String getQuoting() {
        return this.quoting;
    }

    /**
     * NameQuote Setter
     *
     * @param value
     */
    public void setQuoting(String value) {
        this.quoting = value;
    }
    /**
     * The host value
     */
    private String host = "localhost";

    /**
     * host getter
     *
     * @return host value
     */
    @ConfigurationProperty(order = 2, displayMessageKey = "HOST_DISPLAY", helpMessageKey = "HOST_HELP")
    public String getHost() {
        return this.host;
    }

    /**
     * host Setter
     *
     * @param value
     */
    public void setHost(String value) {
        this.host = value;
    }
    /**
     * The port value
     */
    private String port = "3306";

    /**
     * port getter
     *
     * @return port value
     */
    @ConfigurationProperty(order = 3, displayMessageKey = "PORT_DISPLAY", helpMessageKey = "PORT_HELP")
    public String getPort() {
        return this.port;
    }

    /**
     * port Setter
     *
     * @param value
     */
    public void setPort(String value) {
        this.port = value;
    }
    /**
     * Database Login User name. This user name is used to connect to database.
     * The provided user name and password should have rights to
     * insert/update/delete the rows in the configured identity holder table.
     * Required configuration property, and should be validated
     */
    private String user = EMPTY_STR;

    /**
     * @return user value
     */
    @ConfigurationProperty(order = 4, displayMessageKey = "USER_DISPLAY", helpMessageKey = "USER_HELP")
    public String getUser() {
        return this.user;
    }

    /**
     * @param value
     */
    public void setUser(String value) {
        this.user = value;
    }
    /**
     * Database access Password. This password is used to connect to database.
     * The provided user name and password should have rights to
     * insert/update/delete the rows in the configured identity holder table.
     * Required configuration property, and should be validated
     */
    private GuardedString password;

    /**
     * @return password value
     */
    @ConfigurationProperty(order = 5, confidential = true, displayMessageKey = "PASSWORD_DISPLAY", helpMessageKey = "PASSWORD_HELP")
    public GuardedString getPassword() {
        return this.password;
    }

    /**
     * @param value
     */
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
    @ConfigurationProperty(order = 6, displayMessageKey = "DATABASE_DISPLAY", helpMessageKey = "DATABASE_HELP")
    public String getDatabase() {
        return this.database;
    }

    /**
     * @param value
     */
    public void setDatabase(String value) {
        this.database = value;
    }
    /**
     * The Driver class. The jdbcDriver is located by connector framework to
     * connect to database. Required configuration property, and should be
     * validated. 
     * Oracle: oracle.jdbc.driver.OracleDriver 
     * MySQL: com.mysql.jdbc.Driver db2: COM.ibm.db2.jdbc.net.DB2Driver 
     * MSSQL: com.microsoft.sqlserver.jdbc.SQLServerDriver 
     * Sybase: com.sybase.jdbc2.jdbc.SybDriver 
     * Derby: org.apache.derby.jdbc.ClientDriver
     * Derby embedded: org.apache.derby.jdbc.EmbeddedDriver
     *
     */
    private String jdbcDriver = "com.mysql.jdbc.Driver";

    /**
     * @return jdbcDriver value
     */
    @ConfigurationProperty(order = 10, displayMessageKey = "JDBC_DRIVER_DISPLAY", helpMessageKey = "JDBC_DRIVER_HELP")
    public String getJdbcDriver() {
        return this.jdbcDriver;
    }

    /**
     * @param value
     */
    public void setJdbcDriver(String value) {
        this.jdbcDriver = value;
    }
    /**
     * Database connection URL. The url is used to connect to database. Required
     * configuration property, and should be validated. 
     * Oracle: jdbc:oracle:thin:@%h:%p:%d MySQL jdbc:mysql://%h:%p/%d 
     * Sybase: jdbc:sybase:Tds:%h:%p/%d DB2 jdbc:db2://%h:%p/%d 
     * SQL: jdbc:sqlserver://%h:%p;DatabaseName=%d 
     * Derby: jdbc:derby://%h:%p/%d 
     * Derby: embedded jdbc:derby:%d
     */
    private String jdbcUrlTemplate = "jdbc:mysql://%h:%p/%d";

    /**
     * Return the jdbcUrlTemplate
     *
     * @return url value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "URL_TEMPLATE_DISPLAY", helpMessageKey = "URL_TEMPLATE_HELP")
    public String getJdbcUrlTemplate() {
        return jdbcUrlTemplate;
    }

    /**
     * @param value
     */
    public void setJdbcUrlTemplate(String value) {
        this.jdbcUrlTemplate = value;
    }
    /**
     * With autoCommit set to true, each SQL statement is treated as a
     * transaction. It is automatically committed right after it is executed.
     */
    private boolean autoCommit = true;

    /**
     * Accessor for the autoCommit property
     *
     * @return the autoCommit
     */
    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * Setter for the autoCommit property.
     *
     * @param autoCommit
     */
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }
    /**
     * The empty string setting allow conversion of a null into an empty string
     * for not-null char columns
     */
    private boolean enableEmptyString = false;

    /**
     * Accessor for the enableEmptyString property
     *
     * @return the enableEmptyString
     */
    @ConfigurationProperty(order = 12, displayMessageKey = "ENABLE_EMPTY_STRING_DISPLAY", helpMessageKey = "ENABLE_EMPTY_STRING_HELP")
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
     * Some database drivers will throw the SQLError when setting the parameters
     * to the statement with zero ErrorCode. This mean no error. This switch
     * allow to switch off ignoring this SQLError
     */
    public boolean rethrowAllSQLExceptions = true;

    /**
     * Accessor for the rethrowAllSQLExceptions property
     *
     * @return the rethrowAllSQLExceptions
     */
    @ConfigurationProperty(order = 14, displayMessageKey = "RETHROW_ALL_SQLEXCEPTIONS_DISPLAY", helpMessageKey = "RETHROW_ALL_SQLEXCEPTIONS_HELP")
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
     * Some JDBC drivers (ex: Oracle) may not be able to get correct string
     * representation of TIMESTAMP data type of the column from the database
     * table. To get correct value , one needs to use rs.getTimestamp() rather
     * rs.getString().
     */
    public boolean nativeTimestamps = false;

    /**
     * Accessor for the nativeTimestamps property
     *
     * @return the nativeTimestamps
     */
    @ConfigurationProperty(order = 15, displayMessageKey = "NATIVE_TIMESTAMPS_DISPLAY", helpMessageKey = "NATIVE_TIMESTAMPS_HELP")
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
     * Some JDBC drivers (ex: DerbyDB) may need to access all the datatypes with
     * native types to get correct value.
     */
    public boolean allNative = false;

    /**
     * Accessor for the allNativeproperty
     *
     * @return the allNative
     */
    @ConfigurationProperty(order = 16, displayMessageKey = "ALL_NATIVE_DISPLAY", helpMessageKey = "ALL_NATIVE_HELP")
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
     * The new connection validation query. The query can be empty. Then the
     * auto commit true/false command is applied by default. This can be
     * insufficient on some database drivers because of caching Then the
     * validation query is required.
     */
    private String validConnectionQuery;

    /**
     * connection validation query getter
     *
     * @return validConnectionQuery value
     */
    @ConfigurationProperty(order = 17, displayMessageKey = "VALID_CONNECTION_QUERY_DISPLAY", helpMessageKey = "VALID_CONNECTION_QUERY_HELP")
    public String getValidConnectionQuery() {
        return this.validConnectionQuery;
    }

    /**
     * Connection validation query setter
     *
     * @param value
     */
    public void setValidConnectionQuery(String value) {
        this.validConnectionQuery = value;
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
    @ConfigurationProperty(order = 20, displayMessageKey = "DATASOURCE_DISPLAY", helpMessageKey = "DATASOURCE_HELP")
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
     *
     * @return jndiFactory value
     */
    @ConfigurationProperty(order = 21, displayMessageKey = "JNDI_PROPERTIES_DISPLAY", helpMessageKey = "JNDI_PROPERTIES_HELP")
    public String[] getJndiProperties() {
        return jndiProperties;
    }

    /**
     * @param value
     */
    public void setJndiProperties(String[] value) {
        this.jndiProperties = value;
    }
    // =======================================================================
    // Scripts
    // =======================================================================
    /**
     * Scripting language
     */
    private String scriptingLanguage = "GROOVY";

    /**
     * Return the scripting language string
     *
     * @return adapterCompat value
     */
    public String getScriptingLanguage() {
        return scriptingLanguage;
    }

    /**
     * Set the scripting language string
     *
     * @param value
     */
    public void setScriptingLanguage(String value) {
        this.scriptingLanguage = value;
    }
    /**
     * Should password be passed to scripts in clear text?
     */
    private boolean clearTextPasswordToScript = true;

    /**
     * Return the clearTextPasswordToScript boolean
     *
     * @return value
     */
    public boolean getClearTextPasswordToScript() {
        return clearTextPasswordToScript;
    }

    /**
     * Set the clearTextPasswordToScript value
     *
     * @param value
     */
    public void setClearTextPasswordToScript(boolean value) {
        this.clearTextPasswordToScript = value;
    }
    /**
     * By default, scripts are loaded and compiled when a connector instance
     * is created and initialized. Setting reloadScriptOnExecution to true will
     * make the connector load and compile the script every time it is called.
     * Use only for test/debug purpose since this can have a significant impact on performance.
     */
    private boolean reloadScriptOnExecution = false;

    /**
     * Accessor for the reloadScriptOnExecution property
     *
     * @return the autoCommit
     */
    public boolean isReloadScriptOnExecution() {
        return reloadScriptOnExecution;
    }

    /**
     * Setter for the reloadScriptOnExecution property.
     *
     * @param reloadScriptOnExecution
     */
    public void setReloadScriptOnExecution(boolean reloadScriptOnExecution) {
        this.reloadScriptOnExecution = reloadScriptOnExecution;
    }
    /**
     * Create script string
     */
    private String createScript = "";

    /**
     * Return the Create script string
     *
     * @return value
     */
    public String getCreateScript() {
        return createScript;
    }

    /**
     * Set the Create script string
     *
     * @param value
     */
    public void setCreateScript(String value) {
        this.createScript = value;
    }
    /**
     * Update script string
     */
    private String updateScript = "";

    /**
     * Return the Update script string
     *
     * @return updateScript value
     */
    public String getUpdateScript() {
        return updateScript;
    }

    /**
     * Set the Update script string
     *
     * @param value
     */
    public void setUpdateScript(String value) {
        this.updateScript = value;
    }
    /**
     * Delete script string
     */
    private String deleteScript = "";

    /**
     * Return the Delete script string
     *
     * @return deleteScript value
     */
    public String getDeleteScript() {
        return deleteScript;
    }

    /**
     * Set the Delete script string
     *
     * @param value
     */
    public void setDeleteScript(String value) {
        this.deleteScript = value;
    }
    /**
     * Search script string
     */
    private String searchScript = "";

    /**
     * Return the Search script string
     *
     * @return searchScript value
     */
    public String getSearchScript() {
        return searchScript;
    }

    /**
     * Set the Search script string
     *
     * @param value
     */
    public void setSearchScript(String value) {
        this.searchScript = value;
    }
    /**
     * Sync script string
     */
    private String syncScript = "";

    /**
     * Return the Sync script string
     *
     * @return syncScript value
     */
    public String getSyncScript() {
        return syncScript;
    }

    /**
     * Set the Sync script string
     *
     * @param value
     */
    public void setSyncScript(String value) {
        this.syncScript = value;
    }
    /**
     * Test script string
     */
    private String testScript = "";

    /**
     * Return the Test script string
     *
     * @return testScript value
     */
    public String getTestScript() {
        return testScript;
    }

    /**
     * Set the Test script string
     *
     * @param value
     */
    public void setTestScript(String value) {
        this.testScript = value;
    }
    /**
     * Create script filename
     */
    private String createScriptFileName = null;

    /**
     * Return the Create script FileName
     *
     * @return value
     */
    public String getCreateScriptFileName() {
        return createScriptFileName;
    }

    /**
     * Set the Create script FileName
     *
     * @param value
     */
    public void setCreateScriptFileName(String value) {
        this.createScriptFileName = value;
    }
    /**
     * Update script FileName
     */
    private String updateScriptFileName = null;

    /**
     * Return the Update script FileName
     *
     * @return updateScriptFileName value
     */
    public String getUpdateScriptFileName() {
        return updateScriptFileName;
    }

    /**
     * Set the Update script FileName
     *
     * @param value
     */
    public void setUpdateScriptFileName(String value) {
        this.updateScriptFileName = value;
    }
    /**
     * Delete script FileName
     */
    private String deleteScriptFileName = null;

    /**
     * Return the Delete script FileName
     *
     * @return deleteScriptFileName value
     */
    public String getDeleteScriptFileName() {
        return deleteScriptFileName;
    }

    /**
     * Set the Delete script FileName
     *
     * @param value
     */
    public void setDeleteScriptFileName(String value) {
        this.deleteScriptFileName = value;
    }
    /**
     * Search script FileName
     */
    private String searchScriptFileName = null;

    /**
     * Return the Search script FileName
     *
     * @return searchScriptFileName value
     */
    public String getSearchScriptFileName() {
        return searchScriptFileName;
    }

    /**
     * Set the Search script FileName
     *
     * @param value
     */
    public void setSearchScriptFileName(String value) {
        this.searchScriptFileName = value;
    }
    /**
     * Sync script FileName
     */
    private String syncScriptFileName = null;

    /**
     * Return the Sync script FileName
     *
     * @return syncScriptFileName value
     */
    public String getSyncScriptFileName() {
        return syncScriptFileName;
    }

    /**
     * Set the Sync script FileName
     *
     * @param value
     */
    public void setSyncScriptFileName(String value) {
        this.syncScriptFileName = value;
    }
    /**
     * Test script FileName
     */
    private String testScriptFileName = null;

    /**
     * Return the Test script FileName
     *
     * @return testScriptFileName value
     */
    public String getTestScriptFileName() {
        return testScriptFileName;
    }

    /**
     * Set the Test script FileName
     *
     * @param value
     */
    public void setTestScriptFileName(String value) {
        this.testScriptFileName = value;
    }

    // =======================================================================
    // Configuration Interface
    // =======================================================================
    /**
     * Attempt to validate the arguments added to the Configuration.
     */
    @Override
    public void validate() {
        log.info("Validate ScriptedSQLConfiguration");
        // check the url is configured
        if (StringUtil.isBlank(getJdbcUrlTemplate())) {
            throw new IllegalArgumentException(getMessage("MSG_JDBC_TEMPLATE_BLANK"));
        }
        // check that there is not a datasource
        if (StringUtil.isBlank(getDatasource())) {
            log.info("Validate driver configuration.");

            // determine if you can get a connection to the database..
            if (getUser() == null) {
                throw new IllegalArgumentException(getMessage("MSG_USER_BLANK"));
            }
            // check that there is a pwd to query..
            if (getPassword() == null) {
                throw new IllegalArgumentException(getMessage("MSG_PASSWORD_BLANK"));
            }
            // host required
            if (getJdbcUrlTemplate().contains("%h")) {
                if (StringUtil.isBlank(getHost())) {
                    throw new IllegalArgumentException(getMessage("MSG_HOST_BLANK"));
                }
            }
            // port required
            if (getJdbcUrlTemplate().contains("%p")) {
                if (StringUtil.isBlank(getPort())) {
                    throw new IllegalArgumentException(getMessage("MSG_PORT_BLANK"));
                }
            }
            // database required
            if (getJdbcUrlTemplate().contains("%d")) {
                if (StringUtil.isBlank(getDatabase())) {
                    throw new IllegalArgumentException(getMessage("MSG_DATABASE_BLANK"));
                }
            }
            // make sure the jdbcDriver is in the class path..
            if (StringUtil.isBlank(getJdbcDriver())) {
                throw new IllegalArgumentException(getMessage("MSG_JDBC_DRIVER_BLANK"));
            }
            try {
                Class.forName(getJdbcDriver());
            }
            catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(getMessage("MSG_JDBC_DRIVER_NOT_FOUND"));
            }
            log.ok("driver configuration is ok");
        } else {
            log.info("Validate datasource configuration");
            //Validate the JNDI properties
            JNDIUtil.arrayToHashtable(getJndiProperties(), getConnectorMessages());
            log.ok("datasource configuration is ok");
        }

        // Validate the actions

        log.info("Checking Create Script filename");
        checkFileIsReadable("Create", getCreateScriptFileName());
        log.info("Checking Update Script filename");
        checkFileIsReadable("Update", getUpdateScriptFileName());
        log.info("Checking Delete Script filename");
        checkFileIsReadable("Delete", getDeleteScriptFileName());
        log.info("Checking Search Script filename");
        checkFileIsReadable("Search", getSearchScriptFileName());
        log.info("Checking Sync Script filename");
        checkFileIsReadable("Sync", getSyncScriptFileName());
        log.info("Checking Test Script filename");
        checkFileIsReadable("Test", getTestScriptFileName());

        log.ok("Configuration is valid");
    }

    /**
     * Format a URL given a template. Recognized template characters are: %
     * literal % h host p port d database
     *
     * @return the database url
     */
    public String formatUrlTemplate() {
        log.info("format UrlTemplate");
        final StringBuffer b = new StringBuffer();
        final String url = getJdbcUrlTemplate();
        final int len = url.length();
        for (int i = 0; i < len; i++) {
            char ch = url.charAt(i);
            if (ch != '%') {
                b.append(ch);
            } else if (i + 1 < len) {
                i++;
                ch = url.charAt(i);
                if (ch == '%') {
                    b.append(ch);
                } else if (ch == 'h') {
                    b.append(getHost());
                } else if (ch == 'p') {
                    b.append(getPort());
                } else if (ch == 'd') {
                    b.append(getDatabase());
                }
            }
        }
        String formattedURL = b.toString();
        log.ok("UrlTemplate is formated to {0}", formattedURL);
        return formattedURL;
    }

    /**
     * Format the connector message
     *
     * @param key key of the message
     * @return return the formated message
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
     * @param objects arguments
     * @return the localized message string
     */
    public String getMessage(String key, Object... objects) {
        final String fmt = getConnectorMessages().format(key, key, objects);
        log.ok("Get for a key {0} connector message {1}", key, fmt);
        return fmt;
    }

    private void checkFileIsReadable(String type, String fileName) {
        if (fileName == null) {
            log.info("{0} Script Filename is null",type);
        } else {
            File f = new File(fileName);
            try {
                if (f.canRead()) {
                    log.ok("{0} is readable",fileName);
                } else {
                    throw new IllegalArgumentException("Can't read " + fileName);
                }
            }
            catch (SecurityException e) {
                throw new IllegalArgumentException("Can't read " + fileName);
            }
        }
    }
}
