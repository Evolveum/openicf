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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.SolarisConnection;

public class DeleteNISUser extends AbstractNISOp {

    public static void delete(String accountId, SolarisConnection connection) {

        try {
            connection.doSudoStart();

            try {
                connection.executeMutexAcquireScript(PWD_MUTEX_FILE, TMP_PWD_MUTEX_FILE, PWD_PID_FILE);

                deleteImpl(accountId, connection);
            } finally {
                connection.executeMutexReleaseScript(PWD_MUTEX_FILE);
            }
        } finally {
            connection.doSudoReset();
        }
    }

    private static void deleteImpl(String accountName, SolarisConnection connection) {

        final String pwddir = connection.getConfiguration().getNisPwdDir();
        final String pwdFile = pwddir + "/passwd";
        final String shadowFile = pwddir + "/shadow";
        final String removeTmpFilesScript = AbstractNISOp.getRemovePwdTmpFiles(connection);



        final String getOwner = initGetOwner(pwdFile);
        final String workScript = initWorkScript(accountName, pwdFile, connection);
        connection.executeCommand(getOwner);
        connection.executeCommand(workScript);
        // two extra 'waitFor(rootShellPrompt)'-s are needed, because they are produced by the script
        // waitFor root shell prompt #1
        connection.waitForRootShellPrompt();
        // waitFor root shell prompt #2 -- it should not result in ERROR.
        connection.waitForRootShellPrompt(CollectionUtil.newSet("ERROR"));
        connection.executeCommand(removeTmpFilesScript);

        if (connection.getConfiguration().isNisShadowPasswordSupport()) {
            final String getOwnerShadow = initGetOwner(shadowFile);
            final String workScriptShadow = initWorkScript(accountName, shadowFile, connection);
            connection.executeCommand(getOwnerShadow);
            connection.executeCommand(workScriptShadow);
            // two extra 'waitFor(rootShellPrompt)'-s are needed, because they are produced by the script
            // waitFor root shell prompt #1
            connection.waitForRootShellPrompt();
            // waitFor root shell prompt #2
            connection.waitForRootShellPrompt();
        }

        connection.executeCommand(removeTmpFilesScript);

        // The user has to be removed from the NIS database
        AbstractNISOp.addNISMake("passwd", connection);
    }

    private static String initWorkScript(final String accountId, final String pwdFile, final SolarisConnection connection) {
        final String cpCmd = connection.buildCommand(false, "cp");
        final String mvCmd = connection.buildCommand(false, "mv");
        final String chownCmd = connection.buildCommand(true, "chown");
        final String grepCmd = connection.buildCommand(false, "grep");

        StringBuilder workScript = new StringBuilder();
        workScript.append(
            cpCmd + "-p " + pwdFile + " " + TMP_PWDFILE_1 + "; ");
        workScript.append(
            grepCmd + "-v \"^" + accountId + ":\" " + pwdFile + " > " + TMP_PWDFILE_2 + "\n");
        workScript.append(
            cpCmd + "-p " + TMP_PWDFILE_2 + " " + TMP_PWDFILE_1 + "; ");
        workScript.append(
            mvCmd + "-f " + TMP_PWDFILE_1 + " " + pwdFile + "\n");
        workScript.append(
            chownCmd + "$OWNER:$GOWNER " + pwdFile);
        return workScript.toString();
    }

}
