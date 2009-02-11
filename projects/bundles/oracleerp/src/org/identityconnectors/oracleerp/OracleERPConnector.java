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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
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
     * 
     */
    public static final int ORACLE_TIMEOUT = 1800;

    static final String CURLY_BEGIN = "{ ";
    static final String CURLY_END = " }";
    
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
                ps.setQueryTimeout(ORACLE_TIMEOUT);
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
                cs.setQueryTimeout(ORACLE_TIMEOUT);
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
            ps.setQueryTimeout(ORACLE_TIMEOUT);
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
            ps.setQueryTimeout(ORACLE_TIMEOUT);
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
        final Map<String, SQLParam> userValues = getUserValuesMap(oclass, attrs, options, true);
        
        // Run the create call, new style is using the defaults
        CallableStatement cs = null;
        final String sql = getUserCallSQL(userValues, true, config.getSchemaId());
        final List<SQLParam> userSQLParams = getUserSQLParams(userValues);
        final String msg = "Create user account {0} : {1}";
        final String user_name = (String) userValues.get(Account.USER_NAME).getValue();
        log.ok(msg, user_name, sql);
        try {
            // Create the user
            cs = conn.prepareCall(sql, userSQLParams);
            cs.setQueryTimeout(ORACLE_TIMEOUT);
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
     * The Uprate Account helper class
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
        final Map<String, SQLParam> userValues = getUserValuesMap(oclass, attrs, options, false);
        
        // Run the create call, new style is using the defaults
        CallableStatement cs = null;
        final String sql = getUserCallSQL(userValues, false, config.getSchemaId());
        final String msg = "Create user account {0} : {1}";
        final String user_name = (String) userValues.get(Account.USER_NAME).getValue();
        log.ok(msg, user_name, sql);
        try {
            // Create the user
            cs = conn.prepareCall(sql, getUserSQLParams(userValues));
            cs.setQueryTimeout(ORACLE_TIMEOUT);
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
            ps.setQueryTimeout(ORACLE_TIMEOUT);
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
            ps.setQueryTimeout(ORACLE_TIMEOUT);
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
            cs.setQueryTimeout(ORACLE_TIMEOUT);
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
            st.setQueryTimeout(ORACLE_TIMEOUT);
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
            query.setWhere(where);

            ResultSet result = null;
            PreparedStatement statement = null;
            try {
                statement = conn.prepareStatement(query);
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
        schemaBld.defineObjectClass(getAccountSchema());
        
        // The Responsibilities
        ObjectClassInfoBuilder roc = new ObjectClassInfoBuilder();
        roc.setType(OracleERPUtil.RESP);
        // name='name' type='string' audit='false'
        roc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of( Flags.REQUIRED)));
        schemaBld.defineObjectClass(roc.build());
       
        
        //responsibilityNames
        ObjectClassInfoBuilder rnoc = new ObjectClassInfoBuilder();
        rnoc.setType(OracleERPUtil.RESP_NAME);
        // name='name' type='string' audit='false'
        rnoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of( Flags.REQUIRED)));
        schemaBld.defineObjectClass(rnoc.build());

        //applications, securityGroups, auditorResps
        ObjectClassInfoBuilder aoc = new ObjectClassInfoBuilder();
        aoc.setType(OracleERPUtil.APPS);
        // name='name' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of( Flags.REQUIRED)));
        schemaBld.defineObjectClass(aoc.build());

        //securityGroups
        ObjectClassInfoBuilder sgoc = new ObjectClassInfoBuilder();
        sgoc.setType(ObjectClass.GROUP_NAME);
        // name='name' type='string' audit='false'
        sgoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of( Flags.REQUIRED)));
        schemaBld.defineObjectClass(sgoc.build());
        
        //auditorResps
        ObjectClassInfoBuilder aroc = new ObjectClassInfoBuilder();
        aroc.setType(OracleERPUtil.AUDITOR_RESPS);
        // name='name' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of( Flags.REQUIRED)));
        
        // name='formNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.FORM_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readOnlyFormNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RO_FORM_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RW_FORM_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));

        // name='userFormNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.USER_FORM_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readOnlyUserFormNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RO_USER_FORM_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RW_USER_FORM_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));

        // name='formIds' type='string' audit='false'    
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.APP_ID_FORM_IDS, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readOnlyFormIds' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RO_APP_ID_FORM_IDS, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RW_APP_ID_FORM_IDS, String.class, EnumSet.of( Flags.REQUIRED)));

        // name='functionNames' type='string' audit='false'    
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.FUNCTION_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RO_FUNCTION_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RW_FUNCTION_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));

        // name='functionIds' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.FUNCTION_IDS, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readOnlyFunctionIds' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RO_FUNCTION_IDS, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'        
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.RW_FUNCTION_IDS, String.class, EnumSet.of( Flags.REQUIRED)));

        // name='userFunctionNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.USER_FUNCTION_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));

        // name='menuIds' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.MENU_IDS, String.class, EnumSet.of( Flags.REQUIRED)));
        // name='userMenuNames' type='string' audit='false'
        aroc.addAttributeInfo(AttributeInfoBuilder.build(OracleERPUtil.USER_MENU_NAMES, String.class, EnumSet.of( Flags.REQUIRED)));

        schemaBld.defineObjectClass(sgoc.build());

        return schemaBld.build();
    }

    
    /**
     */
    public static class Account {
        // static variable on update to set dates to local server time
        static final String SYSDATE = "sysdate";
        static final String NULL_DATE = "FND_USER_PKG.null_date";
        static final String NULL_CHAR = "FND_USER_PKG.null_char";
        static final String NULL_NUMBER = "FND_USER_PKG.null_number";
        static final String Q = "?";

        // The predefined attribute names
        static final String USER_NAME = "user_name";
        static final String OWNER = "owner";
        static final String UNENCRYPT_PWD = "unencrypted_password";
        static final String SESS_NUM = "session_number";
        static final String START_DATE = "start_date";
        static final String END_DATE = "end_date";
        static final String LAST_LOGON_DATE = "last_logon_date";
        static final String DESCR = "description";
        static final String PWD_DATE = "password_date";
        static final String PWD_ACCESSES_LEFT = "password_accesses_left";
        static final String PWD_LIFE_ACCESSES = "password_lifespan_accesses";
        static final String PWD_LIFE_DAYS = "password_lifespan_days";
        static final String EMP_ID = "employee_id";
        static final String EMAIL = "email_address";
        static final String FAX = "fax";
        static final String CUST_ID = "customer_id";
        static final String SUPP_ID = "supplier_id";

        // The supported operations
        static final String CREATE_FNC = "CreateUser";
        static final String UPDATE_FNC = "UpdateUser";

        // Unrelated account attribute names
        static final String EMP_NUM = "employee_number";
        static final String PERSON_FULLNAME = "person_fullname";
        static final String NPW_NUM = "npw_number";

        //Special attributes
        static final String USER_ID = "user_id";
        static final String PERSON_PARTY_ID = "person_party_id";
        static final String EXP_PWD = "expirePassword";

        // The container attributes 
        static final String RESP = "responsibilities";
        static final String RESPKEYS = "responsibilityKeys";
        static final String SEC_ATTRS = "securingAttrs";

        // The SQL call update function SQL template
        static final String SQL_CALL = "call {0}fnd_user_pkg.{1} ( {2} )"; // {0} .. "APPL.", {1} .. "CreateUser"/"UpdateUser"

        /**
         * The Account function call predefined attributeNames, columnName and call parameterName names bindings
         */
        static final Account[] UI = { new Account(USER_ID), //0
                new Account(Name.NAME, USER_NAME, "x_user_name => {0}"), //1
                new Account(null, OWNER, "x_owner => upper({0})"), //2   unreadable   
                new Account(null, UNENCRYPT_PWD, "x_unencrypted_password => {0}"),//3 unreadable     
                new Account(SESS_NUM, "x_session_number => {0}"), //4     
                new Account(START_DATE, "x_start_date => {0}"), //5     
                new Account(END_DATE, "x_end_date => {0}"), //6     
                new Account(LAST_LOGON_DATE, "x_last_logon_date => {0}"), //7     
                new Account(DESCR, "x_description => {0}"), //8     
                new Account(PWD_DATE, "x_password_date => {0}"), //9     
                new Account(PWD_ACCESSES_LEFT, "x_password_accesses_left => {0}"), //10     
                new Account(PWD_LIFE_ACCESSES, "x_password_lifespan_accesses => {0}"), //11    
                new Account(PWD_LIFE_DAYS, "x_password_lifespan_days => {0}"), //12     
                new Account(EMP_ID, "x_employee_id => {0}"), //13     
                new Account(EMAIL, "x_email_address => {0}"), //14     
                new Account(FAX, "x_fax => {0}"), //15     
                new Account(CUST_ID, "x_customer_id => {0}"), //16     
                new Account(SUPP_ID, "x_supplier_id => {0}"), //17     
            //    new Account(EMP_NUM), //18     
            //    new Account(PERSON_FULLNAME), //19     
            //    new Account(NPW_NUM), //20
                new Account(PERSON_PARTY_ID), //21     
            //    new Account(RESP), //22
            //    new Account(RESPKEYS), //23     
            //    new Account(SEC_ATTRS), //24     
            //    new Account(EXP_PWD), //25  
                new Account(Uid.NAME, USER_NAME, null) //26
        };

        /**
         * The map of attribute names and Account values
         */
        static final Map<String, String> UM = CollectionUtil.<String> newCaseInsensitiveMap();

        /**
         * Initialization of the map
         */
        static {
            for (Account ui : UI) {
                if(ui.attrName != null) {
                    UM.put(ui.attrName, ui.columnName);
                }
            }
        }

        String attrName;
        String columnName;
        String parameterName ;

        /**
         * @param attrName
         * @param columnName
         * @param parameterName
         */
        public Account(String attrName, String columnName, String parameterName) {
            this.attrName = attrName;
            this.columnName = columnName;
            this.parameterName = parameterName;
        }
        
        /**
         * @param attrName
         * @param columnName
         * @param parameterName
         */
        public Account(String columnName, String parameterName) {
            this(columnName, columnName, parameterName);
        }
        
        /**
         * @param attrName
         * @param columnName
         * @param parameterName
         */
        public Account(String columnName) {
            this(columnName, columnName, null);
        }        

    }

    /**
     * Evaluate the User Values Map
     * 
     * @param oclass
     *            the object class
     * @param attrs
     *            the set of attributes
     * @param options
     *            the operation options
     * @param create
     *            true/false for create/update
     * @return a map of userValues
     */
    Map<String, SQLParam> getUserValuesMap(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options,
            boolean create) {

        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }

        final Map<String, SQLParam> userValues = new HashMap<String, SQLParam>();

        // At first, couldn't do anything to null fields except make sql call
        // updating tables directly. Bug#9005 forced us to find oracle constants
        // to do this.
        // I couldn't null out fields with Oracle constants thru callablestatement,
        // instead, collect all null fields and make a preparedstatement call to
        // api with null fields.        

        // Handle the special attributes first to use them in decission later
        for (Attribute attr : attrs) {
            if (attr.is(Name.NAME)) {
                //         cstmt1.setString(1, identity.toUpperCase());
                userValues.put(Account.USER_NAME, new SQLParam(AttributeUtil.getAsStringValue(attr).toUpperCase(), Types.VARCHAR));
            } else {
                final SQLParam currentDate = new SQLParam(new java.sql.Date(System.currentTimeMillis()), Types.DATE);
                if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                    /*
                    cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
                    //only set 'password_date' if password changed
                    if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                        cstmt1.setDate(9, new java.sql.Date(
                                (new java.util.Date().getTime()) ));
                    }*/
                    if (AttributeUtil.getSingleValue(attr) != null) {
                        userValues.put(Account.UNENCRYPT_PWD, new SQLParam(AttributeUtil.getGuardedStringValue(attr)));
                        userValues.put(Account.PWD_DATE, currentDate);
                    }

                } else if (attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)) {
                    /* ------ adapter code ----------
                    Boolean expirePassword = null;
                    if ( ((String)accountAttrChanges.get("OPERATION")).equalsIgnoreCase("CREATE") ) {
                        if (accountAttrChanges.containsKey(EXP_PWD)) {
                            expirePassword = ((Boolean)accountAttrChanges.get(EXP_PWD));
                            if (expirePassword.booleanValue()) {
                                nullFields.append(",x_last_logon_date => FND_USER_PKG.null_date");                    
                                nullFields.append(",x_password_date => FND_USER_PKG.null_date");
                            } else {
                                cstmt1.setDate(7, new java.sql.Date(
                                                        (new java.util.Date().getTime()) ));
                            }
                        } else {
                            cstmt1.setDate(7, new java.sql.Date(
                                                    (new java.util.Date().getTime()) ));
                        }
                    } else if ( ((String)accountAttrChanges.get("OPERATION")).equalsIgnoreCase("UPDATE") ) {
                        if (accountAttrChanges.containsKey(EXP_PWD)) {
                            expirePassword = ((Boolean)accountAttrChanges.get(EXP_PWD));
                            if (expirePassword.booleanValue()) {
                                nullFields.append(",x_last_logon_date => FND_USER_PKG.null_date");                    
                                nullFields.append(",x_password_date => FND_USER_PKG.null_date");                    
                            } 
                        } 
                    }*/
                    /*
                     * On create if expirePassword is false/null, set last_logon_date to today
                     * On update if expirePassword is false/null, do nothing
                     * On both is if expirePassword is true, null out last_logon_date, and password_date 
                     * Handle expiring password differently in create vs update
                     */
                    boolean passwordExpired = false;
                    if (AttributeUtil.getSingleValue(attr) != null) {
                        passwordExpired = AttributeUtil.getBooleanValue(attr);
                    }
                    if (passwordExpired) {
                        userValues.put(Account.LAST_LOGON_DATE, new SQLParam(Account.NULL_DATE));
                        userValues.put(Account.PWD_DATE, new SQLParam(Account.NULL_DATE));
                    } else if (create) {
                        userValues.put(Account.LAST_LOGON_DATE, currentDate);
                    }

                } else if (attr.is(Account.OWNER)) {
                    //         cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
                    userValues.put(Account.OWNER, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
                } else if (attr.is(Account.START_DATE)) {
                    /* ------ adapter code ---------- 
                    // start_date 'not null' type
                    if (accountAttrChanges.containsKey(START_DATE)) {
                        if (accountAttrChanges.get(START_DATE) != null) {
                            cstmt1.setTimestamp(5, java.sql.Timestamp.valueOf((String)accountAttrChanges.get(START_DATE)) );
                        }
                    }
                     */
                    final String dateString = AttributeUtil.getAsStringValue(attr);
                    if (dateString != null) {
                        Timestamp tms = stringToTimestamp(dateString);// stringToTimestamp(dateString);
                        userValues.put(Account.START_DATE,new SQLParam(tms, Types.TIMESTAMP));
                    }

                } else if (attr.is(Account.END_DATE)) {
                    /* ------ adapter code ----------
                    if (accountAttrChanges.containsKey(END_DATE)) {
                       if (accountAttrChanges.get(END_DATE) == null) {
                           nullFields.append(",x_end_date => FND_USER_PKG.null_date");                
                       } else if ( ((String)accountAttrChanges.get(END_DATE)).equalsIgnoreCase(SYSDATE)) {
                           // force sysdate into end_date
                           nullFields.append(",x_end_date => sysdate");
                       } else {
                           cstmt1.setTimestamp(6, java.sql.Timestamp.valueOf(
                                   (String)accountAttrChanges.get(END_DATE)) );
                       }
                    }*/

                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.END_DATE, new SQLParam(Account.NULL_DATE));
                        continue;
                    }
                    final String dateString = AttributeUtil.getAsStringValue(attr);
                    if (Account.SYSDATE.equalsIgnoreCase(dateString)) {
                        userValues.put(Account.END_DATE, new SQLParam(Account.SYSDATE));
                        continue;
                    }
                    Timestamp tms = stringToTimestamp(dateString);
                    userValues.put(Account.END_DATE, new SQLParam(tms, Types.TIMESTAMP));
                } else if (attr.is(Account.DESCR)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(DESCR)) {
                        if (accountAttrChanges.get(DESCR) == null) {
                            nullFields.append(",x_description => FND_USER_PKG.null_char");                    
                            
                        } else {
                            cstmt1.setString(8, (String)accountAttrChanges.get(DESCR));               
                        }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.DESCR,  new SQLParam(Account.NULL_CHAR, Types.VARCHAR));
                    } else {
                        userValues.put(Account.DESCR,  new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
                    }

                } else if (attr.is(Account.PWD_ACCESSES_LEFT)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(PWD_ACCESSES_LEFT)) {
                        if ( (accountAttrChanges.get(PWD_ACCESSES_LEFT) == null) ||
                        ( ((String)accountAttrChanges.get(PWD_ACCESSES_LEFT)).length() == 0) ) {
                            nullFields.append(",x_password_accesses_left => FND_USER_PKG.null_number");                               
                        } else {
                            cstmt1.setInt(10, (new Integer((String)accountAttrChanges.get(PWD_ACCESSES_LEFT)).intValue()) );
                        }
                    }*/
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.PWD_ACCESSES_LEFT, new SQLParam(Account.NULL_NUMBER,Types.VARCHAR));
                    } else {
                        userValues.put(Account.PWD_ACCESSES_LEFT, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }

                } else if (attr.is(Account.PWD_LIFE_ACCESSES)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(PWD_LIFE_ACCESSES)) {
                       if ( (accountAttrChanges.get(PWD_LIFE_ACCESSES) == null)  ||
                       ( ((String)accountAttrChanges.get(PWD_LIFE_ACCESSES)).length() == 0) ) {
                           nullFields.append(",x_password_lifespan_accesses => FND_USER_PKG.null_number");                                
                       } else {
                           cstmt1.setInt(11, (new Integer((String)accountAttrChanges.get(PWD_LIFE_ACCESSES)).intValue()) );
                       }
                    } */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.PWD_LIFE_ACCESSES, new SQLParam(Account.NULL_NUMBER,Types.VARCHAR));
                    } else {
                        userValues.put(Account.PWD_LIFE_ACCESSES, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }

                } else if (attr.is(Account.PWD_LIFE_DAYS)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(PWD_LIFE_DAYS)) {
                        if ( (accountAttrChanges.get(PWD_LIFE_DAYS) == null) ||
                        ( ((String)accountAttrChanges.get(PWD_LIFE_DAYS)).length() == 0) ) {
                            nullFields.append(",x_password_lifespan_days => FND_USER_PKG.null_number");                
                        } else {
                            cstmt1.setInt(12, (new Integer((String)accountAttrChanges.get(PWD_LIFE_DAYS)).intValue()) );
                        }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.PWD_LIFE_DAYS, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.PWD_LIFE_DAYS, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }

                } else if (attr.is(Account.EMP_ID)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(EMP_ID)) {
                        if ( (accountAttrChanges.get(EMP_ID) == null)  ||
                        ( ((String)accountAttrChanges.get(EMP_ID)).length() == 0) ) {
                            nullFields.append(",x_employee_id => FND_USER_PKG.null_number");               
                        } else {
                            cstmt1.setInt(13, (new Integer((String)accountAttrChanges.get(EMP_ID)).intValue()) );
                        }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.EMP_ID, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.EMP_ID, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }

                } else if (attr.is(Account.EMAIL)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(EMAIL)) {
                        if (accountAttrChanges.get(EMAIL) == null) {
                            nullFields.append(",x_email_address => FND_USER_PKG.null_char");                
                        } else {
                            cstmt1.setString(14, (String)accountAttrChanges.get(EMAIL));
                        }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.EMAIL, new SQLParam(Account.NULL_CHAR, Types.VARCHAR));
                    } else {
                        userValues.put(Account.EMAIL, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
                    }

                } else if (attr.is(Account.FAX)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(FAX)) {
                        if (accountAttrChanges.get(FAX) == null) {
                            nullFields.append(",x_fax => FND_USER_PKG.null_char");                
                        } else {
                            cstmt1.setString(15, (String)accountAttrChanges.get(FAX));
                        }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.FAX, new SQLParam(Account.NULL_CHAR, Types.VARCHAR));
                    } else {
                        userValues.put(Account.FAX, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
                    }

                } else if (attr.is(Account.CUST_ID)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(CUST_ID)) {
                       if ( (accountAttrChanges.get(CUST_ID) == null) ||
                       ( ((String)accountAttrChanges.get(CUST_ID)).length() == 0) ) {
                           nullFields.append(",x_customer_id => FND_USER_PKG.null_number");                
                       } else {
                           cstmt1.setInt(16, (new Integer((String)accountAttrChanges.get(CUST_ID)).intValue()) );
                       }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.CUST_ID, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.CUST_ID, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }

                } else if (attr.is(Account.SUPP_ID)) {
                    /*  ------ adapter code ----------
                    if (accountAttrChanges.containsKey(SUPP_ID)) {
                        if ( (accountAttrChanges.get(SUPP_ID) == null) ||
                        ( ((String)accountAttrChanges.get(SUPP_ID)).length() == 0) ) {
                            nullFields.append(",x_supplier_id => FND_USER_PKG.null_number");                
                        } else {
                            cstmt1.setInt(17, (new Integer((String)accountAttrChanges.get(SUPP_ID)).intValue()) );
                        }
                    } 
                    */
                    if (AttributeUtil.getSingleValue(attr) == null) {
                        userValues.put(Account.SUPP_ID, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.SUPP_ID, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }
                } else if (AttributeUtil.isSpecial(attr)) {
                    log.ok("Unhandled special attribute {0}", attr.getName());
                } else {
                    log.ok("Unhandled attribute {0}", attr.getName());
                }
            }
        }
        //Check required attributes
        Assertions.nullCheck(userValues.get(Account.USER_NAME), Name.NAME);
        Assertions.nullCheck(userValues.get(Account.UNENCRYPT_PWD), OperationalAttributes.PASSWORD_NAME);
        Assertions.nullCheck(userValues.get(Account.OWNER), Account.OWNER);
        return userValues;
    }

    /**
     * @param dateString
     * @return
     */
    private Timestamp stringToTimestamp(final String dateString) {
        Timestamp tms;
        try {
            tms = Timestamp.valueOf(dateString);
        } catch (IllegalArgumentException expected) {
            tms = new Timestamp(new Long(dateString));
        }
        return tms;
    }

    /**
     * Return the userAccount create/update sql with defaults
     * 
     * @param userValues
     *            the Map of user values
     * @param create
     *            true for create/false update
     * @param schemaId
     *            the configuration schema id
     * 
     * @return a <CODE>String</CODE> sql string
     */
    String getUserCallSQL(Map<String, SQLParam> userValues, boolean create, String schemaId) {
        final String fn = (create) ? Account.CREATE_FNC : Account.UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            SQLParam val = userValues.get(ui.columnName);
            if (val == null) {
                continue; //skip all non setup values
            }
            if (!first)
                body.append(", ");
            if (isDefault(val)) {
                body.append(MessageFormat.format(ui.parameterName, val.getValue()));
            } else {
                body.append(MessageFormat.format(ui.parameterName, Account.Q)); // Non default values will be binded
            }
            first = false;
        }

        final String sql = CURLY_BEGIN + MessageFormat.format(Account.SQL_CALL, schemaId, fn, body.toString())
                + CURLY_END;
        log.ok("getSQL {0}", sql);
        return sql;
    }

    /**
     * Return the create/update parameters
     * 
     * @param userValues
     *            the Map of user values
     * @return a <CODE>List</CODE> sql object list
     */
    List<SQLParam> getUserSQLParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            final SQLParam val = userValues.get(ui.columnName);
            if (val == null) {
                continue; //skip all non setup values
            }
            if (!isDefault(val)) {
                ret.add(new SQLParam(val));
            }
        }
        return ret;
    }

    /**
     * @param val
     * @return true/false if predefined default value
     */
    private boolean isDefault(SQLParam val) {
        return Account.SYSDATE.equals(val.getValue()) 
                || Account.NULL_NUMBER.equals(val.getValue()) 
                || Account.NULL_DATE.equals(val.getValue())
                || Account.NULL_CHAR.equals(val.getValue());
    }

    /**
     * Return the userAccount create/update sql full syntax (all fields)
     * 
     * @param userValues
     *            the Map of user values
     * @param create
     *            true for create/false update
     * @param schemaId
     *            the configuration schema id
     * @return a <CODE>String</CODE> sql string
     */
    public String getAllSQL(Map<String, SQLParam> userValues, boolean create, String schemaId) {
        final String fn = (create) ? Account.CREATE_FNC : Account.UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            if (!first)
                body.append(", ");
            body.append(Account.Q); // All values will be binded
            first = false;
        }

        final String sql = CURLY_BEGIN + MessageFormat.format(Account.SQL_CALL, schemaId, fn, body.toString())
                + CURLY_END;
        log.ok("getSQL {0}", sql);
        return sql;
    }

    /**
     * Return the userAccount create/update parameters (all fields)
     * 
     * @param userValues
     *            the Map of user values
     * @return a <CODE>List</CODE> object list
     */
    public List<SQLParam> getAllSQLParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            final SQLParam val = userValues.get(ui.columnName);
            ret.add(val);
        }
        return ret;
    }

    /**
     * Return the userAccount create/update sql null updates
     * 
     * @param userValues
     *            the Map of user values
     * @param schemaId
     *            the configuration schema id
     * @return a <CODE>String</CODE> sql string
     */
    String getUserUpdateNullsSQL(Map<String, SQLParam> userValues, String schemaId) {
        StringBuilder body = new StringBuilder("x_user_name => ?, x_owner => upper(?)");
        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            SQLParam val = userValues.get(ui.columnName);
            if (val == null) {
                continue; //skip all non setup values
            }
            if (isDefault(val)) {
                body.append(", ");
                body.append(MessageFormat.format(ui.parameterName, val.getValue())); // Update just default 
            }
        }

        final String sql = CURLY_BEGIN
                + MessageFormat.format(Account.SQL_CALL, schemaId, Account.UPDATE_FNC, body.toString())
                + CURLY_END;
        log.ok("getUpdateDefaultsSQL {0}", sql);
        return sql;
    }

    /**
     * Return the create/update parameters
     * S
     * @param userValues
     *            the Map of user values
     * 
     * @return a <CODE>List</CODE> sql object list
     */
    List<SQLParam> getUserUpdateNullsParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();
        ret.add(userValues.get(Account.USER_NAME)); //1
        ret.add(userValues.get(Account.OWNER)); //2
        return ret;
    }

    /**
     * Test for null attribute values
     * 
     * @param userValues
     *            the Map of user values
     * @return <code>boolean<code> true if the update null attributes is needed
     */
    boolean isUpdateNeeded(Map<String, SQLParam> userValues) {
        for (SQLParam value : userValues.values()) {
            if (isDefault(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the Account Object Class Info
     * 
     * @return ObjectClassInfo value
     */
    public ObjectClassInfo getAccountSchema() {
        ObjectClassInfoBuilder aoc = new ObjectClassInfoBuilder();
        aoc.setType(ObjectClass.ACCOUNT_NAME);

        // The Name is supported attribute
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='OWNER' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.OWNER, String.class, EnumSet.of(Flags.NOT_READABLE, Flags.REQUIRED)));
        // name='SESS_NUM' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder
                .build(Account.SESS_NUM, String.class, EnumSet.of(Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        // <Views><String>Enable</String></Views>
        aoc.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        // name='UNENCRYPT_PWD',  Password is mapped to operationalAttribute
        aoc.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        // name='EXP_PWD' type='string' required='false'
        aoc.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRED);
        //Session number is not supported

        // name='START_DATE' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.START_DATE, String.class));
        // name='END_DATE' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.END_DATE, String.class));
        // name='END_DATE' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.LAST_LOGON_DATE, String.class, EnumSet.of(Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        // name='DESCR' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.DESCR, String.class));
        // name='EMP_ID' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.EMP_ID, String.class));
        // name='EMAIL' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.EMAIL, String.class));
        // name='FAX' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.FAX, String.class));
        // name='CUST_ID' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.CUST_ID, String.class));
        // name='SUPP_ID' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.SUPP_ID, String.class));
        // name='PWD_DATE' type='string' required='false'  TODO one or other
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_DATE, String.class));
        //ais.add(OperationalAttributeInfos.ENABLE_DATE);
        // name='PWD_ACCESSES_LEFT' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_ACCESSES_LEFT, Integer.class));
        // name='PWD_LIFE_ACCESSES' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_LIFE_ACCESSES, Integer.class));
        // name='PWD_LIFE_DAYS' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_LIFE_DAYS, Integer.class));

        // name='EMP_NUM' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.EMP_NUM, Integer.class));
        // name='PERSON_FULLNAME' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PERSON_FULLNAME, String.class));
        // name='NPW_NUM' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.NPW_NUM, Integer.class));

        // name='PERSON_PARTY_ID' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PERSON_PARTY_ID, String.class));
        // name='RESP' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.RESP, String.class));
        // name='RESPKEYS' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.RESPKEYS, String.class));
        // name='SEC_ATTRS' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.SEC_ATTRS, String.class));

        // <Views><String>Password</String><String>Reset</String></Views>
        //aoc.addAttributeInfo(OperationalAttributeInfos.RESET_PASSWORD); reset is implemented as change password

        return aoc.build();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        // TODO Auto-generated method stub
        return null;
    }   
}
