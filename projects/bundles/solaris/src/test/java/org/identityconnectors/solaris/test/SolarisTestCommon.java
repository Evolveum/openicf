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

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;

/**
 * 
 * Create a solaris test configuration (for unit tests only)
 * 
 */
public class SolarisTestCommon {

    private static final PropertyBag testProps = TestHelpers.getProperties(SolarisConnector.class);

    /**
     * create a new solaris connector and initialize it with the given
     * configuration
     * 
     * @param config
     *            the configuration to be used.
     */
    public static SolarisConnector createConnector() {
        SolarisConnector conn = new SolarisConnector();
        conn.init(createConfiguration());
        return conn;
    }

    private static String getStringProperty(String name) {
        return getProperty(name, String.class);
    }

    public static <T> T getProperty(String name, Class<T> type) {
        T value = testProps.getProperty(name, type);
        return value;
    }

    public static SolarisConfiguration createConfiguration() {
        // save configuration
        SolarisConfiguration config = new SolarisConfiguration();

        config.setHost(getStringProperty("host"));
        config.setPort(Integer.valueOf(getProperty("port", Integer.class)));
        config.setConnectionType(getStringProperty("connectionType"));

        // login user credentials
        config.setLoginUser(getStringProperty("user"));
        
        final GuardedString password = getProperty("pass", GuardedString.class);
        config.setPassword(password);
        
        config.setLoginShellPrompt(getStringProperty("loginShellPrompt"));
        
        // root user credentials
        String propName = "credentials";
        if (isPropertyDefined(propName, GuardedString.class)) {
            config.setCredentials(getProperty(propName, GuardedString.class));
        }

        propName = "rootUser";
        if (isStringPropertyDefined(propName)) {
            config.setRootUser(getStringProperty(propName));
        }
        
        propName = "rootShellPrompt";
        if (isStringPropertyDefined(propName)) {
            config.setRootShellPrompt(getStringProperty(propName));
        }
        
        propName = "systemDatabaseType";
        if (isStringPropertyDefined(propName)) {
            config.setSystemDatabaseType(getStringProperty(propName));
        }
        
        propName = "nisBuildDirectory";
        if (isStringPropertyDefined(propName)) {
            config.setNisBuildDirectory(getStringProperty(propName));
        }
        
        propName = "nisPwdDir";
        if (isStringPropertyDefined(propName)) {
            config.setNisPwdDir(getStringProperty(propName));
        }
        
        propName = "nisShadowPasswordSupport";
        if (isPropertyDefined(propName, Boolean.class)) {
            config.setNisShadowPasswordSupport(getProperty(propName, Boolean.class));
        }
        
        propName = "defaultPrimaryGroup";
        if (isStringPropertyDefined(propName)) {
            config.setDefaultPrimaryGroup(getStringProperty(propName));
        }

        propName = "loginShell";
        if (isStringPropertyDefined(propName)) {
            config.setLoginShell(getStringProperty(propName));
        }

        propName = "homeBaseDirectory";
        if (isStringPropertyDefined(propName)) {
            config.setHomeBaseDirectory(getStringProperty(propName));
        }

        return config;
    }

    private static boolean isStringPropertyDefined(String propName) {
        return isPropertyDefined(propName, String.class);
    }
    
    private static <T> boolean isPropertyDefined(String propName, Class<T> type) {
        try {
            getProperty(propName, type);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** for simulating API calls */
    public static ConnectorFacade createConnectorFacade(SolarisConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(SolarisConnector.class, conf);
        return factory.newInstance(apiCfg);
    }

    public static SolarisConnection getSolarisConn() {
        SolarisConfiguration config = createConfiguration();
        SolarisConnection conn = new SolarisConnection(config);
        return conn;
    }
}