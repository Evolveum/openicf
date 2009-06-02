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

import java.util.Map;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;


/** helper class for Solaris specific operations */
public class SolarisHelper {
    
    /** helper method for getting the password from an attribute map */
    public static GuardedString getPasswordFromMap(Map<String, Attribute> attrMap) {
        Attribute attrPasswd = attrMap.get(OperationalAttributes.PASSWORD_NAME);
        if (attrPasswd == null) {
            String msg = String.format("password should be of type GuardedString, inside attribute map: %s",
                    attrMap.toString());
            
            throw new IllegalArgumentException(msg);
        }
        return AttributeUtil.getGuardedStringValue(attrPasswd);
    }
    
    public static void controlObjectClassValidity(ObjectClass oclass) {
        if (!(oclass.is(ObjectClass.ACCOUNT_NAME) || oclass.is(ObjectClass.GROUP_NAME))) {
            throw new IllegalArgumentException(String.format(
                    MSG_NOT_SUPPORTED_OBJECTCLASS, oclass, ObjectClass.ACCOUNT_NAME, ObjectClass.GROUP_NAME));
        }
    }
}
