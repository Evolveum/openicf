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
package org.identityconnectors.solaris.attr;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.test.SolarisTestBase;

/**
 * Unit tests for attributes of Role-based access control (RBAC).
 * 
 * These attributes are: {@link AccountAttribute#ROLES},
 * {@link AccountAttribute#PROFILE}, {@link AccountAttribute#AUTHORIZATION}.
 * 
 * @author David Adam
 */
public class RBACAttributeTests extends SolarisTestBase {
    
    private static final Log log = Log.getLog(RBACAttributeTests.class);
    
    /**
     * test preconditions: tests assume that Operator is allowed to use 'Printer
     * Management' and 'Media Backup' profiles.
     */
    @Test
    public void testUpdateProfiles() {
        if (getConnection().isNis()) {
            log.info("skipping test for NIS configuration");
            return;
        }
        
        String username = getUsername();
        String profileToUpdate = "Operator";
         
        // check preconditions
        String checkOperatorProfiles = getConnection().executeCommand("cat /etc/security/prof_attr | grep ^" + profileToUpdate + ":");
        AssertJUnit.assertTrue("test preconditions not satisfied", checkOperatorProfiles.contains(profileToUpdate));

        // update profile
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.PROFILE.getName(), profileToUpdate)), null);
        String profilesOut = getConnection().executeCommand(getConnection().buildCommand("profiles", username));
        AssertJUnit.assertTrue("user has not been updated to match the profiles of '" + profileToUpdate + "' config role.", profilesOut.contains(profileToUpdate));
    }
    
    /**
     * an empty parameter passed to Profiles should clean up all except basic profiles.
     */
    @Test
    public void testUpdateProfilesEmpty() {
        if (getConnection().isNis()) {
            log.info("skipping test for NIS configuration");
            return;
        }
        
        String username = getUsername();
        String profileToUpdate = "";

        // add the Operator profile, so we have at least one item to delete.
        String operatorRole = "Operator";
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.PROFILE.getName(), operatorRole)), null);
        String profilesOut = getConnection().executeCommand(getConnection().buildCommand("profiles", username));
        AssertJUnit.assertTrue(profilesOut.contains(operatorRole));
        Set<String> profilesBefore = parseProfiles(profilesOut, username);
        
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.PROFILE.getName(), profileToUpdate)), null);
        
        profilesOut = getConnection().executeCommand(getConnection().buildCommand("profiles", username));
        Set<String> profilesAfter = parseProfiles(profilesOut, username);
        AssertJUnit.assertTrue(profilesAfter.size() < profilesBefore.size());
        AssertJUnit.assertTrue(!profilesAfter.contains(operatorRole));
    }

    private Set<String> parseProfiles(String profilesOut, String skipString) {
        String[] lines = profilesOut.split("\n");
        Set<String> result = CollectionUtil.<String>newSet();
        for (String line : lines) {
            if (line.contains(skipString))
                continue;
            
            result.add(line.trim());
        }
        return result;
    }
    
    @Test
    public void testRoles() {
        if (getConnection().isNis()) {
            log.info("skipping test for NIS configuration");
            return;
        }
        
        String username = getUsername();
        String rolesOut = getConnection().executeCommand("roles " + username);
        AssertJUnit.assertTrue(rolesOut.contains("No roles"));
        
        // create a fictive role
        final String fictiveRole = "solarisconnectorrole";
        getConnection().executeCommand("roleadd " + fictiveRole);
        try {
            // set the 'fictiveRole' for the user
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.ROLES.getName(), fictiveRole)), null);
            rolesOut = getConnection().executeCommand("roles " + username);
            AssertJUnit.assertTrue(rolesOut.contains(fictiveRole));
            
            // erase all roles for the user
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.ROLES.getName(), "")), null);
            rolesOut = getConnection().executeCommand("roles " + username);
            AssertJUnit.assertTrue(rolesOut.contains("No roles"));
        } finally {
            //delete the fictive role
            getConnection().executeCommand("roledel " + fictiveRole);
        }
    }
    
    @Test
    public void testAuths() {
        if (getConnection().isNis()) {
            log.info("skipping test for NIS configuration");
            return;
        }
        
        String username = getUsername();
        final String newAuthorization = "solaris.admin.printer.delete";
        // control preconditions
        String authsOut = getConnection().executeCommand("auths " + username);
        List<String> authorizations = Arrays.asList(authsOut.split(","));
        String msg = String.format("Preconditions were not met. By default users shouldn't have '%s' authorization.", newAuthorization);
        for (String auth : authorizations) {
            AssertJUnit.assertTrue(msg, !auth.contains(newAuthorization));
        }
        
        // add a new authorization
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.AUTHORIZATION.getName(), newAuthorization)), null);
        authsOut = getConnection().executeCommand("auths " + username);
        authorizations = Arrays.asList(authsOut.split(","));
        AssertJUnit.assertTrue(authorizations.contains(newAuthorization));        
        
        // remove new authorization
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.AUTHORIZATION.getName(), "")), null);
        authsOut = getConnection().executeCommand("auths " + username);
        authorizations = Arrays.asList(authsOut.split(","));
        AssertJUnit.assertFalse(authorizations.contains(newAuthorization));
    }

    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 1;
    }
    
    private String getUsername() {
        return getUsername(0);
    }
}
