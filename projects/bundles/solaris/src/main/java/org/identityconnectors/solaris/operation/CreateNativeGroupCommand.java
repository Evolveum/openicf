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

import java.util.List;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.nis.CommonNIS;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class CreateNativeGroupCommand {
    /**
     * Create a native group.
     * @param group The entry that should be created. The new group's name is defined by {@link SolarisEntry#getName()}.
     * @param conn Alive connection.
     */
    public static void create(SolarisEntry group, SolarisConnection conn) {
        conn.doSudoStart();
        try {
            impl(group, conn);
        } finally {
            conn.doSudoReset();
        }
    }

    private static void impl(SolarisEntry group, SolarisConnection conn) {
        final String groupName = group.getName();
        
        // group Id is set only if we're not in saveAs mode
        String groupId = null;
        
        Attribute groupIdAttr = group.searchForAttribute(NativeAttribute.ID);
        if (groupIdAttr != null) {
                groupId = AttributeUtil.getStringValue(groupIdAttr);
        }
        
        final String setGroupId = (StringUtil.isBlank(groupId)) ? "" : ("-g " + groupId);
        final String cmd = conn.buildCommand("groupadd", setGroupId, String.format("'%s'", groupName));
        conn.executeCommand(cmd, CollectionUtil.newSet(
                "ERROR", "Invalid name", // HP errors 
                "not unique", "usage:", "not a valid group name", " exists",  // Red Hat errors
                "command not found", "not allowed to execute") // sudo errors
                );
        
        
        Attribute usersAttribute = group.searchForAttribute(NativeAttribute.USERS);
        if (usersAttribute != null) {
            final List<Object> usersValue = usersAttribute.getValue();
            Assertions.nullCheck(usersValue, "users list");
            CommonNIS.changeGroupMembers(groupName, usersValue, false, conn);
        }
    }

}
