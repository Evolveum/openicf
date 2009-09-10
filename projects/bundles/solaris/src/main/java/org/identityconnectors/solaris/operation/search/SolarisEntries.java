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

import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;

/**
 * Basic retrieval of Items from Solaris Resource.
 * 
 * @author David Adam
 */
public class SolarisEntries {
    private SolarisConnection conn;
    private CommandBuilder bldr;

    public SolarisEntries(SolarisConnection conn, CommandBuilder bldr) {
        this.conn = conn;
        this.bldr = bldr;
    }

    // public abstract Iterator<SolarisEntry> getAllAccounts();

    // public abstract Iterator<SolarisEntry> getAllGroups();

    public SolarisEntry getAccount(String name, Set<NativeAttribute> attrsToGet) {
        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(name).addAttr(NativeAttribute.NAME, name);
        if (isLoginsRequired(attrsToGet)) {
            entryBuilder.addAllAttributesFrom(LoginsCmd.getAttributesFor(name, conn, bldr));
        }

        if (attrsToGet.contains(NativeAttribute.PROFILES)) {
            final Attribute profiles = ProfilesCmd.getProfilesAttributeFor(name, conn, bldr);
            entryBuilder.addAttr(NativeAttribute.PROFILES, profiles.getValue());
        }
        
        if (attrsToGet.contains(NativeAttribute.AUTHS)) {
            final Attribute auths = AuthsCmd.getAuthsAttributeFor(name, conn, bldr);
            entryBuilder.addAttr(NativeAttribute.AUTHS, auths.getValue());
        }
        
        if (attrsToGet.contains(NativeAttribute.LAST_LOGIN)) {
            final Attribute last = LastCmd.getLastAttributeFor(name, conn, bldr);
            entryBuilder.addAttr(NativeAttribute.LAST_LOGIN, last.getValue());
        }
        
        if (attrsToGet.contains(NativeAttribute.ROLES)) {
            final Attribute roles = RolesCmd.getRolesAttributeFor(name, conn, bldr);
            entryBuilder.addAttr(NativeAttribute.ROLES, roles.getValue());
        }

        return entryBuilder.build();
    }

    /**
     * evaluate if logins command is necessary to launch.
     * 
     * @param attrsToGet
     *            attributes to get
     * @return true if {@link LoginsCmd} is required to be called, as it
     *         processes one of the attributes to get.
     */
    private boolean isLoginsRequired(Set<NativeAttribute> attrsToGet) {
        for (NativeAttribute nativeAttribute : attrsToGet) {
            if (LoginsCmd.isProvided(nativeAttribute)) {
                return true;
            }
        }
        return false;
    }

    // public abstract SolarisEntry getGroup(String groupName,
    // Set<NativeAttribute> attrsToGet);
}
