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
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.nis.DeleteNISUserCommand;

public class OpDeleteImpl extends AbstractOp {

    private static final Log _log = Log.getLog(OpDeleteImpl.class);
    
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public OpDeleteImpl(SolarisConnector conn) {
        super(conn);
    }
    
    // TODO
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objClass, acceptOC, getClass());
        
        final String accountId = uid.getUidValue();
        // checkIfUserExists(accountId);
        
        _log.info("delete(''{0}'')", accountId);
        
        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (SolarisUtil.isNis(getConnection())) {
                invokeNISDelete(accountId);
            } else {
                DeleteNativeUserCommand.delete(accountId, getConnection());
            }
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            if (SolarisUtil.isNis(getConnection())) {
                //FIXME
                throw new UnsupportedOperationException("not yet implemented");
            } else {
                DeleteNativeGroupCommand.delete(uid.getUidValue(), getConnection());
            }
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
        if (AbstractNISOp.isDefaultNisPwdDir(getConnection())) {
            DeleteNativeUserCommand.delete(accountId, getConnection());
            /*
             * TODO in adapter, SRA#getDeleteNISUserScript sudo is missing (file another bug?)
             */
            getConnection().doSudoStart();
            try {
                AbstractNISOp.addNISMake("passwd", getConnection());
            } finally {
                getConnection().doSudoReset();
            }
        } else {
            DeleteNISUserCommand.delete(accountId, getConnection());
        }
    }
}