/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity
 * Connectors are trademarks or registered trademarks of Sun
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd.
 *
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 *
 * Portions Copyrighted 2012 ForgeRock Inc.
 *
 */

package org.forgerock.openicf.connectors.googleapps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
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
 * <p>
 * A connector for Google Apps for your domain. This connector can manage
 * accounts on Google Apps. It also manages nicknames (email aliases) associated
 * with users.
 * <p>
 * 
 * Notes
 * 
 * According to this thread: <br/>
 * http://groups.google.com/group/google-apps-apis/browse_thread/thread/
 * d68b2458b4e84777 <br/>
 * The API does not support a rename operation on an account. You have to delete
 * and receate the account. <br/>
 * Quota can be read - but it appears to ignore it on a create. This is the same
 * behavior as creating through the Google Admin Web GUI (it doesnt even let you
 * set it). I believe this is due to the type of google apps account I have used
 * for testing. The default is to allow the user to have the maxium quota -
 * which is what most organizations will want anyways.
 * 
 * 
 * @author $author$
 * @version $Revision$ $Date$
 */
@ConnectorClass(displayNameKey = "GoogleApps", configurationClass = GoogleAppsConfiguration.class)
public class GoogleAppsConnector implements PoolableConnector, AuthenticateOp, CreateOp, DeleteOp,
        ResolveUsernameOp, SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>,
        SyncOp, TestOp, UpdateAttributeValuesOp, UpdateOp, AttributeNormalizer {

    public static final String ATTR_FAMILY_NAME = "familyName";
    public static final String ATTR_GIVEN_NAME = "givenName";
    public static final String ATTR_QUOTA = "quota";
    public static final String ATTR_NICKNAME_LIST = "nicknames";

    // Group Objects
    public static final String ATTR_MEMBER_LIST = "members";
    public static final String ATTR_OWNER_LIST = "owners";
    public static final String ATTR_GROUP_TEXT_NAME = "groupName";
    public static final String ATTR_GROUP_PERMISSIONS = "groupPermissions";

    /**
     * Setup logging for the {@link GoogleAppsConnector}.
     */
    private static final Log LOGGER = Log.getLog(GoogleAppsConnector.class);

    /**
     * Place holder for the Connection created in the init method
     */
    private GoogleAppsClient connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link GoogleAppsConnector#init(org.identityconnectors.framework.spi.Configuration)}
     * .
     */
    private GoogleAppsConfiguration configuration;

    private GoogleAppsUserOps userOps;
    private GoogleAppsGroupOps groupOps;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration configuration) {
        this.configuration = (GoogleAppsConfiguration) configuration;
        this.userOps = new GoogleAppsUserOps(this);
        this.groupOps = new GoogleAppsGroupOps(this);
    }

    /**
     * Disposes of the {@link GoogleAppsConnector}'s resources.
     * 
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    public void dispose() {
        configuration = null;
        if (connection != null) {
            connection = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkAlive() {

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
    public Uid authenticate(final ObjectClass objectClass, final String userName,
            final GuardedString password, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String userName,
            final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes,
            final OperationOptions options) {
        Name name = AttributeUtil.getNameFromAttributes(createAttributes);

        LOGGER.info("Create {0}", name);
        AttributesAccessor a = new AttributesAccessor(createAttributes);

        if (name == null)
            return null;

        if (ObjectClass.ACCOUNT.equals(objectClass))
            return userOps.createUser(name, a);
        else if (ObjectClass.GROUP.equals(objectClass))
            return groupOps.createGroup(name, a);
        else
            throw new IllegalArgumentException("Unsupported Object Class="
                    + objectClass.getObjectClassValue());

    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        String id = uid.getUidValue();
        LOGGER.info("Deleting {0}", id);
        if (ObjectClass.ACCOUNT.equals(objectClass))
            userOps.delete(id);
        else if (ObjectClass.GROUP.equals(objectClass))
            groupOps.delete(id);
        else
            throw new IllegalArgumentException("Unsupported Object Class="
                    + objectClass.getObjectClassValue());

    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(GoogleAppsConnector.class);

        // Build User Schema
        ObjectClassInfoBuilder ocBuilder = new ObjectClassInfoBuilder();
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.GROUPS);

        ocBuilder.addAttributeInfo(new AttributeInfoBuilder(Name.NAME).setCreateable(true)
                .setUpdateable(false).setRequired(true).build());
        ocBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_FAMILY_NAME).setRequired(true)
                .build());
        ocBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_GIVEN_NAME).setRequired(true)
                .build());
        ocBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_QUOTA, Integer.class)
                .setCreateable(true).setUpdateable(false).build());
        // Multi-valued attributes - nicknames - not required, not returned by
        // default
        ocBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_NICKNAME_LIST)
                .setMultiValued(true).setReturnedByDefault(false).build());

        // Operational Attributes - password and enable/disable status
        ocBuilder
                .addAttributeInfo(AttributeInfoBuilder.build(OperationalAttributes.PASSWORD_NAME,
                        GuardedString.class, EnumSet.of(AttributeInfo.Flags.NOT_READABLE,
                                AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT,
                                AttributeInfo.Flags.REQUIRED)));
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        schemaBuilder.defineObjectClass(ocBuilder.build());

        // Build Group Schema
        ocBuilder = new ObjectClassInfoBuilder();
        ocBuilder.setType(ObjectClass.GROUP_NAME);

        ocBuilder.addAttributeInfo(new AttributeInfoBuilder(Name.NAME).setCreateable(true)
                .setUpdateable(false).setRequired(true).build());
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_GROUP_TEXT_NAME, String.class,
                EnumSet.of(AttributeInfo.Flags.REQUIRED)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(PredefinedAttributes.DESCRIPTION,
                String.class, EnumSet.of(AttributeInfo.Flags.REQUIRED)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_GROUP_PERMISSIONS, String.class,
                EnumSet.of(AttributeInfo.Flags.REQUIRED)));

        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_OWNER_LIST, String.class,
                EnumSet.of(AttributeInfo.Flags.REQUIRED, AttributeInfo.Flags.MULTIVALUED,
                        AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_MEMBER_LIST, String.class,
                EnumSet.of(AttributeInfo.Flags.REQUIRED, AttributeInfo.Flags.MULTIVALUED,
                        AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT)));
        schemaBuilder.defineObjectClass(ocBuilder.build());

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
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass,
            OperationOptions options) {
        return new GoogleAppsFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler,
            OperationOptions options) {
        LOGGER.info("query string = {0} options = {1}", query, options.getAttributesToGet());

        if (ObjectClass.ACCOUNT.equals(objectClass))
            userOps.query(query, handler, options);
        else if (ObjectClass.GROUP.equals(objectClass))
            groupOps.query(query, handler, options);
        else
            throw new IllegalArgumentException("Unsupported objectclass '" + objectClass + "'");
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
    public void test() {
        LOGGER.info("test connection");
        getClient().testConnection();
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) {
        LOGGER.info("Update {0}", replaceAttributes);

        if (uid == null) {
            throw new ConnectorException("Uid attribute is missing!");
        }
        if (replaceAttributes == null || replaceAttributes.size() == 0) {
            throw new IllegalArgumentException("Invalid attributes provided to a update operation.");
        }

        if (ObjectClass.ACCOUNT.equals(objectClass))
            return userOps.updateUser(uid, replaceAttributes, options);
        else if (ObjectClass.GROUP.equals(objectClass))
            return groupOps.updateGroup(uid, replaceAttributes, options);
        else
            throw new IllegalArgumentException("Unsupported objectclass '" + objectClass + "'");

    }

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

    public Attribute normalizeAttribute(final ObjectClass oclass, final Attribute attribute) {
        if (ObjectClass.ACCOUNT.equals(oclass)) {
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                // lowercased id
                return AttributeBuilder.build(attribute.getName(), AttributeUtil.getStringValue(
                        attribute).toLowerCase());
            } else if (attribute.is(PredefinedAttributes.GROUPS_NAME)) {
                // all values should include domain name and be lowercased
                return normalizeDomainAttribute(attribute);
            } else if (attribute.is(ATTR_NICKNAME_LIST)) {
                // all nicknames lowercased and alphabetically ordered
                List<Object> values = attribute.getValue();
                // no values to normalize
                if (values == null)
                    return attribute;

                List<String> normalized = new ArrayList<String>(values.size());
                for (Object value : values) {
                    assert value instanceof String;
                    String strValue = (String) value;
                    normalized.add(strValue.toLowerCase());
                }
                Collections.sort(normalized);
                return AttributeBuilder.build(attribute.getName(), normalized);
            }
        } else if (ObjectClass.GROUP.equals(oclass)) {
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME) || attribute.is(ATTR_MEMBER_LIST)
                    || attribute.is(ATTR_OWNER_LIST)) {
                // all values should include domain name and be lowercased
                return normalizeDomainAttribute(attribute);
            }
        }
        return attribute;
    }

    private Attribute normalizeDomainAttribute(final Attribute attribute) {
        List<Object> values = attribute.getValue();
        // no values to normalize
        if (values == null)
            return attribute;

        List<Object> normalized = new ArrayList<Object>(values.size());
        for (Object value : values) {
            assert value instanceof String;
            String strValue = (String) value;
            normalized.add(normalizeDomainValue(strValue));
        }
        return AttributeBuilder.build(attribute.getName(), normalized);
    }

    private String normalizeDomainValue(String oldValue) {
        String newValue = oldValue;
        if (oldValue.indexOf('@') == -1) {
            // add domain name
            StringBuilder sb = new StringBuilder(oldValue);
            sb.append("@").append(configuration.getDomain());
            newValue = sb.toString();
        }
        return newValue.toLowerCase();
    }

    /**
     * Gets a {@code Connection}
     */
    GoogleAppsClient getClient() {
        try {
            synchronized (this) {
                if (connection == null) {
                    connection = new GoogleAppsClient(configuration);
                }
                return connection;
            }
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
}
