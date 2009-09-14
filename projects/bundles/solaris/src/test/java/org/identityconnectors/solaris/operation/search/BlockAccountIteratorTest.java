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

import org.identityconnectors.common.Pair;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.junit.Assert;
import org.junit.Test;

public class BlockAccountIteratorTest {
    @Test 
    public void test() {
        // similar test to AccountIteratorTest
        Pair<SolarisConnection, CommandBuilder> pair = SolarisTestCommon.getSolarisConn();
        String command = pair.second.build("cut -d: -f1 /etc/passwd | grep -v \"^[+-]\"");
        String out = pair.first.executeCommand(command);
        final List<String> usernames = AccountUtil.getAccounts(out);
        SolarisConfiguration config = SolarisTestCommon.createConfiguration();
        
        BlockAccountIterator bai = new BlockAccountIterator(usernames, EnumSet.of(NativeAttribute.NAME), pair.first, pair.second, config , 2);
        List<String> retrievedUsernames = new ArrayList<String>();
        while (bai.hasNext()) {
            retrievedUsernames.add(bai.next().getName());
        }
        Assert.assertEquals(usernames, retrievedUsernames);
    }
}
