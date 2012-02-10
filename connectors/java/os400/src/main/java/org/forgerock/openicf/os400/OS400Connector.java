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
package org.forgerock.openicf.os400;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.*;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SecureAS400;
import com.ibm.as400.access.SystemValue;
import com.ibm.as400.access.User;
import org.identityconnectors.common.security.*;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.common.logging.Log;

/**
 * Main implementation of the OS400 Connector
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@ConnectorClass(
        displayNameKey = "OS400",
        configurationClass = OS400Configuration.class)
public class OS400Connector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>, SyncOp, TestOp, UpdateAttributeValuesOp {
    /**
     * Setup logging for the {@link OS400Connector}.
     */
    private static final Log log = Log.getLog(OS400Connector.class);

    /**
     * Place holder for the {@link Schema} create in the schema() method
     * {@link OS400Connector#schema}.
     */
    private Schema schema;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link OS400Connector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    private OS400Configuration configuration;

    /**
     * Place holder for the Connection created in the init method
     */
    private AS400 as400 = null;
    private int passwordLevel = QPWDLVL_UNFETCHED;
    public static final int QPWDLVL_UNFETCHED = -2;
    public static final int QPWDLVL_UNSET = -1;

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
        this.configuration = (OS400Configuration) configuration1;
        if (as400 == null) {
            try {

                final StringBuilder clear = new StringBuilder();
                GuardedString.Accessor accessor = new GuardedString.Accessor() {
                    public void access(char[] clearChars) {
                        clear.append(clearChars);
                    }
                };
                configuration.getPassword().access(accessor);

                if (configuration.isSsl()) {
                    as400 = new SecureAS400(configuration.getHost(), configuration.getRemoteUser(), clear.toString());
                } else {
                    as400 = new AS400(configuration.getHost(), configuration.getRemoteUser(), clear.toString());
                }
                clear.setLength(0);

                if (as400 == null) {
                    throw new ConnectorException("Connection Exception");
                }
                if (!as400.validateSignon()) {
                    throw new ConnectorSecurityException("Login Error");
                }
                try {
                    as400.setGuiAvailable(false);
                    fetchPasswordLevel();
                } catch (Exception e) {
                }
            } catch (AS400SecurityException e) {
                throw new ConnectorSecurityException(e);
            } catch (IOException e) {
                throw new ConnectorIOException(e);
            }
        }
    }

    /**
     * Disposes of the {@link OS400Connector}'s resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
        configuration = null;
        if (as400 != null) {
            as400.disconnectAllServices();
            as400 = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkAlive() {
        if (null == as400 || !as400.isConnected()) {
            throw new ConnectorException("Connection is not alive");
        }
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
        if (null == schema) {
            SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

            // Users
            ObjectClassInfoBuilder ocBuilder = new ObjectClassInfoBuilder();

            ocBuilder.setType(ObjectClass.ACCOUNT_NAME);
            //The name of the object
            ocBuilder.addAttributeInfo(Name.INFO);
            //ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(AttributeInfo.Flags.REQUIRED, AttributeInfo.Flags.NOT_UPDATEABLE)));
            //User registry name
            //ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(ATTR_FIRST_NAME, String.class, EnumSet.of(AttributeInfo.Flags.NOT_UPDATEABLE)));

            try {
                BeanInfo info = Introspector.getBeanInfo(User.class);
                for (PropertyDescriptor propertyDescriptor : info.getPropertyDescriptors()) {
                    if (FrameworkUtil.isSupportedAttributeType(propertyDescriptor.getPropertyType())) {
                        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(propertyDescriptor.getName(), propertyDescriptor.getPropertyType()));
                    }
                }
            } catch (IntrospectionException e) {

            }

            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.DESCRIPTION);
            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.LAST_LOGIN_DATE);
            ocBuilder.addAttributeInfo(PredefinedAttributeInfos.GROUPS);
            ocBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
            ocBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
            schemaBuilder.defineObjectClass(ocBuilder.build());
            schema = schemaBuilder.build();
        }
        return schema;
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
        throw new UnsupportedOperationException();
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

    protected void fetchPasswordLevel() throws ConnectorException {
        try {
            SystemValue systemValue = new SystemValue(as400, "QPWDLVL");
            Object qpwdlvl = systemValue.getValue();
            if (qpwdlvl instanceof Integer) {
                this.passwordLevel = ((Integer) qpwdlvl).intValue();
            }
        } catch (RequestNotSupportedException e) {
            this.passwordLevel = QPWDLVL_UNSET;
            log.error("QPWDLVL System Value not supported on this resource");
        } catch (Exception e) {
            throw new ConnectorException(e);
        }
    }

    protected boolean runCommand(String command) throws ConnectorException {
        boolean success = false;
        try {
            CommandCall cc = new CommandCall(as400);
            cc.setCommand(command);
            success = cc.run();
            AS400Message[] msgs = cc.getMessageList();
            if (success) {
                //TODO implement
            } else {
                //TODO implement
            }
        } catch (Exception e) {
            throw new ConnectorException(e);
        }
        return success;
    }
}
