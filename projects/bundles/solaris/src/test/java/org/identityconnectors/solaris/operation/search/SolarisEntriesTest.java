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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.identityconnectors.common.Pair;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.junit.Assert;
import org.junit.Test;

public class SolarisEntriesTest {
    @Test
    public void testGetAccount() {
        Pair<SolarisConnection, CommandBuilder> pair = SolarisTestCommon.getSolarisConn();
        SolarisEntries se = new SolarisEntries(pair.first, pair.second);
        final String userName = "root";
        SolarisEntry result = se.getAccount(userName, EnumSet.of(NativeAttribute.AUTHS, NativeAttribute.PROFILES, NativeAttribute.NAME));
        Assert.assertTrue(result.getName().equals(userName));
        Set<Attribute> set = result.getAttributeSet();
        Assert.assertNotNull(set);
        
        boolean isAuths = false;
        boolean isProfiles = false;
        for (Attribute attribute : set) {
            if (!isAuths)
                isAuths = SolarisTestCommon.checkIfNativeAttrPresent(NativeAttribute.AUTHS, attribute);
            
            if (!isProfiles)
                isProfiles = SolarisTestCommon.checkIfNativeAttrPresent(NativeAttribute.PROFILES, attribute);
            
            if (isAuths && isProfiles)
                break;
        }
        Assert.assertTrue(isAuths);
        Assert.assertTrue(isProfiles);
    }
    
    @Test
    public void testGetAllAccounts() {
        Pair<SolarisConnection, CommandBuilder> pair = SolarisTestCommon.getSolarisConn();
        SolarisEntries se = new SolarisEntries(pair.first, pair.second);
        
        final NativeAttribute profilesAttr = NativeAttribute.PROFILES;
        final NativeAttribute rolesAttr = NativeAttribute.ROLES;
        
        Iterator<SolarisEntry> result = se.getAllAccounts(EnumSet.of(profilesAttr, rolesAttr));
        while (result.hasNext()) {
            final SolarisEntry nextIt = result.next();
            final Set<Attribute> attributeSet = nextIt.getAttributeSet();
            
            boolean isProfiles = false;
            boolean isRoles = false;
            for (Attribute attribute : attributeSet) {
                if (!isProfiles)
                    isProfiles = SolarisTestCommon.checkIfNativeAttrPresent(profilesAttr, attribute);
                
                if (!isRoles)
                    isRoles = SolarisTestCommon.checkIfNativeAttrPresent(rolesAttr, attribute);
                
                if (isProfiles && isRoles)
                    break;
            }
            
            final String basicMsg = "Entry: '%s' is missing attribute: '%s'";
            String msg = String.format(basicMsg, nextIt.getName(), profilesAttr);
            Assert.assertTrue(msg, isProfiles);
            
            msg = String.format(basicMsg, nextIt.getName(), rolesAttr);
            Assert.assertTrue(msg, isRoles);
        }//while
    }
}
