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

package org.identityconnectors.racf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.RW3270BaseConnection;
import org.identityconnectors.rw3270.RW3270Connection;
import org.identityconnectors.rw3270.RW3270ConnectionFactory;

/**
 * This class represents a pool of connections to a Racf host.
 * <p>
 * While it's theoretically possible to have multiple pools associated with a single
 * host, we are not going to try to support that here; the application could distinguish
 * between pools, but we can't at this low level.
 * 
 * @author hetrick
 */
public class ConnectionPool {
    private boolean                 _reapable;
    
    /**
     * Use a java.util.logging Logger, since we need a way for tests to know what
     * the ConnectionPool has been doing, but the framework Log provides no access
     * to the log contents.
     */
    private Logger                  _log = Logger.getLogger(ConnectionPool.class.getName());
    
    /**
     * The ActiveConnectionReaper runs periodically to log out connections before they
     * are shut down by a terminal server or the racf mainframe. The ConnectionPool will
     * log the connection back in before handing it back out to the RacfConnector.
     */
    private static class ActiveConnectionReaper extends Thread {
        private Logger              _log = Logger.getLogger(ConnectionPool.class.getName());

        public void run() {
            _log.info("Starting ConnectionPool reaper");
            while (true) {
                try {
                    // Once/minute
                    //
                    Thread.sleep(60000);
                } catch (InterruptedException e) { }
                
                // Reap the connections for each of the currently existing pools
                //
                for (Map.Entry<String, ConnectionPool> pool : ConnectionPool._pools.entrySet().toArray(new Map.Entry[0]))
                    reapConnections(pool);
            }
        }

        private void reapConnections(Map.Entry<String, ConnectionPool> poolEntry) {
            long expire = System.nanoTime()-poolEntry.getValue()._reaperMaximumIdle*1000000;
            ConnectionPool pool = poolEntry.getValue();
            
            // Go through the current set of active connections
            //
            for (QueueEntry entry : pool._activeConnections.toArray(new QueueEntry[0])) {
                if (entry._timeReturned<expire) {
                    if (pool._activeConnections.remove(entry)) {
                        // By using remove, we only get here if no other thread took the
                        // connection for real use
                        //
                        try {
                            _log.info("reaping connection for "+entry._connection.getConfiguration().getUserName()+" on "+pool._currentConfiguration.getHostNameOrIpAddr());
                            entry._connection.logoutUser();
                        } catch (Exception e) {
                            _log.severe("failed to logout connection in reaper: userName '"+entry._connection.getConfiguration().getUserName()+"':"+e);
                        }
                        entry._connection.dispose();
                        entry._connection = null;
                        pool._inactiveConnections.add(entry);
                    }
                }
            }
            // Andrei suggests this as a good point to reap the entire pool
            //
            synchronized (_pools) {
                // First, make sure the pool hasn't been replaced while we were looking.
                // We want to be sure we remove *this* pool entry, not a replacement
                // that snuck in.
                //
                if (_pools.get(poolEntry.getKey())==pool && pool._reapable) {
                    if (pool._count==pool._inactiveConnections.size()) {
                        // All connections are in the inactive pool (or bad connection pool)
                        //
                        _pools.remove(poolEntry.getKey());
                        _log.info("reaping pool for "+pool._currentConfiguration.getHostNameOrIpAddr());
                    }
                }
            }
        }
    };

    private static class QueueEntry {
        public QueueEntry(int index) {
            _index = index;
        }
        public long                             _timeReturned;
        public int                              _index;
        public RW3270BaseConnection             _connection;
    };

    /**
     * The _semaphore is used to enable/disable getConnection()
     */
    private Semaphore                           _semaphore;
    private RacfConfiguration                   _currentConfiguration;
    private long                                _reaperMaximumIdle;

    /**
     * The number of connections in the pool
     */
    private int                                 _count;

    private BlockingQueue<QueueEntry>           _activeConnections;
    private BlockingQueue<QueueEntry>           _badConnections;
    private BlockingQueue<QueueEntry>           _inactiveConnections;
    private Map<RW3270Connection, QueueEntry>   _inUseConnections;

    private static Map<String, ConnectionPool>  _pools = new HashMap<String, ConnectionPool>();

    private static ActiveConnectionReaper       _reaper = new ActiveConnectionReaper();
    static {
        _reaper.setDaemon(true);
        _reaper.start();
    }

    /**
     * Get the ConnectionPool associated with the specified configuration.
     * <p>
     * If there is no pool associated with the host in the configuration,
     * create one, with all connections logged out.
     * <p>
     * If there is a pool associated with the host in the configuration,
     * but any of the pertinent parameters have changed (port, usernames, passwords),
     * shut down the pool, logging out all connections, and create a new pool.
     * <p>
     * If the pertinent parameters of the configuration associated with the
     * hostname are unchanged, return the existing pool.
     * 
     * @param configuration -- the RacfConfiguration with which the pool is associated
     * @return
     */
    public static ConnectionPool getConnectionPool(RacfConfiguration configuration) {
        return getConnectionPool(configuration, true);
    }
    
    static ConnectionPool getConnectionPool(RacfConfiguration configuration, boolean reapable) {
        synchronized (_pools) {
            if (configuration.getUserNames()==null || configuration.getUserNames().length==0)
                throw new IllegalArgumentException(configuration.getMessage(RacfMessages.USERNAMES_NULL));

            Logger log = Logger.getLogger(ConnectionPool.class.getName());
            ConnectionPool pool = _pools.get(configuration.getHostNameOrIpAddr());
            if (pool==null) {
                log.info("creating new pool for "+configuration.getHostNameOrIpAddr());
                pool = new ConnectionPool(configuration);
                _pools.put(configuration.getHostNameOrIpAddr(), pool);
            } else if (changed(pool._currentConfiguration, configuration)) {
                log.info("replacing pool for "+configuration.getHostNameOrIpAddr());

                // Shut down the existing pool for this host
                //
                pool.closeAllConnections();

                pool = new ConnectionPool(configuration);
                _pools.put(configuration.getHostNameOrIpAddr(), pool);
            }
            pool._reapable = reapable;
            return pool;
        }
    }

    /**
     * Get a connection from the pool
     * @param configuration
     * @return
     */
    public static RW3270Connection getConnectionFromPool(RacfConfiguration configuration) {
        ConnectionPool pool = getConnectionPool(configuration, false);
        RW3270Connection connection = pool.getConnection();
        pool._reapable = true;
        return connection;
    }
    
    /**
     * Check if any of the RW3270 parameters have changed:
     * <ul>
     *      <li>host name</li>
     *      <li>host port</li>
     *      <li>user names</li>
     *      <li>passwords</li>
     *      <li>login script</li>
     *      <li>logoff script</li>
     * </ul>
     * @param configuration
     * @return
     */
    private static boolean changed(RacfConfiguration oldConfiguration, RacfConfiguration newConfiguration) {
        if (!oldConfiguration.getHostNameOrIpAddr().equals(newConfiguration.getHostNameOrIpAddr()))
            return true;
        if (!oldConfiguration.getHostTelnetPortNumber().equals(newConfiguration.getHostTelnetPortNumber()))
            return true;
        if (oldConfiguration.getUserNames().length!=newConfiguration.getUserNames().length)
            return true;
        if (oldConfiguration.getPasswords().length!=newConfiguration.getPasswords().length)
            return true;
        if (!Arrays.asList(oldConfiguration.getUserNames()).equals(Arrays.asList(newConfiguration.getUserNames())))
            return true;
        if (!Arrays.asList(oldConfiguration.getPasswords()).equals(Arrays.asList(newConfiguration.getPasswords())))
            return true;
        if (!oldConfiguration.getConnectScript().equals(newConfiguration.getConnectScript()))
            return true;
        if (!oldConfiguration.getDisconnectScript().equals(newConfiguration.getDisconnectScript()))
            return true;
        //The pool must be re-initialized, when the other connection implementation is selected
        if (!oldConfiguration.getConnectionClassName().equals(newConfiguration.getConnectionClassName()))
            return true;
        return false;
    }

    private ConnectionPool(RacfConfiguration configuration) {
        _currentConfiguration = configuration;
        _semaphore = new Semaphore(1);

        // Initialize the queues
        //
        _activeConnections = new LinkedBlockingQueue<QueueEntry>();
        _badConnections = new LinkedBlockingQueue<QueueEntry>();
        _inactiveConnections = new LinkedBlockingQueue<QueueEntry>();
        _inUseConnections = new ConcurrentHashMap<RW3270Connection, QueueEntry>();
        _count = 0;

        _reaperMaximumIdle = _currentConfiguration.getReaperMaximumIdleTime()*1000; 

        // Create an inactiveConnection for each entry in _currentConfiguration
        //
        String[] userNames = _currentConfiguration.getUserNames();
        for (int i=0; i<userNames.length; i++) {
            QueueEntry entry = new QueueEntry(i);
            _log.info("creating inactive connection for "+_currentConfiguration.getUserNames()[i]+" on "+_currentConfiguration.getHostNameOrIpAddr());
            _inactiveConnections.add(entry);
            _count++;
        }
    }

    public synchronized boolean isEmpty() {
        return _count==0;
    }

    boolean isReapable() {
        return _reapable;
    }
    
    void setReapable(boolean reapable) {
        _reapable = reapable;
    }

    private RW3270Connection getConnection(boolean wait) {
        try {
            QueueEntry entry = findUnusedConnection(wait);
            if (entry!=null) {
                _inUseConnections.put(entry._connection, entry);
                return entry._connection;
            } else {
                return null;
            }
        } catch (Exception ie) {
            throw ConnectorException.wrap(ie);
        }
    }

    private RW3270Connection getConnection() {
        return getConnection(false);
    }

    private QueueEntry findUnusedConnection(boolean inTest) throws InterruptedException {
        // Wait for pool to be available to getConnection().
        //
        acquireSemaphore(inTest);
        
        // Are there any connections in the pool?
        //
        if (isEmpty()) {
            releaseSemaphore(inTest);
            throw new ConnectorException(_currentConfiguration.getMessage(RacfMessages.EMPTY_POOL));
        }

        // If there is an available activeConnection, use it
        //
        QueueEntry entry = _activeConnections.poll();
        if (entry!=null) {
            releaseSemaphore(inTest);
            // Fall through into return code
            //
            _log.info("retrieved active connection for "+entry._connection.getConfiguration().getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
        } else {
            // If there is an available inactiveConnection, use it
            //
            entry = _inactiveConnections.poll();
            if (entry!=null) {
                releaseSemaphore(inTest);
                try {
                    RW3270ConnectionFactory factory = new RW3270ConnectionFactory(_currentConfiguration.getRW3270Configuration(entry._index));
                    entry._connection = (RW3270BaseConnection)factory.newConnection();
                    entry._connection.loginUser();
                    _log.info("activated connection for "+entry._connection.getConfiguration().getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
                } catch (Exception e) {
                    if (entry._connection!=null)
                        entry._connection.dispose();
                    _log.log(Level.SEVERE, "failed to create connection: userName '"+_currentConfiguration.getRW3270Configuration(entry._index).getUserName()+"'", e);
                    // If we got an error activating the connection
                    // ignore it, and try again
                    //
                    returnBrokenEntry(entry);
                    if (inTest)
                        throw ConnectorException.wrap(e);
                    else
                        entry = findUnusedConnection(true);
                }
            } else {
                // Wait for an activeConnection to become available
                //
                if (!inTest) {
                    _log.info("waiting for active connection on "+_currentConfiguration.getHostNameOrIpAddr());
                    entry = _activeConnections.take();
                    _log.info("waited for active connection for "+entry._connection.getConfiguration().getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
                }
                releaseSemaphore(inTest);
            }
        }
        return entry;
    }

    private void releaseSemaphore(boolean inTest) {
        if (!inTest)
            _semaphore.release();
    }

    private void acquireSemaphore(boolean inTest) throws InterruptedException {
        if (!inTest)
            _semaphore.acquire();
    }

    /**
     * Return a RacfConnection to the ConnectionPool.
     * 
     * @param connection
     */
    public void returnConnection(RW3270Connection connection) {
        QueueEntry entry = _inUseConnections.remove(connection);
        if (entry==null) {
            throw new ConnectorException(_currentConfiguration.getMessage(RacfMessages.BAD_CONN_ENTRY));
        } else {
            entry._timeReturned = System.nanoTime();
            _log.info("returned connection for "+entry._connection.getConfiguration().getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
            _activeConnections.add(entry);
        }
    }

    /**
     * Return a RacfConnection that is not working to the ConnectionPool.
     * 
     * @param connection
     */
    public void returnBrokenConnection(RW3270Connection connection) {
        QueueEntry entry = _inUseConnections.remove(connection);
        if (entry==null) {
            throw new ConnectorException(_currentConfiguration.getMessage(RacfMessages.BAD_CONN_ENTRY));
        } else {
            entry._timeReturned = System.nanoTime();
            entry._connection.dispose();
            entry._connection = null;
            //TODO: we may eventually want to see if we can restart bad connections
            // While IDM doesn't do this, we can try it.
            returnBrokenEntry(entry);
        }
    }

    private void returnBrokenEntry(QueueEntry entry) {
        _badConnections.add(entry);
        _log.info("returned bad connection for "+_currentConfiguration.getRW3270Configuration(entry._index).getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
        synchronized (this) {
            _count--;
        }
    }

    /**
     * Close all connections in ConnectionPool
     */
    public void closeAllConnections() {
        closeAllConnections(false);
    }

    /**
     * Close all connections in ConnectionPool
     */
    public void closeAllConnections(boolean leaveLocked) {
        try {
            _semaphore.acquire();
            
            // Need to wait for any inUse connections to be returned
            //
            int i = 0;
            while (!_inUseConnections.isEmpty()) {
                // Just to set a limit, make it two tries per connection
                //
                if (i++>_inUseConnections.size()*2)
                    throw new ConnectorException(_currentConfiguration.getMessage(RacfMessages.EMPTY_POOL));
                try {
                    // wait for the allowed timeout of a command
                    //
                    Thread.sleep(_currentConfiguration.getCommandTimeout());
                } catch (InterruptedException ie) {
                    // Just ignore this
                }
            }

            for (QueueEntry entry : _activeConnections.toArray(new QueueEntry[0])) {
                try {
                    _log.info("closing active connection for "+entry._connection.getConfiguration().getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
                    entry._connection.dispose(); //dispose will do an logout entry._connection.logoutUser(); 
                    _activeConnections.remove(entry);
                    _inactiveConnections.add(entry);
                } catch (Exception e) {
                    _activeConnections.remove(entry);
                    _badConnections.add(entry);
                    _log.severe("failed to logout connection in closeAllConnections: userName '"+entry._connection.getConfiguration().getUserName()+"':"+e);
                }
                entry._connection = null;
            }
        } catch (InterruptedException ie) {
            throw ConnectorException.wrap(ie);
        } finally {
            releaseSemaphore(leaveLocked);
        }
    }

    /**
     * Test all connections using _currentConfiguration
     */
    public void testAllConnections() {
        testAllConnections(_currentConfiguration);
    }

    /**
     * Test all connections using specified RacfConfiguration
     * @param configuration
     */
    public void testAllConnections(RacfConfiguration configuration) {
        try {
            closeAllConnections(true);

            // We want to use a Vector here, since it is thread-safe
            //
            final Vector<String> errors = new Vector<String>();

            final String[] userNames = configuration.getUserNames();
            Thread[] threads = new Thread[userNames.length];
            for (int i=0; i<userNames.length; i++) {
                final int index = i;
                threads[i] = new Thread() {
                    RW3270Connection connection = null;
                    public void run() {
                        //TODO: test only connections on this machine, or all?
                        try {
                            connection = getConnection(true);
                            _log.info("activated test connection for "+_currentConfiguration.getRW3270Configuration(index).getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
                            if (connection!=null) {
                                _log.info("shut down test connection for "+_currentConfiguration.getRW3270Configuration(index).getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
                                returnConnection(connection);
                            }
                        } catch (Exception e) {
                            if (connection!=null)
                                returnBrokenConnection(connection);
                            _log.info("bad test connection for "+_currentConfiguration.getRW3270Configuration(index).getUserName()+" on "+_currentConfiguration.getHostNameOrIpAddr());
                            errors.add(_currentConfiguration.getMessage(RacfMessages.ERROR_CONNECTING, userNames[index], e.toString()));
                        }
                    }
                };
            }
            for (Thread thread : threads)
                thread.start();
            for (Thread thread : threads)
                thread.join();
            if (errors.size()>0) {
                StringBuffer buffer = new StringBuffer();
                for (String error : errors) {
                    buffer.append("\n"+error.toString());
                }
                throw new ConnectorException(buffer.substring(1));
            }
        } catch (Exception ie) {
            throw ConnectorException.wrap(ie);
        } finally {
            _semaphore.release();
            _reapable = true;
        }
    }
}
