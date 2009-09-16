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

package org.identityconnectors.solaris.operation.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;


public class AccountUtil {
    public static SolarisEntry getAccount(SolarisConnection conn, String name, Set<NativeAttribute> attrsToGet) {
        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(name).addAttr(NativeAttribute.NAME, name);
        if (SearchHelper.isLoginsRequired(attrsToGet)) {
            entryBuilder.addAllAttributesFrom(LoginsCmd.getAttributesFor(name, conn));
        }

        if (attrsToGet.contains(NativeAttribute.PROFILES)) {
            final Attribute profiles = ProfilesCmd.getProfilesAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.PROFILES, profiles.getValue());
        }
        
        if (attrsToGet.contains(NativeAttribute.AUTHS)) {
            final Attribute auths = AuthsCmd.getAuthsAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.AUTHS, auths.getValue());
        }
        
        if (attrsToGet.contains(NativeAttribute.LAST_LOGIN)) {
            final Attribute last = LastCmd.getLastAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.LAST_LOGIN, last.getValue());
        }
        
        if (attrsToGet.contains(NativeAttribute.ROLES)) {
            final Attribute roles = RolesCmd.getRolesAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.ROLES, roles.getValue());
        }

        return entryBuilder.build();
    }

    public static Iterator<SolarisEntry> getAllAccounts(SolarisConnection conn,
            SolarisConfiguration config, Set<NativeAttribute> attrsToGet) {
        String command = conn.buildCommand("cut -d: -f1 /etc/passwd | grep -v \"^[+-]\"");
        String out = conn.executeCommand(command);
        
        List<String> accountNames = getAccounts(out);
        if (accountNames.size() < 1) {
            throw new RuntimeException("Internal error: the number of retrieved accounts names is: " + accountNames.size());
        }

        // Impl. note: use AccountIterator in case specific attributes are required, that we won't be able to fetch from BlockAccountIteratr
        if (attrsToGet.contains(NativeAttribute.PROFILES) || attrsToGet.contains(NativeAttribute.AUTHS) || attrsToGet.contains(NativeAttribute.ROLES)) {
            return new AccountIterator(accountNames, attrsToGet, conn);
        }
        
        // BlockAccount iterator is optimized for fetching info from 'logins' command and 'last login time' only.
        return new BlockAccountIterator(accountNames, attrsToGet, conn, config, 30);
    }

    static List<String> getAccounts(String out) {
        String[] tokens = out.split("\n");
        List<String> result = new ArrayList<String>(tokens.length);
        for (String string : tokens) {
            result.add(string.trim());
        }
        return result;
    }
}