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
package org.identityconnectors.solaris.operation;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.identityconnectors.test.common.ToListResultsHandler;


public class RenameGroupTest extends SolarisTestBase {
    @Test
    public void testRenameGroup() {
        String groupName = getGroupName();
        
        AssertJUnit.assertTrue(String.format("group '%s' is missing.", groupName), searchForGroup(groupName));
        
        String newName = "newconngrp";
        RenameGroup.renameGroup(new SolarisEntry.Builder(groupName).addAttr(NativeAttribute.NAME, newName).build(), getConnection());
        try {
            AssertJUnit.assertTrue(String.format("group '%s' is missing.", newName), searchForGroup(newName));
        } finally {
            getFacade().delete(ObjectClass.GROUP, new Uid(newName), null);
        }
    }

    private boolean searchForGroup(String groupName) {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.GROUP, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, groupName)), handler, null);
        return handler.getObjects().size() > 0;
    }

    @Override
    public boolean createGroup() {
        return true;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
}
