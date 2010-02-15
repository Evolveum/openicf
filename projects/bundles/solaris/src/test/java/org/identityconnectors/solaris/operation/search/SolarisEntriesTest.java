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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.junit.Assert;
import org.junit.Test;

public class SolarisEntriesTest extends SolarisTestBase {
    
    @Test
    public void testGetAccount() {
        final String userName = "root";
        SolarisEntry result = SolarisEntries.getAccount(userName, EnumSet.of(NativeAttribute.AUTHS, NativeAttribute.PROFILES, NativeAttribute.NAME), getConnection());
        Assert.assertTrue(result.getName().equals(userName));
        Set<Attribute> set = result.getAttributeSet();
        Assert.assertNotNull(set);
        
        boolean isAuths = false;
        boolean isProfiles = false;
        for (Attribute attribute : set) {
            if (!isAuths)
                isAuths = NativeAttribute.AUTHS.getName().equals(attribute.getName());
            
            if (!isProfiles)
                isProfiles = NativeAttribute.PROFILES.getName().equals(attribute.getName());
            
            if (isAuths && isProfiles)
                break;
        }
        if (!getConnection().isNis()) {
            // NIS doesn't support auths and profiles command TODO
            Assert.assertTrue(isAuths);
            Assert.assertTrue(isProfiles);
        }
    }
    
    @Test
    public void testGetAllAccounts() {
        final NativeAttribute profilesAttr = NativeAttribute.PROFILES;
        final NativeAttribute rolesAttr = NativeAttribute.ROLES;
        
        Iterator<SolarisEntry> result = SolarisEntries.getAllAccounts(EnumSet.of(profilesAttr, rolesAttr), getConnection());
        while (result.hasNext()) {
            final SolarisEntry nextIt = result.next();
            final Set<Attribute> attributeSet = nextIt.getAttributeSet();
            
            boolean isProfiles = false;
            boolean isRoles = false;
            for (Attribute attribute : attributeSet) {
                if (!isProfiles)
                    isProfiles = profilesAttr.getName().equals(attribute.getName());
                
                if (!isRoles)
                    isRoles = rolesAttr.getName().equals(attribute.getName());
                
                if (isProfiles && isRoles)
                    break;
            }
            
            final String basicMsg = "Entry: '%s' is missing attribute: '%s'";
            String msg = String.format(basicMsg, nextIt.getName(), profilesAttr);
            if (!getConnection().isNis()) { // NIS doesn't support profiles command TODO
                Assert.assertTrue(msg, isProfiles);
            }
            
            msg = String.format(basicMsg, nextIt.getName(), rolesAttr);
            if (!getConnection().isNis()) { // NIS doesn't support roles command TODO
                Assert.assertTrue(msg, isRoles);
            }
        }//while
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
