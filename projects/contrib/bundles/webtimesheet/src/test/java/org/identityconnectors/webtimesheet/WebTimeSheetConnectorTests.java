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
package org.identityconnectors.webtimesheet;

import java.util.*;

import org.junit.*;
import junit.framework.Assert;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.identityconnectors.common.security.GuardedString;

/**
 * Attempts to test the {@link WebTimeSheetConnector} with the framework.
 * 
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 */
public class WebTimeSheetConnectorTests {

    private static final PropertyBag properties = TestHelpers.getProperties(WebTimeSheetConnector.class);
    private static final String URL = properties.getStringProperty("WTSURL");
    private static final String LOGIN = properties.getStringProperty("WTSLOGIN");
    private static final String PASSWORD = properties.getStringProperty("WTSPASSWORD");
    private static final String APPNAME = properties.getStringProperty("WTSAPPNAME");
    private static final String APPPASSWORD = properties.getStringProperty("WTSAPPPASSWORD");
    //set up logging
    private static final Log log = Log.getLog(WebTimeSheetConnectorTests.class);
    private static WebTimeSheetConnector testConnector = new WebTimeSheetConnector();

    @BeforeClass
    public static void setUp() {
        Assert.assertNotNull(URL);

        //
        //other setup work to do before running tests
        //

        log.info("Using URL: {0}", URL);
        WebTimeSheetConfiguration testConfig = new WebTimeSheetConfiguration();
        testConfig.setURLProperty(URL);
        testConfig.setAdminPasswordProperty(PASSWORD);
        testConfig.setAdminUidProperty(LOGIN);
        testConfig.setAppNameProperty(APPNAME);
        testConfig.setAppPasswordProperty(APPPASSWORD);

        testConnector.init(testConfig);


    }

    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        //
    }

    @Test
    public void testConnection() {
        log.info("Running Connection Test...");
        testConnector.checkAlive();
    }

    @Test
    public void testSchema() {
        Schema schema = testConnector.schema();
        System.out.println("Schema information: " + schema.toString());
        //Schema should not be null
        Assert.assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        Assert.assertNotNull(objectInfos);
        Assert.assertEquals(2, objectInfos.size());
    }

    @Test
    public void testListUsers() {
        log.info("Running List Users Test...");
        List<ConnectorObject> r = TestHelpers.searchToList(testConnector, ObjectClass.ACCOUNT, null);

        log.info("handled {0} results", r.size());
        System.out.println("Users:" + r);
        Assert.assertTrue("zero results returned", r.size() > 0);
    }

    @Test
    public void testListDepartments() {
        log.info("Running List Departments Test...");
        List<ConnectorObject> r = TestHelpers.searchToList(testConnector, new ObjectClass("Department"), null);
        log.info("handled {0} results", r.size());
        System.out.println("Departments:" + r);
        Assert.assertEquals(1, r.size());
    }

    @Test
    public void testListAllDepartments() {
        log.info("Running List All Departments Test...");
        OperationOptionsBuilder ob = new OperationOptionsBuilder();
        ob.setScope(OperationOptions.SCOPE_SUBTREE);
        List<ConnectorObject> r = TestHelpers.searchToList(testConnector, new ObjectClass("Department"), null, ob.build());
        log.info("handled {0} results", r.size());
        System.out.println("Departments:" + r);
        Assert.assertTrue("zero results returned", r.size() > 0);
    }

    @Test
    public void testScopedListDepartments() {
        log.info("Running Scoped List Departments Test...");
        OperationOptionsBuilder ob = new OperationOptionsBuilder();

        ob.setContainer(new QualifiedUid(new ObjectClass("Department"), new Uid("1")));
        ob.setScope(OperationOptions.SCOPE_ONE_LEVEL);

        List<ConnectorObject> r = TestHelpers.searchToList(testConnector, new ObjectClass("Department"), null, ob.build());
        log.info("handled {0} results", r.size());
        System.out.println("Departments:" + r);
        Assert.assertTrue("zero results returned", r.size() > 0);
    }

    @Test
    public void testGetUserById() {


        Filter filter = FilterBuilder.equalTo(new Name("2"));

        log.info("Running Get User Test...");
        List<ConnectorObject> r = TestHelpers.searchToList(testConnector, ObjectClass.ACCOUNT, filter);
        log.info("handled {0} results", r.size());
        System.out.println("Users:" + r);
        Assert.assertEquals(1, r.size());

    }

    @Test
    public void testCreateDeleteUser() {
        log.info("Running Create Users Test...");

        Set<Attribute> attrSet = new HashSet<Attribute>();
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_LAST_NAME, "John"));
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_FIRST_NAME, "Doe"));
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_LOGIN_NAME, "jdoe"));
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_INTERNAL_EMAIL, "jdoe@nulli.com"));
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_DEPARTMENT, "1"));
        attrSet.add(AttributeBuilder.buildEnabled(false));
        attrSet.add(AttributeBuilder.buildPassword(new GuardedString("P@ssw0rd!".toCharArray())));

        Assert.assertFalse(attrSet.isEmpty());

        // create the user
        Uid testUserUid = testConnector.create(ObjectClass.ACCOUNT, attrSet, null);
        Assert.assertNotNull(testUserUid);

        // modify the new user
        attrSet = new HashSet<Attribute>();
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_LOGIN_NAME, "jjones"));
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_LAST_NAME, "Jones"));
        attrSet.add(AttributeBuilder.buildPassword(new GuardedString("P@ssw0rd!".toCharArray())));
        attrSet.add(AttributeBuilder.build(WebTimeSheetConnector.ATTR_INTERNAL_EMAIL, "jjones@nulli.com"));
        testUserUid = testConnector.update(ObjectClass.ACCOUNT, testUserUid, attrSet, null);
        Assert.assertNotNull(testUserUid);

        // delete the new user
        testConnector.delete(ObjectClass.ACCOUNT, testUserUid, null);

    }
}
