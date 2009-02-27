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

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.Configuration;

public class SolarisConfiguration extends AbstractConfiguration {

    /**
     * basic configuration properties for SSH connection {@link SolarisConnection}
     */
    private String userName;
    private GuardedString password;
    private String hostNameOrIpAddr;
    private Integer port;
    private ConnectionType connectionType;

    /* ********** CONSTRUCTOR ************ */
    public SolarisConfiguration() {
        // default constructor
    }
    /** 
     * cloning constructor, deep copy 
     */
    public SolarisConfiguration(Configuration config) {
        if (config instanceof SolarisConfiguration) {
            final SolarisConfiguration cfg = (SolarisConfiguration) config;
            this.userName = cfg.getUserName();
            this.password = cfg.getPassword();
            this.hostNameOrIpAddr = cfg.getHostNameOrIpAddr();
            this.port = cfg.getPort();
            this.connectionType = cfg.getConnectionType();
        } else {
            throw new RuntimeException("cannot clone other than SolarisConfiguration");
        }
    }
    
    /* ********** GET / SET ************ */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        userName = name;
    }

    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString _password) {
        this.password = _password;
    }

    public String getHostNameOrIpAddr() {
        return hostNameOrIpAddr;
    }

    public void setHostNameOrIpAddr(String nameOrIpAddr) {
        hostNameOrIpAddr = nameOrIpAddr;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public void setConnectionType(String connectionType) {
        String connType = connectionType.toUpperCase();
        if (connType.equals(ConnectionType.SSH.toString())) {
            this.connectionType = ConnectionType.SSH;
        } else if (connType.equals(ConnectionType.TELNET.toString())) {
            this.connectionType = ConnectionType.TELNET;
        }
    }

    
    /* *********** AUXILIARY METHODS ***************** */
    @Override
    public void validate() {
        String msg = "'%s' cannot be null or empty.";
        if (StringUtil.isBlank(getUserName())) {
            throw new IllegalArgumentException(String.format(msg, "UserName"));
        }
        
        if (getPassword() == null) {
            throw new IllegalArgumentException(String.format(msg, "Password"));
        }
        
        if (StringUtil.isBlank(getHostNameOrIpAddr())) {
            throw new IllegalArgumentException(String.format(msg, "Hostname/IP address"));
        }
        
        if (port == null || port < 0) {
            throw new IllegalArgumentException(String.format(msg, "Port"));
        }
        
        if (connectionType == null) {
            throw new IllegalArgumentException(String.format(msg, "Connection type"));
        }
    }
}
