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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class UpdateNativeGroupCommand {
    public static void updateGroup(SolarisEntry group, SolarisConnection conn) {
        conn.doSudoStart();
        try {
            updateGroupImpl(group, conn);
        } finally {
            conn.doSudoReset();
        }
    }

    private static void updateGroupImpl(SolarisEntry group, SolarisConnection conn) {
        final String groupName = group.getName();
        final Set<Attribute> attrs = group.getAttributeSet();
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        final String id = NativeAttribute.ID.getName();
        
        final boolean isGidInAttributes = attrMap.containsKey(id);
        final String gid = isGidInAttributes ? AttributeUtil.getStringValue(attrMap.get(id)) : null;
        if (isGidInAttributes && gid == null) {
            throw new IllegalArgumentException("Attribute 'group id' must not be null. (It is not required.)");
        }
        
        if (gid != null) {
            String groupModCmd = conn.buildCommand("groupmod -g", gid, "'" + groupName + "'");
            conn.executeCommand(groupModCmd, CollectionUtil.newSet("ERROR", "not a unique gid", 
                    "command not found"/*sudo*/, "not allowed to execute"/*sudo*/));
        }
        
        NativeAttribute usersAttrType = NativeAttribute.USERS;
        boolean usersModified = attrMap.containsKey(usersAttrType.getName());
        if (usersModified) {
            List<Object> usersValue = attrMap.get(usersAttrType.getName()).getValue();
            if (usersValue == null) {
                usersValue = Collections.<Object>emptyList();
            }
            AbstractNISOp.changeGroupMembers(groupName, usersValue, false, conn);
        }
    }
}
