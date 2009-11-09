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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.operation.search.OpSearchImpl;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpSearchImplTest {
    private static final String USERNAME_BASE = "sampleFooBar";
    private SolarisConfiguration config;
    private ConnectorFacade facade;

    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
        config.setPort(23);
        config.setConnectionType("TELNET");
        facade = SolarisTestCommon.createConnectorFacade(config);
        
        SolarisTestCommon.printIPAddress(config);
    }

    @After
    public void tearDown() throws Exception {
        config = null;
        facade = null;
    }
    
    /**
     * no exception should be thrown.
     * Searching for all account on the resource -- by null filter.
     */
    @Test 
    public void testSearchNullFilter() {
        final int NR_OF_USERS = 3;
        List<Pair<String, GuardedString>> pairs = null;
        try {
            // create users
            pairs = createUsers(NR_OF_USERS);
            
            ToListResultsHandler handler = new ToListResultsHandler();
            facade.search(ObjectClass.ACCOUNT, /* filter */null, handler, null);
            final List<ConnectorObject> l = handler.getObjects();
            final boolean[] found = new boolean[NR_OF_USERS];
            int cntr = 0;
            for (Pair<String, GuardedString> pair : pairs) {
                for (ConnectorObject connectorObject : l) {
                    if (pair.first.equals(connectorObject.getName().getNameValue())) {
                        found[cntr] = true;
                    }
                }
                cntr++;
            }
            
            cntr = 0;
            for (boolean b : found) {
                String msg = String.format("created user '%s' was not found on the resource.", pairs.get(cntr).first);
                Assert.assertTrue(msg, b);
                cntr++;
            }
        } finally {
            // cleanup users
            if (pairs != null) {
                cleanupUsers(pairs);
            }
        }
    }

    private List<Pair<String, GuardedString>> createUsers(int nrOfUsers) {
        List<Pair<String, GuardedString>> pairs = new ArrayList<Pair<String, GuardedString>>();
        
        for (int i = 0; i < nrOfUsers; i++) {
            final Set<Attribute> attrs = SolarisTestCommon.initSampleUser(nameFormatter(i));
            final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
            final String username = ((Name) attrMap.get(Name.NAME)).getNameValue();
            final GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
            pairs.add(new Pair<String, GuardedString>(username, password));
            
            facade.create(ObjectClass.ACCOUNT, attrs, null);
        }
        return pairs;
    }

    private String nameFormatter(int i) {
        return USERNAME_BASE + i;
    }

    private void cleanupUsers(List<Pair<String, GuardedString>> list) {
        List<String> notCleanedUp = new ArrayList<String>();
        
        for (Pair<String, GuardedString> pair : list) {
            facade.delete(ObjectClass.ACCOUNT, new Uid(pair.first), null);
            try {
                facade.authenticate(ObjectClass.ACCOUNT, pair.first, pair.second, null);
                notCleanedUp.add(pair.first);
            } catch (RuntimeException ex) {
                // OK
            }
        }//for
        
        if (notCleanedUp.size() == 0) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Account(s) was not cleaned up: ");
        for (Iterator<String> iterator = notCleanedUp.iterator(); iterator.hasNext();) {
            String acc = iterator.next();
            sb.append(String.format("'%s'", acc));
            String toAppend = (iterator.hasNext()) ? ", " : "." ;
            sb.append(toAppend);
        }
        Assert.fail(sb.toString());
    }

    

    /**
     * Searching using filter
     */
    @Test 
    public void testSearchWithFilter() {
        
        final Set<Attribute> attrs = SolarisTestCommon.initSampleUser();
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(
                AttributeUtil.toMap(attrs));
        String username = ((Name) attrMap.get(Name.NAME)).getNameValue();
        GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
        try {
            // create a new user
            Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
            Assert.assertNotNull(uid);

            ToListResultsHandler handler = new ToListResultsHandler();
            // attribute that we search for:
            Attribute usernameAttrToSearch = (Name) attrMap.get(Name.NAME);

            // perform search
            facade.search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(usernameAttrToSearch),
                    handler, null);
            final List<ConnectorObject> l = handler.getObjects();
            String msg = String.format(
                    "Size of results is less than expected: %s", l.size());
            Assert.assertTrue(msg, l.size() == 1);
            
            final String returnedUsername = l.get(0).getName().getNameValue();
            msg = String.format("The returned username '%s', differs from the expected '%s'", returnedUsername, username);
            Assert.assertTrue(msg, returnedUsername.equals(username));
        } finally {
            if (username != null && password != null) {
                // cleanup the new user
                facade.delete(ObjectClass.ACCOUNT, new Uid(username), null);
                try {
                    facade.authenticate(ObjectClass.ACCOUNT, username,
                            password, null);
                    Assert.fail(String.format(
                            "Account was not cleaned up: '%s'", username));
                } catch (RuntimeException ex) {
                    // OK
                }
            }
        }
    }
    
    @Test
    public void testGetShells() {
        ToListResultsHandler handler = new ToListResultsHandler();
        facade.search(OpSearchImpl.SHELL, null, handler, null);
        List<ConnectorObject> result = handler.getObjects();
        Assert.assertNotNull(result);
        
        if (result.size() > 0) {
            for (ConnectorObject connectorObject : result) {
                String value = connectorObject.getName().getNameValue();
                Assert.assertTrue(value.trim().length() > 0);
            }
        }
    }
}
