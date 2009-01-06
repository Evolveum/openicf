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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.vms.GuardedStringAccessor;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 
 * @author David Adam
 *
 */
public class SolarisConnection {
    /**
     * properties of SSH channel
     */
    private Session _session;
    private Channel _channel;
    private boolean connected = false;

//    TODO
//    /**
//     * Setup logging for the {@link SolarisConnection}.
//     */
//    private static final Log log = Log.getLog(SolarisConnection.class);

    /**
     * the configuration object from which this connection is created.
     */
    private SolarisConfiguration config;
    
    private InputStream _in;
    private OutputStream _out;

    /* *************** CONSTRUCTOR ****************** */
    public SolarisConnection(SolarisConfiguration config) {
        if (config == null) {
            throw new ConfigurationException(
                    "Cannot create a SolarisConnection on a null configuration.");
        }
        this.config = config;
        startConnection();
    }

    /* *************** METHODS ****************** */
    
    /**
     * Opens the connection and authenticates the user specified
     * in the {@link SolarisConfiguration}
     */
    private void startConnection() {
        if (!isConnected()) {
            try {
                _session = openSession();
                _channel = _session.openChannel("shell");
                _in = _channel.getInputStream();
                _out = _channel.getOutputStream();
                _channel.connect();
                setConnected(true);
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    /**
     * tests if connection exists.
     */
    public void test() {
        startConnection();
        dispose();
    }

    /**
     * Disconnect from SSH server. Just closes the streams and resets them to null.
     */
    public void dispose() {
        if (_channel != null) {
            _channel.disconnect();
        }
        if (_session != null) {
            _session.disconnect();
        }
        
        _in = null;
        _out = null;
        _session = null;
        _channel = null;
    }

    /* ************ AUXILIARY METHODS *************** */
    
    private Session openSession() throws JSchException, ConnectorException,
            IOException {

        // TODO this line makes the connection ignore the fingerprint
        // in production version fingerprint should be considered
        JSch.setConfig("StrictHostKeyChecking", "no");

        JSch jsch = new JSch();
        Session session = jsch.getSession(config.getUserName(), config
                .getHostNameOrIpAddr(), Integer.parseInt(config.getPort()));
        session.setPassword(getPassword(config));
        session.connect();
        return session;
    }

    private String getPassword(SolarisConfiguration config2) {
        GuardedString pass = config2.getPassword();
        GuardedStringAccessor gsa = new GuardedStringAccessor();
        pass.access(gsa);
        char[] cleanPasswd = gsa.getArray();
        return new String(cleanPasswd);
    }

    /* ***************** GET/SET *********************** */
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
