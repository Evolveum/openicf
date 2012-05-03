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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
#if ($ALL_OPERATIONS == 'Y' || $ALL_OPERATIONS == 'y')
        #set( $all_operations_safe = true)
#end
#if ( $poolableConnector == 'Y' || $poolableConnector == 'y' )
    #set( $poolable_connector_safe = true)
#end
#if ( $attributeNormalizer == 'Y' || $attributeNormalizer == 'y')
    #set( $attribute_normalizer_safe = true)
#end
#if ( $all_operations_safe || $OP_AUTHENTICATE == 'Y' || $OP_AUTHENTICATE == 'y' )
    #set( $op_authenticate_safe = true)
#end
#if ( $all_operations_safe || $OP_CREATE == 'Y' || $OP_CREATE == 'y')
    #set( $op_create_safe = true)
#end
#if ( $all_operations_safe || $OP_DELETE == 'Y' || $OP_DELETE == 'y' )
    #set( $op_delete_safe = true)
#end
#if ( $all_operations_safe || $OP_RESOLVEUSERNAME == 'Y' || $OP_RESOLVEUSERNAME == 'y')
    #set( $op_resolveusername_safe = true)
#end
#if ( $all_operations_safe || $OP_SCHEMA == 'Y' || $OP_SCHEMA == 'y' )
    #set( $op_schema_safe = true)
#end
#if ( $all_operations_safe || $OP_SCRIPTONCONNECTOR == 'Y' || $OP_SCRIPTONCONNECTOR == 'y')
    #set( $op_scriptonconnector_safe = true)
#end
#if ( $all_operations_safe || $OP_SCRIPTONRESOURCE == 'Y' || $OP_SCRIPTONRESOURCE == 'y' )
    #set( $op_scriptonresource_safe = true)
#end
#if ( $all_operations_safe || $OP_SEARCH == 'Y' || $OP_SEARCH == 'y')
    #set( $op_search_safe = true)
#end
#if ( $all_operations_safe || $OP_SYNC == 'Y' || $OP_SYNC == 'y' )
    #set( $op_sync_safe = true)
#end
#if ( $all_operations_safe || $OP_TEST == 'Y' || $OP_TEST == 'y')
    #set( $op_test_safe = true)
#end
#if ( $all_operations_safe || $OP_UPDATEATTRIBUTEVALUES == 'Y' || $OP_UPDATEATTRIBUTEVALUES == 'y' )
    #set( $op_updateattributevalues_safe = true)
#end
#if ( $all_operations_safe || $OP_UPDATE == 'Y' || $OP_UPDATE == 'y')
    #set( $op_update_safe = true)
#end

package ${package};

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
#if ( $poolable_connector_safe )
import org.identityconnectors.framework.spi.PoolableConnector;
#else
import org.identityconnectors.framework.spi.Connector;
#end
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Main implementation of the ${connector_name} Connector
 *
 * @author ${symbol_dollar}author${symbol_dollar}
 * @version ${symbol_dollar}Revision${symbol_dollar} ${symbol_dollar}Date${symbol_dollar}
 */
@ConnectorClass(
        displayNameKey = "${connector_name}",
        configurationClass = ${connector_name}Configuration.class)
public class ${connector_name}Connector implements
#if ( $poolable_connector_safe )
        PoolableConnector
#else
        Connector
#end
#if ( $attribute_normalizer_safe )
        ,AttributeNormalizer
#end
#if ( $op_authenticate_safe )
        ,AuthenticateOp
#end
#if ( $op_create_safe )
        ,CreateOp
#end
#if ( $op_delete_safe )
        ,DeleteOp
#end
#if ( $op_resolveusername_safe )
        ,ResolveUsernameOp
#end
#if ( $op_schema_safe )
        ,SchemaOp
#end
#if ( $op_scriptonconnector_safe )
        ,ScriptOnConnectorOp
#end
#if ( $op_scriptonresource_safe )
        ,ScriptOnResourceOp
#end
#if ( $op_search_safe )
        ,SearchOp<String>
#end
#if ( $op_sync_safe )
        ,SyncOp
#end
#if ( $op_test_safe )
        ,TestOp
#end
#if ( $op_updateattributevalues_safe )
        ,UpdateAttributeValuesOp
#end
#if ( $op_update_safe )
        ,UpdateOp
#end
    {
    /**
     * Setup logging for the {@link ${connector_name}Connector}.
     */
    private static final Log LOGGER = Log.getLog(${connector_name}Connector.class);

    /**
     * Place holder for the Connection created in the init method
     */
    private ${connector_name}Connection connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link ${connector_name}Connector${symbol_pound}init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private ${connector_name}Configuration configuration;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector${symbol_pound}init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration configuration1) {
        this.configuration = (${connector_name}Configuration) configuration1;
        this.connection = new ${connector_name}Connection(this.configuration);
    }

    /**
     * Disposes of the {@link ${connector_name}Connector}'s resources.
     *
     * @see Connector${symbol_pound}dispose()
     */
    public void dispose() {
        configuration = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
    }

#if ( $poolable_connector_safe )
    /**
    *  {@inheritDoc}
    */
    public void checkAlive() {
        connection.test();
    }

#end
#if ( $attribute_normalizer_safe )
    /**
    *  {@inheritDoc}
    */
    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        return attribute;
    }

#end

    /******************
     * SPI Operations
     *
     * Implement the following operations using the contract and
     * description found in the Javadoc for these methods.
     ******************/

#if ( $op_authenticate_safe )
    /**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass, final String userName,
            final GuardedString password, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_resolveusername_safe )
    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String userName,
            final OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_create_safe )
    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes,
            final OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_delete_safe )
    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_schema_safe )
    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_scriptonconnector_safe )
    /**
     * {@inheritDoc}
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_scriptonresource_safe )
    /**
     * {@inheritDoc}
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_search_safe )
    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_sync_safe )
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
#end

#if ( $op_test_safe )
    /**
     * {@inheritDoc}
     */
    public void test() {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_update_safe )
    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end

#if ( $op_updateattributevalues_safe )
    /**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid,
            Set<Attribute> valuesToRemove, OperationOptions options) {
        throw new UnsupportedOperationException();
    }
#end
}
