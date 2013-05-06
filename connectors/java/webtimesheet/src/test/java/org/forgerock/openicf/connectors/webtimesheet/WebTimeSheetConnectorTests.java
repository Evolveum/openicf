/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openicf.connectors.webtimesheet;



import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link org.forgerock.openicf.connectors.webtimesheet.WebTimeSheetConnector} with the framework.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class WebTimeSheetConnectorTests {

    /*
    * Example test properties.
    * See the Javadoc of the TestHelpers class for the location of the public and private configuration files.
    */
    private static final PropertyBag properties = TestHelpers.getProperties(WebTimeSheetConnector.class);
    
    
    // Host is a public property read from public configuration file
    private static final String WTS_URI = properties.getStringProperty("configuration.wtsURI");
    private static final String ADMIN_UID = properties.getStringProperty("configuration.adminUid");
    private static final GuardedString ADMIN_PASSWORD = properties.getProperty("configuration.adminPassword", GuardedString.class);
    //private static final GuardedString ADMIN_PASSWORD = properties.getStringProperty("configuration.adminPassword");
    private static final String WTS_HOST = properties.getStringProperty("configuration.wtsHost");
    private static final Integer WTS_PORT = properties.getProperty("configuration.wtsPort", Integer.class);

    
    
    //set up logging
    private static final Log log = Log.getLog(WebTimeSheetConnectorTests.class);
    private static WebTimeSheetConfiguration conf = null;
    
    private static Uid newuid = null;

    @BeforeClass
    public static void setUp() {
        Assert.assertNotNull(WTS_URI);
        Assert.assertNotNull(ADMIN_UID);
        Assert.assertNotNull(ADMIN_PASSWORD);
        Assert.assertNotNull(WTS_HOST);
        Assert.assertNotNull(WTS_PORT);
        
        
        conf = new WebTimeSheetConfiguration();
        conf.setAdminPassword(ADMIN_PASSWORD);
        conf.setAdminUid(ADMIN_UID);
        conf.setWtsHost(WTS_HOST);
        conf.setWtsPort(WTS_PORT);
        conf.setWtsURI(WTS_URI);
        
        //TestHelpers.fillConfiguration(conf,properties.getProperty("configuration",Map.class));
        conf.validate();

        
        //
        //other setup work to do before running tests
        //
        
    }

    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        //
    }
    
    @Test
    public void connectionTest() {
        log.info("Test Connection");
        getFacade(conf).test();
        
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
    }

    @Test
    public void schemaTest() {
        log.info("Fetch Schema");
        
        Schema schema = getFacade(conf).schema();
        Assert.assertNotNull(schema.getObjectClassInfo());
    }

    @Test
    public void searchUser() {
        log.info("Search User");
        Attribute searchName = AttributeBuilder.build("__NAME__", ADMIN_UID);
        List<ConnectorObject> r = TestHelpers.searchToList(getFacade(conf), ObjectClass.ACCOUNT, FilterBuilder.equalTo(searchName));
        log.info("handled {0} results", r.size());
        System.out.println("Users:" + r);
        Assert.assertTrue(r.size() > 0,"zero results returned");
    }
    
    @Test
    public void searchUserByUID() {
        log.info("Search User By UID");
        Attribute searchName = AttributeBuilder.build("__UID__", "1");
        List<ConnectorObject> r = TestHelpers.searchToList(getFacade(conf), ObjectClass.ACCOUNT, FilterBuilder.equalTo(searchName));
        log.info("handled {0} results", r.size());
        System.out.println("Users:" + r);
        Assert.assertTrue(r.size() > 0,"zero results returned");
    }
    
    @Test
    public void listUsers() {
        log.info("List Users");
        List<ConnectorObject> r = TestHelpers.searchToList(getFacade(conf), ObjectClass.ACCOUNT, null);
        log.info("handled {0} results", r.size());
        System.out.println("Users:" + r);
        Assert.assertTrue(r.size() > 0,"zero results returned");
    }
    
    @Test
    public void createUser() {
        log.info("Create New User");
        
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.build("FirstName", "Unit"));
        attrs.add(AttributeBuilder.build("LastName", "Test"));
        attrs.add(AttributeBuilder.build("Email", "utest@example.com"));
        attrs.add(AttributeBuilder.build("LoginName", "utest"));
        attrs.add(AttributeBuilder.build("Password", ADMIN_PASSWORD));
        
        newuid = getFacade(conf).create(ObjectClass.ACCOUNT, attrs, null);
        Assert.assertNotNull(newuid);
    }
    
    @Test
    public void updateAndDeleteUser() {
        log.info("Update and Delete New User");
        
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.build("FirstName", "Changed"));
        attrs.add(AttributeBuilder.build("LoginName", "changed@example.com"));
        attrs.add(AttributeBuilder.build("Password", ADMIN_PASSWORD));
        
        Uid updateduid = getFacade(conf).update(ObjectClass.ACCOUNT, newuid, attrs, null);
        Assert.assertNotNull(updateduid);
    
        log.info("Delete New User");
        getFacade(conf).delete(ObjectClass.ACCOUNT, newuid, null);
        
    }

    protected ConnectorFacade getFacade(WebTimeSheetConfiguration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(WebTimeSheetConnector.class, config);
        return factory.newInstance(impl);
    }
}
