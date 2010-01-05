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
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.junit.Assert;
import org.junit.Test;


public class GroupIteratorTest extends SolarisTestBase {
    @Test
    public void test() {
        // similar test to BlockAccountIteratorTest
        String command = (getConnection().isNis()) ? "ypcat group | cut -d: -f1" : "cut -d: -f1 /etc/group | grep -v \"^[+-]\"";
        String out = getConnection().executeCommand(command);
        final List<String> groups = SolarisEntries.getNewlineSeparatedItems(out);
        
        GroupIterator gi = new GroupIterator(groups, EnumSet.of(NativeAttribute.NAME, NativeAttribute.USERS, NativeAttribute.ID), getConnection());
        List<String> retrievedGroups = new ArrayList<String>();
        while (gi.hasNext()) {
            SolarisEntry currentGroup = gi.next();
            retrievedGroups.add(currentGroup.getName());
            Assert.assertNotNull(currentGroup.searchForAttribute(NativeAttribute.ID));
            Attribute users = currentGroup.searchForAttribute(NativeAttribute.USERS);
            for (Object it : users.getValue()) {
                Assert.assertNotNull(it);
            }
        }
        Assert.assertEquals(CollectionUtil.newSet(groups), CollectionUtil.newSet(retrievedGroups));
        
        try {
            gi.next();
            Assert.fail("no Exception was thrown after invalid call of next.");
        } catch (NoSuchElementException nex) {
            // OK
        }
    }
    
    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
}
