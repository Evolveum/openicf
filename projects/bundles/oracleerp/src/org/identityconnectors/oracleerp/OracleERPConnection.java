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
package org.identityconnectors.oracleerp;

import java.sql.Connection;
import java.util.Hashtable;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseConnection;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.Configuration;

/**
 * Class to represent a OracleErp Connection 
 *  
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class OracleERPConnection extends DatabaseConnection { 
    /**
     * Use the {@link Configuration} passed in to immediately connect to a database. If the {@link Connection} fails a
     * {@link RuntimeException} will be thrown.
     * 
     * @param conn
     *            Real connection.
     * @throws RuntimeException
     *             if there is a problem creating a {@link java.sql.Connection}.
     */
    public OracleERPConnection(Connection conn) {
        super(conn);
    }

    /**
     * Test enabled create connection function
     * 
     * @param config
     * @return a new {@link DatabaseTableConnection} connection
     */
    static OracleERPConnection getConnection(OracleERPConfiguration config) {
        java.sql.Connection connection;
        final String user = config.getUser();
        final GuardedString password = config.getPassword();
        final String datasource = config.getDataSource();
        if (StringUtil.isNotBlank(datasource)) {
            final String[] jndiProperties = config.getJndiProperties();
            final ConnectorMessages connectorMessages = config.getConnectorMessages();
            final Hashtable<String, String> prop = JNDIUtil.arrayToHashtable(jndiProperties, connectorMessages);                
            if(StringUtil.isNotBlank(user) && password != null) {
                connection = SQLUtil.getDatasourceConnection(datasource, user, password, prop);
            } else {
                connection = SQLUtil.getDatasourceConnection(datasource, prop);
            } 
        } else {
            final String driver = config.getDriver();
            final String connectionUrl = config.getConnectionUrl();
            connection = SQLUtil.getDriverMangerConnection(driver, connectionUrl, user, password);
        }
        return new OracleERPConnection(connection);
    }

}
