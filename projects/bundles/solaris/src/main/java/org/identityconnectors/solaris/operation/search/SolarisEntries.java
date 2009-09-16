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

import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Basic retrieval of Items from Solaris Resource.
 * 
 * @author David Adam
 */
public class SolarisEntries {
    private SolarisConnection conn;

    public SolarisEntries(SolarisConnection conn) {
        this.conn = conn;
    }

    public Iterator<SolarisEntry> getAllAccounts(Set<NativeAttribute> attrsToGet) {
        return AccountUtil.getAllAccounts(conn, attrsToGet);
    }

    // public abstract Iterator<SolarisEntry> getAllGroups();

    public SolarisEntry getAccount(String name, Set<NativeAttribute> attrsToGet) {
        return AccountUtil.getAccount(conn, name, attrsToGet);
    }



    // public abstract SolarisEntry getGroup(String groupName,
    // Set<NativeAttribute> attrsToGet);
}
