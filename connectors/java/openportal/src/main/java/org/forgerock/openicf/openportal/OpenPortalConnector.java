/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openicf.openportal;

import java.util.*;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.security.*;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.common.logging.Log;

/**
 * Main implementation of the OpenPortal Connector
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@ConnectorClass(
        displayNameKey = "OpenPortal",
        configurationClass = OpenPortalConfiguration.class)
public class OpenPortalConnector implements PoolableConnector, AuthenticateOp, CreateOp, DeleteOp, SchemaOp, SearchOp<String>, TestOp, UpdateOp{
    /**
     * Setup logging for the {@link OpenPortalConnector}.
     */
    private static final Log log = Log.getLog(OpenPortalConnector.class);

    /**
     * Place holder for the OpenPortalConnection created in the init method
     */
    private OpenPortalConnection connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link OpenPortalConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private OpenPortalConfiguration config;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.config;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration cfg) {
        this.config = (OpenPortalConfiguration) cfg;
        this.connection = new OpenPortalConnectionImpl(config);
    }

    /**
     * Disposes of the {@link OpenPortalConnector}'s resources.
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

    /******************
     * SPI Operations
     *
     * Implement the following operations using the contract and
     * description found in the Javadoc for these methods.
     ******************/

    /**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass, final String username, final GuardedString password, final OperationOptions options) {
        final String method = "authenticate";
        log.info("Entry {0}", method);

        Assertions.nullCheck(objectClass, "objectClass");
        Assertions.nullCheck(password, "password");
        Assertions.nullCheck(username, "username");
        Assertions.blankCheck(username, "username");

        if(!objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new IllegalArgumentException("Authentication failed. Can only authenticate against " + ObjectClass.ACCOUNT_NAME + " resources.");
        }

        Uid uid = connection.authenticate(username, password);

        if(uid == null){
            throw new InvalidPasswordException("Invalid password for user: " + username);
        }

        log.info("Exit {0}", method);

        return uid;

    }

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objClass, final Set<Attribute> attrs, final OperationOptions options) {
        final String method = "create";
        log.info("Entry {0} ", method);

        Assertions.nullCheck(objClass, "objectClass");
        Assertions.nullCheck(attrs, "attrs");

        Uid uid = connection.create(objClass, attrs);

        log.info("Exit {0}", method);
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        final String method = "update";
        log.info("Entry {0}", method);

        Assertions.nullCheck(objclass, "objClass");
        Assertions.nullCheck(replaceAttributes, "replaceAttributes");

        Uid returnUid = connection.update(objclass, uid, replaceAttributes);

        log.info("Exit {0}", method);

        return returnUid;
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objClass, final Uid uid, final OperationOptions options) {
        final String method = "delete";
        log.info("Entry {0}", method);

        Assertions.nullCheck(objClass, "objClass");
        Assertions.nullCheck(uid, "uid");

        connection.delete(objClass, uid);

        log.info("Exit {0}", method);
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        final String method = "schema";
        log.info("Entry {0}", method);
        log.info("Exit {0}", method);

        return connection.schema();
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objClass, OperationOptions options) {
        return new OpenPortalFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objClass, String query, ResultsHandler handler, OperationOptions options) {
        final String method = "executeQuery";
        log.info("Entry {0}", method);

        Assertions.nullCheck(objClass, "objClass");
        Assertions.nullCheck(handler, "handler");

        Collection<ConnectorObject> result = connection.allConnectorObjects(objClass);

        for(ConnectorObject connectorObject: result){
            handler.handle(connectorObject);
        }

        log.info("Exit {0}", method);
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        final String method = "test";
        log.info("Entry {0}", method);

        Assertions.nullCheck(config, "config");
        Assertions.nullCheck(connection, "connection");

        if(connection != null){
            connection.test();
        }

        log.info("Exit {0}", method);
    }
}
