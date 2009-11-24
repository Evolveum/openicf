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

import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * @author David Adam
 *
 */
class CreateNativeUserCommand  {
    private final static Map<NativeAttribute, String> createSwitches = CommandSwitches.commonSwitches;
    
    private final static Set<String> errorsUseradd = CollectionUtil.newSet("invalid", "ERROR", "command not found", "not allowed to execute");
    
    public static void createUser(SolarisEntry entry, GuardedString password, SolarisConnection conn) {
        conn.doSudoStart();
        try {

            conn.executeMutexAcquireScript();
            try {
                createUserImpl(entry, conn);
            } finally {
                conn.executeMutexReleaseScript();
            }
            
            PasswdCommand.configureUserPassword(entry, password, conn);
            PasswdCommand.configurePasswordProperties(entry, conn);
        } finally {
            conn.doSudoReset();
        }
    }
    
    private static void createUserImpl(SolarisEntry entry, SolarisConnection conn) {

        // create command line switches construction
        String commandSwitches = formatCreateCommandSwitches(entry, conn);

        // useradd command execution
        String command = conn.buildCommand("useradd", commandSwitches, entry.getName());
        conn.executeCommand(command, errorsUseradd);
    }

    /**
     * creates command line switches construction
     * 
     * @param entry
     *            the entry whose attribute should be transferred to command
     *            line switches.
     * @param conn
     * @return the create command line switches based on entry's attribute/value
     *         pairs. Return a zero-length string in case no switch (
     *         {@link CreateNativeUserCommand#createSwitches}) matched the attributes in
     *         given entry.
     */
    private static String formatCreateCommandSwitches(SolarisEntry entry, SolarisConnection conn) {
        StringBuilder buffer = makeOptionalSkelDir(conn);
        
        for (Attribute attr : entry.getAttributeSet()) {
            NativeAttribute nAttrName = NativeAttribute.forAttributeName(attr.getName());
            // assuming Single values only
            String value = (attr.getValue().size() > 0) ? (String) attr.getValue().get(0) : null;

            /* 
             * append command line switch
             */
            String cmdSwitchForAttr = createSwitches.get(nAttrName);
            if (cmdSwitchForAttr != null) {
                buffer.append(cmdSwitchForAttr);
                buffer.append(" ");

                // preprocess the values and use resource configuration values to control them.
                switch (nAttrName) {
                case DIR:
                    value = setHomeDirValue(value, entry, conn);
                    break;
                case GROUP_PRIM:
                    value = setGroupPrimValue(value, conn);
                    break;
                case SHELL:
                    value = setShellValue(value, conn);
                    break;
                }

                /*
                 * append the single-value for the given switch
                 */
                if (value != null) {
                    // quote value
                    buffer.append("\"" + value + "\"");
                    buffer.append(" ");
                }
            }
        }// for
        return buffer.toString().trim();
    }

    /**
     * @param explicitShellValue
     * @return
     */
    private static String setShellValue(final String explicitShellValue, SolarisConnection conn) {
        if (explicitShellValue != null)
            return explicitShellValue;
        
        String loginShell = null;
        String configuredLoginShell = conn.getConfiguration().getLoginShell();
        if ((configuredLoginShell != null) && (configuredLoginShell.length() > 0)) {
            // If there is a specific login shell specified for the
            // user then that takes priority.
            loginShell = configuredLoginShell;
        }
        return loginShell;
    }
    /**
     * @param explicitPrimGroupValue
     * @return
     */
    private static String setGroupPrimValue(final String explicitPrimGroupValue, SolarisConnection conn) {
        if (explicitPrimGroupValue != null)
            return explicitPrimGroupValue;
        
        String defaultPrimaryGroup = null;
        String configuredDefPrimGroup = conn.getConfiguration().getDefaultPrimaryGroup();
        if ((configuredDefPrimGroup != null) && (configuredDefPrimGroup.length() > 0)) {
            // If there is a specific default primary group specified for the
            // user then that takes priority.
            defaultPrimaryGroup = configuredDefPrimGroup;
        }
        return defaultPrimaryGroup;
    }
    
    /**
     * optionally create a skeleton directory if given in
     * {@link SolarisConfiguration#isMakeDir()}
     * 
     * IMPL. NOTE: <i>This method is used to initialize the buffer for command switches.</i>
     * 
     * @return an empty {@link StringBuilder} if the optionalProperty is not
     *         defined, otherwise the create the default skeleton directory for
     *         home.
     */
    private static StringBuilder makeOptionalSkelDir(SolarisConnection conn) {
        StringBuilder makeDirectory = new StringBuilder();
        Boolean configuredIsMakeDir = conn.getConfiguration().getMakeDir();
        if ((configuredIsMakeDir != null) && configuredIsMakeDir) {
            makeDirectory.append(" -m");
            String skeldir = conn.getConfiguration().getSkelDir();

            if (skeldir != null) {
                // note switch '-k' is specific for making skeleton directory, is not assigned to any {@link NativeAttribute}
                makeDirectory.append(" -k " + skeldir + " ");
            }
        }
        return makeDirectory;
    }
    
    /**
     * set the value for {@link NativeAttribute#DIR} according to the
     * {@link SolarisConfiguration#getHomeBaseDir()} resource configuration
     * (optional)
     * 
     * @param explicitHomeDirAttrValue
     *            explicit value for Home directory, if it is null then the
     *            default is used from the resource configuration property.
     * @param conn
     * @return the value with correct settings
     */
    private static String setHomeDirValue(final String explicitHomeDirAttrValue, SolarisEntry entry, SolarisConnection conn) {
        if (explicitHomeDirAttrValue != null)
            return explicitHomeDirAttrValue;
        
        String homeDirectory = null;
        
        String basedir = conn.getConfiguration().getHomeBaseDir();
        if ((basedir != null) && (basedir.length() > 0)) {
            // If there is a specific home directory specified for the
            // user then that takes priority.
            StringBuffer homedirBuffer = new StringBuffer(basedir);

            if (!basedir.endsWith("/")) {
                homedirBuffer.append("/");
            }

            homedirBuffer.append(entry.getName());
            homeDirectory = homedirBuffer.toString();
        }
        return homeDirectory;
    }
}
