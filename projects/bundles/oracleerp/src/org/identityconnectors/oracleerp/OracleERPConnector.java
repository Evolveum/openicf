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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.objects.Attribute;
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
public class OracleERPConnector implements Connector, AuthenticateOp, DeleteOp, SyncOp, SearchOp<FilterWhereBuilder>,
        UpdateOp, CreateOp, TestOp, SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp {

    /**
     * Setup logging for the {@link OracleERPConnector}.
     */
    static final Log log = Log.getLog(OracleERPConnector.class);

    // format flags for processing responsibilities strings, used by getResp() and getResps() methods
    private static final int RESP_FMT_KEYS = 0; // return responsibilities with keys only.
    private static final int RESP_FMT_NORMALIZE_DATES = 1; // return whole responsibilities with time data removed from date columns.
    private static final int ORA_01403 = 1403;
    public static final String PATTERN = "searchPattern";

    /**
     * used for adminUserId for calling storing procedures
     */
    private int adminUserId = 0;

    /**
     * Place holder for the {@link Configuration} passed into the init() method {@link OracleERPConnector#init}.
     */
    private OracleERPConfiguration cfg;

    /**
     * Place holder for the {@link Connection} passed into the setConnection() callback
     * {@link ConnectionFactory#setConnection(Connection)}.
     */
    private OracleERPConnection conn;

    /**
     * The account delegate instance
     */
    private Account account = Account.getInstance(this);

    /**
     * The responsibility names delegate instance
     */
    private ResponsibilityNames respNames = ResponsibilityNames.getInstance(this);

    /**
     * If 11.5.10, determine if description field exists in responsibility views. Default to true
     */
    private boolean descrExists = true;

    /**
     * Check to see which responsibility account attribute is sent. Version 11.5.9 only supports responsibilities, and
     * 11.5.10 only supports directResponsibilities and indirectResponsibilities Default to false If 11.5.10, determine
     * if description field exists in responsibility views.
     */
    private boolean newResponsibilityViews = false;
    /**
     * Responsibility Application Id
     */
    private String respApplId = "";

    /**
     * Responsibility Id
     */
    private String respId = "";
    /**
     * User id from cfg.User
     */
    private String userId = "";

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, final String username, final GuardedString password,
            final OperationOptions options) {
        // Get the needed attributes
        if (objectClass == null || !objectClass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Assertions.blankCheck(username, "username");
        Assertions.nullCheck(password, "password");

        //TODO test the user had expired

        final String SQL = "? = call {0}FND_USER_PKG.ValidateLogin(?, ?)";
        final String sql = "{ " + MessageFormat.format(SQL, cfg.app()) + " }";
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
            return new Uid(OracleERPUtil.getUserId(this, username));
        } catch (SQLException ex) {
            log.error(ex, sql);
            throw ConnectorException.wrap(ex);
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
    }

    /******************
     * SPI Operations
     * 
     * Implement the following operations using the contract and description found in the Javadoc for these methods.
     ******************/

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(attrs, "attrs");
        if (attrs.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }

        //doBeforeCreateActionScripts(oclass, attrs, options);

        if (oclass.equals(ObjectClass.ACCOUNT)) {
            //doBeforeCreateActionScripts(oclass, attrs, options);
            final Uid uid = account.create(oclass, attrs, options);
            //doAfterCreateActionScripts(oclass, attrs, options);
            return uid;
        } else if (oclass.equals(ResponsibilityNames.RESP_NAMES)) {
            final Uid uid = respNames.create(oclass, attrs, options);
            return uid;
        }

        throw new IllegalArgumentException("Create operation requires one 'ObjectClass' of "
                + "account, responsibilityNames");
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, account);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        Assertions.nullCheck(objClass, "oclass");
        Assertions.nullCheck(uid, "uid");
        if (!objClass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException("Delete operation requires an 'ObjectClass' of type account");
        }

        account.delete(objClass, uid, options);
    }

    /**
     * Disposes of the {@link OracleERPConnector}'s resources.
     * 
     * @see Connector#dispose()
     */
    public void dispose() {
        cfg = null;
        conn = null;
        account = null;
        respNames = null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {

        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(handler, "handler");

        if (oclass.equals(ObjectClass.ACCOUNT)) {
            account.executeQuery(oclass, where, handler, options);

        } else if (oclass.equals(OracleERPUtil.RESP_OC)) { //OK
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.DIRECT_RESP_OC)) { //OK
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.INDIRECT_RESP_OC)) { //OK
            // TODO add implementation 
        } else if (oclass.equals(OracleERPUtil.APP_OC)) {
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.AUDITOR_RESP_OC)) { // ok
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FORM_OC)) {
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.FUNCTION_OC)) {
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.MENU_OC)) {
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.RESP_NAMES_OC)) {
            // TODO add implementation
        } else if (oclass.equals(OracleERPUtil.SEC_GROUP_OC)) {
            // TODO add implementation
        }

    }

    /**
     * Accessor for the cfg property
     * 
     * @return the cfg
     */
    public OracleERPConfiguration getCfg() {
        return this.cfg;
    }

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.cfg;
    }

    /**
     * Accessor for the conn property
     * 
     * @return the conn
     */
    public OracleERPConnection getConn() {
        return this.conn;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SyncOp#getLatestSyncToken()
     */
    public SyncToken getLatestSyncToken(ObjectClass objClass) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see Connector#init
     */
    public void init(Configuration cfg) {
        /*  
         * RA: startConnection(): 
         * TODO: convert _dbu = new OracleDBUtil(); _dbu.setUpArgs(_resource)
         *  initUserName(), is implemented in OracleERPConfiguration: getSchemaId
         *  _ctx = makeConnection(result);
         */
        log.info("Init using configuration {0}", cfg);
        this.cfg = (OracleERPConfiguration) cfg;
        this.conn = OracleERPConnection.createOracleERPConnection(this.cfg);
        Assertions.nullCheck(this.conn, "connection");

        /*  
         * RA: startConnection(): 
         *  setNewRespView();
         *  initFndGlobal();
         */

        this.newResponsibilityViews = getNewResponsibilityViews();
        if (this.newResponsibilityViews) {
            this.descrExists = getDescriptionExiests();
        }
        this.userId = OracleERPUtil.getUserId(this, this.cfg.getUser());

        initResponsibilities();

        initFndGlobal();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        final ClassLoader loader = getClass().getClassLoader();

        String scriptLanguage = request.getScriptLanguage();
        if (StringUtil.isBlank(scriptLanguage) || !"GROOVY".equals(scriptLanguage)) {
            throw new IllegalArgumentException("invalid script language. The GROOVY is only supported");
        }

        /*
         * Build the actionContext to pass to script
         */
        final Map<String, Object> scriptArguments = request.getScriptArguments();

        final ScriptExecutorFactory scriptExFact = ScriptExecutorFactory.newInstance(scriptLanguage);
        final ScriptExecutor scripEx = scriptExFact.newScriptExecutor(loader, request.getScriptText(), true);
        try {
            //openConnection();
            scriptArguments.put("conn", conn.getConnection()); //The real connection
            return scripEx.execute(scriptArguments);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        } finally {
            //closeConnection();    
        }

    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnResourceOp#runScriptOnResource(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        final ClassLoader loader = getClass().getClassLoader();

        String scriptLanguage = request.getScriptLanguage();
        if (StringUtil.isBlank(scriptLanguage) || !"GROOVY".equals(scriptLanguage)) {
            throw new IllegalArgumentException("invalid script");
        }

        /*
         * Build the actionContext to pass to script
         */
        final Map<String, Object> actionContext = new HashMap<String, Object>();
        final Map<String, Object> scriptArguments = request.getScriptArguments();
        final String nameValue = ((Name) scriptArguments.get(Name.NAME)).getNameValue();
        final GuardedString password = ((GuardedString) scriptArguments.get(OperationalAttributes.PASSWORD_NAME));

        actionContext.put("conn", conn.getConnection()); //The real connection
        actionContext.put("action", scriptArguments.get("operation")); // The action is the operation name createUser/updateUser/deleteUser/disableUser/enableUser
        actionContext.put("timing", scriptArguments.get("timing")); // The timming before / after
        actionContext.put("attributes", scriptArguments.get("attributes")); // The attributes
        // TODO actionContext.put("currentAttributes", scriptArguments.get("attributes"));  // The attributes
        // TODO actionContext.put("changedAttributes", scriptArguments.get("attributes"));  // The attributes
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

    /*
     * (non-Javadoc)
     * 
     * @see org.identityconnectors.framework.spi.operations.SchemaOp#schema()
     */
    public Schema schema() {
        // Use SchemaBuilder to build the schema.
        SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        schemaBld.defineObjectClass(account.getSchema());
        // The Responsibilities
        schemaBld.defineObjectClass(respNames.getSchema());
        return schemaBld.build();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SyncOp#sync(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.SyncToken, org.identityconnectors.framework.common.objects.SyncResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void sync(ObjectClass objClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.TestOp#test()
     */
    public void test() {
        cfg.validate();
        conn.test();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        Assertions.nullCheck(objclass, "objclass");
        Assertions.nullCheck(replaceAttributes, "replaceAttributes");
        if (replaceAttributes.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }

        //doBeforeCreateActionScripts(oclass, attrs, options);

        if (objclass.equals(ObjectClass.ACCOUNT)) {
            //doBeforeUpdateActionScripts(oclass, attrs, options);
            uid = account.update(objclass, uid, replaceAttributes, options);
            //doAfterUpdateActionScripts(oclass, attrs, options);
            return uid;
        } else if (objclass.equals(ResponsibilityNames.RESP_NAMES)) {
            uid = respNames.update(objclass, uid, replaceAttributes, options);
        }

        throw new IllegalArgumentException("Update operation requires one 'ObjectClass' of "
                + "account,responsibilityNames");
    }

    /**
     * @return
     */
    private boolean getDescriptionExiests() {
        final String sql = "select user_id, description from " + cfg.app()
                + "fnd_user_resp_groups_direct where USER_ID = '9999999999'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = conn.prepareStatement(sql);
            res = ps.executeQuery();
            log.ok("description exists");
            return true;
        } catch (SQLException e) {
            //log.error(e, sql);
            log.ok("description does not exists");
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
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
        final String sql = "select * from " + cfg.app()
                + "fnd_views where VIEW_NAME = 'FND_USER_RESP_GROUPS_DIRECT' and APPLICATION_ID = '0'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = conn.prepareStatement(sql);
            res = ps.executeQuery();
            log.ok(sql);
            if (res != null && res.next()) {
                log.ok("ResponsibilityViews exists");
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
        log.ok("ResponsibilityViews does not exists");
        return false;
    }

    private void initFndGlobal() {

        //Real initialize call
        if (StringUtil.isNotBlank(this.userId) && StringUtil.isNotBlank(this.respId)
                && StringUtil.isNotBlank(this.respApplId)) {
            CallableStatement cs = null;
            try {
                final String sql = "call " + cfg.app() + "FND_GLOBAL.APPS_INITIALIZE(?,?,?)";
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE({1}, {2}, {3}) called.";
                log.ok(msg, cfg.app(), this.userId, this.respId, this.respApplId);
                List<SQLParam> pars = new ArrayList<SQLParam>();
                pars.add(new SQLParam(this.userId));
                pars.add(new SQLParam(this.respId));
                pars.add(new SQLParam(this.respApplId));

                cs = conn.prepareCall(sql, pars);
                cs.execute();
                // Result ?
                // cstmt1 closed in finally below

            } catch (SQLException e) {
                final String msg = "Oracle ERP: Failed to call {0}FND_GLOBAL.APPS_INITIALIZE()";
                log.error(e, msg, cfg.app());

            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(cs);
                cs = null;
            }
        } else {
            log.info("Oracle ERP: one of the userIDStr:{0}, respId: {1}, respApplId: {2} is null", this.userId,
                    this.respId, this.respApplId);
            log.ok("Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE() NOT called.", cfg.app());
        }
    }

    /**
     * 
     */
    private void initResponsibilities() {
        // three pieces of data need for apps_initialize()
        final String auditResponsibility = cfg.getAuditResponsibility();
        final String userId = OracleERPUtil.getUserId(this, cfg.getUser());

        if (StringUtil.isNotBlank(auditResponsibility)) {
            if (StringUtil.isNotBlank(userId)) {
                try {
                    adminUserId = new Integer(userId).intValue();
                    log.ok("The adminUserId is : {0} ", userId);
                } catch (Exception ex) {
                    log.error(ex, "The User Id String {0} is not a number", userId);
                }
            }

            final String view = cfg.app()
                    + ((newResponsibilityViews) ? OracleERPUtil.RESPS_ALL_VIEW : OracleERPUtil.RESPS_TABLE);
            final String sql = "select responsibility_id, responsibility_application_id from "
                    + view
                    + " where user_id = ? and "
                    + "(responsibility_id,responsibility_application_id) = (select responsibility_id,application_id from "
                    + "{0}fnd_responsibility_vl where responsibility_name = ?)";

            final String msg = "Oracle ERP SQL: {0} returned: RESP_ID = {1}, RESP_APPL_ID = {2}";

            ArrayList<SQLParam> params = new ArrayList<SQLParam>();
            params.add(new SQLParam(userId));
            params.add(new SQLParam(auditResponsibility));
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                log.info("Select responsibility for user_id: {0}, and audit responsibility {1}", userId,
                        auditResponsibility);
                ps = conn.prepareStatement(sql, params);
                rs = ps.executeQuery();
                if (rs != null) {
                    if (rs.next()) {
                        respId = rs.getString(1);
                        respApplId = rs.getString(2);
                    }
                }

                log.ok(msg, sql, respId, respApplId);
            } catch (SQLException e) {
                log.error(e, msg, sql, respId, respApplId);
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(rs);
                rs = null;
                SQLUtil.closeQuietly(ps);
                ps = null;
            }
        }
    }

    /**
     * getResponsibilities
     * 
     * @param id
     * @param respLocation
     * @param activeOnly
     * @return list of strings
     */
    public List<String> getResponsibilities(String id, String respLocation, boolean activeOnly) {

        final String method = "getResponsibilities";
        log.info(method);

        if (respLocation == null) {
            respLocation = OracleERPUtil.RESPS_TABLE;
        }

        StringBuffer b = new StringBuffer();

        b.append("SELECT fndappvl.application_name, fndrespvl.responsibility_name, ");
        b.append("fndsecgvl.Security_group_name ");
        // descr may not be available in view or in native ui with new resp views
        // bug#15492 - do not include user tables in query if id not specified, does not return allr responsibilities
        if (id != null) {
            if (!newResponsibilityViews
                    || (descrExists && respLocation.equalsIgnoreCase(OracleERPUtil.RESPS_DIRECT_VIEW))) {
                b.append(", fnduserg.DESCRIPTION");
            }
            b.append(", fnduserg.START_DATE, fnduserg.END_DATE ");
        }
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(cfg.app() + "fnd_application_vl fndappvl, ");
        // bug#15492 - don't include this join if no id is specified.
        if (id != null) {
            b.append(cfg.app() + "fnd_user fnduser, ");
            b.append(cfg.app() + respLocation + " fnduserg, ");
        }
        b.append(cfg.app() + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        // bug#15492 - don't include this join if no id is specified.
        if (id != null) {
            b.append("AND fnduser.user_id = fnduserg.user_id ");
            b.append("AND fndrespvl.RESPONSIBILITY_ID = fnduserg.RESPONSIBILITY_ID ");
            b.append("AND fndrespvl.APPLICATION_ID = fnduserg.RESPONSIBILITY_APPLICATION_ID ");
            b.append("AND fnduser.USER_NAME = ? ");
            b.append("AND fndsecgvl.security_group_id = fnduserg.security_group_id ");
        }
        if (activeOnly) {
            if (id != null) {
                b.append(" AND fnduserg.START_DATE - SYSDATE <= 0 "
                        + "AND (fnduserg.END_DATE IS NULL OR fnduserg.END_DATE - SysDate > 0)");
            }
        }

        PreparedStatement st = null;
        ResultSet res = null;
        List<String> arrayList = new ArrayList<String>();
        final String sql = b.toString();
        try {
            log.info("sql select {0}", sql);
            st = conn.prepareStatement(sql);
            if (id != null) {
                st.setString(1, id.toUpperCase());
            }
            res = st.executeQuery();
            while (res.next()) {

                // six columns with old resp table, 5 with new views - 
                // no description available
                StringBuffer sb = new StringBuffer();
                String s = getColumn(res, 2); // fndrespvl.responsibility_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 1); // fndappvl.application_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 3); // fndsecgvl.Security_group_name
                sb.append(s);
                sb.append("||");
                if (id != null) {
                    s = getColumn(res, 4); // fnduserg.DESCRIPTION or fnduserg.START_DATE
                    sb.append(s);
                }
                sb.append("||");
                if (id != null) {
                    s = getColumn(res, 5); // fnduserg.START_DATE or fnduserg.END_DATE
                    sb.append(s);
                }
                if (!newResponsibilityViews
                        || (descrExists && respLocation.equalsIgnoreCase(OracleERPUtil.RESPS_DIRECT_VIEW))) {
                    sb.append("||");
                    if (id != null) {
                        s = getColumn(res, 6); // fnduserg.END_DATE
                        sb.append(s);
                    }
                }

                arrayList.add(sb.toString());
            }
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }

        log.ok(method);
        return arrayList;
    }

    /**
     * Get a string from a result set, trimming trailing blanks.
     * 
     * @param result
     * @param col
     * @return the resulted string
     * @throws java.sql.SQLException
     */
    public String getColumn(ResultSet result, int col) throws java.sql.SQLException {
        String s = result.getString(col);
        if (s != null)
            s = s.trim();
        return s;
    }

    /**
     * Takes a date in string format and returns a normalized version of the date i.e., removing time data. null dates
     * are returned as string "null".
     */
    private String normalizeStrDate(String strDate) {
        String retDate = strDate;
        if ((strDate == null) || strDate.equalsIgnoreCase("null")) {
            retDate = "null";
        } else if (strDate.length() == 10) {
            retDate = strDate;
        } else if (strDate.length() > 10) {
            retDate = strDate.substring(0, 10); // truncate to only date i.e.,yyyy-mm-dd 
        }
        return retDate;
    } // normalizeStrDate()    

    /**
     * 
     * @param respList
     * @param identity
     * @param result
     * @throws WavesetException
     */
    public void updateUserResponsibilities(List<String> respList, String identity) {
        final String method = "updateUserResponsibilities";
        log.info(method);

        List<String> errors = new ArrayList<String>();

        // get Users Current Responsibilties
        List<String> oldResp = null;
        if (!newResponsibilityViews) {
            oldResp = getResponsibilities(identity, OracleERPUtil.RESPS_TABLE, false);
        } else {
            // can only update directly assigned resps; indirect resps are readonly
            // thru ui            
            oldResp = getResponsibilities(identity, OracleERPUtil.RESPS_DIRECT_VIEW, false);
        }
        //preserve the previous behavior where oldResp is never null.
        if (oldResp == null) {
            oldResp = new ArrayList<String>();
        }
        List<String> oldRespKeys = getResps(oldResp, RESP_FMT_KEYS);
        List<String> newRespKeys = getResps(respList, RESP_FMT_KEYS);
        // bug#13889
        // create responsibilities list with dates normalized i.e., with no time data.
        // We ignore the time data due to potential time differences between the Oracle DB environment and the IDM client.
        // start and end dates are specified as date only from the Oracle Application GUI.
        List<String> oldRespsWithNormalizedDates = getResps(oldResp, RESP_FMT_NORMALIZE_DATES);
        // if old key is not in new list, delete it
        if (oldRespKeys != null) {
            if (!oldRespKeys.isEmpty()) {
                int index = 0;
                Iterator<String> it = oldRespKeys.iterator();
                while (it.hasNext()) {
                    Object resp = it.next();
                    if (!newRespKeys.contains(resp)) {
                        // bug#9637 check to see if resp is already 
                        // endDated (disabled), if so, ignore, if not,
                        // delete resp from User
                        java.util.Date curDate = getCurrentDate();
                        java.sql.Date endDate = null;
                        boolean delResp = false;
                        String respStr = oldResp.get(index);
                        StringTokenizer tok = new StringTokenizer(respStr, "||", false);
                        if (tok != null) {
                            String endDateStr = null;
                            while (tok.hasMoreTokens()) {
                                endDateStr = tok.nextToken();
                            }
                            if (endDateStr != null && !endDateStr.equalsIgnoreCase("null")) {
                                // format date input
                                int i = endDateStr.indexOf(" ");
                                endDate = java.sql.Date.valueOf(endDateStr.substring(0, i));
                                delResp = endDate.after(curDate);
                            } else {
                                delResp = true;
                            }
                        }
                        if (delResp) {
                            deleteUserResponsibility(identity, (String) resp, errors);
                            log.error("deleted, (end_dated), responsibility: '" + resp + "' for " + identity);
                        }
                    }
                    index++;
                }
            }
        }
        // if new key is not in old list add it and remove from respList
        // after adding
        if (respList != null) {
            if (!respList.isEmpty()) {
                // make copy of array to itereate through because we will be
                // modifying the respList
                List<String> resps = new ArrayList<String>(respList);
                Iterator<String> it = resps.iterator();
                String resp = null;
                while (it.hasNext()) {
                    resp = it.next();
                    // Add/Update resp to user
                    String respKey = getResp(resp, RESP_FMT_KEYS);
                    if (!resp.equalsIgnoreCase("") && !oldRespKeys.contains(respKey)) {
                        addUserResponsibility(identity, resp, errors);
                        respList.remove(resp);
                        log.info("added responsibility: '" + resp + "' for " + identity);
                    }
                }// end-while
            }//end-if
        }//end-if
        // if new key is both lists, update it
        if (respList != null) {
            if (!respList.isEmpty()) {
                Iterator<String> it = respList.iterator();
                String resp = null;
                String respWithNormalizedDates = null;
                while (it.hasNext()) {
                    // bug#13889 -  do not update all responsibilities
                    //              only update the ones that changed.
                    //              Updating all responsibilities every time masks the audit records.
                    //              Added check to see if oldResp list 
                    //              contains the current entire responsibility
                    //              string.
                    resp = it.next();
                    if (resp != null) {
                        log.info("checking if update required for responsibility: '" + resp + "' for " + identity);
                    } else {
                        log.warn(" resp=NULL while processing updates");
                    }
                    // Add/Update resp to user
                    if (resp != null && !resp.equalsIgnoreCase("")) {
                        // normalize the date string to only contain the date, no time information.
                        respWithNormalizedDates = getResp(resp, RESP_FMT_NORMALIZE_DATES);

                        if (respWithNormalizedDates != null) {
                            log.info("respWithNormalizedDates='" + respWithNormalizedDates + "'");
                        } else {
                            log.warn("respWithNormalizedDates=null while processing updates");
                        }

                        // Add/update resp to user if the date normalized responsibility string is not in the old date normalized list.
                        if ((oldRespsWithNormalizedDates != null) && respWithNormalizedDates != null
                                && !respWithNormalizedDates.equalsIgnoreCase("")
                                && !oldRespsWithNormalizedDates.contains(respWithNormalizedDates)) {
                            updateUserResponsibility(identity, resp, errors);

                            String msg = "updated responsibility: '" + resp + "' for " + identity;
                            log.info(msg);
                        }
                    }
                }// end-while
            }//end-if
        }//end-if

        // bug#16656: delayed error handling for missing responsibilities
        if (!errors.isEmpty()) {
            StringBuffer error = new StringBuffer();
            for (int i = 0; i < errors.size(); i++) {
                String msg = errors.get(i);
                error.append(msg);
            }
            log.error(error.toString());
            throw new ConnectorException(error.toString());
        }

        log.ok(method);

    }

    private void updateUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "updateUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;
        String description = null;
        String fromDate = null;
        String toDate = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        int count = tok.countTokens();
        if ((tok != null) && (count > 4)) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
            // descr optionable in 11.5.10 - check if sent
            if (count > 5) {
                description = tok.nextToken();
            }
            fromDate = tok.nextToken();
            toDate = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        // descr null conversion
        if (description != null && !description.equalsIgnoreCase("null")) {
            description = "'" + description + "'";
        } else {
            description = null;
        }
        // date field convert - start_date cannot be null, set to sysdate
        if ((fromDate == null) || fromDate.equalsIgnoreCase("null")) {
            fromDate = "sysdate";
        } else if (fromDate.length() == 10) {
            fromDate = "to_date('" + fromDate + "', 'yyyy-mm-dd')";
        } else if (fromDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            fromDate = "to_date('" + fromDate.substring(0, fromDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        if ((toDate == null) || toDate.equalsIgnoreCase("null")) {
            toDate = null;
        } else if (toDate.equalsIgnoreCase(Account.SYSDATE)) {
            toDate = "sysdate";
        } else if (toDate.length() == 10) {
            toDate = "to_date('" + toDate + "', 'yyyy-mm-dd')";
        } else if (toDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            toDate = "to_date('" + toDate.substring(0, toDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        StringBuffer b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, false /* not doing an insert, doing an update */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            log.info("sql select {0}", sql);
            st = conn.prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            //
            // 19057: check whether this is a "no data found" error;
            // if so, then perhaps the responsibility we seek doesn't
            // have a valid app name.  We'll retry the query without
            // specifying the app name.
            //
            if (e.getErrorCode() == ORA_01403) {
                doRetryWithoutAppname = true;
            } else {
                final String msg = "Error in sql :" + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }

        if (doRetryWithoutAppname) {
            //
            // 19057: without the responsibility's application name, must
            // fall back to using just the responsibility name to identify
            // the desired responsibility
            //
            b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                    null /* respAppName is not valid */, fromDate, toDate, description, false);

            sql = b.toString();
            try {
                log.info("sql select {0}", sql);
                st = conn.prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    errors.add("Failed to update '" + resp + "' responsibility:" + e.getMessage());
                } else {
                    final String msg = "Can not execute the sql " + sql;
                    log.error(e, msg);
                    throw new ConnectorException(msg, e);
                }
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.ok(method);
    }

    private void addUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "addUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;
        String description = null;
        String fromDate = null;
        String toDate = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        int count = tok.countTokens();
        if (tok != null && count > 4) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
            // descr optionable in 11.5.10 - check if sent
            if (count > 5) {
                description = tok.nextToken();
            }
            fromDate = tok.nextToken();
            toDate = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        // descr null conversion
        if (description != null && !description.equalsIgnoreCase("null")) {
            description = "'" + description + "'";
        } else {
            description = null;
        }
        // date field convert - start_date cannot be null, set to sysdate
        if ((fromDate == null) || fromDate.equalsIgnoreCase("null")) {
            fromDate = "sysdate";
        } else if (fromDate.length() == 10) {
            fromDate = "to_date('" + fromDate + "', 'yyyy-mm-dd')";
        } else if (fromDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            fromDate = "to_date('" + fromDate.substring(0, fromDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        if ((toDate == null) || toDate.equalsIgnoreCase("null")) {
            toDate = null;
        } else if (toDate.length() == 10) {
            toDate = "to_date('" + toDate + "', 'yyyy-mm-dd')";
        } else if (toDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            toDate = "to_date('" + toDate.substring(0, toDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        StringBuffer b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, true /* doing an insert */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            log.info("execute statement ''{0}''", sql);
            st = getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            //
            // 19057: check whether this is a "no data found" error;
            // if so, then perhaps the responsibility we seek doesn't
            // have a valid app name.  We'll retry the query without
            // specifying the app name.
            //
            if (e.getErrorCode() == ORA_01403) {
                doRetryWithoutAppname = true;
            } else {
                final String msg = "Can not execute the sql " + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }

        if (doRetryWithoutAppname) {
            //
            // 19057: without the responsibility's application name, must
            // fall back to using just the responsibility name to identify
            // the desired responsibility
            //
            b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                    null /* respAppName is not valid */, fromDate, toDate, description, true);

            sql = b.toString();
            try {
                log.info("execute statement ''{0}''", sql);
                st = getConn().prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    errors.add("Failed to add '" + resp + "' responsibility:" + e.getMessage());
                } else {
                    final String msg = "Can not execute the sql " + sql;
                    log.error(e, msg);
                    throw new ConnectorException(msg, e);
                }
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.ok(method);
    }

    private void deleteUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "deleteUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        StringBuffer b = new StringBuffer();

        b.append("DECLARE user_id varchar2(300); security_group varchar2(300); ");
        b.append("responsibility_long_name varchar2(300); responsibility_app_name ");
        b.append("varchar2(300); resp_app varchar2(300); resp_key varchar2(300); ");
        b.append("description varchar2(300); resp_sec_g_key varchar2(300); ");

        b.append("BEGIN user_id := ");
        addQuoted(b, identity.toUpperCase());
        b.append("; security_group := ");
        addQuoted(b, securityGroup);
        b.append("; responsibility_long_name := ");
        addQuoted(b, respName);
        b.append("; responsibility_app_name := ");
        addQuoted(b, respAppName);
        b.append("; SELECT  fndsecg.security_group_key INTO resp_sec_g_key ");
        b.append("FROM " + cfg.app() + "fnd_security_groups fndsecg, " + cfg.app()
                + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndsecg.security_group_id = fndsecgvl.security_group_id ");
        b.append("AND fndsecgvl.security_group_name = security_group; ");
        b.append("SELECT fndapp.application_short_name, fndresp.responsibility_key, ");
        b.append("fndrespvl.description INTO resp_app, resp_key, description ");
        b
                .append("FROM " + cfg.app() + "fnd_responsibility_vl fndrespvl, " + cfg.app()
                        + "fnd_responsibility fndresp, ");
        b.append(cfg.app() + "fnd_application_vl fndappvl, " + cfg.app() + "fnd_application fndapp ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        b.append("AND fndappvl.APPLICATION_ID = fndapp.APPLICATION_ID ");
        b.append("AND fndappvl.APPLICATION_NAME = responsibility_app_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_NAME = responsibility_long_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_ID = fndresp.RESPONSIBILITY_ID ");
        b.append("AND fndrespvl.APPLICATION_ID = fndresp.APPLICATION_ID; ");
        b.append(cfg.app() + "fnd_user_pkg.DelResp (user_id, resp_app, resp_key, resp_sec_g_key); ");
        b.append("COMMIT; END;");

        final String sql = b.toString();
        try {
            log.info("execute statement ''{0}''", sql);
            st = getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_01403) {
                // bug#16656: delay error handling for missing responsibilities
                errors.add("Failed to delete '" + resp + "' responsibility:" + e.getMessage());
            } else {
                final String msg = "Can not execute the sql " + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
    }

    /**
     * This method is shared by addUserResponsibility and updateUserResponsibility to build their PL/SQL statements.
     */
    private StringBuffer buildUserRespStatement(String user, String secGroup, String respName, String respAppName,
            String fromDate, String toDate, String description, boolean doInsert) {

        StringBuffer b = new StringBuffer();
        b.append("DECLARE user varchar2(300); security_group varchar2(300); ");
        b.append("responsibility_long_name varchar2(300); ");
        if (respAppName != null) {
            b.append("responsibility_app_name varchar2(300); ");
        }
        b.append("sec_group_id Number; user_id_num Number; resp_id varchar2(300); app_id Number; sec_id Number; ");
        b.append("description varchar2(300); resp_sec_g_key varchar2(300); ");
        b.append("BEGIN user := ");
        addQuoted(b, user.toUpperCase());
        b.append("; security_group := ");
        addQuoted(b, secGroup.toUpperCase());
        b.append("; responsibility_long_name := ");
        addQuoted(b, respName);
        if (respAppName != null) {
            b.append("; responsibility_app_name := ");
            addQuoted(b, respAppName);
        }
        b.append("; ");
        b.append("SELECT responsibility_id, application_id INTO resp_id, app_id ");
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl ");
        b.append("WHERE responsibility_name = responsibility_long_name");
        if (respAppName != null) {
            b.append(" AND application_id = ");
            b.append("(SELECT application_id FROM " + cfg.app() + "fnd_application_vl ");
            b.append("WHERE application_name = responsibility_app_name)");
        }
        b.append("; ");
        b.append("SELECT user_id INTO user_id_num ");
        b.append("FROM " + cfg.app() + "fnd_user ");
        b.append("WHERE USER_NAME = user; ");
        b.append("SELECT security_group_id INTO sec_group_id ");
        b.append("FROM " + cfg.app() + "fnd_security_groups_vl ");
        b.append("WHERE SECURITY_GROUP_KEY = security_group; ");

        b.append(cfg.app());
        if (doInsert) {
            b.append("fnd_user_resp_groups_api.Insert_Assignment (user_id_num, resp_id, app_id, sec_group_id, ");
        } else {
            b.append("fnd_user_resp_groups_api.Update_Assignment (user_id_num, resp_id, app_id, sec_group_id, ");
        }
        b.append(fromDate);
        b.append(", ");
        b.append(toDate);
        b.append(", ");
        b.append(description);
        b.append("); COMMIT; END;");

        return b;
    }

    /**
     * 
     * 
     * @param secAttrList
     * @param identity
     * @param result
     * @throws WavesetException
     * 
     *             Interesting thing here is that a user can have exact duplicate securing attributes, as crazy as that
     *             sounds, they just show up multiple times in the native gui.
     * 
     *             Since there is no available key, we will delete all and add all new ones
     * 
     */
    private void updateUserSecuringAttrs(List<String> secAttrList, String identity, String userId) {
        final String method = "updateUserSecuringAttrs";
        log.info(method);

        // get Users Securing Attrs
        List<String> oldSecAttrs = getSecuringAttrs(identity, null);

        // add new attrs
        if (secAttrList != null) {
            if (!secAttrList.isEmpty()) {
                Iterator it = secAttrList.iterator();
                String secAttr = null;
                while (it.hasNext()) {
                    secAttr = (String) it.next();
                    // Only add if not there already (including value)
                    // , otherwise delete from old list
                    if (oldSecAttrs.contains(secAttr)) {
                        oldSecAttrs.remove(secAttr);
                    } else {
                        addSecuringAttr(userId, secAttr);
                    }
                }// end-while
            }//end-if
        }//end-if
        // delete old attrs
        if (oldSecAttrs != null) {
            if (!oldSecAttrs.isEmpty()) {
                Iterator it = oldSecAttrs.iterator();
                while (it.hasNext()) {
                    deleteSecuringAttr(userId, (String) it.next());
                }
            }
        }
        
        getConn().commit();

        log.ok(method);
    }

    /**
     * bug#13889 : Added method to create a responsibility string with dates normalized. respFmt: RESP_FMT_KEYS: get
     * responsibility keys (resp_name, app_name, sec_group) RESP_FMT_NORMALIZE_DATES: get responsibility string
     * (resp_name, app_name, sec_group, description, start_date, end_date) start_date, end_date (no time data, allow
     * nulls)
     */
    public String getResp(String strResp, int respFmt) {
        final String method = "getResp(String, int)";
        log.info(method + "respFmt=" + respFmt);
        String strRespRet = null;
        StringTokenizer tok = new StringTokenizer(strResp, "||", false);
        if (tok != null && tok.countTokens() > 2) {
            StringBuffer key = new StringBuffer();
            key.append(tok.nextToken()); // responsiblity name
            key.append("||");
            key.append(tok.nextToken()); // application name
            key.append("||");
            key.append(tok.nextToken()); // security group name
            if (respFmt != RESP_FMT_KEYS) {
                key.append("||");
                // descr possibly not available in ui version 11.5.10
                if (!newResponsibilityViews || descrExists) {
                    key.append(tok.nextToken()); // description
                }
                key.append("||");
                key.append(normalizeStrDate(tok.nextToken())); // start_date
                key.append("||");
                key.append(normalizeStrDate(tok.nextToken())); // end_date
            }
            strRespRet = key.toString();
        }
        log.ok(method);
        return strRespRet;
    } // getRespWithNormalizeDates()    

    /**
     * bug#13889 : Added method to create a responsibilities list with dates normalized. RESP_FMT_KEYS: get
     * responsibility keys (resp_name, app_name, sec_group) RESP_FMT_NORMALIZE_DATES: get responsibility keys
     * (resp_name, app_name, sec_group, description, start_date, end_date) start_date, end_date (no time data, allow
     * nulls)
     * 
     * @param resps
     * @param respFmt
     * @return list of Sting
     */
    public List<String> getResps(List<String> resps, int respFmt) {
        final String method = "getResps(ArrayList, int)";
        log.info(method + " respFmt=" + respFmt);
        List<String> respKeys = null;
        if (resps != null) {
            respKeys = new ArrayList<String>();
            for (int i = 0; i < resps.size(); i++) {
                String strResp = resps.get(i);
                String strRespReformatted = getResp(strResp, respFmt);
                log.info(method + " strResp='" + strResp + "', strRespReformatted='" + strRespReformatted + "'");
                respKeys.add(strRespReformatted);
            }
        }
        log.ok(method);
        return respKeys;
    } // getResps()

    private java.sql.Date getCurrentDate() {
        Calendar rightNow = Calendar.getInstance();
        java.util.Date utilDate = rightNow.getTime();
        return new java.sql.Date(utilDate.getTime());
    }

    /**
     * 
     * Return Object of Auditor Data
     * 
     * List auditorResps (GO) userMenuNames menuIds userFunctionNames functionIds formIds formNames userFormNames
     * readOnlyFormIds readWriteOnlyFormIds readOnlyFunctionIds readWriteOnlyFunctionIds readOnlyFormNames
     * readOnlyUserFormNames readWriteOnlyFormNames readWriteOnlyUserFormNames
     * 
     * @param id
     * @throws SQLException
     * @throws WavesetException
     * 
     */
    private Object getAuditorDataObject(List resps) throws SQLException {
        final String method = "getAuditorDataObject";
        log.info(method);
        // Profile Options used w/SOB and Organization
        String sobOption = "GL Set of Books ID";
        String ouOption = "MO: Operating Unit";

        GenericObject auditorData = new GenericObject();
        ArrayList auditorResps = new ArrayList();
        // for each resp
        for (int i = 0; resps != null && resps.size() > i; i++) {
            String curResp = (String) resps.get(i);
            String resp = null;
            String app = null;
            if (curResp != null) {
                StringTokenizer tok = new StringTokenizer(curResp, "||", false);
                if (tok != null && tok.countTokens() > 1) {
                    resp = tok.nextToken();
                    app = tok.nextToken();
                }
            }
            StringBuffer b = new StringBuffer();

            //one query 
            b.append("SELECT DISTINCT 'N/A' userMenuName, 0 menuID, fffv.function_id,");
            b.append("fffv.user_function_name , ffv.form_id, ffv.form_name, ffv.user_form_name, ");
            b.append("fffv.function_name, ");
            b.append("fffv.parameters  FROM fnd_form_functions_vl fffv, ");
            b.append("fnd_form_vl ffv WHERE fffv.form_id=ffv.form_id(+) ");
            b.append("AND fffv.function_id NOT IN (SELECT action_id FROM fnd_resp_functions frf1 ");
            b.append("WHERE responsibility_id=(SELECT frv.responsibility_id ");
            b.append("FROM fnd_responsibility_vl frv , fnd_application_vl fa WHERE ");
            b.append("frv.application_id=fa.application_id AND  frv.responsibility_name=? ");
            b.append("AND fa.application_name=?) AND rule_type='F') ");
            b.append("AND function_id IN (SELECT function_id FROM fnd_menu_entries fme ");
            b.append("WHERE menu_id NOT IN (SELECT action_id FROM fnd_resp_functions ");
            b.append("WHERE responsibility_id=(SELECT frv.responsibility_id FROM fnd_responsibility_vl frv ");
            b.append(", fnd_application_vl fa WHERE frv.application_id=fa.application_id ");
            b.append("AND  frv.responsibility_name=? ");
            b.append("AND fa.application_name=?) AND rule_type='M')");
            b.append("START WITH menu_id=(SELECT frv.menu_id FROM fnd_responsibility_vl frv ");
            b.append(", fnd_application_vl fa WHERE frv.application_id=fa.application_id ");
            b.append("AND  frv.responsibility_name=? ");
            b.append("AND fa.application_name=?) CONNECT BY prior sub_menu_id=menu_id) ");
            b.append("UNION SELECT DISTINCT user_menu_name userMenuName, menu_id MenuID, ");
            b.append("0 function_id, 'N/A' user_function_name, 0 form_id, 'N/A' form_name, 'N/A' user_form_name, ");
            b.append(" 'N/A' function_name, ");
            b.append("'N/A' parameters  FROM fnd_menus_vl fmv WHERE menu_id IN (");
            b.append("SELECT menu_id FROM fnd_menu_entries fme WHERE menu_id NOT IN (");
            b.append("SELECT action_id FROM fnd_resp_functions WHERE responsibility_id=(");
            b.append("SELECT frv.responsibility_id FROM fnd_responsibility_vl frv, fnd_application_vl fa ");
            b.append("WHERE frv.application_id=fa.application_id AND frv.responsibility_name=? ");
            b.append("AND fa.application_name=?) ");
            b.append("AND rule_type='M') START WITH menu_id=(SELECT frv.menu_id ");
            b.append("FROM fnd_responsibility_vl frv , fnd_application_vl fa WHERE ");
            b.append("frv.application_id=fa.application_id AND  frv.responsibility_name=? ");
            b.append("AND fa.application_name=?) ");
            b.append("CONNECT BY prior sub_menu_id=menu_id) ORDER BY 2,4");
            // one query
            TRACE.info3(method, "SQL statement: " + b.toString());
            TRACE.info4(method, "Resp: " + curResp);

            PreparedStatement st = null;
            ResultSet res = null;

            List menuIds = new ArrayList();
            List menuNames = new ArrayList();
            List functionIds = new ArrayList();
            List userFunctionNames = new ArrayList();
            List roFormIds = new ArrayList();
            List rwFormIds = new ArrayList();
            List roFormNames = new ArrayList();
            List rwFormNames = new ArrayList();
            List roUserFormNames = new ArrayList();
            List rwUserFormNames = new ArrayList();
            List roFunctionNames = new ArrayList();
            List rwFunctionNames = new ArrayList();
            List roFunctionIds = new ArrayList();
            List rwFunctionIds = new ArrayList();

            // objects to collect all read/write functions and related info
            // which is used later for false positive fix-up
            GenericObject functionIdMap = new GenericObject();
            GenericObject attrMap = new GenericObject();

            try {

                st = getConn().prepareStatement(b.toString());
                st.setString(1, resp);
                st.setString(2, app);
                st.setString(3, resp);
                st.setString(4, app);
                st.setString(5, resp);
                st.setString(6, app);
                st.setString(7, resp);
                st.setString(8, app);
                st.setString(9, resp);
                st.setString(10, app);
                res = st.executeQuery();

                while (res != null && res.next()) {

                    String menuName = getColumn(res, 1);
                    if (menuName != null && !menuName.equals("N/A")) {
                        menuNames.add(menuName);
                    }
                    String menuId = getColumn(res, 2);
                    if (menuId != null && !menuId.equals("0")) {
                        menuIds.add(menuId);
                    }
                    String funId = getColumn(res, 3);
                    if (funId != null && !funId.equals("0")) {
                        functionIds.add(funId);
                    }
                    String funName = getColumn(res, 4);
                    if (funName != null && !funName.equals("N/A")) {
                        userFunctionNames.add(funName);
                    }
                    String param = getColumn(res, 9);// column added for parameters
                    boolean qo = false;
                    if (param != null) {
                        // pattern can be QUERY_ONLY=YES, QUERY_ONLY = YES, QUERY_ONLY="YES",
                        // QUERY_ONLY=Y, etc..
                        Pattern pattern = Pattern.compile("\\s*QUERY_ONLY\\s*=\\s*\"*Y");
                        Matcher matcher = pattern.matcher(param.toUpperCase());
                        if (matcher.find()) {
                            qo = true;
                        }
                    }
                    if (qo) {
                        String ROfunId = getColumn(res, 3);
                        if (ROfunId != null && !ROfunId.equals("0")) {
                            roFunctionIds.add(ROfunId);
                        }
                        String ROfunctionName = getColumn(res, 8);
                        if (ROfunctionName != null && !ROfunctionName.equals("N/A")) {
                            roFunctionNames.add(ROfunctionName);
                        }
                        String ROformId = getColumn(res, 5);
                        if (ROformId != null && !ROformId.equals("0")) {
                            roFormIds.add(ROformId);
                        }
                        String ROformName = getColumn(res, 6);
                        if (ROformName != null && !ROformName.equals("N/A")) {
                            roFormNames.add(ROformName);
                        }
                        String ROuserFormName = getColumn(res, 7);
                        if (ROuserFormName != null && !ROuserFormName.equals("N/A")) {
                            roUserFormNames.add(ROuserFormName);
                        }
                    } else {
                        String RWfunId = getColumn(res, 3);
                        if (RWfunId != null && !RWfunId.equals("0")) {
                            rwFunctionIds.add(RWfunId);
                        }
                        String RWfunctionName = getColumn(res, 8);
                        if (RWfunctionName != null && !RWfunctionName.equals("N/A")) {
                            rwFunctionNames.add(RWfunctionName);
                            attrMap.put("rwFunctionName", RWfunctionName);
                        }
                        String RWformId = getColumn(res, 5);
                        if (RWformId != null && !RWformId.equals("0")) {
                            rwFormIds.add(RWformId);
                            attrMap.put("rwFormId", RWformId);
                        }
                        String RWformName = getColumn(res, 6);
                        if (RWformName != null && !RWformName.equals("N/A")) {
                            rwFormNames.add(RWformName);
                            attrMap.put("rwFormName", RWformName);
                        }
                        String RWuserFormName = getColumn(res, 7);
                        if (RWuserFormName != null && !RWuserFormName.equals("N/A")) {
                            rwUserFormNames.add(RWuserFormName);
                            attrMap.put("rwUserFormName", RWuserFormName);
                        }
                        if (!attrMap.isEmpty()) {
                            functionIdMap.put(RWfunId, attrMap.cloneObject());
                            attrMap.clear();
                        }
                    }// end-if (qo)
                }// end-while
                // no catch, just use finally to ensure closes happen
            } finally {
                closeResult(res);
                res = null;
                closeStatement(st);
                st = null;
            }

            // Post Process Results looking for false-positive (misidentified rw objects) only if 
            // there are any read only functions (roFunctionIds != null)
            // The results of this query are additional roFunctionIds by following logic
            // in bug#13405.
            if (roFunctionIds != null && roFunctionIds.size() > 0) {
                b = new StringBuffer();
                b.append("SELECT function_id from fnd_compiled_menu_functions ");
                b.append("WHERE menu_id IN ");
                b.append("( SELECT sub_menu_id from fnd_menu_entries ");
                b.append("WHERE function_id IN (");
                b.append(com.waveset.util.Util.listToCommaDelimitedString(roFunctionIds));
                b.append(") AND sub_menu_id > 0 AND grant_flag = 'Y' ");
                b.append("AND sub_menu_id IN (");
                b.append(com.waveset.util.Util.listToCommaDelimitedString(menuIds));
                b.append(") )");
                TRACE.info3(method, "SQL statement (Post Processing): " + b.toString());
                try {
                    st = _ctx.prepareStatement(b.toString());
                    res = st.executeQuery();
                    while (res != null && res.next()) {
                        // get each functionId and use as key to find associated rw objects
                        // remove from rw bucket and place in ro bucket
                        String functionId = getColumn(res, 1);
                        if (functionId != null) {
                            GenericObject idObj = functionIdMap.getObject(functionId);
                            if (idObj != null) {
                                if (rwFunctionIds.contains(functionId)) {
                                    rwFunctionIds.remove(functionId);
                                    roFunctionIds.add(functionId);
                                }
                                String rwFunctionName = (String) idObj.get("rwFunctionName");
                                if (rwFunctionNames.contains(rwFunctionName)) {
                                    rwFunctionNames.remove(rwFunctionName);
                                    roFunctionNames.add(rwFunctionName);
                                }
                                String rwFormId = (String) idObj.get("rwFormId");
                                if (rwFormIds.contains(rwFormId)) {
                                    rwFormIds.remove(rwFormId);
                                    roFormIds.add(rwFormId);
                                }
                                String rwFormName = (String) idObj.get("rwFormName");
                                if (rwFormNames.contains(rwFormName)) {
                                    rwFormNames.remove(rwFormName);
                                    roFormNames.add(rwFormName);
                                }
                                String rwUserFormName = (String) idObj.get("rwUserFormName");
                                if (rwUserFormNames.contains(rwUserFormName)) {
                                    rwUserFormNames.remove(rwUserFormName);
                                    roUserFormNames.add(rwUserFormName);
                                }
                            }// if idObj ! null
                        }// if functionId != null                    
                    }// end while

                    // no catch, just use finally to ensure closes happen
                } finally {
                    closeResult(res);
                    res = null;
                    closeStatement(st);
                    st = null;
                }
            } // end-if roFunctionIds has contents              

            // create objects and load auditor data
            GenericObject respData = new GenericObject();
            ArrayList userFormNameList = new ArrayList(roUserFormNames);
            userFormNameList.addAll(rwUserFormNames);
            ArrayList formNameList = new ArrayList(roFormNames);
            formNameList.addAll(rwFormNames);
            ArrayList formIdList = new ArrayList(roFormIds);
            formIdList.addAll(rwFormIds);
            ArrayList functionNameList = new ArrayList(roFunctionNames);
            functionNameList.addAll(rwFunctionNames);
            ArrayList functionIdsList = new ArrayList(roFunctionIds);
            functionIdsList.addAll(rwFunctionIds);

            respData.put(USER_MENU_NAMES, menuNames);
            respData.put(MENU_IDS, menuIds);
            respData.put(USER_FUNCTION_NAMES, userFunctionNames);
            respData.put(FUNCTION_IDS, functionIdsList);
            respData.put(RO_FUNCTION_IDS, roFunctionIds);
            respData.put(RW_FUNCTION_IDS, rwFunctionIds);
            respData.put(APP_ID_FORM_IDS, formIdList);
            respData.put(RO_APP_ID_FORM_IDS, roFormIds);
            respData.put(RW_APP_ID_FORM_IDS, rwFormIds);
            respData.put(FORM_NAMES, formNameList);
            respData.put(RO_FORM_NAMES, roFormNames);
            respData.put(RW_FORM_NAMES, rwFormNames);
            respData.put(USER_FORM_NAMES, userFormNameList);
            respData.put(RO_USER_FORM_NAMES, roUserFormNames);
            respData.put(RW_USER_FORM_NAMES, rwUserFormNames);
            respData.put(FUNCTION_NAMES, functionNameList);
            respData.put(RO_FUNCTION_NAMES, roFunctionNames);
            respData.put(RW_FUNCTION_NAMES, rwFunctionNames);
            respData.put(RESP_NAME, resp + "||" + app);

            // check to see if SOB/ORGANIZATION is required
            if (sobOrg != null && (Boolean.valueOf(sobOrg)).booleanValue()) {
                b = new StringBuffer();
                // query for SOB / Organization
                b.append("Select distinct ");
                b.append("decode(fpo1.user_profile_option_name, '");
                b
                        .append(sobOption
                                + "', fpo1.user_profile_option_name||'||'||gsob.name||'||'||gsob.set_of_books_id, '");
                b.append(ouOption + "', fpo1.user_profile_option_name||'||'||hou1.name||'||'||hou1.organization_id)");
                b.append(" from " + cfg.app() + "fnd_responsibility_vl fr, " + cfg.app()
                        + "fnd_profile_option_values fpov, " + cfg.app() + "fnd_profile_options fpo");
                b.append(" , " + cfg.app() + "fnd_profile_options_vl fpo1, " + cfg.app()
                        + "hr_organization_units hou1, " + cfg.app() + "gl_sets_of_books gsob");
                b
                        .append(" where fr.responsibility_id = fpov.level_value and gsob.set_of_books_id = fpov.profile_option_value");
                b
                        .append(" and  fpo.profile_option_name = fpo1.profile_option_name and fpo.profile_option_id = fpov.profile_option_id");
                b
                        .append(" and  fpo.application_id = fpov.application_id and   fpov.profile_option_value = to_char(hou1.organization_id(+))");
                b
                        .append(" and  fpov.profile_option_value = to_char(gsob.set_of_books_id(+)) and   fpov.level_id = 10003");
                b.append(" and  fr.responsibility_name = ?");
                b.append(" order by 1");

                TRACE.info3(method, "SQL statement: " + b.toString());
                TRACE.info4(method, "Resp: " + curResp);

                try {
                    st = _ctx.prepareStatement(b.toString());
                    st.setString(1, resp);
                    res = st.executeQuery();

                    while (res != null && res.next()) {
                        String option = getColumn(res, 1);
                        if (option != null && option.startsWith(sobOption)) {
                            List values = Util.stringToList(option, "||");
                            if (values != null && values.size() == 3) {
                                respData.put(SOB_NAME, values.get(1));
                                respData.put(SOB_ID, values.get(2));
                            }
                        } else if (option != null && option.startsWith(ouOption)) {
                            List values = Util.stringToList(option, "||");
                            if (values != null && values.size() == 3) {
                                respData.put(OU_NAME, values.get(1));
                                respData.put(OU_ID, values.get(2));
                            }
                        }
                    }
                } finally {
                    closeResult(res);
                    res = null;
                    closeStatement(st);
                    st = null;
                }
            }

            if (TRACE.level4(method)) {
                if (menuNames != null) {
                    Collections.sort(menuNames);
                    TRACE.info4(method, "USER_MENU_NAMES " + menuNames.toString());
                }
                if (menuIds != null) {
                    Collections.sort(menuIds);
                    TRACE.info4(method, "MENU_IDS " + menuIds.toString());
                }
                if (userFunctionNames != null) {
                    Collections.sort(userFunctionNames);
                    TRACE.info4(method, "USER_FUNCTION_NAMES " + userFunctionNames.toString());
                }
                if (functionIdsList != null) {
                    Collections.sort(functionIdsList);
                    TRACE.info4(method, "FUNCTION_IDS " + functionIdsList.toString());
                }
                if (roFunctionIds != null) {
                    Collections.sort(roFunctionIds);
                    TRACE.info4(method, "RO_FUNCTION_IDS " + roFunctionIds.toString());
                }
                if (rwFunctionIds != null) {
                    Collections.sort(rwFunctionIds);
                    TRACE.info4(method, "RW_FUNCTION_IDS " + rwFunctionIds.toString());
                }
                if (formIdList != null) {
                    Collections.sort(formIdList);
                    TRACE.info4(method, "APP_ID_FORM_IDS " + formIdList.toString());
                }
                if (roFormIds != null) {
                    Collections.sort(roFormIds);
                    TRACE.info4(method, "RO_APP_ID_FORM_IDS " + roFormIds.toString());
                }
                if (rwFormIds != null) {
                    Collections.sort(rwFormIds);
                    TRACE.info4(method, "RW_APP_ID_FORM_IDS " + rwFormIds.toString());
                }
                if (formNameList != null) {
                    Collections.sort(formNameList);
                    TRACE.info4(method, "FORM_NAMES " + formNameList.toString());
                }
                if (roFormNames != null) {
                    Collections.sort(roFormNames);
                    TRACE.info4(method, "RO_FORM_NAMES " + roFormNames.toString());
                }
                if (rwFormNames != null) {
                    Collections.sort(rwFormNames);
                    TRACE.info4(method, "RW_FORM_NAMES " + rwFormNames.toString());
                }
                if (userFormNameList != null) {
                    Collections.sort(userFormNameList);
                    TRACE.info4(method, "USER_FORM_NAMES " + userFormNameList.toString());
                }
                if (roUserFormNames != null) {
                    Collections.sort(roUserFormNames);
                    TRACE.info4(method, "RO_USER_FORM_NAMES " + roUserFormNames.toString());
                }
                if (rwUserFormNames != null) {
                    Collections.sort(rwUserFormNames);
                    TRACE.info4(method, "RW_USER_FORM_NAMES " + rwUserFormNames.toString());
                }
                if (functionNameList != null) {
                    Collections.sort(functionNameList);
                    TRACE.info4(method, "FUNCTION_NAMES " + functionNameList.toString());
                }
                if (roFunctionNames != null) {
                    Collections.sort(roFunctionNames);
                    TRACE.info4(method, "RO_FUNCTION_NAMES " + roFunctionNames.toString());
                }
                if (rwFunctionNames != null) {
                    Collections.sort(rwFunctionNames);
                    TRACE.info4(method, "RW_FUNCTION_NAMES " + rwFunctionNames.toString());
                }
            }
            auditorResps.add(respData);
        }// end for each resp
        auditorData.put(AUDITOR_RESPS, auditorResps);
        TRACE.exit1(method);
        return auditorData;
    }

    /**
     * Add a quoted string to a SQL statement we're building in a buffer. If the attribute might be an integer, then
     * call addAttributeValue() instead, which factors in the syntax of the attribute when determining whether or not to
     * quote the value.
     */
    public void addQuoted(StringBuffer b, String s) {
        b.append("'");
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\'')
                    b.append("''");
                else
                    b.append(ch);
            }
        }
        b.append("'");
    }

    /**
     * 
     * Get Securing Attributes
     */
    private ArrayList getSecuringAttrs(String id, Map options) {
        final String method = "getSecAttrs";
        log.info(method);
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuffer b = new StringBuffer();
        //default value
        String pattern = null;
        if (options != null) {
            pattern = (String) options.get(PATTERN);
        }
        b.append("SELECT distinct akattrvl.NAME, fndappvl.APPLICATION_NAME ");
        if (id != null) {
            b.append(", akwebsecattr.VARCHAR2_VALUE, akwebsecattr.DATE_VALUE, akwebsecattr.NUMBER_VALUE ");
        }
        b.append("FROM " + cfg.app() + "AK_ATTRIBUTES_VL akattrvl, " + cfg.app()
                + "FND_APPLICATION_VL fndappvl ");
        // conditionalize including AK_WEB_USER_SEC_ATTR_VALUES in the FROM
        // list, has significant performance impact when present but not
        // referenced.
        if (id != null) {
            b.append(", " + cfg.app() + "AK_WEB_USER_SEC_ATTR_VALUES akwebsecattr, ");
            b.append(cfg.app() + "FND_USER fnduser ");
        }

        b.append("WHERE akattrvl.ATTRIBUTE_APPLICATION_ID = fndappvl.APPLICATION_ID ");
        if (id != null) {
            b.append("AND akwebsecattr.WEB_USER_ID = fnduser.USER_ID ");
            b.append("AND akattrvl.ATTRIBUTE_APPLICATION_ID = akwebsecattr.ATTRIBUTE_APPLICATION_ID ");
            b.append("AND akattrvl.ATTRIBUTE_CODE = akwebsecattr.ATTRIBUTE_CODE ");
            b.append("AND fnduser.USER_NAME = ?");
            pattern = "%";
        }
        b.append(" AND akattrvl.NAME LIKE '");
        b.append(pattern);
        b.append("' ");
        b.append("ORDER BY akattrvl.NAME");

        ArrayList arrayList = new ArrayList();
        final String sql = b.toString();
        try {
            log.info("execute sql {0}", sql);
            st = getConn().prepareStatement(sql);
            if (id != null) {
                st.setString(1, id.toUpperCase());
            }
            res = st.executeQuery();
            while (res.next()) {

                StringBuffer sb = new StringBuffer();
                sb.append(getColumn(res, 1));
                sb.append("||");
                sb.append(getColumn(res, 2));
                // get one of three values (one column per type) if id is specified
                // value can be type varchar2, date, number
                if (id != null) {
                    if (getColumn(res, 3) != null) {
                        sb.append("||");
                        sb.append(getColumn(res, 3));
                    }
                    if (getColumn(res, 4) != null) {
                        sb.append("||");
                        sb.append(getColumn(res, 4));
                    }
                    if (getColumn(res, 5) != null) {
                        sb.append("||");
                        sb.append(getColumn(res, 5));
                    }
                }
                arrayList.add(sb.toString());
            }
        } catch (SQLException e) {
            final String msg = "could not get Securing attributes";
            log.error(e, msg);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
        return arrayList;
    }

    /**
     *  // PROCEDURE CREATE_USER_SEC_ATTR
        // Argument Name           Type            In/Out Default?
        // ------------------------------ ----------------------- ------ --------
        P_API_VERSION_NUMBER        NUMBER          IN
        P_INIT_MSG_LIST     VARCHAR2        IN     DEFAULT
        P_SIMULATE          VARCHAR2        IN     DEFAULT
        P_COMMIT            VARCHAR2        IN     DEFAULT
        P_VALIDATION_LEVEL      NUMBER          IN     DEFAULT
        P_RETURN_STATUS     VARCHAR2        OUT
        P_MSG_COUNT         NUMBER          OUT
        P_MSG_DATA          VARCHAR2        OUT
        P_WEB_USER_ID           NUMBER          IN
        P_ATTRIBUTE_CODE        VARCHAR2        IN
        P_ATTRIBUTE_APPL_ID     NUMBER          IN
        P_VARCHAR2_VALUE        VARCHAR2        IN
        P_DATE_VALUE            DATE            IN
        P_NUMBER_VALUE      NUMBER          IN
        P_CREATED_BY            NUMBER          IN
        P_CREATION_DATE     DATE            IN
        P_LAST_UPDATED_BY       NUMBER          IN
        P_LAST_UPDATE_DATE      DATE            IN
        P_LAST_UPDATE_LOGIN     NUMBER          IN            
    */    
    private void addSecuringAttr(String userId, String secAttr) {
        final String method = "addUserSecuringAttrs";
        log.info(method);
        String attributeName = null;
        String applicationName = null;
        String value = null;

        StringTokenizer tok = new StringTokenizer(secAttr, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            attributeName = tok.nextToken();
            if (attributeName != null) {
                attributeName = attributeName.trim();
            }
            applicationName = tok.nextToken();
            if (applicationName != null) {
                applicationName = applicationName.trim();
            }
            value = tok.nextToken();
            if (value != null) {
                value = value.trim();
            }
        } else {
            final String msg = "Invalid Securing Attribute: " + secAttr;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        int intUserId = new Integer(userId).intValue();
        ResultSet rs = null; // SQL query on all users, result
        PreparedStatement pstmt = null; // statement that generates the query
        CallableStatement cstmt1 = null;
        try {
            // get attribute_code and attribute_appl_id
            // also need to get type of data value
            String attributeCode = null;
            String strAttrApplId = null;
            String dataType = null;
            String sql = "select distinct akattr.ATTRIBUTE_APPLICATION_ID,"
                    + " akattr.ATTRIBUTE_CODE, akattr.DATA_TYPE FROM FND_APPLICATION_VL fndapplvl,"
                    + " AK_ATTRIBUTES_VL akattrvl, AK_ATTRIBUTES akattr WHERE akattrvl.NAME = ?"
                    + " AND fndapplvl.application_name = ? AND akattrvl.attribute_code = akattr.attribute_code "
                    + " AND akattr.ATTRIBUTE_APPLICATION_ID = fndapplvl.application_id";
            
            String msg = "Oracle ERP: sql = ''{0}''";
            log.info(msg, sql);
            

            pstmt = getConn().prepareStatement(sql);
            pstmt.setString(1, attributeName);
            pstmt.setString(2, applicationName);
            rs = pstmt.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    strAttrApplId = rs.getString(1);
                    attributeCode = rs.getString(2);
                    dataType = rs.getString(3);
                }
                // rs closed in finally below
            }
            // pstmt closed in finally below

            sql = "{ call " + cfg.app()
                    + "icx_user_sec_attr_pub.create_user_sec_attr(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
            cstmt1 = getConn().prepareCall(sql);
            msg = "Oracle ERP: securing attribute create SQL: " + sql;
            log.ok(msg);
            

            cstmt1.setInt(1, 1);
            msg = "Oracle ERP: api_version_number = " + 1;
            log.ok(msg);
            
            cstmt1.setNull(2, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: init_msg_list = NULL";
            log.ok(msg);
            
            cstmt1.setNull(3, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: simulate = NULL";
            log.ok(msg);
            
            cstmt1.setNull(4, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: commit = NULL";
            log.ok(msg);
            
            cstmt1.setNull(5, java.sql.Types.NUMERIC);
            msg = "Oracle ERP: validation_level = NULL";
            log.ok(msg);
            
            // return_status
            cstmt1.registerOutParameter(6, java.sql.Types.VARCHAR);
            //msg_count
            cstmt1.registerOutParameter(7, java.sql.Types.NUMERIC);
            //msg_data
            cstmt1.registerOutParameter(8, java.sql.Types.VARCHAR);
            cstmt1.setInt(9, intUserId);
            msg = "Oracle ERP: web_user_id = " + intUserId;
            log.ok(msg);
            
            cstmt1.setString(10, attributeCode);
            msg = "Oracle ERP: attribute_code = " + attributeCode;
            log.ok(msg);
            
            int attrApplId = 0;
            if (strAttrApplId != null) {
                attrApplId = new Integer(strAttrApplId).intValue();
            }
            cstmt1.setInt(11, attrApplId);
            msg = "Oracle ERP: attribute_appl_id = " + strAttrApplId;
            log.ok(msg);
            
            if (dataType.equalsIgnoreCase("VARCHAR2")) {
                cstmt1.setString(12, value);
                msg = "Oracle ERP: varchar2_value = " + value;
                log.ok(msg);
                
            } else {
                cstmt1.setNull(12, Types.VARCHAR);
                msg = "Oracle ERP: varchar2_value = null";
                log.ok(msg);
                
            }

            if (dataType.equalsIgnoreCase("DATE")) {
                cstmt1.setTimestamp(13, java.sql.Timestamp.valueOf(value));
                msg = "Oracle ERP: date_value = " + value;
                log.ok(msg);
                

            } else {
                cstmt1.setNull(13, java.sql.Types.DATE);
                msg = "Oracle ERP: date_value = NULL";
                log.ok(msg);
                
            }
            if (dataType.equalsIgnoreCase("NUMBER")) {
                if (value != null) {
                    int intValue = new Integer(value).intValue();
                    cstmt1.setInt(14, intValue);
                    msg = "Oracle ERP: number_value = " + intValue;
                    log.ok(msg);
                    
                }
            } else {
                cstmt1.setNull(14, java.sql.Types.NUMERIC);
                msg = "Oracle ERP: number_value = null";
                log.ok(msg);
                
            }
            cstmt1.setInt(15, adminUserId);
            msg = "Oracle ERP: created_by = " + adminUserId;
            log.ok(msg);
            
            java.sql.Date sqlDate = getCurrentDate();
            cstmt1.setDate(16, sqlDate);
            msg = "Oracle ERP: creation_date = sysdate";
            log.ok(msg);
            
            cstmt1.setInt(17, adminUserId);
            msg = "Oracle ERP: last_updated_by = " + adminUserId;
            log.ok(msg);
            
            cstmt1.setDate(18, sqlDate);
            msg = "Oracle ERP: last_updated_date = sysdate";
            log.ok(msg);
            
            cstmt1.setInt(19, adminUserId);
            msg = "Oracle ERP: last_update_login = " + adminUserId;
            log.ok(msg);
            

            cstmt1.execute();
            // cstmt1 closed in finally below

        } catch (SQLException e) {
            final String msg = "SQL Exception:" + e.getMessage();
            log.error(e, msg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(pstmt);
            pstmt = null;
            SQLUtil.closeQuietly(cstmt1);
            cstmt1 = null;
        }
        log.ok(method);
    } // addSecuringAttr()

    /**    PROCEDURE DELETE_USER_SEC_ATTR
    Argument Name           Type            In/Out Default?
    ------------------------------ ----------------------- ------ --------
    P_API_VERSION_NUMBER        NUMBER          IN
    P_INIT_MSG_LIST     VARCHAR2        IN     DEFAULT
    P_SIMULATE          VARCHAR2        IN     DEFAULT
    P_COMMIT            VARCHAR2        IN     DEFAULT
    P_VALIDATION_LEVEL      NUMBER          IN     DEFAULT
    P_RETURN_STATUS     VARCHAR2        OUT
    P_MSG_COUNT         NUMBER          OUT
    P_MSG_DATA          VARCHAR2        OUT
    P_WEB_USER_ID           NUMBER          IN
    P_ATTRIBUTE_CODE        VARCHAR2        IN
    P_ATTRIBUTE_APPL_ID     NUMBER          IN
    P_VARCHAR2_VALUE        VARCHAR2        IN
    P_DATE_VALUE            DATE            IN
    P_NUMBER_VALUE      NUMBER          IN
    */
    private void deleteSecuringAttr(String userId, String secAttr) {
        final String method = "deleteSecuringAttr";
        log.info(method);
        String attributeName = null;
        String applicationName = null;
        String value = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(secAttr, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            attributeName = tok.nextToken();
            if (attributeName != null) {
                attributeName = attributeName.trim();
            }
            applicationName = tok.nextToken();
            if (applicationName != null) {
                applicationName = applicationName.trim();
            }
            value = tok.nextToken();
            if (value != null) {
                value = value.trim();
            }
        } else {
            final String msg = "Invalid Securing Attribute: " + secAttr;
            log.error(msg);
            throw new ConnectorException(msg);
        }
        int intUserId = new Integer(userId).intValue();
        ResultSet rs = null; // SQL query on all users, result
        PreparedStatement pstmt = null; // statement that generates the query
        CallableStatement cstmt1 = null;
        try {
            // get attribute_code and attribute_appl_id
            // also need to get type of data value
            String attributeCode = null;
            String strAttrApplId = null;
            String dataType = null;
            String sql = "select distinct akattr.ATTRIBUTE_APPLICATION_ID,"
                    + " akattr.ATTRIBUTE_CODE, akattr.DATA_TYPE FROM FND_APPLICATION_VL fndapplvl,"
                    + " AK_ATTRIBUTES_VL akattrvl, AK_ATTRIBUTES akattr WHERE akattrvl.NAME = ?"
                    + " AND fndapplvl.application_name = ? AND akattrvl.attribute_code = akattr.attribute_code "
                    + " AND akattr.ATTRIBUTE_APPLICATION_ID = fndapplvl.application_id";

            String msg = "execute sql = ''{0}''";
            log.info(msg, sql);

            pstmt = getConn().prepareStatement(sql);
            pstmt.setString(1, attributeName);
            pstmt.setString(2, applicationName);
            rs = pstmt.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    strAttrApplId = rs.getString(1);
                    attributeCode = rs.getString(2);
                    dataType = rs.getString(3);
                }
                // rs closed in finally below
            }
            // pstmt closed in finally below
            sql = "{ call " + cfg.app() + "icx_user_sec_attr_pub.delete_user_sec_attr(?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
            cstmt1 = getConn().prepareCall(sql);

            cstmt1.setInt(1, 1);
            msg = "Oracle ERP: api_version_number = " + 1;
            log.ok(msg);
            
            cstmt1.setNull(2, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: init_msg_list = NULL";
            log.ok(msg);

            cstmt1.setNull(3, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: simulate = NULL";
            log.ok(msg);

            cstmt1.setNull(4, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: commit = NULL";
            log.ok(msg);

            cstmt1.setNull(5, java.sql.Types.NUMERIC);
            msg = "Oracle ERP: validation_level = NULL";
            log.ok(msg);

            // return_status
            cstmt1.registerOutParameter(6, java.sql.Types.VARCHAR);
            //msg_count
            cstmt1.registerOutParameter(7, java.sql.Types.NUMERIC);
            //msg_data
            cstmt1.registerOutParameter(8, java.sql.Types.VARCHAR);
            
            cstmt1.setInt(9, intUserId);
            msg = "Oracle ERP: web_user_id = {0}";
            log.ok(msg, intUserId);

            cstmt1.setString(10, attributeCode);
            msg = "Oracle ERP: attribute_code = {0}";
            log.ok(msg, attributeCode);

            int attrApplId = 0;
            if (strAttrApplId != null) {
                attrApplId = new Integer(strAttrApplId).intValue();
            }
            cstmt1.setInt(11, attrApplId);         
            msg = "Oracle ERP: attribute_appl_id = {0}";
            log.ok(msg, strAttrApplId);
           
            if (dataType.equalsIgnoreCase("VARCHAR2")) {
                cstmt1.setString(12, value);                
                msg = "Oracle ERP: varchar2_value  = {0}";
                log.ok(msg, value);
                
            } else {
                cstmt1.setNull(12, Types.VARCHAR);                
                msg = "Oracle ERP: varchar2_value = null";
                log.ok(msg);
                
            }

            if (dataType.equalsIgnoreCase("DATE")) {
                cstmt1.setTimestamp(13, java.sql.Timestamp.valueOf(value));                
                msg = "Oracle ERP: date_value  = {0}";
                log.ok(msg, value);                

            } else {
                cstmt1.setNull(13, java.sql.Types.DATE);
                msg = "Oracle ERP: date_value = NULL";
                log.ok(msg);                
            }
            if (dataType.equalsIgnoreCase("NUMBER")) {
                if (value != null) {
                    int intValue = new Integer(value).intValue();
                    cstmt1.setInt(14, intValue);
                    msg = "Oracle ERP: number_value = " + intValue;
                    log.ok(msg, value);                       
                }
            } else {
                cstmt1.setNull(14, java.sql.Types.NUMERIC);                
                msg = "Oracle ERP: number_value = null";
                log.ok(msg);                 
            }
            cstmt1.execute();
            // cstmt1 closed in finally below

        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(conn);

            final String msg = "error in statement";
            log.error(e, msg);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(pstmt);
            pstmt = null;
            SQLUtil.closeQuietly(cstmt1);
            cstmt1 = null;
        }
        log.ok(method);
    }

}
