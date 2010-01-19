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

import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.rw3270.RW3270Configuration;

/**
 * Configuration proxy 
 * Prefer to use external configuration proxy class, when there will be needed more configurations.S
 */
public class RW3270ConfigurationProxy implements RW3270Configuration {
    private RacfConfiguration _config;
    private int _index;

    public RW3270ConfigurationProxy(RacfConfiguration config, int index) {
        _config = config;
        _index = index;
    }

    public Script getConnectScript() {
        return _config.getConnectScript();
    }

    public String getConnectionClassName() {
        return _config.getConnectionClassName();
    }

    public String[] getConnectionProperties() {
        return _config.getConnectionProperties();
    }

    public Script getDisconnectScript() {
        return _config.getDisconnectScript();
    }

    public String getHostNameOrIpAddr() {
        return _config.getHostNameOrIpAddr();
    }

    public Integer getHostTelnetPortNumber() {
        return _config.getHostTelnetPortNumber();
    }

    public GuardedString getPassword() {
        return _config.getPasswords()[_index];
    }

    public String getUserName() {
        return _config.getUserNames()[_index];
    }

    public void setConnectScript(Script script) {
        _config.setConnectScript(script);
    }

    public void setConnectionClassName(String className) {
        _config.setConnectionClassName(className);
    }

    public void setConnectionProperties(String[] properties) {
        _config.setConnectionProperties(properties);
    }

    public void setDisconnectScript(Script script) {
        _config.setDisconnectScript(script);
    }

    public void setHostNameOrIpAddr(String nameOrIpAddr) {
        _config.setHostNameOrIpAddr(nameOrIpAddr);
    }

    public void setHostTelnetPortNumber(Integer port) {
        _config.setHostTelnetPortNumber(port);
    }

    public void setPassword(GuardedString password) {
        GuardedString[] passwords = _config.getPasswords();
        passwords[_index] = password;
        _config.setPasswords(passwords);
    }

    public void setUserName(String name) {
        String[] userNames = _config.getUserNames();
        userNames[_index] = name;
        _config.setUserNames(userNames);
    }

    public ConnectorMessages getConnectorMessages() {
        return _config.getConnectorMessages();
    }

    public void setConnectorMessages(ConnectorMessages messages) {
        _config.setConnectorMessages(messages);
    }

    public void validate() {
        _config.validate();
    }
}