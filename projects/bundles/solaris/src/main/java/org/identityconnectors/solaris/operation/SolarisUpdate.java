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
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.UpdateNISGroup;
import org.identityconnectors.solaris.operation.nis.UpdateNISUser;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * Implementation of update SPI operation
 * 
 * @author David Adam
 */
public class SolarisUpdate extends AbstractOp {
    
    private static final Log log = Log.getLog(SolarisUpdate.class);
    
    private SolarisConnection connection;

    /** These objectclasses are valid for update operation. */
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public SolarisUpdate(SolarisConnector connector) {
        super(connector);
        connection = connector.getConnection();
    }

    /** main update method */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {

        log.info("update ('{0}', name: '{1}'", objclass.toString(), uid.getUidValue());

        SolarisUtil.controlObjectClassValidity(objclass, acceptOC, getClass());

        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(replaceAttributes));
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(uid.getUidValue(), objclass, replaceAttributes);
        
        final String newName = fetchName(entry);
        if (objclass.is(ObjectClass.ACCOUNT_NAME)) {
            GuardedString passwd = null; 
            Attribute attrPasswd = attrMap.get(OperationalAttributes.PASSWORD_NAME);
            if (attrPasswd != null) {
                passwd = AttributeUtil.getGuardedStringValue(attrPasswd); 
            }
            
            if (connection.isNis()) {
                // NIS doesn't control duplicate account names so we need to do it in advance
                if (StringUtil.isNotBlank(newName) && 
                        SolarisUtil.exists(objclass, new SolarisEntry.Builder(newName).build(), connection) &&
                        !newName.equals(entry.getName())) {
                    throw new AlreadyExistsException("Account already exits: " + entry.getName());
                }
                
                invokeNISUserUpdate(entry, passwd);
            } else {
                UpdateNativeUser.updateUser(entry, passwd, connection);
            }
        } else if (objclass.is(ObjectClass.GROUP_NAME)) {
            if (connection.isNis()) {
                // NIS doesn't control duplicate account names so we need to do it in advance
                if (StringUtil.isNotBlank(newName) && 
                        SolarisUtil.exists(objclass, new SolarisEntry.Builder(newName).build(), connection) &&
                        !newName.equals(entry.getName())) {
                    throw new AlreadyExistsException("Group already exits: " + entry.getName());
                }
                
                invokeNISGroupUpdate(entry);
            } else {
                UpdateNativeGroup.updateGroup(entry, connection);
            }
            
            // Rename was separate operation in Adapter
            RenameGroup.renameGroup(entry, connection);
        } else {
            throw new UnsupportedOperationException();
        }

        log.info("update successful ('{0}', name: '{1}')",
                objclass.toString(), uid.getUidValue());
        
        Uid newUid = uid; // uid is the uid before update.
        // if new uid is in replaceAttributes, return the updated uid
        if (attrMap.get(Name.NAME) != null) {
            String name = ((Name) attrMap.get(Name.NAME)).getNameValue();
            newUid = new Uid(name);
        }
        return newUid;
    }

    private String fetchName(SolarisEntry entry) {
        String newName = null;
        Attribute newNameAttr = entry.searchForAttribute(NativeAttribute.NAME);
        if (newNameAttr != null) {
            newName = AttributeUtil.getStringValue(newNameAttr);
        }
        return newName;
    }

    /**
     * Compare with Native update operation: {@see OpUpdateImpl#invokeNativeGroupUpdate(SolarisEntry)}
     */
    private void invokeNISGroupUpdate(SolarisEntry groupEntry) {
        if (connection.isDefaultNisPwdDir()) {
            UpdateNativeGroup.updateGroup(groupEntry, connection);
            
            connection.doSudoStart();
            try {
                AbstractNISOp.addNISMake("group", connection);
            } finally {
                connection.doSudoReset();
            }
        } else {
            UpdateNISGroup.updateGroup(groupEntry, connection);
        }
    }

    /**
     * Compare with Native update operation: {@see OpUpdateImpl#invokeNativeUserUpdate(SolarisEntry, GuardedString)}
     */
    private void invokeNISUserUpdate(final SolarisEntry userEntry,
            final GuardedString passwd) {
        if (connection.isDefaultNisPwdDir()) {
            UpdateNativeUser.updateUser(userEntry, passwd, connection);
            
            connection.doSudoStart();
            try {
                // The user has to be added to the NIS database
                AbstractNISOp.addNISMake("passwd", connection);
            } finally {
                connection.doSudoReset();
            }
        } else {
            UpdateNISUser.updateUser(userEntry, passwd, connection);
        }
    }
}