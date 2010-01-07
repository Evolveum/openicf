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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.operation.search.SolarisSearch;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.Test;

public class SolarisSearchTest extends SolarisTestBase {
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
            expectedNames.add(getUsername(i));
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
        String username = getUsername(0);

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
        getFacade().search(SolarisSearch.SHELL, null, handler, null);
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
    
    /**
     * this test requires a previously created account
     */
    @Test
    public void testFetchUid() {
        String username = getUsername(0);
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.ACCOUNT, 
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler, 
                new OperationOptionsBuilder().setAttributesToGet(AccountAttribute.UID.getName()).build()
                );
        Assert.assertTrue("no results returned", handler.getObjects().size() == 1);
        ConnectorObject accountEntry = handler.getObjects().get(0);
        for (Attribute attr : accountEntry.getAttributes()) {
            if (attr.getName().equals(AccountAttribute.UID.getName())) {
                
                String uidValue = (String) AttributeUtil.getSingleValue(attr);
                String out = getConnection().executeCommand("logins -oxma -l \"" + username + "\"");
                String realUid = out.split(":")[1];
                
                Assert.assertEquals(realUid, uidValue);
                return;
            }
        }
        Assert.fail("no uid attribute found");
    }
    
    /**
     * This test requires previously created group.
     */
    @Test
    public void testFetchGid() {
        String groupName = getGroupName();
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.GROUP, 
                FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, groupName)), handler, 
                new OperationOptionsBuilder().setAttributesToGet(GroupAttribute.GID.getName()).build()
                );
        Assert.assertTrue("no results returned", handler.getObjects().size() == 1);
        ConnectorObject accountEntry = handler.getObjects().get(0);
        for (Attribute attr : accountEntry.getAttributes()) {
            if (attr.getName().equals(GroupAttribute.GID.getName())) {
                
                String uidValue = (String) AttributeUtil.getSingleValue(attr);
                String cmd = (!getConnection().isNis()) ? "cut -d: -f1,3 /etc/group | grep -v \"^[+-]\"" : "ypcat group | cut -d: -f1,3";
                cmd += " | grep " + groupName;
                String out = getConnection().executeCommand(cmd);
                
                Assert.assertEquals(out.split(":")[1].trim(), uidValue);
                return;
            }
        }
        Assert.fail("no uid attribute found");
    }

    @Override
    public boolean createGroup() {
        return true;
    }

    @Override
    public int getCreateUsersNumber() {
        return NR_OF_USERS;
    }
}
