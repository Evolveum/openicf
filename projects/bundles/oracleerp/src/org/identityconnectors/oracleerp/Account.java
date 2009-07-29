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

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.contract.exceptions.ObjectNotFoundException;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Main implementation of the Account Object Class
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class Account implements CreateOp, UpdateOp, DeleteOp, SearchOp<FilterWhereBuilder>, ColumnNameResolver {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(Account.class);
    
    /**
     * The instance or the parent object
     */
    private OracleERPConnection conn = null;
    
    /**
     * The instance or the parent object
     */
    private OracleERPConfiguration cfg = null;    
      

    /**
     * Temp oracle connector
     */
    private OracleERPConnector co;

    /**
     * The account
     * @param conn 
     * @param cfg 
     * @param co 
     */
    public Account(OracleERPConnection conn, OracleERPConfiguration cfg, OracleERPConnector co) {
        this.conn = conn;
        this.cfg = cfg;
        this.co = co;
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
     * @param oclass ObjectClass
     * @param attrs Set<Attribute>
     * @param options OperationOptions
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        log.ok("create");               
        final Name nameAttr = AttributeUtil.getNameFromAttributes(attrs);
        if( nameAttr == null || nameAttr.getNameValue() == null) {
            throw new IllegalArgumentException(cfg.getMessage(MSG_ACCOUNT_NAME_REQUIRED));
        }        
        final String name = nameAttr.getNameValue();

        attrs = CollectionUtil.newSet(attrs); //modifiable set       
        if (AttributeUtil.find(OWNER, attrs) == null) {
            attrs.add(AttributeBuilder.build(OWNER, CUST));
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

            final String msg = "Create user account {0} : {1}";
            log.ok(msg, name, sql);
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
            } catch (SQLException e) {
                log.error(e, name, sql);
                SQLUtil.rollbackQuietly(conn);
                throw new IllegalStateException(cfg.getMessage(MSG_ACCOUNT_NOT_CREATE, name), e);
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
            co.getRespNames().updateUserResponsibilities( resp, name);
        } else if ( directResp != null ) {
            co.getRespNames().updateUserResponsibilities( directResp, name);
        }
        // update securing attributes
        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            co.getSecAttrs().updateUserSecuringAttrs(secAttr, name);
        }
        
        conn.commit();     
        return new Uid(name);
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        //TODO possibly create other account ColumnNameResolver
        return new OracleERPFilterTranslator(oclass, options, AccountSQL.FND_USER_COLS, this);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {       
        final String sql = "{ call "+cfg.app()+"fnd_user_pkg.disableuser(?) }";
        log.info("delete");
        CallableStatement cs = null;
        try {
            cs = conn.prepareCall(sql);
            final String identity = AttributeUtil.getAsStringValue(uid).toUpperCase();
            cs.setString(1, identity);
            cs.execute();
            
            conn.commit();
            // No Result ??
        } catch (SQLException e) {
            if (e.getErrorCode() == 20001 || e.getErrorCode() == 1403) {
                final String msg = "SQL Exception trying to delete Oracle user '{0}' ";
                SQLUtil.rollbackQuietly(conn);
                throw new IllegalArgumentException(MessageFormat.format(msg, uid),e);
            } else {
              throw new UnknownUidException(uid, objClass);
            }
        } finally {
            SQLUtil.closeQuietly(cs);
            cs = null;
        }
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        final String method = "executeQuery";
        
        final String tblname = cfg.app() + "fnd_user";
        final Set<AttributeInfo> ais = getAttributeInfos(cfg.getSchema(), ObjectClass.ACCOUNT_NAME);
        final Set<String> attributesToGet = getAttributesToGet(options, ais);        
        final Set<String> fndUserColumnNames = getColumnNamesToGet(attributesToGet);
        final Set<String> perPeopleColumnNames = CollectionUtil.newSet(fndUserColumnNames);
        final String filterId = getFilterId(where);

        fndUserColumnNames.retainAll(AccountSQL.FND_USER_COLS);
        perPeopleColumnNames.retainAll(AccountSQL.PER_PEOPLE_COLS);
        
        // For all user query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, fndUserColumnNames );
        String sqlSelect = query.getSQL();
        
        if(StringUtil.isNotBlank(cfg.getAccountsIncluded())) {
            sqlSelect += whereAnd(sqlSelect, cfg.getAccountsIncluded());
        } else if( cfg.isActiveAccountsOnly()) {
            sqlSelect += whereAnd(sqlSelect, ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE);
        }
        
        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                AttributeMergeBuilder amb = new AttributeMergeBuilder(attributesToGet);
                final Map<String, SQLParam> columnValues = SQLUtil.getColumnValues(result);
                final SQLParam userNameParm = columnValues.get(USER_NAME);
                final String userName = (String) userNameParm.getValue();
                final boolean getAuditorData = userNameParm.getValue().toString().equals(filterId);               
                // get users account attributes
                this.buildAccountObject(amb, columnValues);
                
                // if person_id not null and employee_number in schema, return employee_number
                buildPersonDetails(amb, columnValues, perPeopleColumnNames);
                
                // get users responsibilities only if if resp || direct_resp in account attribute
                co.getRespNames().buildResponsibilitiesToAccountObject(amb, userName);
                // get user's securing attributes
                co.getSecAttrs().buildSecuringAttributesToAccountObject(amb, userName);

                //Auditor data for get user only
                log.info("get auditor data: {0}", getAuditorData);
                if (getAuditorData) {
                    co.getRespNames().buildAuditorDataObject(amb, userName);
                }
                
                // create the connector object..
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(ObjectClass.ACCOUNT);
                bld.addAttributes(amb.build());
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
            conn.commit();
            log.ok(method);
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
    }

    /**
     * @param where
     * @return
     */
    private String getFilterId(FilterWhereBuilder where) {
        String filterId = null;
        if (where != null) {
            for (SQLParam sqlp : where.getParams()) {
                if (sqlp.getName().equalsIgnoreCase(USER_NAME)) {
                    filterId = sqlp.getValue().toString();
                }
            }
        }
        return filterId;
    }
    
    /**
     * @param attributeName
     * @return the columnName
     */
    public String getFilterColumnName(String attributeName) {
        if(Name.NAME.equalsIgnoreCase(attributeName)) { 
            return USER_NAME;
        } else if (Uid.NAME.equalsIgnoreCase(attributeName)) { 
            return USER_NAME;
        }
        //We need to filter just a known columns
        if ( AccountSQL.FND_USER_COLS.contains(attributeName)) {
            return attributeName;
        }

        return null;
    }    
    
    /**
     * @param attributeName
     * @return the columnName
     */
    public String getColumnName(String attributeName) {
        if (attributeName.equalsIgnoreCase(PERSON_FULLNAME)) {
            return FULL_NAME;
        }
        //We need to filter just a known columns
        if ( AccountSQL.PER_PEOPLE_COLS.contains(attributeName)) {
            return attributeName;
        }       

        return getFilterColumnName(attributeName);
    }    

    /**
     * @param attributesToGet from application
     * @return the set of the column names
     */
    private Set<String> getColumnNamesToGet(Set<String> attributesToGet) {        
        Set<String> columnNamesToGet = CollectionUtil.newCaseInsensitiveSet();

        // Replace attributes to quoted columnNames
        for (String attributeName :  attributesToGet) {
            final String columnName = getColumnName(attributeName);
            if ( columnName != null) {
                columnNamesToGet.add(columnName);
            }            
        }
        //We always wont to have user id and user name
        columnNamesToGet.add(USER_NAME);

        log.ok("columnNamesToGet {0}", columnNamesToGet);
        return columnNamesToGet;
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
     * @param oclass ObjectClass
     * @param attrs Set<Attribute>
     * @param options OperationOptions
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        final String name = uid.getUidValue();
        
        if ( uid == null || uid.getUidValue() == null ) {
            throw new IllegalArgumentException(cfg.getMessage(MSG_ACCOUNT_UID_REQUIRED));
        }
        
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
                disable(objclass, name, options);
            } else {
                enable(objclass, name, options);
            }
        }
        
        // Get the User values
        final AccountSQLBuilder asb = new AccountSQLBuilder(cfg.app(), false).build(objclass, attrs, options);
        if ( !asb.isEmpty() ) {
            // Run the create call, new style is using the defaults
            CallableStatement cs = null;
            final String sql = asb.getUserCallSQL();
            final String msg = "Update user account {0} : {1}";
            log.ok(msg, name, sql);
            try {
                // Create the user
                cs = conn.prepareCall(sql, asb.getUserSQLParams());
                cs.execute();
            } catch (SQLException e) {
                log.error(e, msg, name, sql);
                SQLUtil.rollbackQuietly(conn);
                throw new ObjectNotFoundException(e);
            } finally {
                SQLUtil.closeQuietly(cs);
            }            
        }
                        
        // Update responsibilities
        final Attribute resp = AttributeUtil.find(RESPS, attrs);
        final Attribute directResp = AttributeUtil.find(DIRECT_RESPS, attrs);
        if ( resp != null ) {
            co.getRespNames().updateUserResponsibilities( resp, name);
        } else if ( directResp != null ) {
            co.getRespNames().updateUserResponsibilities( directResp, name);
        }

        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            co.getSecAttrs().updateUserSecuringAttrs(secAttr, name);
        }

        conn.commit();
        //Return new UID
        return new Uid(name);
    }

    /**
     * @param objclass
     * @param userName
     * @param options
     */
    private void enable(ObjectClass objclass, String userName, OperationOptions options) {
        final String method = "realEnable";
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
        } catch (SQLException e) {
            final String msg = cfg.getMessage(MSG_COULD_NOT_ENABLE_USER, userName);
            log.error(e, msg);
            SQLUtil.rollbackQuietly(conn);
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
    private void disable(ObjectClass objclass, String name, OperationOptions options) {
        final String sql = "{ call "+cfg.app()+"fnd_user_pkg.disableuser(?) }";
        log.info(sql);
        CallableStatement cs = null;
        try {
            cs = conn.prepareCall(sql);
            cs.setString(1, name);
            cs.execute();
            // No Result ??
        } catch (SQLException e) {
            final String msg = "SQL Exception trying to disable Oracle user '{0}' ";
            SQLUtil.rollbackQuietly(conn);
            throw new IllegalArgumentException(MessageFormat.format(msg, name),e);
        } finally {
            SQLUtil.closeQuietly(cs);
            cs = null;
        }

    }
    

    /**
     * @param bld
     * @param columnValues
     * @param columnNames 
     */
    private void buildPersonDetails(AttributeMergeBuilder bld, final Map<String, SQLParam> columnValues ,
            Set<String> personColumns) {
        
        if (columnValues == null || columnValues.get(EMP_ID) == null) {
            // No personId(employId)
            log.ok("buildPersonDetails: No personId(employId)");
            return;
        }
        final BigDecimal personId = (BigDecimal) columnValues.get(EMP_ID).getValue();
        if (personId == null ) {
            log.ok("buildPersonDetails: Null personId(employId)");
            return;
        }
        log.info("buildPersonDetails for personId: {0}", personId );
        
        //Names to get filter
        final String tblname = cfg.app()+ "PER_PEOPLE_F";

        if (personColumns.isEmpty()) {
            // No persons column required
            log.ok("No persons column To Get");
            return;
        }
        log.ok("personColumns {0} To Get", personColumns);
        
        // For all account query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, personColumns);
        final FilterWhereBuilder where = new FilterWhereBuilder();
        where.addBind(new SQLParam(PERSON_ID, personId, Types.DECIMAL), "=");
        query.setWhere(where);        
        
        final String sql = query.getSQL();
        String msg = "Oracle ERP: sql = ''{0}''";
        log.info(msg, sql);

            ResultSet result = null; // SQL query on person_id
            PreparedStatement statement = null; // statement that generates the query
            try {
                statement = conn.prepareStatement(query);
                result = statement.executeQuery();
                if (result != null) {
                    log.ok("executeQuery {0}", query.getSQL());
                    if (result.next()) {
                        final Map<String, SQLParam> personValues = SQLUtil.getColumnValues(result);
                        // get users account attributes
                        this.buildAccountObject(bld, personValues);
                        log.ok("Person values {0} from result set ", personValues);
                    }
                }
               
            } catch (SQLException e) {
                String emsg = e.getMessage();
                msg = "Caught SQLException when executing: ''{0}'': {1}";
                log.error(msg, sql, emsg);
                SQLUtil.rollbackQuietly(conn);
                throw new ConnectorException(msg, e);
            } finally {
                SQLUtil.closeQuietly(result);
                result = null;
                SQLUtil.closeQuietly(statement);
                statement = null;
            }
        }

    /**
     * Construct a connector object
     * <p>Taking care about special attributes</p>
     *  
     * @param attributeSet from the database table
     * @param columnValues 
     * @throws SQLException 
     */
    private void buildAccountObject(AttributeMergeBuilder amb, Map<String, SQLParam> columnValues) throws SQLException {
        final String method = "buildAccountObject";
        log.info(method);
        for (Map.Entry<String, SQLParam> val : columnValues.entrySet()) {
            final String columnName = val.getKey().toLowerCase();
            final SQLParam param = val.getValue();
            // Map the special
            if (columnName.equalsIgnoreCase(USER_NAME)) {
                if (param == null || param.getValue() == null) {
                    String msg = "Name cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                amb.addAttribute(Name.NAME, param.getValue().toString());
                amb.addAttribute(Uid.NAME, param.getValue().toString());
            } else if (columnName.equalsIgnoreCase(UNENCRYPT_PWD)) {
                // No Password in the result object
            } else if (columnName.equalsIgnoreCase(OWNER)) {
                // No Owner in the result object
            } else {
                //Convert the data type and create attribute from it.
                final Object value = SQLUtil.jdbc2AttributeValue(param.getValue());
                if (columnName.equalsIgnoreCase(FULL_NAME)) {
                    amb.addAttribute(PERSON_FULLNAME, value);
                } else {
                    amb.addAttribute(columnName, value);
                }
            }
        }
    }
}
