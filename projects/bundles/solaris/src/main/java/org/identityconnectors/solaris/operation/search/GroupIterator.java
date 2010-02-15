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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

class GroupIterator implements Iterator<SolarisEntry> {

    private SolarisConnection conn;
    private Iterator<String> it;
    private SolarisEntry nextEntry;
    private Set<NativeAttribute> requiredAttrs;

    GroupIterator (Set<NativeAttribute> requiredAttrs, SolarisConnection connection) {
        this(Collections.<String>emptyList(), requiredAttrs, connection);
    }
    
    GroupIterator(List<String> groupNames,
            Set<NativeAttribute> requiredAttrs, SolarisConnection connection) {
        
        this.conn = connection;
        
        if (CollectionUtil.isEmpty(groupNames)) {
            String command = (!conn.isNis()) ? "cut -d: -f1 /etc/group | grep -v \"^[+-]\"" : "ypcat group | cut -d: -f1" ;
            String groupsSeparatedByNewline = connection.executeCommand(command);
            String[] groupNamesList = groupsSeparatedByNewline.split("\n");
            groupNames = Arrays.asList(groupNamesList);
        }
        it = groupNames.iterator();
        this.requiredAttrs = requiredAttrs;
    }

    public boolean hasNext() {
        while ((nextEntry == null) && it.hasNext()) {
            nextEntry = buildGroup(it.next());
        }
        return nextEntry != null;
    }

    /**
     * get the group entry for given username
     * @param name
     * @return the initialized entry, or Null in case the user was not found on the resource.
     */
    private SolarisEntry buildGroup(String groupName) {
        String cmd = (!conn.isNis()) ? "grep '^" + groupName + ":' /etc/group"
                : "ypmatch " + groupName + " group";
        String groupLine = conn.executeCommand(cmd);
        if (StringUtil.isBlank(groupLine) || (conn.isNis() && groupLine.toLowerCase().contains("can't match"))) {
            return null;
        }
        
        String[] groupTokens = groupLine.split(":", -1);
        if (groupTokens.length < 3)
            throw new ConnectorException("ERROR: invalid format of /etc/group file: <" + groupLine + ">");
        
        String name = groupTokens[0];
        if (!name.trim().equals(groupName))
            throw new ConnectorException("ERROR: parsed groupName and the given group differs. Requested: " + groupName + ", Found: " + name);
        
        String gid = groupTokens[2];
        // users is optional parameter, may be empty
        String usersLine = (groupTokens.length > 3) ? groupTokens[3].trim() : "";
        List<String> usersList = (!StringUtil.isBlank(usersLine)) ? Arrays.asList(usersLine.split(",")) : Collections.<String>emptyList();
        
        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(groupName).addAttr(NativeAttribute.NAME, groupName);
        for (NativeAttribute attrToGet : requiredAttrs) {
            switch (attrToGet) {
            case USERS:
                entryBuilder.addAttr(NativeAttribute.USERS, usersList);
                break;
            case ID:
                entryBuilder.addAttr(NativeAttribute.ID, gid);
                break;
            }
        }
        return entryBuilder.build();
    }

    public SolarisEntry next() {
        if (!hasNext())
            throw new NoSuchElementException();
        
        SolarisEntry result = nextEntry;
        nextEntry = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("Internal error: GroupIterators do not allow remove().");
    }

}
