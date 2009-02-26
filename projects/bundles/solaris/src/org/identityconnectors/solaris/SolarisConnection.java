/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.solaris;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

import com.jcraft.jsch.JSchException;

import expect4j.Expect4j;
import expect4j.ExpectUtils;

/**
 * maps functionality of SSHConnection.java
 * @author David Adam
 *
 */
class SolarisConnection {
    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration _configuration;
    private final Log log = Log.getLog(SolarisConnection.class);
    private Expect4j _expect4j;

    /* *************** CONSTRUCTOR ****************** */
    public SolarisConnection(SolarisConfiguration configuration) {
        if (configuration == null) {
            throw new ConfigurationException(
                    "Cannot create a SolarisConnection on a null configuration.");
        }
        _configuration = configuration;
        
        // initialize EXPECT4J
        final GuardedString password = _configuration.getPassword();
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                try {
                    _expect4j = ExpectUtils.SSH(_configuration.getHostNameOrIpAddr(), _configuration.getUserName(), new String(clearChars), _configuration.getPort());
                } catch (Exception e) {
                    log.warn("Starting connection: Exception thrown (cause: invalid configuration, etc.)");
                    throw ConnectorException.wrap(e);
                }
            }
        });
    }

    /* *************** METHODS ****************** */
    /** once connection is disposed it won't be used at all. */
    void dispose() {
        log.info("dispose()");
        if (_expect4j != null){
            _expect4j.close();
        }
    }
    
    /**
     * Try to authenticate with the given configuration
     * If the test fails, an exception is thrown.
     * @param configuration the configuration that should be tested.
     * @throws Exception
     */
    static void test(SolarisConfiguration configuration) throws Exception {
        SolarisConnection connection = new SolarisConnection(configuration);
    }
}
