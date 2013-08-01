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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
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
import org.testng.Assert;
import org.testng.annotations.Test;

public class SolarisConnectorTest extends SolarisTestBase {
    private static final Log logger = Log.getLog(SolarisConnectorTest.class);

    @Test
    public void testBasicTest() {
        String username = getUsername();
        // positive test update shell:
        Attribute expectedAttribute =
                AttributeBuilder.build(AccountAttribute.SHELL.getName(), "/bin/sh");
        Set<Attribute> replaceAttrs = CollectionUtil.newSet(expectedAttribute);
        getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
        assertTrue(checkUser(username, expectedAttribute));

        // negative test update shell:
        expectedAttribute =
                AttributeBuilder.build(AccountAttribute.SHELL.getName(), "/nonsense/shell");
        replaceAttrs = CollectionUtil.newSet(expectedAttribute);
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
            fail("Using bad shell value did not fail.");
        } catch (Exception ex) {
            // OK
        }

        // negative test: primary group
        expectedAttribute =
                AttributeBuilder.build(AccountAttribute.GROUP.getName(), "nonsensegroup");
        replaceAttrs = CollectionUtil.newSet(expectedAttribute);
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
            fail("Changing the primary group to an invalid value did not fail.");
        } catch (Exception ex) {
            // OK
        }

        // negative test: secondary group
        if (!getConnection().isNis()) { // not applicable for NIS resources
            replaceAttrs =
                    CollectionUtil.newSet(AttributeBuilder
                            .build(AccountAttribute.GROUP.getName() /*
                                                                     * value is
                                                                     * null
                                                                     */), AttributeBuilder.build(
                            AccountAttribute.SECONDARY_GROUP.getName(), "nonsensegroup"));
            try {
                getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
                fail("Changing the secondary group to an invalid value did not fail.");
            } catch (Exception ex) {
                // OK
            }
        }

        // negative test: create
        Set<Attribute> attrs =
                CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, "donaldduck"),
                        AttributeBuilder.buildPassword("sample_1".toCharArray()), AttributeBuilder
                                .build(AccountAttribute.SHELL.getName(), "/invalid/shell"));
        try {
            getFacade().create(ObjectClass.ACCOUNT, attrs, null);
            fail("did not fail when creating user with invalid data");
        } catch (Exception ex) {
            // OK
        } finally {
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid("sample_1"), null);
            } catch (Exception ex) {
                // OK
            }
        }
    }

    /**
     * Error should be thrown when attemting to create a user using an already
     * existing username. (Usernames are inherently unique on Unix).
     */
    @Test
    public void testDuplicateCreate() {
        Set<Attribute> attrs =
                CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, getUsername()), // duplicate
                                                                                        // username
                        AttributeBuilder.buildPassword("sample_1".toCharArray()));
        try {
            getFacade().create(ObjectClass.ACCOUNT, attrs, null);
            fail("expected exception on create of user with non-unique username (==uid)");
        } catch (Exception ex) {
            // OK
        }
    }

    /**
     * Test for false positives during account deletions.
     */
    @Test
    public void testUserDeletion() {
        if (getConnection().isNis()) {
            // Workaround: skipping. TODO Solaris NIS scripts doesn't throw an
            // error during this test.
            logger.info("skipping test 'testUserDeletion' for Solaris NIS configuration.");
            return;
        }

        // special configuration for this test + facade
        SolarisConfiguration config = SolarisTestCommon.createConfiguration();
        // Set up the resource to not make the home directory but attempt to
        // remove it when the
        // user is deleted.
        config.setMakeDirectory(false);
        config.setDeleteHomeDirectory(true);
        ConnectorFacade facade = SolarisTestCommon.createConnectorFacade(config);

        String username = "mrbean";
        String password = "sno0py";
        facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME,
                username), AttributeBuilder.buildPassword(password.toCharArray())), null);
        try {
            String command =
                    (!getConnection().isNis()) ? "logins -oxma -l " + username : "ypmatch "
                            + username + " passwd";
            String out = getConnection().executeCommand(command);
            assertTrue(out.contains(username), "user '" + username + "' is missing");

            try {
                facade.delete(ObjectClass.ACCOUNT, new Uid(username), null);
                fail("deleted the user when deletion should have failed.");
            } catch (Exception ex) {
                // OK
            }
        } finally {
            // we are using the original facade here, because it has a correct
            // configuration.
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
            } catch (Exception ex) {
                // OK
            }
        }
    }

    /**
     * Test create, update, search, delete operations on
     * {@link ObjectClass#GROUP}
     */
    @Test
    public void testGroupCRUD() {
        doGroupCRUD(getConfiguration().isSudoAuthorization());
    }

    private void doGroupCRUD(boolean isSudoAuthorization) {
        final String groupName = "conngroup";
        // this user belongs to group named after groupName above.
        final String belongingUser = getUsername();
        final String groupNameForUpdate = "connnewgroup";

        try {
            cleanGroup(groupName);
            cleanGroup(groupNameForUpdate);
        } catch (Exception ex) {
            fail("Failed to clean up preliminary groups.");
        }

        final Set<Attribute> groupAttrs =
                CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, groupName),
                        AttributeBuilder.build(GroupAttribute.USERS.getName(), getUsername(),
                                belongingUser));
        getFacade().create(ObjectClass.GROUP, groupAttrs, null);

        // creating a duplicate group, exception expected.
        try {
            getFacade().create(ObjectClass.GROUP, groupAttrs, null);
            fail("Attempt to create a duplicate group should fail.");
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
        assertTrue(result.size() > 0, "failed retrieving group: " + groupName);

        ConnectorObject co = result.get(0);
        if (!getConnection().isNis()) {
            Attribute usersAttr = co.getAttributeByName(GroupAttribute.USERS.getName());
            assertNotNull(usersAttr);
            boolean found = false;
            for (Object it : usersAttr.getValue()) {
                if (it.toString().equals(belongingUser)) {
                    found = true;
                    break;
                }
            }
            String msg =
                    String.format("user '%s' is missing from group '%s'.", belongingUser, groupName);
            assertTrue(found, msg);
        }

        Attribute gidAttr = co.getAttributeByName(GroupAttribute.GID.getName());
        assertNotNull(gidAttr);
        Integer gid = AttributeUtil.getIntegerValue(gidAttr);
        Assert.assertNotNull(gid);

        // Create a new group object with a duplicate gid, this one should fail
        try {
            getFacade().create(
                    ObjectClass.GROUP,
                    CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, groupNameForUpdate),
                            AttributeBuilder.build(GroupAttribute.GID.getName(), gid)), null);
            fail("improperly create a gropu with duplicate id, exception should be thrown.");
        } catch (Exception ex) {
            // OK
        }

        // Update the the group that was just created
        // TODO to be continued.
    }

    private void cleanGroup(String groupName) {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.GROUP,
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, groupName)), handler, null);
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
        Set<Attribute> replaceAttrs =
                CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, getSecondUsername())); // duplicate
                                                                                               // username
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), replaceAttrs, null);
            fail("expected exception on update of user with non-unique username (==uid)");
        } catch (Exception ex) {
            // OK
        }
    }

    /**
     * check if the user has the given attribute set to the given value.
     *
     * @param username
     *            the accountid to check
     * @param expectedAttribute
     *            sources of attribute name/value to check
     * @return true if the attribute is set, false otherwise.
     */
    private boolean checkUser(String username, Attribute expectedAttribute) {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(
                ObjectClass.ACCOUNT,
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)),
                handler,
                new OperationOptionsBuilder().setAttributesToGet(expectedAttribute.getName())
                        .build());

        List<ConnectorObject> l = handler.getObjects();
        assertTrue(l.size() == 1, "the requested attribute is missing");
        ConnectorObject co = l.get(0);
        Attribute attr = co.getAttributeByName(expectedAttribute.getName());
        // workaround for shell attribute, as sometimes /bin/sh is a simlink to
        // /sbin/sh,
        // and the system treats it as the same shell. So we should compare only
        // stuff after
        // the last slash.
        if (attr.getName().equals(AccountAttribute.SHELL.getName())) {
            String expectedShell = AttributeUtil.getStringValue(expectedAttribute);
            String actualShell = AttributeUtil.getStringValue(attr);
            int i = expectedShell.lastIndexOf("/");
            if (i != -1) {
                expectedShell = expectedShell.substring(i + 1);
            }
            i = actualShell.lastIndexOf("/");
            if (i != -1) {
                actualShell = actualShell.substring(i + 1);
            }
            expectedShell = expectedShell.trim();
            actualShell = actualShell.trim();
            return expectedShell.equals(actualShell);
        }
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
        String loginsCmd =
                (!getConnection().isNis()) ? "logins -oxma -l " + getUsername() : "ypmatch \""
                        + getUsername() + "\" passwd";
        String out = getConnection().executeCommand(loginsCmd);
        String firstUid = out.split(":")[1];

        // update second users' uid to the first:
        try {
            getFacade().update(
                    ObjectClass.ACCOUNT,
                    new Uid(getSecondUsername()),
                    CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.UID.getName(),
                            firstUid)), null);
            fail("Update of 'solaris uid' attribute the with an existing uid should throw RuntimeException.");
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
                String passwd =
                        new StringBuilder().append("foo").append(controlCharacter).append("bar")
                                .toString();
                getConnection().sendPassword(new GuardedString(passwd.toCharArray()));
                fail("Exception should be thrown when attempt to send control chars within the password");
            } catch (RuntimeException ex) {
                // OK
            }
        }
    }

    /**
     * analogical to {@link SolarisConnectorTest#testControlCharPassword()}, but
     * for create.
     */
    @Test
    public void testCreateControlCharPasswd() {
        String dummyUser = "bugsbunny";
        try {
            String controlChar = "\r";
            String password =
                    new StringBuilder().append("foo").append(controlChar).append("bar").toString();
            getFacade().create(
                    ObjectClass.ACCOUNT,
                    CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, dummyUser),
                            AttributeBuilder.buildPassword(password.toCharArray())), null);
            fail("Exception should be thrown when password containing control char sent.");
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

    /**
     * analogical to {@link SolarisConnectorTest#testControlCharPassword()}, but
     * for update.
     */
    @Test
    public void testUpdateControlCharPasswd() {
        String controlChar = "\n";
        String password =
                new StringBuilder().append("fo0").append(controlChar).append("bar").toString();
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()),
                    CollectionUtil.newSet(AttributeBuilder.buildPassword(password.toCharArray())),
                    null);
            fail("Exception should be thrown when password containing control char sent.");
        } catch (RuntimeException ex) {
            // OK
        }
    }

    @Test
    public void testCreateUidWithNonUniqueValue() {
        final String username2 = getSecondUsername();
        final String dummyUser = "porkyPig";

        try {
            getFacade().create(
                    ObjectClass.ACCOUNT,
                    CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, dummyUser),
                            AttributeBuilder.buildPassword("foopass".toCharArray()),
                            AttributeBuilder.build(AccountAttribute.UID.getName(), username2)),
                    null);
            fail("Create of user ID with existing uid should fail - throw an exception.");
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
        List<Character> controlChars = CollectionUtil.<Character> newList();
        for (char i = 0; i <= 0x001F; i++) {
            controlChars.add(i);
        }
        for (char i = 0x007F; i <= 0x009F; i++) {
            controlChars.add(i);
        }
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
