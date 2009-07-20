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
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
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
public class Account implements OracleERPColumnNameResolver, CreateOp, UpdateOp, DeleteOp, SearchOp<FilterWhereBuilder> {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(Account.class);

    // The SQL call update function SQL template
    static final String FND_USER_CALL = "fnd_user_pkg.";

    /**
     * The map of column name parameters mapping
     */
    static final List<Pair<String, String>> CALL_PARAMS = new ArrayList<Pair<String,String>>();

    /**
     * The column names to get
     */
    static final Set<String> FND_USER_COLS = CollectionUtil.newCaseInsensitiveSet();

    /**
     * The column names to get
     */
    static final Set<String> PER_PEOPLE_COLS = CollectionUtil.newCaseInsensitiveSet();

    /**
     * Initialization of the map
     */
    static {       
        FND_USER_COLS.add(USER_ID); //0 not createble, updatable 
        FND_USER_COLS.add(USER_NAME);//1
        FND_USER_COLS.add(SESS_NUM); //4     
        FND_USER_COLS.add(START_DATE); //5     
        FND_USER_COLS.add(END_DATE); //6     
        FND_USER_COLS.add(LAST_LOGON_DATE); //7     
        FND_USER_COLS.add(DESCR); //8     
        FND_USER_COLS.add(PWD_DATE);//9     
        FND_USER_COLS.add(PWD_ACCESSES_LEFT); //10     
        FND_USER_COLS.add(PWD_LIFE_ACCESSES); //11    
        FND_USER_COLS.add(PWD_LIFE_DAYS); //12     
        FND_USER_COLS.add(EMP_ID); //13     
        FND_USER_COLS.add(EMAIL); //14     
        FND_USER_COLS.add(FAX); //15     
        FND_USER_COLS.add(CUST_ID); //16     
        FND_USER_COLS.add(SUPP_ID); //17     
        FND_USER_COLS.add(PERSON_PARTY_ID); //18);
        
        PER_PEOPLE_COLS.add(EMP_NUM);
        PER_PEOPLE_COLS.add(NPW_NUM);
        PER_PEOPLE_COLS.add(FULL_NAME);

        CALL_PARAMS.add( new Pair<String, String>(USER_NAME, "x_user_name => {0}")); //1
        CALL_PARAMS.add( new Pair<String, String>(OWNER, "x_owner => upper({0})")); //2   write only   
        CALL_PARAMS.add( new Pair<String, String>(UNENCRYPT_PWD, "x_unencrypted_password => {0}"));//3 write only      
        CALL_PARAMS.add( new Pair<String, String>(SESS_NUM, "x_session_number => {0}")); //4     
        CALL_PARAMS.add( new Pair<String, String>(START_DATE, "x_start_date => {0}")); //5     
        CALL_PARAMS.add( new Pair<String, String>(END_DATE, "x_end_date => {0}")); //6     
        CALL_PARAMS.add( new Pair<String, String>(LAST_LOGON_DATE, "x_last_logon_date => {0}")); //7     
        CALL_PARAMS.add( new Pair<String, String>(DESCR, "x_description => {0}")); //8     
        CALL_PARAMS.add( new Pair<String, String>(PWD_DATE, "x_password_date => {0}")); //9     
        CALL_PARAMS.add( new Pair<String, String>(PWD_ACCESSES_LEFT, "x_password_accesses_left => {0}")); //10     
        CALL_PARAMS.add( new Pair<String, String>(PWD_LIFE_ACCESSES, "x_password_lifespan_accesses => {0}")); //11    
        CALL_PARAMS.add( new Pair<String, String>(PWD_LIFE_DAYS, "x_password_lifespan_days => {0}")); //12     
        CALL_PARAMS.add( new Pair<String, String>(EMP_ID, "x_employee_id => {0}")); //13     
        CALL_PARAMS.add( new Pair<String, String>(EMAIL, "x_email_address => {0}")); //14     
        CALL_PARAMS.add( new Pair<String, String>(FAX, "x_fax => {0}")); //15     
        CALL_PARAMS.add( new Pair<String, String>(CUST_ID, "x_customer_id => {0}")); //16     
        CALL_PARAMS.add( new Pair<String, String>(SUPP_ID, "x_supplier_id => {0}")); //17          
    }
    
    /**
     * The get Instance method
     * @param connector the connector instance
     * @return the account
     */
    public static Account getInstance(OracleERPConnector connector) {
       return new Account(connector);
    }
    
    /**
     * The instance or the parent object
     */
    private OracleERPConnector co = null;
    
    
    /**
     * The schema objectClass builder cache 
     */
    private ObjectClassInfo oci = null;
    
    /**
     * Set of the attributes returned by default
     */
    private Set<String> attributesByDefault = null; 
    
    /**
     * The account
     */
    private Account(OracleERPConnector connector) {
        this.co = connector;
        //No public
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
        //Get the person_id and set is it as a employee id
        final String identity = getName(attrs);
        final Integer person_id = getPersonId(identity, co, attrs);
        if (person_id != null) {
            // Person Id as a Employee_Id
            attrs.add(AttributeBuilder.build(EMP_ID, person_id));
        }
        
        // Get the User values
        final Map<String, SQLParam> userValues = getParamsMap(oclass, attrs, options, true);        
        // Run the create call, new style is using the defaults
        
        if ( !userValues.isEmpty() ) {
            CallableStatement cs = null;
            final String sql = getUserCallSQL(userValues, true, co.app());
            final List<SQLParam> userSQLParams = getUserSQLParams(userValues);
            final String msg = "Create user account {0} : {1}";
            final String user_name = getStringParamValue(userValues, USER_NAME);
            log.ok(msg, user_name, sql);
            try {
                // Create the user
                cs = co.getConn().prepareCall(sql, userSQLParams);
                cs.execute();                              
            } catch (SQLException e) {
                log.error(e, user_name, sql);
                SQLUtil.rollbackQuietly(co.getConn());
                throw new AlreadyExistsException(e);
            } finally {
                SQLUtil.closeQuietly(cs);
            }
        }
                
        // Update responsibilities
        final Attribute resp = AttributeUtil.find(RESPS, attrs);
        final Attribute directResp = AttributeUtil.find(DIRECT_RESPS, attrs);
        if ( resp != null ) {
            co.getRespNames().updateUserResponsibilities( resp, identity);
        } else if ( directResp != null ) {
            co.getRespNames().updateUserResponsibilities( directResp, identity);
        }
        // update securing attributes
        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            co.getSecAttrs().updateUserSecuringAttrs(secAttr, identity);
        }
        
        //Return new UID
        final String userId=getUserId(co, identity);
        co.getConn().commit();     
        return new Uid(userId);
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, this);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {       
        final String sql = "{ call "+co.app()+"fnd_user_pkg.disableuser(?) }";
        log.info(sql);
        CallableStatement cs = null;
        try {
            cs = co.getConn().prepareCall(sql);
            final String identity = AttributeUtil.getAsStringValue(uid).toUpperCase();
            cs.setString(1, identity);
            cs.execute();
            
            co.getConn().commit();
            // No Result ??
        } catch (SQLException e) {
            if (e.getErrorCode() == 20001 || e.getErrorCode() == 1403) {
                final String msg = "SQL Exception trying to delete Oracle user '{0}' ";
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
        //Names
        final String tblname = co.app() + "fnd_user";
        // create the connector object..
        final Set<String> attributesToGet = getAttributesToGet(options);        
        final Set<String> fndUserColumnNames = getColumnNamesToGet(attributesToGet);
        final Set<String> perPeopleColumnNames = CollectionUtil.newSet(fndUserColumnNames);

        fndUserColumnNames.retainAll(FND_USER_COLS);
        perPeopleColumnNames.retainAll(PER_PEOPLE_COLS);
        
        // For all user query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, fndUserColumnNames );
        String sqlSelect = query.getSQL();
        
        if(StringUtil.isNotBlank(co.getCfg().getAccountsIncluded())) {
            sqlSelect += whereAnd(sqlSelect, co.getCfg().getAccountsIncluded());
        } else if( co.getCfg().isActiveAccountsOnly()) {
            sqlSelect += whereAnd(sqlSelect, ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE);
        }
        
        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = co.getConn().prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                AttributeMergeBuilder amb = new AttributeMergeBuilder(attributesToGet);
                final Map<String, SQLParam> columnValues = SQLUtil.getColumnValues(result);
                final SQLParam userNameParm = columnValues.get(USER_NAME);
                final SQLParam userIdParm = columnValues.get(USER_ID);
                final String userName = (String) userNameParm.getValue();
                final boolean getAuditorData = where.getParams().contains( userIdParm );                
                // get users account attributes
                this.buildAccountObject(amb, columnValues);
                
                // if person_id not null and employee_number in schema, return employee_number
                final BigDecimal personId = (BigDecimal) columnValues.get(EMP_ID).getValue();
                buildPersonDetails(amb, personId, perPeopleColumnNames);
                
                // get users responsibilities only if if resp || direct_resp in account attribute
                co.getRespNames().buildResponsibilitiesToAccountObject(amb, userName, attributesToGet);
                // get user's securing attributes
                co.getSecAttrs().buildSecuringAttributesToAccountObject(amb, userName);

                //Auditor data for get user only
                log.info("get auditor data: {0}", getAuditorData);
                if (getAuditorData) {
                    co.getRespNames().buildAuditorDataObject(amb, userName, attributesToGet);
                }
                
                // create the connector object..
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(ObjectClass.ACCOUNT);
                bld.addAttributes(amb.build());
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
            co.getConn().commit();
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
    }
    
    /**
     * @param attributeName
     * @return the columnName
     */
    public String getFilterColumnName(String attributeName) {
        if(Name.NAME.equalsIgnoreCase(attributeName)) { 
            return USER_NAME;
        } else if (Uid.NAME.equalsIgnoreCase(attributeName)) { 
            return USER_ID;
        }
        //We need to filter just a known columns
        if ( FND_USER_COLS.contains(attributeName)) {
            return attributeName;
        }

        return null;
    }    
    
    /**
     * @param attributeName
     * @return the columnName
     */
    private String getColumnName(String attributeName) {
        if (attributeName.equalsIgnoreCase(PERSON_FULLNAME)) {
            return FULL_NAME;
        }
        //We need to filter just a known columns
        if ( PER_PEOPLE_COLS.contains(attributeName)) {
            return attributeName;
        }       

        return getFilterColumnName(attributeName);
    }    
    
    /**
     * The account attributes returned by default
     * @return The set of attribute names
     */
    private Set<String> getAttributesByDefault() {
        if (attributesByDefault == null) {
            attributesByDefault = getDefaultAttributesToGet(getObjectClassInfo().getAttributeInfo());
        }          
        return attributesByDefault;
    }

    /**
     * @param options
     * @return
     */
    private Set<String> getAttributesToGet(OperationOptions options) {
        Set<String> attributesToGet = OracleERPUtil.getAttributesToGet(options, getAttributesByDefault());
        return attributesToGet;
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

        log.ok("columnNamesToGet {0}", columnNamesToGet);
        return columnNamesToGet;
    }

    /**
     * Get the Account Object Class Info
     * @param schemaBld 
     * @return The cached object class info
     */
     ObjectClassInfo getObjectClassInfo() {
        if (oci == null) {
            ObjectClassInfoBuilder ocib = new ObjectClassInfoBuilder();
            ocib.setType(ObjectClass.ACCOUNT_NAME);

            // The Name is supported attribute
            ocib.addAttributeInfo(Name.INFO);
            // name='owner' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(OWNER, String.class, EnumSet.of(Flags.REQUIRED, Flags.NOT_READABLE, Flags.NOT_RETURNED_BY_DEFAULT)));
            // name='session_number' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(SESS_NUM, String.class, EnumSet.of(Flags.NOT_UPDATEABLE,
                    Flags.NOT_CREATABLE)));
            // name='start_date' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(START_DATE, String.class));
            // name='end_date' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(END_DATE, String.class));
            // name='last_logon_date' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(LAST_LOGON_DATE, String.class, EnumSet.of(
                    Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
            // name='description' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(DESCR, String.class));
            // <Views><String>Enable</String></Views>
            ocib.addAttributeInfo(OperationalAttributeInfos.ENABLE);
            // <Views><String>Password</String><String>Reset</String></Views>
            //aoc.addAttributeInfo(OperationalAttributeInfos.RESET_PASSWORD); 
            // reset is implemented as change password
            // name='Password',  Password is mapped to operationalAttribute
            ocib.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
            // name='password_accesses_left' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_DATE, String.class));
            // name='password_accesses_left' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_ACCESSES_LEFT, String.class));
            // name='password_lifespan_accesses' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_LIFE_ACCESSES, String.class));
            // name='password_lifespan_days' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(PWD_LIFE_DAYS, String.class));
            // name='employee_id' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(EMP_ID, String.class));
            // name='employee_number' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(EMP_NUM, Integer.class));
            // name='person_fullname' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(PERSON_FULLNAME, String.class));
            // name='npw_number' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(NPW_NUM, Integer.class));
            // name='email_address' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(EMAIL, String.class));
            // name='fax' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(FAX, String.class));
            // name='customer_id' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(CUST_ID, String.class));
            // name='supplier_id' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(SUPP_ID, String.class));
            // name='person_party_id' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(PERSON_PARTY_ID, String.class));
            
            if ( co.getRespNames().isNewResponsibilityViews() ) {
                // name='DIRECT_RESPS' type='string' required='false'
                ocib.addAttributeInfo(AttributeInfoBuilder.build(DIRECT_RESPS, String.class, EnumSet.of(Flags.MULTIVALUED)));
                // name='INDIRECT_RESPS' type='string' required='false'
                ocib.addAttributeInfo(AttributeInfoBuilder.build(INDIRECT_RESPS, String.class, EnumSet.of(Flags.MULTIVALUED)));
            } else {
                // name='RESPS' type='string' required='false'
                ocib.addAttributeInfo(AttributeInfoBuilder.build(RESPS, String.class, EnumSet.of(Flags.MULTIVALUED)));                
            }
            // name='RESPKEYS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(RESPKEYS, String.class, EnumSet.of(Flags.MULTIVALUED)));
            // name='SEC_ATTRS' type='string' required='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(SEC_ATTRS, String.class, EnumSet.of(Flags.MULTIVALUED)));
            // name='expirePassword' type='string' required='false' is mapped to PASSWORD_EXPIRED
            ocib.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRED);

            //Optional aggregated user attributes
            final EnumSet<Flags> STD_NRD = EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE,
                    Flags.NOT_RETURNED_BY_DEFAULT);

            // name='userMenuNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, STD_NRD));
            // name='menuIds' type='string' audit='false'    
            ocib.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, STD_NRD));
            // name='userFunctionNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, STD_NRD));
            // name='functionIds' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, STD_NRD));
            // name='formIds' type='string' audit='false'    
            ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, STD_NRD));
            // name='formNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, STD_NRD));
            // name='functionNames' type='string' audit='false'    
            ocib.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, STD_NRD));
            // name='userFormNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, STD_NRD));
            // name='readOnlyFormIds' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, STD_NRD));
            // name='readWriteOnlyFormIds' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FORM_IDS, String.class, STD_NRD));
            // name='readOnlyFormNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_NAMES, String.class, STD_NRD));
            // name='readOnlyFunctionNames' type='string' audit='false'    
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTION_NAMES, String.class, STD_NRD));
            // name='readOnlyUserFormNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_USER_FORM_NAMES, String.class, STD_NRD));
            // name='readOnlyFunctionIds' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTIONS_IDS, String.class, STD_NRD));
            // name='readWriteOnlyFormNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FORM_NAMES, String.class, STD_NRD));
            // name='readWriteOnlyUserFormNames' type='string' audit='false'
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_USER_FORM_NAMES));
            // name='readWriteOnlyFunctionNames' type='string' audit='false'        
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_NAMES, String.class, STD_NRD));
            // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
            ocib.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_IDS, String.class, STD_NRD));
            
            oci = ocib.build();
        }   
        return oci;
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
        final String identity = getId(attrs);
        // update securing attributes
        
        
        // Enable/dissable user
        final Attribute enableAttr = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, attrs);
        if ( enableAttr != null ) {
            boolean enable =AttributeUtil.getBooleanValue(enableAttr);
            if ( enable ) {
                //delete user is the same as dissable
                disable(objclass, uid, options);
                final String userId=getUserId(co, identity);
                return new Uid(userId);
            } else {
                enable(objclass, uid, options);
                final String userId=getUserId(co, identity);
                return new Uid(userId);
            }
        }
        
        // Get the User values
        final Map<String, SQLParam> userValues = getParamsMap(objclass, attrs, options, false);
        if ( !userValues.isEmpty() ) {
            // Run the create call, new style is using the defaults
            CallableStatement cs = null;
            final String sql = getUserCallSQL(userValues, false, co.app());
            final String msg = "Create user account {0} : {1}";
            final String userName = getStringParamValue(userValues, USER_NAME); 
            log.ok(msg, userName, sql);
            try {
                // Create the user
                cs = co.getConn().prepareCall(sql, getUserSQLParams(userValues));
                cs.execute();
                co.getConn().commit();
            } catch (SQLException e) {
                log.error(e, msg, userName, sql);
                SQLUtil.rollbackQuietly(co.getConn());
                throw new AlreadyExistsException(e);
            } finally {
                SQLUtil.closeQuietly(cs);
            }            
        }
                        
        // Update responsibilities
        final Attribute resp = AttributeUtil.find(RESPS, attrs);
        final Attribute directResp = AttributeUtil.find(DIRECT_RESPS, attrs);
        if ( resp != null ) {
            co.getRespNames().updateUserResponsibilities( resp, identity);
        } else if ( directResp != null ) {
            co.getRespNames().updateUserResponsibilities( directResp, identity);
        }

        final Attribute secAttr = AttributeUtil.find(SEC_ATTRS, attrs);
        if ( secAttr != null ) {
            co.getSecAttrs().updateUserSecuringAttrs(secAttr, identity);
        }
        
        final String userId=getUserId(co, identity);
        //Return new UID
        return new Uid(userId);
    }

    /**
     * @param objclass
     * @param uid
     * @param options
     */
    private void enable(ObjectClass objclass, Uid uid, OperationOptions options) {
        final String method = "realEnable";
        log.info( method);
        String identity = uid.getUidValue();
        //Map attrs = _actionUtil.getAccountAttributes(user, JActionUtil.OP_ENABLE_USER);

        // no enable user stored procedure that I could find, null out
        // end_date will do nicely
        // Need user's OWNER, so need to do a getUser();
        PreparedStatement st = null;
        try {
            StringBuilder b = new StringBuilder();
            b.append("{ call " + co.app() + "fnd_user_pkg.updateuser(x_user_name => ?");
            b.append(",x_owner => upper(?),x_end_date => FND_USER_PKG.null_date");
            b.append(") }");
            
            String msg = "Oracle ERP: realEnable sql: {0}";
            final String sql = b.toString();
            log.info( msg, sql);

            st = co.getConn().prepareStatement(sql);
            st.setString(1, identity.toUpperCase());
            st.setString(2, co.getCfg().getUser());
            st.execute();
            co.getConn().commit();
        } catch (SQLException e) {
            final String msg = "Cold not enable user {0}";
            log.error(msg, uid.getUidValue());
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
    private void disable(ObjectClass objclass, Uid uid, OperationOptions options) {
        final String sql = "{ call "+co.app()+"fnd_user_pkg.disableuser(?) }";
        log.info(sql);
        CallableStatement cs = null;
        try {
            cs = co.getConn().prepareCall(sql);
            final String asStringValue = AttributeUtil.getAsStringValue(uid);
            cs.setString(1, asStringValue);
            cs.execute();
            co.getConn().commit();
            // No Result ??
        } catch (SQLException e) {
            final String msg = "SQL Exception trying to disable Oracle user '{0}' ";
            throw new IllegalArgumentException(MessageFormat.format(msg, uid),e);
        } finally {
            SQLUtil.closeQuietly(cs);
            cs = null;
        }

    }
    

    /**
     * @param schemaId
     * @param fn
     * @param body
     * @return
     */
    private String createCallSQL(String schemaId, final String fn, StringBuilder body) {
        return "{ call " + schemaId + FND_USER_CALL + fn + " ( "+ body.toString() + " ) }";
    }    

    /**
     * @param bld
     * @param personId
     * @param columnNames 
     */
    private void buildPersonDetails(AttributeMergeBuilder bld, final BigDecimal personId ,
            Set<String> personColumns) {
        if (personId == null) {
            // No personId(employId)
            log.ok("buildPersonDetails: No personId(employId)");
            return;
        }
        log.info("buildPersonDetails for personId: {0}", personId );
        
        //Names to get filter
        final String tblname = co.app()+ "PER_PEOPLE_F";

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
                statement = co.getConn().prepareStatement(query);
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
                co.getConn().commit();
            } catch (SQLException e) {
                String emsg = e.getMessage();
                msg = "Caught SQLException when executing: ''{0}'': {1}";
                log.error(msg, sql, emsg);
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
        String uidValue = null;
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
            } else if (columnName.equalsIgnoreCase(USER_ID)) {
                if (param == null || param.getValue() == null) {
                    String msg = "Uid cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                uidValue = param.getValue().toString();
                amb.addAttribute(Uid.NAME, uidValue);
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
    String getAllSQL(Map<String, SQLParam> userValues, boolean create, String schemaId) {
        final String fn = (create) ? CREATE_FNC : UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Pair<String, String>  par : CALL_PARAMS) {
            final String columnName = par.first;

            if (!first)
                body.append(", ");
            body.append( columnName == null ? "" : Q); // All values will be binded
            first = false;
        }

        final String sql = createCallSQL(schemaId, fn, body);
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
    List<SQLParam> getAllSQLParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for (Pair<String, String>  par : CALL_PARAMS) {
            final String columnName = par.first;

            final SQLParam val = userValues.get(columnName);
            ret.add(val);
        }
        return ret;
    }    
    
    
    /**
     * Evaluate the User Values Map
     * 
     * @param oclass
     *  
     *            the object class
     * @param attrs
     *            the set of attributes
     * @param options
     *            the operation options
     * @param create
     *            true/false for create/update
     * @return a map of userValues
     */
    Map<String, SQLParam> getParamsMap(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options,
            boolean create) {
        log.info("Account: getParamsMap");
        
        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }

        final Map<String, SQLParam> userValues = CollectionUtil.newCaseInsensitiveMap();
        final java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());

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
                final String userName = AttributeUtil.getAsStringValue(attr).toUpperCase();
                userValues.put(USER_NAME, new SQLParam(USER_NAME, userName, Types.VARCHAR));
                log.ok("{0} => {1}, Types.VARCHAR", USER_NAME, userName);
            } else {
                if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                    /*
                    cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
                    //only set 'password_date' if password changed
                    if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                        cstmt1.setDate(9, new java.sql.Date(
                                (new java.util.Date().getTime()) ));
                    }*/
                    if (AttributeUtil.getSingleValue(attr) != null) {
                        final GuardedString password = AttributeUtil.getGuardedStringValue(attr);
                        userValues.put(UNENCRYPT_PWD, new SQLParam(UNENCRYPT_PWD, password));
                        log.ok("{0} is a password", UNENCRYPT_PWD);
                        userValues.put(PWD_DATE, new SQLParam(PWD_DATE, currentDate, Types.DATE));
                        log.ok("append also {0} => {1} ,Types.DATE", PWD_DATE ,currentDate);
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
                        userValues.put(LAST_LOGON_DATE, new SQLParam(LAST_LOGON_DATE, NULL_DATE));
                        log.ok("passwordExpired: {0} => NULL_DATE", LAST_LOGON_DATE);
                        userValues.put(PWD_DATE, new SQLParam(PWD_DATE, NULL_DATE));
                        log.ok("append also {0} => NULL_DATE", PWD_DATE);
                    } else if (create) {
                        userValues.put(LAST_LOGON_DATE,  new SQLParam(LAST_LOGON_DATE, currentDate));
                        log.ok("create account with not expired password {0} => {1}", LAST_LOGON_DATE, currentDate);
                    }

                } else if (attr.is(OWNER)) {
                    //         cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
                    final String owner = AttributeUtil.getAsStringValue(attr);
                    userValues.put(OWNER, new SQLParam(OWNER, owner, Types.VARCHAR));
                    log.ok("{0} = > {1}, Types.VARCHAR", OWNER, owner);
                } else if (attr.is(START_DATE)) {
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
                        userValues.put(START_DATE, new SQLParam(START_DATE, tms, Types.TIMESTAMP));
                        log.ok("{0} => {1} , Types.TIMESTAMP", START_DATE, tms);
                    }

                } else if (attr.is(END_DATE)) {
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
                        userValues.put(END_DATE, new SQLParam(END_DATE, NULL_DATE));
                        log.ok("NULL {0} => NULL_DATE : continue", END_DATE);
                    } else {
                        final String dateString = AttributeUtil.getAsStringValue(attr);
                        if (SYSDATE.equalsIgnoreCase(dateString)) {
                            userValues.put(END_DATE, new SQLParam(END_DATE, SYSDATE));
                            log.ok("sysdate value in {0} => {1} : continue", END_DATE, SYSDATE);
                        } else {
                        Timestamp tms = stringToTimestamp(dateString);
                        userValues.put(END_DATE, new SQLParam(END_DATE, tms, Types.TIMESTAMP));
                        log.ok("{0} => {1}, Types.TIMESTAMP", END_DATE, tms);
                        }
                    }
                } else if (attr.is(DESCR)) {
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
                        userValues.put(DESCR, new SQLParam(DESCR, NULL_CHAR, Types.VARCHAR));
                        log.ok("NULL {0} => NULL_CHAR", DESCR);
                    } else {
                        final String descr = AttributeUtil.getAsStringValue(attr);
                        userValues.put(DESCR, new SQLParam(DESCR, descr, Types.VARCHAR));
                        log.ok("{0} => {1}, Types.VARCHAR", DESCR, descr);
                    }

                } else if (attr.is(PWD_ACCESSES_LEFT)) {
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
                        userValues.put(PWD_ACCESSES_LEFT, new SQLParam(PWD_ACCESSES_LEFT, NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => NULL_NUMBER", PWD_ACCESSES_LEFT);
                    } else {
                        final Integer accessLeft = AttributeUtil.getIntegerValue(attr);
                        userValues.put(PWD_ACCESSES_LEFT, new SQLParam(PWD_ACCESSES_LEFT, accessLeft, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", DESCR, accessLeft);
                    }

                } else if (attr.is(PWD_LIFE_ACCESSES)) {
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
                        userValues.put(PWD_LIFE_ACCESSES, new SQLParam(PWD_LIFE_ACCESSES, NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => NULL_NUMBER", PWD_LIFE_ACCESSES);
                    } else {
                        final Integer lifeAccess = AttributeUtil.getIntegerValue(attr);
                        userValues.put(PWD_LIFE_ACCESSES, new SQLParam(PWD_LIFE_ACCESSES, lifeAccess, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", PWD_LIFE_ACCESSES, lifeAccess);
                    }

                } else if (attr.is(PWD_LIFE_DAYS)) {
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
                        userValues.put(PWD_LIFE_DAYS, new SQLParam(PWD_LIFE_DAYS, NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => NULL_NUMBER", PWD_LIFE_DAYS);
                    } else {
                       final Integer lifeDays = AttributeUtil.getIntegerValue(attr);
                       userValues.put(PWD_LIFE_DAYS, new SQLParam(PWD_LIFE_DAYS, lifeDays, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", PWD_LIFE_DAYS, lifeDays);
                    }

                } else if (attr.is(EMP_ID)) {
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
                        userValues.put(EMP_ID, new SQLParam(EMP_ID, NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => NULL_NUMBER", EMP_ID);
                    } else {
                        final Integer empId = AttributeUtil.getIntegerValue(attr);                        
                        userValues.put(EMP_ID, new SQLParam(EMP_ID, empId, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", EMP_ID, empId);
                    }

                } else if (attr.is(EMAIL)) {
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
                        userValues.put(EMAIL, new SQLParam(EMAIL, NULL_CHAR, Types.VARCHAR));
                        log.ok("NULL {0} => NULL_CHAR", EMAIL);
                    } else {
                        final String email = AttributeUtil.getAsStringValue(attr);                        
                        userValues.put(EMAIL, new SQLParam(EMAIL, email, Types.VARCHAR));
                        log.ok("{0} => {1}, Types.VARCHAR", EMAIL, email);
                    }

                } else if (attr.is(FAX)) {
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
                        userValues.put(FAX, new SQLParam(FAX, NULL_CHAR, Types.VARCHAR));
                        log.ok("NULL {0} => NULL_CHAR", FAX);
                    } else {
                        final String fax = AttributeUtil.getAsStringValue(attr);
                        userValues.put(FAX, new SQLParam(FAX, fax, Types.VARCHAR));
                        log.ok("{0} => {1}, Types.VARCHAR", FAX, fax);
                    }

                } else if (attr.is(CUST_ID)) {
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
                        userValues.put(CUST_ID, new SQLParam(CUST_ID, NULL_NUMBER, Types.VARCHAR));
                        log.ok("NULL {0} => NULL_NUMBER", CUST_ID);
                    } else {
                        final Integer custId = AttributeUtil.getIntegerValue(attr);
                        userValues.put(CUST_ID, new SQLParam(CUST_ID, custId, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", CUST_ID, custId);
                    }

                } else if (attr.is(SUPP_ID)) {
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
                        userValues.put(SUPP_ID, new SQLParam(SUPP_ID, NULL_NUMBER, Types.VARCHAR));
                        log.ok("NULL {0} => NULL_NUMBER", SUPP_ID);
                    } else {
                        final Integer suppId = AttributeUtil.getIntegerValue(attr);
                        userValues.put(SUPP_ID, new SQLParam(SUPP_ID, suppId, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", SUPP_ID, suppId);
                    }
                } 
            }
        }
        //Check required attributes
        Assertions.nullCheck(userValues.get(USER_NAME), Name.NAME);
        Assertions.nullCheck(userValues.get(UNENCRYPT_PWD), OperationalAttributes.PASSWORD_NAME);
        Assertions.nullCheck(userValues.get(OWNER), OWNER);
        log.ok("Account ParamsMap created");
        return userValues;
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    
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
        final String fn = (create) ? CREATE_FNC : UPDATE_FNC;
        log.info("getUserCallSQL: {0}", fn);
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Pair<String, String>  par : CALL_PARAMS) {
            final String columnName = par.first;
            final String parameterExpress = par.second;
              
            SQLParam val = userValues.get(columnName);
            if (val == null) {
                log.ok("skip empty value for {0}", columnName);
                continue; //skip all non setup values
            }
            if (!first)
                body.append(", ");
            if (isDefault(val)) {
                body.append(MessageFormat.format(parameterExpress, val.getValue()));
            } else {
                body.append(MessageFormat.format(parameterExpress, Q)); // Non default values will be binded
            }
            first = false;
        }

        final String sql = createCallSQL(schemaId, fn, body);

        log.ok("getUserCallSQL {0}", sql);
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

        for (Pair<String, String>  par : CALL_PARAMS) {
            final String columnName = par.first;

            final SQLParam val = userValues.get(columnName);
            if (val == null) {
               // log.ok("skip empty value for {0}",columnName);
               continue; //skip all non setup values
            }
            if (!isDefault(val)) {
                ret.add(val);
            }
        }
        return ret;
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
        ret.add(userValues.get(USER_NAME)); //1
        ret.add(userValues.get(OWNER)); //2
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
        for (Pair<String, String>  par : CALL_PARAMS) {
            final String columnName = par.first;
            final String parameterExpress = par.second;
            SQLParam val = userValues.get(columnName);
            
            if (val == null) {
                log.ok("skip empty value for {0}",columnName);
                continue; //skip all non setup values
            }
            if (isDefault(val)) {
                body.append(", ");
                body.append(MessageFormat.format(parameterExpress, val.getValue())); // Update just default 
            }
        }

        final String sql = createCallSQL(schemaId, UPDATE_FNC, body);

        log.ok("getUpdateDefaultsSQL {0}", sql);
        return sql;
    }

    /**
     * @param val
     * @return true/false if predefined default value
     */
    private boolean isDefault(SQLParam val) {
        return SYSDATE.equals(val.getValue()) 
                || NULL_NUMBER.equals(val.getValue()) 
                || NULL_DATE.equals(val.getValue())
                || NULL_CHAR.equals(val.getValue());
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
  
}
