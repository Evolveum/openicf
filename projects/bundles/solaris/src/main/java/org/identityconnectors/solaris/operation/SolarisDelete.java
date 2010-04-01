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

import java.util.EnumSet;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.DeleteNISGroup;
import org.identityconnectors.solaris.operation.nis.DeleteNISUser;
import org.identityconnectors.solaris.operation.search.SolarisEntries;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class SolarisDelete extends AbstractOp {

    private static final Log log = Log.getLog(SolarisDelete.class);
    
    private SolarisConnection connection;
    
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public SolarisDelete(SolarisConnector connector) {
        super(connector);
        connection = connector.getConnection();
    }
    
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objClass, acceptOC, getClass());
        
        final String entryName = uid.getUidValue();
        
        log.info("{0} delete(''{1}'')",((objClass.is(ObjectClass.ACCOUNT_NAME))? "account" : "group") , entryName);
        
        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (connection.isNis()) {
                // NIS is not able to signal that account is missing, so search in advance:
                SolarisEntry searchedEntry = SolarisEntries.getAccount(entryName, EnumSet.of(NativeAttribute.NAME), connection);
                if (searchedEntry == null) {
                    throw new UnknownUidException("user does not exist: " + entryName);
                }
                
                invokeNISUserDelete(entryName);
            } else {
                DeleteNativeUser.delete(entryName, connection);
            }
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            if (connection.isNis()) {
                // NIS is not able to signal that group is missing, so search in advance:
                SolarisEntry searchedEntry = SolarisEntries.getGroup(entryName, EnumSet.of(NativeAttribute.NAME), connection);
                if (searchedEntry == null) {
                    throw new UnknownUidException("user does not exist: " + entryName);
                }
                
                invokeNISGroupDelete(entryName);
            } else {
                DeleteNativeGroup.delete(entryName, connection);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        log.ok("userdel(''{0}'')", entryName);

    }

    /**
     * compare with Native delete operation: {@see OpDeleteImpl#invokeNativeGroupDelete(Uid)}
     */
    private void invokeNISGroupDelete(String groupName) {
        if (connection.isDefaultNisPwdDir()) {
            DeleteNativeGroup.delete(groupName, connection);
            
            /*
             * TODO in adapter, SRA#getDeleteNISUserScript sudo is missing (file another bug?)
             */
            connection.doSudoStart();
            try {
                AbstractNISOp.addNISMake("group", connection);
            } finally {
                connection.doSudoReset();
            }
        } else {
            DeleteNISGroup.delete(groupName, connection);
        }
    }

    /**
     * Compare with Native delete operation: {@see OpDeleteImpl#invokeNativeDelete(String)}
     */
    private void invokeNISUserDelete(String accountName) {
        // If the password source file is in /etc then use the native
        // utilities
        if (connection.isDefaultNisPwdDir()) {
            DeleteNativeUser.delete(accountName, connection);
            /*
             * TODO in adapter, SRA#getDeleteNISUserScript sudo is missing (file another bug?)
             */
            connection.doSudoStart();
            try {
                AbstractNISOp.addNISMake("passwd", connection);
            } finally {
                connection.doSudoReset();
            }
        } else {
            DeleteNISUser.delete(accountName, connection);
        }
    }
}