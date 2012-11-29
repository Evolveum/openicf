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

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

/**
 * Main implementation of the ScriptedJDBC Connector
 *
 * @author gael
 * @version 1.0
 * @since 1.0
 */
@ConnectorClass(displayNameKey = "ScriptedSQL", configurationClass = ScriptedSQLConfiguration.class)
public class ScriptedSQLConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp, SearchOp<Map>, SyncOp, TestOp, UpdateAttributeValuesOp, ScriptOnConnectorOp {

    /**
     * Setup logging for the {@link ScriptedSQLConnector}.
     */
    private static final Log log = Log.getLog(ScriptedSQLConnector.class);
    /**
     * Place holder for the Connection created in the init method
     */
    private ScriptedSQLConnection connection;
    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link ScriptedSQLConnector#init}.
     */
    private ScriptedSQLConfiguration config;
    private ScriptExecutorFactory factory = null;
    private ScriptExecutor createExecutor = null;
    private ScriptExecutor updateExecutor = null;
    private ScriptExecutor deleteExecutor = null;
    private ScriptExecutor searchExecutor = null;
    private ScriptExecutor syncExecutor = null;
    private ScriptExecutor runOnConnectorExecutor = null;
    private ScriptExecutor testExecutor = null;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.config;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init
     */
    public void init(Configuration cfg) {
        this.config = (ScriptedSQLConfiguration) cfg;
        this.connection = new ScriptedSQLConnection(this.config);
        this.factory = ScriptExecutorFactory.newInstance(this.config.getScriptingLanguage());

        // We need an executor for each and every script. At least, they'll get
        // evaluated and compiled.
        // We privilege the script file over the script string
        // if script filename is null, then we use the script string

        createExecutor = getScriptExecutor(config.getCreateScript(), config.getCreateScriptFileName());
        log.ok("Create script loaded");

        updateExecutor = getScriptExecutor(config.getUpdateScript(), config.getUpdateScriptFileName());
        log.ok("Update script loaded");

        deleteExecutor = getScriptExecutor(config.getDeleteScript(), config.getDeleteScriptFileName());
        log.ok("Delete script loaded");

        searchExecutor = getScriptExecutor(config.getSearchScript(), config.getSearchScriptFileName());
        log.ok("Search script loaded");

        syncExecutor = getScriptExecutor(config.getSyncScript(), config.getSyncScriptFileName());
        log.ok("Sync script loaded");

        testExecutor = getScriptExecutor(config.getTestScript(), config.getTestScriptFileName());
        log.ok("Test script loaded");
    }

    /**
     * Disposes of the {@link ScriptedSQLConnector}'s resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
        config = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
    }

    public void checkAlive() {
        connection.test();
    }

    /**
     * SPI Operations
     */
    /**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass, final String username, final GuardedString password, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String username, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objClass, final Set<Attribute> attrs, final OperationOptions options) {
        if (config.isReloadScriptOnExecution()) {
            createExecutor = getScriptExecutor(config.getCreateScript(), config.getCreateScriptFileName());
            log.ok("Create script loaded");
        }
        if (createExecutor != null) {
            if (objClass == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
            }
            log.ok("Object class: {0}",objClass.getObjectClassValue());

            if (attrs == null || attrs.isEmpty()) {
                throw new IllegalArgumentException(config.getMessage("MSG_INVALID_ATTRIBUTE_SET"));
            }

            final Map<String, Object> arguments = new HashMap<String, Object>();

            arguments.put("connection", connection.getSqlConnection());
            arguments.put("action", "CREATE");
            arguments.put("log", log);
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("options", options.getOptions());
            // We give the id (name) as an argument, more friendly than dealing with __NAME__
            arguments.put("id", AttributeUtil.getNameFromAttributes(attrs).getNameValue());

            Map<String, List> attrMap = new HashMap();
            for (Attribute attr : attrs) {
                attrMap.put(attr.getName(), attr.getValue());
            }
            // let's get rid of __NAME__
            attrMap.remove("__NAME__");
            arguments.put("attributes", attrMap);

            // Password - if allowed we provide it in clear
            if (config.getClearTextPasswordToScript()) {
                GuardedString gpasswd = AttributeUtil.getPasswordValue(attrs);
                if (gpasswd != null) {
                    gpasswd.access(new Accessor() {

                        public void access(char[] clearChars) {
                            arguments.put("password", new String(clearChars));
                        }
                    });
                } else {
                    arguments.put("password", null);
                }
            }

            try {
                Object uidAfter = createExecutor.execute(arguments);
                if (uidAfter instanceof String) {
                    log.ok("{0} created",uidAfter);
                    return new Uid((String) uidAfter);
                } else {
                    throw new ConnectorException("Create script didn't return with the __UID__ value");
                }
            }
            catch (Exception e) {
                throw new ConnectorException("Create script error", e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        return genericUpdate("UPDATE", objClass, uid, replaceAttributes, options);
    }

    /**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        return genericUpdate("ADD_ATTRIBUTE_VALUES", objClass, uid, valuesToAdd, options);
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        return genericUpdate("REMOVE_ATTRIBUTE_VALUES", objClass, uid, valuesToRemove, options);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objClass, final Uid uid, final OperationOptions options) {
        if (config.isReloadScriptOnExecution()) {
            deleteExecutor = getScriptExecutor(config.getDeleteScript(), config.getDeleteScriptFileName());
            log.ok("Delete script loaded");
        }
        if (deleteExecutor != null) {
            if (objClass == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
            }
            log.ok("Object class: {0}",objClass.getObjectClassValue());

            if (uid == null || ( uid.getUidValue() == null )) {
                throw new IllegalArgumentException(config.getMessage("MSG_UID_BLANK"));
            }
            final String id = uid.getUidValue();

            final Map<String, Object> arguments = new HashMap<String, Object>();

            arguments.put("connection", connection.getSqlConnection());
            arguments.put("action", "DELETE");
            arguments.put("log", log);
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("uid", id);
            arguments.put("options", options.getOptions());

            try {
                deleteExecutor.execute(arguments);
                log.ok("{0} deleted",id);
            }
            catch (Exception e) {
                throw new ConnectorException("Delete script error", e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<Map> createFilterTranslator(ObjectClass objClass, OperationOptions options) {
        if (objClass == null) {
            throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
        }
        log.ok("ObjectClass: {0}",objClass.getObjectClassValue());
        return new ScriptedSQLFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objClass, Map query, ResultsHandler handler, OperationOptions options) {
        if (config.isReloadScriptOnExecution()) {
            searchExecutor = getScriptExecutor(config.getSearchScript(), config.getSearchScriptFileName());
            log.ok("Search script loaded");
        }

        if (searchExecutor != null) {
            if (objClass == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
            }
            log.ok("ObjectClass: {0}",objClass.getObjectClassValue());
            if (handler == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_RESULT_HANDLER_NULL"));
            }
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("connection", connection.getSqlConnection());
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("action", "SEARCH");
            arguments.put("log", log);
            arguments.put("options", options.getOptions());
            arguments.put("query", query);
            try {
                List<Map> results = (List<Map>) searchExecutor.execute(arguments);
                log.ok("Search ok");
                processResults(objClass, results, handler);
            }
            catch (Exception e) {
                throw new ConnectorException("Search script error", e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
        if (config.isReloadScriptOnExecution()) {
            syncExecutor = getScriptExecutor(config.getSyncScript(), config.getSyncScriptFileName());
            log.ok("Sync script loaded");
        }

        if (syncExecutor != null) {
            if (objClass == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
            }
            log.ok("ObjectClass: {0}",objClass.getObjectClassValue());
            if (handler == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_RESULT_HANDLER_NULL"));
            }
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("connection", connection.getSqlConnection());
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("action", "SYNC");
            arguments.put("log", log);
            arguments.put("options", options.getOptions());
            arguments.put("token", token.getValue());
            try {
                List<Map> results = (List<Map>) syncExecutor.execute(arguments);
                log.ok("Sync ok");
                processDeltas(objClass, results, handler);
            }
            catch (Exception e) {
                throw new ConnectorException("Sync script error", e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass objClass) {
        if (config.isReloadScriptOnExecution()) {
            syncExecutor = getScriptExecutor(config.getSyncScript(), config.getSyncScriptFileName());
            log.ok("Sync script loaded");
        }
        if (syncExecutor != null) {
            SyncToken st = null;
            if (objClass == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
            }
            log.ok("ObjectClass: {0}",objClass.getObjectClassValue());

            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("connection", connection.getSqlConnection());
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("action", "GET_LATEST_SYNC_TOKEN");
            arguments.put("log", log);
            try {
                // We expect the script to return a value (or null) that makes the sync token
                // !! result has to be one of the framework known types...
                Object result = syncExecutor.execute(arguments);
                log.ok("GetLatestSyncToken ok");
                FrameworkUtil.checkAttributeType(result.getClass());
                st = new SyncToken(result);
            }
            catch (java.lang.IllegalArgumentException ae) {
                throw new ConnectorException("Unknown Token type", ae);
            }
            catch (Exception e) {
                throw new ConnectorException("Sync (GetLatestSyncToken) script error", e);
            }
            return st;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        Object result = null;
        try {
            if (request.getScriptText() != null && request.getScriptText().length() > 0) {
                assert request.getScriptLanguage().equalsIgnoreCase(config.getScriptingLanguage());
                runOnConnectorExecutor = factory.newScriptExecutor(getClass().getClassLoader(), request.getScriptText(), true);
            }
        }
        catch (Exception e) {
            throw new ConnectorException("RunOnConnector script parse error", e);
        }
        if (runOnConnectorExecutor != null) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("connection", connection.getSqlConnection());
            arguments.put("action", "RUNSCRIPTONCONNECTOR");
            arguments.put("log", log);
            arguments.put("options", options.getOptions());
            arguments.put("scriptsArguments", request.getScriptArguments());
            try {
                // We return any object from the script
                result = runOnConnectorExecutor.execute(arguments);
                log.ok("runOnConnector script ok");
            }
            catch (Exception e) {
                throw new ConnectorException("runOnConnector script error", e);
            }
            finally {
                // clean up.. ??? should we close?
            }
            return result;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        config.validate();

        if (config.isReloadScriptOnExecution()) {
            testExecutor = getScriptExecutor(config.getTestScript(), config.getTestScriptFileName());
            log.ok("Test script loaded");
        }

        if (testExecutor != null) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("connection", connection.getSqlConnection());
            arguments.put("action", "TEST");
            arguments.put("log", log);
            try {
                testExecutor.execute(arguments);
                log.ok("Test ok");
            }
            catch (Exception e) {
                throw new ConnectorException("Test script error", e);
            }
        }
    }

    /*
     * Private methods
     */
    private void processResults(ObjectClass objClass, List<Map> results, ResultsHandler handler) {

        // Let's iterate over the results:
        for (Map<String, Object> result : results) {
            ConnectorObjectBuilder cobld = new ConnectorObjectBuilder();
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                final String attrName = entry.getKey();
                final Object attrValue = entry.getValue();
                // Special first
                if (attrName.equalsIgnoreCase("__UID__")) {
                    if (attrValue == null) {
                        throw new IllegalArgumentException("Uid cannot be null");
                    }
                    cobld.setUid(attrValue.toString());
                } else if (attrName.equalsIgnoreCase("__NAME__")) {
                    if (attrValue == null) {
                        throw new IllegalArgumentException("Name cannot be null");
                    }
                    cobld.setName(attrValue.toString());
                } else if (attrName.equalsIgnoreCase("password")) {
                    // is there a chance we fetch password from search?
                } else {
                    if (attrValue instanceof Collection) {
                        cobld.addAttribute(AttributeBuilder.build(attrName, (Collection)attrValue));
                    } else if (attrValue != null) {
                        cobld.addAttribute(AttributeBuilder.build(attrName, attrValue));
                    } else {
                        cobld.addAttribute(AttributeBuilder.build(attrName));
                    }
                }
            }
            cobld.setObjectClass(objClass);
            handler.handle(cobld.build());
            log.ok("ConnectorObject is built");
        }
    }

    private void processDeltas(ObjectClass objClass, List<Map> results, SyncResultsHandler handler) {

        // Let's iterate over the results:
        for (Map<String, Object> result : results) {
            // The Map should look like:
            // token: <Object> token
            // operation: <String> CREATE_OR_UPDATE|DELETE (defaults to CREATE_OR_UPDATE)
            // uid: <String> uid
            // previousUid: <String> prevuid (This is for rename ops)
            // password: <String> password
            // attributes: <Map> of attributes <String>name/<List>values
            SyncDeltaBuilder syncbld = new SyncDeltaBuilder();
            String uid = (String) result.get("uid");
            if (uid != null && !uid.isEmpty()) {
                syncbld.setUid(new Uid(uid));
                Object token = result.get("token");
                // Null token, set some acceptable value
                if (token == null) {
                    log.ok("token value is null, replacing to 0L");
                    token = 0L;
                }
                syncbld.setToken(new SyncToken(token));

                // Start building the connector object
                ConnectorObjectBuilder cobld = new ConnectorObjectBuilder();
                cobld.setName(uid);
                cobld.setUid(uid);
                cobld.setObjectClass(objClass);

                // operation
                // We assume that if DELETE, then we don't need to care about the rest
                String op = (String) result.get("operation");
                if (op != null && op.equalsIgnoreCase("DELETE")) {
                    syncbld.setDeltaType(SyncDeltaType.DELETE);

                } else {
                    // we assume this is CREATE_OR_UPDATE
                    syncbld.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);

                    // previous UID
                    String prevUid = (String) result.get("previousUid");
                    if (prevUid != null && !prevUid.isEmpty()) {
                        syncbld.setPreviousUid(new Uid(prevUid));
                    }

                    // password? is password valid if empty string? let's assume yes...
                    if (result.get("password") != null) {
                        cobld.addAttribute(AttributeBuilder.buildCurrentPassword(( (String) result.get("password") ).toCharArray()));
                    }

                    // Remaining attributes
                    for (Map.Entry<String, List> attr : ( (Map<String, List>) result.get("attributes") ).entrySet()) {
                        final String attrName = attr.getKey();
                        final Object attrValue = attr.getValue();
                        if (attrValue instanceof Collection) {
                            cobld.addAttribute(AttributeBuilder.build(attrName, (Collection)attrValue));
                        } else if (attrValue != null) {
                            cobld.addAttribute(AttributeBuilder.build(attrName, attrValue));
                        } else {
                            cobld.addAttribute(AttributeBuilder.build(attrName));
                        }
                    }
                }
                syncbld.setObject(cobld.build());
                if (!handler.handle(syncbld.build())) {
                    log.ok("Stop processing of the sync result set");
                    break;
                }
            } else {
                // we have a null uid... mmmm....
            }
        }
    }

    private Uid genericUpdate(String method, ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        if (config.isReloadScriptOnExecution()) {
            updateExecutor = getScriptExecutor(config.getUpdateScript(), config.getUpdateScriptFileName());
            log.ok("Update ({0}) script loaded",method);
        }
        if (updateExecutor != null) {

            if (objClass == null) {
                throw new IllegalArgumentException(config.getMessage("MSG_ACCOUNT_OBJECT_CLASS_REQUIRED"));
            }
            log.ok("Object class: {0}",objClass.getObjectClassValue());

            if (attrs == null || attrs.isEmpty()) {
                throw new IllegalArgumentException(config.getMessage("MSG_INVALID_ATTRIBUTE_SET"));
            }

            if (uid == null || ( uid.getUidValue() == null )) {
                throw new IllegalArgumentException(config.getMessage("MSG_UID_BLANK"));
            }
            final String id = uid.getUidValue();

            final Map<String, Object> arguments = new HashMap<String, Object>();

            arguments.put("connection", connection.getSqlConnection());
            arguments.put("action", method);
            arguments.put("log", log);
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("uid", id);
            arguments.put("options", options.getOptions());

            Map<String, List> attrMap = new HashMap();
            for (Attribute attr : attrs) {
                if (OperationalAttributes.isOperationalAttribute(attr)) {
                    if (method.equalsIgnoreCase("UPDATE")) {
                        attrMap.put(attr.getName(), attr.getValue());
                    }
                } else {
                    attrMap.put(attr.getName(), attr.getValue());
                }
            }
            arguments.put("attributes", attrMap);

            // Do we need to update the password?
            if (config.getClearTextPasswordToScript() && method.equalsIgnoreCase("UPDATE")) {
                GuardedString gpasswd = AttributeUtil.getPasswordValue(attrs);
                if (gpasswd != null) {
                    gpasswd.access(new Accessor() {

                        public void access(char[] clearChars) {
                            arguments.put("password", new String(clearChars));
                        }
                    });
                } else {
                    arguments.put("password", null);
                }
            }
            try {
                Object uidAfter = updateExecutor.execute(arguments);
                if (uidAfter instanceof String) {
                    log.ok("{0} updated ({1})",uidAfter,method );
                    return new Uid((String) uidAfter);
                }
            }
            catch (Exception e) {
                throw new ConnectorException("Update(" + method + ") script error", e);
            }
            throw new ConnectorException("Update script didn't return with the __UID__ value");
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private String readFile(String filename) {
        File file = new File(filename);
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = null;
        String text;

        try {
            reader = new BufferedReader(new FileReader(file));
            while (( text = reader.readLine() ) != null) {
                contents.append(text).append(System.getProperty(
                        "line.separator"));
            }
        }
        catch (FileNotFoundException e) {
            throw new ConnectorException(filename + " not found", e);
        }
        catch (IOException e) {
            throw new ConnectorException(filename, e);
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e) {
                throw new ConnectorException(filename, e);
            }
        }
        return contents.toString();
    }

    private ScriptExecutor getScriptExecutor(String script, String scriptFileName) {
        String scriptCode = script;
        ScriptExecutor scriptExec = null;

        try {
            if (scriptFileName != null) {
                scriptCode = readFile(scriptFileName);
            }
            if (scriptCode.length() > 0) {
                scriptExec = factory.newScriptExecutor(getClass().getClassLoader(), scriptCode, true);
            }
        }
        catch (Exception e) {
            throw new ConnectorException("Script error", e);
        }
        return scriptExec;
    }
}
