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

import java.util.EnumMap;
import java.util.Map;

import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.MatchBuilder;

import expect4j.matches.Match;

/**
 * Switches for {@link CreateCommand} and {@link UpdateCommand}.
 * @author David Adam
 *
 */
abstract class CommandSwitches {
    //create and update operation switches (identical for both operations)
    private static final Map<NativeAttribute, String> _CU_switches;
    static {
        _CU_switches = new EnumMap<NativeAttribute, String>(NativeAttribute.class);
        // values based on SVIDResourceAdapter's paramToFlagMap
        _CU_switches.put(NativeAttribute.ID, "-u");
        _CU_switches.put(NativeAttribute.GROUP_PRIM, "-g");
        _CU_switches.put(NativeAttribute.GROUPS_SEC, "-G");
        _CU_switches.put(NativeAttribute.DIR, "-d");
        _CU_switches.put(NativeAttribute.SHELL, "-s");
        _CU_switches.put(NativeAttribute.COMMENT, "-c");
        _CU_switches.put(NativeAttribute.USER_INACTIVE, "-f");
        _CU_switches.put(NativeAttribute.USER_EXPIRE, "-e");
        _CU_switches.put(NativeAttribute.AUTHS, "-A");
        _CU_switches.put(NativeAttribute.PROFILES, "-P");
        _CU_switches.put(NativeAttribute.ROLES, "-R");
        
// TODO what is the value for these attributes?
//        _CU_switches.put(NativeAttribute.NAME, null);
//        _CU_switches.put(NativeAttribute.LAST_LOGIN, null);
//        _CU_switches.put(NativeAttribute.USERS, null);
    }
    
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
    
    /**
     * get the command line switch used in {@link CreateCommand} and
     * {@link UpdateCommand}
     * 
     * @param attr
     *            the native attribute
     * @return the command line switch (for instance '-s' for shell of the user)
     *         for the given native attribute. Return null if switch doesn't
     *         exist.
     */
    protected String getCreateOrUpdate(NativeAttribute attr) {
        return _CU_switches.get(attr);
    }

    /**
     * get the command line switch used in {@link PasswdCommand}
     * 
     * @param attr
     *            the native attribute
     * @return the command line switch (for instance '-w' for
     *         {@link NativeAttribute#DAYS_BEFORE_TO_WARN}) for the given native
     *         attribute. Return null if switch doesn't exist.
     */
    protected String getPasswd(NativeAttribute attr) {
        return _passwdSwitches.get(attr);
    }
    
    protected static Match[] prepareMatches(String string, Match[] commonErrMatches) {
        MatchBuilder builder = new MatchBuilder();
        builder.addNoActionMatch(string);
        builder.addMatches(commonErrMatches);
        
        return builder.build();
    }
}
