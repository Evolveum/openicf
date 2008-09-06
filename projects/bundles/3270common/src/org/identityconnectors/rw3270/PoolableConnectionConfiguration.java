/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.rw3270;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.Configuration;

public interface PoolableConnectionConfiguration extends Configuration {
    /**
     * Get Host Name for RACF Connection
     * @return Host Name for RACF Connection
     */
    public String getHostNameOrIpAddr();
    /**
     * Set Host Name for RACF Connection
     * @param nameOrIpAddr -- Host Name for RACF Connection
     */
    public void setHostNameOrIpAddr(String nameOrIpAddr);
    /**
     * Get Host Port for RACF Telnet Connection (used for command line interaction)
     * @return Host Port for RACF LDAP Connection
     */
    public Integer getHostTelnetPortNumber();
    /**
     * Set Host Port for RACF Telnet Connection (used for command line interaction)
     * @param port -- Host Port for RACF Telnet Connection
     */
    public void setHostTelnetPortNumber(Integer port);
    /**
     * Get whether SSL connection is to be used for command line interaction
     * @return true if SSL connection is to be used for command line interaction; false otherwise
     */
    public Boolean getUseSsl();
    /**
     * Set whether SSL connection is to be used for command line interaction
     * @param useSSL -- true if SSL connection is to be used for command line interaction; false otherwise
     */
    public void setUseSsl(Boolean useSSL);
    /**
     * Racf connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the username portion.
     * @return an array of usernames
     */
    public String[] getUserNames();
    /**
     * Racf connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the username portion.
     * 
     * @param names -- an array ud usernames
     */
    public void setUserNames(String[] names);
    /**
     * Racf connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the poolname portion.
     * @return an array of poolnames
     */
    public String[] getPoolNames();
    /**
     * Racf connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the poolname portion.
     * @param names -- an array of poolnames
     */
    public void setPoolNames(String[] names);
    /**
     * Racf connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the password portion.
     * @return an array of passwords
     */
    public GuardedString[] getPasswords();
    /**
     * Racf connections are defined as a triple:
     * <ul>
     * <li>username</li>
     * <li>password</li>
     * <li>poolname</li>
     * </ul>
     * This is the poolname portion.
     * @param passwords -- an array of passwords
     */
    public void setPasswords(GuardedString[] passwords);
    /**
     * Get the Groovy script to be executed to establish a command line session
     * @return groovy script
     */
    public String getConnectScript();
    /**
     * Set the Groovy script to be executed to establish a command line session
     * @param script -- groovy script
     */
    public void setConnectScript(String script);
    /**
     * Get the Groovy script to be executed to terminate a command line session
     * @return groovy script
     */
    public String getDisconnectScript();
    /**
     * Set the Groovy script to be executed to establish a command line session
     * @param script -- groovy script
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
    /**
     * Get the interval between executions of the eviction thread for the connection pool
     * @return eviction interval
     */
    public Integer getEvictionInterval();
    /**
     * Set the interval (in milliseconds) between executions of the eviction thread for the connection pool
     * @param interval -- milliseconds between eviction runs
     */
    public void setEvictionInterval(Integer interval);
}
