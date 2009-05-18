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

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.test.common.TestHelpers;

/**
 * 
 * Create a solaris test configuration (for unit tests only)
 *
 */
public class SolarisTestCommon {
    
    /**
     * create a new solaris connector and initialize it with the given configuration
     * @param config the configuration to be used.
     */
    static SolarisConnector createConnector(SolarisConfiguration config) {
        SolarisConnector conn = new SolarisConnector();
        conn.init(config);
        
        return conn;
    }
    
    /**
     * set up a configuration with given property key/value pair. 
     * The setting is done via helper interface
     * 
     * @param propertyName
     * @param config
     * @param method
     * @return the new configuration with performed setup
     */
    private static SolarisConfiguration augmentConfiguration(
            String propertyName, SolarisConfiguration config,
            SetConfiguration method) {
        SolarisConfiguration configResult = new SolarisConfiguration(config);
        final String propValue = TestHelpers.getProperty(propertyName, null);
        String msg = "%s must be provided in build.groovy";
        Assert.assertNotNull(String.format(msg, propertyName), propValue);
        configResult = method.set(propValue, configResult);
        return configResult;
    }
    
    static SolarisConfiguration createConfiguration() {
        // names of properties in the property file (build.groovy)
        final String PROP_HOST = "host";
        final String PROP_SYSTEM_PASSWORD = "pass";
        final String PROP_SYSTEM_USER = "user";
        final String PROP_PORT = "port";
        final String PROP_CONN_TYPE = "connectionType";
        final String ROOT_SHELL_PROMPT = "rootShellPrompt";

        // save configuration
        SolarisConfiguration config = new SolarisConfiguration();

        config = augmentConfiguration(PROP_HOST, config,
                new SetConfiguration() {
                    public SolarisConfiguration set(String value,
                            SolarisConfiguration config) {
                        config.setHostNameOrIpAddr(value);
                        return config;
                    }
                });

        config = augmentConfiguration(PROP_SYSTEM_PASSWORD, config,
                new SetConfiguration() {
                    public SolarisConfiguration set(String value,
                            SolarisConfiguration config) {
                        config.setPassword(new GuardedString(value
                                .toCharArray()));
                        return config;
                    }
                });

        config = augmentConfiguration(PROP_SYSTEM_USER, config,
                new SetConfiguration() {
                    public SolarisConfiguration set(String value,
                            SolarisConfiguration config) {
                        config.setUserName(value);
                        return config;
                    }
                });

        config = augmentConfiguration(PROP_PORT, config,
                new SetConfiguration() {
                    public SolarisConfiguration set(String value,
                            SolarisConfiguration config) {
                        config.setPort(Integer.valueOf(value));
                        return config;
                    }
                });

        config = augmentConfiguration(PROP_CONN_TYPE, config,
                new SetConfiguration() {
                    public SolarisConfiguration set(String value,
                            SolarisConfiguration config) {
                        config.setConnectionType(value);
                        return config;
                    }
                });

        config = augmentConfiguration(ROOT_SHELL_PROMPT, config,
                new SetConfiguration() {
                    public SolarisConfiguration set(String value,
                            SolarisConfiguration config) {
                        config.setRootShellPrompt(value);
                        return config;
                    }
                });

        return config;
    }
    
    /** for simulating API calls */
    static ConnectorFacade createConnectorFacade(SolarisConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(SolarisConnector.class, conf);
        return factory.newInstance(apiCfg);
    }
}

interface SetConfiguration {
    public SolarisConfiguration set(String value, SolarisConfiguration config);
}