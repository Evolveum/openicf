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

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.ClosureFactory;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

import expect4j.matches.Match;

/**
 * Updates any {@link NativeAttribute}, except {@link OperationalAttributes#PASSWORD_NAME}.
 * 
 * @author David Adam
 * 
 */
class UpdateCommand extends CommandSwitches {
    private static final Match[] usermodErrors;
    static {
        MatchBuilder builder = new MatchBuilder();
        builder.addRegExpMatch("ERROR", ClosureFactory.newConnectorException("ERROR occured during update [usermod]"));
        builder.addRegExpMatch("command not found", ClosureFactory.newConnectorException("usermod command is not found"));
        builder.addRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("not allowed to execute usermod"));
        usermodErrors = builder.build();
    }
    
    private final static Map<NativeAttribute, String> updateSwitches;
    static {
        updateSwitches = new HashMap<NativeAttribute, String>(CommandSwitches.commonSwitches);
        updateSwitches.put(NativeAttribute.NAME, "-l"); // for new username attribute
    }
    
    public static void updateUser(SolarisEntry entry, SolarisConnection conn) {
        String newName = findNewName(entry);
        /*
         * UPDATE OF USER ATTRIBUTES (except password) {@see PasswdCommand}
         */
        String commandSwitches = CommandSwitches.formatCommandSwitches(entry, conn, updateSwitches);
        
        if (newName != null) {
            String newUserNameParams = " -l \"" + newName + "\" -G \"\"";
            commandSwitches += newUserNameParams;
        }

        if (commandSwitches.length() == 0) {
            return; // no update switch found, nothing to process
        }
        
        if (newName != null) {
            // The secondary groups the target user belongs to must
            // be extracted, the rename operation will temporarily
            // remove them to keep /etc/group clean.
            String groupsScript = getSecondaryGroupsScript(entry, conn);
            conn.executeCommand(groupsScript);
        }

        MatchBuilder builder = new MatchBuilder();
        builder.addRegExpMatch(conn.getRootShellPrompt(), ClosureFactory.newNullClosure());
        builder.addMatches(usermodErrors);
        try {
            conn.send(conn.buildCommand("usermod", commandSwitches, entry.getName()));
            conn.expect(builder.build());
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
        
        // If this is a rename operation, check to see if the user's
        // home directory needs to be renamed as well.
        if (newName != null) {
            // This script will restore the secondary groups to the
            // renamed user.
            final String updateGroupsCmd = conn.buildCommand("usermod", "-G \"$WSGROUPS\"", newName);
            executeCommandWithUserModErrors(updateGroupsCmd, conn);
            
            // Test to see if the user's home directory is to be renamed.
            // If a new home directory was specified as part of the rename
            // then skip this.
            if (!commandSwitches.contains("-d ")) {
             // Rename the home directory of the user to match the new
                // user name.  This will only be done if the basename of
                // the home directory matches the old username.  Also, if
                // the renamed home directory already exists, then the
                // rename of the home directory will not occur.
                final String renameDirScript = getRenameDirScript(entry, conn, newName);
                executeCommandWithUserModErrors(renameDirScript, conn);
            }
        }
    }

    private static String getRenameDirScript(SolarisEntry entry,
            SolarisConnection conn, String newName) {
        String renameDir =
            "NEWNAME=" + newName + "; " +
            "OLDNAME=" + entry.getName() + "; " +
            "OLDDIR=`" + conn.buildCommand("logins") + " -ox -l $NEWNAME | cut -d: -f6`; " +
            "OLDBASE=`basename $OLDDIR`; " +
            "if [ \"$OLDNAME\" = \"$OLDBASE\" ]; then\n" +
              "PARENTDIR=`dirname $OLDDIR`; " +
              "NEWDIR=`echo $PARENTDIR/$NEWNAME`; " +
              "if [ ! -s $NEWDIR ]; then " +
                conn.buildCommand("chown") + " $NEWNAME $OLDDIR; " +
                conn.buildCommand("mv") + " -f $OLDDIR $NEWDIR; " +
                "if [ $? -eq 0 ]; then\n" +
                  conn.buildCommand("usermod") + " -d $NEWDIR $NEWNAME; " +
                "fi; " +
              "fi; " +
            "fi";
        return renameDir;
    }

    /**
     * @param newName
     * @param conn
     */
    private static void executeCommandWithUserModErrors(String command,
            SolarisConnection conn) {
        
        MatchBuilder builder = new MatchBuilder();
        builder.addRegExpMatch(conn.getRootShellPrompt(), ClosureFactory.newNullClosure());
        builder.addMatches(usermodErrors);

        try {
            conn.send(command);
            conn.expect(builder.build());
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    private static String getSecondaryGroupsScript(SolarisEntry entry,
            SolarisConnection conn) {
        String getGroups =
            "n=1; " +
            "WSGROUPS=; " +
            "GROUPSWORK=`" + conn.buildCommand("logins") + " -m -l " + entry.getName() + " | awk '{ print $1 }'`; " +
            "for i in $GROUPSWORK; " +
            "do "  +
              "if [ $n -eq 1 ]; then\n" +
                "n=2; " +
              "else " +
                "if [ $n -eq 2 ]; then " +
                  "WSGROUPS=$i; " +
                  "n=3; " +
                "else\n" +
                  "WSGROUPS=`echo \"$WSGROUPS,$i\"`; " +
                "fi; " +
              "fi; " +
            "done";
        
        return getGroups;
    }

    private static String findNewName(SolarisEntry entry) {
        for (Attribute attr : entry.getAttributeSet()) {
            NativeAttribute nativeAttr = NativeAttribute.forAttributeName(attr.getName());
            if (nativeAttr.equals(NativeAttribute.NAME)) {
                return (String) attr.getValue().get(0);
            }
        }
        return null;
    }
}
