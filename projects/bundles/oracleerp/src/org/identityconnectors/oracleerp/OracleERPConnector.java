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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;


/**
 * Main implementation of the OracleErp Connector
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
@ConnectorClass(displayNameKey = "oracleerp.connector.display", configurationClass = OracleERPConfiguration.class)
public class OracleERPConnector implements Connector, AuthenticateOp, DeleteOp, SyncOp, SearchOp<FilterWhereBuilder>, UpdateOp,
        CreateOp, TestOp, SchemaOp, ScriptOnConnectorOp, OracleERPColumnNameResolver {
    
    /**
     * Setup logging for the {@link OracleERPConnector}.
     */
    static final Log log = Log.getLog(OracleERPConnector.class);
    
    /**
     * Place holder for the {@link Connection} passed into the setConnection() callback
     * {@link ConnectionFactory#setConnection(Connection)}.
     */
    private OracleERPConnection conn;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link OracleERPConnector#init}.
     */
    private OracleERPConfiguration config;

    /**
     * Check to see which responsibility account attribute is sent.
     * Version 11.5.9 only supports responsibilities, and 11.5.10 only supports
     * directResponsibilities and indirectResponsibilities
     * Default to false
     * If 11.5.10, determine if description field exists in responsibility views. 
     */     
    private boolean newResponsibilityViews = false;
    /**
     * If 11.5.10, determine if description field exists in responsibility views. 
     * Default to true
     */
    private boolean descrExists = true;  
    
    // used for admin id in calling storing procedures
    private int adminUserId = 0; 
    
    
    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.config;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see Connector#init
     */
    public void init(Configuration cfg) {
        this.config = (OracleERPConfiguration) cfg;
        this.conn = OracleERPConnection.getConnection(this.config);
        Assertions.nullCheck(this.conn, "connection");
        this.newResponsibilityViews = getNewResponsibilityViews();
        if(this.newResponsibilityViews) {
            this.descrExists = getDescriptionExiests();
        }
        final String id = getUserId(config.getUser());
        if(StringUtil.isNotBlank(id)) {
            adminUserId = new Integer(id).intValue();
        }
        initFndGlobal();
    }
    
    private void initFndGlobal() {
        // three pieces of data need for apps_initialize()
        String user_id = getUserId(config.getUser());
        String resp_id = null;
        String resp_appl_id = null;

        if (StringUtil.isNotBlank(config.getAuditResponsibility())) {

            final String SQL = "select responsibility_id, responsibility_application_id from {0}{1} where user_id = ? and "
                    + "(responsibility_id,responsibility_application_id) = (select responsibility_id,application_id from "
                    + "{0}fnd_responsibility_vl where responsibility_name = ?)";
            final String sql = MessageFormat.format(SQL, config.getSchemaId(),
                    newResponsibilityViews ? OracleERPUtil.RESPS_ALL_VIEW : OracleERPUtil.RESPS_TABLE);

            final String msg = "Oracle ERP: SELECT from {0}" + "FND_RESPONSIBILILITY_V1 " + "returned: "
                    + "RESP_ID = {1}, " + "RESP_APPL_ID = {2}";

            ArrayList<SQLParam> params = new ArrayList<SQLParam>();
            params.add(new SQLParam(user_id));
            params.add(new SQLParam(config.getAuditResponsibility()));
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {

                ps = conn.prepareStatement(sql, params);
                ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
                rs = ps.executeQuery();                
                if (rs != null) {
                    if (rs.next()) {
                        resp_id = rs.getString(1);
                        resp_appl_id = rs.getString(2);
                    }
                }

                log.ok(msg, config.getSchemaId(), resp_id, resp_appl_id);
            } catch (SQLException e) {
                log.error(e, msg, config.getSchemaId(), resp_id, resp_appl_id);
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(rs);
                rs = null;
                SQLUtil.closeQuietly(ps);
                ps = null;
            }
        }
        
        //Real initialize call
        CallableStatement cs = null;
        try {
            if (resp_id != null && resp_appl_id != null) {
                final String SQL = "call {0}FND_GLOBAL.APPS_INITIALIZE(?,?,?)";
                final String sql = "{ "+MessageFormat.format(SQL, config.getSchemaId())+" }";
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE({1}, {2}, {3}) called.";
                log.ok(msg, config.getSchemaId(), user_id, resp_id, resp_appl_id);
                cs = conn.getConnection().prepareCall(sql);
                cs.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
                cs.setString(1, user_id);
                cs.setString(2, resp_id);
                cs.setString(3, resp_appl_id);
                cs.execute();
                // Result ?
                // cstmt1 closed in finally below

            } else {
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE() NOT called.";
                log.error(msg, config.getSchemaId());
            }
        } catch (SQLException e) {
            final String msg = "Oracle ERP: Failed to call {0}FND_GLOBAL.APPS_INITIALIZE()";
            log.error(e, msg, config.getSchemaId());

        } finally {
            // close everything in case we had an exception in the middle of something
            SQLUtil.closeQuietly(cs);
            cs = null;
        }
    }

   

    /**
     * @return
     */
    private boolean getDescriptionExiests() {
        final String SQL = "select user_id, description from {0}fnd_user_resp_groups_direct where USER_ID = '9999999999'";
        PreparedStatement ps = null;
        final String sql = MessageFormat.format(SQL, config.getSchemaId());
        try {
            log.ok(sql);
            ps = conn.getConnection().prepareStatement(sql);
            ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            ps.executeQuery();
            return true;
        } catch (SQLException expected) {
            log.ok("description does not exists");
        } finally {
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        return false;
    }

    /**
     * The New responsibility format there
     * 
     * @return true/false
     */
    private boolean getNewResponsibilityViews() {
        final String sql = "select * from "+config.getSchemaId()+"fnd_views where VIEW_NAME = 'FND_USER_RESP_GROUPS_DIRECT' and APPLICATION_ID = '0'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = conn.getConnection().prepareStatement(sql);
            ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            res = ps.executeQuery();
            log.ok(sql);
            if (res.next()) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        return false;
    }

    /**
     * Disposes of the {@link OracleERPConnector}'s resources.
     * 
     * @see Connector#dispose()
     */
    public void dispose() {
        config = null;
        conn = null;
    }

    /******************
     * SPI Operations
     * 
     * Implement the following operations using the contract and
     * description found in the Javadoc for these methods.
     ******************/
    
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(attrs, "attrs");       
        if(attrs.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }
        
        //doBeforeCreateActionScripts(oclass, attrs, options);
        
        if(oclass.equals(ObjectClass.ACCOUNT)) {
            //doBeforeCreateActionScripts(oclass, attrs, options);
            final Uid uid = createAccount(oclass, attrs, options);
            //doAfterCreateActionScripts(oclass, attrs, options);
            return uid;
        } else if (oclass.equals(OracleERPUtil.RESP_OC)) {            
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.RESP_NAME_OC)) {
            // TODO add implementation            
        } else if (oclass.equals(OracleERPUtil.DIRECT_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.INDIRECT_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.APP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.AUDITOR_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FORM_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FUNCTION_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.MENU_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.SEC_GROUP_OC)) {
         // TODO add implementation   
        }

        throw new IllegalArgumentException("Create operation requires one 'ObjectClass' of "+
                "account,responsibilities, responsibilityNames, applications, securityGroups, auditorResps"+
                "or subtype: manu, form, function");
    }

    /**
     * The Create Account helper class
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
     * @param oclass
     * @param attrs
     * @param options
     */
    private Uid createAccount(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        
        final Attribute empAttr = AttributeUtil.find(Account.EMP_NUM, attrs);
        final Integer empNum =  empAttr == null ? null :  AttributeUtil.getIntegerValue(empAttr);
        final Attribute npwAttr = AttributeUtil.find(Account.NPW_NUM, attrs);
        final Integer nwpNum = npwAttr == null ? null : AttributeUtil.getIntegerValue(npwAttr);
        
        //Get the person_id and set is it as a employee id
        final String person_id = getPersonId(empNum, nwpNum);
        if (person_id != null) {
            // Person Id as a Employee_Id
            attrs.add(AttributeBuilder.build(Account.EMP_ID, person_id));
        }
        
        // Get the User values
        final Map<String, SQLParam> userValues = Account.getUserValuesMap(oclass, attrs, options, true);
        
        // Run the create call, new style is using the defaults
        CallableStatement cs = null;
        final String sql = Account.getUserCallSQL(userValues, true, config.getSchemaId());
        final List<SQLParam> userSQLParams = Account.getUserSQLParams(userValues);
        final String msg = "Create user account {0} : {1}";
        final String user_name = (String) userValues.get(Account.USER_NAME).getValue();
        log.ok(msg, user_name, sql);
        try {
            // Create the user
            cs = conn.prepareCall(sql, userSQLParams);
            cs.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            cs.execute();
        } catch (SQLException e) {
            log.error(e, user_name, sql);
            SQLUtil.rollbackQuietly(conn);
            throw new AlreadyExistsException(e);
        } finally {
            SQLUtil.closeQuietly(cs);
        }
        //Commit all
        conn.commit();
        
        //Return new UID
        return new Uid(user_name);
    }
    
   
    
    /**
     * The Update Account helper class
     * 
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
     * @param oclass
     * @param attrs
     * @param options
     */
    private Uid updateAccount(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        
        // Get the User values
        final Map<String, SQLParam> userValues = Account.getUserValuesMap(oclass, attrs, options, false);
        
        // Run the create call, new style is using the defaults
        CallableStatement cs = null;
        final String sql = Account.getUserCallSQL(userValues, false, config.getSchemaId());
        final String msg = "Create user account {0} : {1}";
        final String user_name = (String) userValues.get(Account.USER_NAME).getValue();
        log.ok(msg, user_name, sql);
        try {
            // Create the user
            cs = conn.prepareCall(sql, Account.getUserSQLParams(userValues));
            cs.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            cs.execute();
        } catch (SQLException e) {
            log.error(e, msg, user_name, sql);
            SQLUtil.rollbackQuietly(conn);
            throw new AlreadyExistsException(e);
        } finally {
            SQLUtil.closeQuietly(cs);
        }
        //Commit all
        conn.commit();
        
        //Return new UID
        return new Uid(getUserId(user_name).toString());
    }    




    /**
     * @param userName
     * @return
     * @throws SQLException
     */
    private String getUserId(String userName) {
        //Create the Uid
        String userId = null;
        final String SQL = "select user_id from {0}FND_USER where upper(user_name) = ?";
        final String sql = MessageFormat.format(SQL, config.getSchemaId());
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.getConnection().prepareStatement(sql);
            ps.setString(1, userName.toUpperCase());
            ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            rs = ps.executeQuery();
            if (rs != null) {
                if ( rs.next()) {
                    userId = rs.getString(1);
                }
                // rs closed in finally below
            }
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(rs);
        }
        
        // pstmt closed in finally below
        Assertions.nullCheck(userId, "userId");       
        return userId;
    }    

    
    /**
     * Get The personId from employeNumber or NPW number
     * @param empNum employeNumber or null 
     * @param npwNum mpw number or null
     * @return
     */
    private String getPersonId(final Integer empNum, final Integer npwNum) {

        String ret = null;
        String columnName = "";
        int number;
        if (empNum != null) {
            columnName = Account.EMP_NUM;
            number = empNum;
        } else if ( npwNum != null) {
            columnName = Account.NPW_NUM;
            number = npwNum;
        } else {
            return null;
        }
         
        final String SQL = "select person_id from {0}.PER_PEOPLE_F where {1} = ?";
        final String sql = MessageFormat.format(SQL, config.getSchemaId(), columnName);
        ResultSet rs = null; // SQL query on person_id
        PreparedStatement ps = null; // statement that generates the query
        log.ok(sql);
        try {
            ps = conn.getConnection().prepareStatement(sql);
            ps.setInt(1, number);
            ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getString(1);
            }
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        Assertions.nullCheck(ret, "person_id");
        return ret;
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid update(ObjectClass oclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(attrs, "attrs");       
        if(attrs.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }
        
        //doBeforeCreateActionScripts(oclass, attrs, options);
        
        if(oclass.equals(ObjectClass.ACCOUNT)) {
            //doBeforeUpdateActionScripts(oclass, attrs, options);
            uid = updateAccount(oclass, attrs, options);
            //doAfterUpdateActionScripts(oclass, attrs, options);
            return uid;
        } else if (oclass.equals(OracleERPUtil.RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.DIRECT_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.INDIRECT_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.APP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.AUDITOR_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FORM_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FUNCTION_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.MENU_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.RESP_NAME_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.SEC_GROUP_OC)) {
         // TODO add implementation
        }

        throw new IllegalArgumentException("Update operation requires one 'ObjectClass' of "+
                "account,responsibilities, responsibilityNames, applications, securityGroups, auditorResps"+
                "or subtype: manu, form, function");
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass oclass, Uid uid, OperationOptions options) {
        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(uid, "uid");     
        if(!oclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException("Delete operation requires an 'ObjectClass' of type account");
        }
        
        
        final String SQL ="call {0}fnd_user_pkg.disableuser(?)";
        final String sql = "{ " + MessageFormat.format(SQL, config.getSchemaId())+ " }";
   //     log.ok(sql);
        CallableStatement cs = null;
        try {
            cs = conn.getConnection().prepareCall(sql);
            final String asStringValue = AttributeUtil.getAsStringValue(uid);
            cs.setString(1, asStringValue);
            cs.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            cs.execute();
            // No Result ??
        } catch (SQLException e) {
            if (e.getErrorCode() == 20001 || e.getErrorCode() == 1403) {
                final String msg = "SQL Exception trying to delete Oracle user '{0}' ";
                throw new IllegalArgumentException(MessageFormat.format(msg, uid),e);
            } else {
              throw new UnknownUidException(uid, oclass);
            }
        } finally {
            SQLUtil.closeQuietly(cs);
            cs = null;
        }
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, final String username, final GuardedString password, final OperationOptions options) {
        // Get the needed attributes
        if ( objectClass == null || !objectClass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Assertions.blankCheck(username, "username");
        Assertions.nullCheck(password, "password");

        //TODO test the user had expired
        
        final String SQL = "? = call {0}FND_USER_PKG.ValidateLogin(?, ?)";
        final String sql = "{ " +MessageFormat.format(SQL, config.getSchemaId())+" }";
        log.ok(sql); 
        CallableStatement st = null;
        try {
            st = conn.getConnection().prepareCall(sql);
            st.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            st.registerOutParameter(1, Types.BOOLEAN);
            st.setString(2, username.toUpperCase());
            SQLUtil.setGuardedStringParam(st, 3, password); //Guarded String unwrapping 
            st.execute();
            final boolean valid = st.getBoolean(1);
            if (!valid) {
                throw new InvalidPasswordException("User not authenticated");
            }
            return new Uid(getUserId(username));
        } catch (SQLException ex) {
            log.error(ex, sql);
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(st);
            st=null;
        }
    }
    

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SyncOp#getLatestSyncToken()
     */
    public SyncToken getLatestSyncToken(ObjectClass objClass) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SyncOp#sync(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.SyncToken, org.identityconnectors.framework.common.objects.SyncResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void sync(ObjectClass objClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, this);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {

        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(handler, "handler");   
        
        if(oclass.equals(ObjectClass.ACCOUNT)) {
            
            //Names
            final String tblname = config.getSchemaId() + "fnd_user";
            final Set<String> columnNamesToGet = accountAttributesToColumnNames(options);
            // For all user query there is no need to replace or quote anything
            final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, columnNamesToGet);
            String sqlSelect = query.getSQL();
            
            if(StringUtil.isNotBlank(config.getAccountsIncluded())) {
                sqlSelect += whereAnd(sqlSelect, config.getAccountsIncluded());
            } else if( config.isActiveAccountsOnly()) {
                sqlSelect += whereAnd(sqlSelect, OracleERPUtil.ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE);
            }
            
            query.setWhere(where);

            ResultSet result = null;
            PreparedStatement statement = null;
            try {
                statement = conn.prepareStatement(sqlSelect, query.getParams());
                result = statement.executeQuery();
                while (result.next()) {
                    final Set<Attribute> attributeSet = SQLUtil.getAttributeSet(result);
                    // create the connector object..
                    final ConnectorObjectBuilder bld = buildAccountObject(attributeSet);
                    if (!handler.handle(bld.build())) {
                        break;
                    }
                }
            } catch (SQLException e) {
                throw ConnectorException.wrap(e);
            } finally {
                SQLUtil.closeQuietly(result);
                SQLUtil.closeQuietly(statement);
            }
            
        } else if (oclass.equals(OracleERPUtil.RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.DIRECT_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.INDIRECT_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.APP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.AUDITOR_RESP_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FORM_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FUNCTION_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.MENU_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.RESP_NAME_OC)) {
         // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.SEC_GROUP_OC)) {
         // TODO add implementation
        }        

    }

    /**
     * @param sqlSelect 
     * @param whereAnd
     * @return
     */
    private String whereAnd(String sqlSelect, String whereAnd) {
        int iofw = sqlSelect.indexOf("WHERE");
        return (iofw == -1) ? sqlSelect + " WHERE " + whereAnd : sqlSelect.substring(0, iofw) + "WHERE ("+sqlSelect.substring(iofw + 5) +") AND ( " + whereAnd + " )";
    }    
    
    /**
     * Construct a connector object
     * <p>Taking care about special attributes</p>
     *  
     * @param attributeSet from the database table
     * @return ConnectorObjectBuilder object
     */
    private ConnectorObjectBuilder buildAccountObject(Set<Attribute> attributeSet) {
        String uidValue = null;
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        for (Attribute attribute : attributeSet) {
            final String columnName = attribute.getName();
            final Object value = AttributeUtil.getSingleValue(attribute);
            // Map the special
            if (columnName.equalsIgnoreCase(Account.USER_NAME)) {
                if (value == null) {
                    String msg = "Name cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                bld.setName(value.toString());
            } else if (columnName.equalsIgnoreCase(Account.USER_ID)) {
                if (value == null) {
                    String msg = "Uid cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                uidValue = value.toString();
                bld.setUid(uidValue);
            } else if (columnName.equalsIgnoreCase(Account.UNENCRYPT_PWD)) {
                // No Password in the result object
            } else if (columnName.equalsIgnoreCase(Account.OWNER)) {
                // No Owner in the result object
            } else {
                bld.addAttribute(AttributeBuilder.build(columnName, value));
            }
        }

        // To be sure that uid and name are present
        if(uidValue == null) {
            throw new IllegalStateException("The uid value is missing in query");
        }
        bld.setObjectClass(ObjectClass.ACCOUNT);
        return bld;
    }        
    

    /**
     * @param options
     * @return
     */
    Set<String> accountAttributesToColumnNames(OperationOptions options) {
        Set<String> columnNamesToGet = new HashSet<String>();        
        if (options != null && options.getAttributesToGet() != null) {
            // Replace attributes to quoted columnNames
            for (String attributeName : options.getAttributesToGet()) {
                columnNamesToGet.add(accountAttributeToColumnName(attributeName));
            }        
        } 
        if(columnNamesToGet.isEmpty()) {
            columnNamesToGet = new HashSet<String>(Account.UM.values());
        }
        
        return columnNamesToGet;
    }           

    
    /**
     * @param options
     * @return a columnName
     */
    public String accountAttributeToColumnName(String attributeName) {
        return Account.UM.get(attributeName);
    }     
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.TestOp#test()
     */
    public void test() {
        config.validate();
        conn.test();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.identityconnectors.framework.spi.operations.SchemaOp#schema()
     */
    public Schema schema() {

        // Use SchemaBuilder to build the schema.
        SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        schemaBld.defineObjectClass(Account.getSchema());     
        // The Responsibilities
        schemaBld.defineObjectClass(ResponsibilityNames.getSchema());
        return schemaBld.build();
    }
    

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        final ClassLoader loader = getClass().getClassLoader();
        
        String scriptLanguage = request.getScriptLanguage();
        if("JAVASCRIPT".equals(scriptLanguage) || StringUtil.isBlank(scriptLanguage)) {
            scriptLanguage = "GROOVY"; // TODO javascript is handled by groovy, think about javascript executor
        }
        
        /*
         * Build the actionContext to pass to script
         */        
        final Map<String, Object> actionContext = new HashMap<String, Object>();
        final Map<String, Object> scriptArguments = request.getScriptArguments();
        final String nameValue = ((Name) scriptArguments.get(Name.NAME)).getNameValue();
        final GuardedString password = ((GuardedString) scriptArguments.get(OperationalAttributes.PASSWORD_NAME));

        actionContext.put("conn", conn.getConnection());  //The real connection
        actionContext.put("action", scriptArguments.get("operation")); // The action is the operation name createUser/updateUser/deleteUser/disableUser/enableUser
        actionContext.put("timing", scriptArguments.get("timing")); // The timming before / after
        actionContext.put("attributes", scriptArguments.get("attributes"));  // The attributes
        actionContext.put("id", nameValue); // The user name
        if (password != null) {
            password.access(new GuardedString.Accessor() {
                public void access(char[] clearChars) {
                    actionContext.put("password", new String(clearChars)); //The password
                }
            });
        }
        actionContext.put("trace", log); //The loging
        List<String> errorList = new ArrayList<String>();
        actionContext.put("errors", errorList); // The error list
    

        Map<String, Object> inputMap = new HashMap<String, Object>();
        inputMap.put("actionContext", actionContext);        
        
        final ScriptExecutorFactory scriptExFact = ScriptExecutorFactory.newInstance(scriptLanguage);
        final ScriptExecutor scripEx = scriptExFact.newScriptExecutor(loader, request.getScriptText(), true);
        try {
            return scripEx.execute(inputMap);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }        
    }

}
