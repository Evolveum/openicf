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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.UpdateOp;


/**
 * The Account CreateOp implementation of the SPI
 * 
 * { call {0}fnd_user_pkg.{1} ( {2} ) } // {0} .. "APPL.", {1} .. "UpdateUser"
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

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(AccountOperationUpdate.class);
    
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
        final String name = uid.getUidValue().toUpperCase();      
        log.info("update user ''{0}''", name );
        
        attrs = CollectionUtil.newSet(attrs); //modifiable set       
        
        //Name is not present
        final Name nameAttr = AttributeUtil.getNameFromAttributes(attrs);
        if (nameAttr == null) {
            attrs.add(AttributeBuilder.build(Name.NAME, name));
        } else {
            //Cannot rename user
            if (nameAttr.getNameValue() != null) {
                final String newName = nameAttr.getNameValue();
                if (!name.equalsIgnoreCase(newName)) {
                    final String emsg = cfg.getMessage(MSG_COULD_NOT_RENAME_USER, name, newName);
                    throw new IllegalStateException(emsg);
                }
            } else {
               //empty name, replace using UID
                attrs.remove(nameAttr);                
                attrs.add(AttributeBuilder.build(Name.NAME, name));
            }
        }

        //Add default owner
        if (AttributeUtil.find(OWNER, attrs) == null) {
            attrs.add(AttributeBuilder.build(OWNER, CUST));
        }        
        
        // Enable/dissable user
        final Attribute enableAttr = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, attrs);
        if ( enableAttr != null ) {
            boolean enable =AttributeUtil.getBooleanValue(enableAttr);
            if ( enable ) {
                //delete user is the same as dissable
                enable(objclass, name, options);
            } else {
                disable(objclass, name, options);
            }
        }
        
        // Get the User values
        final AccountSQLBuilder asb = new AccountSQLBuilder(cfg.app(), false).build(objclass, attrs, options);
        if ( !asb.isEmpty() ) {
            // Run the create call, new style is using the defaults
            CallableStatement cs = null;
            final String sql = asb.getUserCallSQL();
            final String msg = "Update user account {0} : {1}";
            log.info(msg, name, sql);
            try {
                // Create the user
                cs = conn.prepareCall(sql, asb.getUserSQLParams());
                cs.execute();
            } catch (Exception e) {
                log.error(e, msg, name, sql);
                SQLUtil.rollbackQuietly(conn);
                throw new UnknownUidException(e);
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

        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            new SecuringAttributesOperations(conn, cfg).updateUserSecuringAttrs(secAttr, name);
        }

        conn.commit();
        //Return new UID
        log.info( "update user ''{0}'' ok", name );
        return new Uid(name);
    }
    
    /**
     * @param objclass
     * @param userName
     * @param options
     */
    private void enable(ObjectClass objclass, String userName, OperationOptions options) {
        final String method = "enable";
        log.info( method);
        //Map attrs = _actionUtil.getAccountAttributes(user, JActionUtil.OP_ENABLE_USER);

        // no enable user stored procedure that I could find, null out
        // end_date will do nicely
        // Need user's OWNER, so need to do a getUser();
        PreparedStatement st = null;
        try {
            StringBuilder b = new StringBuilder();
            b.append("{ call " + cfg.app() + "fnd_user_pkg.updateuser(x_user_name => ?");
            b.append(",x_owner => upper(?),x_end_date => FND_USER_PKG.null_date");
            b.append(") }");
            
            String msg = "Oracle ERP: realEnable sql: {0}";
            final String sql = b.toString();
            log.info( msg, sql);

            st = conn.prepareStatement(sql);
            st.setString(1, userName.toUpperCase());
            st.setString(2, cfg.getUser());
            st.execute();
        } catch (Exception e) {
            final String msg = cfg.getMessage(MSG_COULD_NOT_ENABLE_USER, userName);
            log.error(e, msg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.info( method); 
    }

    /**
     * @param objclass
     * @param uid
     * @param options
     */
    private void disable(ObjectClass objclass, String userName, OperationOptions options) {
        final String sql = "{ call "+cfg.app()+"fnd_user_pkg.disableuser(?) }";
        final String method = "disable";
        log.info( method);
        CallableStatement cs = null;
        try {
            cs = conn.prepareCall(sql);
            cs.setString(1, userName);
            cs.execute();
            // No Result ??
        } catch (Exception e) {
            final String msg = cfg.getMessage(MSG_COULD_NOT_DISABLE_USER, userName);
            SQLUtil.rollbackQuietly(conn);
            throw new IllegalArgumentException(MessageFormat.format(msg, userName),e);
        } finally {
            SQLUtil.closeQuietly(cs);
            cs = null;
        }
        log.info( method); 
    }    
}
