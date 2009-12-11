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

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.DeleteNISGroupCommand;
import org.identityconnectors.solaris.operation.nis.DeleteNISUserCommand;

public class OpDeleteImpl extends AbstractOp {

    private static final Log _log = Log.getLog(OpDeleteImpl.class);
    
    private SolarisConnection connection;
    
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public OpDeleteImpl(SolarisConnector connector) {
        super(connector);
        connection = connector.getConnection();
    }
    
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objClass, acceptOC, getClass());
        
        final String entryName = uid.getUidValue();
        
        _log.info("{0} delete(''{1}'')",((objClass.is(ObjectClass.ACCOUNT_NAME))? "account" : "group") , entryName);
        
        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (connection.isNis()) {
                invokeNISUserDelete(entryName);
            } else {
                invokeNativeUserDelete(entryName);
            }
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            if (connection.isNis()) {
                invokeNISGroupDelete(entryName);
            } else {
                invokeNativeGroupDelete(entryName);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        // TODO add handling of exceptions: existing user, etc.
        _log.ok("userdel(''{0}'')", entryName);

    }

    /**
     * compare with NIS delete operation: {@see OpDeleteImpl#invokeNISGroupDelete(String)}
     */
    private void invokeNativeGroupDelete(String groupName) {
        DeleteNativeGroupCommand.delete(groupName, connection);
    }

    /**
     * compare with Native delete operation: {@see OpDeleteImpl#invokeNativeGroupDelete(Uid)}
     */
    private void invokeNISGroupDelete(String groupName) {
        if (connection.isDefaultNisPwdDir()) {
            invokeNativeGroupDelete(groupName);
            
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
            DeleteNISGroupCommand.delete(groupName, connection);
        }
    }

    /**
     * compare with NIS delete operation: {@see OpDeleteImpl#invokeNISUserDelete(String)}
     */
    private void invokeNativeUserDelete(final String accountName) {
        DeleteNativeUserCommand.delete(accountName, connection);
    }

    /**
     * Compare with Native delete operation: {@see OpDeleteImpl#invokeNativeDelete(String)}
     */
    private void invokeNISUserDelete(String accountName) {
        // If the password source file is in /etc then use the native
        // utilities
        if (connection.isDefaultNisPwdDir()) {
            invokeNativeUserDelete(accountName);
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
            DeleteNISUserCommand.delete(accountName, connection);
        }
    }
}