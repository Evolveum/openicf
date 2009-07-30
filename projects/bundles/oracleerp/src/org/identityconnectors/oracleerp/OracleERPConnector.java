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

import static org.identityconnectors.oracleerp.OracleERPUtil.MSG_ACCOUNT_UID_REQUIRED;
import static org.identityconnectors.oracleerp.OracleERPUtil.MSG_UNKNOWN_OPERATION_TYPE;
import static org.identityconnectors.oracleerp.OracleERPUtil.RESP_NAMES;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
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
        UpdateOp, CreateOp, TestOp, SchemaOp, ScriptOnConnectorOp {

    /**
     * Setup logging for the {@link OracleERPConnector}.
     */
    static final Log log = Log.getLog(OracleERPConnector.class);

    /**
     * Place holder for the {@link Configuration} passed into the init() method {@link OracleERPConnector#init}.
     */
    private OracleERPConfiguration cfg;

    /**
     * Gets the Configuration context for this connector.
     * @return connector configuration
     */
    public Configuration getConfiguration() {
        return this.cfg;
    }
    /**
     * getter method
     * @return OracleERPConfiguration
     */
    public OracleERPConfiguration getCfg() {
        return cfg;
    }    

    /**
     * Place holder for the {@link Connection} passed into the setConnection() callback
     * {@link ConnectionFactory#setConnection(Connection)}.
     */
    private OracleERPConnection conn; 
 
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, final String username, final GuardedString password,
            final OperationOptions options) {
        Assertions.nullCheck(objectClass, "objectClass");
        Assertions.nullCheck(username, "username");        
        Assertions.nullCheck(password, "password");

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationAutenticate(conn, cfg).authenticate(objectClass, username, password, options);
        } 
        throw new IllegalArgumentException(cfg.getMessage(MSG_UNKNOWN_OPERATION_TYPE, objectClass.toString()));
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

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {            
            return new AccountOperationCreate(conn, cfg).create(oclass, attrs, options);
        } else if (oclass.is(RESP_NAMES)) {
        // TODO create resp names
        }

        throw new IllegalArgumentException(cfg.getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));

    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {

        Assertions.nullCheck(oclass, "oclass");

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            return new RespNamesOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            return new ResponsibilitiesOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.DIRECT_RESPS)) {
            return new ResponsibilitiesOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.INDIRECT_RESPS)) {
            return new ResponsibilitiesOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.APPS)) {
            return new ApplicationOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.AUDITOR_RESPS)) {
            return new AuditorOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.SEC_GROUPS)) {
            return new SecuringGroupsOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.SEC_ATTRS)) {
            return new SecuringAttributesOperationSearch(conn, cfg).createFilterTranslator(oclass, options);
        }

        throw new IllegalArgumentException(cfg.getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        Assertions.nullCheck(objClass, "oclass");
        Assertions.nullCheck(uid, "uid");
          
        
        if ( uid == null || uid.getUidValue() == null ) {
            throw new IllegalArgumentException(cfg.getMessage(MSG_ACCOUNT_UID_REQUIRED));
        }   
        
        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            new AccountOperationDelete(conn, cfg).delete(objClass, uid, options);
            return;
        }  else if (objClass.is(RESP_NAMES)) {
            // TODO delete rsp names
        }

        throw new IllegalArgumentException(cfg.getMessage(MSG_UNKNOWN_OPERATION_TYPE, objClass.toString()));
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
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {

        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(handler, "handler");

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            new AccountOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            new RespNamesOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.RESPS)) {
            new ResponsibilitiesOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.DIRECT_RESPS)) {
            new ResponsibilitiesOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.INDIRECT_RESPS)) {
            new ResponsibilitiesOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.APPS)) {
            new ApplicationOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.AUDITOR_RESPS)) {
            new AuditorOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.SEC_GROUPS)) {
            new SecuringGroupsOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.SEC_ATTRS)) {
            new SecuringAttributesOperationSearch(conn, cfg).executeQuery(oclass, where, handler, options);
            return;
        }

        throw new IllegalArgumentException(cfg.getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        final ClassLoader loader = getClass().getClassLoader();

        /*
         * Build the actionContext to pass it to the script according the documentation
         */
        final Map<String, Object> actionContext = new HashMap<String, Object>();
        final Map<String, Object> scriptArguments = request.getScriptArguments();
        final String nameValue = ((Uid) scriptArguments.get(Uid.NAME)).getUidValue();
        final GuardedString password = ((GuardedString) scriptArguments.get(OperationalAttributes.PASSWORD_NAME));

        actionContext.put("conn", conn.getConnection()); //The real connection
        actionContext.put("action", scriptArguments.get("operation")); // The action is the operation name createUser/updateUser/deleteUser/disableUser/enableUser
        actionContext.put("timing", scriptArguments.get("timing")); // The timming before / after
        actionContext.put("attributes", scriptArguments.get("attributes")); // The attributes
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

        final String scriptLanguage = request.getScriptLanguage();
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
        if (cfg.getSchema() == null) {
            cfg.setSchema(new OracleERPOperationSchema(conn, cfg).schema());
        }
        return cfg.getSchema();
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
    public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        Assertions.nullCheck(objClass, "oclass");
        Assertions.nullCheck(uid, "uid");
        Assertions.nullCheck(replaceAttributes, "replaceAttributes");
        if (replaceAttributes.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }
        
        if ( uid == null || uid.getUidValue() == null ) {
            throw new IllegalArgumentException(cfg.getMessage(MSG_ACCOUNT_UID_REQUIRED));
        }        

        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationUpdate(conn, cfg).update(objClass, uid, replaceAttributes, options);
        } else if (objClass.is(RESP_NAMES)) {
            // TODO update resp names
        }

        throw new IllegalArgumentException(cfg.getMessage(MSG_UNKNOWN_OPERATION_TYPE, objClass.toString()));
    }


    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see Connector#init
     */
    public void init(Configuration configuration) {
        /*  
         * RA: startConnection(): 
         * TODO: convert _dbu = new OracleDBUtil(); _dbu.setUpArgs(_resource)
         *  initUserName(), is implemented in OracleERPConfiguration: getSchemaId
         *  _ctx = makeConnection(result);
         */
        log.info("Init using configuration {0}", configuration);
        this.cfg = (OracleERPConfiguration) configuration;
        this.conn = OracleERPConnection.createOracleERPConnection(cfg);
        log.info("createOracleERPConnection");
        
        cfg.setUserId(OracleERPUtil.getUserId(conn, cfg, cfg.getUser()));
        log.info("Init: for user {0} the configUserId is {1}", cfg.getUser(), cfg.getUserId());
        
        initResponsibilities();        
        initFndGlobal();
        schema();
        log.ok("init");
    }
    
    
    /**
     * Init connector`s call
     */
    private void initFndGlobal() {
        final String respId = cfg.getRespId();
        final String respApplId = cfg.getRespApplId();
        log.info("Init global respId={0}, respApplId={1}", respId, respApplId);
        //Real initialize call
        if (StringUtil.isNotBlank(cfg.getUserId()) && StringUtil.isNotBlank(respId)
                && StringUtil.isNotBlank(respApplId)) {
            CallableStatement cs = null;
            try {
                final String sql = "call " + cfg.app() + "FND_GLOBAL.APPS_INITIALIZE(?,?,?)";
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE({1}, {2}, {3}) called.";
                log.ok(msg, cfg.app(), cfg.getUserId(), respId, respApplId);
                List<SQLParam> pars = new ArrayList<SQLParam>();
                pars.add(new SQLParam("userId", cfg.getUserId(), Types.VARCHAR));
                pars.add(new SQLParam("respId", respId, Types.VARCHAR));
                pars.add(new SQLParam("respAppId", respApplId, Types.VARCHAR));

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
            log.ok("Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE() NOT called.", cfg.app());
        }
    }
    
    /**
     * Init the responsibilities
     * 
     * @param configUserId
     *            configUserId
     */
    private void initResponsibilities() {
        log.info("initResponsibilities");
        cfg.setNewResponsibilityViews(getNewResponsibilityViews());

        if (cfg.isNewResponsibilityViews()) {
            cfg.setDescrExists(getDescriptionExiests());
        }

        // three pieces of data need for apps_initialize()
        final String auditResponsibility = cfg.getAuditResponsibility();
        log.info("auditResponsibility = {0}", auditResponsibility);
        
        if (StringUtil.isNotBlank(auditResponsibility) && StringUtil.isNotBlank(cfg.getUserId())) {
            final String view = cfg.app() + ((cfg.isNewResponsibilityViews()) ? OracleERPUtil.RESPS_ALL_VIEW : OracleERPUtil.RESPS_TABLE);
            final String sql = "select responsibility_id, responsibility_application_id from "
                    + view
                    + " where user_id = ? and "
                    + "(responsibility_id,responsibility_application_id) = (select responsibility_id,application_id from "
                    + cfg.app()
                    + "fnd_responsibility_vl where responsibility_name = ?)";

            final String msg = "Oracle ERP SQL: RESP_ID = {1}, RESP_APPL_ID = {2}";

            ArrayList<SQLParam> params = new ArrayList<SQLParam>();
            params.add(new SQLParam("userId", cfg.getUserId()));
            params.add(new SQLParam("responsibilityName", auditResponsibility));
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                log.info("Select responsibility for user_id: {0}, and audit responsibility {1}", cfg.getUserId(),
                        auditResponsibility);
                ps = conn.prepareStatement(sql, params);
                rs = ps.executeQuery();
                if (rs != null) {
                    if (rs.next()) {
                        cfg.setRespId(rs.getString(1));
                        cfg.setRespApplId(rs.getString(2));
                    }
                }

                log.ok(msg, cfg.getRespId(), cfg.getRespApplId());
            } catch (SQLException e) {
                SQLUtil.rollbackQuietly(conn);
                log.error(e, msg, cfg.getRespId(), cfg.getRespApplId());
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
     * @return the boolean value
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
                log.ok("newResponsibilityViews: true");
                return true;
            }
        } catch (SQLException e) {
            log.error(e, sql);
            SQLUtil.rollbackQuietly(conn);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        log.ok("newResponsibilityViews: true");
        return false;
    }
}
