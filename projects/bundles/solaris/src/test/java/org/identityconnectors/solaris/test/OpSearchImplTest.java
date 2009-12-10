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
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.operation.search.OpSearchImpl;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.Test;

public class OpSearchImplTest extends SolarisTestBase {
    private static final int NR_OF_USERS = 3;

    /**
     * no exception should be thrown. Searching for all account on the resource
     * -- by null filter.
     */
    @Test
    public void testSearchNullFilter() {

        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.ACCOUNT, /* filter */null, handler, null);

        Set<String> expectedNames = new HashSet<String>();
        for (int i = 0; i < NR_OF_USERS; i++) {
            expectedNames.add(formatName(i));
        }

        for (ConnectorObject connectorObject : handler.getObjects()) {
            expectedNames.remove(connectorObject.getName().getNameValue());
        }

        Assert.assertTrue(String.format("Searched failed to find the following users: %s", expectedNames), expectedNames.isEmpty());
    }

    /**
     * Searching using filter
     */
    @Test
    public void testSearchWithFilter() {
        String username = formatName(0);

        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.ACCOUNT, 
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), 
                handler, null);
        
        final List<ConnectorObject> l = handler.getObjects();
        String msg = String.format("Size of results is less than expected: %s", l.size());
        Assert.assertTrue(msg, l.size() == 1);

        final String returnedUsername = l.get(0).getName().getNameValue();
        msg = String.format("The returned username '%s', differs from the expected '%s'", returnedUsername, username);
        Assert.assertTrue(msg, returnedUsername.equals(username));
    }

    @Test
    public void testGetShells() {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(OpSearchImpl.SHELL, null, handler, null);
        List<ConnectorObject> result = handler.getObjects();
        Assert.assertNotNull(result);

        if (result.size() > 0) {
            for (ConnectorObject connectorObject : result) {
                String value = connectorObject.getName().getNameValue();
                Assert.assertTrue(value.trim().length() > 0);
            }
        }
    }

    @Test
    public void testGetGroups() {
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.GROUP, null, handler, null);
        List<ConnectorObject> result = handler.getObjects();
        Assert.assertNotNull(result);

        if (result.size() > 0) {
            for (ConnectorObject connectorObject : result) {
                String value = connectorObject.getName().getNameValue();
                Assert.assertTrue(value.trim().length() > 0);
            }
        }
    }

    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return NR_OF_USERS;
    }
}
