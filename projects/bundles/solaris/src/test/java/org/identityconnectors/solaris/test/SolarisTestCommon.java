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
        Assertions.nullCheck(value, name);
        return value;
    }

    public static SolarisConfiguration createConfiguration() {
        // names of properties in the property file (build.groovy)
        final String propHost = "host";
        final String propLoginPassword = "pass";
        final String propLoginUser = "user";
        final String propPort = "port";
        final String propConnectionType = "connectionType";
        final String propRootShellPrompt = "rootShellPrompt";

        // save configuration
        SolarisConfiguration config = new SolarisConfiguration();

        config.setHost(getStringProperty(propHost));

        final String password = getStringProperty(propLoginPassword);
        config.setPassword(new GuardedString(password.toCharArray()));

        config.setLoginUser(getStringProperty(propLoginUser));

        config.setPort(Integer.valueOf(getProperty(propPort, Integer.class)));

        config.setConnectionType(getStringProperty(propConnectionType));

        config.setLoginShellPrompt(getStringProperty(propRootShellPrompt));

        return config;
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