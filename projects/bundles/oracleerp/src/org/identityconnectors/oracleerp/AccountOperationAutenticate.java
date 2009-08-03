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

import static org.identityconnectors.oracleerp.OracleERPUtil.*;
import static org.identityconnectors.oracleerp.OracleERPUtil.MSG_AUTH_FAILED;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
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
        log.ok("authenticate user ''{0}''", username);
        
        Assertions.nullCheck(objectClass, "objectClass");
        Assertions.nullCheck(username, "username");
        Assertions.nullCheck(password, "password");
        
        createCheckValidateFunction();        
        
        //call the validation function
        PreparedStatement ps = null;
        ResultSet rs = null;

        
        final String sqlAccount = "select user_name, password_accesses_left,"
                    + " password_date, password_lifespan_days from "
                    + cfg.app() + "fnd_user where upper(user_name) = ?";
        
        List<SQLParam> par1 = new ArrayList<SQLParam>();
        par1.add(new SQLParam(USER_NAME, username.toUpperCase()));
        // expired passwords, to the authenticate functionality 
        AttributeMergeBuilder amb = new AttributeMergeBuilder();
        try {
            ps = conn.prepareCall(sqlAccount, par1);
            rs = ps.executeQuery();
            if (rs == null || !rs.next()) {
                //not found
                throw new InvalidCredentialException(cfg.getMessage(MSG_AUTH_FAILED, username));
            }
            final Map<String, SQLParam> columnValues = SQLUtil.getColumnValues(rs);
            //build special attributes
            new AccountOperationSearch(conn, cfg).buildSpecialAttributes(amb, columnValues);
            
            if (amb.isExpected(OperationalAttributes.PASSWORD_EXPIRED_NAME, 0, Boolean.TRUE)) {
                throw new PasswordExpiredException(cfg.getMessage(MSG_AUTH_FAILED, username));
            } 
        } catch (SQLException ex) {
            log.error(ex, sqlAccount);
            SQLUtil.rollbackQuietly(conn);
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }

        // add password param
        List<SQLParam> params = new ArrayList<SQLParam>();
        params.add(new SQLParam(USER_NAME, username));
        params.add(new SQLParam("password", password));       
        
        final String sql = "select wavesetValidateFunc1(? , ?) from dual";
        try {
            ps = conn.prepareCall(sql, params);
            rs = ps.executeQuery();
            if (rs == null || !rs.next()) {
                throw new InvalidCredentialException(cfg.getMessage(MSG_AUTH_FAILED, username));
            }            
            final boolean valid = (rs.getInt(1) == 1);
            if (!valid) {
                throw new InvalidCredentialException(cfg.getMessage(MSG_AUTH_FAILED, username));
                // password or user name
            }                
        } catch (SQLException ex) {
            log.error(ex, sql);
            SQLUtil.rollbackQuietly(conn);
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        
        conn.commit();
        return new Uid(username);
    }

    /**
     * 
     */
    private void createCheckValidateFunction() {
        StringBuilder b = new StringBuilder();
        b.append("create or replace function wavesetValidateFunc1 (username IN varchar2, password IN varchar2) ");
        b.append("RETURN NUMBER is ");
        b.append("BEGIN ");
        b.append("IF (" +cfg.app() + "FND_USER_PKG.ValidateLogin(username, password) = TRUE ) THEN RETURN 1; ");
        b.append("ELSE RETURN 0; ");
        b.append("END IF; ");
        b.append("END;");
     
        //make sure the function exist
        CallableStatement st = null;
        try {
            st = conn.prepareCall(b.toString());
            st.execute();
        } catch (SQLException ex) {
            log.error(ex, b.toString());
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
    }
    
 
}
