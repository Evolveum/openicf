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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.operation.OpAuthenticateImpl;
import org.identityconnectors.solaris.operation.OpCreateImpl;
import org.identityconnectors.solaris.operation.OpDeleteImpl;
import org.identityconnectors.solaris.operation.OpUpdateImpl;
import org.identityconnectors.solaris.operation.search.OpSearchImpl;
import org.identityconnectors.solaris.operation.search.SolarisFilterTranslator;
import org.identityconnectors.solaris.operation.search.nodes.Node;

/**
 * @author David Adam
 * 
 */
@ConnectorClass(displayNameKey = "Solaris", configurationClass = SolarisConfiguration.class)
public class SolarisConnector implements PoolableConnector, AuthenticateOp,
        SchemaOp, CreateOp, DeleteOp, UpdateOp, SearchOp<Node>, TestOp {

    /**
     * Setup logging for the {@link DatabaseTableConnector}.
     */
    private final Log _log = Log.getLog(SolarisConnector.class);

    private SolarisConnection _connection;

    private SolarisConfiguration _configuration;

    private static Schema _schema;
    
    private static String CHECK_ALIVE = "checkAlive";

    /**
     * {@see
     * org.identityconnectors.framework.spi.Connector#init(org.identityconnectors
     * .framework.spi.Configuration)}
     */
    public void init(Configuration cfg) {
        _configuration = (SolarisConfiguration) cfg;
        _connection = new SolarisConnection(_configuration);
    }

    /**
     * {@see org.identityconnectors.framework.spi.PoolableConnector#checkAlive()}
     */
    public void checkAlive() {
        try {
            String out = getConnection().executeCommand("echo '" + CHECK_ALIVE + "'");
            if (!out.contains(CHECK_ALIVE)) {
                throw new RuntimeException("Solaris Connector no longer alive.");
            }
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * Disposes of {@link SolarisConnector}'s resources
     * 
     * {@see org.identityconnectors.framework.spi.Connector#dispose()}
     */
    public void dispose() {
        _configuration = null;
        if (_connection != null) {
            _connection.dispose();
            _connection = null;
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
        return new OpAuthenticateImpl(_log, this).authenticate(objectClass, username, password, options);
        
    }

    /** {@inheritDoc} */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs,
            OperationOptions options) {
        
        return new OpCreateImpl(_log, this).create(oclass, attrs, options);
    }
    
    /** {@inheritDoc} */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        
        new OpDeleteImpl(_log, this).delete(objClass, uid, options);
    }
    
    public Uid update(ObjectClass objclass, Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {
        return new OpUpdateImpl(_log, this).update(objclass, uid, AttributeUtil.addUid(replaceAttributes, uid), options);
    }
    
    public void executeQuery(ObjectClass oclass, Node query,
            ResultsHandler handler, OperationOptions options) {
        new OpSearchImpl(_log, this, oclass, query, handler, options).executeQuery();
    }

    public FilterTranslator<Node> createFilterTranslator(
            ObjectClass oclass, OperationOptions options) {
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
            attributes.add(AttributeInfoBuilder.build(attr.getName()));
        }
        
        //GROUP supports no authentication:
        final ObjectClassInfoBuilder ociB = new ObjectClassInfoBuilder();
        ociB.setType(ObjectClass.GROUP_NAME);
        ociB.addAllAttributeInfo(attributes);
        final ObjectClassInfo ociInfo = ociB.build();
        schemaBuilder.defineObjectClass(ociInfo);
        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfo);
        
        /*
         * ACCOUNT
         */
        attributes = new HashSet<AttributeInfo>();
        attributes.add(OperationalAttributeInfos.PASSWORD);
        for (AccountAttribute attr : AccountAttribute.values()) {
            AttributeInfo newAttr = null;
            
            if (!attr.equals(AccountAttribute.UID)) {
                newAttr = AttributeInfoBuilder.build(attr.getName());
            } else {
                // 'uid' is not returned by default, as __NAME__ already contains this information. 
                // This attribute is there just for sake of backward compatibility.
                newAttr = AttributeInfoBuilder.build(attr.getName(), String.class, EnumSet.of(Flags.NOT_RETURNED_BY_DEFAULT));
            }
            
            attributes.add(newAttr);
        }
        
        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        
        _schema = schemaBuilder.build();
        return _schema;
    }

    /* ********************** AUXILIARY METHODS ********************* */
    /**
     * @throws Exception if the test of connection was failed.
     */
    public void test() {
        _configuration.validate();        
        checkAlive();
    }

    /* ********************** GET / SET methods ********************* */
    public SolarisConnection getConnection() {
        return _connection;
    }
    
    /**
     * {@see org.identityconnectors.framework.spi.Connector#getConfiguration()}
     */
    public Configuration getConfiguration() {
        return _configuration;
    }




}
