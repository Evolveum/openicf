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
 *
 * Portions Copyrighted 2012 Evolveum, Radovan Semancik
 */
package org.identityconnectors.solaris.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.CreateNISGroup;
import org.identityconnectors.solaris.operation.nis.CreateNISUser;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class SolarisCreate extends AbstractOp {

    private static final Log logger = Log.getLog(SolarisCreate.class);

    private final SolarisConnection connection;
    private boolean sunCompat;

    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public SolarisCreate(final SolarisConnector connector) {
        super(connector);
        connection = connector.getConnection();
        this.sunCompat = ((SolarisConfiguration) connector.getConfiguration()).getSunCompat();
    }

    /**
     * central method of Create operation. Internally it delegates the work to
     * to NIS / Non-NIS (native) subordinates.
     */
    public Uid create(ObjectClass oclass, final Set<Attribute> attrs, final OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(oclass, acceptOC, getClass(), connection
                .getConfiguration());

        // Read only list of attributes
        final Map<String, Attribute> attrMap =
                new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));

        final Name name = (Name) attrMap.get(Name.NAME);
        if (name == null || StringUtil.isBlank(name.getNameValue())) {
            throw new IllegalArgumentException("Name attribute is missing.");
        }
        final String entryName = name.getNameValue();

        logger.info("~~~~~~~ create {0}(''{1}'') ~~~~~~~", oclass.getObjectClassValue(), entryName);

        final SolarisEntry entry =
                SolarisUtil.forConnectorAttributeSet(name.getNameValue(), oclass, attrs, sunCompat);
        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            GuardedString password = null;
            Attribute attrPasswd = attrMap.get(OperationalAttributes.PASSWORD_NAME);
            if (attrPasswd != null) {
                password = AttributeUtil.getGuardedStringValue(attrPasswd);
            }

            if (connection.isNis()) {
                // NIS doesn't control duplicate account names so we need to do
                // it in advance
                if (SolarisUtil.exists(oclass, entry, connection)) {
                    throw new AlreadyExistsException("Account already exits: " + entry.getName());
                }
                invokeNISUserCreate(entry, password);
            } else {
                CreateNativeUser.createUser(entry, password, connection);
            }
        } else if (oclass.is(ObjectClass.GROUP_NAME)) {
            if (connection.isNis()) {
                // NIS doesn't control duplicate account names so we need to do
                // it in advance
                if (SolarisUtil.exists(oclass, entry, connection)) {
                    throw new AlreadyExistsException("Group already exits: " + entry.getName());
                }
                invokeNISGroupCreate(entry);
            } else {
                CreateNativeGroup.create(entry, connection);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        return new Uid(entryName);
    }

    /**
     * Compare with Native create operation:
     * {@link CreateNativeGroup#create(SolarisEntry,SolarisConnection)}.
     */
    private void invokeNISGroupCreate(final SolarisEntry group) {
        if (connection.isDefaultNisPwdDir()) {
            CreateNativeGroup.create(group, connection);

            AbstractNISOp.addNISMake("group", connection);
        } else {
            CreateNISGroup.create(group, connection);
        }
    }

    /**
     * Compare with Native create operation
     * {@link CreateNativeGroup#create(SolarisEntry,SolarisConnection)}.
     */
    private void invokeNISUserCreate(SolarisEntry entry, GuardedString password) {

        if (connection.isDefaultNisPwdDir()) {
            CreateNativeUser.createUser(entry, password, connection);

            // The user has to be added to the NIS database
            connection.doSudoStart();
            try {
                AbstractNISOp.addNISMake("passwd", connection);
            } finally {
                connection.doSudoReset();
            }
        } else {
            CreateNISUser.performNIS(entry, password, connection);
        }
    }
}
