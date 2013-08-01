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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class CreateNISUser extends AbstractNISOp {
    private static final Log logger = Log.getLog(CreateNISUser.class);

    private final static Set<String> CHSH_REJECTS = CollectionUtil.newSet("password:", "passwd:");

    private static final String DEFAULTS_FILE = "/usr/sadm/defadduser";
    private static final String NO_DEFAULT_PRIMARY_GROUP = "No default primary group";
    private static final String NO_DEFAULT_HOME_DIR = "No default home directory";
    private static final String NO_DEFAULT_LOGIN_SHELL = "No default login shell";
    private static final String UID_NOT_UNIQUE = "uid is not unique.";

    /** initialize reject messages for the password cleanup command. */
    private final static Set<String> PASSWD_CLEANUP_REJECT = CollectionUtil.newSet(
            NO_DEFAULT_PRIMARY_GROUP, NO_DEFAULT_HOME_DIR, NO_DEFAULT_LOGIN_SHELL, UID_NOT_UNIQUE);

    private static final String INVALID_SHELL = "unacceptable as a new shell";

    private final static Set<String> SHELL_REJECTS = CollectionUtil.newSet(INVALID_SHELL);

    public static void performNIS(SolarisEntry entry, GuardedString password,
            SolarisConnection connection) {

        final SolarisConfiguration config = connection.getConfiguration();

        final String accountId = entry.getName();

        String shell = null;
        String uid = null;
        String gid = null;
        String gecos = null;
        String homedir = null;

        final String pwdDir = connection.getConfiguration().getNisPwdDir();
        String pwdfile = pwdDir + "/passwd";
        String shadowfile = pwdDir + "/shadow";
        String salt = "";
        StringBuilder passwordRecord;
        String shadowOwner = "";
        String shadowRecord = "";

        boolean shadow = config.isNisShadowPasswordSupport();
        String cpCmd;
        String chownCmd;
        String diffCmd;
        String removeTmpFilesScript = AbstractNISOp.getRemovePwdTmpFiles(connection);

        String basedir = config.getHomeBaseDirectory();
        if (StringUtil.isNotBlank(basedir)) {
            StringBuffer homedirBuffer = new StringBuffer(basedir);

            if (!basedir.endsWith("/")) {
                homedirBuffer.append("/");
            }

            homedirBuffer.append(accountId);
            homedir = homedirBuffer.toString();
            logger.ok("{0) got {1} from Configuration attribute 'homeBaseDir'", accountId, homedir);
        }

        String loginGroup = config.getDefaultPrimaryGroup();
        if (StringUtil.isNotBlank(loginGroup)) {
            gid = loginGroup;
            logger.ok("{0} got {1} from Configuration attribute 'defaultPrimaryGroup'", accountId,
                    loginGroup);
        }

        String loginShell = config.getLoginShell();
        if (StringUtil.isNotBlank(loginShell)) {
            shell = loginShell;
            logger.ok("{0} got {1} from Configuration attribute 'loginShell'", accountId,
                    loginShell);
        }

        // Get specified user attributes, which can override above resource
        // attributes
        Map<NativeAttribute, List<Object>> attributes =
                AbstractNISOp.constructNISUserAttributeParameters(entry, ALLOWED_NIS_ATTRIBUTES);

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
            }
            if (matched) {
                logger.ok("{0} attribute '{1}' got value '{2}'", entry.getName(), key.toString(),
                        value);
            }
        }

        cpCmd = connection.buildCommand(false, "cp");
        chownCmd = connection.buildCommand(false, "chown");
        diffCmd = connection.buildCommand(false, "diff");

        // Seed the password field accordingly on whether or not
        // a shadow file is used
        if (shadow) {
            salt = "x";
            // @formatter:off
            shadowOwner =
                "OWNER=`ls -l " + shadowfile + " | awk '{ print $3 }'`; " +
                "GOWNER=`ls -l " + shadowfile + " | awk '{ print $4 }'`";
            shadowRecord =
                cpCmd + "-p " + shadowfile + " " + TMP_PWDFILE_1 + "; " +
                cpCmd + "-p " + shadowfile + " " + TMP_PWDFILE_2 + "; " +
                chownCmd + "$WHOIAM " + TMP_PWDFILE_2 + "\n " +
                "echo \"" + accountId + "::::::::\" >> " + TMP_PWDFILE_2 + "; " +
                diffCmd + shadowfile + " " + TMP_PWDFILE_1 + " 2>&1 >/dev/null; " +
                "RC=$?; " +
                "if [ $RC -eq 0 ]; then\n" +
                  cpCmd + "-f " + TMP_PWDFILE_2 + " " + shadowfile + "; " +
                  chownCmd + "$OWNER:$GOWNER " + shadowfile + "; " +
                "else " +
                  "GRPERRMSG=\""+ ERROR_MODIFYING + shadowfile + ", for entry " + accountId + ".\"; " +
                "fi";
            // @formatter:on
        }

        // Create script for adding password file entry
        // Test for existence and readability of defaults file before trying to
        // load it
        // @formatter:off
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

        if (StringUtil.isNotBlank(gid)) {
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
        if (StringUtil.isNotBlank(homedir)) {
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
        if (StringUtil.isNotBlank(shell)) {
            passwordRecord.append("defshell=" + shell + "; ");
        } else {
            passwordRecord.append("if [ -z \"$defshell\" ]; then\n");
            passwordRecord.append("SHLERRMSG=\"" + NO_DEFAULT_LOGIN_SHELL + " found in resource or account attributes, or " + DEFAULTS_FILE + "\"; ");
            // error message reject on script execution will throw exception before defshell is needed, no need to set it here
            passwordRecord.append("fi; ");
        }
        // @formatter:on

        if (gecos != null) {
            passwordRecord.append("gecos=\"" + gecos + "\"; ");
        } else {
            passwordRecord.append("unset gecos; ");
        }

        if (uid != null) {
            passwordRecord.append("newuid=" + uid + "; \\\n");
            // check whether newuid is duplicate or not.
            passwordRecord.append("dupuid=`ypmatch \"$newuid\" passwd.byuid |  cut -d: -f3`; ");
            passwordRecord.append("if [ \"$dupuid\" ]; then\n");
            passwordRecord.append("DUPUIDERRMSG=\"" + UID_NOT_UNIQUE + " change uid " + uid
                    + " to some other unique value." + "\"; ");
            passwordRecord.append("fi; ");
        }
        // emit any errors so the reject processing can see them
        String passwordCleanup =
                "echo \"$GRPERRMSG\"; echo \"$PARERRMSG\"; echo \"$SHLERRMSG\";  echo \"$DUPUIDERRMSG\"; "
                        +
                        // The connection to the resource is pooled. Clear the
                        // environment
                        // variables that were used.
                        "unset GRPERRMSG; unset PARERRMSG; unset SHLERRMSG;  unset dupuid; unset DUPUIDERRMSG; ";

        String getOwner = initGetOwner(pwdfile);
        // @formatter:off
        String createRecord1 =
            cpCmd + "-p " + pwdfile + " " + TMP_PWDFILE_1 + "; " +
            cpCmd + "-p " + pwdfile + " " + TMP_PWDFILE_2 + "\n " +
            chownCmd + "$WHOIAM " + TMP_PWDFILE_2 + "; " +
            "echo \"" + accountId + ":" + salt + ":$newuid:$defgroup:$gecos:$defhome:\" >> " + TMP_PWDFILE_2;

        String createRecord2 =
            diffCmd + pwdfile + " " + TMP_PWDFILE_1 + " 2>&1 >/dev/null; " +
            "RC=$?; " +
            "if [ $RC -eq 0 ]; then\n" +
              cpCmd + "-f " + TMP_PWDFILE_2 + " " + pwdfile + "; " +
              chownCmd + "$OWNER:$GOWNER " + pwdfile + "; " +
            "else " +
              "GRPERRMSG=\""+ ERROR_MODIFYING + pwdfile + ", for entry " + accountId + ".\"; "+
            "fi";
        // @formatter:on

        try {
            connection.doSudoStart();

            // get required password settings
            connection.executeCommand(AbstractNISOp.WHO_I_AM);
            connection.executeCommand(passwordRecord.toString());
        } finally {
            // The reject below can throw an exception when the script is
            // executed, so reset sudo before that test
            connection.doSudoReset();
        }

        try {
            connection.executeCommand(passwordCleanup, PASSWD_CLEANUP_REJECT);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }

        try {
            connection.doSudoStart();
            try {
                // Acquire password file update mutex
                connection.executeMutexAcquireScript(PWD_MUTEX_FILE, TMP_PWD_MUTEX_FILE,
                        PWD_PID_FILE);

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
                    // second prompt due to chown nl (the first is hidden in
                    // executeCommand impl.)
                    connection.waitForRootShellPrompt();
                } catch (Exception ex) {
                    throw ConnectorException.wrap(ex);
                }
                connection.executeCommand(createRecord2);
                connection.executeCommand(removeTmpFilesScript);

                // Add shadow record if needed
                if (shadow) {
                    connection.executeCommand(shadowOwner);
                    connection.executeCommand(shadowRecord);
                    // second prompt due to chown nl (the first is hidden in
                    // executeCommand impl.)
                    connection.waitForRootShellPrompt();
                    connection.executeCommand(removeTmpFilesScript);
                }

                // NIS database has to be updated before updates to shell or
                // password
                // If the option is to bypass the make, only issue a make if
                // there is a shell or
                // password to be set.
                AbstractNISOp.addNISMake("passwd", connection);

                if (shell != null) {
                    addNISShellUpdateWithCleanup(accountId, shell, connection);
                }

                if (password != null) {
                    addNISPasswordUpdate(accountId, password, connection);
                }

                AbstractNISOp.addNISMake("passwd", connection);
            } finally {
                // Release the "mutex"
                connection.executeMutexReleaseScript(PWD_MUTEX_FILE);
            }
        } finally {
            // The reject below can throw an exception when the script is
            // executed, so reset sudo before that test
            connection.doSudoReset();
        }
    }

    private static void addNISPasswordUpdate(String accountId, GuardedString password,
            SolarisConnection connection) {

        String passwdCmd = connection.buildCommand(true, "yppasswd", accountId);

        try {
            connection.executeCommand(passwdCmd, Collections.<String> emptySet(), CollectionUtil
                    .newSet("password:"));

            connection.sendPassword(password, Collections.<String> emptySet(), CollectionUtil
                    .newSet("new password:"));
            connection.sendPassword(password, CollectionUtil.newSet(" denied"), Collections
                    .<String> emptySet());
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    /**
     * Updates Shell for the new user, if shell is valid value for NIS
     * resources. Otherwise, deletes the newly creating user as it is failure
     * for updating with invalid shell for that user.
     */
    private static void addNISShellUpdateWithCleanup(String accountId, String shell,
            SolarisConnection connection) {
        final String passwdCmd = connection.buildCommand(true, "passwd");

        final String passwordRecord =
                passwdCmd + "-r nis -e " + accountId + " 2>&1 | tee " + TMP_PWDFILE_3 + " ; ";
        try {
            connection.executeCommand(passwordRecord, CHSH_REJECTS, CollectionUtil
                    .newSet("new shell:"));
            connection.executeCommand(shell);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }

        final String passwordCleanup =
                "unset INVALID_SHELL_ERRMSG; INVALID_SHELL_ERRMSG=`grep \"" + INVALID_SHELL + "\" "
                        + TMP_PWDFILE_3 + "`;";
        connection.executeCommand(passwordCleanup);

        final String pwddir = connection.getConfiguration().getNisPwdDir();
        final String pwdFile = pwddir + "/passwd";
        final String shadowFile = pwddir + "/shadow";
        final String removeTmpFilesScript = AbstractNISOp.getRemovePwdTmpFiles(connection);

        // Add script to remove entry in passwd file if shell update fails
        String getOwner = initGetOwner(pwdFile);

        final String cleanUpScript =
                initPasswdShadowCleanUpScript(accountId, connection, pwdFile, shadowFile, getOwner);
        connection.executeCommand(cleanUpScript);

        connection.executeCommand(removeTmpFilesScript);

        // The user has to be removed from the NIS database, incase of invalid
        // shell failures
        AbstractNISOp.addNISMake("passwd", connection);

        final String invalidShellCheck = "echo $INVALID_SHELL_ERRMSG; unset INVALID_SHELL_ERRMSG;";

        connection.executeCommand(invalidShellCheck, SHELL_REJECTS);
    }

    private static String initPasswdShadowCleanUpScript(String accountId,
            SolarisConnection connection, String pwdFile, String shadowFile, String getOwner) {
        String cpCmd = connection.buildCommand(false, "cp");
        String mvCmd = connection.buildCommand(false, "mv");
        String chownCmd = connection.buildCommand(true, "chown");
        String grepCmd = connection.buildCommand(false, "grep");

        StringBuilder workScript = new StringBuilder();
        // @formatter:off
        String passwdEntryCleanup =
            "if [ \"$INVALID_SHELL_ERRMSG\" ]; then \n" +
            getOwner + " \n" +
            cpCmd + "-p " + pwdFile + " " + TMP_PWDFILE_1 + "; \n " +
            grepCmd + "-v \"^" + accountId + ":\" " + pwdFile + " > " + TMP_PWDFILE_2 + "; \n" +
            cpCmd + "-p " + TMP_PWDFILE_2 + " " + TMP_PWDFILE_1 + "; \n" +
            mvCmd + "-f " + TMP_PWDFILE_1 + " " + pwdFile + "; \n" +
            chownCmd + "$OWNER:$GOWNER " + pwdFile + "; \n";
        // @formatter:on
        workScript.append(passwdEntryCleanup);

        String shadowEntryCleanup = "";
        if (connection.getConfiguration().isNisShadowPasswordSupport()) {
            // Do the same thing we just did but for the shadow file
            String getShadowOwner =
                    "OWNER=`ls -l " + shadowFile + " | awk '{ print $3 }'`; " + "GOWNER=`ls -l "
                            + shadowFile + " | awk '{ print $4 }'`";

            shadowEntryCleanup =
                    shadowEntryCleanup + getShadowOwner + " \n" + cpCmd + "-p " + shadowFile + " "
                            + TMP_PWDFILE_1 + "; \n" + grepCmd + "-v \"^" + accountId + ":\" "
                            + shadowFile + " > " + TMP_PWDFILE_2 + "; \n" + cpCmd + "-p "
                            + TMP_PWDFILE_2 + " " + TMP_PWDFILE_1 + "; \n" + mvCmd + "-f "
                            + TMP_PWDFILE_1 + " " + shadowFile + "; \n" + chownCmd
                            + "$OWNER:$GOWNER " + shadowFile + "; \n";
        }

        workScript.append(shadowEntryCleanup);
        workScript.append("fi");

        return workScript.toString();
    }

    private static String getNISNewUidScript() {
        // @formatter:off
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
        // @formatter:on

        return script;
    }

}
