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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisHelper;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class OpSearchImplTest {
    private SolarisConfiguration config;
    private ConnectorFacade facade;

    /**
     * set valid credentials based on build.groovy property file
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
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
        ToListResultsHandler handler = new ToListResultsHandler();
        facade.search(ObjectClass.ACCOUNT, /*filter*/ null, handler, null);
        
        //print
//        final List<ConnectorObject> l = handler.getObjects();
//        for (ConnectorObject connectorObject : l) {
//            Attribute attr = connectorObject.getAttributeByName(Name.NAME);
//            System.out.println(attr.getValue().get(0));
//        }
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
        GuardedString password = SolarisHelper.getPasswordFromMap(attrMap);
        try {
            // create a new user
            Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
            Assert.assertNotNull(uid);

            ToListResultsHandler handler = new ToListResultsHandler();
            // attribute that we search for:
            Attribute attr = (Name) attrMap.get(Name.NAME);

            // perform search
            facade.search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(attr),
                    handler, null);
            final List<ConnectorObject> l = handler.getObjects();
            String msg = String.format(
                    "Size of results is less than expected: %s", l.size());
            Assert.assertTrue(msg, l.size() == 1);
            
            //print
            System.out.println(l.get(0));
            
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
    
    @Test @Ignore
    public void testSearchUnknownUid() {
        //Filter filter = new SolarisFilter("NONEXISTING_USERNAME__");
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                return false;
            }
        };
        facade.search(ObjectClass.ACCOUNT, /*filter*/ null, handler, null);
    }
}
