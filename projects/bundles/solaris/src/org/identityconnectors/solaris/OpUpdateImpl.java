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

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.operation.AbstractOp;

public class OpUpdateImpl extends AbstractOp {

    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT, ObjectClass.GROUP};
    
    public OpUpdateImpl(SolarisConfiguration configuration,
            SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }

    public Uid update(ObjectClass objclass, Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objclass, acceptOC, getClass());
        
        // Read only list of attributes
//        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(
//                AttributeUtil.toMap(replaceAttributes));

        
        return null;
    }
}
