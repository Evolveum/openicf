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
package org.identityconnectors.solaris;

import static org.identityconnectors.solaris.SolarisMessages.MSG_NOT_SUPPORTED_OBJECTCLASS;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;

public class OpDeleteImpl extends AbstractOp {

    public OpDeleteImpl(Configuration config, SolarisConnection connection, Log log) {
        super(config, connection, log);
    }
    
    // TODO
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        if (!objClass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new IllegalArgumentException(String.format(
                    MSG_NOT_SUPPORTED_OBJECTCLASS, ObjectClass.ACCOUNT_NAME));
        }
        
        final String accountId = uid.getUidValue();
        getLog().info("delete(''{0}'')", accountId);
        
        // USERDEL accountId
        final String command = String.format("userdel %s", accountId);
        String output = executeCommand(command);
        
        //TODO add handling of exceptions: existing user, etc.
        System.out.println(output);
        getLog().info("userdel(''{0}'')", accountId);

    }
    
    private String executeCommand(String command) {
        return SolarisHelper.executeCommand(getConfiguration(), getConnection(), command);
    }
}
