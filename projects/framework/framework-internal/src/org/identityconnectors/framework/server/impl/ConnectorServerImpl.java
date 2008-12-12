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
package org.identityconnectors.framework.server.impl;

import java.net.ServerSocket;
import java.net.URL;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.server.ConnectorServer;


public class ConnectorServerImpl extends ConnectorServer {

    private ConnectionListener _listener;
    
    @Override
    public boolean isStarted() {
        return _listener != null;
    }

    @Override
    public void start() {
        if ( isStarted() ) {
            throw new IllegalStateException("Server is already running.");
        }
        if ( getPort() == 0 ) {
            throw new IllegalStateException("Port must be set prior to starting server.");
        }
        if ( getBundleURLs().size() == 0 ) {
            throw new IllegalStateException("Bundle URLs must be set prior to starting server.");
        }
        if ( getKeyHash() == null ) {
            throw new IllegalStateException("Key hash must be set prior to starting server.");            
        }
        //make sure we are configured properly
        ConnectorInfoManagerFactory.getInstance().getLocalManager(
                getBundleURLs().toArray(new URL[0]));
        
        ServerSocket socket =
            createServerSocket();
        ConnectionListener listener = new ConnectionListener(this,socket);
        listener.setName("ConnectionListener");
        listener.start();
        _listener = listener;
    }
    
    private ServerSocket createServerSocket() {
        try {
            ServerSocketFactory factory;
            if (getUseSSL()) {
                factory = createSSLServerSocketFactory();
            }
            else {
                factory = ServerSocketFactory.getDefault();
            }
            ServerSocket rv;
            if ( getIfAddress() == null ) {
                rv = factory.createServerSocket(getPort(), 
                        getMaxConnections());
            }
            else {
                rv = factory.createServerSocket(getPort(), 
                        getMaxConnections(),
                        getIfAddress());
            }
            return rv;
        }
        catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    private ServerSocketFactory createSSLServerSocketFactory() 
        throws Exception {
        KeyManager [] keyManagers = null;
        //convert empty to null
        if ( getKeyManagers().size() > 0 ) {
            keyManagers = getKeyManagers().toArray(new KeyManager[0]);
        }
        //the only way to get the default keystore is this way
        if (keyManagers == null) {
            return SSLServerSocketFactory.getDefault();
        }
        else {
            SSLContext context = SSLContext.getInstance("TLS");        
            context.init(keyManagers, null, null);
            return context.getServerSocketFactory();
        }        
    }
    
    @Override
    public void stop() {
        if (_listener != null) {
            _listener.shutdown();
            _listener = null;
        }
        ConnectorFacadeFactory.getInstance().dispose();
    }

}
