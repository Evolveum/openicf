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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.CommonNIS;
import org.identityconnectors.solaris.operation.nis.OpDeleteNISImpl;

public class OpDeleteImpl extends AbstractOp {

    private static final Log _log = Log.getLog(OpDeleteImpl.class);
    
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public OpDeleteImpl(SolarisConnector conn) {
        super(conn);
    }
    
    // TODO
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objClass, acceptOC, getClass());
        
        if (objClass.is(ObjectClass.GROUP_NAME)) {
            throw new UnsupportedOperationException();
        }
        
        final String accountId = uid.getUidValue();
        // checkIfUserExists(accountId);
        
        _log.info("delete(''{0}'')", accountId);
        
        if (SolarisUtil.isNis(getConnection())) {
            invokeNISDelete(accountId);
        } else {
            invokeNativeDelete(accountId);
        }

        // TODO add handling of exceptions: existing user, etc.
        _log.ok("userdel(''{0}'')", accountId);

    }

    /**
     * NIS Delete implementation.
     * 
     * Compare with Native delete operation: {@see OpDeleteImpl#invokeNativeDelete(String)}
     */
    private void invokeNISDelete(String accountId) {
        // If the password source file is in /etc then use the native
        // utilities
        if (CommonNIS.isDefaultNisPwdDir(getConnection())) {
            invokeNativeDelete(accountId);
            /*
             * START SUDO
             * TODO in adapter, SRA#getDeleteNISUserScript sudo is missing (file another bug?)
             */
            getConnection().doSudoStart();
            try {
                CommonNIS.addNISMake("passwd", getConnection());
            } finally {
                /*
                 * END SUDO
                 */
                getConnection().doSudoReset();
            }
        } else {
            OpDeleteNISImpl.performNIS(accountId, getConnection());
        }
    }

    /**
     * implementation of the Native Delete operation.
     * 
     * Compare with NIS implementation: {@see OpDeleteImpl#invokeNISDelete(String)} 
     */
    private void invokeNativeDelete(final String accountId) {
        // USERDEL accountId
        final String command = getConnection().buildCommand("userdel", ((getConfiguration().isDelHomeDir()) ? "-r" : ""), accountId);
        
        try {
            String output = getConnection().executeCommand(command, CollectionUtil.newSet("ERROR"));
            if (output.contains("does not exist") || output.contains("nknown user")) {
                throw new UnknownUidException("Unknown Uid: " + accountId);
            } else if (output.contains("ERROR")) {
                throw new ConnectorException("Error deleting user '" + accountId + "'.");
            }
            
            output = getConnection().executeCommand("echo $?");
            if (!output.equals("0")) {
                throw new ConnectorException("Error deleting user '" + accountId + "'.");
            }
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }
}