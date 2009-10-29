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
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.CommonNIS;
import org.identityconnectors.solaris.operation.nis.OpUpdateNISImpl;
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

        if (!objclass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new UnsupportedOperationException("GROUP and other objectclasses are not yet supported.");
        }

        _log.info("update ('{0}', name: '{1}'", objclass.toString(), uid.getUidValue());

        SolarisUtil.controlObjectClassValidity(objclass, acceptOC, getClass());

        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(replaceAttributes));
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(uid.getUidValue(), replaceAttributes);
        final GuardedString passwd = SolarisUtil.getPasswordFromMap(attrMap);
        
        if (SolarisUtil.isNis(getConnection())) {
            invokeNISUpdate(entry, passwd);
        } else {
            invokeNativeUpdate(entry, passwd);
        }

        _log.info("update successful ('{0}', name: '{1}')",
                objclass.toString(), uid.getUidValue());
        
        Uid replaceUid = (Uid) attrMap.get(Uid.NAME);
        Uid newUid = (replaceUid == null) ? uid : replaceUid;
        return newUid;
    }

    /**
     * NIS Update implementation.
     * 
     * Compare with Native update operation: {@see OpUpdateImpl#invokeNativeUpdate(SolarisEntry, GuardedString)}
     */
    private void invokeNISUpdate(final SolarisEntry entry,
            final GuardedString passwd) {
        if (CommonNIS.isDefaultNisPwdDir(getConnection())) {
            invokeNativeUpdate(entry, passwd);
            
            /*
             * START SUDO
             */
            getConnection().doSudoStart();
            try {
                // The user has to be added to the NIS database
                CommonNIS.addNISMake("passwd", getConnection());
            } finally {
                /*
                 * END SUDO
                 */
                getConnection().doSudoReset();
            }
        } else {
            OpUpdateNISImpl.performNIS(entry, getConnection());
        }
    }
    
    /**
     * implementation of the Native Update operation.
     * 
     * Compare with other NIS implementation: {@see OpUpdateImpl#invokeNISUpdate(SolarisEntry, GuardedString)} 
     */
    private void invokeNativeUpdate(final SolarisEntry entry,
            final GuardedString passwd) {
        /*
         * START SUDO
         */
        getConnection().doSudoStart();
        try {
            updateImpl(entry, passwd );
        } finally {
            /*
             * SUDO STOP
             */
            getConnection().doSudoReset();
        }
    }

    private void updateImpl(SolarisEntry entry, GuardedString passwd) {
        /*
         * First acquire the "mutex" for uid creation
         */
        getConnection().executeMutexAcquireScript();
        
        // UPDATE OF ALL ATTRIBUTES EXCEPT PASSWORD
        
        try {
            UpdateCommand.updateUser(entry, getConnection());
        } finally {
            /*
             * Release the uid "mutex"
             */
            getConnection().executeMutexReleaseScript();
        }
       
        // PASSWORD UPDATE
        if (passwd != null) {
            PasswdCommand.configureUserPassword(entry, passwd, getConnection());
        }
    }


}