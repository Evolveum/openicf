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

package org.identityconnectors.oracle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;

/**
 * This class shows how we can use the connector in any application using the framework. To run the class, you must first build the connector using
 * ant to have jars file in dist directory.
 * @author kitko
 */
public class OracleConnectorTestApplication {

    public static void main(String[] args) throws MalformedURLException {
        // Here we find our connectors jar files
        URL[] jarFiles = findConnectorsJar(args);
        // Creates the local manager instead of RemoteManager
        ConnectorInfoManager manager = ConnectorInfoManagerFactory.getInstance().getLocalManager(jarFiles);
        // Just for presentation purpose print all connectors available
        printAvailableConnectors(manager.getConnectorInfos());
        // Find key for oracle connector
        ConnectorKey key = findOracleConnectorKey(manager.getConnectorInfos());
        ConnectorInfo connectorInfo = manager.findConnectorInfo(key);
        // Create the configuration
        APIConfiguration config = connectorInfo.createDefaultAPIConfiguration();
        // Now we can set the connect properties of connector
        ConfigurationProperties configProps = config.getConfigurationProperties();
        // Print all available configuration options
        printConfigProperties(configProps);
        // Here we could harcode the connect properties
        // configProps.setPropertyValue("host", "myHost");
        // But we rather use TestHelpers property bag
        setConnectProperties(configProps);
        ConnectorFacade facade = ConnectorFacadeFactory.getInstance().newInstance(config);
        printSupportedOperations(facade);

        facade.validate();
        facade.test();

        // Now use connector
        findAllUsers(facade);
        findSpecificUsers(facade);
        createUser(facade);
        updateUser(facade);
        authenticateUser(facade);
    }
    
    private static void logUsecaseStart(String name){
        System.out.println("------------------------" + name + "-----------------------------------------------");
    }

    private static void updateUser(ConnectorFacade facade) {
        logUsecaseStart("Update user");
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name("testUser"), AttributeBuilder
                .buildPassword("testPasswd".toCharArray()), AttributeBuilder.build("oraclePrivs", "CREATE SESSION")),
                null);
        // Update password , expire password and lock user
        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.buildPassword("newPassword"
                .toCharArray()), AttributeBuilder.buildEnabled(false), AttributeBuilder.buildPasswordExpired(true)),
                null);
        OperationOptions options = new OperationOptionsBuilder().setAttributesToGet("oracleRoles", "oraclePrivs",
                OperationalAttributes.PASSWORD_EXPIRED_NAME, OperationalAttributes.ENABLE_NAME).build();
        //Show user
        ConnectorObject obj = facade.getObject(ObjectClass.ACCOUNT, uid, options);
        System.out.println(obj);
        System.out.println(obj.getAttributeByName("oraclePrivs"));
        //Now add values to privileges
        facade.addAttributeValues(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build("oraclePrivs",
                "ALTER SESSION", "SELECT ANY TABLE","INSERT ANY TABLE")), null);
        //Show privileges
        obj = facade.getObject(ObjectClass.ACCOUNT, uid, options);
        System.out.println(obj.getAttributeByName("oraclePrivs"));
        //Remove one privilege
        facade.removeAttributeValues(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build("oraclePrivs",
                "SELECT ANY TABLE")), null);
        //And again show privileges
        obj = facade.getObject(ObjectClass.ACCOUNT, uid, options);
        System.out.println(obj.getAttributeByName("oraclePrivs"));
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        System.out.println();
    }

    private static void findSpecificUsers(ConnectorFacade facade) {
        logUsecaseStart("Find specific users");
        ResultsHandler printHandler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                System.out.print(obj.getUid().getUidValue() + " ");
                return true;
            }
        };
        System.out.println("Users starting on S");
        facade.search(ObjectClass.ACCOUNT, FilterBuilder.startsWith(new Name("S")), printHandler, null);
        System.out.println("Users starting on S and ending on R");
        facade.search(ObjectClass.ACCOUNT, FilterBuilder.and(FilterBuilder.startsWith(new Name("S")), FilterBuilder
                .endsWith(new Name("R"))), printHandler, null);
        System.out.println();
    }

    /**
     * @param configProps
     */
    private static void printConfigProperties(ConfigurationProperties configProps) {
        logUsecaseStart("Config properties ");
        System.out.println(configProps.getPropertyNames());
        System.out.println();
    }

    /**
     * @param facade
     */
    private static void authenticateUser(ConnectorFacade facade) {
        logUsecaseStart("Authenticate user");
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name("testUser"), AttributeBuilder
                .buildPassword("testPasswd".toCharArray()), AttributeBuilder.build("oraclePrivs", "CREATE SESSION")),
                null);
        facade.authenticate(ObjectClass.ACCOUNT, "TESTUSER", new GuardedString("testPasswd".toCharArray()), null);
        facade.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.buildPasswordExpired(true)),
                null);
        try {
            facade.authenticate(ObjectClass.ACCOUNT, "TESTUSER", new GuardedString("testPasswd".toCharArray()), null);
        } catch (PasswordExpiredException e) {
            System.out.println(e.getMessage());
        }
        facade.delete(ObjectClass.ACCOUNT, uid, null);
    }

    private static void createUser(ConnectorFacade facade) {
        logUsecaseStart("Creating user");
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name("testUser"), AttributeBuilder
                .buildPassword("testPasswd".toCharArray())), null);
        System.out.println("User created : " + uid.getUidValue());
        ConnectorObject object = facade.getObject(ObjectClass.ACCOUNT, uid, null);
        System.out.println("User attributes");
        System.out.println(object);
        try {
            facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name("testUser"), AttributeBuilder
                    .buildPassword("testPasswd".toCharArray())), null);
        } catch (AlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        System.out.println();
    }

    /**
     * @param facade
     */
    private static void findAllUsers(ConnectorFacade facade) {
        logUsecaseStart("Find all users");
        final int[] count = new int[1];
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                System.out.print(obj.getUid().getUidValue() + " ");
                count[0]++;
                return true;
            }
        };
        facade.search(ObjectClass.ACCOUNT, null, handler, null);
        System.out.println("Users count : " + count[0]);
        System.out.println();
    }

    /**
     * @param facade
     */
    private static void printSupportedOperations(ConnectorFacade facade) {
        logUsecaseStart("Supported operations");
        System.out.println(facade.getSupportedOperations());
        System.out.println();

    }

    /**
     * @param configProps
     */
    private static void setConnectProperties(ConfigurationProperties configProps) {
        PropertyBag testProps = TestHelpers.getProperties(OracleConnector.class);
        String user = testProps.getStringProperty("thin.user");
        String passwordString = testProps.getStringProperty("thin.password");
        GuardedString password = new GuardedString(passwordString.toCharArray());
        String driver = OracleSpecifics.THIN_DRIVER;
        String host = testProps.getStringProperty("thin.host");
        String port = testProps.getProperty("thin.port", String.class, "1524");
        String database = testProps.getStringProperty("thin.database");

        configProps.setPropertyValue("host", host);
        configProps.setPropertyValue("port", port);
        configProps.setPropertyValue("driver", driver);
        configProps.setPropertyValue("database", database);
        configProps.setPropertyValue("user", user);
        configProps.setPropertyValue("password", password);
    }

    private static void printAvailableConnectors(List<ConnectorInfo> connectorInfos) {
        logUsecaseStart("Available connectors : ");
        for (ConnectorInfo info : connectorInfos) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("Display name : ").append(info.getConnectorDisplayName());
            buffer.append("|Key : ").append(info.getConnectorKey());
            System.out.println(buffer.toString());
        }
        System.out.println();
    }

    private static ConnectorKey findOracleConnectorKey(List<ConnectorInfo> connectorInfos) {
        for (ConnectorInfo info : connectorInfos) {
            if ("org.identityconnectors.oracle.OracleConnector".equals(info.getConnectorKey().getConnectorName())) {
                return info.getConnectorKey();
            }
        }
        throw new IllegalStateException("No oracle connector available");
    }

    private static URL[] findConnectorsJar(String[] args) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (String jar : args) {
            File file = new File(jar);
            if (!file.exists()) {
                throw new RuntimeException("Jar does not exist");
            }
            URL url = file.toURI().toURL();
            urls.add(url);
        }
        return urls.toArray(new URL[urls.size()]);
    }

}
