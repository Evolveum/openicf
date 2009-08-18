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
package org.identityconnectors.rw3270;


import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.Configuration;

public interface RW3270Configuration extends Configuration {
    /**
     * Get Host Name for RW3270 Connection
     * @return Host Name for RW3270 Connection
     */
    public String getHostNameOrIpAddr();
    /**
     * Set Host Name for RW3270 Connection
     * @param nameOrIpAddr -- Host Name for RW3270 Connection
     */
    public void setHostNameOrIpAddr(String nameOrIpAddr);
    /**
     * Get Host Port for RW3270 Telnet Connection (used for command line interaction)
     * @return Host Port for RW3270 LDAP Connection
     */
    public Integer getHostTelnetPortNumber();
    /**
     * Set Host Port for RW3270 Telnet Connection (used for command line interaction)
     * @param port -- Host Port for RW3270 Telnet Connection
     */
    public void setHostTelnetPortNumber(Integer port);
    /**
     * Get the set of properties needed to configure the connection
     * @return the set of properties needed to configure the connection
     */
    public String[] getConnectionProperties();
    /**
     * Set the set of properties needed to configure the connection
     * @param properties -- the set of properties needed to configure the connection
     */
    public void setConnectionProperties(String[] properties);
    /**
     * RW3270 connections are defined as a pair:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * </ul>
     * This is the username portion.
     * @return username
     */
    public String getUserName();
    /**
     * RW3270 connections are defined as a pair:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * </ul>
     * This is the username portion.
     * 
     * @param name -- username
     */
    public void setUserName(String name);
    /**
     * RW3270 connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the password portion.
     * @return an array of passwords
     */
    public GuardedString getPassword();
    /**
     * RW3270 connections are defined as a pair:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * </ul>
     * This is the password portion.
     * @param password -- password
     */
    public void setPassword(GuardedString password);
    /**
     * Get the he language to be used for Scripts
     * @return language
     */
    public String getScriptingLanguage();
    /**
     * Set the language to be used for Scripts
     * @param language -- scripting language
     */
    public void setScriptingLanguage(String language);
    /**
     * Get the script to be executed to establish a command line session
     * @return script
     */
    public String getConnectScript();
    /**
     * Set the script to be executed to establish a command line session
     * @param script -- script
     */
    public void setConnectScript(String script);
    /**
     * Get the script to be executed to terminate a command line session
     * @return script
     */
    public String getDisconnectScript();
    /**
     * Set the script to be executed to establish a command line session
     * @param script -- script
     */
    public void setDisconnectScript(String script);
    /**
     * Get the name of the implementation of RW3270Connection in use.
     * @return RW3270Connection implementation class name
     */
    public String getConnectionClassName();
    /**
     * Set the name of the implementation of RW3270Connection to use for the command line session.
     * @param className -- RW3270Connection implementation class name
     */
    public void setConnectionClassName(String className);
}
