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
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.UpdateNISUserCommand;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * Implementation of update SPI operation
 * 
 * @author David Adam
 */
public class OpUpdateImpl extends AbstractOp {
    
    private static final Log _log = Log.getLog(OpUpdateImpl.class);

    /** These objectclasses are valid for update operation. */
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public OpUpdateImpl(SolarisConnector conn) {
        super(conn);
    }

    /** main update method */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {

        _log.info("update ('{0}', name: '{1}'", objclass.toString(), uid.getUidValue());

        SolarisUtil.controlObjectClassValidity(objclass, acceptOC, getClass());

        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(replaceAttributes));
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(uid.getUidValue(), objclass, replaceAttributes);
        final GuardedString passwd = SolarisUtil.getPasswordFromMap(attrMap);

        if (objclass.is(ObjectClass.ACCOUNT_NAME)) {
            if (SolarisUtil.isNis(getConnection())) {
                invokeNISUserUpdate(entry, passwd);
            } else {
                invokeNativeUserUpdate(entry, passwd);
            }
        } else if (objclass.is(ObjectClass.GROUP_NAME)) {
            if (SolarisUtil.isNis(getConnection())) {
                throw new UnsupportedOperationException(); //TODO
            } else {
                throw new UnsupportedOperationException(); //TODO
            }
        } else {
            throw new UnsupportedOperationException();
        }

        _log.info("update successful ('{0}', name: '{1}')",
                objclass.toString(), uid.getUidValue());
        
        Uid newUid = uid; // uid is the uid before update.
        // if new uid is in replaceAttributes, return the updated uid
        if (attrMap.get(Name.NAME) != null) {
            String name = ((Name) attrMap.get(Name.NAME)).getNameValue();
            newUid = new Uid(name);
        }
        return newUid;
    }

    /**
     * NIS Update implementation.
     * 
     * Compare with Native update operation: {@see OpUpdateImpl#invokeNativeUpdate(SolarisEntry, GuardedString)}
     */
    private void invokeNISUserUpdate(final SolarisEntry entry,
            final GuardedString passwd) {
        if (AbstractNISOp.isDefaultNisPwdDir(getConnection())) {
            invokeNativeUserUpdate(entry, passwd);
            
            getConnection().doSudoStart();
            try {
                // The user has to be added to the NIS database
                AbstractNISOp.addNISMake("passwd", getConnection());
            } finally {
                getConnection().doSudoReset();
            }
        } else {
            UpdateNISUserCommand.performNIS(entry, getConnection());
        }
    }
    
    /**
     * implementation of the Native Update operation.
     * 
     * Compare with other NIS implementation: {@see OpUpdateImpl#invokeNISUpdate(SolarisEntry, GuardedString)} 
     */
    private void invokeNativeUserUpdate(final SolarisEntry entry, final GuardedString passwd) {
        UpdateNativeUserCommand.updateUser(entry, passwd, getConnection());
    }
}