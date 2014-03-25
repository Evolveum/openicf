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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */

package org.identityconnectors.framework.server.impl;

import java.net.ServerSocket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.impl.api.ConnectorInfoManagerFactoryImpl;
import org.identityconnectors.framework.impl.api.ManagedConnectorFacadeFactoryImpl;
import org.identityconnectors.framework.server.ConnectorServer;

public class ConnectorServerImpl extends ConnectorServer {

    private ConnectionListener listener;
    private CountDownLatch stopLatch;
    private Timer timer = null;
    private Long startDate = null;

    @Override
    public Long getStartTime() {
        return startDate;
    }

    @Override
    public boolean isStarted() {
        return listener != null;
    }

    @Override
    public void start() {
        if (isStarted()) {
            throw new IllegalStateException("Server is already running.");
        }
        if (getPort() == 0) {
            throw new IllegalStateException("Port must be set prior to starting server.");
        }
        if (getKeyHash() == null) {
            throw new IllegalStateException("Key hash must be set prior to starting server.");
        }
        // make sure we are configured properly
        final ConnectorInfoManagerFactoryImpl factory =
                (ConnectorInfoManagerFactoryImpl) ConnectorInfoManagerFactory.getInstance();
        factory.getLocalManager(getBundleURLs(), getBundleParentClassLoader());

        final ServerSocket socket = createServerSocket();
        final ConnectionListener listener = new ConnectionListener(this, socket);
        listener.start();
        stopLatch = new CountDownLatch(1);
        startDate = System.currentTimeMillis();
        this.listener = listener;

        // Create an inferred delegate that invokes methods for the timer.
        if (getMaxFacadeLifeTime() > 0) {
            FacadeDisposer statusChecker =
                    new FacadeDisposer(getMaxFacadeLifeTime(), TimeUnit.MINUTES);
            timer = new Timer();
            timer.scheduleAtFixedRate(statusChecker, new Date(), TimeUnit.MINUTES.toMillis(Math
                    .min(getMaxFacadeLifeTime(), 10)));
        }
    }

    private ServerSocket createServerSocket() {
        try {
            ServerSocketFactory factory;
            if (getUseSSL()) {
                factory = createSSLServerSocketFactory();
            } else {
                factory = ServerSocketFactory.getDefault();
            }
            final ServerSocket rv;
            if (getIfAddress() == null) {
                rv = factory.createServerSocket(getPort(), getMaxConnections());
            } else {
                rv = factory.createServerSocket(getPort(), getMaxConnections(), getIfAddress());
            }
            return rv;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    private ServerSocketFactory createSSLServerSocketFactory() throws Exception {
        KeyManager[] keyManagers = null;
        // convert empty to null
        if (getKeyManagers().size() > 0) {
            keyManagers = getKeyManagers().toArray(new KeyManager[getKeyManagers().size()]);
        }
        // the only way to get the default keystore is this way
        if (keyManagers == null) {
            return SSLServerSocketFactory.getDefault();
        } else {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, null, null);
            return context.getServerSocketFactory();
        }
    }

    @Override
    public void stop() {
        if (listener != null) {
            try {
                listener.shutdown();
            } finally {
                stopLatch.countDown();
            }
            stopLatch = null;
            startDate = null;
            listener = null;
        }
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
        ConnectorFacadeFactory.getManagedInstance().dispose();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        stopLatch.await();
    }

    private class FacadeDisposer extends TimerTask {
        private final long delay;
        private final TimeUnit unit;

        public FacadeDisposer(long time, TimeUnit unit) {
            this.delay = time;
            this.unit = unit;

        }

        @Override
        public void run() {
            logger.ok("Invoking Managed ConnectorFacade Disposer");
            ConnectorFacadeFactory factory = ConnectorFacadeFactory.getManagedInstance();
            if (factory instanceof ManagedConnectorFacadeFactoryImpl) {
                ((ManagedConnectorFacadeFactoryImpl) factory).evictIdle(delay, unit);
            }
        }
    }

}
