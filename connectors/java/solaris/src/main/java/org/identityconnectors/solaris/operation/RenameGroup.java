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

import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.nis.AbstractNISOp;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * Implementation of Group rename operation.
 * 
 * @author David Adam
 * 
 * The implementation includes both native and NIS version of rename
 */
public class RenameGroup extends AbstractNISOp {
    
    private static final Set<String> rejects = CollectionUtil.newSet("ERROR", "Error", "Invalid name", "not a valid group", "usage:");
    
    /**
     * Rename the given entry for the name given in {@link Name.NAME} attribute.
     * @param group to rename
     * @param conn
     */
    public static void renameGroup(SolarisEntry group, SolarisConnection conn) {
        String newGroupName = getNewGroupRename(group);
        if (newGroupName == null) {
            return;
        }
        
        if (!conn.isNis()) {
            invokeNativeRename(group, conn);
        } else {
            invokeNISRename(group, conn);
        }
    }

    private static void invokeNativeRename(SolarisEntry group, SolarisConnection conn) {
        final String groupModCmd = conn.buildCommand("groupmod");
        String groupName = group.getName();
        String newName = getNewGroupRename(group);
        final String command = new StringBuilder(groupModCmd).append(" -n '").append(newName).append("' '").append(groupName).append("'").toString();
        conn.doSudoStart();
        try {
            conn.executeCommand(command, rejects);
        } finally {
            conn.doSudoReset();
        }
    }
    
    private static void invokeNISRename(SolarisEntry group, SolarisConnection conn) {
        if (conn.isDefaultNisPwdDir()) {
            invokeNativeRename(group, conn);
            
            conn.doSudoStart();
            try {
                AbstractNISOp.addNISMake("group", conn);
            } finally {
                conn.doSudoReset();
            }
        } else {
            conn.doSudoStart();
            try {
                doNISUpdate(group, conn);
            } finally {
                conn.doSudoReset();
            }
        }
    }

    /**
     * Method deals with renaming an NIS group when the NIS source files do not
     * reside in the /etc directory.
     */
    private static void doNISUpdate(SolarisEntry group, SolarisConnection conn) {
        String groupName = group.getName();
        String newName = getNewGroupRename(group);
        
        String removeTmpFilesScript = getRemoveGroupTmpFiles(conn);
        String groupFile = conn.getConfiguration().getNisPwdDir() + "/group";
        String getOwner = initGetOwner(groupFile);
        
        String grepCmd = conn.buildCommand("grep");
        String cpCmd = conn.buildCommand("cp");
        String sedCmd = conn.buildCommand("sed");
        String chownCmd = conn.buildCommand("chown");
        String updateGroup = new StringBuilder()
            .append("WS_GROUPNAME=`" + grepCmd + "\"^" + groupName + ":\" " + groupFile + "`; ")
            .append("WS_NEWGROUP=`" + grepCmd + "\"^" + newName + ":\" " + groupFile + "`; ")
            .append("if [ -n \"$WS_GROUPNAME\" ]; then\n")
              .append("if [ -z \"$WS_NEWGROUP\" ]; then ")
                .append(cpCmd + "-p " + groupFile + " " + tmpGroupfile1 + "; ")
                .append(sedCmd + "'s/^" + groupName + ":/" + newName + ":/' " + tmpGroupfile1 + " > " + tmpGroupfile2 + "\n")
                .append("diff " + groupFile + " " + tmpGroupfile1 + " 2>&1 >/dev/null\n")
                .append("RC=$?; ")
                .append("if [ $RC -eq 0 ]; then\n")
                  .append(cpCmd + "-f " + tmpGroupfile2 + " " + groupFile + "; ")
                  .append(chownCmd + "$OWNER:$GOWNER " + groupFile + "; ")
                .append("else\n")
                  .append("echo \"Error modifying " + groupFile + ", for entry " + groupName + ".\"; ")
                .append("fi; ")
              .append("else ")
                .append("echo \"Error: " + newName + " already exists in " + groupFile + ".\";\n")
              .append("fi; ")
            .append("else ")
              .append("echo \"Error: " + groupName + " not found in " + groupFile + ".\"; ")
            .append("fi").toString();
        
        conn.executeMutexAcquireScript(grpMutexFile, tmpGrpMutexFile, grpPidFile);
        try {
            conn.executeCommand(getOwner);
            conn.executeCommand(removeTmpFilesScript);
            conn.executeCommand(updateGroup, rejects);
            conn.executeCommand(removeTmpFilesScript);
            
            AbstractNISOp.addNISMake("group", conn);
        } finally {
            conn.executeMutexReleaseScript(grpMutexFile);
        }
    }

    /**
     * @param group
     * @return null if new name doesn't exist, or is equal to the original name.
     */
    private static String getNewGroupRename(SolarisEntry group) {
        Attribute newName = group.searchForAttribute(NativeAttribute.NAME);
        if (newName == null) {
            return null;
        }
        String newNameValue = AttributeUtil.getStringValue(newName);
        if (newNameValue.equals(group.getName())) {
            return null;
        }
        return newNameValue;
    }
}
