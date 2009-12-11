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
package org.identityconnectors.solaris.operation.nis;

import java.util.Collections;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.SolarisConnection;

public class DeleteNISUserCommand extends AbstractNISOp {

    public static void delete(String accountId, SolarisConnection connection) {

        try {
            connection.doSudoStart();
            
            try {
                connection.executeMutexAcquireScript(pwdMutexFile, tmpPwdMutexFile, pwdPidFile);
                
                deleteImpl(accountId, connection);
            } finally {
                connection.executeMutexReleaseScript(pwdMutexFile);
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
        // waitFor #1
        connection.executeCommand(null);
        // waitFor #2 -- it should not result in ERROR.
        connection.executeCommand(null/*no command sent on purpose*/, CollectionUtil.newSet("ERROR"));
        connection.executeCommand(removeTmpFilesScript);
        
        if (connection.getConfiguration().isNisShadowPasswordSupport()) {
            final String getOwnerShadow = initGetOwner(shadowFile);
            final String workScriptShadow = initWorkScript(accountName, shadowFile, connection);
            connection.executeCommand(getOwnerShadow);
            connection.executeCommand(workScriptShadow);
            // two extra 'waitFor(rootShellPrompt)'-s are needed, because they are produced by the script
            // waitFor #1
            connection.executeCommand(null, Collections.<String>emptySet());
            // waitFor #2
            connection.executeCommand(null, Collections.<String>emptySet());
        }
        
        connection.executeCommand(removeTmpFilesScript);
        
        // The user has to be removed from the NIS database
        // TODO this comment doesn't make much sense (from adapter)
        AbstractNISOp.addNISMake("passwd", connection);
    }

    private static String initWorkScript(final String accountId, final String pwdFile, final SolarisConnection connection) {
        final String cpCmd = connection.buildCommand("cp");
        final String mvCmd = connection.buildCommand("mv");
        final String chownCmd = connection.buildCommand("chown");
        final String grepCmd = connection.buildCommand("grep");
        
        StringBuilder workScript = new StringBuilder();
        workScript.append(
            cpCmd + "-p " + pwdFile + " " + tmpPwdfile1 + "; "); 
        workScript.append(
            grepCmd + "-v \"^" + accountId + ":\" " + pwdFile + " > " + tmpPwdfile2 + "\n");
        workScript.append(
            cpCmd + "-p " + tmpPwdfile2 + " " + tmpPwdfile1 + "; ");
        workScript.append(
            mvCmd + "-f " + tmpPwdfile1 + " " + pwdFile + "\n");
        workScript.append(
            chownCmd + "$OWNER:$GOWNER " + pwdFile);
        return workScript.toString();
    }

}
