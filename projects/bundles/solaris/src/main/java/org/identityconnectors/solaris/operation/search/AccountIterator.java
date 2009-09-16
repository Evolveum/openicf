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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

public class AccountIterator implements Iterator<SolarisEntry> {

    private List<String> accounts;
    
    /** bunch of boolean flags says if the command is needed to be launched (based on attributes to get) */
    private boolean isLogins;
    private boolean isProfiles;
    private boolean isAuths;
    private boolean isLast;
    private boolean isRoles;

    private Iterator<String> it;
    private SolarisConnection conn;

    public AccountIterator(List<String> usernames, Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        this.conn = conn;
        
        accounts = usernames;
        it = accounts.iterator();
        
        isLogins = LoginsCommand.isLoginsRequired(attrsToGet);
        isProfiles = attrsToGet.contains(NativeAttribute.PROFILES);
        isAuths = attrsToGet.contains(NativeAttribute.AUTHS);
        isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);
        isRoles = attrsToGet.contains(NativeAttribute.ROLES);
    }
    
    public boolean hasNext() {
        return it.hasNext();
    }

    public SolarisEntry next() {
        String name = it.next();
        return buildUser(name);

    }

    private SolarisEntry buildUser(String name) {
        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(name).addAttr(NativeAttribute.NAME, name);
        if (isLogins) {
            entryBuilder.addAllAttributesFrom(LoginsCommand.getAttributesFor(name, conn));
        }
        if (isProfiles) {
            final Attribute profiles = ProfilesCommand.getProfilesAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.PROFILES, profiles.getValue());
        }
        if (isAuths) {
            final Attribute auths = AuthsCommand.getAuthsAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.AUTHS, auths.getValue());
        }
        if (isLast) {
            final Attribute last = LastCommand.getLastAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.LAST_LOGIN, last.getValue());
        }
        if (isRoles) {
            final Attribute roles = RolesCommand.getRolesAttributeFor(name, conn);
            entryBuilder.addAttr(NativeAttribute.ROLES, roles.getValue());
        }
        return entryBuilder.build();
    }

    public void remove() {
        throw new UnsupportedOperationException("Internal error: AccountIterators do not allow remove().");
    }

}
