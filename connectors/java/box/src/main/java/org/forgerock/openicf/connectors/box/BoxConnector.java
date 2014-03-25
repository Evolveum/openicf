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

package org.forgerock.openicf.connectors.box;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.box.boxjavalibv2.requests.requestobjects.BoxUserDeleteRequestObject;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

import com.box.boxjavalibv2.dao.BoxGroup;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.requests.requestobjects.BoxUserRequestObject;
import com.box.boxjavalibv2.resourcemanagers.IBoxGroupsManager;
import com.box.boxjavalibv2.resourcemanagers.IBoxUsersManager;
import com.box.restclientv2.exceptions.BoxRestException;

/**
 * Main implementation of the Box Connector.
 * 
 */
@ConnectorClass(displayNameKey = "box.connector.display",
        configurationClass = BoxConfiguration.class)
public class BoxConnector implements Connector, CreateOp, DeleteOp, SearchOp<String>, TestOp,
        UpdateOp, SchemaOp, ScriptOnConnectorOp {
    /**
     * Setup logging for the {@link BoxConnector}.
     */
    private static final Log logger = Log.getLog(BoxConnector.class);
    protected static final String GROOVY = "Groovy";
    protected static final String CONNECTOR_ARG = "connector";
    protected static final String BOX_CLIENT_ARG = "boxClient";
    protected static final String FORCE = "force";
    protected static final String NOTIFY = "notify";

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link BoxConnector#init(org.identityconnectors.framework.spi.Configuration)}
     * .
     */
    private BoxConfiguration configuration;

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
     * @param configuration
     *            the new {@link Configuration}
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(final Configuration configuration) {
        this.configuration = (BoxConfiguration) configuration;
    }

    /**
     * Disposes of the {@link BoxConnector}'s resources.
     * 
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    public void dispose() {
        configuration = null;
    }

    /******************
     * SPI Operations
     * 
     * Implement the following operations using the contract and description
     * found in the Javadoc for these methods.
     ******************/

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes,
            final OperationOptions options) {

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            AttributesAccessor attributeAccessor = new AttributesAccessor(createAttributes);
            // TODO need to figure out how to create user. Read the javadoc.
            // TODO examples how to get an attribute and throw error message in
            // case something is wrong

            Name name = attributeAccessor.getName();

            Attribute login = attributeAccessor.find("login");
            String loginString = AttributeUtil.getStringValue(login);

            if (StringUtil.isBlank(loginString)) {
                throw new InvalidAttributeValueException(configuration.getConnectorMessages()
                        .format("ErrorAttributeRequired", "Email attribute is required.", "email"));
            }

            final IBoxUsersManager manager = configuration.getBoxClient().getUsersManager();

            BoxUserRequestObject request =
                    BoxUserRequestObject.createEnterpriseUserRequestObject(loginString, name
                            .getNameValue());

            try {
                return new Uid(manager.createEnterpriseUser(request).getId());
            } catch (BoxRestException e) {
                e.printStackTrace();
            } catch (BoxServerException e) {
                e.printStackTrace();
            } catch (AuthFatalFailureException e) {
                e.printStackTrace();
            }
        } else if (ObjectClass.GROUP.equals(objectClass)) {
            for (Attribute attribute : createAttributes) {
                if (attribute.is(Name.NAME)) {
                    final IBoxGroupsManager manager =
                            configuration.getBoxClient().getGroupsManager();
                    try {
                        return new Uid(manager.createGroup(AttributeUtil.getStringValue(attribute))
                                .getId());
                    } catch (final BoxRestException e) {
                        throw ConnectorException.wrap(e);
                    } catch (final AuthFatalFailureException e) {
                        throw ConnectorException.wrap(e);
                    } catch (final BoxServerException e) {
                        throw ConnectorException.wrap(e);
                    }
                }
            }
            throw new InvalidAttributeValueException("Required attribute __NAME__ is missing");
        } else {
            throw new UnsupportedOperationException();
        }

        return null;
    }

    private <T> T getValueAs(Object source, Class<T> type, T defaultValue) {
        if (null == source || type.isAssignableFrom(source.getClass()) == false) {
            return defaultValue;
        } else {
            return (T) source;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objectClass)) {

            final IBoxUsersManager manager = configuration.getBoxClient().getUsersManager();
            boolean force = getValueAs(options.getOptions().get(FORCE), Boolean.TYPE, false);
            boolean notify = getValueAs(options.getOptions().get(NOTIFY), Boolean.TYPE, false);

            try {
                manager.deleteEnterpriseUser(uid.getUidValue(), BoxUserDeleteRequestObject
                        .deleteEnterpriseUserRequestObject(notify, force));
            } catch (BoxRestException e) {
                throw ConnectorException.wrap(e);
            } catch (BoxServerException e) {
                throw ConnectorException.wrap(e);
            } catch (AuthFatalFailureException e) {
                throw ConnectorException.wrap(e);
            }

        } else if (ObjectClass.GROUP.equals(objectClass)) {
            final IBoxGroupsManager manager = configuration.getBoxClient().getGroupsManager();
            try {
                manager.deleteGroup(uid.getUidValue(), null);
            } catch (final BoxRestException e) {
                throw ConnectorException.wrap(e);
            } catch (final AuthFatalFailureException e) {
                throw ConnectorException.wrap(e);
            } catch (final BoxServerException e) {
                throw ConnectorException.wrap(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(final ObjectClass objectClass,
            OperationOptions options) {
        return new BoxFilterTranslator(objectClass);
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        // this tests the connection is OK.
        if (!Assertions.nullChecked(configuration.getBoxClient(), "BoxClient").isAuthenticated()) {
            throw new IllegalStateException("BoxClient is not authenticated");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Schema schema() {
        SchemaBuilder builder = new SchemaBuilder(BoxConnector.class);

        // User
        ObjectClassInfoBuilder userInfo = new ObjectClassInfoBuilder();
        // - name: The name of this user.
        userInfo.addAttributeInfo(Name.INFO);
        // - login: The email address this user uses to login.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_LOGIN, String.class,
                EnumSet.of(Flags.REQUIRED)));
        // - created_at: The time this user was created.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_CREATED_AT,
                String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)));
        // - modified_at: The time this user was last modified.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_MODIFIED_AT,
                String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)));
        // - role: This user’s enterprise role. Can be admin, coadmin, or user.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_ROLE, String.class,
                EnumSet.of(Flags.NOT_RETURNED_BY_DEFAULT)));
        // - language: The language of this user.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_LANGUAGE));
        // - space_amount: The user’s total available space amount in bytes.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_SPACE_AMOUNT, int.class));
        // - space_used: The amount of space in use by the user.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_SPACE_USED, int.class));
        // - max_upload_size: The maximum individual file size in bytes this
        // user can have.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_MAX_UPLOAD_SIZE,
                int.class));
        // - tracking_codes: An array of key/value pairs set by the user’s
        // admin.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_TRACKING_CODES,
                String.class, EnumSet.of(Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT)));
        // - can_see_managed_users: Whether this user can see other enterprise
        // users in its contact list.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_CAN_SEE_MANAGED_USERS,
                Boolean.class, EnumSet.of(Flags.NOT_RETURNED_BY_DEFAULT)));
        // - is_sync_enabled: Whether or not this user can use Box Sync.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_IS_SYNC_ENABLED,
                Boolean.class, EnumSet.of(Flags.NOT_RETURNED_BY_DEFAULT)));
        // - status: Can be active or inactive.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_STATUS));
        // - job_title: The user’s job title.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_JOB_TITLE));
        // - phone: The user’s phone number.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_PHONE));
        // - address: The user’s address. The user’s address.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_ADDRESS));
        // - avatar_url: URL of this user’s avatar image.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_AVATAR_URL));

        // - is_exempt_from_device_limits: Whether to exempt this user from
        // Enterprise device limits.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(
                BoxUser.FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS, Boolean.class, EnumSet
                        .of(Flags.NOT_RETURNED_BY_DEFAULT)));
        // - is_exempt_from_login_verification: Whether or not this user must
        // use two-factor authentication.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(
                BoxUser.FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION, Boolean.class, EnumSet
                        .of(Flags.NOT_RETURNED_BY_DEFAULT)));
        // - enterprise: Mini representation of this user’s enterprise,
        // including the ID of its enterprise.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_ENTERPRISE, Map.class,
                EnumSet.of(Flags.NOT_RETURNED_BY_DEFAULT)));
        // - my_tags: Tags for all files and folders owned by this user.
        userInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxUser.FIELD_MY_TAGS, String.class,
                EnumSet.of(Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT)));

        ObjectClassInfo user = userInfo.build();
        builder.defineObjectClass(user);
        builder.removeSupportedObjectClass(DeleteOp.class, user);

        // Group
        ObjectClassInfoBuilder groupInfo = new ObjectClassInfoBuilder();
        groupInfo.setType(ObjectClass.GROUP_NAME);
        // - name: The name of this group
        groupInfo.addAttributeInfo(Name.INFO);
        // - created_at: When this groups was created on Box’s servers
        groupInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxGroup.FIELD_CREATED_AT,
                String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)));
        // - modified_at: When this group was last updated on the Box servers
        groupInfo.addAttributeInfo(AttributeInfoBuilder.build(BoxGroup.FIELD_MODIFIED_AT,
                String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)));

        builder.defineObjectClass(groupInfo.build());

        // Define Operation Options
        //builder.defineOperationOption(OperationOptionInfoBuilder.build(NOTIFY, Boolean.TYPE),
        //        CreateOp.class, UpdateOp.class, DeleteOp.class);
        //builder.defineOperationOption(OperationOptionInfoBuilder.build(FORCE, Boolean.TYPE),
        //        DeleteOp.class);

        return builder.build();
    }

    @Override
    public Object runScriptOnConnector(ScriptContext scriptContext,
            OperationOptions operationOptions) {
        final String language = scriptContext.getScriptLanguage();
        if (GROOVY.equalsIgnoreCase(language)) {

            final ScriptExecutor executor =
                    ScriptExecutorFactory.newInstance(language).newScriptExecutor(
                            getClass().getClassLoader(), scriptContext.getScriptText(), false);
            Map<String, Object> scriptArgs = new HashMap<String, Object>();
            scriptArgs.putAll(scriptContext.getScriptArguments());
            scriptArgs.put(CONNECTOR_ARG, this);
            scriptArgs.put(BOX_CLIENT_ARG, configuration.getBoxClient());
            try {
                return executor.execute(scriptArgs);
            } catch (Exception e) {
                throw ConnectorException.wrap(e);
            }
        } else {
            throw new InvalidAttributeValueException(String.format("Language not supported: %s",
                    language));
        }
    }
}
