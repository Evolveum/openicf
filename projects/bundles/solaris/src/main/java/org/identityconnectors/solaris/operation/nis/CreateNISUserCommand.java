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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class CreateNISUserCommand extends AbstractNISOp {
    private static final Log _log = Log.getLog(CreateNISUserCommand.class);
    
    private final static Set<String> chshRejects = CollectionUtil.newSet("password:", "passwd:");

    private static final String DEFAULTS_FILE = "/usr/sadm/defadduser";
    private static final String NO_DEFAULT_PRIMARY_GROUP = "No default primary group";
    private static final String NO_DEFAULT_HOME_DIR = "No default home directory";
    private static final String NO_DEFAULT_LOGIN_SHELL = "No default login shell";
    private static final String UID_NOT_UNIQUE = "uid is not unique.";
    
    private static final String whoIAm = "WHOIAM=`who am i | cut -d ' ' -f1`";
    
    /** initialize reject messages for the password cleanup command. */
    private final static Set<String> passwdCleanupReject = CollectionUtil.newSet(
            NO_DEFAULT_PRIMARY_GROUP, NO_DEFAULT_HOME_DIR, NO_DEFAULT_LOGIN_SHELL, UID_NOT_UNIQUE
            );
    
    private static final String INVALID_SHELL = "unacceptable as a new shell";
    
    private final static Set<String> shellRejects = CollectionUtil.newSet(INVALID_SHELL);
    
    public static void performNIS(SolarisEntry entry, SolarisConnection connection) {

        final SolarisConfiguration config = connection.getConfiguration();
        
        final String accountId = entry.getName();
        
        String shell = null;
        String uid = null; 
        String gid = null;
        String gecos = null;
        String homedir = null;

        final String pwdDir = CommonNIS.getNisPwdDir(connection);
        String pwdfile = pwdDir + "/passwd";
        String shadowfile = pwdDir + "/shadow";
        String salt = "";
        StringBuilder passwordRecord;
        String shadowOwner = "";
        String shadowRecord = "";
        
        boolean shadow = config.isNisShadow();
        String cpCmd;
        String chownCmd;
        String diffCmd;
        String removeTmpFilesScript = CommonNIS.getRemovePwdTmpFiles(connection);
        
        String basedir = config.getHomeBaseDir();
        if ((basedir != null) && (basedir.length() > 0)) {
            StringBuffer homedirBuffer = new StringBuffer(basedir);

            if (!basedir.endsWith("/")) {
                homedirBuffer.append("/");
            }

            homedirBuffer.append(accountId);
            homedir = homedirBuffer.toString();
            _log.ok(accountId + " got " + homedir + " from Configuration attribute 'homeBaseDir'");
        }
        
        String loginGroup = config.getDefaultPrimaryGroup();
        if ((loginGroup != null) && (loginGroup.length() > 0)) {
            gid = loginGroup;
            _log.ok(accountId + " got " + loginGroup + " from Configuration attribute 'defaultPrimaryGroup'");
        }
        
        String loginShell  = config.getLoginShell();
        if ((loginShell != null) && (loginShell.length() > 0)) {
            shell = loginShell;
            _log.ok(accountId + " got " + loginShell + " from Configuration attribute 'loginShell'");
        }
        
        // Get specified user attributes, which can override above resource attributes
        Map<NativeAttribute, List<Object>> attributes = CommonNIS.constructNISUserAttributeParameters(entry, allowedNISattributes);
        
        for (Map.Entry<NativeAttribute, List<Object>> it : attributes.entrySet()) {
            NativeAttribute key = it.getKey();
            String value = (String) it.getValue().get(0);
            boolean matched = true;
            switch (key) {
            case SHELL:
                shell = value;
                break;
            case GROUP_PRIM:
                gid = value;
                break;
            case DIR:
                homedir = value;
                break;
            case COMMENT:
                gecos = value;
                break;
            case ID:
                uid = value;
                break;
            default:
                matched = false;
                break;
            }// switch
            if (matched) {
                _log.ok(entry.getName() + " attribute '" + key.toString() + "' got value '" + value + "'");
            }
        }// for
        
        cpCmd = connection.buildCommand("cp");
        chownCmd = connection.buildCommand("chown");
        diffCmd = connection.buildCommand("diff");
        
        // Seed the password field accordingly on whether or not
        // a shadow file is used
        if (shadow) {
            salt = "x";
            shadowOwner =
                "OWNER=`ls -l " + shadowfile + " | awk '{ print $3 }'`; " +
                "GOWNER=`ls -l " + shadowfile + " | awk '{ print $4 }'`";
            shadowRecord =
                cpCmd + "-p " + shadowfile + " " + tmpPwdfile1 + "; " +
                cpCmd + "-p " + shadowfile + " " + tmpPwdfile2 + "; " +
                chownCmd + "$WHOIAM " + tmpPwdfile2 + "\n " +
                "echo \"" + accountId + "::::::::\" >> " + tmpPwdfile2 + "; " +
                diffCmd + shadowfile + " " + tmpPwdfile1 + " 2>&1 >/dev/null; " +
                "RC=$?; " +
                "if [ $RC -eq 0 ]; then\n" +
                  cpCmd + "-f " + tmpPwdfile2 + " " + shadowfile + "; " +
                  chownCmd + "$OWNER:$GOWNER " + shadowfile + "; " +
                "else " +
                  "GRPERRMSG=\""+ ERROR_MODIFYING + shadowfile + ", for entry " + accountId + ".\"; " +
                "fi";
        }
        
        // Create script for adding password file entry
        // Test for existence and readability of defaults file before trying to load it
        passwordRecord = new StringBuilder(
                // The connection to the resource is pooled.  Clear the environment
                // variables that will be used.
                "unset defgroup; unset defgname; unset defhome; unset defparent; " +
                "unset defshell; unset gecos; unset newuid; unset dupuid; " +
                "unset GRPERRMSG; unset PARERRMSG; unset SHLERRMSG; unset DUPUIDERRMSG; " +
                "if [ -r " + DEFAULTS_FILE + " ]; then\n" +
                    ". " + DEFAULTS_FILE + "; " +
                "fi; "
                );
        // If resource attributes are available, override results from defadduser
        // At the moment, we are using defgroup, defshell and defparent
        // Override for defgroup (RA_DEFAULT_PRIMARY_GROUP or USER_GROUP) has already been loaded above.
        if ((gid != null) && (gid.length() > 0)) {
            passwordRecord.append("defgroup=`ypmatch " + gid + " group | cut -d: -f3`; ");
            passwordRecord.append("if [ -z \"$defgroup\" ]; then\n");
            passwordRecord.append("GRPERRMSG=\"" + NO_DEFAULT_PRIMARY_GROUP + " matches in ypmatch " + gid + " group.\"; ");
            passwordRecord.append("fi; ");
        } else {
            passwordRecord.append("if [ -z \"$defgname\" ]; then\n");
            passwordRecord.append("GRPERRMSG=\"" + NO_DEFAULT_PRIMARY_GROUP + " found in resource or account attributes, or " + DEFAULTS_FILE + "\"; ");
            passwordRecord.append("else\n");
            passwordRecord.append("  defgroup=`ypmatch \"$defgname\" group | cut -d: -f3`; ");
            passwordRecord.append("  if [ -z \"$defgroup\" ]; then\n");
            passwordRecord.append("  GRPERRMSG=\"" + NO_DEFAULT_PRIMARY_GROUP + " found in resource or account attributes, or " + DEFAULTS_FILE + "\"; ");
            passwordRecord.append("  fi; ");
            passwordRecord.append("fi; ");
        }
        
        // Override for defparent (homeBasedir or userDir) has already been loaded above.
        if ((homedir != null) && (homedir.length() > 0)) {
            passwordRecord.append("defhome=" + homedir +"; ");
        } else {
            passwordRecord.append("if [ -z \"$defparent\" ]; then\n");
            passwordRecord.append("PARERRMSG=\"" + NO_DEFAULT_HOME_DIR + " found in resource or account attributes, or " + DEFAULTS_FILE + "\"; ");
            // error message reject on script execution will throw exception before defhome is needed, no need to set it here
            passwordRecord.append("else\n");
            passwordRecord.append("defhome=$defparent/" + accountId + "; ");
            passwordRecord.append("fi; ");
        }
        
        // Override for defshell (RA_LOGIN_SHELL or USER_SHELL) has already been loaded above.
        if ((shell != null) && (shell.length() > 0)) {
            passwordRecord.append("defshell=" + shell + "; ");
        } else {
            passwordRecord.append("if [ -z \"$defshell\" ]; then\n");
            passwordRecord.append("SHLERRMSG=\"" + NO_DEFAULT_LOGIN_SHELL + " found in resource or account attributes, or " + DEFAULTS_FILE + "\"; ");
            // error message reject on script execution will throw exception before defshell is needed, no need to set it here
            passwordRecord.append("fi; ");
        }

        if (gecos != null) {
            passwordRecord.append("gecos=\"" + gecos + "\"; ");
        } else {
            passwordRecord.append("unset gecos; ");
        }
        
        if (uid != null) {
            passwordRecord.append("newuid=" + uid + "; \\\n");
            //check whether newuid is duplicate or not.
            passwordRecord.append("dupuid=`ypmatch \"$newuid\" passwd.byuid |  cut -d: -f3`; ");
            passwordRecord.append("if [ \"$dupuid\" ]; then\n");
            passwordRecord.append("DUPUIDERRMSG=\"" + UID_NOT_UNIQUE + " change uid " + uid  + " to some other unique value." + "\"; ");
            passwordRecord.append("fi; ");
        }
        // emit any errors so the reject processing can see them
        String passwordCleanup = "echo \"$GRPERRMSG\"; echo \"$PARERRMSG\"; echo \"$SHLERRMSG\";  echo \"$DUPUIDERRMSG\"; " +
        // The connection to the resource is pooled.  Clear the environment
        // variables that were used.
        "unset GRPERRMSG; unset PARERRMSG; unset SHLERRMSG;  unset dupuid; unset DUPUIDERRMSG; ";
        
        String getOwner =
            "OWNER=`ls -l " + pwdfile + " | awk '{ print $3 }'`; " +
            "GOWNER=`ls -l " + pwdfile + " | awk '{ print $4 }'`";

        String createRecord1 =
            cpCmd + "-p " + pwdfile + " " + tmpPwdfile1 + "; " +
            cpCmd + "-p " + pwdfile + " " + tmpPwdfile2 + "\n " +
            chownCmd + "$WHOIAM " + tmpPwdfile2 + "; " +
            "echo \"" + accountId + ":" + salt + ":$newuid:$defgroup:$gecos:$defhome:\" >> " + tmpPwdfile2;

        String createRecord2 =
            diffCmd + pwdfile + " " + tmpPwdfile1 + " 2>&1 >/dev/null; " +
            "RC=$?; " +
            "if [ $RC -eq 0 ]; then\n" +
              cpCmd + "-f " + tmpPwdfile2 + " " + pwdfile + "; " +
              chownCmd + "$OWNER:$GOWNER " + pwdfile + "; " +
            "else " +
              "GRPERRMSG=\""+ ERROR_MODIFYING + pwdfile + ", for entry " + accountId + ".\"; "+
            "fi";
        
        try {
            connection.doSudoStart();
            
            // get required password settings
            connection.executeCommand(whoIAm);
            connection.executeCommand(passwordRecord.toString());
        } finally {
            // The reject below can throw an exception when the script is executed, so reset sudo before that test
            connection.doSudoReset();
        }
        
        try {
            connection.executeCommand(passwordCleanup, passwdCleanupReject);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
        
        try {
            connection.doSudoStart();
            try {
                // Acquire password file update mutex
                connection.executeMutexAcquireScript(pwdMutexFile, tmpPwdMutexFile, pwdPidFile);
                
                // Clear any leftover temporary files
                connection.executeCommand(removeTmpFilesScript);
                
                // Add the script to determine the user's id if not specified
                if (uid == null) {
                    final String uidScript = getNISNewUidScript();
                    connection.executeCommand(uidScript);
                }
                
                // Add password file record
                connection.executeCommand(getOwner);
                connection.executeCommand(createRecord1);
                try {
                    connection.waitForRootShellPrompt(); // second prompt due to chown nl (the first is hidden in executeCommand impl.)
                } catch (Exception ex) {
                    throw ConnectorException.wrap(ex);
                }
                connection.executeCommand(createRecord2);
                connection.executeCommand(removeTmpFilesScript);
                
                // Add shadow record if needed
                if (shadow) {
                    connection.executeCommand(shadowOwner);
                    connection.executeCommand(shadowRecord);
                    connection.waitForRootShellPrompt();// second prompt due to chown nl (the first is hidden in executeCommand impl.)
                    connection.executeCommand(removeTmpFilesScript);
                }
                
                // NIS database has to be updated before updates to shell or password
                // If the option is to bypass the make, only issue a make if there is a shell or
                // password to be set.
                CommonNIS.addNISMake("passwd", connection);
                
                if (shell != null) {
                    addNISShellUpdateWithCleanup(accountId, shell, connection);
                }
                
                GuardedString password = getPassword(entry);
                if (password != null) {
                    addNISPasswordUpdate(accountId, password, connection);
                }
                
                CommonNIS.addNISMake("passwd", connection);
            } finally {
                // Release the "mutex"
                connection.executeMutexReleaseScript(pwdMutexFile);
            }
        } finally {
            // The reject below can throw an exception when the script is executed, so reset sudo before that test
            connection.doSudoReset();
        }
        
        // TODO:  Need to add to groups (++++++++++++++++++++++++++++++++++++++++++++++++)
    }

    private static void addNISPasswordUpdate(String accountId,
            GuardedString password, SolarisConnection connection) {
        
        String passwdCmd = connection.buildCommand("yppasswd", accountId);

        try {
            connection.executeCommand(passwdCmd, Collections.<String>emptySet(), CollectionUtil.newSet("password:"));

            SolarisConnection.sendPassword(password, Collections.<String>emptySet(), CollectionUtil.newSet("new password:"), connection);
            SolarisConnection.sendPassword(password, CollectionUtil.newSet(" denied"), Collections.<String>emptySet(), connection);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }
    
    /**
     * Updates Shell for the new user, if shell is valid value for NIS resources.
     * Otherwise, deletes the newly creating user as it is failure for updating with invalid
     * shell for that user. 
     */
    private static void addNISShellUpdateWithCleanup(String accountId,
            String shell, SolarisConnection connection) {
        final String passwdCmd = connection.executeCommand("passwd");
        
        final String passwordRecord = passwdCmd + "-r nis -e " + accountId + " 2>&1 | tee " + tmpPwdfile3 + " ; ";
        try {
            connection.executeCommand(passwordRecord, chshRejects, CollectionUtil.newSet("new shell:"));
            connection.executeCommand(shell);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }

        
        final String passwordCleanup = "unset INVALID_SHELL_ERRMSG; INVALID_SHELL_ERRMSG=`grep \"" + INVALID_SHELL + "\" " + tmpPwdfile3 + "`;";
        connection.executeCommand(passwordCleanup);

        final String pwddir = CommonNIS.getNisPwdDir(connection);
        final String pwdFile = pwddir + "/passwd";
        final String shadowFile = pwddir + "/shadow";
        final String removeTmpFilesScript = CommonNIS.getRemovePwdTmpFiles(connection);

        // Add script to remove entry in passwd file if shell update fails
        String getOwner =
            "OWNER=`ls -l " + pwdFile + " | awk '{ print $3 }'`; " +
            "GOWNER=`ls -l " + pwdFile + " | awk '{ print $4 }'`";

        final String cleanUpScript = initPasswdShadowCleanUpScript(accountId, connection, pwdFile, shadowFile, getOwner);
        connection.executeCommand(cleanUpScript);
        
        connection.executeCommand(removeTmpFilesScript);
        
        // The user has to be removed from the NIS database, incase of invalid shell failures
        CommonNIS.addNISMake("passwd", connection);
        
        final String invalidShellCheck= "echo $INVALID_SHELL_ERRMSG; unset INVALID_SHELL_ERRMSG;";

        connection.executeCommand(invalidShellCheck, shellRejects);
    }

    private static String initPasswdShadowCleanUpScript(String accountId,
            SolarisConnection connection, String pwdFile, String shadowFile,
            String getOwner) {
        String cpCmd = connection.buildCommand("cp");
        String mvCmd = connection.buildCommand("mv");
        String chownCmd = connection.buildCommand("chown");
        String grepCmd = connection.buildCommand("grep");
        
        StringBuilder workScript = new StringBuilder();
        String passwdEntryCleanup =
            "if [ \"$INVALID_SHELL_ERRMSG\" ]; then \n" +
            getOwner + " \n" + 
            cpCmd + "-p " + pwdFile + " " + tmpPwdfile1 + "; \n " +
            grepCmd + "-v \"^" + accountId + ":\" " + pwdFile + " > " + tmpPwdfile2 + "; \n" +
            cpCmd + "-p " + tmpPwdfile2 + " " + tmpPwdfile1 + "; \n" +
            mvCmd + "-f " + tmpPwdfile1 + " " + pwdFile + "; \n" +
            chownCmd + "$OWNER:$GOWNER " + pwdFile + "; \n";

        workScript.append(passwdEntryCleanup); 

        String shadowEntryCleanup = "";
        if (connection.getConfiguration().isNisShadow()) {
            // Do the same thing we just did but for the shadow file
            String getShadowOwner =
                "OWNER=`ls -l " + shadowFile + " | awk '{ print $3 }'`; " +
                "GOWNER=`ls -l " + shadowFile + " | awk '{ print $4 }'`";

            shadowEntryCleanup = shadowEntryCleanup + 
                getShadowOwner +  " \n" + 
                cpCmd + "-p " + shadowFile + " " + tmpPwdfile1 + "; \n" +
                grepCmd + "-v \"^" + accountId + ":\" " + shadowFile + " > " + tmpPwdfile2 + "; \n" +
                cpCmd + "-p " + tmpPwdfile2 + " " + tmpPwdfile1 + "; \n" +
                mvCmd + "-f " + tmpPwdfile1 + " " + shadowFile + "; \n" +
                chownCmd + "$OWNER:$GOWNER " + shadowFile + "; \n"; 
        }

        workScript.append(shadowEntryCleanup); 
        workScript.append("fi");
        
        return workScript.toString();
    }

    private static String getNISNewUidScript() {
        String script =
            "minuid=100; " +
            "newuid=`ypcat passwd | sort -n -t: -k3 | tail -1 | cut -d: -f3`; " +
            // prevent -lt from failing when there are no users
            "if [ -z \"$newuid\" ]; then\n" +
              "newuid=$minuid; " +
            "fi; " +
            "newuid=`expr $newuid + 1`; " +
            "if [ $newuid -lt $minuid ]; then\n" +
              "newuid=$minuid; " +
            "fi";

        return script;
    }
    
}
