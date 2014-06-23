/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/CDDL-1.0
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://opensource.org/licenses/CDDL-1.0
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

import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
#if ( $poolable_connector_safe )
import org.identityconnectors.framework.spi.PoolableConnector;
#else
import org.identityconnectors.framework.spi.Connector;
#end
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.SyncTokenResultsHandler;
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
 * Main implementation of the ${connectorName} Connector.
 *
 */
#set( $connectorName = $connector_name.toLowerCase())
@ConnectorClass(
        displayNameKey = "${connectorName}.connector.display",
        configurationClass = ${connectorName}Configuration.class)
public class ${connectorName}Connector implements #if($poolable_connector_safe)PoolableConnector#{else}Connector#end#if ( $attribute_normalizer_safe ), AttributeNormalizer#end#if ( $op_authenticate_safe ), AuthenticateOp#end#if ( $op_create_safe ), CreateOp#end#if ( $op_delete_safe ), DeleteOp#end#if ( $op_resolveusername_safe ), ResolveUsernameOp#end#if ( $op_schema_safe ), SchemaOp#end#if ( $op_scriptonconnector_safe ), ScriptOnConnectorOp#end#if ( $op_scriptonresource_safe ), ScriptOnResourceOp#end#if ( $op_search_safe ), SearchOp<String>#end#if ( $op_sync_safe ), SyncOp#end#if ( $op_test_safe ), TestOp#end#if ( $op_updateattributevalues_safe ), UpdateAttributeValuesOp#elseif ( $op_update_safe ), UpdateOp#end {

    /**
     * Setup logging for the {@link ${connectorName}Connector}.
     */
    private static final Log logger = Log.getLog(${connectorName}Connector.class);

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link ${connectorName}Connector${symbol_pound}init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private ${connectorName}Configuration configuration;

    private Schema schema = null;

    /**
     * Gets the Configuration context for this connector.
     *
     * @return The current {@link Configuration}
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @param configuration the new {@link Configuration}
     * @see org.identityconnectors.framework.spi.Connector${symbol_pound}init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(final Configuration configuration) {
        this.configuration = (${connectorName}Configuration) configuration;
    }

    /**
     * Disposes of the {@link ${connectorName}Connector}'s resources.
     *
     * @see org.identityconnectors.framework.spi.Connector${symbol_pound}dispose()
     */
    public void dispose() {
        configuration = null;
    }

#if ( $poolable_connector_safe )
    /**
    *  {@inheritDoc}
    */
    public void checkAlive() {
        // Do some cheap operartion to verify it's a healty Connector instance from the pool.
    }

#end
#if ( $attribute_normalizer_safe )
    /**
    *  {@inheritDoc}
    */
    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        if (AttributeUtil.namesEqual(attribute.getName(), Uid.NAME)) {
            return new Uid(AttributeUtil.getStringValue(attribute).toLowerCase(Locale.US));
        }
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
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            return new Uid(userName);
        } else {
            logger.warn("Authenticate of type {0} is not supported", configuration
                    .getConnectorMessages().format(objectClass.getDisplayNameKey(),
                            objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("Authenticate of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }
#end

#if ( $op_resolveusername_safe )
    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String userName,
            final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            return new Uid(userName);
        } else {
            logger.warn("ResolveUsername of type {0} is not supported", configuration
                    .getConnectorMessages().format(objectClass.getDisplayNameKey(),
                            objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("ResolveUsername of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }
#end

#if ( $op_create_safe )
    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes,
            final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objectClass) || ObjectClass.GROUP.equals(objectClass)) {
            Name name = AttributeUtil.getNameFromAttributes(createAttributes);
            if (name != null) {
                // do real create here
                return new Uid(AttributeUtil.getStringValue(name).toLowerCase(Locale.US));
            } else {
                throw new InvalidAttributeValueException("Name attribute is required");
            }
        } else {
            logger.warn("Delete of type {0} is not supported", configuration.getConnectorMessages()
                    .format(objectClass.getDisplayNameKey(), objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("Delete of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }
#end

#if ( $op_delete_safe )
    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objectClass) || ObjectClass.GROUP.equals(objectClass)) {
            // do real delete here
        } else {
            logger.warn("Delete of type {0} is not supported", configuration.getConnectorMessages()
                    .format(objectClass.getDisplayNameKey(), objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("Delete of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }
#end

#if ( $op_schema_safe )
    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        if (null == schema) {
            final SchemaBuilder builder = new SchemaBuilder(${connectorName}Connector.class);
            // Account
            ObjectClassInfoBuilder accountInfoBuilder = new ObjectClassInfoBuilder();
            accountInfoBuilder.addAttributeInfo(Name.INFO);
            accountInfoBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
            accountInfoBuilder.addAttributeInfo(PredefinedAttributeInfos.GROUPS);
            accountInfoBuilder.addAttributeInfo(AttributeInfoBuilder.build("firstName"));
            accountInfoBuilder.addAttributeInfo(AttributeInfoBuilder.define("lastName")
                    .setRequired(true).build());
            builder.defineObjectClass(accountInfoBuilder.build());

            // Group
            ObjectClassInfoBuilder groupInfoBuilder = new ObjectClassInfoBuilder();
            groupInfoBuilder.setType(ObjectClass.GROUP_NAME);
            groupInfoBuilder.addAttributeInfo(Name.INFO);
            groupInfoBuilder.addAttributeInfo(PredefinedAttributeInfos.DESCRIPTION);
            groupInfoBuilder.addAttributeInfo(AttributeInfoBuilder.define("members").setCreateable(
                    false).setUpdateable(false).setMultiValued(true).build());

            // Only the CRUD operations
            builder.defineObjectClass(groupInfoBuilder.build(), CreateOp.class, SearchOp.class,
                    UpdateOp.class, DeleteOp.class);

            // Operation Options
            builder.defineOperationOption(OperationOptionInfoBuilder.buildAttributesToGet(),
                    SearchOp.class);

            // Support paged Search
            builder.defineOperationOption(OperationOptionInfoBuilder.buildPageSize(),
                    SearchOp.class);
            builder.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsCookie(),
                    SearchOp.class);

            // Support to execute operation with provided credentials
            builder.defineOperationOption(OperationOptionInfoBuilder.buildRunWithUser());
            builder.defineOperationOption(OperationOptionInfoBuilder.buildRunWithPassword());

            schema = builder.build();
        }
        return schema;
    }
#end

#if ( $op_scriptonconnector_safe )
    /**
     * {@inheritDoc}
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        final ScriptExecutorFactory factory =
                ScriptExecutorFactory.newInstance(request.getScriptLanguage());
        final ScriptExecutor executor =
                factory.newScriptExecutor(getClass().getClassLoader(), request.getScriptText(),
                        true);

        if (StringUtil.isNotBlank(options.getRunAsUser())) {
            String password = SecurityUtil.decrypt(options.getRunWithPassword());
            // Use these to execute the script with these credentials
            Assertions.blankCheck(password, "password");
        }
        try {
            return executor.execute(request.getScriptArguments());
        } catch (Throwable e) {
            logger.warn(e, "Failed to execute Script");
            throw ConnectorException.wrap(e);
        }
    }
#end

#if ( $op_scriptonresource_safe )
    /**
     * {@inheritDoc}
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        try {
            // Execute the script on remote resource
            if (StringUtil.isNotBlank(options.getRunAsUser())) {
                String password = SecurityUtil.decrypt(options.getRunWithPassword());
                // Use these to execute the script with these credentials
                Assertions.blankCheck(password, "password");
                return options.getRunAsUser();
            }
            throw new UnknownHostException("Failed to connect to remote SSH");
        } catch (Throwable e) {
            logger.warn(e, "Failed to execute Script");
            throw ConnectorException.wrap(e);
        }
    }
#end

#if ( $op_search_safe )
    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass,
            OperationOptions options) {
        return new ${connectorName}FilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler,
            OperationOptions options) {
        final ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setUid("3f50eca0-f5e9-11e3-a3ac-0800200c9a66");
        builder.setName("Foo");
        builder.addAttribute(AttributeBuilder.buildEnabled(true));

        for (ConnectorObject connectorObject : CollectionUtil.newSet(builder.build())) {
            if (!handler.handle(connectorObject)) {
                // Stop iterating because the handler stopped processing
                break;
            }
        }
        if (options.getPageSize() != null && 0 < options.getPageSize()) {
            logger.info("Paged Search was requested");
            ((SearchResultsHandler) handler).handleResult(new SearchResult("0", 0));
        }
    }
#end

#if ( $op_sync_safe )
    /**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler,
            final OperationOptions options) {
        if (ObjectClass.ALL.equals(objectClass)) {
            //
        } else if (ObjectClass.ACCOUNT.equals(objectClass)) {
            final ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
            builder.setUid("3f50eca0-f5e9-11e3-a3ac-0800200c9a66");
            builder.setName("Foo");
            builder.addAttribute(AttributeBuilder.buildEnabled(true));

            final SyncDeltaBuilder deltaBuilder = new SyncDeltaBuilder();
            deltaBuilder.setObject(builder.build());
            deltaBuilder.setDeltaType(SyncDeltaType.CREATE);
            deltaBuilder.setToken(new SyncToken(10));

            for (SyncDelta connectorObject : CollectionUtil.newSet(deltaBuilder.build())) {
                if (!handler.handle(connectorObject)) {
                    // Stop iterating because the handler stopped processing
                    break;
                }
            }
        } else {
            logger.warn("Sync of type {0} is not supported", configuration.getConnectorMessages()
                    .format(objectClass.getDisplayNameKey(), objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("Sync of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
        ((SyncTokenResultsHandler) handler).handleResult(new SyncToken(10));
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            return new SyncToken(10);
        } else {
            logger.warn("Sync of type {0} is not supported", configuration.getConnectorMessages()
                    .format(objectClass.getDisplayNameKey(), objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("Sync of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }
#end

#if ( $op_test_safe )
    /**
     * {@inheritDoc}
     */
    public void test() {
        logger.ok("Test works well");
    }
#end

#if ( $op_update_safe )
    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) {
        AttributesAccessor attributesAccessor = new AttributesAccessor(replaceAttributes);
        Name newName = attributesAccessor.getName();
        Uid uidAfterUpdate = uid;
        if (newName != null) {
            logger.info("Rename the object {0}:{1} to {2}", objectClass.getObjectClassValue(), uid
                    .getUidValue(), newName.getNameValue());
            uidAfterUpdate = new Uid(newName.getNameValue().toLowerCase(Locale.US));
        }

        if (ObjectClass.ACCOUNT.equals(objectClass)) {

        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
            if (attributesAccessor.hasAttribute("members")) {
                throw new InvalidAttributeValueException(
                        "Requested to update a read only attribute");
            }
        } else {
            logger.warn("Update of type {0} is not supported", configuration.getConnectorMessages()
                    .format(objectClass.getDisplayNameKey(), objectClass.getObjectClassValue()));
            throw new UnsupportedOperationException("Update of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
        return uidAfterUpdate;
    }
#end

#if ( $op_updateattributevalues_safe )
    /**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd,
            OperationOptions options) {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid,
            Set<Attribute> valuesToRemove, OperationOptions options) {
        return uid;
    }
#end
}
