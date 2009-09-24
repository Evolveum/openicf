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
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Switches for {@link CreateCommand} and {@link UpdateCommand}.
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
}
