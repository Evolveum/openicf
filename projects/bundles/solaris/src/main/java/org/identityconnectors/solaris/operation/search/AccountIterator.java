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
import java.util.NoSuchElementException;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

public class AccountIterator implements Iterator<SolarisEntry> {

    /** bunch of boolean flags says if the command is needed to be launched (based on attributes to get) */
    private boolean isLogins;
    private boolean isProfiles;
    private boolean isAuths;
    private boolean isLast;
    private boolean isRoles;

    private Iterator<String> it;
    private SolarisConnection conn;
    
    private SolarisEntry nextEntry;

    public AccountIterator(List<String> usernames, Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        this.conn = conn;
        
        it = usernames.iterator();
        
        isLogins = LoginsCommand.isLoginsRequired(attrsToGet);
        isProfiles = attrsToGet.contains(NativeAttribute.PROFILES);
        isAuths = attrsToGet.contains(NativeAttribute.AUTHS);
        isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);
        isRoles = attrsToGet.contains(NativeAttribute.ROLES);
    }
    
    public boolean hasNext() {
        while ((nextEntry == null) && it.hasNext()) {
            nextEntry = buildUser(it.next());
        }
        return nextEntry != null;
    }

    /**
     * @return the next user as {@link SolarisEntry} or null instead if the user
     *         does not exist on the resource.
     */
    public SolarisEntry next() {
        if (!hasNext())
            throw new NoSuchElementException();
        
        SolarisEntry result = nextEntry;
        nextEntry = null;
        return result;
    }

    /**
     * get the user entry for given username
     * @param name
     * @return the initialized entry, or Null in case the user was not found on the resource.
     */
    private SolarisEntry buildUser(String name) {
        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(name).addAttr(NativeAttribute.NAME, name);
        
        // we need to execute Logins command always, to figure out if the user exists at all.
        SolarisEntry loginsEntry = LoginsCommand.getAttributesFor(name, conn);
        
        if (isLogins) {
            // Null indicates that the user was not found.
            if (loginsEntry == null)
                return null;
            
            entryBuilder.addAllAttributesFrom(loginsEntry);
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
