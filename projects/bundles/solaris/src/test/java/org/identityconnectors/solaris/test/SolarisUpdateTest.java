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

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.junit.Test;

public class SolarisUpdateTest extends SolarisTestBase {

    /**
     * create a new user and try to change its password, and later try to
     * authenticate
     */
    @Test
    public void testUpdate() {
        final String username = getUserName();
        if (isTrustedExtensions()) {
            String command = "usermod -K min_label=ADMIN_LOW -K clearance=ADMIN_HIGH " + username; 
            getConnection().executeCommand(command);
        }

        Set<Attribute> replaceAttributes = new HashSet<Attribute>();
        final GuardedString newPassword = new GuardedString("buzz".toCharArray());
        Attribute chngPasswdAttribute = AttributeBuilder.buildPassword(newPassword);
        replaceAttributes.add(chngPasswdAttribute);
        // 1) PERFORM THE UPDATE OF PASSWORD
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), replaceAttributes, null);

        // 2) try to authenticate with new password
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, username, newPassword, null);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            Assert.fail(String.format("Authenticate failed for user with changed password: '%s'\n ExceptionMessage: %s", username, ex.getMessage()));
        }
    }

    @Test(expected = RuntimeException.class)
    public void unknownObjectClass() {
        String username = getConfiguration().getRootUser();
        Set<Attribute> replaceAttributes = CollectionUtil.newSet(AttributeBuilder.buildPassword("buzz".toCharArray()));
        getFacade().update(new ObjectClass("NONEXISTING_OBJECTCLASS"), new Uid(username), replaceAttributes, null);
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateUnknownUid() {
        Set<Attribute> replaceAttributes = CollectionUtil.newSet(AttributeBuilder.buildPassword("buzz".toCharArray()));

        getFacade().update(ObjectClass.ACCOUNT, new Uid("NONEXISTING_UID___"), replaceAttributes, null);
    }

    @Test
    public void testUpdateGroup() {
        final String username = getUserName();
        final String groupName = getGroupName();

        // verify if group exists
        final String command = (!getConnection().isNis()) ? "cat /etc/group | grep '" + groupName + "'" : "ypcat group | grep '" + groupName + "'";
        String output = getConnection().executeCommand(command);
        Assert.assertTrue(output.contains(groupName));

        Set<Attribute> replaceAttributes = CollectionUtil.newSet(AttributeBuilder.build(GroupAttribute.USERS.getName(), CollectionUtil.newList("root", username)));
        getFacade().update(ObjectClass.GROUP, new Uid(groupName), replaceAttributes, null);
        output = getConnection().executeCommand(command);
        String msg = "Output is missing attribute '%s', buffer: <%s>";
        Assert.assertTrue(String.format(msg, groupName, output), output.contains(groupName));
        Assert.assertTrue(String.format(msg, username, output), output.contains(username));
        Assert.assertTrue(String.format(msg, "root", output), output.contains("root"));
    }

    /*    ************* AUXILIARY METHODS *********** */

    @Override
    public boolean createGroup() {
        return true;
    }

    @Override
    public int getCreateUsersNumber() {
        return 1;
    }

    private String getUserName() {
        return formatName(0);
    }
}
