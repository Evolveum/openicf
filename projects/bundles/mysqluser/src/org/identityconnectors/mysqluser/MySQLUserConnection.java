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
package org.identityconnectors.mysqluser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Hashtable;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseConnection;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.Configuration;


/**
 * The MySQLUserConnection extends the DatabaseConnection overriding the test method.
 * 
 * @version $Revision $
 * @since 1.0
 */
public class MySQLUserConnection extends DatabaseConnection {

    /**
     * Use the {@link Configuration} passed in to immediately connect to a database. If the {@link Connection} fails a
     * {@link RuntimeException} will be thrown.
     * 
     * @param conn
     *            Real connection.
     * @throws RuntimeException
     *             if there is a problem creating a {@link java.sql.Connection}.
     */
    private MySQLUserConnection(Connection conn) {
        super(conn);
    }

    /**
     * Determines if the underlying JDBC {@link java.sql.Connection} is valid.
     * 
     * @see org.identityconnectors.framework.spi.Connection#test()
     * @throws RuntimeException
     *             if the underlying JDBC {@link java.sql.Connection} is not valid otherwise do nothing.
     */
    @Override
    public void test() {
    	// make sure to clear any buffers in database
        final String VALIDATE_CONNECTION = "FLUSH STATUS";
        // attempt through auto commit..
        PreparedStatement stmt = null;
        try {
            stmt = getConnection().prepareStatement(VALIDATE_CONNECTION);
            // valid queries will return a result set...
            stmt.execute();
        } catch (Exception ex) {
            // anything, not just SQLException
            // nothing to do, just invalidate the connection
            SQLUtil.rollbackQuietly(getConnection());
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(stmt);
        }
    }

    /**
     * Get the instance method
     * @param config a {@link MySQLUserConfiguration} configuration object
     * @return The connection instance
     */
    static MySQLUserConnection getConnection(MySQLUserConfiguration config) {
        java.sql.Connection connection;
        final String login = config.getUser();
        final GuardedString password = config.getPassword();
        final String datasource = config.getDatasource();
        final String[] jndiProperties = config.getJndiProperties();
        final ConnectorMessages connectorMessages = config.getConnectorMessages();
        if (StringUtil.isNotBlank(datasource)) {
            Hashtable<String, String> prop = JNDIUtil.arrayToHashtable(jndiProperties, connectorMessages);                
            if(StringUtil.isNotBlank(login) && password != null) {
                connection = SQLUtil.getDatasourceConnection(datasource, login, password, prop);
            } else {
                connection = SQLUtil.getDatasourceConnection(datasource, prop);
            } 
        } else {
            connection = SQLUtil.getDriverMangerConnection(
                    config.getDriver(), 
                    config.getUrlString(), 
                    config.getUser(), 
                    config.getPassword());
        }
        return new MySQLUserConnection(connection);
    }
}
