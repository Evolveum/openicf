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
package org.identityconnectors.solaris;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.Configuration;

public abstract class AbstractOp {
    private SolarisConfiguration _configuration;
    private SolarisConnection _connection;
    private Log _log;
    
    public AbstractOp(Configuration config, SolarisConnection connection, Log log) {
        if (config instanceof SolarisConfiguration) {
            _configuration = (SolarisConfiguration) config;
        } else {
            throw new IllegalArgumentException("AbstractOp's constructor should be passed Solaris Configuration only");
        }
        if (connection != null) {
            _connection = connection;
        } else {
            throw new IllegalArgumentException("Connection should not be null.");
        }
        _log = log;
    }

    public Log getLog() {
        return _log;
    }

    protected SolarisConfiguration getConfiguration() {
        return _configuration;
    }
    
    protected SolarisConnection getConnection() {
        return _connection;
    }
}
