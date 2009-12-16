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
package org.identityconnectors.solaris.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.CreateNISGroup;
import org.identityconnectors.solaris.operation.nis.CreateNISUser;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class SolarisCreate extends AbstractOp {

    private static final Log _log = Log.getLog(SolarisCreate.class);
    
    private SolarisConnection connection;
    
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    
    public SolarisCreate(SolarisConnector connector) {
        super(connector);
        connection = connector.getConnection();
    }

    /**
     * central method of Create operation. Internally it delegates the work to 
     * to NIS / Non-NIS (native) subordinates.
     */
    public Uid create(ObjectClass oclass, final Set<Attribute> attrs, final OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(oclass, acceptOC, getClass());
        
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));

        final Name name = (Name) attrMap.get(Name.NAME);
        final String entryName = name.getNameValue();

        _log.info("~~~~~~~ create {0}(''{1}'') ~~~~~~~", oclass.getObjectClassValue(), entryName);
        
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(name.getNameValue(), oclass, attrs);
        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            final GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
            if (connection.isNis()) {
                invokeNISUserCreate(entry, password);
            } else {
                invokeNativeUserCreate(entry, password);
            }
        } else if (oclass.is(ObjectClass.GROUP_NAME)) {
            if (connection.isNis()) {
                invokeNISGroupCreate(entry);
            } else {
                invokeNativeGroupCreate(entry);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        
        return new Uid(entryName);
    }

    /**
     * Compare with Native create operation:
     * {@link SolarisCreate#invokeNativeGroupCreate(SolarisEntry)}
     */
    private void invokeNISGroupCreate(final SolarisEntry group) {
        if (connection.isDefaultNisPwdDir()) {
            invokeNativeGroupCreate(group);

            AbstractNISOp.addNISMake("group", connection);
        } else {
            CreateNISGroup.create(group, connection);
        }
    }

    /**
     * Compare with other NIS implementation counterpart:
     * {@link SolarisCreate#invokeNISGroupCreate(SolarisEntry)}
     */
    private void invokeNativeGroupCreate(final SolarisEntry group) {
        CreateNativeGroup.create(group, connection);
    }

    /**
     * Compare with Native create operation 
     * {@link SolarisCreate#invokeNativeUserCreate(SolarisEntry, GuardedString)}
     */
    private void invokeNISUserCreate(SolarisEntry entry, GuardedString password) {
        
        if (connection.isDefaultNisPwdDir()) {
            invokeNativeUserCreate(entry, password);
            
            // The user has to be added to the NIS database
            connection.doSudoStart();
            try {
                AbstractNISOp.addNISMake("passwd", connection);
            } finally {
                connection.doSudoReset();
            }
        } else {
            CreateNISUser.performNIS(entry, connection);
        }
    }
    

    /**
     * Compare with other NIS implementation counterpart: 
     * {@see OpCreateImpl#invokeNISUserCreate(SolarisEntry, GuardedString)}
     */
    private void invokeNativeUserCreate(SolarisEntry entry, GuardedString password) {
        CreateNativeUser.createUser(entry, password, connection);
    }
}
