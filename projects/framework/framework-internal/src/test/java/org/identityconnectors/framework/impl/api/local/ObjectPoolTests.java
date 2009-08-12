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
package org.identityconnectors.framework.impl.api.local;

import junit.framework.Assert;

import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.impl.api.local.ObjectPool;
import org.identityconnectors.framework.impl.api.local.ObjectPoolHandler;
import org.identityconnectors.framework.impl.api.local.ObjectPool.Statistics;
import org.junit.Test;


public class ObjectPoolTests {

    private class MyTestConnection  {
        
        private boolean _isGood = true;
        
        public void test() {
            if (!_isGood) {
                throw new ConnectorException("Connection is bad");
            }
        }
        
        public void dispose() {
            _isGood = false;
        }
        
        public boolean isGood() {
            return _isGood;
        }
    }
    
    private class MyTestConnectionFactory implements ObjectPoolHandler<MyTestConnection> {
        private boolean _createBadConnection = false;
        private int _totalCreatedConnections = 0;
        
        public MyTestConnection newObject() {
            _totalCreatedConnections++;
            MyTestConnection rv = new MyTestConnection();
            if (_createBadConnection) {
                rv.dispose();
            }
            return rv;
        }
        public void testObject(MyTestConnection object) {
            object.test();
        }
        public void disposeObject(MyTestConnection object) {
            object.dispose();
        }
        
        public int getTotalCreatedConnections() {
            return _totalCreatedConnections;
        }
        
        
        public void setCreateBadConnection(boolean v) {
            _createBadConnection = v;
        }

    }
    
    private class MyTestThread extends Thread {
        private final ObjectPool<MyTestConnection> _pool;
        private final int _numIterations;
        private Exception _exception;
        public MyTestThread(ObjectPool<MyTestConnection> pool,
                int numIterations) {
            _pool = pool;
            _numIterations = numIterations;
        }
        public void run() {
            try {
                for ( int i = 0; i < _numIterations; i++ ) {
                    MyTestConnection con =
                        _pool.borrowObject();
                    Thread.sleep(300);
                    _pool.returnObject(con);
                }
            }
            catch (Exception e) {
                _exception = e;
            }
        }
        public void shutdown() throws Exception {
            join();
            if (_exception != null) {
                throw _exception;
            }
        }
        
    }
    
    @Test
    public void testWithManyThreads()
        throws Exception
    {
        final int NUM_ITERATIONS = 10;
        final int NUM_THREADS = 10;
        final int MAX_CONNECTIONS = NUM_THREADS-3; //make sure we get some waiting
        ObjectPoolConfiguration config = new ObjectPoolConfiguration();
        config.setMaxObjects(MAX_CONNECTIONS); 
        config.setMaxIdle(MAX_CONNECTIONS);
        config.setMinIdle(MAX_CONNECTIONS);
        config.setMinEvictableIdleTimeMillis(60*1000);
        config.setMaxWait(60*1000);
        MyTestConnectionFactory fact = new MyTestConnectionFactory();
        
        ObjectPool<MyTestConnection> pool = new ObjectPool<MyTestConnection>(fact,config);
        
        MyTestThread [] threads = new MyTestThread[NUM_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new MyTestThread(pool,NUM_ITERATIONS);
            threads[i].start();
        }
                
        for (MyTestThread thread : threads) {
            thread.shutdown();
        }
        
        //these should be the same since we never 
        //should have disposed anything
        Assert.assertEquals(MAX_CONNECTIONS, fact.getTotalCreatedConnections());
        Statistics stats = pool.getStatistics();
        Assert.assertEquals(0, stats.getNumActive());
        Assert.assertEquals(MAX_CONNECTIONS, stats.getNumIdle());
        
        pool.shutdown();
        stats = pool.getStatistics();
        Assert.assertEquals(0, stats.getNumActive());
        Assert.assertEquals(0, stats.getNumIdle());        
        
    }
    
    @Test
    public void testBadConnection()
    {
        final int MAX_CONNECTIONS = 3;
        ObjectPoolConfiguration config = new ObjectPoolConfiguration();
        config.setMaxObjects(MAX_CONNECTIONS); 
        config.setMaxIdle(MAX_CONNECTIONS);
        config.setMinIdle(MAX_CONNECTIONS);
        config.setMinEvictableIdleTimeMillis(60*1000);
        config.setMaxWait(60*1000);
        MyTestConnectionFactory fact = new MyTestConnectionFactory();
        
        ObjectPool<MyTestConnection> pool = new ObjectPool<MyTestConnection>(fact,config);
        
        //borrow first connection and return
        MyTestConnection conn = pool.borrowObject();
        Assert.assertEquals(1, fact.getTotalCreatedConnections());
        pool.returnObject(conn);
        Assert.assertEquals(1, fact.getTotalCreatedConnections());
        
        //re-borrow same connection and return
        conn = pool.borrowObject();
        Assert.assertEquals(1, fact.getTotalCreatedConnections());
        pool.returnObject(conn);
        Assert.assertEquals(1, fact.getTotalCreatedConnections());
        
        //dispose and make sure we get a new connection
        conn.dispose(); 
        conn = pool.borrowObject();
        Assert.assertEquals(2, fact.getTotalCreatedConnections());
        pool.returnObject(conn);
        Assert.assertEquals(2, fact.getTotalCreatedConnections());
    }
    
    @Test
    public void testIdleCleanup()
        throws Exception
    {
        ObjectPoolConfiguration config = new ObjectPoolConfiguration();
        config.setMaxObjects(3); 
        config.setMaxIdle(2);
        config.setMinIdle(1);
        config.setMinEvictableIdleTimeMillis(3000);
        config.setMaxWait(60*1000);
        MyTestConnectionFactory fact = new MyTestConnectionFactory();
        
        ObjectPool<MyTestConnection> pool = new ObjectPool<MyTestConnection>(fact,config);

        MyTestConnection conn1 = (MyTestConnection)pool.borrowObject();
        MyTestConnection conn2 = (MyTestConnection)pool.borrowObject();
        MyTestConnection conn3 = (MyTestConnection)pool.borrowObject();
        
        Assert.assertEquals(3, fact.getTotalCreatedConnections());
        pool.returnObject(conn1);
        Assert.assertEquals(1, pool.getStatistics().getNumIdle());
        pool.returnObject(conn2);
        Assert.assertEquals(2, pool.getStatistics().getNumIdle());
        pool.returnObject(conn3);
        Assert.assertEquals(2, pool.getStatistics().getNumIdle());
        Assert.assertEquals(false, conn1.isGood());
        Assert.assertEquals(true, conn2.isGood());
        Assert.assertEquals(true, conn3.isGood());
        Thread.sleep(config.getMinEvictableIdleTimeMillis()+1000);
        MyTestConnection conn4 = (MyTestConnection)pool.borrowObject();
        Assert.assertSame(conn3, conn4);
        Assert.assertEquals(false, conn1.isGood());
        Assert.assertEquals(false, conn2.isGood());
        Assert.assertEquals(true, conn3.isGood());
        Assert.assertEquals(true, conn4.isGood());
    }
    
    @Test
    public void testCreateBadConnection()
        throws Exception
    {
        MyTestConnectionFactory fact = new MyTestConnectionFactory();
        fact.setCreateBadConnection(true);
        
        ObjectPool<MyTestConnection> pool = new ObjectPool<MyTestConnection>(fact,new ObjectPoolConfiguration());
        try {
            pool.borrowObject();
            Assert.fail("expected exception");
        }
        catch (ConnectorException e) {
            Assert.assertEquals("Connection is bad", e.getMessage());
        }
    }
    
}
