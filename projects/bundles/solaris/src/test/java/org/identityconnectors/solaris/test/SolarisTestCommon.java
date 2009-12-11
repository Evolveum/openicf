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
        return testProps.getStringProperty(name);
    }

    private static <T> T getProperty(String name, Class<T> type) {
        return testProps.getProperty(name, type);
    }

    public static SolarisConfiguration createConfiguration() {
        // names of properties in the property file (build.groovy)
        final String PROP_HOST = "host";
        final String PROP_SYSTEM_PASSWORD = "pass";
        final String PROP_SYSTEM_USER = "user";
        final String PROP_PORT = "port";
        final String PROP_CONN_TYPE = "connectionType";
        final String ROOT_SHELL_PROMPT = "rootShellPrompt";

        // save configuration
        SolarisConfiguration config = new SolarisConfiguration();

        config.setHost(getStringProperty(PROP_HOST));

        final String password = getStringProperty(PROP_SYSTEM_PASSWORD);
        config.setCredentials(new GuardedString(password.toCharArray()));

        config.setRootUser(getStringProperty(PROP_SYSTEM_USER));

        config.setPort(Integer.valueOf(getProperty(PROP_PORT, Integer.class)));

        config.setConnectionType(getStringProperty(PROP_CONN_TYPE));

        config.setRootShellPrompt(getStringProperty(ROOT_SHELL_PROMPT));

        return config;
    }

    /** for simulating API calls */
    public static ConnectorFacade createConnectorFacade(SolarisConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(SolarisConnector.class, conf);
        return factory.newInstance(apiCfg);
    }

    public static SolarisConnection getSolarisConn() {
        SolarisConfiguration config = SolarisTestCommon.createConfiguration();
        SolarisConnection conn = new SolarisConnection(config);
        return conn;
    }
}