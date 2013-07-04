/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openicf.connectors.tam;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link TAMConnector} with the framework.
 *
 */
@Test(groups = { "integration" })
public class TAMConnectorTest {

    private TAMConfiguration config;
    protected ConnectorFacade facade = null;
    /**
     *
     */
    protected static final String CFG_BASE = "configuration.";
    protected static final String MSG = " must be configured for running unit test";

    /*
     * Configuration properties
     */
    protected static final String CFG_CERTIFICATEBASED = CFG_BASE + "certificateBased";
    protected static final String CFG_ADMIN_USERID = CFG_BASE + "adminUserID";
    protected static final String CFG_ADMIN_PASSWORD = CFG_BASE + "adminPassword";
    protected static final String CFG_CONFIGURATION_FILE_URL = CFG_BASE + "configurationFileURL";
    protected static final String CFG_DELETEFROMREGISTRY = CFG_BASE + "deleteFromRegistry";
    protected static final String CFG_SYNCGSOCREDENTIALS = CFG_BASE + "syncGSOCredentials";
    protected static final String USER_REGISTRY_TEMPLATE = "userRegistryTemplate";
    protected static final String GROUP_REGISTRY_TEMPLATE = "groupRegistryTemplate";
    /*
     * Example test properties. See the Javadoc of the TestHelpers class for the
     * location of the public and private configuration files.
     */
    private PropertyBag properties = null;
    private String userRegistryTemplate = null;
    private String groupRegistryTemplate = null;

    /**
     * Create the test suite
     *
     * @throws Exception
     *             a resource exception
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        properties = TestHelpers.getProperties(TAMConnector.class);
        config = new TAMConfiguration();

        Boolean certificateBased =
                properties.getProperty(CFG_CERTIFICATEBASED, Boolean.class, Boolean.FALSE);

        if (!certificateBased) {
            // adminUserID and adminPassword are private properties read from
            // private configuration file
            String adminUserID = properties.getStringProperty(CFG_ADMIN_USERID);
            Assert.assertNotNull(adminUserID, CFG_ADMIN_USERID + MSG);
            config.setAdminUserID(adminUserID);
            // adminUserID and adminPassword are private properties read from
            // private configuration file
            GuardedString adminPassword =
                    new GuardedString(properties.getProperty(CFG_ADMIN_PASSWORD, String.class, "")
                            .toCharArray());
            Assert.assertNotNull(adminPassword, CFG_ADMIN_PASSWORD + MSG);
            config.setAdminPassword(adminPassword);
        } else {
            config.setCertificateBased(true);
        }
        // Host is a public property read from public configuration file
        String configurationFileURL = properties.getStringProperty(CFG_CONFIGURATION_FILE_URL);
        Assert.assertNotNull(configurationFileURL, CFG_CONFIGURATION_FILE_URL + MSG);
        config.setConfigurationFileURL(configurationFileURL);

        Boolean deleteFromRegistry = properties.getProperty(CFG_DELETEFROMREGISTRY, Boolean.class);
        if (null != deleteFromRegistry) {
            config.setDeleteFromRegistry(deleteFromRegistry);
        }
        Boolean syncGSOCredentials = properties.getProperty(CFG_SYNCGSOCREDENTIALS, Boolean.class);
        if (null != syncGSOCredentials) {
            config.setSyncGSOCredentials(syncGSOCredentials);
        }
        userRegistryTemplate =
                properties.getProperty(USER_REGISTRY_TEMPLATE, String.class,
                        "uid=%s,cn=users,__configureme__");
        Assert.assertNotNull(userRegistryTemplate, USER_REGISTRY_TEMPLATE + MSG);
        groupRegistryTemplate =
                properties.getProperty(GROUP_REGISTRY_TEMPLATE, String.class,
                        "cn=%s,cn=groups,__configureme__");
        Assert.assertNotNull(groupRegistryTemplate, GROUP_REGISTRY_TEMPLATE + MSG);
    }

    /**
     * Clean up the test suite
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        // Dispose of all connector pools, resources, etc.
        // ConnectorFacadeFactory.getInstance().dispose();
    }

    /**
     * Setup the test
     *
     * @throws Exception
     */
    @BeforeMethod
    public void setup() throws Exception {
        // config = newConfiguration();
        facade = getFacade();
    }

    /**
     * @throws Exception
     */
    @AfterMethod
    public void teardown() throws Exception {
        // config = null;
        facade = null;
    }

    protected ConnectorFacade getFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(TAMConnector.class, config);
        return factory.newInstance(impl);
    }

    /**
     * Test of init method, of class TAMConnector.
     */
    @Test(enabled = false)
    public void testInit() {
        System.out.println("init");
        TAMConnector connector = new TAMConnector();
        try {
            connector.init(config);
        } catch (Exception e) {
            Assert.fail("PDAdmin initialisation is failed");
        }
    }

    @Test(enabled = true)
    public void testCreateGroup() {
        Uid uid = new Uid("AAB0001");
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Name(uid.getUidValue()));
        attributes.add(AttributeBuilder.build(PredefinedAttributes.DESCRIPTION,
                "Test Group for TAM Connector test"));
        // attributes.add(AttributeBuilder.build(TAMConnector.ATTR_REGISTRY_NAME,
        // "cn=" + uid.getUidValue() +
        // ",cn=SecurityGroups,secAuthority=Default"));
        attributes.add(AttributeBuilder.build(TAMConnector.ATTR_REGISTRY_NAME, String.format(
                groupRegistryTemplate, uid.getUidValue())));
        OperationOptions oo = null;
        Uid result = facade.create(ObjectClass.GROUP, attributes, oo);
        Assert.assertEquals(uid, result);
    }

    /**
     * Test of create method, of class TAMConnector.
     */
    @Test(enabled = true, dependsOnMethods = { "testCreateGroup" })
    public void testCreateUser() {
        Uid expResult = new Uid("AAA0001");
        Set<Attribute> attributes = sampleUser(expResult.getUidValue());
        OperationOptions oo = null;
        Uid result = facade.create(ObjectClass.ACCOUNT, attributes, oo);
        Assert.assertEquals(expResult, result);
    }

    @Test(enabled = true, dependsOnMethods = { "testCreateUser" })
    public void testUpdateUser() {
        Uid uid = new Uid("AAA0001");
        Set<Attribute> replaceAttributes = new HashSet<Attribute>();
        replaceAttributes.add(AttributeBuilder.build(PredefinedAttributes.GROUPS_NAME, "AAB0001"));
        OperationOptions oo = null;
        Uid result = facade.update(ObjectClass.ACCOUNT, uid, replaceAttributes, oo);
        Assert.assertEquals(uid, result);
    }

    /**
     * Test of delete method, of class TAMConnector.
     */
    @Test(enabled = true, dependsOnMethods = { "testCreateUser", "testUpdateUser" })
    public void testDeleteUser() {
        Uid uid = new Uid("AAA0001");
        OperationOptions oo = null;
        facade.delete(ObjectClass.ACCOUNT, uid, oo);
    }

    /**
     * Test of delete method, of class TAMConnector.
     */
    @Test(enabled = true, dependsOnMethods = { "testDeleteUser" })
    public void testDeleteGroup() {
        Uid uid = new Uid("AAB0001");
        OperationOptions oo = null;
        facade.delete(ObjectClass.GROUP, uid, oo);

    }

    protected Set<Attribute> sampleUser(String testUser) {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Name(testUser));
        attributes.add(AttributeBuilder.build(TAMConnector.ATTR_FIRST_NAME, "TAM Connector Test"));
        attributes.add(AttributeBuilder.build(TAMConnector.ATTR_IMPORT_FROM_REGISTRY, false));
        attributes.add(AttributeBuilder.build(TAMConnector.ATTR_LAST_NAME, testUser));
        attributes.add(AttributeBuilder.build(PredefinedAttributes.DESCRIPTION,
                "Test User for TAM Connector test"));
        attributes.add(AttributeBuilder.build(TAMConnector.ATTR_REGISTRY_NAME, String.format(
                userRegistryTemplate, testUser)));
        attributes.add(AttributeBuilder.build(PredefinedAttributes.GROUPS_NAME, Arrays
                .asList(new String[] { "AAB0001" })));
        attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME,
                new GuardedString("Passw0rd".toCharArray())));
        return attributes;
    }

    /**
     * Test of executeQuery method, of class TAMConnector.
     */
    @Test(enabled = false)
    public void testExecuteQuery() {
        EqualsFilter filter = new EqualsFilter(AttributeBuilder.build(Name.NAME, "AAB0001"));
        OperationOptions oo = null;
        List<ConnectorObject> results =
                TestHelpers.searchToList(facade, ObjectClass.GROUP, filter, oo);
        Assert.assertFalse(results.isEmpty(), "result is empty");
    }

    /**
     * Test of authenticate method, of class TAMConnector.
     */
    @Test(enabled = false)
    public void testSuccesAuthenticate() {
        Uid uid = new Uid("AAA0001");
        GuardedString password = new GuardedString("Passw0rd".toCharArray());
        OperationOptions oo = null;
        Uid result = facade.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, oo);
        Assert.assertEquals(uid, result);
    }

    @Test(enabled = false, expectedExceptions = Exception.class)
    public void testFailAuthenticate() {
        Uid uid = new Uid("AAA0001");
        GuardedString password = new GuardedString("dr0wssaP".toCharArray());
        OperationOptions oo = null;
        Uid result = facade.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, oo);
        Assert.assertEquals(uid, result);
    }
}
