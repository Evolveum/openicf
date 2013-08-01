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

import java.util.List;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class CreateNISGroup extends AbstractNISOp {

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
        final String pwddir = conn.getConfiguration().getNisPwdDir();
        StringBuffer groupRecord;
        String gid = null;
        String groupFile = pwddir + "/group";
        final String removeTmpFilesScript = AbstractNISOp.getRemoveGroupTmpFiles(conn);
        final String cpCmd = conn.buildCommand(false, "cp");
        final String chownCmd = conn.buildCommand(true, "chown");
        final String diffCmd = conn.buildCommand(false, "diff");
        final String grepCmd = conn.buildCommand(false, "grep");
        final String catCmd = conn.buildCommand(false, "cat");

        conn.executeCommand(AbstractNISOp.WHO_I_AM);

        /*
         * FIXME: The connector behaves differently from adapter. Adapter
         * behavior: in case 'saveAs' operation is performed, disregard the
         * given GroupId, as it belongs to the cloned group. (If used it, we'd
         * have a duplicate groupId)
         *
         * Connector behavior: we're not able to detect 'saveAs' operation yet.
         * So we apply ostrich strategy, until the framework is not able to
         * provide this information. -- Proposal: introduce a 'saveAs' operation
         * option to signal the saveAs operation, so groupId can be ignored
         * then.
         *
         * Occurences (2):
         * org.identityconnectors.solaris.operation.nis.CreateNISGroupCommand
         * org.identityconnectors.solaris.operation.CreateNativeGroupCommand
         */
        Attribute groupIdAttr = group.searchForAttribute(NativeAttribute.ID);
        if (groupIdAttr != null) {
            gid = AttributeUtil.getStringValue(groupIdAttr);
        }

        // Create and perform script for adding group file entry
        groupRecord = new StringBuffer();
        if (StringUtil.isNotBlank(gid)) {
            groupRecord.append("newgid=" + gid + "; ");
        }

        final String getOwner = initGetOwner(groupFile);

        // @formatter:off
        final String createRecord =
            "WS_GROUPNAME=`" + grepCmd + "\"^" + groupName + ":\" " + groupFile + "`; " +
            "WS_GROUPID=`" + catCmd + groupFile + " | cut -d: -f3 | grep $newgid`; " +
            "if [ -z \"$WS_GROUPNAME\" ]; then\n" +
              "if [ -z \"$WS_GROUPID\" ]; then " +
                cpCmd + "-p " + groupFile + " " + TMP_GROUPFILE_1 + "; " +
                cpCmd + "-p " + groupFile + " " + TMP_GROUPFILE_2 + "; " +
                chownCmd + "$WHOIAM " + TMP_GROUPFILE_2 + "\n " +
                "echo \"" + groupName + "::$newgid:" + "\" >> " + TMP_GROUPFILE_2 + ";" +
                diffCmd + groupFile + " " + TMP_GROUPFILE_1 + "; " +
                "RC=$?; " +
                "if [ $RC -eq 0 ]; then\n" +
                  cpCmd + "-f " + TMP_GROUPFILE_2 + " " + groupFile + "; " +
                  chownCmd + "$OWNER:$GOWNER " + groupFile + "; " +
                "fi\n" +
              "else " +
                "echo \"" + AbstractNISOp.DUPLICATE_GROUP_ID_MSG + "\"; " +
              "fi; " +
            "else " +
              "echo \"" + AbstractNISOp.DUPLICATE_GROUP_NAME_MSG + "\"; " +
            "fi";
        // @formatter:on

        groupRecord.append(createRecord);

        conn.executeMutexAcquireScript(GRP_MUTEX_FILE, TMP_GRP_MUTEX_FILE, GRP_PID_FILE);
        try {
            // Determine the group's id if not specified
            if (StringUtil.isBlank(gid)) {
                String gidScript = getNISNewGidScript();
                conn.executeCommand(gidScript);
            }

            conn.executeCommand(removeTmpFilesScript);
            conn.executeCommand(getOwner);

            final String groupRecourdOutput = conn.executeCommand(groupRecord.toString());
            parseNisOutputForErrors(groupRecourdOutput);

            conn.executeCommand(removeTmpFilesScript);

            Attribute usersAttr = group.searchForAttribute(NativeAttribute.USERS);
            if (usersAttr != null) {
                final List<Object> usersValue = usersAttr.getValue();
                Assertions.nullCheck(usersValue, "users list");
                AbstractNISOp.changeGroupMembers(groupName, usersValue, true, conn);
            }

            // NIS database has to be updated before updates to shell or
            // password
            AbstractNISOp.addNISMake("group", conn);
        } finally {
            conn.executeMutexReleaseScript(GRP_MUTEX_FILE);
        }
    }

    private static String getNISNewGidScript() {
        // @formatter:off
        String script =
            "mingid=100; " +
            "newgid=`ypcat group | sort -n -t: -k3 | tail -1 | cut -d: -f3`; " +
            // prevent -lt from failing when there are no groups
            "if [ -z \"$newgid\" ]; then\n" +
              "newgid=$mingid; " +
            "fi; " +
            "newgid=`expr $newgid + 1`; " +
            "if [ $newgid -lt $mingid ]; then\n" +
              "newgid=$mingid; " +
            "fi";
        // @formatter:on
        return script;
    }
}
