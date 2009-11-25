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

import static org.identityconnectors.solaris.test.SolarisTestCommon.getStringProperty;
import static org.identityconnectors.solaris.test.SolarisTestCommon.getProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpUpdateImplTest {
    
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
     * create a new user and try to change its password, and later try to authenticate
     */
    @Test
    public void testUpdate() {
        // create a new user
        final Set<Attribute> attrs = initSampleUser();
        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        final String username = ((Name) attrMap.get(Name.NAME)).getNameValue();
        final GuardedString password = SolarisUtil.getPasswordFromMap(attrMap);
        
        try {
            Uid uid = facade.create(ObjectClass.ACCOUNT, attrs, null);
            Assert.assertNotNull(uid);
        
            // try to authenticate 
            try {
                facade.authenticate(ObjectClass.ACCOUNT, username, password, null);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                Assert.fail(String.format("Authenticate failed for newly created user: '%s'\n ExceptionMessage: %s", username, ex.getMessage()));
            }
            
            Set<Attribute> replaceAttributes = new HashSet<Attribute>();
            final GuardedString newPassword = getProperty("modified.samplePasswd", GuardedString.class);
            Attribute chngPasswdAttribute = AttributeBuilder.buildPassword(newPassword);
            replaceAttributes.add(chngPasswdAttribute);
            // 1) PERFORM THE UPDATE OF PASSWORD
            facade.update(ObjectClass.ACCOUNT, new Uid(username), replaceAttributes , null);
            
            // 2) try to authenticate with new password
            try {
                facade.authenticate(ObjectClass.ACCOUNT, username, newPassword, null);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                Assert.fail(String.format("Authenticate failed for user with changed password: '%s'\n ExceptionMessage: %s", username, ex.getMessage()));
            }
            
        } finally {
            // cleanup the new user
            facade.delete(ObjectClass.ACCOUNT, new Uid(username), null);
            try {
                facade.authenticate(ObjectClass.ACCOUNT, username, password, null);
                Assert.fail(String.format("Account was not cleaned up: '%s'", username));
            } catch (RuntimeException ex) {
                //OK
            }
        }
    }
    
    @Test (expected=RuntimeException.class) 
    public void unknownObjectClass() {
        String username = config.getUserName();
        Set<Attribute> replaceAttributes = new HashSet<Attribute>();
        final GuardedString newPassword = getProperty("modified.samplePasswd", GuardedString.class);
        Attribute chngPasswdAttribute = AttributeBuilder.buildPassword(newPassword);
        replaceAttributes.add(chngPasswdAttribute);

        facade.update(new ObjectClass("NONEXISTING_OBJECTCLASS"), new Uid(
                username), replaceAttributes, null);
    }
    
    @Test(expected = RuntimeException.class)
    public void testUpdateUnknownUid() {
        Set<Attribute> replaceAttributes = new HashSet<Attribute>();
        final GuardedString newPassword = getProperty("modified.samplePasswd", GuardedString.class);
        Attribute chngPasswdAttribute = AttributeBuilder.buildPassword(newPassword);
        replaceAttributes.add(chngPasswdAttribute);

        facade.update(ObjectClass.ACCOUNT, new Uid("NONEXISTING_UID___"), replaceAttributes, null);
    }
    
    /* ************* AUXILIARY METHODS *********** */


    /** fill in sample user/password for sample user used in create */
    private Set<Attribute> initSampleUser() {
        Set<Attribute> res = new HashSet<Attribute>();
        
        res.add(AttributeBuilder.build(Name.NAME, getStringProperty("sampleUser")));
        
        GuardedString samplePasswd = getProperty("samplePasswd", GuardedString.class);
        res.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, samplePasswd));
        
        return res;
    }
}
