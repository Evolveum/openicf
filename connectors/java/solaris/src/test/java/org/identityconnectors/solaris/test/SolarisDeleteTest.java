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
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class SolarisDeleteTest extends SolarisTestBase {
    @Test
    public void testDeleteUnknownUid() {
        try {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid("nonExistingUid"), null);
            AssertJUnit.fail("no exception was thrown when attempt to create a nonexisting account");
        } catch (ConnectorException ex) {
            // OK
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void unknownObjectClass() {
        getFacade().delete(new ObjectClass("nonExistingObjectclass"), new Uid(getUserName()), null);
    }

    @Test(expectedExceptions = ConnectorException.class)
    public void testDeleteUnknownGroup() {
        getFacade().delete(ObjectClass.GROUP, new Uid("nonExistingGroup"), null);
    }
    
    @Test
    public void testDeleteAccount() {
        getFacade().delete(ObjectClass.ACCOUNT, new Uid(getUserName()), null);
        //search for the account
        String command = (!getConnection().isNis()) ? "cut -d: -f1 /etc/passwd | grep '" + getUserName() + "'" : "ypcat passwd | cut -d: -f1 | grep '" + getUserName() + "'";
        String out = getConnection().executeCommand(command);
        AssertJUnit.assertTrue(!out.contains(getUserName()));
    }

    @Test
    public void testDeleteGroup() {
        // verify if it exists
        checkGroup(getGroupName(), false);

        // perform delete
        getFacade().delete(ObjectClass.GROUP, new Uid(getGroupName()), null);
        checkGroup(getGroupName(), true);
    }

    /**
     * @param groupName
     *            the group name to search
     * @param isDeleted
     *            if it is true, than we expect the group to be deleted, otherwise we expect the group to be present on the resource.
     */
    private void checkGroup(String groupName, boolean isDeleted) {
        String command = (!getConnection().isNis()) ? "cat /etc/group | grep '" + groupName + "'" : "ypcat group | cut -d: -f1 | grep '" + groupName + "'";
        String out = getConnection().executeCommand(command);
        String msg = String.format((isDeleted) ? "group '%s' was not properly deleted from the resource, it is still there" : "group '%s' is missing from the resource, create failed." , groupName);
        AssertJUnit.assertTrue(msg, (isDeleted) ? !out.contains(groupName) : out.contains(groupName));
    }

    @Override
    public boolean createGroup() {
        return true;
    }

    @Override
    public int getCreateUsersNumber() {
        // we are using 1 automatically created user
        return 1;
    }

    private String getUserName() {
        return getUsername(0);
    }
}
