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

import java.sql.SQLException;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

/**
 * Class to represent a MSSQLServer Connection
 * 
 * @author $author$
 * @version $Revision$ $Date$
 */
public class MSSQLServerConnection {

    private static final Log LOGGER = Log.getLog(MSSQLServerConnection.class);

    private java.sql.Connection con = null;

    private MSSQLServerConfiguration configuration;

    public MSSQLServerConnection(MSSQLServerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Release internal resources
     */
    public void dispose() {
        if (null != con) {
            try {
                con.close();
            } catch (SQLException e) {
                LOGGER.error(e, "Failed to dispose the connection");
            }
        }
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        // implementation
    }

    private java.sql.Connection getConnection() {
        try {
            Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
            // Decrypt the password
            final StringBuilder clear = new StringBuilder();
            GuardedString.Accessor accessor = new GuardedString.Accessor() {
                public void access(char[] clearChars) {
                    clear.append(clearChars);
                }
            };
            con =
                    java.sql.DriverManager.getConnection(configuration.getConnectionUrl(),
                            configuration.getUserName(), clear.toString());
            // Clear the password from the cache.
            clear.setLength(0);
            if (con != null)
                System.out.println("Connection Successful!");
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e, "Error Trace in getConnection() : url = {0}", configuration
                    .getConnectionUrl());
        }
        return con;
    }
}
