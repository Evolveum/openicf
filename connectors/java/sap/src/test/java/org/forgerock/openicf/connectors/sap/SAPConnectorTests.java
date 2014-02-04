/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 * 
* The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
* You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html See the License for the specific
 * language governing permission and limitations under the License.
 * 
* When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://forgerock.org/license/CDDLv1.0.html If
 * applicable, add the following below the CDDL Header, with the fields enclosed
 * by brackets [] replaced by your own identifying information: "Portions
 * Copyrighted [year] [name of copyright owner]"
 * 
* @author Gael Allioux <gael.allioux@forgerock.com>
 * 
*/
package org.forgerock.openicf.connectors.sap;

import java.util.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link SAPConnector} with the framework.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * @version $Revision$ $Date$
 */
public class SAPConnectorTests {

    private static final PropertyBag properties = TestHelpers.getProperties(SAPConnector.class);
    private SAPConfiguration config;
    private SAPConnector connector;
    /**
     * Single ThreadSafe Facade.
     */
    private ConnectorFacade facade = null;
    //set up logging
    private static final Log LOGGER = Log.getLog(SAPConnectorTests.class);

    @BeforeClass
    public void setUp() {
        config = new SAPConfiguration();
        config.setClient(properties.getStringProperty("configuration.client"));
        config.setUser(properties.getStringProperty("configuration.user"));
        config.setHost(properties.getStringProperty("configuration.host"));
        config.setDestination(properties.getStringProperty("configuration.destination"));
        config.setDirectConnection(true);
        config.setSapRouter(properties.getStringProperty("configuration.sapRouter"));
        config.setSystemNumber(properties.getStringProperty("configuration.systemNumber"));
        config.setLanguage(properties.getStringProperty("configuration.language"));
        config.setPassword(properties.getProperty("configuration.password", GuardedString.class));

        config.setTestScriptFileName(properties.getStringProperty("configuration.testScriptFileName"));
        config.setSearchScriptFileName(properties.getStringProperty("configuration.searchScriptFileName"));
        config.setSearchAllScriptFileName(properties.getStringProperty("configuration.searchAllScriptFileName"));
        config.setUpdateScriptFileName(properties.getStringProperty("configuration.updateScriptFileName"));
        config.setCreateScriptFileName(properties.getStringProperty("configuration.createScriptFileName"));
        config.setDeleteScriptFileName(properties.getStringProperty("configuration.deleteScriptFileName"));

        //connector = new SAPConnector();
        //connector.init(config);
        facade = getFacade(config);


    }

    @AfterClass
    public static void tearDown() {
//        connector.dispose();
    }

//    @Test(enabled = false)
//    public void testCheckAlive() {
//        LOGGER.info("Running testCheckAlive...");
//        //You can use TestHelpers to do some of the boilerplate work in running a search
//        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
//        connector.checkAlive();
//    }
    @Test(enabled = false)
    public void testTest() {
        LOGGER.info("Running testTest...");
        //Another example using TestHelpers
        //List<ConnectorObject> results = TestHelpers.searchToList(theConnector, ObjectClass.GROUP, filter);
        //connector.test();
        facade.test();
    }

    @Test(enabled = false)
    public void testQueryListAllSAPHREMployee() {
        // just need to send an empty query
        // to fetch all entries
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        List<ConnectorObject> results = TestHelpers.searchToList(facade, new ObjectClass("EMPLOYEE"), null, oob.build());
        System.out.println("testQueryListAllSAPHREMployee reports " + results.size() + " employees");
        System.out.println(results.toString());
    }

    @Test(enabled = false)
    public void testQueryGetSAPHREmployee() {
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("EMPLOYEE_ID", "200014"));
        List<ConnectorObject> results = TestHelpers.searchToList(facade, new ObjectClass("EMPLOYEE"), filter, oob.build());
        System.out.println("testQueryGetSAPHREmployee reports " + results.size() + " employee");
        System.out.println(results.toString());
        System.out.println("Returned email is: " + results.get(0).getAttributeByName("COMMUNICATION:EMAIL:ID").getValue().get(0));
        System.out.println("Returned sysname is: " + results.get(0).getAttributeByName("COMMUNICATION:ACCOUNT:ID").getValue().get(0));
    }

    @Test(enabled = false)
    public void testUpdateSAPHREmail() {
        String empno = "200013";
        Uid uid = new Uid(empno);
        AttributeBuilder ab = new AttributeBuilder();
        ab.setName("COMMUNICATION:EMAIL:ID");
        ab.addValue("Bob.Flemming@fast.com");
        java.util.Set<Attribute> replaceAttributes = new java.util.HashSet<Attribute>();
        replaceAttributes.add(ab.build());
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        facade.update(new ObjectClass("EMPLOYEE"), uid, replaceAttributes, oob.build());
        System.out.println("UPDATE done, retrieving user to check... ");

        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("EMPLOYEE_ID", empno));
        List<ConnectorObject> results = TestHelpers.searchToList(facade, new ObjectClass("EMPLOYEE"), filter, oob.build());
        System.out.println("Returned email is: " + results.get(0).getAttributeByName("COMMUNICATION:EMAIL:ID").getValue().get(0));
    }

    @Test(enabled = false)
    public void testUpdateSAPHRSysName() {
        String empno = "200013";
        Uid uid = new Uid(empno);
        AttributeBuilder ab = new AttributeBuilder();
        ab.setName("COMMUNICATION:ACCOUNT:ID");
        ab.addValue("BFlemm");
        java.util.Set<Attribute> replaceAttributes = new java.util.HashSet<Attribute>();
        replaceAttributes.add(ab.build());
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        facade.update(new ObjectClass("EMPLOYEE"), uid, replaceAttributes, oob.build());
        System.out.println("UPDATE done, retrieving user to check... ");

        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("EMPLOYEE_ID", empno));
        List<ConnectorObject> results = TestHelpers.searchToList(facade, new ObjectClass("EMPLOYEE"), filter, oob.build());
        System.out.println("Returned account is: " + results.get(0).getAttributeByName("COMMUNICATION:ACCOUNT:ID").getValue().get(0));
    }

    @Test(enabled = false)
    public void testQueryListAllSAPR3Users() {
        // just need to send an empty query
        // to fetch all entries
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        List<ConnectorObject> results = TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, null, oob.build());
        //System.out.println("testQueryListAllSAPR3 reports " + results.size() + " employees");
        //System.out.println(results.toString());
    }

    @Test(enabled = false, priority = 2)
    public void testQueryGetSAPR3User() {
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("USERNAME", "BOBF"));
        List<ConnectorObject> results = TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, filter, oob.build());
        //System.out.println("testQueryListAllSAPR3 reports " + results.size() + " employees");
        //System.out.println(results.toString());
    }

    @Test(enabled = false, priority = 3)
    public void testDeleteSAPR3User() {
        Uid uid = new Uid("TESTFR1");
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        facade.delete(ObjectClass.ACCOUNT, uid, oob.build());
    }

    protected ConnectorFacade getFacade(SAPConfiguration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(SAPConnector.class, config);
        return factory.newInstance(impl);
    }
}