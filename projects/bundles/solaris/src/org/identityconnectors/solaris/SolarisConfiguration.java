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

public class SolarisConfiguration extends AbstractConfiguration {

    /**
     * basic configuration properties for SSH connection {@link SolarisConnection}
     */
    private String userName;
    private GuardedString password;
    private String hostNameOrIpAddr;
    private String port;

    /* ********** CONSTRUCTOR ************ */
    public SolarisConfiguration() {
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

    public String getPort() {
        return port;
    }

    public void setPort(String _port) {
        this.port = _port;
    }

    /* *********** AUXILIARY METHODS ***************** */
    @Override
    public void validate() {
        String msg = "%s cannot be null or empty.";
        if (StringUtil.isBlank(getUserName())) {
            throw new IllegalArgumentException(String.format(msg, "UserName"));
        }
        
        if (getPassword() == null) {
            throw new IllegalArgumentException(String.format(msg, "Password"));
        }
        
        if (StringUtil.isBlank(getHostNameOrIpAddr())) {
            throw new IllegalArgumentException(String.format(msg, "Hostname/IP address"));
        }
        
        if (StringUtil.isBlank(getPort())) {
            throw new IllegalArgumentException(String.format(msg, "Port"));
        }
    }
}
