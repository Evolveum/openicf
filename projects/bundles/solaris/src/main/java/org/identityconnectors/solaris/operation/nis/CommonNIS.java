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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class CommonNIS extends AbstractNISOp {
    private static final String DEFAULT_NISPWDDIR = "/etc";
    
    public static String getNisPwdDir(SolarisConnection connection) {
        String pwdDir = connection.getConfiguration().getNisPwdDir();
        if (StringUtil.isBlank(pwdDir)) {
            pwdDir = DEFAULT_NISPWDDIR;
        }
        return pwdDir;
    }
    
    private static boolean isDefaultNisPwdDirImpl(String nisPwdDir) {
        return nisPwdDir.equals(DEFAULT_NISPWDDIR);
    }
    
    /**
     * evaluate if the NisPwdDir (NIS password directory) is configured to the
     * default one, defined by {@link CommonNIS#DEFAULT_NISPWDDIR}.
     */
    public static boolean isDefaultNisPwdDir(SolarisConnection conn) {
        return isDefaultNisPwdDirImpl(getNisPwdDir(conn));
    }
    
    public static void addNISMake(String target, SolarisConnection conn) {
        final String makeCmd = conn.buildCommand("/usr/ccs/bin/make");
        
        final String nisDir = getNISDir(conn);
        
        StringBuilder buildscript = new StringBuilder("nisdomain=`domainname`; ");
        buildscript.append("cd " + nisDir + "/$nisdomain\n");

        // TODO question: where do we get the Makefile???
        buildscript.append(makeCmd + "-f ../Makefile " + target + "; cd");
        try {
            conn.executeCommand(buildscript.toString());
            conn.waitForRootShellPrompt();// one of the waitFor(RootShellPrompt) is hidden in executeCommand impl.
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    private static String getNISDir(SolarisConnection conn) {
        final String nisDir = conn.getConfiguration().getNisDir();
        if (StringUtil.isBlank(nisDir)) {
            throw new ConnectorException("NIS directory not specified.");
        }
        return nisDir;
    }
    
    public static String getRemovePwdTmpFiles(SolarisConnection conn) {
        final String rmCmd = conn.buildCommand("rm");

        String removePwdTmpFiles =
            "if [ -f " + tmpPwdfile1 + " ]; then " +
              rmCmd + " -f " + tmpPwdfile1 + "; " +
            "fi; " +
            "if [ -f " + tmpPwdfile2 + " ]; then " +
              rmCmd + " -f " + tmpPwdfile2 + "; " +
            "fi; " +
            "if [ -f " + tmpPwdfile3 + " ]; then " +
              rmCmd + " -f " + tmpPwdfile3 + "; " +
            "fi";

        return removePwdTmpFiles;
    }
    
    /**
     * filters the given entry's attributes, so they are just the ones that are allowed NIS attributes.
     */
    public static Map<NativeAttribute, List<Object>> constructNISUserAttributeParameters(
            SolarisEntry entry, Set<NativeAttribute> allowedNISattributes) {
        
        Map<NativeAttribute, List<Object>> result = new HashMap<NativeAttribute, List<Object>>();
        
        for (Attribute attr : entry.getAttributeSet()) {
            String type = attr.getName();
            List<Object> value = attr.getValue();
            
            for (NativeAttribute nattr : allowedNISattributes) {
                if (type.equals(nattr.toString())) {
                    result.put(NativeAttribute.forAttributeName(type), value);
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * check if the users exists, that are given by the user list argument.
     * @param groupName name of the new group.
     * @param userNames The list of usernames that we want to add to the new group.
     * @param conn
     * 
     * Note: used in both native and NIS attributes.
     */
    public static void changeGroupMembers(String group,
            List<Object> userNames, boolean isNIS, SolarisConnection conn) {
        // Three temporary files are needed by this script.
        // One to serve as a baseline to make sure /etc/group doesn't
        // change underneath us.  The second is to serve as the destination
        // for the changes to be copied to /etc/group.  The third is as
        // an intermediate step so that sudo could function correctly.
        final String tmpfile1 = "/tmp/wsgroup.$$";
        final String tmpfile2 = "/tmp/wsgroupwork.$$";
        final String tmpfile3 = "/tmp/wsgroupwork2.$$";
        
        final String cpCmd = conn.buildCommand("/usr/bin/cp");
        final String rmCmd = conn.buildCommand("rm");
        final String mvCmd = conn.buildCommand("mv");
        final String grepCmd = conn.buildCommand("grep");
        final String sedCmd = conn.buildCommand("sed");
        final String diffCmd = conn.buildCommand("diff");
        final String chownCmd = conn.buildCommand("chown");
        final String realUserCmd;
        
        final String groupFile;
        
        if (!isNIS) {
            groupFile = "/etc/group";
            realUserCmd = conn.buildCommand("logins", "-ol $WSUSER 2>&1 | grep \"not found\"");
        } else {
            String pwdDir = CommonNIS.getNisPwdDir(conn);
            groupFile = pwdDir + "/group";
            
            realUserCmd = "ypmatch $WSUSER passwd 2>&1 | grep \"an't match key\"";
        }
        
        final String checkUsers =
            "for WSUSER in $WSUSERS;\n" +
            "do " +
              "USERTEXT=`" + realUserCmd + "`;\n" +
              "if [ -n \"$USERTEXT\" ]; then\n" +
                "if [ -z \"$BADWSUSERS\" ]; then\n" +
                  "BADWSUSERS=\"$WSUSER\";\n" +
                "else " +
                  "BADWSUSERS=\"$BADWSUSERS,$WSUSER\";\n" +
                "fi\n" +
              "else " +
                "if [ -z \"$ADDWSUSERS\" ]; then\n" +
                  "ADDWSUSERS=\"$WSUSER\";\n" +
                "else "+
                  "ADDWSUSERS=\"$ADDWSUSERS,$WSUSER\";\n" +
                "fi\n" +
              "fi\n" +
            "done";
        
        final String rmTmpFiles =
            "if [ -f " + tmpfile1 + " ]; then " +
              rmCmd + " -f " + tmpfile1 + "; " +
            "fi; " + 
            "if [ -f " + tmpfile2 + " ]; then " +
              rmCmd + " -f " + tmpfile2 + "; " +
            "fi; " +
            "if [ -f " + tmpfile3 + " ]; then " +
              rmCmd + " -f " + tmpfile3 + "; " +
            "fi";
        
        final String changeUsersSetup =
            cpCmd + " -p " + groupFile + " " + tmpfile1 + "; " +
            cpCmd + " -p " + groupFile + " " + tmpfile2;
        
        final String changeUsersEnv =
            "OWNER=`ls -l " + groupFile + " | awk '{ print $3 }'`; " +
            "GOWNER=`ls -l " + groupFile + " | awk '{ print $4 }'`; " +
            "GROUPTEXT=`" + grepCmd + " \"^$WSTARGETGRP:\" " + tmpfile1 + "`";
        
        final String changeUsers =
            "if [ -n \"$GROUPTEXT\" ]; then\n" +
          "GRPPWD=`echo $GROUPTEXT | awk -F: '{ print $2 }'`; " +
          "GRPGID=`echo $GROUPTEXT | awk -F: '{ print $3 }'`; " +
          "REPLTEXT=`echo $WSTARGETGRP:$GRPPWD:$GRPGID:`;\n" +
          sedCmd + " 's/'$GROUPTEXT'/'$REPLTEXT''$ADDWSUSERS'/g' " + tmpfile1 + " > " + tmpfile3 + "; \n" +
          diffCmd + " " + groupFile + " " + tmpfile1 + " 2>/dev/null 1>/dev/null; " +
          "RC=$?;\n" +
          "if [ $RC -eq 0 ]; then " +
                cpCmd + " -f " + tmpfile3 + " " + tmpfile2 + "; " +
                mvCmd + " -f " + tmpfile2 + " " + groupFile + ";\n" +
                chownCmd + " $OWNER:$GOWNER " + groupFile + "; " +
          "else " +
            "GRPERRMSG=\"Error modifying " + groupFile + ", edit $WSTARGETGRP to include correct groups.\"; \n" +
          "fi \n" +
        "else " +
          "GRPERRMSG=\"$WSTARGETGRP not found in " + groupFile + ".\"; " +
        "fi";
        
        // Clear the environment variables that will be used. The connection to
        // the resource is pooled.
        conn.executeCommand("BADWSUSERS=;ADDWSUSERS=;GRPERRMSG=");
        
        // for loop in bash script needs symbols separated by blank space.
        final String userList = prepareUserList(userNames);
        conn.executeCommand(String.format("WSUSERS=\"%s\"", userList));
        conn.executeCommand(checkUsers);
        final String badUsers = conn.executeCommand("echo $BADWSUSERS");
        if (badUsers.length() > 0) {
            throw new ConnectorException("SolarisConnector: users do not exist: " + badUsers);
        }
        
        conn.executeCommand("WSTARGETGRP='" + group + "'");
        conn.executeCommand(rmTmpFiles);
        conn.executeCommand(changeUsersSetup);
        conn.executeCommand(changeUsersEnv);
        conn.executeCommand(changeUsers);
        conn.executeCommand(rmTmpFiles);
        final String grpErrMsg = conn.executeCommand("echo $GRPERRMSG");
        if (grpErrMsg.length() > 0) {
            throw new ConnectorException(grpErrMsg);
        }

        // Reset the environment variables in case this connection
        // is reused for this task
        conn.executeCommand("unset BADWSUSERS;unset ADDWSUSERS;unset GRPERRMSG");
    }

    private static String prepareUserList(List<Object> userNames) {
        final String separator = " ";
        StringBuilder buff = new StringBuilder();
        for (Object name : userNames) {
            String username = (String) name;
            buff.append(username);
            buff.append(separator);
        }
        return buff.toString();
    }


}
