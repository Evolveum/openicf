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
package org.identityconnectors.solaris.test;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.attr.GroupAttribute;

public class SolarisCreateTest extends SolarisTestBase {
    /**
     * creates a sample user
     */
    @Test
    public void testCreate() {
        // create a new user
        final String username = "spams";
        final GuardedString password = new GuardedString("samplePassword".toCharArray());
        final Set<Attribute> attrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), AttributeBuilder.buildPassword(password));

        try {
            Uid uid = null;
            try {
                uid = getFacade().create(ObjectClass.ACCOUNT, attrs, null);
            } catch (RuntimeException ex) {
                AssertJUnit.fail(String.format("Create failed for: '%s'\n ExceptionMessage: %s", username, ex.getMessage()));
            }
            AssertJUnit.assertNotNull(uid);

            // search for the created account
            String command = (!getConnection().isNis()) ? "cut -d: -f1 /etc/passwd | grep '" + username + "'" : "ypcat passwd | cut -d: -f1 | grep '" + username + "'";
            String out = getConnection().executeCommand(command);
            AssertJUnit.assertTrue(String.format("user '%s' not found on the resource.", username), out.contains(username));
            
        } finally {
            // cleanup the new user
            if (!getConnection().isNis()) {
                getConnection().executeCommand("userdel " + username);
            } else {
                try {
                    // NIS delete is just way too big to inline it here:
                    getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
                } catch (RuntimeException ex) {
                    // OK
                }
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unknownObjectClass() {
        final Set<Attribute> attrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, "foo"));
        getFacade().create(new ObjectClass("NONEXISTING_OBJECTCLASS"), attrs, null);
    }

    @Test(expectedExceptions = ConnectorException.class)
    public void createExistingAccount() {
        final Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.build(Name.NAME, "root"));
        attrs.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString("".toCharArray())));
        getFacade().create(ObjectClass.ACCOUNT, attrs, null);
    }

    @Test(expectedExceptions = ConnectorException.class)
    public void createExistingGroup() {
        final Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.build(Name.NAME, "root"));
        getFacade().create(ObjectClass.GROUP, attrs, null);
    }

    @Test
    public void createGroupTest() {
        Set<Attribute> attrs = new HashSet<Attribute>();
        final String groupName = "testgrp";
        attrs.add(AttributeBuilder.build(Name.NAME, groupName));
        attrs.add(AttributeBuilder.build(GroupAttribute.USERS.getName(), CollectionUtil.newList("root")));

        // create a group
        getFacade().create(ObjectClass.GROUP, attrs, null);
        try {
            // verify if group exists (throws ConnectorException in case of
            // missing group)
            String command = (!getConnection().isNis()) ? "cat /etc/group | grep '" + groupName + "'" : "ypcat group | grep '" + groupName + "'";
            String out = getConnection().executeCommand(command);
            AssertJUnit.assertTrue(out.contains(groupName));
            AssertJUnit.assertTrue(out.contains("root"));
        } finally {
            // cleanup the created group
            if (!getConnection().isNis()) {
                getConnection().executeCommand("groupdel '" + groupName + "'");
            } else {
                // NIS delete is just way too big to inline it here:
                try {
                    getFacade().delete(ObjectClass.GROUP, new Uid(groupName), null);
                } catch (RuntimeException ex) {
                    // OK
                }
            }
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
