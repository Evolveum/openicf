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

import static org.identityconnectors.oracleerp.OracleERPUtil.MSG_UNKNOWN_OPERATION_TYPE;
import static org.identityconnectors.oracleerp.OracleERPUtil.RESP_NAMES;

import java.sql.CallableStatement;
import java.sql.Connection;
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
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
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
public class OracleERPConnector implements Connector, AuthenticateOp, DeleteOp, SearchOp<FilterWhereBuilder>,
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
    public String getConfigUserId() {
        return configUserId;
    }


    /**
     * User id from cfg.User
     */
    private String configUserId = "";
    
    

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
            st = getConn().prepareCall(sql);
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
            final Uid uid = getAccount().create(oclass, attrs, options);
            
            //doAfterCreateActionScripts(oclass, attrs, options);
            return uid;
        } else if (oclass.equals(RESP_NAMES)) {
        //    final Uid uid = respNames.create(oclass, attrs, options);
        //    return uid;
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));

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
    public void delete(ObjectClass oclass, Uid uid, OperationOptions options) {
        if (oclass.equals(ObjectClass.ACCOUNT)) {
            getAccount().delete(oclass, uid, options);
            return;
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }

    /**
     * Disposes of the {@link OracleERPConnector}'s resources.
     * 
     * @see Connector#dispose()
     */
    public void dispose() {
        conn.dispose();
        cfg = null;
        conn = null;
        account = null;
        respNames = null;
        secAttrs = null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {

        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(handler, "handler");

        if (oclass.equals(ObjectClass.ACCOUNT)) {
            getAccount().executeQuery(oclass, where, handler, options);
            return;    
        } else if (oclass.equals(OracleERPUtil.RESP_NAMES_OC)) {
            getRespNames().executeQuery(oclass, where, handler,options);
            return;
        } else if (oclass.equals(OracleERPUtil.RESP_OC)) { //OK
            getRespNames().executeQuery(oclass, where, handler,options);
            return;
        } else if (oclass.equals(OracleERPUtil.DIRECT_RESP_OC)) { //OK
            getRespNames().executeQuery(oclass, where, handler,options);
            return;
        } else if (oclass.equals(OracleERPUtil.INDIRECT_RESP_OC)) { //OK
            getRespNames().executeQuery(oclass, where, handler,options);
            return;
        } else if (oclass.equals(OracleERPUtil.APPS_OC)) {
            getRespNames().executeQuery(oclass, where, handler,options);
        } else if (oclass.equals(OracleERPUtil.AUDITOR_RESPS_OC)) { // ok
            getRespNames().executeQuery(oclass, where, handler,options);
        } else if (oclass.equals(OracleERPUtil.SEC_GROUPS_OC)) {
            getSecAttrs().executeQuery(oclass, where, handler,options);
        } else if (oclass.equals(OracleERPUtil.SEC_ATTRS_OC)) {
            getSecAttrs().executeQuery(oclass, where, handler,options);
        }
        
        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
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
        this.conn = OracleERPConnection.createOracleERPConnection(getCfg());

        /*  
         * RA: startConnection(): 
         *  setNewRespView();
         *  initFndGlobal();
         */
        configUserId = OracleERPUtil.getUserId(this, getCfg().getUser());
        log.info("Init Responsibilities for config user {0}", configUserId);
        getRespNames().initResponsibilities(getConfigUserId());
        log.info("Init global");
        initFndGlobal();
        log.ok("init");
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
            scriptArguments.put("conn", getConn().getConnection()); //The real connection
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
        final String nameValue = ((Uid) scriptArguments.get(Uid.NAME)).getUidValue();
        final GuardedString password = ((GuardedString) scriptArguments.get(OperationalAttributes.PASSWORD_NAME));

        actionContext.put("conn", getConn().getConnection()); //The real connection
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
        getAccount().schema(schemaBld);

        // The Responsibilities
        getRespNames().schema(schemaBld);
        
        // The securing attributes
        getSecAttrs().schema(schemaBld);
        return schemaBld.build();
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.TestOp#test()
     */
    public void test() {
        getCfg().validate();
        getConn().test();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid update(ObjectClass oclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        if (replaceAttributes.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }
        if (oclass.equals(ObjectClass.ACCOUNT)) {
            //doBeforeUpdateActionScripts(oclass, attrs, options);
            uid = getAccount().update(oclass, uid, replaceAttributes, options);
            //doAfterUpdateActionScripts(oclass, attrs, options);
            return uid;
        } 

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }

    /**
     * Init connector`s call 
     */
    private void initFndGlobal() {
        final String respId = getRespNames().getRespId();        
        final String respApplId = getRespNames().getRespApplId();
        //Real initialize call
        if (StringUtil.isNotBlank(getConfigUserId()) && StringUtil.isNotBlank(respId)
                && StringUtil.isNotBlank(respApplId)) {
            CallableStatement cs = null;
            try {
                final String sql = "call " + app() + "FND_GLOBAL.APPS_INITIALIZE(?,?,?)";
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE({1}, {2}, {3}) called.";
                log.ok(msg, app(), getConfigUserId(), respId, respApplId);
                List<SQLParam> pars = new ArrayList<SQLParam>();
                pars.add(new SQLParam("userId", getConfigUserId(), Types.VARCHAR));
                pars.add(new SQLParam("respId", respId, Types.VARCHAR));
                pars.add(new SQLParam("respAppId", respApplId, Types.VARCHAR));

                cs = getConn().prepareCall(sql, pars);
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
     * The application id from the user
     * see the bug id. 19352
     * @return The "APPL." or empty, if noSchemaId is true
     */
    public String app() {
        if(getCfg().isNoSchemaId()) return "";
        return getCfg().getUser().trim().toUpperCase()+".";
    }
}
