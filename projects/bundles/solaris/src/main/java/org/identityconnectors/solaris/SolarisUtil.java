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
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.operation.AbstractOp;
import org.identityconnectors.solaris.operation.search.SolarisEntry;


/** helper class for Solaris specific operations */
public class SolarisUtil {
    
    /** Maximum number of characters per line in Solaris shells */
    public static final int DEFAULT_LIMIT = 120;
    
    private static StringBuilder limitString(StringBuilder data, int limit) {
        StringBuilder result = null;
        if (data.length() > limit) {
            result = new StringBuilder(limit);
            result.append(data.substring(0, limit));
            result.append("\\\n"); // /<newline> separator of Unix command line cmds.
            
            final String remainder = data.substring(limit, data.length()); 
            if (remainder.length() > limit) {
                // TODO performance: might be a more effective way to handle this copying. Maybe skip copying and pass the respective part of stringbuffer directly to the recursive call.
                StringBuilder sbtmp = new StringBuilder();
                sbtmp.append(remainder);
                result.append(limitString(sbtmp, limit));
            } else {
                result.append(remainder);
            }
        } else {
            return data;
        }
        return result;
    }

    /**
     * Cut the command into pieces, so it doesn't have a longer line than the given DEFAULT_LIMIT
     * @param data
     * @return
     */
    public static String limitString(StringBuilder data) {
        return limitString(data, DEFAULT_LIMIT /* == max length of line from SolarisResourceAdapter#getUpdateNativeUserScript(), line userattribparams */).toString();
    }
    
    /** helper method for getting the password from an attribute map */
    public static GuardedString getPasswordFromMap(Map<String, Attribute> attrMap) {
        Attribute attrPasswd = attrMap.get(OperationalAttributes.PASSWORD_NAME);
        if (attrPasswd == null) {
            throw new IllegalArgumentException("Password missing from attribute map");
        }
        return AttributeUtil.getGuardedStringValue(attrPasswd);
    }
    
    public static void controlObjectClassValidity(ObjectClass oclass, ObjectClass[] acceptedObjectClasses, Class<? extends AbstractOp> operation) {
        for (ObjectClass objectClass : acceptedObjectClasses) {
            if (objectClass.equals(oclass)) {
                return;
            }
        }
        
        throw new IllegalArgumentException(String.format(
                MSG_NOT_SUPPORTED_OBJECTCLASS, oclass, operation.getName()));
    }
    
    public static SolarisEntry forConnectorAttributeSet(String userName, Set<Attribute> attrs) {
        // translate connector attributes to native counterparts
        final SolarisEntry.Builder builder = new SolarisEntry.Builder(userName);
        for (Attribute attribute : attrs) {
            final AccountAttribute accAttrName = AccountAttribute.forAttributeName(attribute.getName());
            if (accAttrName != null) {
                builder.addAttr(accAttrName.getNative(), attribute.getValue());
            } else if (!attribute.getName().equals(OperationalAttributes.PASSWORD_NAME) && !attribute.getName().equals(Uid.NAME)) {
                //TODO it might be a more beautiful sort out this error (filter the attributes in layers above this class).
                throw new ConnectorException("Unsupported attribute in update(user='" + userName + "'): " + attribute.getName());
            }
        }
        return builder.build();
    }
    
    public static boolean isNis(SolarisConnection conn) {
        final String sysDB = conn.getConfiguration().getSysDbType();
        boolean sysDbType = false;
        if ((sysDB != null) && sysDB.equalsIgnoreCase("nis")) {
            sysDbType = true;
        }
        return sysDbType;
    }
}
