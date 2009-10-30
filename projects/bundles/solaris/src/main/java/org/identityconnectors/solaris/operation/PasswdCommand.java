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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;


/**
 * @author David Adam
 *
 */
class PasswdCommand extends CommandSwitches {

    private static final String NEW_PASSWORD_MATCH = "ew Password:";
    
    // passwd operation switches
    private static final Map<NativeAttribute, String> _passwdSwitches;
    static {
        _passwdSwitches = new EnumMap<NativeAttribute, String>(NativeAttribute.class);
        _passwdSwitches.put(NativeAttribute.PWSTAT, "-f");
        //passwdSwitches.put(NativeAttribute.PW_LAST_CHANGE, null); // this is not used attribute (see LoginsCommand and its SVIDRA counterpart). TODO erase this comment.
        _passwdSwitches.put(NativeAttribute.MIN_DAYS_BETWEEN_CHNG, "-x");
        _passwdSwitches.put(NativeAttribute.MAX_DAYS_BETWEEN_CHNG, "-n");
        _passwdSwitches.put(NativeAttribute.DAYS_BEFORE_TO_WARN, "-w");
        _passwdSwitches.put(NativeAttribute.LOCK, "-l");
    }
    
    private final static Set<String> passwdRejects = CollectionUtil.newSet("Permission denied", "command not found", "not allowed to execute");
    
    public static void configureUserPassword(SolarisEntry entry, GuardedString password, SolarisConnection conn) {
        try {
            String command = conn.buildCommand("passwd -r files", entry.getName());
            conn.executeCommand(command, passwdRejects, CollectionUtil.newSet(NEW_PASSWORD_MATCH));

            SolarisConnection.sendPassword(password, Collections.<String>emptySet(), CollectionUtil.newSet(NEW_PASSWORD_MATCH), conn);

            SolarisConnection.sendPassword(password, Collections.<String>emptySet(), Collections.<String>emptySet(), conn);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }
    
    public static void configurePasswordProperties(SolarisEntry entry, SolarisConnection conn) {
        final String cmdSwitches = CommandSwitches.formatCommandSwitches(entry, conn, _passwdSwitches);
        if (cmdSwitches.length() == 0) {
            return; // no password related attribute present in the entry.
        }
        
        try {
            final String command = conn.buildCommand("passwd", cmdSwitches, entry.getName());
            final String out = conn.executeCommand(command);
            final String loweredOut = out.toLowerCase();
            if (loweredOut.contains("usage:") || loweredOut.contains("password aging is disabled") || loweredOut.contains("command not found")) {
                throw new ConnectorException("Error during configuration of password related attributes. Buffer content: <" + out + ">");
            }
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }
}
