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

import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.Test;


public class SolarisConnectorTest extends SolarisTestBase {
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
    
    @Test
    public void testResetPassword() {
        final String username = "bugsBunny";
        final String oldPassword = "bugsPasswd";
        Set<Attribute> attrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), 
                AttributeBuilder.buildPassword(oldPassword.toCharArray()),
                AttributeBuilder.build(AccountAttribute.PASSWD_FORCE_CHANGE.getName(), Boolean.TRUE.toString()));
        // cleanup the user if it's there from previous runs
        try {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        } catch (Exception ex) {
            // OK
        }
        
        getFacade().create(ObjectClass.ACCOUNT, attrs, null);
        try {
            // check if user exists
            String out = getConnection().executeCommand("logins -oxma -l " + username);
            Assert.assertTrue("user " + username + " is missing, buffer: <" + out + ">", out.contains(username));

            // lets change the password for checking expire password.
            final String newPasswd = "changedpwd";
            Set<Attribute> replaceAttributes = CollectionUtil.newSet(
                    AttributeBuilder.buildPassword(newPasswd.toCharArray())
                    );
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), replaceAttributes, null);

            try {
                getFacade().authenticate(ObjectClass.ACCOUNT, username, new GuardedString(newPasswd.toCharArray()), null);
                Assert.fail("expected to wait for 'new password:' prompt failed.");
            } catch (ConnectorException ex) {
                if (!ex.getMessage().contains("New Password:")) {
                    Assert.fail("expected to wait for 'new password:' prompt failed with exception: " + ex.getMessage());
                }
            }
        } finally {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
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
        String out = getConnection().executeCommand("logins -oxma -l \"" + getUsername() + "\"");
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
        
        try {
            getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, "bugsBunny"), AttributeBuilder.buildPassword("foopass".toCharArray()), AttributeBuilder.build(AccountAttribute.UID.getName(), username2)), null);
            Assert.fail("Create of user ID with existing uid should fail - throw an exception.");
        } catch (RuntimeException ex) {
            // OK
            System.out.println(ex.toString());
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
