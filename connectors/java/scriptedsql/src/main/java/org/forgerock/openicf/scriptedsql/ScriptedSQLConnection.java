/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
package org.forgerock.openicf.scriptedsql;

import org.identityconnectors.framework.common.exceptions.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.*;

/**
 * Class to represent a ScriptedJDBC Connection 
 *  
 * @author gael
 * @version 1.0
 * @since 1.0
 */
public class ScriptedSQLConnection {

    private ScriptedSQLConfiguration _configuration;
    private Connection sqlConn = null;

    public ScriptedSQLConnection(ScriptedSQLConfiguration configuration) {
        _configuration = configuration;
    }

    /**
     * Setup logging for the {@link ScriptedSQLConnection}.
     */
    static Log log = Log.getLog(ScriptedSQLConnection.class);

    /**
     * @param config
     * @return
     */
    private static Connection connect(ScriptedSQLConfiguration config) {
        Connection connection;
        // User
        final String login = config.getUser();
        // Password
        final GuardedString password = config.getPassword();

        // Data source anyone?
        final String datasource = config.getDatasource();
        if (StringUtil.isNotBlank(datasource)) {
            log.info("Get a new connection using datasource {0}", datasource);
            final String[] jndiProperties = config.getJndiProperties();
            final ConnectorMessages connectorMessages = config.getConnectorMessages();
            final Hashtable<String, String> prop = JNDIUtil.arrayToHashtable(jndiProperties, connectorMessages);
            if (StringUtil.isNotBlank(login) && password != null) {
                connection = SQLUtil.getDatasourceConnection(datasource, login, password, prop);
            } else {
                connection = SQLUtil.getDatasourceConnection(datasource, prop);
            }
            log.ok("The new connection using datasource {0} created", datasource);
        } else {
            final String driver = config.getJdbcDriver();
            final String connectionUrl = config.formatUrlTemplate();
            log.info("Getting a new connection using connection url {0} and user {1}", connectionUrl, login);
            connection = SQLUtil.getDriverMangerConnection(driver, connectionUrl, login, password);
            log.ok("The new connection using connection url {0} and user {1} created", connectionUrl, login);
        }

        //Disable auto-commit mode
        try {
            if (connection.getAutoCommit()) {
                log.info("setAutoCommit(false)");
                connection.setAutoCommit(false);
            }
        } catch (SQLException expected) {
            //expected
            log.error(expected, "setAutoCommit(false) exception");
        }
        return connection;
    }

    /**
     * Release internal resources
     */
    public void dispose() {
        SQLUtil.closeQuietly(sqlConn);
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        //implementation
    }

    /**
     * Get the internal JDBC connection.
     * @return the connection
     */
    public Connection getSqlConnection() {
        if (sqlConn == null){
            sqlConn = connect(_configuration);
        }
        return this.sqlConn;
    }

    /**
     * Set the internal JDBC connection.
     * @param connection
     */
    public void setSqlConnection(Connection connection) {
        this.sqlConn = connection;
    }
}
