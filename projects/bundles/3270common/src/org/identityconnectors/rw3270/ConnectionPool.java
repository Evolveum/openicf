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

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * A pool of RW3270Connections. Multiple subpools are maintained, each specified by a key.
 * Each subpool maintains a set of RW3270 connections, which may
 * 
 * @author gh202029
 *
 */
public class ConnectionPool extends GenericKeyedObjectPool {
    private PoolableConnectionFactory       _factory;
    
    @Override
    public void close() throws Exception {
        _factory.destroy();
        super.close();
    }

    public ConnectionPool(PoolableConnectionConfiguration config) throws Exception {
        _factory = new PoolableConnectionFactory(config);
        setFactory(_factory);

        _factory.resetObjectCount();
        
        
        // each connection in the pool is represented by a pair of
        //      poolName, userName
        String[] poolNames = config.getPoolNames();
        String[] userNames = config.getUserNames();
        for (int i=0; i<userNames.length; i++) {
            addObject(poolNames[i]);
        }
        
        setWhenExhaustedAction(WHEN_EXHAUSTED_BLOCK);
        setMaxIdle(-1);
        setMaxActive(-1);
        setTimeBetweenEvictionRunsMillis(config.getEvictionInterval());
    }
}
