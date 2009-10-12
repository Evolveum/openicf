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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.oracleerp.AccountSQLCall.AccountSQLCallBuilder;


/**
 * The Account CreateOp implementation of the SPI
 *
 * { call {0}fnd_user_pkg.{1} ( {2} ) } // {0} .. "APPS.", {1} .. "CreateUser"
 * {2} ...  is an array of
 * x_user_name => ?,
 * x_owner => ?,
 * x_unencrypted_password => ?,
 * x_session_number => ?,
 * x_start_date => ?,
 * x_end_date => ?,
 * x_last_logon_date => ?,
 * x_description => ?,
 * x_password_date => ?,
 * x_password_accesses_left => ?,
 * x_password_lifespan_accesses => ?,
 * x_password_lifespan_days => ?,
 * x_employee_id => ?,
 * x_email_address => ?,
 * x_fax => ?,
 * x_customer_id => ?,
 * x_supplier_id => ? ) };
 *
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountOperationCreate extends Operation implements CreateOp {

    /**
     * Setup logging.
     */
    private static final Log log = Log.getLog(AccountOperationCreate.class);

    /** ResOps */
    private ResponsibilitiesOperations respOps;

    /** SecuringAttributes Operations */
    private SecuringAttributesOperations secAttrOps;

    /**
     * @param conn
     * @param cfg
     */
    AccountOperationCreate(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
        respOps = new ResponsibilitiesOperations(getConn(), getCfg());
        secAttrOps = new SecuringAttributesOperations(getConn(), getCfg());
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.CreateOp#create(org.identityconnectors.framework.common.objects.ObjectClass, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        final Name nameAttr = AttributeUtil.getNameFromAttributes(attrs);
        if( nameAttr == null || nameAttr.getNameValue() == null) {
            throw new IllegalArgumentException(getCfg().getMessage(MSG_ACCOUNT_NAME_REQUIRED));
        }
        final String name = nameAttr.getNameValue().toUpperCase();
        log.ok("create user ''{0}''", name);


        // Get the User values
        final AccountSQLCallBuilder asb = new AccountSQLCallBuilder(getCfg().app(), true);
        //add required owner, if missing
        if (AttributeUtil.find(OWNER, attrs) == null) {
            asb.setAttribute(oclass, AttributeBuilder.build(OWNER, getCfg().getUser()), options);
        }
        
        //Get the person_id and set is it as a employee id
        final Integer person_id = getPersonId(name, attrs);
        if (person_id != null) {
            // Person Id as a Employee_Id
            asb.setAttribute(oclass, AttributeBuilder.build(EMP_ID, person_id), options);
        }
        
        //Add password not expired in create
        if (AttributeUtil.find(EXP_PWD, attrs) == null && AttributeUtil.find(OperationalAttributes.PASSWORD_EXPIRED_NAME, attrs) == null) {
            asb.setAttribute(oclass, AttributeBuilder.buildPasswordExpired(false), options);
        }
        
        for (Attribute attr : attrs) {
            asb.setAttribute(oclass, attr, options);
        }
        // Run the create call, new style is using the defaults

        if ( !asb.isEmpty() ) {
            CallableStatement cs = null;
            AccountSQLCall aSql = asb.build();

            final String msg = "Create user account {0}";
            log.ok(msg, name);
            try {
                // Create the user
                cs = getConn().prepareCall(aSql.getCallSql(), aSql.getSqlParams());
                cs.execute();
            } catch (Exception e) {
                SQLUtil.rollbackQuietly(getConn());
                final String message = getCfg().getMessage(MSG_ACCOUNT_NOT_CREATE, name);
                log.error(e, message);
                throw new ConnectorException(message, e);
            } finally {
                SQLUtil.closeQuietly(cs);
            }
        }

        // Update responsibilities
        final Attribute resp = AttributeUtil.find(RESPS, attrs);
        final Attribute directResp = AttributeUtil.find(DIRECT_RESPS, attrs);
        if ( resp != null ) {
            respOps.updateUserResponsibilities( resp, name);
        } else if ( directResp != null ) {
            respOps.updateUserResponsibilities( directResp, name);
        }
        // update securing attributes
        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            secAttrOps.updateUserSecuringAttrs(secAttr, name);
        }

        getConn().commit();
        log.ok("create user ''{0}'' done", name);
        return new Uid(name);
    }
    
    /**
     * Get The personId from employeNumber or NPW number
     * @param name user identity
     * @param attrs attributes 
     * @return personid the id of the person
     */
    private Integer getPersonId(String name, Set<Attribute> attrs) {
        log.ok("getPersonId for userId: ''{0}''", name);
        Integer ret = null;
        int num = 0;
        String columnName = null;
        final Attribute empAttr = AttributeUtil.find(EMP_NUM, attrs);
        final Attribute npwAttr = AttributeUtil.find(NPW_NUM, attrs);
        if (empAttr != null) {
            num = Integer.valueOf(AttributeUtil.getAsStringValue(empAttr));
            columnName = EMP_NUM;
            log.ok("{0} present with value ''{1}''", columnName, num);
        } else if (npwAttr != null) {
            num = Integer.valueOf(AttributeUtil.getAsStringValue(npwAttr));
            columnName = NPW_NUM;
            log.ok("{0} present with value ''{1}''", columnName, num);
        } else {
            log.ok("neither {0} not {1} attributes for personId are present", EMP_NUM, NPW_NUM);
            return null;
        }

        log.ok("clomunName ''{0}''", columnName);
        String sql = "select " + PERSON_ID + " from " + getCfg().app() + "PER_PEOPLE_F where " + columnName + " = ?";        
        sql = whereAnd(sql, ACTIVE_PEOPLE_ONLY_WHERE_CLAUSE);
        ResultSet rs = null; // SQL query on person_id
        PreparedStatement ps = null; // statement that generates the query
        try {
            ps = getConn().prepareStatement(sql);
            ps.setInt(1, num);
            ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            log.ok("Oracle ERP: PERSON_ID return from {0} = {1}", sql, ret);

            if (ret == null) {
                final String msg = getCfg().getMessage(MSG_HR_LINKING_ERROR, num, name);
                log.error(msg);
                throw new ConnectorException(msg);
            }

            log.ok("getPersonId for userId: ''{0}'' -> ''{1}''", name, ret);
            return ret;
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
    }    
}
