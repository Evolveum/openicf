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
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Basic retrieval of Items from Solaris Resource.
 * 
 * @author David Adam
 */
public class SolarisEntries {

    public static Iterator<SolarisEntry> getAllAccounts(Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        // Impl. note: use AccountIterator in case specific attributes are required, that we won't be able to fetch from BlockAccountIteratr
        if (conn.isNis() || attrsToGet.contains(NativeAttribute.PROFILES) || attrsToGet.contains(NativeAttribute.AUTHS) || attrsToGet.contains(NativeAttribute.ROLES)) {
            return new AccountIterator(attrsToGet, conn);
        }
        
        // BlockAccount iterator is optimized for fetching info from 'logins' command and 'last login time' only.
        return new BlockAccountIterator(attrsToGet, conn);
    }

    public static Iterator<SolarisEntry> getAllGroups(Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        return new GroupIterator(attrsToGet, conn);
    }

    /**
     * @return the {@link SolarisEntry} if the user with given name was found. If the user was not found return null.
     */
    public static SolarisEntry getAccount(String name, Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        // the result will be an Iterable collection with a single element returned.
        AccountIterator it = new AccountIterator(CollectionUtil.newList(name), attrsToGet, conn);
        return (it != null && it.hasNext()) ? it.next() : null;
    }

    public static SolarisEntry getGroup(String groupName, Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        // the result will be an Iterable collection with a single element returned.
        GroupIterator it = new GroupIterator(CollectionUtil.newList(groupName), attrsToGet, conn);
        return (it != null && it.hasNext()) ? it.next() : null;
    }
}
