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
    
   
    
    static String getNisPwdDir(SolarisConnection connection) {
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
            conn.waitFor(conn.getRootShellPrompt());// one of the waitFor(RootShellPrompt) is hidden in executeCommand impl.
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

}
