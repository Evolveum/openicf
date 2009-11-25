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
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class UpdateNISUserCommand extends AbstractNISOp {
    private static final Log _log = Log.getLog(UpdateNISUserCommand.class);
    
    private static final String NO_PRIMARY_GROUP = "No primary group";
    private static final String UID_NOT_UNIQUE = "uid is not unique.";
    
    public static void performNIS(SolarisEntry entry, SolarisConnection connection) {
        // TODO eliminate duplicate accountId(Name)
        final String accountId = entry.getName();
        final String accountName = accountId;
        String pwddir = AbstractNISOp.getNisPwdDir(connection);
        String pwdfile = pwddir + "/passwd";
        
        boolean recordUpdate = false;
        boolean isRename = false;
        
        String shell = null;
        String uid = null;
        String gid = null;
        String gecos = null;
        String homedir = null;
        final String cpCmd = connection.buildCommand("cp");
        final String chownCmd = connection.buildCommand("chown");
        final String diffCmd = connection.buildCommand("diff");
        final String grepCmd = connection.buildCommand("grep");
        
        // Test for a rename operation
        String newName = getNameValue(entry);
        if (!newName.equals(entry.getName())) {
            // Make sure we update the entry
            recordUpdate = true;
            isRename = true;
        }
        
        String removeTmpFilesScript = AbstractNISOp.getRemovePwdTmpFiles(connection);
        String getOwner =
            "OWNER=`ls -l " + pwdfile + " | awk '{ print $3 }'`; " +
            "GOWNER=`ls -l " + pwdfile + " | awk '{ print $4 }'`; " +
            "unset GRPERRMSG";
        
        String updateUser =
            "if [ -n \"$ENTRYTEXT\" ]; then " +
              cpCmd + "-p " + pwdfile + " " + tmpPwdfile1 + "; " +
              grepCmd + "-v \"^" + accountId + ":\" " + tmpPwdfile1 + " > " + tmpPwdfile2 + "; " +
              chownCmd + "$WHOIAM " + tmpPwdfile2 + "\n " +
              "echo " + newName + ":$PASSWD:$NEWUID:$GROUP:$GECOS:$HOMEDIR:$SHELL >> " + tmpPwdfile2 + "; " +
              diffCmd + pwdfile + " " + tmpPwdfile1 + " 2>&1 >/dev/null; " +
              "RC=$?\n" +
              "if [ $RC -eq 0 ]; then " +
                cpCmd + "-f " + tmpPwdfile2 + " " + pwdfile + "; " +
                chownCmd + "$OWNER:$GOWNER " + pwdfile + "; " +
              "else " +
                "GRPERRMSG=\"" + ERROR_MODIFYING + pwdfile + ", for entry " + newName + ".\"; " +
              "fi; " +
            "else\n" +
            "GRPERRMSG=\"" + accountId + " not found in " + pwdfile + ".\"; " +
            "fi";
        
        // Get specified user attributes
        Map<NativeAttribute, List<Object>> attributes = AbstractNISOp.constructNISUserAttributeParameters(entry, allowedNISattributes);
        
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
        
        try {
            connection.doSudoStart();
            connection.executeCommand(AbstractNISOp.whoIAm);
            try {
                connection.executeMutexAcquireScript(pwdMutexFile, tmpPwdMutexFile, pwdPidFile);

                final Pair<Boolean, String> response = initPasswordRecord1(accountName, uid, gid, homedir, recordUpdate);
                recordUpdate = response.first;
                final String passwordRecord1 = response.second;
                
                final String passwordRecord2 = (gecos != null) ? ("GECOS=\"" + gecos + "\"; ") : ("GECOS=`echo $ENTRYTEXT | cut -d: -f5`; ");
                if (gecos != null) {
                    recordUpdate = true;
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

                    if ((isRename == true) && connection.getConfiguration().isNisShadow()) {
                        // Make sure we get the rename to the shadow file 
                        String shadowfile = pwddir + "/shadow";
                        String shadowRename =
                            "GRPENTRY=`grep '^" + accountName + ":' " + shadowfile + "`; " +
                            "if [ -n \"$GRPENTRY\" ]; then " +
                              cpCmd + "-p " + shadowfile + " " + tmpPwdfile1 + "; " +
                              grepCmd + "-v \"^" + accountId + ":\" " + tmpPwdfile1 + " > " + tmpPwdfile2 + "; " +
                              chownCmd + "$WHOIAM " + tmpPwdfile2 + "\n " +
                              "echo $GRPENTRY | sed 's/^" +accountId + ":/" + newName + ":/g' >> " + tmpPwdfile2 + "; " +
                              diffCmd + shadowfile + " " + tmpPwdfile1 + " 2>&1 >/dev/null; " +
                              "RC=$?\n" +
                              "if [ $RC -eq 0 ]; then " +
                                cpCmd + "-f " + tmpPwdfile2 + " " + shadowfile + "; " +
                                chownCmd + "$OWNER:$GOWNER " + shadowfile + "; " +
                              "else\n" +
                              "GRPERRMSG=\"" + ERROR_MODIFYING + shadowfile + ", for entry " + newName + ".\"; " +
                              "fi; " +
                            "else\n" +
                            "GRPERRMSG=\"" + ERROR_MODIFYING + shadowfile + ", " + accountName + " not found.\"; " +
                            "fi";

                        getOwner =
                            "OWNER=`ls -l " + shadowfile + " | awk '{ print $3 }'`; " +
                            "GOWNER=`ls -l " + shadowfile + " | awk '{ print $4 }'`";

                        connection.executeCommand(getOwner);
                        connection.executeCommand(shadowRename);
                        connection.executeCommand(removeTmpFilesScript);
                    }

                    // Migrate the changes to the NIS database.
                    // The changes to the NIS database have to be made before the
                    // changes for shell and password.
                    AbstractNISOp.addNISMake("passwd", connection);
                }//if (recordUpdate)
                
                if (shell != null) {
                    addNISShellUpdate(accountId, shell, connection);
                }

                final GuardedString password = getPassword(entry);
                if (password != null) {
                    addNISPasswordUpdate(accountId, password, connection);
                }

                AbstractNISOp.addNISMake("passwd", connection);


            } finally {
                connection.executeMutexReleaseScript(pwdMutexFile);
            }
        } finally {
            connection.doSudoReset();
        }
    }

    private static void addNISPasswordUpdate(String account,
            GuardedString password, SolarisConnection connection) {
        final String passwdCmd = connection.buildCommand("yppasswd", account);
        connection.executeCommand(passwdCmd, Collections.<String>emptySet(), CollectionUtil.newSet(" password:"));
        SolarisConnection.sendPassword(password, Collections.<String>emptySet(), CollectionUtil.newSet("new password:"), connection);
        SolarisConnection.sendPassword(password, CollectionUtil.newSet(" denied"), Collections.<String>emptySet(), connection);
    }

    private static void addNISShellUpdate(String account, String shell,
            SolarisConnection connection) {
        final String passwdCmd = connection.buildCommand("passwd", "-r nis -e", account);
        
        final Set<String> chshReject = CollectionUtil.newSet("password:", "passwd:" /* // passwd: User unknown: <id>\nPermission denied\n */);
        connection.executeCommand(passwdCmd, chshReject, CollectionUtil.newSet("new shell:"));
        
        connection.executeCommand(shell, CollectionUtil.newSet("unacceptable as a new shell"));
    }

    private static String initPasswdVars(SolarisConfiguration configuration) {
        StringBuilder builder = new StringBuilder();
        builder.append(configuration.isNisShadow() ? "PASSWD=\"x\"; " : // password  held in shadow file
                "PASSWD=`echo $ENTRYTEXT | cut -d: -f2`; " // password in passwd file
        );
        builder.append("SHELL=`echo $ENTRYTEXT | cut -d: -f7`");

        return builder.toString();
    }

    private static Pair<Boolean,String> initPasswordRecord1(String accountName, String uid,
            String gid, String homedir, final boolean recordUpdate) {
        boolean _recordUpdate = recordUpdate;
        StringBuffer passwordRecord1 = new StringBuffer(
                // The connection to the resource is pooled.  Clear the environment
                // variables that will be used.
                "unset GRPERRMSG; unset dupuid; unset DUPUIDERRMSG; " 
                );
        passwordRecord1.append("ENTRYTEXT=`ypmatch " + accountName + " passwd`; ");
        
        
        if ((gid != null) && (gid.length() > 0)) { // "" gid does not make sense
            passwordRecord1.append("GROUP=`ypmatch " + gid + " group | cut -d: -f3`; ");
            passwordRecord1.append("if [ -z \"$GROUP\" ]; then\n");
            passwordRecord1.append("GRPERRMSG=\"" + NO_PRIMARY_GROUP + " matches in ypmatch " + gid + " group.\"; ");
            passwordRecord1.append("fi; ");
            _recordUpdate = true;
        } else {
            passwordRecord1.append("GROUP=`echo $ENTRYTEXT | cut -d: -f4`; ");
        }
        
        if (homedir != null) {
            passwordRecord1.append("HOMEDIR=" + homedir +"; ");
            _recordUpdate = true;            
        } else {
            passwordRecord1.append("HOMEDIR=`echo $ENTRYTEXT | cut -d: -f6`; ");
        }
        
        if ((uid != null) && (uid.length() > 0)) { // "" uid does not make sense
            passwordRecord1.append("NEWUID=" + uid + "; \\\n");
            //check whether newuid is duplicate or not.
            passwordRecord1.append("dupuid=`ypmatch \"$NEWUID\" passwd.byuid |  cut -d: -f3`; ");
            passwordRecord1.append("if [ \"$dupuid\" ]; then\n");
            passwordRecord1.append("DUPUIDERRMSG=\"" + UID_NOT_UNIQUE + " change uid " + uid  + " to some other unique value." + "\"; ");
            passwordRecord1.append("fi; ");
            _recordUpdate = true;
        } else {
            passwordRecord1.append("NEWUID=`echo $ENTRYTEXT | cut -d: -f3`; ");
        }
        
        return new Pair<Boolean, String>(_recordUpdate, passwordRecord1.toString());
    }

    /**
     * return the value of new name, based on the given entry
     * 
     * @param entry
     *            whose Attributes are searched for new value of
     *            {@link NativeAttribute#NAME}
     * @return the name of the entry:
     *         <ul>
     *         <li>no change in name -- return the original name</li>
     *         <li>change in name -- return the changed name</li>
     *         </ul>
     */
    private static String getNameValue(SolarisEntry entry) {
        String value = entry.getName();
        for (Attribute attr : entry.getAttributeSet()) {
            if (attr.is(NativeAttribute.NAME.getName())) {
                value = (String) attr.getValue().get(0);
                break;
            }
        }
        return value;
    }
}
