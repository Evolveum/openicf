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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class OpCreateNISImpl {
    private static final Log _log = Log.getLog(OpCreateNISImpl.class);
    
    private final static Set<NativeAttribute> allowedNISattributes;
    static {
        allowedNISattributes = new HashSet<NativeAttribute>();
        allowedNISattributes.add(NativeAttribute.ID);
        allowedNISattributes.add(NativeAttribute.GROUP_PRIM);
        allowedNISattributes.add(NativeAttribute.DIR);
        allowedNISattributes.add(NativeAttribute.COMMENT);
        allowedNISattributes.add(NativeAttribute.SHELL);
    }
    
    // Temporary file names
    private static final String tmpPwdfile1 = "/tmp/wspasswd.$$";
    private static final String tmpPwdfile2 = "/tmp/wspasswd_work.$$";
    private static final String tmpPwdfile3 = "/tmp/wspasswd_out.$$";
    
    public static void addNISMake(String target, SolarisConnection conn) {
        final String makeCmd = conn.buildCommand("/usr/ccs/bin/make");
        
        final String nisDir = getNISDir(conn);
        
        StringBuilder buildscript = new StringBuilder("nisdomain=`domainname`; ");
        buildscript.append("cd " + nisDir + "/$nisdomain\n");

        // TODO question: where do we get the Makefile???
        buildscript.append(makeCmd + "-f ../Makefile " + target + "; cd");
        try {
            conn.send(buildscript.toString());
            conn.waitFor(conn.getRootShellPrompt());
            conn.waitFor(conn.getRootShellPrompt());
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    private static String getNISDir(SolarisConnection conn) {
        final String nisDir = conn.getConfiguration().getNisDir();
        if ((nisDir == null) || (nisDir.length() == 0)) {
            throw new ConnectorException("NIS directory not specified.");
        }
        return nisDir;
    }

    public static void performNIS(String pwdDir, SolarisEntry entry, SolarisConnection connection) {

        final SolarisConfiguration config = connection.getConfiguration();
        
        final String accountId = entry.getName();
        
        String shell = null;
        String uid = null; 
        String gid = null;
        String gecos = null;
        String homedir = null;

        String pwdfile = pwdDir + "/passwd";
        String shadowfile = pwdDir + "/shadow";
        String salt = "";
        StringBuffer passwordRecord;
        String shadowOwner = "";
        String shadowRecord = "";
        
        boolean shadow = config.isNisShadow();
        String cpCmd;
        String chownCmd;
        String diffCmd;
        String removeTmpFilesScript = getRemovePwdTmpFiles(connection);
        
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
        //Map<NativeAttribute, String> attributes = constructNISUserAttributeParameters(user, allowedNISAttributes);
    }
    
    private static String getRemovePwdTmpFiles(SolarisConnection conn)
    {
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
    
    
}
