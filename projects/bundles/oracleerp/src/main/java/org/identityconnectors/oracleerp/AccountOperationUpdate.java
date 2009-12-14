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
import java.text.MessageFormat;
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
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.oracleerp.AccountSQLCall.AccountSQLCallBuilder;


/**
 * The Account CreateOp implementation of the SPI
 *
 * { call {0}fnd_user_pkg.{1} ( {2} ) } // {0} .. "APPS.", {1} .. "UpdateUser"
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
 *
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountOperationUpdate extends Operation implements UpdateOp {

    private static final Log log = Log.getLog(AccountOperationUpdate.class);

    /**
     * Resp Operations
     */
    private ResponsibilitiesOperations respOps;


    /**
     * @param conn
     * @param cfg
     */
    protected AccountOperationUpdate(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
        respOps = new ResponsibilitiesOperations(conn, cfg);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        final String id = uid.getUidValue().toUpperCase();
        log.ok("update user ''{0}''", id );


        // Enable/dissable user
        final Attribute enableAttr = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, attrs);
        if ( enableAttr != null ) {
            boolean enable =AttributeUtil.getBooleanValue(enableAttr);
            if ( enable ) {
                //delete user is the same as dissable
                enable(objclass, id, options);
            } else {
                disable(objclass, id, options);
            }
        }

        final Name nameAttr = AttributeUtil.getNameFromAttributes(attrs);
        if (nameAttr != null) {
            //Cannot rename user
            if (nameAttr.getNameValue() != null) {
                final String newName = nameAttr.getNameValue();
                if (!id.equalsIgnoreCase(newName)) {
                    final String emsg = getCfg().getMessage(MSG_COULD_NOT_RENAME_USER, id, newName);
                    throw new IllegalStateException(emsg);
                }
            }
        }

        // Get the User values
        final AccountSQLCallBuilder asb = new AccountSQLCallBuilder(getCfg(), false);
        // add the id
        asb.setAttribute(objclass, AttributeBuilder.build(Uid.NAME, id), options);
        //Add default owner
        asb.setAttribute(objclass, AttributeBuilder.build(OWNER, getCfg().getDefaultOwner()), options);
        
        for (Attribute attr : attrs) {
            asb.setAttribute(objclass, attr, options);
        }

        if ( !asb.isEmpty() ) {
            // Run the create call, new style is using the defaults
            CallableStatement cs = null;
            final AccountSQLCall aSql = asb.build();
            final String msg = "Update user account {0}";
            log.ok(msg, id);
            try {
                // Create the user
                cs = getConn().prepareCall(aSql.getCallSql(), aSql.getSqlParams());
                cs.execute();
            } catch (Exception e) {
                String message = getCfg().getMessage(MSG_ACCOUNT_NOT_UPDATE, id);
                log.error(e, message);
                SQLUtil.rollbackQuietly(getConn());
                throw new ConnectorException(message, e);
            } finally {
                SQLUtil.closeQuietly(cs);
            }
        }

        // Update responsibilities
        final Attribute resp = AttributeUtil.find(RESPS, attrs);
        final Attribute directResp = AttributeUtil.find(DIRECT_RESPS, attrs);
        if ( directResp != null ) {
            respOps.updateUserResponsibilities( directResp, id);
        } else if ( resp != null ) {
            respOps.updateUserResponsibilities( resp, id);
        }

        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            new SecuringAttributesOperations(getConn(), getCfg()).updateUserSecuringAttrs(secAttr, id);
        }

        getConn().commit();
        //Return new UID
        log.ok( "update user ''{0}'' done", id );
        return new Uid(id);
    }

    /**
     * @param objclass
     * @param userName
     * @param options
     */
    private void enable(ObjectClass objclass, String userName, OperationOptions options) {
        final String method = "enable";
        log.ok( method);
        //Map attrs = _actionUtil.getAccountAttributes(user, JActionUtil.OP_ENABLE_USER);

        // no enable user stored procedure that I could find, null out
        // end_date will do nicely
        // Need user's OWNER, so need to do a getUser();
        PreparedStatement st = null;
        try {
            StringBuilder b = new StringBuilder();
            b.append("{ call " + getCfg().app() + "fnd_user_pkg.updateuser(x_user_name => ?");
            b.append(",x_owner => upper(?),x_end_date => FND_USER_PKG.null_date");
            b.append(") }");

            final String sql = b.toString();
            st = getConn().prepareStatement(sql);
            st.setString(1, userName.toUpperCase());
            st.setString(2, getCfg().getDefaultOwner());
            st.execute();
        } catch (Exception e) {
            final String msg = getCfg().getMessage(MSG_COULD_NOT_ENABLE_USER, userName);
            log.error(e, msg);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok( method);
    }

    /**
     * @param objclass
     * @param uid
     * @param options
     */
    private void disable(ObjectClass objclass, String userName, OperationOptions options) {
        final String sql = "{ call "+getCfg().app()+"fnd_user_pkg.disableuser(?) }";
        final String method = "disable";
        log.ok( method);
        CallableStatement cs = null;
        try {
            cs = getConn().prepareCall(sql);
            cs.setString(1, userName);
            cs.execute();
            // No Result ??
        } catch (Exception e) {
            final String msg = getCfg().getMessage(MSG_COULD_NOT_DISABLE_USER, userName);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(MessageFormat.format(msg, userName),e);
        } finally {
            SQLUtil.closeQuietly(cs);
            cs = null;
        }
        log.ok( method);
    }
}
