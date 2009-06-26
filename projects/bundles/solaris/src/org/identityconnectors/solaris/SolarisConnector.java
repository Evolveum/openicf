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

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
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

/**
 * @author David Adam
 * 
 */
@ConnectorClass(displayNameKey = "Solaris", configurationClass = SolarisConfiguration.class)
public class SolarisConnector implements PoolableConnector, AuthenticateOp,
        SchemaOp, CreateOp, DeleteOp, UpdateOp, SearchOp<String>, TestOp {

    /**
     * Setup logging for the {@link DatabaseTableConnector}.
     */
    private final Log _log = Log.getLog(SolarisConnector.class);

    private SolarisConnection _connection;

    private SolarisConfiguration _configuration;

    private Schema _schema;

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
            SolarisConnection.test(_configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        return new OpAuthenticateImpl(_configuration, _connection, _log).authenticate(objectClass, username, password, options);
        
    }

    /** {@inheritDoc} */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs,
            OperationOptions options) {
        
        return new OpCreateImpl(_configuration, _connection, _log).create(oclass, attrs, options);
    }
    
    /** {@inheritDoc} */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        
        new OpDeleteImpl(_configuration, _connection, _log).delete(objClass, uid, options);
    }
    
    public Uid update(ObjectClass objclass, Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {
        return new OpUpdateImpl(_configuration, _connection, _log).update(objclass, uid, AttributeUtil.addUid(replaceAttributes, uid), options);
    }
    
    public void executeQuery(ObjectClass oclass, String query,
            ResultsHandler handler, OperationOptions options) {
        new OpSearchImpl(_configuration, _connection, _log).executeQuery(oclass, query, handler, options);
    }

    public FilterTranslator<String> createFilterTranslator(
            ObjectClass oclass, OperationOptions options) {
        return new SolarisFilterTranslator(/*oclass, options*/);
    }
    
    /**
     * TODO
     */
    public Schema schema() {
        if (_schema != null) {
            return _schema;
        }
        
        final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());
        
        // GROUPS
        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
        // to adjust the schema: 
        //attributes.add(AttributeInfoBuilder.build(STRING_CONSTANT));
        schemaBuilder.defineObjectClass(ObjectClass.GROUP_NAME, attributes);
        
        // USERS
        attributes = new HashSet<AttributeInfo>();
        
        attributes.add(OperationalAttributeInfos.PASSWORD);
        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        
        _schema = schemaBuilder.build();
        return _schema;
        
//      + "  <AccountAttributeTypes>\n"
//      + "    <AccountAttributeType name='accountId' type='string' mapName='accountId' mapType='string' required='true'>\n"
//      + "      <AttributeDefinitionRef>\n"
//      + "        <ObjectRef type='AttributeDefinition' name='accountId'/>\n"
//      + "      </AttributeDefinitionRef>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Home directory' type='string' mapName='dir' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Login shell' type='string' mapName='shell' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Primary group' type='string' mapName='group' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Secondary groups' type='string' mapName='secondary_group' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='User ID' type='string' mapName='uid' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Expiration date' type='string' mapName='expire' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Inactive' type='string' mapName='inactive' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Description' type='string' mapName='comment' mapType='string'>\n"
//      + "    </AccountAttributeType>\n"
//      + "    <AccountAttributeType name='Last login time' type='string' mapName='time_last_login' mapType='string' readOnly='true'>\n"
//      + "    </AccountAttributeType>\n"
//      + "     <AccountAttributeType  name='Maximum Password Age' type='string' mapName='max' mapType='string'>\n"
//      + "     </AccountAttributeType>\n"
//      + "     <AccountAttributeType  name='Minimum Password Age' type='string' mapName='min' mapType='string'>\n"
//      + "     </AccountAttributeType>\n"
//      + "     <AccountAttributeType  name='Password Warn Time' type='string' mapName='warn'   mapType='string'>\n"
//      + "     </AccountAttributeType>\n"
//      + "     <AccountAttributeType  name='Lock Account' type='string' mapName='lock' mapType='string'>\n"
//      + "     </AccountAttributeType>\n"
//      + "  </AccountAttributeTypes>\n"
//      + "  <ObjectTypes>\n"
//      + "    <ObjectType name='Group' nameKey='UI_RESOURCE_OBJECT_TYPE_GROUP' icon='group'>\n"
//      + "      <ObjectClasses operator='AND'>\n"
//      + "        <ObjectClass name='group'/>\n"
//      + "      </ObjectClasses>\n"
//      + "      <ObjectFeatures>\n"
//      + "        <ObjectFeature name='create'/>\n"
//      + "        <ObjectFeature name='update'/>\n"
//      + "        <ObjectFeature name='delete'/>\n"
//      + "        <ObjectFeature name='rename'/>\n"
//      + "        <ObjectFeature name='saveas'/>\n"
//      + "      </ObjectFeatures>\n"
//      + "      <ObjectAttributes idAttr='groupName' displayNameAttr='groupName' descriptionAttr='description'>\n"
//      + "        <ObjectAttribute name='groupName' type='string'/>\n"
//      + "        <ObjectAttribute name='gid' type='string'/>\n"
//      + "        <ObjectAttribute name='users' type='string'/>\n"
//      + "      </ObjectAttributes>\n"
//      + "    </ObjectType>\n"
//      + "  </ObjectTypes>\n"
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
