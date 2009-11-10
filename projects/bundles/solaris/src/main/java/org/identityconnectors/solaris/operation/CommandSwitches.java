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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * Switches for {@link CreateNativeUserCommand} and {@link UpdateCommand}.
 * @author David Adam
 *
 */
class CommandSwitches {
    //create and update operation switches (identical for both operations)
    static final Map<NativeAttribute, String> commonSwitches;

    static {
        Map<NativeAttribute, String> switchMap = new EnumMap<NativeAttribute, String>(NativeAttribute.class);
        // values based on SVIDResourceAdapter's paramToFlagMap
        switchMap.put(NativeAttribute.ID, "-u");
        switchMap.put(NativeAttribute.GROUP_PRIM, "-g");
        switchMap.put(NativeAttribute.GROUPS_SEC, "-G");
        switchMap.put(NativeAttribute.DIR, "-d");
        switchMap.put(NativeAttribute.SHELL, "-s");
        switchMap.put(NativeAttribute.COMMENT, "-c");
        switchMap.put(NativeAttribute.USER_INACTIVE, "-f");
        switchMap.put(NativeAttribute.USER_EXPIRE, "-e");
        switchMap.put(NativeAttribute.AUTHS, "-A");
        switchMap.put(NativeAttribute.PROFILES, "-P");
        switchMap.put(NativeAttribute.ROLES, "-R");
        //switchMap.put(NativeAttribute.PASSWD, null); // password doesn't have ANY switch.
        commonSwitches = CollectionUtil.asReadOnlyMap(switchMap);
        
// TODO what is the value for these attributes?
//        _CU_switches.put(NativeAttribute.NAME, null); // Create only
//        _CU_switches.put(NativeAttribute.LAST_LOGIN, null);
//        _CU_switches.put(NativeAttribute.USERS, null);
    }
    
    /**
     * creates command line switches construction
     * 
     * @param entry
     *            the account that is source of values for the switches
     * @param conn
     * @param command
     *            line switches
     * @return the formatted -switch "value" pairs, separated by space. Returns
     *         a zero-length string, in case no switch matched the attributes in
     *         the given entry.
     */
    static String formatCommandSwitches(SolarisEntry entry, SolarisConnection conn, Map<NativeAttribute, String> switches) {
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
        return buffer.toString().trim();
    }
}
