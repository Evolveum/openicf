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
package org.identityconnectors.oracleerp;

import java.sql.Connection;

import org.identityconnectors.dbcommon.DatabaseConnection;
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

}
