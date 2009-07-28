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
package org.identityconnectors.test.framework.api;


import java.net.InetAddress;
import java.net.URL;
import java.util.List;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
import org.identityconnectors.framework.server.ConnectorServer;


public class RemoteConnectorInfoManagerClearTests extends ConnectorInfoManagerTests {

    private static ConnectorServer _server;
    
    /**
     * To be overridden by subclasses to get different ConnectorInfoManagers
     * @return
     * @throws Exception
     */
    @Override
    protected ConnectorInfoManager getConnectorInfoManager() throws Exception {
        List<URL> urls = getTestBundles();
        
        final int PORT = 8759;
        
        _server = ConnectorServer.newInstance();
        _server.setKeyHash(SecurityUtil.computeBase64SHA1Hash("changeit".toCharArray()));
        _server.setBundleURLs(urls);
        _server.setPort(PORT);
        _server.setIfAddress(InetAddress.getByName("127.0.0.1"));
        _server.start();
        ConnectorInfoManagerFactory fact = ConnectorInfoManagerFactory.getInstance();
        
        RemoteFrameworkConnectionInfo connInfo = new
        RemoteFrameworkConnectionInfo("127.0.0.1",PORT,new GuardedString("changeit".toCharArray()));
        
        ConnectorInfoManager manager = fact.getRemoteManager(connInfo);
        
        return manager;
    }
    
    @Override
    protected void shutdownConnnectorInfoManager() {
        if (_server != null) {
            _server.stop();
            _server = null;
        }
    }
    
    
}
