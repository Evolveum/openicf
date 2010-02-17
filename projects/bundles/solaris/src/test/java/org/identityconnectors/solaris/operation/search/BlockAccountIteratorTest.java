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
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.junit.Assert;
import org.junit.Test;

public class BlockAccountIteratorTest extends SolarisTestBase {
    @Test 
    public void test() {
        if (getConnection().isNis()) {
            // skip tests for NIS configuration, as block account iterator doesn't support iterating over NIS accounts.
            return;
        }
        
        // similar test to AccountIteratorTest
        String command = (!getConnection().isNis()) ? getConnection().buildCommand("cut -d: -f1 /etc/passwd | grep -v \"^[+-]\"") : "ypcat passwd | cut -d: -f1";
        String out = getConnection().executeCommand(command);
        String[] tmp = out.split("\n");
        List<String> usernames = CollectionUtil.newList();
        for (String string : tmp) {
            usernames.add(string.trim());
        }
        
        BlockAccountIterator bai = new BlockAccountIterator(usernames, EnumSet.of(NativeAttribute.NAME), 2 , getConnection());
        List<String> retrievedUsernames = new ArrayList<String>();
        while (bai.hasNext()) {
            retrievedUsernames.add(bai.next().getName());
        }
        Assert.assertEquals(CollectionUtil.newSet(usernames), CollectionUtil.newSet(retrievedUsernames));
        
        try {
            bai.next();
            Assert.fail("no exception thrown when next was called more time than there are elements in the list.");
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
