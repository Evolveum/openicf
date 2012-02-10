/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openicf.salesforce;

import java.util.*;

import org.forgerock.openicf.salesforce.utils.ForceAttributeUtils;
import org.identityconnectors.common.security.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.common.logging.Log;
import org.restlet.ext.jackson.JacksonRepresentation;

/**
 * Main implementation of the Salesforce Connector
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@ConnectorClass(
        displayNameKey = "SALESFORCE",
        configurationClass = SalesforceConfiguration.class)
public class SalesforceConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp,
        SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>, SyncOp, TestOp, UpdateAttributeValuesOp {
    /**
     * Setup logging for the {@link SalesforceConnector}.
     */
    private static final Log log = Log.getLog(SalesforceConnector.class);

    /**
     * Place holder for the Connection created in the init method
     */
    private SalesforceConnection connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link SalesforceConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private SalesforceConfiguration configuration;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration configuration1) {
        this.configuration = (SalesforceConfiguration) configuration1;
        this.connection = new SalesforceConnection(this.configuration);
    }

    /**
     * Disposes of the {@link SalesforceConnector}'s resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
        configuration = null;
        if (connection != null) {
            connection.release();
            connection = null;
        }
    }

    public void checkAlive() {
        connection.test();
    }

    /******************
     * SPI Operations
     *
     * Implement the following operations using the contract and
     * description found in the Javadoc for these methods.
     ******************/

    /**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass, final String userName, final GuardedString password, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String userName, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        JacksonRepresentation<Map> body = null;
        SchemaBuilder schemaBuilder = new SchemaBuilder(SalesforceConnector.class);
        try {
            body = new JacksonRepresentation<Map>(connection.getChild("/services/data/v23.0/sobjects").get(), Map.class);
            Object o = body.getObject();
            body.release();
            if (o instanceof Map) {
                List<Map<String, Object>> sobjects = (List<Map<String, Object>>) ((Map) o).get("sobjects");
                if (null != sobjects) {
                    for (Map<String, Object> sobject : sobjects) {
                        Object urls = sobject.get("urls");
                        if (urls instanceof Map) {
                            Object describe = ((Map) urls).get("describe");
                            System.out.println(describe);
                            if (describe instanceof String && ((String) describe).contains("/User/")) {
                                body = new JacksonRepresentation<Map>(connection.getChild((String) describe).get(), Map.class);
                                /* File root = new File(SalesforceConnector.class.getResource("/").toURI().resolve("cache/"));
                                root = new File(root, ((String) describe));
                                root.mkdirs();
                                FileOutputStream out = new FileOutputStream(new File(root,"GET.json"));
                                body.write(out);*/
                                Object so = body.getObject();
                                body.release();
                                if (so instanceof Map) {
                                    ForceAttributeUtils.parseDescribe((Map) so, schemaBuilder);
                                }

                            }
                        }
                    }
                } else {
                    log.error("/services/data/v24.0/sobjects/sobjects is null");
                }
            }
        } finally {
            if (null != body) {
                body.release();
            }
        }
        return schemaBuilder.build();
    }


    /**
     * {@inheritDoc}
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        /**
         * other option instead of query string, pass just the fields you want back:
         *  https://instance_name.salesforce.com/services/data/v23.0/sobjects/Account/
         *       001D000000INjVe?fields=AccountNumber,BillingPostalCode
         */
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
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
    public void test() {
        this.connection.test();
    }


    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass,
                      Uid uid,
                      Set<Attribute> replaceAttributes,
                      OperationOptions options) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass,
                                  Uid uid,
                                  Set<Attribute> valuesToAdd,
                                  OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass,
                                     Uid uid,
                                     Set<Attribute> valuesToRemove,
                                     OperationOptions options) {
        throw new UnsupportedOperationException();
    }

}
