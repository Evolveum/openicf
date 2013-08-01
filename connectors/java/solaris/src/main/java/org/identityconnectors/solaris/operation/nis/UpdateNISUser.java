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
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class UpdateNISUser extends AbstractNISOp {
    private static final Log logger = Log.getLog(UpdateNISUser.class);

    private static final String NO_PRIMARY_GROUP = "No primary group";
    private static final String UID_NOT_UNIQUE = "uid is not unique.";

    public static void updateUser(SolarisEntry userEntry, final GuardedString passwd, SolarisConnection connection) {
        final String accountName = userEntry.getName();
        String pwddir = connection.getConfiguration().getNisPwdDir();
        String pwdfile = pwddir + "/passwd";

        boolean recordUpdate = false;
        boolean isRename = false;

        String shell = null;
        String uid = null;
        String gid = null;
        String gecos = null;
        String homedir = null;
        final String cpCmd = connection.buildCommand(false, "cp");
        final String chownCmd = connection.buildCommand(true, "chown");
        final String diffCmd = connection.buildCommand(false, "diff");
        final String grepCmd = connection.buildCommand(false, "grep");

        // Test for a rename operation
        Attribute nameAttr = userEntry.searchForAttribute(NativeAttribute.NAME);
        String newName = (nameAttr != null) ? AttributeUtil.getStringValue(nameAttr) : accountName;
        if (newName != null && !newName.equals(userEntry.getName())) {
            // Make sure we update the entry
            recordUpdate = true;
            isRename = true;
        }

        String removeTmpFilesScript = AbstractNISOp.getRemovePwdTmpFiles(connection);
        // @formatter:off
        String getOwner =
            "OWNER=`ls -l " + pwdfile + " | awk '{ print $3 }'`; " +
            "GOWNER=`ls -l " + pwdfile + " | awk '{ print $4 }'`; " +
            "unset GRPERRMSG";

        String updateUser =
            "if [ -n \"$ENTRYTEXT\" ]; then " +
              cpCmd + "-p " + pwdfile + " " + TMP_PWDFILE_1 + "; " +
              grepCmd + "-v \"^" + accountName + ":\" " + TMP_PWDFILE_1 + " > " + TMP_PWDFILE_2 + "; " +
              chownCmd + "$WHOIAM " + TMP_PWDFILE_2 + "\n " +
              "echo " + newName + ":$PASSWD:$NEWUID:$GROUP:$GECOS:$HOMEDIR:$SHELL >> " + TMP_PWDFILE_2 + "; " +
              diffCmd + pwdfile + " " + TMP_PWDFILE_1 + " 2>&1 >/dev/null; " +
              "RC=$?\n" +
              "if [ $RC -eq 0 ]; then " +
                cpCmd + "-f " + TMP_PWDFILE_2 + " " + pwdfile + "; " +
                chownCmd + "$OWNER:$GOWNER " + pwdfile + "; " +
              "else " +
                "GRPERRMSG=\"" + ERROR_MODIFYING + pwdfile + ", for entry " + newName + ".\"; " +
              "fi; " +
            "else\n" +
            "GRPERRMSG=\"" + accountName + " not found in " + pwdfile + ".\"; " +
            "fi";
        // @formatter:on

        // Get specified user attributes
        Map<NativeAttribute, List<Object>> attributes = AbstractNISOp.constructNISUserAttributeParameters(userEntry, ALLOWED_NIS_ATTRIBUTES);

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
                logger.ok("{0} attribute '{1}' got value '{2}'", userEntry.getName(), key.toString(), value);
            }
        }

        connection.doSudoStart();
        try {
            connection.executeCommand(AbstractNISOp.WHO_I_AM);
            try {
                connection.executeMutexAcquireScript(PWD_MUTEX_FILE, TMP_PWD_MUTEX_FILE, PWD_PID_FILE);

                final Pair<Boolean, String> response = initPasswordRecord1(accountName, uid, gid, homedir, recordUpdate);
                recordUpdate = response.first;
                final String passwordRecord1 = response.second;

                final String passwordRecord2;
                if (gecos != null) {
                    passwordRecord2 = ("GECOS=\"" + gecos + "\"; ");
                    recordUpdate = true;
                } else {
                    passwordRecord2 = ("GECOS=`echo $ENTRYTEXT | cut -d: -f5`; ");
                }

                if (recordUpdate) {
                    final String passwdVars = initPasswdVars(connection.getConfiguration());

                    connection.executeCommand(getOwner);
                    connection.executeCommand(passwordRecord1);

                    final String passwordCleanup1 = "echo \"$GRPERRMSG\"; unset GRPERRMSG; echo \"$DUPUIDERRMSG\"; unset DUPUIDERRMSG; unset dupuid; ";
                    connection.executeCommand(passwordCleanup1, CollectionUtil.newSet(NO_PRIMARY_GROUP, UID_NOT_UNIQUE));

                    connection.executeCommand(passwordRecord2);
                    connection.executeCommand(passwdVars);

                    connection.executeCommand(removeTmpFilesScript);
                    connection.executeCommand(updateUser);
                    connection.executeCommand(removeTmpFilesScript);

                    if (isRename && connection.getConfiguration().isNisShadowPasswordSupport()) {
                        // Make sure we get the rename to the shadow file
                        String shadowfile = pwddir + "/shadow";
                        // @formatter:off
                        String shadowRename =
                            "GRPENTRY=`grep '^" + accountName + ":' " + shadowfile + "`; " +
                            "if [ -n \"$GRPENTRY\" ]; then " +
                              cpCmd + "-p " + shadowfile + " " + TMP_PWDFILE_1 + "; " +
                              grepCmd + "-v \"^" + accountName + ":\" " + TMP_PWDFILE_1 + " > " + TMP_PWDFILE_2 + "; " +
                              chownCmd + "$WHOIAM " + TMP_PWDFILE_2 + "\n " +
                              "echo $GRPENTRY | sed 's/^" + accountName + ":/" + newName + ":/g' >> " + TMP_PWDFILE_2 + "; " +
                              diffCmd + shadowfile + " " + TMP_PWDFILE_1 + " 2>&1 >/dev/null; " +
                              "RC=$?\n" +
                              "if [ $RC -eq 0 ]; then " +
                                cpCmd + "-f " + TMP_PWDFILE_2 + " " + shadowfile + "; " +
                                chownCmd + "$OWNER:$GOWNER " + shadowfile + "; " +
                              "else\n" +
                              "GRPERRMSG=\"" + ERROR_MODIFYING + shadowfile + ", for entry " + newName + ".\"; " +
                              "fi; " +
                            "else\n" +
                            "GRPERRMSG=\"" + ERROR_MODIFYING + shadowfile + ", " + accountName + " not found.\"; " +
                            "fi";
                        // @formatter:on

                        getOwner = initGetOwner(shadowfile);

                        connection.executeCommand(getOwner);
                        connection.executeCommand(shadowRename);
                        connection.executeCommand(removeTmpFilesScript);
                    }

                    // Migrate the changes to the NIS database.
                    // The changes to the NIS database have to be made before the
                    // changes for shell and password.
                    AbstractNISOp.addNISMake("passwd", connection);
                }

                if (shell != null) {
                    addNISShellUpdate(accountName, shell, connection);
                }

                if (passwd != null) {
                    addNISPasswordUpdate(accountName, passwd, connection);
                }

                AbstractNISOp.addNISMake("passwd", connection);


            } finally {
                connection.executeMutexReleaseScript(PWD_MUTEX_FILE);
            }
        } finally {
            connection.doSudoReset();
        }
    }

    private static void addNISPasswordUpdate(String account,
            GuardedString password, SolarisConnection connection) {
        final String passwdCmd = connection.buildCommand(true, "yppasswd", account);
        connection.executeCommand(passwdCmd, Collections.<String>emptySet(), CollectionUtil.newSet(" password:"));
        connection.sendPassword(password, Collections.<String>emptySet(), CollectionUtil.newSet("new password:"));
        connection.sendPassword(password, CollectionUtil.newSet(" denied"), Collections.<String>emptySet());
    }

    private static void addNISShellUpdate(String account, String shell,
            SolarisConnection connection) {
        final String passwdCmd = connection.buildCommand(true, "passwd", "-r nis -e", account);

        final Set<String> chshReject = CollectionUtil.newSet("password:", "passwd:" /* // passwd: User unknown: <id>\nPermission denied\n */);
        connection.executeCommand(passwdCmd, chshReject, CollectionUtil.newSet("new shell:"));

        connection.executeCommand(shell, CollectionUtil.newSet("unacceptable as a new shell"));
    }

    private static String initPasswdVars(SolarisConfiguration configuration) {
        StringBuilder builder = new StringBuilder();
        builder.append(configuration.isNisShadowPasswordSupport() ? "PASSWD=\"x\"; " : // password  held in shadow file
                "PASSWD=`echo $ENTRYTEXT | cut -d: -f2`; " // password in passwd file
        );
        builder.append("SHELL=`echo $ENTRYTEXT | cut -d: -f7`");

        return builder.toString();
    }

    private static Pair<Boolean, String> initPasswordRecord1(String accountName, String uid,
            String gid, String homedir, final boolean recordUpdate) {
        boolean recordUpdateTmp = recordUpdate;
        StringBuffer passwordRecord1 = new StringBuffer(
        // The connection to the resource is pooled. Clear the environment
        // variables that will be used.
                "unset GRPERRMSG; unset dupuid; unset DUPUIDERRMSG; "
                );
        passwordRecord1.append("ENTRYTEXT=`ypmatch " + accountName + " passwd`; ");


        if (StringUtil.isNotBlank(gid)) { // "" gid does not make sense
            passwordRecord1.append("GROUP=`ypmatch " + gid + " group | cut -d: -f3`; ");
            passwordRecord1.append("if [ -z \"$GROUP\" ]; then\n");
            passwordRecord1.append("GRPERRMSG=\"" + NO_PRIMARY_GROUP + " matches in ypmatch " + gid + " group.\"; ");
            passwordRecord1.append("fi; ");
            recordUpdateTmp = true;
        } else {
            passwordRecord1.append("GROUP=`echo $ENTRYTEXT | cut -d: -f4`; ");
        }

        if (homedir != null) {
            passwordRecord1.append("HOMEDIR=" + homedir +"; ");
            recordUpdateTmp = true;
        } else {
            passwordRecord1.append("HOMEDIR=`echo $ENTRYTEXT | cut -d: -f6`; ");
        }

        if (StringUtil.isNotBlank(uid)) { // "" uid does not make sense
            passwordRecord1.append("NEWUID=" + uid + "; \\\n");
            //check whether newuid is duplicate or not.
            passwordRecord1.append("dupuid=`ypmatch \"$NEWUID\" passwd.byuid |  cut -d: -f3`; ");
            passwordRecord1.append("if [ \"$dupuid\" ]; then\n");
            passwordRecord1.append("DUPUIDERRMSG=\"" + UID_NOT_UNIQUE + " change uid " + uid  + " to some other unique value." + "\"; ");
            passwordRecord1.append("fi; ");
            recordUpdateTmp = true;
        } else {
            passwordRecord1.append("NEWUID=`echo $ENTRYTEXT | cut -d: -f3`; ");
        }

        return new Pair<Boolean, String>(recordUpdateTmp, passwordRecord1.toString());
    }
}
