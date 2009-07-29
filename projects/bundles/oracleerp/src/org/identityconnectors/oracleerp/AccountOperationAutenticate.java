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

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;


/**
 * The AuthenticateOp implementation of the SPI
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountOperationAutenticate extends Operation implements AuthenticateOp {

    /**
     * @param conn
     * @param cfg
     */
    protected AccountOperationAutenticate(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(AccountOperationAutenticate.class);

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        final String sql = "{ ? = call "+cfg.app()+"FND_USER_PKG.ValidateLogin(?, ?) }";
        log.ok(sql);
        CallableStatement st = null;
        try {
            st = conn.prepareCall(sql);
            st.registerOutParameter(1, Types.BOOLEAN);
            st.setString(2, username.toUpperCase());
            SQLUtil.setGuardedStringParam(st, 3, password); //Guarded String unwrapping 
            st.execute();
            final boolean valid = st.getBoolean(1);
            if (!valid) {
                throw new InvalidPasswordException("User not authenticated");
            }
            return new Uid(OracleERPUtil.getUserId(conn, cfg, username));
        } catch (SQLException ex) {
            log.error(ex, sql);
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
    }
    
 
}
