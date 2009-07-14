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

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.operation.AbstractOp;

public class OpAuthenticateImpl extends AbstractOp {

    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT};
    
    public OpAuthenticateImpl(SolarisConfiguration configuration, SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }

    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objectClass, acceptOC, getClass());
        
        SolarisConnection connection = null;
        try {
            connection = new SolarisConnection(getConfiguration(), username, password);
        } catch (RuntimeException ex) {
            getLog().warn("Failed to authenticate user ''{0}'' RuntimeException thrown during authentication.", username);
            // in case of invalid credentials propagate the exception
            throw ex;
        } finally {
            if (connection != null) {
                connection.dispose();
            }
        }
        
        getLog().ok("User ''{0}'' succesfully authenticated", username);
        return new Uid(username);
    }

}
