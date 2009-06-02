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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
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
     * The read column names
     */
    static final String[] CN = {
            USER_ID, //0 not createble, updatable 
            USER_NAME, //1
            OWNER,  //2   write only   
            UNENCRYPT_PWD, //3 write only      
            SESS_NUM,  //4     
            START_DATE,  //5     
            END_DATE, //6     
            LAST_LOGON_DATE, //7     
            DESCR, //8     
            PWD_DATE, //9     
            PWD_ACCESSES_LEFT, //10     
            PWD_LIFE_ACCESSES, //11    
            PWD_LIFE_DAYS, //12     
            EMP_ID, //13     
            EMAIL, //14     
            FAX, //15     
            CUST_ID, //16     
            SUPP_ID, //17     
            PERSON_PARTY_ID, //18   not createble, updatable  
            PERSON_FULLNAME, //19     
            NPW_NUM, NPW_NUM, //20
            RESP, //22
            EMP_NUM, //21     
            RESPKEYS, //23     
            SEC_ATTRS, //24     
            EXP_PWD, //25  
    };          
    
    
    /**
     * The read column names
     */
    static final String[] RCN = {
            // USER_ID, //0 not createble, updatable 
            // USER_NAME //1
            // OWNER,  //2   write only   
            // UNENCRYPT_PWD //3 write only      
            // SESS_NUM,  //4     
            START_DATE,  //5     
            END_DATE, //6     
            LAST_LOGON_DATE, //7     
            DESCR, //8     
            PWD_DATE, //9     
            PWD_ACCESSES_LEFT, //10     
            PWD_LIFE_ACCESSES, //11    
            PWD_LIFE_DAYS, //12     
            EMP_ID, //13     
            EMAIL, //14     
            FAX, //15     
            CUST_ID, //16     
            SUPP_ID, //17     
            PERSON_PARTY_ID, //18   not createble, updatable  
            //PERSON_FULLNAME, //19     
            //NPW_NUM, NPW_NUM, //20
            //RESP //22
            //EMP_NUM, //21     
            //RESPKEYS, //23     
            //SEC_ATTRS, //24     
            //EXP_PWD, //25  
            //USER_NAME //26 not createble, updatable
    };       
    
    /**
     * The map of column name parameters mapping
     */
    static final Map<String, String> CPM = CollectionUtil.<String> newCaseInsensitiveMap();      

    /**
     * Initialization of the map
     */
    static {
       CPM.put(USER_NAME, "x_user_name => {0}"); //1
       CPM.put(OWNER, "x_owner => upper({0})"); //2   write only   
       CPM.put(UNENCRYPT_PWD, "x_unencrypted_password => {0}");//3 write only      
       CPM.put(SESS_NUM, "x_session_number => {0}"); //4     
       CPM.put(START_DATE, "x_start_date => {0}"); //5     
       CPM.put(END_DATE, "x_end_date => {0}"); //6     
       CPM.put(LAST_LOGON_DATE, "x_last_logon_date => {0}"); //7     
       CPM.put(DESCR, "x_description => {0}"); //8     
       CPM.put(PWD_DATE, "x_password_date => {0}"); //9     
       CPM.put(PWD_ACCESSES_LEFT, "x_password_accesses_left => {0}"); //10     
       CPM.put(PWD_LIFE_ACCESSES, "x_password_lifespan_accesses => {0}"); //11    
       CPM.put(PWD_LIFE_DAYS, "x_password_lifespan_days => {0}"); //12     
       CPM.put(EMP_ID, "x_employee_id => {0}"); //13     
       CPM.put(EMAIL, "x_email_address => {0}"); //14     
       CPM.put(FAX, "x_fax => {0}"); //15     
       CPM.put(CUST_ID, "x_customer_id => {0}"); //16     
       CPM.put(SUPP_ID, "x_supplier_id => {0}"); //17   
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
     * the clone is not supported
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    /**
     * The account sigleton 
     */
    private Account(OracleERPConnector connector) {
        this.parent = connector;
        //No public
    }
    
    /**
     * The instance or the parent object
     */
    private OracleERPConnector parent = null;    
    
    /**
     * @param options
     * @return the set of names
     */
    public Set<String> accountAttributesToColumnNames(OperationOptions options) {
        Set<String> columnNamesToGet = new HashSet<String>();        
        if (options != null && options.getAttributesToGet() != null) {
            // Replace attributes to quoted columnNames
            for (String attributeName : options.getAttributesToGet()) {
                columnNamesToGet.add(getColumnName(attributeName));
            }        
        } 
        if(columnNamesToGet.isEmpty()) {
            columnNamesToGet = CollectionUtil.newReadOnlySet(Account.RCN);
        }
        
        return columnNamesToGet;
    }
    
    /**
     * @param attributeName
     * @return the columnName
     */
    public String getColumnName(String attributeName) {
        if(Name.NAME.equalsIgnoreCase(attributeName)) { 
            return USER_NAME;
        } else if (Uid.NAME.equalsIgnoreCase(attributeName)) { 
            return USER_NAME;
        } 
        return attributeName;  
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
        final OracleERPConfiguration cfg = parent.getCfg();
        final OracleERPConnection conn = parent.getConn();
        
        final Attribute empAttr = AttributeUtil.find(Account.EMP_NUM, attrs);
        final Integer empNum =  empAttr == null ? null :  AttributeUtil.getIntegerValue(empAttr);
        final Attribute npwAttr = AttributeUtil.find(Account.NPW_NUM, attrs);
        final Integer nwpNum = npwAttr == null ? null : AttributeUtil.getIntegerValue(npwAttr);
        
        //Get the person_id and set is it as a employee id
        final String person_id = OracleERPUtil.getPersonId(parent, empNum, nwpNum);
        if (person_id != null) {
            // Person Id as a Employee_Id
            attrs.add(AttributeBuilder.build(Account.EMP_ID, person_id));
        }
        
        // Get the User values
        final Map<String, SQLParam> userValues = getParamsMap(oclass, attrs, options, true);
        
        // Run the create call, new style is using the defaults
        CallableStatement cs = null;
        final String sql = getUserCallSQL(userValues, true, cfg.app());
        final List<SQLParam> userSQLParams = getUserSQLParams(userValues);
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
        final String fn = (create) ? Account.CREATE_FNC : Account.UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (String columnName : CN) {
            final String parameterExpress = CPM.get(columnName);
            if (parameterExpress == null) {
                continue; //skip all non call parameterName values                    
            }    
            if (!first)
                body.append(", ");
            body.append(Account.Q); // All values will be binded
            first = false;
        }

        final String sql = OracleERPUtil.CURLY_BEGIN + MessageFormat.format(Account.SQL_CALL, schemaId, fn, body.toString())
                + OracleERPUtil.CURLY_END;
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

        for (String columnName : CN) {
            final String parameterExpress = CPM.get(columnName);
            if (parameterExpress == null) {
                continue; //skip all non call parameterName values                    
            }    
            final SQLParam val = userValues.get(columnName);
            ret.add(val);
        }
        return ret;
    }

    /**
     * 
     * @param cn
     * @return
     */
    String getAttributeName(String cn) {
        if(USER_NAME.equalsIgnoreCase(cn)) { //Name 
            return Name.NAME;
        } else if (OWNER.equalsIgnoreCase(cn)) { //2   write only 
            return null;
        } else if (UNENCRYPT_PWD.equalsIgnoreCase(cn)) { //3 write only
            return null;
        }
        return cn;        
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

        final Map<String, SQLParam> userValues = new HashMap<String, SQLParam>();
        final SQLParam currentDate = new SQLParam(new java.sql.Date(System.currentTimeMillis()), Types.DATE);

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
                userValues.put(Account.USER_NAME, new SQLParam(userName, Types.VARCHAR));
                log.ok("{0} => {1}, Types.VARCHAR", Account.USER_NAME, userName);
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
                        userValues.put(Account.UNENCRYPT_PWD, new SQLParam(password));
                        log.ok("{0} is a password", Account.UNENCRYPT_PWD);
                        userValues.put(Account.PWD_DATE, currentDate);
                        log.ok("append also {0} => {1} ,Types.DATE", Account.PWD_DATE ,currentDate);
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
                        log.ok("passwordExpired: {0} => Account.NULL_DATE", Account.LAST_LOGON_DATE);
                        userValues.put(Account.PWD_DATE, new SQLParam(Account.NULL_DATE));
                        log.ok("append also {0} => Account.NULL_DATE", Account.PWD_DATE);
                    } else if (create) {
                        userValues.put(Account.LAST_LOGON_DATE, currentDate);
                        log.ok("create account with not expired password {0} => {1}", Account.LAST_LOGON_DATE, currentDate);
                    }

                } else if (attr.is(Account.OWNER)) {
                    //         cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
                    final String owner = AttributeUtil.getAsStringValue(attr);
                    userValues.put(Account.OWNER, new SQLParam(owner, Types.VARCHAR));
                    log.ok("{0} = > {1}, Types.VARCHAR", Account.OWNER, owner);
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
                        Timestamp tms = OracleERPUtil.stringToTimestamp(dateString);// stringToTimestamp(dateString);
                        userValues.put(Account.START_DATE, new SQLParam(tms, Types.TIMESTAMP));
                        log.ok("{0} => {1} , Types.TIMESTAMP", Account.START_DATE, tms);
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
                        log.ok("NULL {0} => Account.NULL_DATE : continue", Account.END_DATE);
                    } else {
                        final String dateString = AttributeUtil.getAsStringValue(attr);
                        if (Account.SYSDATE.equalsIgnoreCase(dateString)) {
                            userValues.put(Account.END_DATE, new SQLParam(Account.SYSDATE));
                            log.ok("sysdate value in {0} => {1} : continue", Account.END_DATE, Account.SYSDATE);
                        } else {
                        Timestamp tms = OracleERPUtil.stringToTimestamp(dateString);
                        userValues.put(Account.END_DATE, new SQLParam(tms, Types.TIMESTAMP));
                        log.ok("{0} => {1}, Types.TIMESTAMP", Account.END_DATE, tms);
                        }
                    }
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
                        userValues.put(Account.DESCR, new SQLParam(Account.NULL_CHAR, Types.VARCHAR));
                        log.ok("NULL {0} => Account.NULL_CHAR", Account.DESCR);
                    } else {
                        final String descr = AttributeUtil.getAsStringValue(attr);
                        userValues.put(Account.DESCR, new SQLParam(descr, Types.VARCHAR));
                        log.ok("{0} => {1}, Types.VARCHAR", Account.DESCR, descr);
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
                        userValues.put(Account.PWD_ACCESSES_LEFT, new SQLParam(Account.NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => Account.NULL_NUMBER", Account.PWD_ACCESSES_LEFT);
                    } else {
                        final Integer accessLeft = AttributeUtil.getIntegerValue(attr);
                        userValues.put(Account.PWD_ACCESSES_LEFT, new SQLParam(accessLeft, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", Account.DESCR, accessLeft);
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
                        userValues.put(Account.PWD_LIFE_ACCESSES, new SQLParam(Account.NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => Account.NULL_NUMBER", Account.PWD_LIFE_ACCESSES);
                    } else {
                        final Integer lifeAccess = AttributeUtil.getIntegerValue(attr);
                        userValues.put(Account.PWD_LIFE_ACCESSES, new SQLParam(lifeAccess, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", Account.PWD_LIFE_ACCESSES, lifeAccess);
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
                        userValues.put(Account.PWD_LIFE_DAYS, new SQLParam(Account.NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => Account.NULL_NUMBER", Account.PWD_LIFE_DAYS);
                    } else {
                       final Integer lifeDays = AttributeUtil.getIntegerValue(attr);
                       userValues.put(Account.PWD_LIFE_DAYS, new SQLParam(lifeDays, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", Account.PWD_LIFE_DAYS, lifeDays);
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
                        userValues.put(Account.EMP_ID, new SQLParam(Account.NULL_NUMBER, Types.INTEGER));
                        log.ok("NULL {0} => Account.NULL_NUMBER", Account.EMP_ID);
                    } else {
                        final Integer empId = AttributeUtil.getIntegerValue(attr);                        
                        userValues.put(Account.EMP_ID, new SQLParam(empId, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", Account.EMP_ID, empId);
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
                        log.ok("NULL {0} => Account.NULL_CHAR", Account.EMAIL);
                    } else {
                        final String email = AttributeUtil.getAsStringValue(attr);                        
                        userValues.put(Account.EMAIL, new SQLParam(email, Types.VARCHAR));
                        log.ok("{0} => {1}, Types.VARCHAR", Account.EMAIL, email);
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
                        log.ok("NULL {0} => Account.NULL_CHAR", Account.FAX);
                    } else {
                        final String fax = AttributeUtil.getAsStringValue(attr);
                        userValues.put(Account.FAX, new SQLParam(fax, Types.VARCHAR));
                        log.ok("{0} => {1}, Types.VARCHAR", Account.FAX, fax);
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
                        log.ok("NULL {0} => Account.NULL_NUMBER", Account.CUST_ID);
                    } else {
                        final Integer custId = AttributeUtil.getIntegerValue(attr);
                        userValues.put(Account.CUST_ID, new SQLParam(custId, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", Account.CUST_ID, custId);
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
                        log.ok("NULL {0} => Account.NULL_NUMBER", Account.SUPP_ID);
                    } else {
                        final Integer suppId = AttributeUtil.getIntegerValue(attr);
                        userValues.put(Account.SUPP_ID, new SQLParam(suppId, Types.INTEGER));
                        log.ok("{0} => {1}, Types.INTEGER", Account.SUPP_ID, suppId);
                    }
                } 
            }
        }
        //Check required attributes
        Assertions.nullCheck(userValues.get(Account.USER_NAME), Name.NAME);
        Assertions.nullCheck(userValues.get(Account.UNENCRYPT_PWD), OperationalAttributes.PASSWORD_NAME);
        Assertions.nullCheck(userValues.get(Account.OWNER), Account.OWNER);
        log.ok("Account ParamsMap created");
        return userValues;
    }

    /**
     * Get the Account Object Class Info
     * 
     * @return ObjectClassInfo value
     */
    public ObjectClassInfo getSchema() {
        ObjectClassInfoBuilder aoc = new ObjectClassInfoBuilder();
        aoc.setType(ObjectClass.ACCOUNT_NAME);

        // The Name is supported attribute
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='owner' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.OWNER, String.class, EnumSet.of(Flags.NOT_READABLE,
                Flags.REQUIRED)));
        // name='session_number' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.SESS_NUM, String.class, EnumSet.of(
                Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        // name='start_date' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.START_DATE, String.class));
        // name='end_date' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.END_DATE, String.class));
        // name='last_logon_date' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.LAST_LOGON_DATE, String.class, EnumSet.of(
                Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE)));
        // name='description' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.DESCR, String.class));
        // <Views><String>Enable</String></Views>
        aoc.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        // <Views><String>Password</String><String>Reset</String></Views>
        //aoc.addAttributeInfo(OperationalAttributeInfos.RESET_PASSWORD); 
        // reset is implemented as change password
        // name='Password',  Password is mapped to operationalAttribute
        aoc.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        // name='password_accesses_left' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_DATE, String.class));
        // name='password_accesses_left' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_ACCESSES_LEFT, String.class));
        // name='password_lifespan_accesses' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_LIFE_ACCESSES, String.class));
        // name='password_lifespan_days' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PWD_LIFE_DAYS, String.class));
        // name='employee_id' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.EMP_ID, String.class));
        // name='employee_number' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.EMP_NUM, Integer.class));
        // name='person_fullname' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PERSON_FULLNAME, String.class));
        // name='npw_number' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.NPW_NUM, Integer.class));
        // name='email_address' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.EMAIL, String.class));
        // name='fax' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.FAX, String.class));
        // name='customer_id' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.CUST_ID, String.class));
        // name='supplier_id' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.SUPP_ID, String.class));
        // name='person_party_id' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.PERSON_PARTY_ID, String.class));
        // name='RESP' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.RESP, String.class));
        // name='RESPKEYS' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.RESPKEYS, String.class));
        // name='SEC_ATTRS' type='string' required='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Account.SEC_ATTRS, String.class));
        // name='expirePassword' type='string' required='false' is mapped to PASSWORD_EXPIRED
        aoc.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRED);

        return aoc.build();
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
        log.info("getUserCallSQL: {0}", fn);
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (String columnName : CN) {
            final String parameterExpress = CPM.get(columnName);
            if (parameterExpress == null) {
                continue; //skip all non call parameterName values                    
            }            
            SQLParam val = userValues.get(columnName);
            if (val == null) {
                log.ok("skip empty value for {0}",columnName);
                continue; //skip all non setup values
            }
            if (!first)
                body.append(", ");
            if (isDefault(val)) {
                body.append(MessageFormat.format(parameterExpress, val.getValue()));
                log.ok("append {0} default value {1}",parameterExpress, val.getValue());
            } else {
                body.append(MessageFormat.format(parameterExpress, Account.Q)); // Non default values will be binded
                log.ok("append {0} value binding ?",parameterExpress);
            }
            first = false;
        }

        final String sql = OracleERPUtil.CURLY_BEGIN + MessageFormat.format(Account.SQL_CALL, schemaId, fn, body.toString())
                + OracleERPUtil.CURLY_END;
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

        for (String columnName : CN) {
            final String parameterExpress = CPM.get(columnName);
            if (parameterExpress == null) {
                continue; //skip all non call parameterName values                    
            }    
            final SQLParam val = userValues.get(columnName);
            if (val == null) {
                log.ok("skip empty value for {0}",columnName);
               continue; //skip all non setup values
            }
            if (!isDefault(val)) {
                ret.add(new SQLParam(val));
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
        ret.add(userValues.get(Account.USER_NAME)); //1
        ret.add(userValues.get(Account.OWNER)); //2
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
        for (String columnName : CN) {
            final String parameterExpress = CPM.get(columnName);
            if (parameterExpress == null) {
                continue; //skip all non call parameterName values                    
            }    
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

        final String sql = OracleERPUtil.CURLY_BEGIN
                + MessageFormat.format(Account.SQL_CALL, schemaId, Account.UPDATE_FNC, body.toString())
                + OracleERPUtil.CURLY_END;
        log.ok("getUpdateDefaultsSQL {0}", sql);
        return sql;
    }    
    

    /**
     * @param val
     * @return true/false if predefined default value
     */
    boolean isDefault(SQLParam val) {
        return Account.SYSDATE.equals(val.getValue()) 
                || Account.NULL_NUMBER.equals(val.getValue()) 
                || Account.NULL_DATE.equals(val.getValue())
                || Account.NULL_CHAR.equals(val.getValue());
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
    
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    
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
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        final OracleERPConfiguration cfg = parent.getCfg();
        final OracleERPConnection conn = parent.getConn();
        
        // Get the User values
        final Map<String, SQLParam> userValues = getParamsMap(objclass, replaceAttributes, options, false);
        
        // Run the create call, new style is using the defaults
        CallableStatement cs = null;
        final String sql = getUserCallSQL(userValues, false, cfg.app());
        final String msg = "Create user account {0} : {1}";
        final String userName = (String) userValues.get(Account.USER_NAME).getValue();
        log.ok(msg, userName, sql);
        try {
            // Create the user
            cs = conn.prepareCall(sql, getUserSQLParams(userValues));
            cs.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            cs.execute();
        } catch (SQLException e) {
            log.error(e, msg, userName, sql);
            SQLUtil.rollbackQuietly(conn);
            throw new AlreadyExistsException(e);
        } finally {
            SQLUtil.closeQuietly(cs);
        }
        //Commit all
        conn.commit();
        
        //Return new UID
        return new Uid(OracleERPUtil.getUserId(parent, userName).toString());
    }

    /**
     * Construct a connector object
     * <p>Taking care about special attributes</p>
     *  
     * @param attributeSet from the database table
     * @param columnValues TODO
     * @return ConnectorObjectBuilder object
     * @throws SQLException 
     */
    ConnectorObjectBuilder getConnectorObjectBuilder(Map<String, SQLParam> columnValues) throws SQLException {
        String uidValue = null;
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        for (Map.Entry<String, SQLParam> val : columnValues.entrySet()) {
            final String columnName = val.getKey();
            final SQLParam param = val.getValue();
            // Map the special
            if (columnName.equalsIgnoreCase(Account.USER_NAME)) {
                if (param == null || param.getValue() == null) {
                    String msg = "Name cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                bld.setName(param.getValue().toString());
            } else if (columnName.equalsIgnoreCase(Account.USER_ID)) {
                if (param == null || param.getValue() == null) {
                    String msg = "Uid cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                uidValue = param.getValue().toString();
                bld.setUid(uidValue);
            } else if (columnName.equalsIgnoreCase(Account.UNENCRYPT_PWD)) {
                // No Password in the result object
            } else if (columnName.equalsIgnoreCase(Account.OWNER)) {
                // No Owner in the result object
            } else {
                //Convert the data type and create attribute from it.
                final Object value = SQLUtil.jdbc2AttributeValue(param.getValue());
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


    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {       
        final String SQL ="call {0}fnd_user_pkg.disableuser(?)";
        final String sql = "{ " + MessageFormat.format(SQL, parent.getCfg().app())+ " }";
   //     log.ok(sql);
        CallableStatement cs = null;
        try {
            cs = parent.getConn().prepareCall(sql);
            final String asStringValue = AttributeUtil.getAsStringValue(uid);
            cs.setString(1, asStringValue);
            cs.execute();
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
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        //Names
        final String tblname = parent.getCfg().app() + "fnd_user";
        final Set<String> columnNamesToGet = accountAttributesToColumnNames(options);
        // For all user query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, columnNamesToGet);
        String sqlSelect = query.getSQL();
        
        if(StringUtil.isNotBlank(parent.getCfg().getAccountsIncluded())) {
            sqlSelect += whereAnd(sqlSelect, parent.getCfg().getAccountsIncluded());
        } else if( parent.getCfg().isActiveAccountsOnly()) {
            sqlSelect += whereAnd(sqlSelect, OracleERPUtil.ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE);
        }
        
        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = parent.getConn().prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                final Map<String, SQLParam> columnValues = SQLUtil.getColumnValues(result);
                // create the connector object..
                final ConnectorObjectBuilder bld = getConnectorObjectBuilder(columnValues);
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
    }
    

    /**
     * @param sqlSelect 
     * @param whereAnd
     * @return e result string
     */
    public static String whereAnd(String sqlSelect, String whereAnd) {
        int iofw = sqlSelect.indexOf("WHERE");
        return (iofw == -1) ? sqlSelect + " WHERE " + whereAnd : sqlSelect.substring(0, iofw) + "WHERE ("+sqlSelect.substring(iofw + 5) +") AND ( " + whereAnd + " )";
    }      
    
    

}
