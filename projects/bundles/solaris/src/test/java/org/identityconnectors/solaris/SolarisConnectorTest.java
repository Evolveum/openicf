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
package org.identityconnectors.solaris;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.Ignore;
import org.junit.Test;


public class SolarisConnectorTest extends SolarisTestBase {
    private static final Log log = Log.getLog(SolarisConnectorTest.class);
    
    @Test
    public void testBasicTest() {
        String username = getUsername();
        // positive test update shell:
        Attribute expectedAttribute = AttributeBuilder.build(AccountAttribute.SHELL.getName(), "/bin/sh");
        Set<Attribute> replaceAttrs = CollectionUtil.newSet(expectedAttribute);
        getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
        Assert.assertTrue(checkUser(username, expectedAttribute));
        
        // negative test update shell:
        expectedAttribute = AttributeBuilder.build(AccountAttribute.SHELL.getName(), "/nonsense/shell");
        replaceAttrs = CollectionUtil.newSet(expectedAttribute);
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
            Assert.fail("Using bad shell value did not fail.");
        } catch (Exception ex) {
            // OK
        }
        
        // negative test: primary group
        expectedAttribute = AttributeBuilder.build(AccountAttribute.GROUP.getName(), "nonsensegroup");
        replaceAttrs = CollectionUtil.newSet(expectedAttribute);
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
            Assert.fail("Changing the primary group to an invalid value did not fail.");
        } catch (Exception ex) {
            // OK
        }
        
        // negative test: secondary group
        if (!getConnection().isNis()) { // not applicable for NIS resources
            replaceAttrs = CollectionUtil.newSet(
                    AttributeBuilder.build(AccountAttribute.GROUP.getName() /* value is null */), 
                    AttributeBuilder.build(AccountAttribute.SECONDARY_GROUP.getName(), "nonsensegroup"));
            try {
                getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
                Assert.fail("Changing the secondary group to an invalid value did not fail.");
            } catch (Exception ex) {
                // OK
            }
        }
        
        // negative test: create
        Set<Attribute> attrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, "donaldduck"), 
                AttributeBuilder.buildPassword("sample_1".toCharArray()),
                AttributeBuilder.build(AccountAttribute.SHELL.getName(), "/invalid/shell"));
        try {
            getFacade().create(ObjectClass.ACCOUNT, attrs, null);
            Assert.fail("did not fail when creating user with invalid data");
        } catch (Exception ex) {
            // OK
        } finally {
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid("sample_1"), null);
            } catch (Exception ex) {
                //OK
            }
        }
    }
    
    /**
     * Error should be thrown when attemting to create a user using an already 
     * existing username. (Usernames are inherently unique on Unix).
     */
    @Test
    public void testDuplicateCreate() {
        Set<Attribute> attrs = CollectionUtil.newSet(
                AttributeBuilder.build(Name.NAME, getUsername()), // duplicate username
                AttributeBuilder.buildPassword("sample_1".toCharArray()));
        try {
            getFacade().create(ObjectClass.ACCOUNT, attrs, null);
            Assert.fail("expected exception on create of user with non-unique username (==uid)");
        } catch (Exception ex) {
            // OK 
        }
    }

    /**
     * check behaviour of reset password attribute. If it is true, the user
     * should change her password on the next login, thus the "new password:"
     * prompt signalizes this fact.
     * 
     * {@see SolarisConnectorTest#testNegativeResetPassword()}, {@see
     * SolarisConnectorTest#testResetPassword2()}.
     */
    @Test
    public void testResetPassword() {
        final String username = createResetPasswordUser(true);
        try {
            // check if user exists
            String loginsCmd = (!getConnection().isNis()) ? "logins -oxma -l " + username : "ypmatch \"" + username + "\" passwd";
            String out = getConnection().executeCommand(loginsCmd);
            Assert.assertTrue("user " + username + " is missing, buffer: <" + out + ">", out.contains(username));

            final String newPasswd = changePasswordForResetPasswordTest(username);

            try {
                getFacade().authenticate(ObjectClass.ACCOUNT, username, new GuardedString(newPasswd.toCharArray()), null);
                Assert.fail("expected to wait for 'new password:' prompt failed.");
            } catch (ConnectorException ex) {
                if (!ex.getMessage().contains("New Password:")) {
                    Assert.fail("expected to wait for 'new password:' prompt failed with exception: " + ex.getMessage());
                } else {
                    log.ok("test testResetPassword passed");
                }
            }
        } finally {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        }
    }
    
    /**
     * Negative testcase.
     * Check for reset password should not happen, when force_change is set to false explicitly.
     * User login should not prompt for New Password: prompt, even if password is changed/reset.
     */
    @Test
    public void testNegativeResetPassword() {
        String username = createResetPasswordUser(false);
        try {
            // check if user exists
            String loginsCmd = (!getConnection().isNis()) ? "logins -oxma -l " + username : "ypmatch \"" + username + "\" passwd";
            String out = getConnection().executeCommand(loginsCmd);
            Assert.assertTrue("user " + username + " is missing, buffer: <" + out + ">", out.contains(username));

            final String newPasswd = changePasswordForResetPasswordTest(username);

            try {
                getFacade().authenticate(ObjectClass.ACCOUNT, username, new GuardedString(newPasswd.toCharArray()), null);
                log.ok("test testNegativeResetPassword passed");
            } catch (ConnectorException ex) {
                if (ex.getMessage().contains("New Password:")) {
                    Assert.fail("expected to login successfully without waiting for new password: prompt.");
                } else {
                    Assert.fail("expected to login successfully, but failed with unexpected exception: " + ex.getMessage());
                }
            }
        } finally {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        }
    }

    private String changePasswordForResetPasswordTest(final String username) {
        // lets change the password for checking expire password.
        final String newPasswd = "changedpwd";
        Set<Attribute> replaceAttributes = CollectionUtil.newSet(
                AttributeBuilder.buildPassword(newPasswd.toCharArray())
                );
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), replaceAttributes, null);
        return newPasswd;
    }

    private String createResetPasswordUser(boolean isForceChange) {
        final String username = "bugsBunny";
        final String oldPassword = "bugsPasswd";
        Set<Attribute> attrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), 
                AttributeBuilder.buildPassword(oldPassword.toCharArray()),
                AttributeBuilder.build(AccountAttribute.PASSWD_FORCE_CHANGE.getName(), Boolean.toString(isForceChange)));
        // cleanup the user if it's there from previous runs
        try {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        } catch (Exception ex) {
            // OK
        }
        
        getFacade().create(ObjectClass.ACCOUNT, attrs, null);
        if (isTrustedExtensions()) {
            String command = "usermod -K min_label=ADMIN_LOW -K clearance=ADMIN_HIGH " + username; 
            getConnection().executeCommand(command);
        }
        return username;
    }
    
    /**
     * Test for false positives during account deletions.
     */
    @Test
    public void testUserDeletion() {
        // special configuration for this test + facade
        SolarisConfiguration config = SolarisTestCommon.createConfiguration();
        // Set up the resource to not make the home directory but attempt to remove it when the
        // user is deleted.
        config.setMakeDirectory(false);
        config.setDeleteHomeDirectory(true);
        ConnectorFacade facade = SolarisTestCommon.createConnectorFacade(config);
        
        String username = "mrbean";
        String password = "snoopy";
        facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), AttributeBuilder.buildPassword(password.toCharArray())), null);
        try {
            String command = (!getConnection().isNis()) ? "logins -oxma -l " + username : "ypmatch " + username + " passwd";
            String out = getConnection().executeCommand(command);
            Assert.assertTrue("user '" + username + "' is missing", out.contains(username));
            
            try {
                facade.delete(ObjectClass.ACCOUNT, new Uid(username), null);
                Assert.fail("deleted the user when deletion should have failed.");
            } catch (Exception ex) {
                // OK
            }
        } finally {
            // we are using the original facade here, because it has a correct configuration.
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        }
    }
    
    /**
     * Test create, update, search, delete operations on {@link ObjectClass#GROUP}
     */
    @Test @Ignore // until it is fully implemented, SRA...
    public void testGroupCRUD() {
        doGroupCRUD(getConfiguration().isSudoAuthorization());
    }
    
    private void doGroupCRUD(boolean isSudoAuthorization) {
        final String groupName = "connGroup";
        // this user belongs to group named after groupName above.
        final String belongingUser = getUsername();
        final String groupNameForUpdate = "connNewGroup";

        try {
            cleanGroup(groupName);
            cleanGroup(groupNameForUpdate);
        } catch (Exception ex) {
            Assert.fail("Failed to clean up preliminary groups.");
        }
        
        final Set<Attribute> groupAttrs = CollectionUtil.newSet(
                AttributeBuilder.build(Name.NAME, groupName), 
                AttributeBuilder.build(GroupAttribute.USERS.getName(),getUsername(), belongingUser)
                );
        getFacade().create(ObjectClass.GROUP, groupAttrs, null);
        
        // creating a duplicate group, exception expected.
        try {
            getFacade().create(ObjectClass.GROUP, groupAttrs, null);
            Assert.fail("Attempt to create a duplicate group should fail.");
        } catch (Exception ex) {
            // OK 
        }
        
        // retrieve the recently created group
        ToListResultsHandler handler = new ToListResultsHandler();
        Set<String> attributesToGet = CollectionUtil.newSet(GroupAttribute.GID.getName());
        if (!getConnection().isNis()) {
            attributesToGet.add(GroupAttribute.USERS.getName());
        }
        getFacade().search(ObjectClass.GROUP, 
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, groupName)), handler, 
                new OperationOptionsBuilder().setAttributesToGet(attributesToGet).build());
        List<ConnectorObject> result = handler.getObjects();
        Assert.assertTrue("failed retrieving group: " + groupName, result.size() > 0);

        ConnectorObject co = result.get(0);
        if (!getConnection().isNis()) {
            Attribute usersAttr = co.getAttributeByName(GroupAttribute.USERS.getName());
            Assert.assertNotNull(usersAttr);
            boolean found = false;
            for (Object it : usersAttr.getValue()) {
                if (it.toString().equals(belongingUser)) {
                    found = true;
                    break;
                }
            }
            String msg = String.format("user '%s' is missing from group '%s'.", belongingUser, groupName);
            Assert.assertTrue(msg, found);
        }
        
        Attribute gidAttr = co.getAttributeByName(GroupAttribute.GID.getName());
        Assert.assertNotNull(gidAttr);
        String gid = AttributeUtil.getStringValue(gidAttr);
        Assert.assertTrue(StringUtil.isNotBlank(gid));
        
        // Create a new group object with a duplicate gid, this one should fail
        try {
            getFacade().create(ObjectClass.GROUP, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, groupNameForUpdate), 
                    AttributeBuilder.build(GroupAttribute.GID.getName(), gid)), null);
            Assert.fail("improperly create a gropu with duplicate id, exception should be thrown.");
        } catch (Exception ex) {
            // OK
        }
        
        // Update the the group that was just created
        // TODO to be continued.
    }

    private void cleanGroup(String groupName) {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.GROUP, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, groupName)), handler, null);
        if (handler.getObjects().size() > 0) {
            getFacade().delete(ObjectClass.GROUP, new Uid(groupName), null);
        }
    }

    /**
     * Error should be thrown when changing the "uid" of the user to // the same
     * value of the another existing user in the resource.
     */
    @Test
    public void testDuplicateUpdate() {
        Set<Attribute> replaceAttrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, getSecondUsername())); // duplicate username
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
            Assert.fail("expected exception on update of user with non-unique username (==uid)");
        } catch (Exception ex) {
            // OK 
        }
    }
    
    

    /**
     * check if the user has the given attribute set to the given value.
     * @param username the accountid to check
     * @param expectedAttribute sources of attribute name/value to check
     * @return true if the attribute is set, false otherwise.
     */
    private boolean checkUser(String username, Attribute expectedAttribute) {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.ACCOUNT, 
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), 
                handler, 
                new OperationOptionsBuilder().setAttributesToGet(expectedAttribute.getName()).build()
                );
        
        List<ConnectorObject> l = handler.getObjects();
        Assert.assertTrue("the requested attribute is missing", l.size() == 1);
        ConnectorObject co = l.get(0);
        Attribute attr = co.getAttributeByName(expectedAttribute.getName());
        return CollectionUtil.equals(attr.getValue(), expectedAttribute.getValue());
    }
    
    /**
     * Update to an existing Uid should throw exception.
     */
    @Test 
    public void testDuplicateUid() {
        // check if both of users exist:
        checkUser(getUsername(), AttributeBuilder.build(Name.NAME, getUsername()));
        checkUser(getSecondUsername(), AttributeBuilder.build(Name.NAME, getSecondUsername()));
        
        // fetch the uid of first user:
        String loginsCmd = (!getConnection().isNis()) ? "logins -oxma -l " + getUsername() : "ypmatch \"" + getUsername() + "\" passwd";
        String out = getConnection().executeCommand(loginsCmd);
        String firstUid = out.split(":")[1];
        
        // update second users' uid to the first:
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getSecondUsername()),
                    CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.UID.getName(), firstUid)), null);
            Assert.fail("Update of 'solaris uid' attribute the with an existing uid should throw RuntimeException.");
        } catch (RuntimeException ex) {
            // OK
        }
    }
    
    /**
     * Password shouldn't contain control sequence characters 
     */
    @Test
    public void testControlCharPassword() {
        List<Character> controlChars = getControlChars();
        
        // basic test of connection, if it filters invalid password
        for (Character controlCharacter : controlChars) {
            try {
                String passwd = new StringBuilder().append("foo").append(controlCharacter).append("bar").toString();
                SolarisConnection.sendPassword(new GuardedString(passwd.toCharArray()), getConnection());
                Assert.fail("Exception should be thrown when attempt to send control chars within the password");
            } catch (RuntimeException ex) {
                // OK
            }
        }
    }
    
    /** analogical to {@link SolarisConnectorTest#testControlCharPassword()}, but for create. */
    @Test
    public void testCreateControlCharPasswd() {
        String dummyUser = "bugsbunny";
        try {
            String controlChar = "\r";
            String password = new StringBuilder().append("foo").append(controlChar).append("bar").toString();
            getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, dummyUser), AttributeBuilder.buildPassword(password.toCharArray())), null);
            Assert.fail("Exception should be thrown when password containing control char sent.");
        } catch (RuntimeException ex) {
            // OK
        } finally {
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid(dummyUser), null);
            } catch (Exception ex) {
                // OK
            }
        }
    }
    
    /**analogical to {@link SolarisConnectorTest#testControlCharPassword()}, but for update. */
    @Test
    public void testUpdateControlCharPasswd() {
        String controlChar = "\n";
        String password = new StringBuilder().append("foo").append(controlChar).append("bar").toString();
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), CollectionUtil.newSet(AttributeBuilder.buildPassword(password.toCharArray())), null);
            Assert.fail("Exception should be thrown when password containing control char sent.");
        } catch (RuntimeException ex) {
            // OK
        }
    }
    
    @Test
    public void testCreateUidWithNonUniqueValue() {
        final String username2 = getSecondUsername();
        final String dummyUser = "bugsBunny";
        
        try {
            getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, dummyUser), AttributeBuilder.buildPassword("foopass".toCharArray()), AttributeBuilder.build(AccountAttribute.UID.getName(), username2)), null);
            Assert.fail("Create of user ID with existing uid should fail - throw an exception.");
        } catch (RuntimeException ex) {
            // OK
            System.out.println(ex.toString());
        } finally {
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid(dummyUser), null);
            } catch (Exception ex) {
                // OK
            }
        }
    }

    private List<Character> getControlChars() {
        List<Character> controlChars = CollectionUtil.newList();
        for (char i = 0; i <= 0x001F; i++)
            controlChars.add(i);
        for (char i = 0x007F; i <= 0x009F; i++)
            controlChars.add(i);
        return controlChars;
    }

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
        Assert.assertTrue("test preconditions not satisfied", checkOperatorProfiles.contains(profileToUpdate));

        // update profile
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.PROFILE.getName(), profileToUpdate)), null);
        String profilesOut = getConnection().executeCommand(getConnection().buildCommand("profiles", username));
        Assert.assertTrue("user has not been updated to match the profiles of '" + profileToUpdate + "' config role.", profilesOut.contains(profileToUpdate));
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
        Assert.assertTrue(profilesOut.contains(operatorRole));
        Set<String> profilesBefore = parseProfiles(profilesOut, username);
        
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.PROFILE.getName(), profileToUpdate)), null);
        
        profilesOut = getConnection().executeCommand(getConnection().buildCommand("profiles", username));
        Set<String> profilesAfter = parseProfiles(profilesOut, username);
        Assert.assertTrue(profilesAfter.size() < profilesBefore.size());
        Assert.assertTrue(!profilesAfter.contains(operatorRole));
    }

    private Set<String> parseProfiles(String profilesOut, String skipString) {
        String[] lines = profilesOut.split("\n");
        Set<String> result = CollectionUtil.newSet();
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
        Assert.assertTrue(rolesOut.contains("No roles"));
        
        // create a fictive role
        final String fictiveRole = "solarisconnectorrole";
        getConnection().executeCommand("roleadd " + fictiveRole);
        try {
            // set the 'fictiveRole' for the user
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.ROLES.getName(), fictiveRole)), null);
            rolesOut = getConnection().executeCommand("roles " + username);
            Assert.assertTrue(rolesOut.contains(fictiveRole));
            
            // erase all roles for the user
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.ROLES.getName(), "")), null);
            rolesOut = getConnection().executeCommand("roles " + username);
            Assert.assertTrue(rolesOut.contains("No roles"));
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
            Assert.assertTrue(msg, !auth.contains(newAuthorization));
        }
        
        // add a new authorization
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.AUTHORIZATION.getName(), newAuthorization)), null);
        authsOut = getConnection().executeCommand("auths " + username);
        authorizations = Arrays.asList(authsOut.split(","));
        Assert.assertTrue(authorizations.contains(newAuthorization));        
        
        // remove new authorization
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.AUTHORIZATION.getName(), "")), null);
        authsOut = getConnection().executeCommand("auths " + username);
        authorizations = Arrays.asList(authsOut.split(","));
        Assert.assertFalse(authorizations.contains(newAuthorization));
    }

    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 2;
    }
    
    private String getUsername() {
        return getUsername(0);
    }
    
    private String getSecondUsername() {
        return getUsername(1);
    }
}
