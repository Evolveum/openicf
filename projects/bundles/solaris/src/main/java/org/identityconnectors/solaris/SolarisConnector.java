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
package org.identityconnectors.solaris;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
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
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
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
import org.identityconnectors.test.common.ToListResultsHandler;

/**
 * @author David Adam
 * 
 */
@ConnectorClass(displayNameKey = "Solaris", configurationClass = SolarisConfiguration.class)
public class SolarisConnector implements PoolableConnector, AuthenticateOp,
        SchemaOp, CreateOp, DeleteOp, UpdateOp, SearchOp<Node>, TestOp, ScriptOnResourceOp, ResolveUsernameOp {

    /**
     * Setup logging for the {@link SolarisConnector}.
     */
    private static final Log log = Log.getLog(SolarisConnector.class);

    private SolarisConnection connection;

    private SolarisConfiguration configuration;

    private static Schema _schema;

    /**
     * {@see
     * org.identityconnectors.framework.spi.Connector#init(org.identityconnectors
     * .framework.spi.Configuration)}
     */
    public void init(Configuration cfg) {
        configuration = (SolarisConfiguration) cfg;
        connection = initConnection(configuration);
    }

    private SolarisConnection initConnection(final SolarisConfiguration configuration) {
        return new SolarisConnection(configuration);
    }

    /**
     * {@see org.identityconnectors.framework.spi.PoolableConnector#checkAlive()}
     */
    public void checkAlive() {
        log.info("checkAlive()");
        connection.checkAlive();
    }

    /**
     * Disposes of {@link SolarisConnector}'s resources
     * 
     * {@see org.identityconnectors.framework.spi.Connector#dispose()}
     */
    public void dispose() {
        log.info("dispose()");
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
    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        Uid uid = null;
        try {
            uid = new SolarisAuthenticate(this).authenticate(objectClass, username, password, options);
        } finally {
            // after unsuccessful authenticate the connection might be in an unusable state. We have to create a new connection then.
            connection.dispose();
            connection = null;
        }
        return uid; 
    }

    /** {@inheritDoc} */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs,
            OperationOptions options) {        
        return new SolarisCreate(this).create(oclass, attrs, options);
    }
    
    /** {@inheritDoc} */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        
        new SolarisDelete(this).delete(objClass, uid, options);
    }
    
    public Uid update(ObjectClass objclass, Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {
        return new SolarisUpdate(this).update(objclass, uid, AttributeUtil.addUid(replaceAttributes, uid), options);
    }
    
    public void executeQuery(ObjectClass oclass, Node query,
            ResultsHandler handler, OperationOptions options) {
        new SolarisSearch(this, oclass, query, handler, options).executeQuery();
    }

    public FilterTranslator<Node> createFilterTranslator(
            ObjectClass oclass, OperationOptions options) {
        log.info("creating Filter translator.");
        return new SolarisFilterTranslator(oclass);
    }
    
    /**
     * helper class for lazy initialization of a static field (Bloch: Effective Java)
     */
    private static class SchemaHolder {
        static final Schema schema = buildSchema();
    }
    
    // FIXME: control schema identity with adapter.
    public Schema schema() {
        log.info("schema()");
        return SchemaHolder.schema;
    }

    private static Schema buildSchema() {
        final SchemaBuilder schemaBuilder = new SchemaBuilder(SolarisConnector.class);
        
        /* 
         * GROUP
         */
        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
        //attributes.add(Name.INFO);
        for (GroupAttribute attr : GroupAttribute.values()) {
            switch (attr) {
            case USERS:
                attributes.add(AttributeInfoBuilder.build(attr.getName(), String.class, EnumSet.of(Flags.MULTIVALUED)));
                break;
            case GROUPNAME: // adapter also didn't support update of Group's name
                attributes.add(AttributeInfoBuilder.build(attr.getName(), String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
                break;

            default:
                attributes.add(AttributeInfoBuilder.build(attr.getName()));
                break;
            }//switch
        }//for
        
        //GROUP supports no authentication:
        final ObjectClassInfo ociInfoGroup = new ObjectClassInfoBuilder().setType(ObjectClass.GROUP_NAME).addAllAttributeInfo(attributes).build();
        schemaBuilder.defineObjectClass(ociInfoGroup);
        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfoGroup);
        schemaBuilder.removeSupportedObjectClass(ResolveUsernameOp.class, ociInfoGroup);
        
        /*
         * ACCOUNT
         */
        attributes = new HashSet<AttributeInfo>();
        attributes.add(OperationalAttributeInfos.PASSWORD);
        for (AccountAttribute attr : AccountAttribute.values()) {
            AttributeInfo newAttr = null;
            switch (attr) {
            case MIN:
            case MAX:
            case INACTIVE:
                newAttr = AttributeInfoBuilder.build(attr.getName(), int.class);
                break;
            default:
                newAttr = AttributeInfoBuilder.build(attr.getName());
                break;
            }
            
            attributes.add(newAttr);
        }
        attributes.add(OperationalAttributeInfos.PASSWORD);
        final ObjectClassInfo ociInfoAccount = new ObjectClassInfoBuilder().setType(ObjectClass.ACCOUNT_NAME).addAllAttributeInfo(attributes).build();
        schemaBuilder.defineObjectClass(ociInfoAccount);
        
        /*
         * SHELL
         */
        attributes = new HashSet<AttributeInfo>();
        attributes.add(
                AttributeInfoBuilder.build(SolarisSearch.SHELL.getObjectClassValue(), String.class, EnumSet.of(Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT, Flags.NOT_UPDATEABLE))
                );
        final ObjectClassInfo ociInfoShell = new ObjectClassInfoBuilder().addAllAttributeInfo(attributes).setType(SolarisSearch.SHELL.getObjectClassValue()).build();
        schemaBuilder.defineObjectClass(ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(CreateOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(UpdateOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(DeleteOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(SchemaOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(ResolveUsernameOp.class, ociInfoShell);
        
        _schema = schemaBuilder.build();
        return _schema;
    }

    /* ********************** AUXILIARY METHODS ********************* */
    /**
     * @throws Exception if the test of connection was failed.
     */
    public void test() {
        log.info("test()");
        configuration.validate();        
        checkAlive();
        testCheckCommandsAndPermissions();
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
            String whichCmd = getConnection().buildCommand("which", args.toString());
            String out = getConnection().executeCommand(whichCmd, CollectionUtil.newSet("which: not found"));
            final String no = "no ";
            final String in = " in ";
            for (String line : out.split("\n")) {
                final int indexOfNo = line.indexOf(no);
                final int indexOfIn = line.indexOf(in);
                if (line.contains(no) && line.contains(in) && indexOfNo < indexOfIn) {
                    String param1 = line.substring(indexOfNo + no.length(), indexOfIn);
                    String param2 = line.substring(indexOfIn + in.length());
                    throw new ConnectorException(String.format("Failed to find '%s' in the path '%s'", param1, param2));
                }
            }
            
            // Check if the connector has write access to /tmp folder
            final String tmpFileName = "/tmp/SConnectorTmpAccessTest";
            out = getConnection().executeCommand("touch " + tmpFileName);
            if (out.length() > 0) {
                throw new ConnectorException("ERROR: buffer: <" + out + ">");
            }
            getConnection().executeCommand("rm -f " + tmpFileName);
            
            // Execute the read-only command (last) to see if they we have
            // permission to execute it            
            Map<String, ErrorHandler> reject = CollectionUtil.<String, ErrorHandler>newMap("[P,p]ermission|[d,D]enied|not allowed|Sorry,", new ErrorHandler() {
                public void handle(String buffer) {
                    throw new ConnectorException("Invalid resource configuration: permission denied for execution of \"last\" command. Buffer: <" + buffer + ">");
                }
            });
            getConnection().executeCommand("last -n 1", reject, Collections.<String>emptySet());
        } finally {
            getConnection().doSudoReset();
        }
    }

    private Set<String> getRequiredCommands(boolean isNis) {
        Set<String> result = CollectionUtil.newSet(
                // required file commands for all unix connectors
                "ls", "cp", "mv", "rm", "sed", "cat", "cut", "awk", "grep", "diff", "echo", "sort", "touch", "chown", "chmod",
                "sleep"
                );
        if (isNis) {
            result.addAll(CollectionUtil.newSet("ypcat", "ypmatch", "yppasswd"));
        } else {
            result.addAll(CollectionUtil.newSet(
                    //user
                    "last", "useradd", "usermod", "userdel", "passwd",
                    //group
                    "groupadd", "groupmod", "groupdel"
                    ));
        }
        return result;
    }

    /* ********************** GET / SET methods ********************* */
    public SolarisConnection getConnection() {
        if (connection != null)
            return connection;
        
        connection = initConnection(configuration);
        return connection;
    }
    
    /**
     * {@see org.identityconnectors.framework.spi.Connector#getConfiguration()}
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
            ToListResultsHandler handler = new ToListResultsHandler();
            Node query = new EqualsNode(NativeAttribute.NAME, false, CollectionUtil.newList(username));
            executeQuery(objectClass, query, handler, new OperationOptionsBuilder().build());
            searchResult = handler.getObjects();
            if (searchResult.isEmpty()) {
                throw new UnknownUidException(String.format("userName: '%s' cannot be resolved", username));
            }
        } else {
            throw new IllegalArgumentException("ObjectClass: '" + objectClass.getObjectClassValue() + "' is not supported by ResolveUsernameOp.");
        }
        return searchResult.get(0).getUid();
    }
}
