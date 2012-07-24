/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openicf.mssql;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the MSSQLServer Connector.
 * 
 * @author $author$
 * @version $Revision$ $Date$
 */
public class MSSQLServerConfiguration extends AbstractConfiguration {

    private final String url = "jdbc:microsoft:sqlserver://";
    private final String portNumber = "1433";
    private final String databaseName = "pubs";

    // Informs the driver to use server a side-cursor,
    // which permits more than one active statement
    // on a connection.
    private final String selectMethod = "cursor";

    // Exposed configuration properties.

    /**
     * The connector to connect to.
     */
    private String serverName = "localhost";

    /**
     * The Remote user to authenticate with.
     */
    private String userName = null;

    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;

    /**
     * Constructor
     */
    public MSSQLServerConfiguration() {

    }

    @ConfigurationProperty(order = 1, displayMessageKey = "serverName.display",
            groupMessageKey = "basic.group", helpMessageKey = "serverName.help", required = true,
            confidential = false)
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String host) {
        this.serverName = host;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "userName.display",
            groupMessageKey = "basic.group", helpMessageKey = "userName.help", required = true,
            confidential = false)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String remoteUser) {
        this.userName = remoteUser;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "password.display",
            groupMessageKey = "basic.group", helpMessageKey = "password.help", confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(userName)) {
            throw new IllegalArgumentException("Remote User cannot be null or empty.");
        }
        if (StringUtil.isBlank(serverName)) {
            throw new IllegalArgumentException("Host cannot be null or empty.");
        }
        Assertions.blankCheck(databaseName, "databaseName");
    }

    public String getConnectionUrl() {
        return url + serverName + ":" + portNumber + ";databaseName=" + databaseName
                + ";selectMethod=" + selectMethod + ";";
    }
}
