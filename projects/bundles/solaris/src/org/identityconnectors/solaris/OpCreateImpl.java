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

import java.io.IOException;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;

import static org.identityconnectors.solaris.SolarisHelper.executeCommand;

public class OpCreateImpl extends AbstractOp {
    /** message constants */
    private static final String MSG_NOT_SUPPORTED_OBJECTCLASS = "Object class '%s' is not supported";
    
    public OpCreateImpl(Configuration configuration) {
        super(configuration);
    }
    
    Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        if (!oclass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new IllegalArgumentException(String.format(
                    MSG_NOT_SUPPORTED_OBJECTCLASS, ObjectClass.ACCOUNT_NAME));
        }
        
        SolarisConfiguration config = (SolarisConfiguration) getConfiguration();
        SolarisConnection connection = new SolarisConnection(config);
        
        try {
            connection.send("echo \"ahoj\"");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
}
