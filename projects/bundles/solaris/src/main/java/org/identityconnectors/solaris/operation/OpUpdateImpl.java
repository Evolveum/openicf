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
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
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

        /*
         * START SUDO
         */
        doSudoStart();
        try {
            updateImpl(uid, replaceAttributes, attrMap);
        } finally {
            /*
             * SUDO STOP
             */
            doSudoReset();
        }

        _log.info("update successful ('{0}', name: '{1}')",
                objclass.toString(), uid.getUidValue());
        
        Uid replaceUid = (Uid) attrMap.get(Uid.NAME);
        Uid newUid = (replaceUid == null) ? uid : replaceUid;
        return newUid;
    }

    private void updateImpl(Uid uid, Set<Attribute> replaceAttributes,
            final Map<String, Attribute> attrMap) {
        /*
         * First acquire the "mutex" for uid creation
         */
        String mutexOut = getConnection().executeCommand(SolarisUtil.getAcquireMutexScript(getConnection()));
        if (mutexOut.contains("ERROR")) {
            throw new ConnectorException("error when acquiring mutex (update operation). Buffer content: <" + mutexOut + ">");
        }
        
        // UPDATE OF ALL ATTRIBUTES EXCEPT PASSWORD
        final SolarisEntry entry = SolarisUtil.forConnectorAttributeSet(uid.getUidValue(), replaceAttributes);
        try {
            UpdateCommand.updateUser(entry, getConnection());
        } finally {
            /*
             * Release the uid "mutex"
             */
            getConnection().executeCommand(SolarisUtil.getMutexReleaseScript(getConnection()));
        }
       
        // PASSWORD UPDATE
        GuardedString passwd = SolarisUtil.getPasswordFromMap(attrMap);
        if (passwd != null) {
            PasswdCommand.configureUserPassword(entry, passwd, getConnection());
        }
    }


}