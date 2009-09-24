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
        /*
         * UPDATE OF USER ATTRIBUTES (except password) {@see PasswdCommand}
         */
        final String commandSwitches = formatUpdateCommandSwitches(entry, conn, updateSwitches);

        if (commandSwitches.length() <= 0) {
            return;
        }
        
        try {
            MatchBuilder builder = new MatchBuilder();
            builder.addRegExpMatch(conn.getRootShellPrompt(), ClosureFactory.newNullClosure());
            builder.addMatches(usermodErrors);

            conn.send(conn.buildCommand("usermod", commandSwitches, entry.getName()));
            conn.expect(builder.build());
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }
    
    /**
     * creates command line switches construction
     * @param conn 
     * @param createSwitches2 
     */
    private static String formatUpdateCommandSwitches(SolarisEntry entry, SolarisConnection conn, Map<NativeAttribute, String> switches) {
        StringBuilder buffer = new StringBuilder();
        
        for (Attribute attr : entry.getAttributeSet()) {
            NativeAttribute nAttrName = NativeAttribute.forAttributeName(attr.getName());
            // assuming Single values only
            String value = (attr.getValue().size() > 0) ? (String) attr.getValue().get(0) : null;

            /* 
             * append command line switch
             */
            String cmdSwitchForAttr = switches.get(nAttrName);
            if (cmdSwitchForAttr != null) {
                buffer.append(cmdSwitchForAttr);
                buffer.append(" ");

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
}
