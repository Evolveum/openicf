/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
package org.forgerock.openicf.connectors.sap;

import java.io.File;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the SAP Connector.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * @version $Revision$ $Date$
 */
public class SAPConfiguration extends AbstractConfiguration {

    /**
     * Setup logging for the {@link ScriptedSQLConfiguration}.
     */
    static Log log = Log.getLog(SAPConfiguration.class);

    // Exposed configuration properties.
    /**
     * Constructor
     */
    public SAPConfiguration() {
    }
    // ===================================
    // SAP Connection Config
    // ===================================
    /**
     * SAP ABAP application server.
     */
    private String host;

    @ConfigurationProperty(order = 1, displayMessageKey = "host.display",
    groupMessageKey = "basic.group", helpMessageKey = "host.help",
    required = true, confidential = false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
    /**
     * Logon user.
     */
    private String user = null;

    /**
     * Return the User string
     *
     * @return user value
     */
    @ConfigurationProperty(order = 2, displayMessageKey = "user.display",
    groupMessageKey = "basic.group", helpMessageKey = "user.help",
    required = true, confidential = false)
    public String getUser() {
        return user;
    }

    /**
     * Set the User string
     *
     * @param value
     */
    public void setUser(String value) {
        this.user = value;
    }
    /**
     * Logon password.
     */
    private GuardedString password = null;

    /**
     * Return the password string
     *
     * @return password value
     */
    @ConfigurationProperty(order = 3, displayMessageKey = "password.display",
    groupMessageKey = "basic.group", helpMessageKey = "password.help",
    required = true, confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    /**
     * Set the password string
     *
     * @param value
     */
    public void setPassword(GuardedString value) {
        this.password = value;
    }
    /**
     * SAP client, mandatory.
     */
    private String client = "000";

    /**
     * Return the SAP Client string
     *
     * @return client value
     */
    @ConfigurationProperty(order = 4, displayMessageKey = "client.display",
    groupMessageKey = "basic.group", helpMessageKey = "client.help",
    required = true, confidential = false)
    public String getClient() {
        return client;
    }

    /**
     * Set the SAP Client string
     *
     * @param value
     */
    public void setClient(String value) {
        this.client = value;
    }
    /**
     * System number of the SAP ABAP application server.
     */
    private String systemNumber = "00";

    /**
     * Return the systemNumber string
     *
     * @return systemNumber value
     */
    @ConfigurationProperty(order = 5, displayMessageKey = "systemNumber.display",
    groupMessageKey = "basic.group", helpMessageKey = "systemNumber.help",
    required = true, confidential = false)
    public String getSystemNumber() {
        return systemNumber;
    }

    /**
     * Set the systemNumber string
     *
     * @param value
     */
    public void setSystemNumber(String value) {
        this.systemNumber = value;
    }
    /**
     * Logon language.
     */
    private String language = "EN";

    /**
     * Return the language string
     *
     * @return language value
     */
    @ConfigurationProperty(order = 6, displayMessageKey = "language.display",
    groupMessageKey = "basic.group", helpMessageKey = "language.help",
    required = true, confidential = false)
    public String getLanguage() {
        return language;
    }

    /**
     * Set the language string
     *
     * @param value
     */
    public void setLanguage(String value) {
        this.language = value;
    }
    /**
     * Destination name
     */
    private String destination = "OPENIDM";

    /**
     * Return the destination string
     *
     * @return destination value
     */
    @ConfigurationProperty(order = 7, displayMessageKey = "destination.display",
    groupMessageKey = "basic.group", helpMessageKey = "destination.help",
    required = true, confidential = false)
    public String getDestination() {
        return destination;
    }

    /**
     * Set the destination string
     *
     * @param value
     */
    public void setDestination(String value) {
        this.destination = value;
    }
    /**
     * Defines if direct connection
     */
    private boolean directConnection = true;

    /**
     * Return the directConnection
     *
     * @return language value
     */
    @ConfigurationProperty(order = 8, displayMessageKey = "directConnection.display",
    groupMessageKey = "basic.group", helpMessageKey = "directConnection.help",
    required = true, confidential = false)
    public boolean isDirectConnection() {
        return directConnection;
    }

    /**
     * Set the directConnection string
     *
     * @param value
     */
    public void setDirectConnection(boolean value) {
        this.directConnection = value;
    }
    /**
     * SAP Router string for connection to systems behind a SAP Router.
     * (/H/<host>[/S/<port>])+
     */
    private String sapRouter = null;

    /**
     * Return the sapRouter string
     *
     * @return sapRouter value
     */
    @ConfigurationProperty(order = 9, displayMessageKey = "sapRouter.display",
    groupMessageKey = "basic.group", helpMessageKey = "sapRouter.help",
    required = true, confidential = false)
    public String getSapRouter() {
        return sapRouter;
    }

    /**
     * Set the sapRouter string
     *
     * @param value
     */
    public void setSapRouter(String value) {
        this.sapRouter = value;
    }
    /**
     * System ID of the SAP system.
     */
    private String r3Name = null;

    /**
     * Return the r3Name string
     *
     * @return r3Name value
     */
    @ConfigurationProperty(order = 10, displayMessageKey = "r3Name.display",
    groupMessageKey = "advanced.group", helpMessageKey = "r3Name.help",
    required = false, confidential = false)
    public String getR3Name() {
        return r3Name;
    }

    /**
     * Set the r3Name string
     *
     * @param value
     */
    public void setR3Name(String value) {
        this.r3Name = value;
    }
    /**
     * SAP message server.
     */
    private String msHost = null;

    /**
     * Return the msHost string
     *
     * @return msHost value
     */
    @ConfigurationProperty(order = 10, displayMessageKey = "msHost.display",
    groupMessageKey = "advanced.group", helpMessageKey = "msHost.help",
    required = false, confidential = false)
    public String getMsHost() {
        return msHost;
    }

    /**
     * Set the msHost string
     *
     * @param value
     */
    public void setMsHost(String value) {
        this.msHost = value;
    }
    /**
     * SAP message server port.
     */
    private String msServ = null;

    /**
     * Return the msServ string
     *
     * @return msServ value
     */
    @ConfigurationProperty(order = 10, displayMessageKey = "msServ.display",
    groupMessageKey = "advanced.group", helpMessageKey = "msServ.help",
    required = false, confidential = false)
    public String getMsServ() {
        return msServ;
    }

    /**
     * Set the msServ string
     *
     * @param value
     */
    public void setMsServ(String value) {
        this.msServ = value;
    }
    /**
     * Group of SAP application servers.
     */
    private String group = null;

    /**
     * Return the group string
     *
     * @return group value
     */
    @ConfigurationProperty(order = 10, displayMessageKey = "group.display",
    groupMessageKey = "advanced.group", helpMessageKey = "group.help",
    required = false, confidential = false)
    public String getGroup() {
        return group;
    }

    /**
     * Set the group string
     *
     * @param value
     */
    public void setGroup(String value) {
        this.group = value;
    }
    /**
     * X509 certificate for certificate based authentication.
     */
    private String x509Cert = null;

    /**
     * Return the x509Cert string
     *
     * @return x509Cert value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "x509.display",
    groupMessageKey = "sso.group", helpMessageKey = "x509.help",
    required = false, confidential = false)
    public String getX509Cert() {
        return x509Cert;
    }

    /**
     * Set the x509Cert string
     *
     * @param value
     */
    public void setX509Cert(String value) {
        this.x509Cert = value;
    }
    /**
     * SNC partner name.
     */
    private String sncPartnerName = null;

    /**
     * Return the sncPartnerName string
     *
     * @return sncPartnerName value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "sncPartnerName.display",
    groupMessageKey = "snc.group", helpMessageKey = "snvPartnerName.help",
    required = false, confidential = false)
    public String getSncPartnerName() {
        return sncPartnerName;
    }

    /**
     * Set the sncPartnerName string
     *
     * @param value
     */
    public void setSncPartnerName(String value) {
        this.sncPartnerName = value;
    }
    /**
     * SNC level of security, 1 to 9.
     */
    private String sncQoP = null;

    /**
     * Return the sncQoP string
     *
     * @return sncQoP value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "sncQoP.display",
    groupMessageKey = "snc.group", helpMessageKey = "sncQoP.help",
    required = false, confidential = false)
    public String getSncQoP() {
        return sncQoP;
    }

    /**
     * Set the sncQoP string
     *
     * @param value
     */
    public void setSncQoP(String value) {
        this.sncQoP = value;
    }
    /**
     * Own SNC name. Overrides environment settings.
     */
    private String sncMyName = null;

    /**
     * Return the sncMyName string
     *
     * @return sncMyName value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "sncMyName.display",
    groupMessageKey = "snc.group", helpMessageKey = "sncMyName.help",
    required = false, confidential = false)
    public String getSncMyName() {
        return sncMyName;
    }

    /**
     * Set the sncMyName string
     *
     * @param value
     */
    public void setSncMyName(String value) {
        this.sncMyName = value;
    }
    /**
     * Secure network connection (SNC) mode, 0 (off) or 1 (on)
     */
    private String sncMode = "0";

    /**
     * Return the sncMode string
     *
     * @return sncMode value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "sncMode.display",
    groupMessageKey = "snc.group", helpMessageKey = "sncMode.help",
    required = true, confidential = false)
    public String getSncMode() {
        return sncMode;
    }

    /**
     * Set the sncMode string
     *
     * @param value
     */
    public void setSncMode(String value) {
        this.sncMode = value;
    }
    /**
     * Use of the SSO behavior of SNC, 0 (off) or 1 (on).
     */
    private String sncSSO = "1";

    /**
     * Return the sncSSO string
     *
     * @return sncSSO value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "sncSSO.display",
    groupMessageKey = "snc.group", helpMessageKey = "sncSSO.help",
    required = false, confidential = false)
    public String getSncSSO() {
        return sncSSO;
    }

    /**
     * Set the sncSSO string
     *
     * @param value
     */
    public void setSncSSO(String value) {
        this.sncSSO = value;
    }
    /**
     * Path to library which provides SNC service.
     */
    private String sncLibrary = null;

    /**
     * Return the sncLibrary string
     *
     * @return sncLibrary value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "sncLibrary.display",
    groupMessageKey = "snc.group", helpMessageKey = "sncLibrary.help",
    required = false, confidential = false)
    public String getSncLibrary() {
        return sncLibrary;
    }

    /**
     * Set the sncLibrary string
     *
     * @param value
     */
    public void setSncLibrary(String value) {
        this.sncLibrary = value;
    }
    /**
     * Maximum number of active connections that can be created for a
     * destination simultaneously. The default is 0 (unlimited).
     */
    private String peakLimit = "0";

    /**
     * Return the peakLimit string
     *
     * @return peakLimit value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "peakLimit.display",
    groupMessageKey = "pool.group", helpMessageKey = "peakLimit.help",
    required = false, confidential = false)
    public String getPeakLimit() {
        return peakLimit;
    }

    /**
     * Set the peakLimit string
     *
     * @param value
     */
    public void setPeakLimit(String value) {
        this.peakLimit = value;
    }
    /**
     * Maximum number of idle connections kept open by the destination. 0 = no
     * connection pooling. Default is 1.
     */
    private String poolCapacity = "1";

    /**
     * Return the poolCapacity string
     *
     * @return poolCapacity value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "poolCapacity.display",
    groupMessageKey = "pool.group", helpMessageKey = "poolCapacity.help",
    required = false, confidential = false)
    public String getPoolCapacity() {
        return poolCapacity;
    }

    /**
     * Set the poolCapacity string
     *
     * @param value
     */
    public void setPoolCapacity(String value) {
        this.poolCapacity = value;
    }
    /**
     * Time in ms after that a free connection can be closed. Default is one
     * minute.
     */
    private String expirationTime = "60000";

    /**
     * Return the expirationTime string
     *
     * @return expirationTime value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "expirationTime.display",
    groupMessageKey = "pool.group", helpMessageKey = "expirationTime.help",
    required = false, confidential = false)
    public String getExpirationTime() {
        return expirationTime;
    }

    /**
     * Set the expirationTime string
     *
     * @param value
     */
    public void setExpirationTime(String value) {
        this.expirationTime = value;
    }
    /**
     * Period in ms after that the destination checks the released connections
     * for expiration. Default is one minute
     */
    private String expirationPeriod = "60000";

    /**
     * Return the expirationPeriod string
     *
     * @return expirationPeriod value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "expirationPeriod.display",
    groupMessageKey = "pool.group", helpMessageKey = "expirationPeriod.help",
    required = false, confidential = false)
    public String getExpirationPeriod() {
        return expirationPeriod;
    }

    /**
     * Set the expirationPeriod string
     *
     * @param value
     */
    public void setExpirationPeriod(String value) {
        this.expirationPeriod = value;
    }
    /**
     * Max time in ms to wait for a connection, if the max allowed number of
     * connections is allocated by the application. Default is 30 seconds.
     */
    private String maxGetTime = "30000";

    /**
     * Return the maxGetTime string
     *
     * @return maxGetTime value
     */
    @ConfigurationProperty(order = 11, displayMessageKey = "maxGetTime.display",
    groupMessageKey = "pool.group", helpMessageKey = "maxGetTime.help",
    required = false, confidential = false)
    public String getMaxGetTime() {
        return maxGetTime;
    }

    /**
     * Set the maxGetTime string
     *
     * @param value
     */
    public void setMaxGetTime(String value) {
        this.maxGetTime = value;
    }
    /**
     * Enable/disable CPIC trace [0..3].
     */
    private String cpicTrace = "0";

    /**
     * Return the cpicTrace string
     *
     * @return cpicTrace value
     */
    @ConfigurationProperty(order = 2, displayMessageKey = "cpicTrace.display",
    groupMessageKey = "trace.group", helpMessageKey = "cpicTrace.help",
    required = false, confidential = false)
    public String getCpicTrace() {
        return cpicTrace;
    }

    /**
     * Set the cpicTrace string
     *
     * @param value
     */
    public void setCpicTrace(String value) {
        this.cpicTrace = value;
    }
    /**
     * Enable/disable RFC trace (0 or 1).
     */
    private String trace = "0";

    /**
     * Return the trace string
     *
     * @return trace value
     */
    @ConfigurationProperty(order = 1, displayMessageKey = "trace.display",
    groupMessageKey = "trace.group", helpMessageKey = "trace.help",
    required = false, confidential = false)
    public String getTrace() {
        return trace;
    }

    /**
     * Set the trace string
     *
     * @param value
     */
    public void setTrace(String value) {
        this.trace = value;
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
    @ConfigurationProperty(order = 9, displayMessageKey = "scriptingLanguage.display",
    groupMessageKey = "script.group", helpMessageKey = "scriptingLanguage.help",
    required = false, confidential = false)
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
    @ConfigurationProperty(order = 11, displayMessageKey = "clearTextPasswordToScript.display",
    groupMessageKey = "script.group", helpMessageKey = "clearTextPasswordToScript.help",
    required = false, confidential = false)
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
     * By default, scripts are loaded and compiled when a connector instance is
     * created and initialized. Setting reloadScriptOnExecution to true will
     * make the connector load and compile the script every time it is called.
     * Use only for test/debug purpose since this can have a significant impact
     * on performance.
     */
    private boolean reloadScriptOnExecution = false;

    /**
     * Accessor for the reloadScriptOnExecution property
     *
     * @return the reloadScriptOnExecution
     */
    @ConfigurationProperty(order = 10, displayMessageKey = "reloadScriptOnExecution.display",
    groupMessageKey = "script.group", helpMessageKey = "reloadScriptOnExecution.help",
    required = false, confidential = false)
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
     * Create script filename
     */
    private String createScriptFileName = null;

    /**
     * Return the Create script FileName
     *
     * @return value
     */
    @ConfigurationProperty(order = 1, displayMessageKey = "createScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "createScriptFileName.help",
    required = false, confidential = false)
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
    @ConfigurationProperty(order = 2, displayMessageKey = "updateScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "updateScriptFileName.help",
    required = false, confidential = false)
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
    @ConfigurationProperty(order = 3, displayMessageKey = "deleteScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "deleteScriptFileName.help",
    required = false, confidential = false)
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
    @ConfigurationProperty(order = 4, displayMessageKey = "searchScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "searchScriptFileName.help",
    required = false, confidential = false)
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
     * SearchAll script FileName.
     */
    private String searchAllScriptFileName = null;

    /**
     * Return the SearchAll script FileName
     *
     * @return searchScriptFileName value
     */
    @ConfigurationProperty(order = 5, displayMessageKey = "searchAllScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "searchAllScriptFileName.help",
    required = false, confidential = false)
    public String getSearchAllScriptFileName() {
        return searchAllScriptFileName;
    }

    /**
     * Set the SearchAll script FileName
     *
     * @param value
     */
    public void setSearchAllScriptFileName(String value) {
        this.searchAllScriptFileName = value;
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
    @ConfigurationProperty(order = 6, displayMessageKey = "syncScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "syncScriptFileName.help",
    required = false, confidential = false)
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
     * Schema script FileName
     */
    private String schemaScriptFileName = null;

    /**
     * Return the Schema script FileName
     *
     * @return schemaScriptFileName value
     */
    @ConfigurationProperty(order = 7, displayMessageKey = "schemaScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "schemaScriptFileName.help",
    required = true, confidential = false)
    public String getSchemaScriptFileName() {
        return schemaScriptFileName;
    }

    /**
     * Set the Schema script FileName
     *
     * @param value
     */
    public void setSchemaScriptFileName(String value) {
        this.schemaScriptFileName = value;
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
    @ConfigurationProperty(order = 8, displayMessageKey = "testScriptFileName.display",
    groupMessageKey = "script.group", helpMessageKey = "testScriptFileName.help",
    required = true, confidential = false)
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

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(user)) {
            throw new IllegalArgumentException("Remote User cannot be null or empty.");
        }
        if (StringUtil.isBlank(host)) {
            throw new IllegalArgumentException("Host cannot be null or empty.");
        }
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
            log.info("{0} Script Filename is null", type);
        } else {
            File f = new File(fileName);
            try {
                if (f.canRead()) {
                    log.ok("{0} is readable", fileName);
                } else {
                    throw new IllegalArgumentException("Can't read " + fileName);
                }
            } catch (SecurityException e) {
                throw new IllegalArgumentException("Can't read " + fileName);
            }
        }
    }
}
