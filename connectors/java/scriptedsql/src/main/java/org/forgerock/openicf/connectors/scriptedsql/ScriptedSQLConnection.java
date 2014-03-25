/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
* " Portions Copyrighted [year] [name of copyright owner]"
*
*/
package org.forgerock.openicf.connectors.scriptedsql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConnection;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

/**
 * Class to represent a ScriptedSQL Connection.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * 
 */
public class ScriptedSQLConnection implements ScriptedConnection {

    private ScriptedSQLConfiguration configuration;
    private Connection sqlConn = null;

    /**
     * Constructor of ScriptedSQLConnection class.
     *
     * @param configuration the actual {@link ScriptedSQLConfiguration}
     */
    public ScriptedSQLConnection(ScriptedSQLConfiguration configuration) {
        this.configuration = configuration;
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
        final String login = config.getUser();
        final GuardedString password = config.getPassword();

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
            log.ok("The new connection using datasource {0} is created", datasource);
        } else {
            final String driver = config.getJdbcDriver();
            final String connectionUrl = config.formatUrlTemplate();
            log.info("Getting a new connection using connection url {0} and user {1}", connectionUrl, login);
            connection = SQLUtil.getDriverMangerConnection(driver, connectionUrl, login, password);
            log.ok("The new connection using connection url {0} and user {1} is created", connectionUrl, login);
        }

        //Set auto-commit mode 
        try {
            if (config.isAutoCommit()) {
                log.info("Setting AutoCommit to true");
                connection.setAutoCommit(true);
            } else {
                log.info("Setting AutoCommit to false");
                connection.setAutoCommit(false);
            }
        } catch (SQLException expected) {
            log.error(expected, "setAutoCommit() exception");
        }
        return connection;
    }

    /**
     * Release internal resources.
     */
    @Override
    public void dispose() {
        SQLUtil.closeQuietly(sqlConn);
    }

    /**
     * If internal connection is not usable, throw IllegalStateException.
     */
    @Override
    public void test() {
        try {
            if (null == getConnectionHandler() || sqlConn.isClosed() || !sqlConn.isValid(2)) {
                throw new ConnectionBrokenException("JDBC connection is broken");
            }
        } catch (SQLException e) {
            throw ConnectionBrokenException.wrap(e);
        }
    }

    @Override
    public Object getConnectionHandler() {
        if (sqlConn == null) {
            sqlConn = connect(configuration);
        }
        return this.sqlConn;
    }
}
