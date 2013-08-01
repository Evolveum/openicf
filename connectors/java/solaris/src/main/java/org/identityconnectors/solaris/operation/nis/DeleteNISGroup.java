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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.operation.nis;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.solaris.SolarisConnection;

public class DeleteNISGroup extends AbstractNISOp {

    public static void delete(String groupName, SolarisConnection connection) {

        try {
            connection.doSudoStart();

            try {
                connection.executeMutexAcquireScript(GRP_MUTEX_FILE, TMP_GRP_MUTEX_FILE, GRP_PID_FILE);

                deleteImpl(groupName, connection);
            } finally {
                connection.executeMutexReleaseScript(GRP_MUTEX_FILE);
            }
        } finally {
            connection.doSudoReset();
        }
    }

    private static void deleteImpl(String groupName, SolarisConnection connection) {

        final String removeTmpFilesScript = getRemoveGroupTmpFiles(connection);
        connection.executeCommand(removeTmpFilesScript);

        final String pwddir = connection.getConfiguration().getNisPwdDir();
        final String groupFile = pwddir + "/group";
        final String getOwner = initGetOwner(groupFile);
        final String workScript = initWorkScript(groupName, groupFile, connection);
        // Add script to remove the entry from the file
        connection.executeCommand(getOwner);
        String out = connection.executeCommand(workScript);
        out = out.trim();
        if (StringUtil.isNotBlank(out)) {
            if (out.contains(">")) {
                out = out.substring(out.lastIndexOf(">") + 1, out.length());
                out = out.trim();
            }
            if (StringUtil.isNotBlank(out)) {
                throw new ConnectorException("ERROR: " + out);
            }
        }

        out = connection.executeCommand("echo $?; ");
        if (StringUtil.isNotBlank(out) && !out.equals("0")) {
            throw new UnknownUidException("Error deleting group: " + groupName);
        }

        connection.executeCommand(removeTmpFilesScript);

        // The user has to be added to the NIS database
        AbstractNISOp.addNISMake("group", connection);
    }

    private static String initWorkScript(String groupName, String groupFile, SolarisConnection connection) {
        StringBuilder bldr = new StringBuilder();
        String grepCmd = connection.buildCommand(false, "grep");
        String cpCmd = connection.buildCommand(false, "cp");
        String mvCmd = connection.buildCommand(false, "mv");
        String chownCmd = connection.buildCommand(false, "chown");

        bldr.append("WS_GROUPNAME=`" + grepCmd + "\"^" + groupName + ":\" " + groupFile + "`; ");
        bldr.append("if [ -n \"$WS_GROUPNAME\" ]; then\n");
            bldr.append(cpCmd + "-p " + groupFile + " " + TMP_GROUPFILE_1 + "; ");
            bldr.append(grepCmd + "-v \"^" + groupName + ":\" " + groupFile + " > " + TMP_GROUPFILE_2 + "; ");
            bldr.append(cpCmd + "-p " + TMP_GROUPFILE_2 + " " + TMP_GROUPFILE_1 + "; ");
            bldr.append(mvCmd + "-f " + TMP_GROUPFILE_1 + " " + groupFile + "; ");
            bldr.append(chownCmd + "$OWNER:$GOWNER " + groupFile + ";\n");
        bldr.append("else ");
            bldr.append("echo \"does not exist\"; ");
        bldr.append("fi");

        return bldr.toString();
    }
}
