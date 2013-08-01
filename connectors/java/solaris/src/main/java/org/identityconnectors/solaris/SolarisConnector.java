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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2012 Evolveum, Radovan Semancik
 * Portions Copyrighted 2013 ForgeRock
 */
package org.identityconnectors.solaris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.solaris.SolarisConnection.ErrorHandler;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.SolarisAuthenticate;
import org.identityconnectors.solaris.operation.SolarisCreate;
import org.identityconnectors.solaris.operation.SolarisDelete;
import org.identityconnectors.solaris.operation.SolarisScriptOnConnector;
import org.identityconnectors.solaris.operation.SolarisUpdate;
import org.identityconnectors.solaris.operation.search.SolarisFilterTranslator;
import org.identityconnectors.solaris.operation.search.SolarisSearch;
import org.identityconnectors.solaris.operation.search.nodes.EqualsNode;
import org.identityconnectors.solaris.operation.search.nodes.Node;

import com.jcraft.jsch.JSch;

/**
 * @author David Adam
 *
 */
@ConnectorClass(displayNameKey = "Solaris", configurationClass = SolarisConfiguration.class)
public class SolarisConnector implements PoolableConnector, AuthenticateOp, SchemaOp, CreateOp,
        DeleteOp, UpdateOp, SearchOp<Node>, TestOp, ScriptOnResourceOp, ResolveUsernameOp {

    /**
     * Setup logging for the {@link SolarisConnector}.
     */
    private static final Log logger = Log.getLog(SolarisConnector.class);

    // TODO: Use configuration for the pattern
    private static final Pattern USER_NAME = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]{0,30})");

    private SolarisConnection connection;

    private SolarisConfiguration configuration;

    private Schema schema;

    /**
     * {@see org.identityconnectors.framework.spi.Connector#init(org.
     * identityconnectors .framework.spi.Configuration)}.
     */
    public void init(Configuration cfg) {
        configuration = (SolarisConfiguration) cfg;
        connection = new SolarisConnection(configuration);
        JSch.setLogger(new JSchLogger());
    }

    /**
     * {@see
     * org.identityconnectors.framework.spi.PoolableConnector#checkAlive()}.
     */
    public void checkAlive() {
        logger.info("checkAlive()");
        connection.checkAlive();
    }

    /**
     * Disposes of {@link SolarisConnector}'s resources.
     *
     * {@see org.identityconnectors.framework.spi.Connector#dispose()}
     */
    public void dispose() {
        logger.info("dispose()");
        configuration = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
    }

    /* *********************** OPERATIONS ************************** */
    /**
     * {@inheritDoc}
     * <p>
     * attempts to authenticate the given user / password on configured Solaris
     * resource
     */
    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password,
            OperationOptions options) {
        if (!USER_NAME.matcher(username).matches()) {
            throw new UnknownUidException("Invalid username: " + username);
        }
        Uid uid = null;
        try {
            uid =
                    new SolarisAuthenticate(this).authenticate(objectClass, username, password,
                            options);
        } finally {
            // after unsuccessful authenticate the connection might be in an
            // unusable state. We have to create a new connection then.
            connection.dispose();
            connection = null;
        }
        return uid;
    }

    /** {@inheritDoc} */
    public Uid create(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        return new SolarisCreate(this).create(objectClass, attrs, options);
    }

    /** {@inheritDoc} */
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        new SolarisDelete(this).delete(objectClass, uid, options);
    }

    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) {
        return new SolarisUpdate(this).update(objectClass, uid, AttributeUtil.addUid(
                replaceAttributes, uid), options);
    }

    public void executeQuery(ObjectClass objectClass, Node query, ResultsHandler handler,
            OperationOptions options) {
        new SolarisSearch(this, objectClass, query, handler, options).executeQuery();
    }

    public FilterTranslator<Node> createFilterTranslator(ObjectClass objectClass,
            OperationOptions options) {
        logger.info("creating Filter translator.");
        return new SolarisFilterTranslator(objectClass);
    }

    public Schema schema() {
        logger.info("schema()");
        if (schema == null) {
            schema = connection.getModeDriver().buildSchema(configuration.getSunCompat());
        }
        return schema;
    }

    /* ********************** AUXILIARY METHODS ********************* */
    /**
     * @throws Exception
     *             if the test of connection was failed.
     */
    public void test() {
        logger.info("test()");
        configuration.validate();
        checkAlive();
        if (configuration.isCheckCommandsAvailability()) {
            testCheckCommandsAndPermissions();
        }
    }

    /**
     * Check the resource and determine if the commands (used by the connector)
     * available and the account given has permission to execute them.
     */
    private void testCheckCommandsAndPermissions() {
        getConnection().doSudoStart();
        try {
            // determine if the required commands (by connector) are present
            Set<String> requiredCommands = getRequiredCommands(getConnection().isNis());
            StringBuilder args = new StringBuilder();
            for (String c : requiredCommands) {
                args.append(c).append(" ");
            }
            // The 'which' command needs sudo as it needs to probe root's path.
            // Also some commands may not be accessible to non-root user (e.g.
            // on RedHat-like systems).
            String whichCmd = getConnection().buildCommand(true, "which", args.toString());
            String out =
                    getConnection().executeCommand(whichCmd,
                            CollectionUtil.newSet("which: not found"));
            final String no = "no ";
            final String in = " in ";
            for (String line : out.split("\n")) {
                final int indexOfNo = line.indexOf(no);
                final int indexOfIn = line.indexOf(in);
                if (line.contains(no) && line.contains(in) && indexOfNo < indexOfIn) {
                    String param1 = line.substring(indexOfNo + no.length(), indexOfIn);
                    String param2 = line.substring(indexOfIn + in.length());
                    throw new ConnectorException(String.format(
                            "Failed to find '%s' in the path '%s'", param1, param2));
                }
            }

            // Check if the connector has write access to /tmp folder
            final String tmpFileName = "/tmp/SConnectorTmpAccessTest";
            out = getConnection().executeCommand("touch " + tmpFileName);
            if (StringUtil.isNotBlank(out)) {
                throw new ConnectorException("ERROR: non-empty output from 'touch' command: <"
                        + out + ">");
            }
            getConnection().executeCommand("rm -f " + tmpFileName);

            // Execute the read-only command (last) to see if they we have
            // permission to execute it
            Map<String, ErrorHandler> reject =
                    CollectionUtil.<String, ErrorHandler> newMap(
                            "[P,p]ermission|[d,D]enied|not allowed|Sorry,", new ErrorHandler() {
                                public void handle(String buffer) {
                                    throw new ConnectorException(
                                            "Invalid resource configuration: permission denied for execution of \"last\" command. Buffer: <"
                                                    + buffer + ">");
                                }
                            });
            getConnection().executeCommand("last -n 1", reject, Collections.<String> emptySet());
        } finally {
            getConnection().doSudoReset();
        }
    }

    private Set<String> getRequiredCommands(boolean isNis) {
        Set<String> result =
                CollectionUtil.newSet(
                        // required file commands for all unix connectors
                        "ls", "cp", "mv", "rm", "sed", "cat", "cut", "awk", "grep", "diff", "echo",
                        "sort", "touch", "chown", "chmod", "sleep");
        if (isNis) {
            result.addAll(CollectionUtil.newSet("ypcat", "ypmatch", "yppasswd"));
        } else {
            result.addAll(CollectionUtil.newSet(
            // user
                    "last", "useradd", "usermod", "userdel", "passwd",
                    // group
                    "groupadd", "groupmod", "groupdel"));
        }
        return result;
    }

    /* ********************** GET / SET methods ********************* */
    public SolarisConnection getConnection() {
        assert (connection != null);
        return connection;
    }

    /**
     * {@see org.identityconnectors.framework.spi.Connector#getConfiguration()}.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * supported scripting language is {@code /bin/sh} shell, that is present on
     * every Solaris resource.
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        return new SolarisScriptOnConnector(this).runScriptOnResource(request, options);
    }

    public Uid resolveUsername(ObjectClass objectClass, String username, OperationOptions options) {
        List<ConnectorObject> searchResult = null;
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (USER_NAME.matcher(username).matches()) {

                ToListResultsHandler handler = new ToListResultsHandler();
                Node query =
                        new EqualsNode(NativeAttribute.NAME, false, CollectionUtil
                                .newList(username));
                executeQuery(objectClass, query, handler, new OperationOptionsBuilder().build());
                searchResult = handler.getObjects();
                if (searchResult.isEmpty()) {
                    throw new UnknownUidException(String.format(
                            "userName: '%s' cannot be resolved", username));
                }
            } else {
                throw new UnknownUidException("Invalid username: " + username);
            }
        } else {
            throw new UnsupportedOperationException("ObjectClass: '"
                    + objectClass.getObjectClassValue()
                    + "' is not supported by ResolveUsernameOp.");
        }
        return searchResult.get(0).getUid();
    }

    static final class ToListResultsHandler implements ResultsHandler {

        private final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();

        public boolean handle(ConnectorObject object) {
            objects.add(object);
            return true;
        }

        public List<ConnectorObject> getObjects() {
            return objects;
        }

    }
}
