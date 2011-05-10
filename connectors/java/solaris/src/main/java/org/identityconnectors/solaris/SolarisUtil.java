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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.ConnectorAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.AbstractOp;
import org.identityconnectors.solaris.operation.search.SolarisEntries;
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
    
    public static void controlObjectClassValidity(ObjectClass oclass, ObjectClass[] acceptedObjectClasses, Class<? extends AbstractOp> operation) {
        for (ObjectClass objectClass : acceptedObjectClasses) {
            if (objectClass.equals(oclass)) {
                return;
            }
        }
        
        throw new IllegalArgumentException(String.format(
                MSG_NOT_SUPPORTED_OBJECTCLASS, oclass, operation.getName()));
    }

    /**
     * Translate the given connector attributes to their native solaris
     * counterpart.
     * 
     * Contract: PASSWORD and UID shouldn't be contained in the attribute set,
     * they supposed to be handled separately, outside of SolarisEntry.
     * 
     * @param entryName the entry's name (can be either GROUP or ACCOUNT)
     * @param oclass object class type
     * @param attrs connector attributes
     * @return the translated attributes encapsulated
     */
    public static SolarisEntry forConnectorAttributeSet(String entryName, ObjectClass oclass, Set<Attribute> attrs) {
        // translate connector attributes to native counterparts
        final SolarisEntry.Builder builder = new SolarisEntry.Builder(entryName);
        for (Attribute attribute : attrs) {
            final ConnectorAttribute accAttrName = (oclass.is(ObjectClass.ACCOUNT_NAME)) ? AccountAttribute.forAttributeName(attribute.getName()) : GroupAttribute.forAttributeName(attribute.getName());
            if (accAttrName != null) {
                builder.addAttr(accAttrName.getNative(), attribute.getValue());
            } else if (!attribute.getName().equals(OperationalAttributes.PASSWORD_NAME) && !attribute.getName().equals(Uid.NAME)) {
                // FIXME: do we really need this exception here? It'd be way more beautiful without if-s.
                //TODO it might be a more beautiful sort out this error (filter the attributes in layers above this class).
                throw new ConnectorException("ERROR: Unsupported attribute: " + attribute.getName());
            }
        }
        return builder.build();
    }

    /**
     * Search for the existence of given entry based on its unique identificator.
     * @param entryType type of entry 
     * @param entry check the presence of this entry
     * @param conn 
     * @return true if entry exists, false otherwise
     */
    public static boolean exists(ObjectClass entryType, SolarisEntry entry, SolarisConnection conn) {
        Iterator<SolarisEntry> result = null;
        if (entryType.is(ObjectClass.ACCOUNT_NAME)) {
            return SolarisEntries.getAccount(entry.getName(), EnumSet.of(NativeAttribute.NAME), conn) != null;
        } else if (entryType.is(ObjectClass.GROUP_NAME)) {
            result = SolarisEntries.getAllGroups(EnumSet.of(NativeAttribute.NAME), conn);
        } else {
            throw new ConnectorException("Non existing object class: " + entryType.toString());
        }
        while (result != null && result.hasNext()) {
            SolarisEntry it = result.next();
            if (it.getName().equals(entry.getName())) {
                return true;
            }
        }
        return false;
    }
}
