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

import static org.identityconnectors.oracleerp.OracleERPUtil.RESP_NAMES;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Accessor for the respNames property
     * @return the respNames
     */
    public ResponsibilityNames getRespNames() {
        return respNames;
    }

    /**
     * The UserSecuringAttrs delegate instance
     */
    private UserSecuringAttrs secAttrs = UserSecuringAttrs.getInstance(this);


    /**
     * Accessor for the userSecuringAttrs property
     * @return the userSecuringAttrs
     */
    public UserSecuringAttrs getSecAttrs() {
        return secAttrs;
    }

    /**
     * Accessor for the account property
     * @return the account
     */
    public Account getAccount() {
        return account;
    }


    /**
     * Accessor for the userId property
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }


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
        final String sql = "{ " + MessageFormat.format(SQL, app()) + " }";
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
        } else if (oclass.equals(RESP_NAMES)) {
        //    final Uid uid = respNames.create(oclass, attrs, options);
        //    return uid;
        }

        throw new IllegalArgumentException("Create operation requires one 'ObjectClass' of "
                + "account, responsibilityNames");
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        //TODO add other filter translators, if required
        return getAccount().createFilterTranslator(oclass, options);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        if (objClass.equals(ObjectClass.ACCOUNT)) {
            account.delete(objClass, uid, options);
            return;
        }

        throw new IllegalArgumentException("Delete operation requires an 'ObjectClass' of type account");
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

        respNames.initResponsibilities();

        this.userId = OracleERPUtil.getUserId(this, this.cfg.getUser());

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
        schemaBld.defineObjectClass(getAccount().getSchema());
        // The Responsibilities
        schemaBld.defineObjectClass(getSecAttrs().getSchema());
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
        if (replaceAttributes.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }
        if (objclass.equals(ObjectClass.ACCOUNT)) {
            //doBeforeUpdateActionScripts(oclass, attrs, options);
            uid = account.update(objclass, uid, replaceAttributes, options);
            //doAfterUpdateActionScripts(oclass, attrs, options);
            return uid;
        } else if (objclass.equals(RESP_NAMES)) {
           // uid = respNames.update(objclass, uid, replaceAttributes, options);
        }

        throw new IllegalArgumentException("Update operation requires one 'ObjectClass' of "
                + "account,responsibilityNames");
    }

    /**
     * Init connector`s call 
     */
    private void initFndGlobal() {
        final String respId = getRespNames().getRespId();;        
        final String respApplId = getRespNames().getRespApplId();
        //Real initialize call
        if (StringUtil.isNotBlank(this.userId) && StringUtil.isNotBlank(respId)
                && StringUtil.isNotBlank(respApplId)) {
            CallableStatement cs = null;
            try {
                final String sql = "call " + app() + "FND_GLOBAL.APPS_INITIALIZE(?,?,?)";
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE({1}, {2}, {3}) called.";
                log.ok(msg, app(), this.userId, respId, respApplId);
                List<SQLParam> pars = new ArrayList<SQLParam>();
                pars.add(new SQLParam(this.userId, Types.VARCHAR));
                pars.add(new SQLParam(respId, Types.VARCHAR));
                pars.add(new SQLParam(respApplId, Types.VARCHAR));

                cs = conn.prepareCall(sql, pars);
                cs.execute();
                // Result ?
                // cstmt1 closed in finally below

            } catch (SQLException e) {
                final String msg = "Oracle ERP: Failed to call {0}FND_GLOBAL.APPS_INITIALIZE()";
                log.error(e, msg, app());
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(cs);
                cs = null;
            }
        } else {
            log.ok("Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE() NOT called.", app());
        }
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
     *
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
                b.append(" from " + app() + "fnd_responsibility_vl fr, " + app()
                        + "fnd_profile_option_values fpov, " + app() + "fnd_profile_options fpo");
                b.append(" , " + app() + "fnd_profile_options_vl fpo1, " + app()
                        + "hr_organization_units hou1, " + app() + "gl_sets_of_books gsob");
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
     * The application id from the user
     * see the bug id. 19352
     * @return The "APPL." or empty, if noSchemaId is true
     */
    public String app() {
        if(getCfg().isNoSchemaId()) return "";
        return getCfg().getUser().trim().toUpperCase()+".";
    }
}
