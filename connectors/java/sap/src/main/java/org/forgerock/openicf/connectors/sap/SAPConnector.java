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

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Main implementation of the SAP Connector
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * @version $Revision$ $Date$
 */
@ConnectorClass(displayNameKey = "SAP.connector.display", configurationClass = SAPConfiguration.class)
public class SAPConnector implements
        Connector, CreateOp, DeleteOp, SchemaOp, ScriptOnConnectorOp, SearchOp<Map>, SyncOp, TestOp, UpdateAttributeValuesOp, UpdateOp {

    /**
     * Setup logging for the {@link SAPConnector}.
     */
    private static final Log log = Log.getLog(SAPConnector.class);
    /**
     * Place holder for the Connection created in the init method
     */
    private SAPConnection connection;
    private Schema schema;
    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link SAPConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private SAPConfiguration configuration = null;
    private ScriptExecutorFactory factory = null;
    private ScriptExecutor createExecutor = null;
    private ScriptExecutor updateExecutor = null;
    private ScriptExecutor deleteExecutor = null;
    private ScriptExecutor searchExecutor = null;
    private ScriptExecutor searchAllExecutor = null;
    private ScriptExecutor syncExecutor = null;
    private ScriptExecutor runOnConnectorExecutor = null;
    private ScriptExecutor schemaExecutor = null;
    private ScriptExecutor testExecutor = null;

    /**
     * Gets the Configuration context for this connector.
     */
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see
     * org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    @Override
    public void init(Configuration configuration1) {
        this.configuration = (SAPConfiguration) configuration1;
        this.connection = new SAPConnection(this.configuration);
        this.factory = ScriptExecutorFactory.newInstance(this.configuration.getScriptingLanguage());

        // We need an executor for each and every script. At least, they'll get
        // evaluated and compiled.
        // This is a bit expensive.
        // TODO: lazy loading of scripts on demand.

        createExecutor = getScriptExecutor(configuration.getCreateScriptFileName());
        updateExecutor = getScriptExecutor(configuration.getUpdateScriptFileName());
        deleteExecutor = getScriptExecutor(configuration.getDeleteScriptFileName());
        searchExecutor = getScriptExecutor(configuration.getSearchScriptFileName());
        searchAllExecutor = getScriptExecutor(configuration.getSearchAllScriptFileName());
        syncExecutor = getScriptExecutor(configuration.getSyncScriptFileName());
        schemaExecutor = getScriptExecutor(configuration.getSchemaScriptFileName());
        testExecutor = getScriptExecutor(configuration.getTestScriptFileName());
    }

    /**
     * Disposes of the {@link SAPConnector}'s resources.
     *
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    @Override
    public void dispose() {
        configuration = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void checkAlive() {
//        connection.test();
//    }

    /**
     * ****************
     * SPI Operations
     *
     * Implement the following operations using the contract and description
     * found in the Javadoc for these methods.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public Schema schema() {
        SchemaBuilder scmb = new SchemaBuilder(SAPConnector.class);
        if (configuration.isReloadScriptOnExecution()) {
            schemaExecutor = getScriptExecutor(configuration.getSchemaScriptFileName());
        }
        if (schemaExecutor != null) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("action", "SCHEMA");
            arguments.put("log", log);
            arguments.put("builder", scmb);
            try {
                schemaExecutor.execute(arguments);
            } catch (Exception e) {
                throw new ConnectorException("Schema script error", e);
            }
        } else {
            throw new UnsupportedOperationException("SCHEMA script executor is null. Problem loading Schema script");
        }
        schema = scmb.build();
        return schema;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilterTranslator<Map> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        if (objectClass == null) {
            throw new IllegalArgumentException("ObjectClass required");
        }
        return new SAPFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeQuery(ObjectClass objectClass, Map query, ResultsHandler handler, OperationOptions options) {
        ScriptExecutor executor = null;
        String action = "SEARCH";
        if (configuration.isReloadScriptOnExecution()) {
            searchExecutor = getScriptExecutor(configuration.getSearchScriptFileName());
            searchAllExecutor = getScriptExecutor(configuration.getSearchAllScriptFileName());
        }
        if (query == null) {
            executor = searchAllExecutor;
            action = "SEARCHALL";
        } else {
            executor = searchExecutor;
        }

        if (executor != null) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            try {
                arguments.put("repository", connection.getDestination().getRepository());
            } catch (JCoException jcoe) {
                log.error("Can't pass the repository to Groovy script");
            }
            arguments.put("destination", connection.getDestination());
            arguments.put("objectClass", objectClass.getObjectClassValue());
            arguments.put("action", action);
            arguments.put("log", log);
            arguments.put("options", options.getOptions());
            arguments.put("query", query);
            try {
                List<Map> results = (List<Map>) executor.execute(arguments);
                processResults(objectClass, results, handler);
            } catch (Exception e) {
                throw new ConnectorException(action + " script error", e);
            }
        } else {
            throw new UnsupportedOperationException(action + "script executor is null. Problem loading "+action+" script");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options) {
        if (configuration.isReloadScriptOnExecution()) {
            createExecutor = getScriptExecutor(configuration.getCreateScriptFileName());
        }
        if (createExecutor != null) {
            if (createAttributes == null || createAttributes.isEmpty()) {
                throw new IllegalArgumentException("Create Attributes required");
            }

            final Map<String, Object> arguments = new HashMap<String, Object>();
            try {
                arguments.put("repository", connection.getDestination().getRepository());
            } catch (JCoException jcoe) {
                log.error("Can't pass the repository to Groovy script");
            }
            arguments.put("destination", connection.getDestination());
            arguments.put("action", "CREATE");
            arguments.put("log", log);
            arguments.put("objectClass", objectClass.getObjectClassValue());
            arguments.put("options", options.getOptions());
            // We give the id (name) as an argument, more friendly than dealing with __NAME__
            if (AttributeUtil.getNameFromAttributes(createAttributes) != null) {
                arguments.put("id", AttributeUtil.getNameFromAttributes(createAttributes).getNameValue());
            } else {
                throw new IllegalArgumentException("__NAME__ is missing from Create Attributes");
            }

            Map<String, List> attrMap = new HashMap();
            for (Attribute attr : createAttributes) {
                attrMap.put(attr.getName(), attr.getValue());
            }
            // let's get rid of __NAME__
            attrMap.remove("__NAME__");
            arguments.put("attributes", attrMap);

            // Password - if allowed we provide it in clear
            if (configuration.getClearTextPasswordToScript()) {
                GuardedString gpasswd = AttributeUtil.getPasswordValue(createAttributes);
                if (gpasswd != null) {
                    gpasswd.access(new GuardedString.Accessor() {
                        @Override
                        public void access(char[] clearChars) {
                            arguments.put("password", new String(clearChars));
                        }
                    });
                } else {
                    arguments.put("password", null);
                }
            }

            try {
                JCoContext.begin(connection.getDestination());
                Object uidAfter = createExecutor.execute(arguments);
                if (uidAfter instanceof String) {
                    log.ok("{0} created", uidAfter);
                    return new Uid((String) uidAfter);
                } else {
                    throw new ConnectorException("Create script didn't return with the __UID__ value");
                }
            } catch (Exception e) {
                throw new ConnectorException("Create script error", e);
            } finally {
                try {
                    JCoContext.end(connection.getDestination());
                } catch (JCoException jcoe) {
                    throw new ConnectorException(jcoe);
                }
            }
        } else {
            throw new UnsupportedOperationException("CREATE script executor is null. Problem loading Create script");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        return genericUpdate("UPDATE", objectClass, uid, replaceAttributes, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        return genericUpdate("ADD_ATTRIBUTE_VALUES", objectClass, uid, valuesToAdd, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        return genericUpdate("REMOVE_ATTRIBUTE_VALUES", objectClass, uid, valuesToRemove, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {
        if (configuration.isReloadScriptOnExecution()) {
            deleteExecutor = getScriptExecutor(configuration.getDeleteScriptFileName());
        }
        if (deleteExecutor != null) {
            if (uid == null || (uid.getUidValue() == null)) {
                throw new IllegalArgumentException("Uid required");
            }
            final String id = uid.getUidValue();
            final Map<String, Object> arguments = new HashMap<String, Object>();

            try {
                arguments.put("repository", connection.getDestination().getRepository());
            } catch (JCoException jcoe) {
                log.error("Can't pass the repository to Groovy script");
            }
            arguments.put("destination", connection.getDestination());
            arguments.put("action", "DELETE");
            arguments.put("log", log);
            arguments.put("objectClass", objectClass.getObjectClassValue());
            arguments.put("uid", id);
            arguments.put("options", options.getOptions());

            try {
                JCoContext.begin(connection.getDestination());
                deleteExecutor.execute(arguments);
                log.ok("{0} deleted", id);
            } catch (Exception e) {
                throw new ConnectorException("Delete script error", e);
            } finally {
                try {
                    JCoContext.end(connection.getDestination());
                } catch (JCoException jcoe) {
                    throw new ConnectorException(jcoe);
                }
            }
        } else {
            throw new UnsupportedOperationException("DELETE script executor is null. Problem loading Delete script");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler,
            final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void test() {
        configuration.validate();

        if (configuration.isReloadScriptOnExecution()) {
            testExecutor = getScriptExecutor(configuration.getTestScriptFileName());
        }

        if (testExecutor != null) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            try {
                arguments.put("repository", connection.getDestination().getRepository());
            } catch (JCoException jcoe) {
                log.error("Can't pass the repository to Groovy script");
            }
            arguments.put("destination", connection.getDestination());
            arguments.put("action", "TEST");
            arguments.put("log", log);
            try {
                JCoContext.begin(connection.getDestination());
                testExecutor.execute(arguments);
                log.ok("Test ok");
            } catch (Exception e) {
                throw new ConnectorException("Test script error", e);
            } finally {
                try {
                    JCoContext.end(connection.getDestination());
                } catch (JCoException jcoe) {
                    throw new ConnectorException(jcoe);
                }
            }
        } else {
            throw new UnsupportedOperationException("TEST script executor is null. Problem loading Test script");
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
                        cobld.addAttribute(AttributeBuilder.build(attrName, (Collection) attrValue));
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

    private Uid genericUpdate(String method, ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        if (configuration.isReloadScriptOnExecution()) {
            updateExecutor = getScriptExecutor(configuration.getUpdateScriptFileName());
        }
        if (updateExecutor != null) {
            if (attrs == null || attrs.isEmpty()) {
                throw new IllegalArgumentException("Attribute set required");
            }
            if (uid == null || (uid.getUidValue() == null)) {
                throw new IllegalArgumentException("Uid required");
            }

            final String id = uid.getUidValue();
            final Map<String, Object> arguments = new HashMap<String, Object>();
            try {
                arguments.put("repository", connection.getDestination().getRepository());
            } catch (JCoException jcoe) {
                log.error("Can't pass the repository to Groovy script");
            }
            arguments.put("destination", connection.getDestination());
            arguments.put("action", method);
            arguments.put("log", log);
            arguments.put("objectClass", objClass.getObjectClassValue());
            arguments.put("uid", id);
            arguments.put("options", options.getOptions());

            Map<String, List> attrMap = new HashMap<String, List>();
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
            if (configuration.getClearTextPasswordToScript() && method.equalsIgnoreCase("UPDATE")) {
                GuardedString gpasswd = AttributeUtil.getPasswordValue(attrs);
                if (gpasswd != null) {
                    gpasswd.access(new GuardedString.Accessor() {
                        @Override
                        public void access(char[] clearChars) {
                            arguments.put("password", new String(clearChars));
                        }
                    });
                } else {
                    arguments.put("password", null);
                }
            }
            try {
                JCoContext.begin(connection.getDestination());
                Object uidAfter = updateExecutor.execute(arguments);
                if (uidAfter instanceof String) {
                    log.ok("{0} updated ({1})", uidAfter, method);
                    return new Uid((String) uidAfter);
                }
            } catch (Exception e) {
                throw new ConnectorException("Update(" + method + ") script error", e);
            } finally {
                try {
                    JCoContext.end(connection.getDestination());
                } catch (JCoException jcoe) {
                    throw new ConnectorException(jcoe);
                }
            }
            throw new ConnectorException("Update script didn't return with the __UID__ value");
        } else {
            throw new UnsupportedOperationException("UPDATE script executor is null. Problem loading Update script");
        }
    }

    private String readFile(String filename) {
        File file = new File(filename);
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = null;
        String text;

        try {
            reader = new BufferedReader(new FileReader(file));
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(System.getProperty("line.separator"));
            }
        } catch (FileNotFoundException e) {
            throw new ConnectorException(filename + " not found", e);
        } catch (IOException e) {
            throw new ConnectorException(filename, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ConnectorException(filename, e);
            }
        }
        return contents.toString();
    }

    private ScriptExecutor getScriptExecutor(String scriptFileName) {
        ScriptExecutor scriptExec = null;
        String scriptCode = "";

        try {
            if (scriptFileName != null && scriptFileName.length() > 0) {
                scriptCode = readFile(scriptFileName);
            }
            if (scriptCode.length() > 0) {
                scriptExec = factory.newScriptExecutor(getClass().getClassLoader(), scriptCode, true);
                log.ok("Script {0} loaded", scriptFileName);
            }
        } catch (Exception e) {
            throw new ConnectorException("Script error", e);
        }
        return scriptExec;
    }

    private Map getAttrPerInfotype(OperationOptions opts) {
        Map<String, ArrayList<String>> infotypeTables = new HashMap<String, ArrayList<String>>();
        java.lang.String[] attrsToGet = opts.getAttributesToGet();
        if (attrsToGet != null) {
            for (String attr : attrsToGet) {
                if (attr.indexOf(":") != -1) {
                    java.lang.String[] array = attr.split(":");
                    if (infotypeTables.containsKey(array[0])) {
                        infotypeTables.get(array[0]).add(array[1]);
                    } else {
                        infotypeTables.put(array[0], new ArrayList<String>());
                        infotypeTables.get(array[0]).add(array[1]);
                    }
                }
            }
        }
        return infotypeTables;
    }
}