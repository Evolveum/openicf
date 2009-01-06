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
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.TestOp;

/**
 * @author david
 * 
 */
@ConnectorClass(displayNameKey = "Solaris", configurationClass = SolarisConfiguration.class)
public class SolarisConnector implements PoolableConnector, AuthenticateOp,
        SchemaOp, TestOp {

    /**
     * Setup logging for the {@link DatabaseTableConnector}.
     */
    private Log log = Log.getLog(SolarisConnector.class);

    private SolarisConnection _connection;

    private SolarisConfiguration _configuration;

    /*
     * (non-Javadoc)
     * 
     * @see org.identityconnectors.framework.spi.Connector#getConfiguration()
     */
    public Configuration getConfiguration() {
        return _configuration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.identityconnectors.framework.spi.Connector#init(org.identityconnectors
     * .framework.spi.Configuration)
     */
    public void init(Configuration cfg) {
        _configuration = (SolarisConfiguration) cfg;
        _connection = new SolarisConnection(_configuration);
    }

    /*
     * @see org.identityconnectors.framework.spi.PoolableConnector#checkAlive()
     */
    public void checkAlive() {
        _connection.test();
    }

    SolarisConnection getConnection() {
        return _connection;
    }

    /**
     * Disposes of {@link SolarisConnector}'s resources
     * 
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    public void dispose() {
        _configuration = null;
        if (_connection != null) {
            _connection.dispose();
            _connection = null;
        }
    }

    /* *********************** OPERATIONS ************************** */
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate
     * (org.identityconnectors.framework.common.objects.ObjectClass,
     * java.lang.String, org.identityconnectors.common.security.GuardedString,
     * org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

    public Schema schema() {
        // TODO Auto-generated method stub
        return null;
    }

    public void test() {
        if (_configuration == null) {
            throw new IllegalStateException(
                    "Solaris connector has not been initialized");
        }
        _configuration.validate();

        if (_connection == null) {
            throw new IllegalStateException(
                    "Solaris connector does not have a connection");
        }
        _connection.test();
    }

}
