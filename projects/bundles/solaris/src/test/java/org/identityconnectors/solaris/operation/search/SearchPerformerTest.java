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
package org.identityconnectors.solaris.operation.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.constants.AccountAttributes;
import org.identityconnectors.solaris.constants.SolarisAttribute;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SearchPerformerTest {
    private SolarisConfiguration config;
    private SolarisConnector connector;
    private String username;
    private GuardedString password;
    private Set<Attribute> attrs;

    @Before
    public void setUp() throws Exception {
        config = SolarisTestCommon.createConfiguration();
        connector = SolarisTestCommon.createConnector(config);
        // create a new user
        attrs = SolarisTestCommon.initSampleUser();
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(
                AttributeUtil.toMap(attrs));
        username = ((Name) attrMap.get(Name.NAME)).getNameValue();
        password = SolarisUtil.getPasswordFromMap(attrMap);
    }

    @After
    public void tearDown() {
        config = null;
        username = null;
        password = null;
        connector = null;
        attrs = null;
    }

    /*
     * note: if this code would make its way into the tearDown() passage, 
     * the information about Already existing account (an exception) could be lost. 
     */
    private void doCleanup() {
        try {
            // if account already is deleted, return from the test.
            connector.authenticate(ObjectClass.ACCOUNT, username, password, null);
        } catch (RuntimeException ex) {
            return;
        }
        
        // cleanup the new user
        connector.delete(ObjectClass.ACCOUNT, new Uid(username), null);
        try {
            connector.authenticate(ObjectClass.ACCOUNT, username, password, null);
            Assert.fail(String.format("Account was not cleaned up: '%s'", username));
        } catch (RuntimeException ex) {
            // OK
        }
    }

    @Test
    public void testSimpleSearchByInactiveAttribute() {
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        Assert.assertNotNull(uid);
        SearchPerformer sp = new SearchPerformer((SolarisConfiguration) connector.getConfiguration(), connector.getConnection());
        SolarisAttribute attribute = AccountAttributes.INACTIVE;
        
        Set<Uid> result = sp.performSearch(attribute, "-1" /*
                                                            * default value of
                                                            * inactive attribute
                                                            */, false);
        
        Assert.assertTrue(result.size() >= 1);
        boolean b = false;
        for (Uid uidx : result) {
            b = b | uidx.getUidValue().equals(username);
        }
        Assert.assertTrue(b);
        doCleanup();
    }
    
    @Test
    public void testSearchByUserShell() {
        final String USER_SHELL_TYPE = "/bin/ksh";
        attrs.add(AttributeBuilder.build("shell", USER_SHELL_TYPE)); // selecting Korn shell on purpose
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        Assert.assertNotNull(uid);
        SearchPerformer sp = new SearchPerformer((SolarisConfiguration) connector.getConfiguration(), connector.getConnection());
        SolarisAttribute attribute = AccountAttributes.SHELL;
        
        Set<Uid> result = sp.performSearch(attribute, USER_SHELL_TYPE, false);
        
        Assert.assertTrue(result.size() >= 1);
        boolean b = false;
        for (Uid uidx : result) {
            b = b | uidx.getUidValue().equals(username);
        }
        Assert.assertTrue(b);
        doCleanup();
    }
}
