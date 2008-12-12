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
package org.identityconnectors.rw3270;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;


public class PoolableConnectionFactory implements KeyedPoolableObjectFactory {
    private static final ScriptExecutorFactory  factory = ScriptExecutorFactory.newInstance("GROOVY");

    private PoolableConnectionConfiguration     _config;
    private ScriptExecutor                      _connectScriptExecutor;
    private ScriptExecutor                      _disconnectScriptExecutor;
    private int                                 _index;
    private Constructor<RW3270Connection>       _constructor;
    private List<Exception>                     _exceptions;
    
    private List<ConnectionInfo>                _connectionInfoList;
    
    //TODO: need to add a background thread that logs out unused connections

    public PoolableConnectionFactory(PoolableConnectionConfiguration     config) {
        try {
            _exceptions = new LinkedList<Exception>();
            _index = 0;
            _config = config;
            _constructor = (Constructor<RW3270Connection>)Class.forName(config.getConnectionClassName()).getConstructor(PoolableConnectionConfiguration.class);
            _connectScriptExecutor = factory.newScriptExecutor(getClass().getClassLoader(), config.getConnectScript(), true);
            _disconnectScriptExecutor = factory.newScriptExecutor(getClass().getClassLoader(), config.getDisconnectScript(), true);
            _connectionInfoList = new LinkedList<ConnectionInfo>();
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    void destroy() {
        for (ConnectionInfo info : _connectionInfoList) 
            info.getConnection().dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void activateObject(Object key, Object obj) throws Exception {
        try {
            ConnectionInfo info = (ConnectionInfo)obj;
            if (!info.activated) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("SHORT_WAIT", 15000);
                arguments.put("USERNAME", info.userName);
                arguments.put("PASSWORD", info.password);
                arguments.put("connection", info.connection);
                _connectScriptExecutor.execute(arguments);
                info.activated = true;
            }
        } catch (Exception e) {
            _exceptions.add(e);
            e.printStackTrace();
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroyObject(Object key, Object obj) throws Exception {
        ConnectionInfo info = (ConnectionInfo)obj;
        info.connection.dispose();
    }

    public void resetObjectCount() {
        _index = 0;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object makeObject(Object key) throws Exception {
        if (_index>=_config.getUserNames().length) {
            throw new ConnectorException(_config.getConnectorMessages().format("TooManyConnections", "too many connections requested:{0}", _exceptions.toString()));
        }
        ConnectionInfo info = new ConnectionInfo(
                _constructor.newInstance(_config),
                _config.getUserNames()[_index],
                _config.getPasswords()[_index]);
        _index++;
        _connectionInfoList.add(info);
        return info;
    }

    /**
     * {@inheritDoc}
     */
    public void passivateObject(Object key, Object obj) throws Exception {
        ConnectionInfo info = (ConnectionInfo)obj;
        // we skip deactivating in the first round, since we don't really
        // activate it when first created
        //
        info.lastUsed = new Date();
        if (false)
            logoutUser(info);
    }

    private void logoutUser(ConnectionInfo info) throws Exception {
        if (info.activated) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("SHORT_WAIT", 5000);
            arguments.put("USERNAME", info.userName);
            arguments.put("PASSWORD", info.password);
            arguments.put("connection", info.connection);
            _disconnectScriptExecutor.execute(arguments);
            info.activated = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean validateObject(Object key, Object obj) {
        return obj instanceof ConnectionInfo;
    }
    
    public static class ConnectionInfo {
        RW3270Connection        connection;
        String                  userName;
        GuardedString           password;
        boolean                 activated;
        Date                    lastUsed;
        
        public ConnectionInfo(RW3270Connection connection, String userName, GuardedString password) {
            this.connection = connection;
            this.userName = userName;
            this.password= password;
        }
        
        public RW3270Connection getConnection() {
            return connection;
        }
    }
}
