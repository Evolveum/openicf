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

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.ClosureFactory;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

import expect4j.matches.Match;

/**
 * @author David Adam
 *
 */
class CreateCommand extends CommandSwitches {
    private final static Match[] errorsUseradd;
    static {
        MatchBuilder builder = new MatchBuilder();
        builder.addCaseInsensitiveRegExpMatch("invalid", ClosureFactory.newConnectorException("ERROR during execution of 'useradd' -- invalid command"));
        builder.addCaseInsensitiveRegExpMatch("ERROR", ClosureFactory.newConnectorException("ERROR during execution of 'useradd'"));
        builder.addCaseInsensitiveRegExpMatch("command not found", ClosureFactory.newConnectorException("'useradd' command is missing"));
        builder.addCaseInsensitiveRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("Not allowed to execute the 'useradd' command."));
        errorsUseradd = builder.build();
    }
    private SolarisConnection conn;
    
    public CreateCommand(SolarisConnection conn) {
        this.conn = conn;
    }
    public void createUser(SolarisEntry entry/* , OperationOptions options */) {

        // create command line switches construction
        String commandSwitches = formatCreateCommandSwitches(entry);

        // useradd command execution
        String command = conn.buildCommand("useradd", commandSwitches, entry.getName());
        Match[] matches = prepareMatches(conn.getRootShellPrompt(), errorsUseradd);
        try {
            conn.send(command);
            conn.expect(matches);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    /**
     * creates command line switches construction
     * @param conn 
     */
    private String formatCreateCommandSwitches(SolarisEntry entry) {
        StringBuilder buffer = makeOptionalSkelDir();
        
        for (Attribute attr : entry.getAttributeSet()) {
            NativeAttribute nAttrName = NativeAttribute.fromString(attr.getName());
            // assuming Single values only
            String value = (attr.getValue().size() > 0) ? (String) attr.getValue().get(0) : null;

            /* 
             * append command line switch
             */
            String cmdSwitchForAttr = super.getCreateOrUpdate(nAttrName);
            if (cmdSwitchForAttr != null) {
                buffer.append(cmdSwitchForAttr);
                buffer.append(" ");

                // preprocess the values and use resource configuration values to control them.
                switch (nAttrName) {
                case DIR:
                    value = setHomeDirValue(value, entry);
                    break;
                case GROUP_PRIM:
                    value = setGroupPrimValue(value);
                    break;
                case SHELL:
                    value = setShellValue(value);
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
        return buffer.toString();
    }

    /**
     * @param explicitShellValue
     * @return
     */
    private String setShellValue(final String explicitShellValue) {
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
    private String setGroupPrimValue(final String explicitPrimGroupValue) {
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
    private StringBuilder makeOptionalSkelDir() {
        StringBuilder makeDirectory = new StringBuilder();
        Boolean configuredIsMakeDir = conn.getConfiguration().isMakeDir();
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
    private String setHomeDirValue(final String explicitHomeDirAttrValue, SolarisEntry entry) {
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
    
    public static void createGroup(SolarisEntry entry/*, OperationOptions options*/) {
        // TODO
        throw new UnsupportedOperationException();
    }
}
