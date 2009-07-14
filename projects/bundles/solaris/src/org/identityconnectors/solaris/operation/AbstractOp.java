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
package org.identityconnectors.solaris.operation;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.command.CommandBuilder;

public abstract class AbstractOp {
    private SolarisConfiguration _configuration;
    private SolarisConnection _connection;
    private Log _log;
    private CommandBuilder _cmdBuilder;
    
    public AbstractOp(SolarisConfiguration config, SolarisConnection connection, Log log) {
        _configuration = (SolarisConfiguration) config;
        
        Assertions.nullCheck(connection, "connection");
        _connection = connection;
        _log = log;
        
        _cmdBuilder = new CommandBuilder(_configuration);
    }

    protected final Log getLog() {
        return _log;
    }

    protected final SolarisConfiguration getConfiguration() {
        return _configuration;
    }
    
    protected final SolarisConnection getConnection() {
        return _connection;
    }
    
    protected final String executeCommand(String command) {
        return getConnection().executeCommand(command);
    }
    
    /** @return the command formatter */
    protected final CommandBuilder getCmdBuilder() {
        return _cmdBuilder;
    }
    
    protected final void addSudoStart() {
        SudoUtil.addSudoStart(getConfiguration(), getConnection());
    }

    protected final void addSudoReset() {
        SudoUtil.addSudoReset(getConfiguration(), getConnection());
    }
}
