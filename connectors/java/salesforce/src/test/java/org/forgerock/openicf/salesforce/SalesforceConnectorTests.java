/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openicf.salesforce;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.identityconnectors.test.common.TestHelpers;
import org.identityconnectors.test.common.PropertyBag;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link org.forgerock.openicf.salesforce.SalesforceConnector} with the framework.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class SalesforceConnectorTests {

    /*
    * Example test properties.
    * See the Javadoc of the TestHelpers class for the location of the public and private configuration files.
    */
    private static final PropertyBag properties = TestHelpers.getProperties(SalesforceConnector.class);
    // Host is a public property read from public configuration file
    //private static final String HOST = properties.getStringProperty("configuration.host");
    // Login and password are private properties read from private configuration file 
    private static final String CLIENTID = properties.getStringProperty("configuration.clientId");
    private static final GuardedString CLIENTSECRET = properties.getProperty("configuration.clientSecret", GuardedString.class);
    private static final String USERNAME = properties.getStringProperty("configuration.username");
    private static final GuardedString PASSWORD = properties.getProperty("configuration.password", GuardedString.class);
    private static final GuardedString SECURITYTOKEN = properties.getProperty("configuration.securityToken", GuardedString.class);

    //set up logging
    private static final Log log = Log.getLog(SalesforceConnectorTests.class);
    private static SalesforceConfiguration conf = null;

    @BeforeClass
    public static void setUp() {
        conf = new SalesforceConfiguration();
        conf.setClientId(CLIENTID);
        conf.setClientSecret(CLIENTSECRET);
        conf.setUsername(USERNAME);
        conf.setPassword(PASSWORD);
        conf.setSecurityToken(SECURITYTOKEN);
        //TestHelpers.fillConfiguration(conf,properties.getProperty("configuration",Map.class));
        conf.validate();
    }

    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        //
    }

    @Test
    public void exampleTest1() {
        log.info("Running Test 1...");
        //getFacade(conf).schema();
        System.out.println(SerializerUtil.serializeXmlObject(getFacade(conf).schema(), false));
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
    }

    @Test
    public void exampleTest2() {
        log.info("Running Test 2...");
        //Another example using TestHelpers
        //List<ConnectorObject> results = TestHelpers.searchToList(theConnector, ObjectClass.GROUP, filter);
    }

    protected ConnectorFacade getFacade(SalesforceConfiguration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(SalesforceConnector.class, config);
        return factory.newInstance(impl);
    }
}
