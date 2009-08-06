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
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.CreateOp;


/**
 * The Account CreateOp implementation of the SPI
 * 
 * { call {0}fnd_user_pkg.{1} ( {2} ) } // {0} .. "APPL.", {1} .. "CreateUser"
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
    static final Log log = Log.getLog(AccountOperationCreate.class);
    
    /** ResOps */
    private ResponsibilitiesOperations respOps;

    /** SecuringAttributes Operations */
    private SecuringAttributesOperations secAttrOps;
    
    /**
     * @param conn
     * @param cfg
     */
    protected AccountOperationCreate(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
        respOps = new ResponsibilitiesOperations(conn, cfg);
        secAttrOps = new SecuringAttributesOperations(conn, cfg);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.CreateOp#create(org.identityconnectors.framework.common.objects.ObjectClass, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        final Name nameAttr = AttributeUtil.getNameFromAttributes(attrs);
        if( nameAttr == null || nameAttr.getNameValue() == null) {
            throw new IllegalArgumentException(cfg.getMessage(MSG_ACCOUNT_NAME_REQUIRED));
        }        
        final String name = nameAttr.getNameValue().toUpperCase();
        log.info("create user ''{0}''", name);               

        attrs = CollectionUtil.newSet(attrs); //modifiable set
        //add required owner, if missing
        if (AttributeUtil.find(OWNER, attrs) == null) {
            attrs.add(AttributeBuilder.build(OWNER, cfg.getUser() ));
        }
        
        //Get the person_id and set is it as a employee id
        final Integer person_id = getPersonId(name, conn, cfg, attrs);
        if (person_id != null) {
            // Person Id as a Employee_Id
            attrs.add(AttributeBuilder.build(EMP_ID, person_id));
        }
        
        // Get the User values
        final AccountSQLBuilder asb = new AccountSQLBuilder(cfg.app(), true)
                                            .build(oclass, attrs, options);        
        // Run the create call, new style is using the defaults
        
        if ( !asb.isEmpty() ) {
            CallableStatement cs = null;
            CallableStatement csAll = null;
            CallableStatement csUpdate = null;
            
            String sql = null;
            boolean isUpdateNeeded = false;

            final String msg = "Create user account {0}";
            log.ok(msg, name);
            try {
                // Create the user
                if (cfg.isCreateNormalizer()) {
                    sql = asb.getUserCallSQL();
                    final List<SQLParam> userSQLParams = asb.getUserSQLParams();
                    cs = conn.prepareCall(sql, userSQLParams);
                    cs.execute();
                } else {
                    sql = asb.getAllSQL();
                    final List<SQLParam> userSQLParams = asb.getAllSQLParams();                    
                    //create all
                    csAll = conn.prepareCall(sql, userSQLParams);
                    csAll.execute();

                    isUpdateNeeded = asb.isUpdateNeeded();
                    if (isUpdateNeeded) {
                        sql = asb.getUserUpdateNullsSQL();
                        final List<SQLParam> updateSQLParams = asb.getUserUpdateNullsParams();
                        csUpdate = conn.prepareCall(sql, updateSQLParams);
                        csUpdate.execute();
                    }
                }
            } catch (Exception e) {
                SQLUtil.rollbackQuietly(conn);
                final String message = cfg.getMessage(MSG_ACCOUNT_NOT_CREATE, name);
                log.error(e, message);
                throw new IllegalStateException(message, e);
            } finally {
                SQLUtil.closeQuietly(cs);
                SQLUtil.closeQuietly(csAll);
                SQLUtil.closeQuietly(csUpdate);
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
        
        conn.commit();  
        log.info("create user ''{0}'' done", name);               
        return new Uid(name);
    }
}
