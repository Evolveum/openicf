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

import static org.identityconnectors.oracleerp.AccountOperations.CALL_PARAMS;
import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.oracleerp.AccountOperations.CallParam;

/**
 * Main implementation of the AccountSQLBuilder Class
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class AccountSQLBuilder {

    /**
     * Setup logging.
     */
    private static final Log log = Log.getLog(AccountSQLBuilder.class);
    
    /**
     * The create/update SQL string
     */
    private boolean create = false;
    
    /**
     * The schema id
     */
    private String schemaId = "";
    
    
    /**
     * The sqlParamsSet
     */
    private Set<SQLParam> sqlParams = CollectionUtil.<SQLParam>newSet();

    /**
     * The sqlPrama map
     */
    private Map<String, SQLParam> sqlParamsMap = CollectionUtil.<SQLParam>newCaseInsensitiveMap();

    /**
     * The account
     * @param create
     *            true/false for create/update
     * @param schemaId
     *            the configuration schema id            
     */
    public AccountSQLBuilder(final String schemaId, boolean create) {
        super();
        this.create = create;
        this.schemaId = schemaId;
    }    

    /**
     * Return the userAccount create/update sql full syntax (all fields)
     *            true for create/false update
     * @return a <CODE>String</CODE> sql string
     */
    public String getAllSQL() {
        final String fn = (create) ? CREATE_FNC : UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (CallParam par : CALL_PARAMS) {
            final String columnName = par.name;

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
     * @return a <CODE>List</CODE> object list
     */
    public List<SQLParam> getAllSQLParams() {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for ( CallParam par : CALL_PARAMS ) {
            final String columnName = par.name;
            Integer sqlType = par.sqlType;

            SQLParam val = sqlParamsMap.get(columnName);
            //null default values
            if( val == null || isPredefinedValue(val)) {                
                val = new SQLParam(columnName, null, sqlType);
            }
            ret.add(val);
        }
        return ret;
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
     * @return self
     */
    public AccountSQLBuilder build(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        log.info("init");
        
        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }

        // At first, couldn't do anything to null fields except make sql call
        // updating tables directly. Bug#9005 forced us to find oracle constants
        // to do this.
        // I couldn't null out fields with Oracle constants thru callablestatement,
        // instead, collect all null fields and make a preparedstatement call to
        // api with null fields.        
       
        // Fill the helper set
        for (Attribute attr : attrs) {
            addSQLParam(attr, oclass, options);
        }
        
        // Create the SQLParam map
        for (SQLParam par : sqlParams) {
            sqlParamsMap.put(par.getName(), par);
        }
        //Check required columns
        Assertions.nullCheck(sqlParamsMap.get(USER_NAME), Name.NAME);
        //Assertions.nullCheck(sqlParamsMap.get(OWNER), OWNER);
        if (create) {
            Assertions.nullCheck(sqlParamsMap.get(UNENCRYPT_PWD), OperationalAttributes.PASSWORD_NAME);
        }
        log.ok("init");
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    
    /**
     * Return the userAccount create/update sql with defaults
     * 
     * @param sqlParamsMap
     *            the Map of user values
     * @param create
     *            true for create/false update
     * 
     * @return a <CODE>String</CODE> sql string
     */
    public String getUserCallSQL() {
        final String fn = (create) ? CREATE_FNC : UPDATE_FNC;
        log.info("getUserCallSQL: {0}", fn);
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for ( CallParam par : CALL_PARAMS ) {
            final String columnName = par.name;
            final String parameterExpress = par.expression;
              
            SQLParam val = sqlParamsMap.get(columnName);
            if (val == null) {
                log.ok("skip empty value for {0}", columnName);
                continue; //skip all non setup values
            }
            if (!first)
                body.append(", ");
            if (isPredefinedValue(val)) {
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
     * @return a <CODE>List</CODE> sql object list
     */
    public List<SQLParam> getUserSQLParams() {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for (CallParam par : CALL_PARAMS) {
            final String columnName = par.name;

            final SQLParam val = sqlParamsMap.get(columnName);
            if (val == null) {
               // log.ok("skip empty value for {0}",columnName);
               continue; //skip all non setup values
            }
            if (!isPredefinedValue(val)) {
                ret.add(val);
            }
        }
        return ret;
    }


    /**
     * Return the create/update parameters
     * @return a <CODE>List</CODE> sql object list
     */
    public List<SQLParam> getUserUpdateNullsParams() {
        final List<SQLParam> ret = new ArrayList<SQLParam>();
        ret.add(sqlParamsMap.get(USER_NAME)); //1
        ret.add(sqlParamsMap.get(OWNER)); //2
        return ret;
    }

    /**
     * Return the userAccount create/update sql null updates
     * @return a <CODE>String</CODE> sql string
     */
    public String getUserUpdateNullsSQL() {
        StringBuilder body = new StringBuilder("x_user_name => ?, x_owner => upper(?)");
        for (CallParam par : CALL_PARAMS) {
            final String columnName = par.name;
            final String parameterExpress = par.expression;
            SQLParam val = sqlParamsMap.get(columnName);
            
            if (val == null) {
                log.ok("skip empty value for {0}",columnName);
                continue; //skip all non setup values
            }
            if (isPredefinedValue(val)) {
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
    private boolean isPredefinedValue(SQLParam val) {
        return SYSDATE.equals(val.getValue()) 
                || NULL_NUMBER.equals(val.getValue()) 
                || NULL_DATE.equals(val.getValue())
                || NULL_CHAR.equals(val.getValue());
    }
    
    /**
     * Test for null attribute values
     * @return <code>boolean<code> true if the update null attributes is needed
     */
    public boolean isUpdateNeeded() {
        for (SQLParam value : sqlParamsMap.values()) {
            if (isPredefinedValue(value)) {
                return true;
            }
        }
        return false;
    }


    /**
     * @param attr
     * @param oclass
     * @param options
     */
    public void addSQLParam(Attribute attr, ObjectClass oclass, OperationOptions options) {
        log.info("Account: getSQLParam");

        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }

        SQLParam ret = null;
        final java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());

        // At first, couldn't do anything to null fields except make sql call
        // updating tables directly. Bug#9005 forced us to find oracle constants
        // to do this.
        // I couldn't null out fields with Oracle constants thru callablestatement,
        // instead, collect all null fields and make a preparedstatement call to
        // api with null fields.        

        // Handle the special attributes first to use them in decission later

        if (attr.is(Name.NAME)) {
            //         cstmt1.setString(1, identity.toUpperCase());
            final String userName = AttributeUtil.getAsStringValue(attr).toUpperCase();
            ret = new SQLParam(USER_NAME, userName, Types.VARCHAR);
            sqlParams.add(ret);
            log.ok("{0} => {1}, Types.VARCHAR", USER_NAME, userName);
        } else if (attr.is(Uid.NAME)) {
            //         cstmt1.setString(1, identity.toUpperCase());
            final String userName = AttributeUtil.getAsStringValue(attr).toUpperCase();
            ret = new SQLParam(USER_NAME, userName, Types.VARCHAR);
            sqlParams.add(ret);
            log.ok("{0} => {1}, Types.VARCHAR", USER_NAME, userName);
        } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
            /*
            cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
            //only set 'password_date' if password changed
            if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                cstmt1.setDate(9, new java.sql.Date(
                        (new java.util.Date().getTime()) ));
            }*/
            if (AttributeUtil.getSingleValue(attr) != null) {
                final GuardedString password = AttributeUtil.getGuardedStringValue(attr);
                ret = new SQLParam(UNENCRYPT_PWD, password);
                sqlParams.add(ret);
                log.ok("{0} is a password", UNENCRYPT_PWD);
                final SQLParam add = new SQLParam(PWD_DATE, currentDate, Types.DATE);
                sqlParams.add(add);
                log.ok("append also {0} => {1} ,Types.DATE", PWD_DATE, currentDate);
            }

        } else if (attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME) || attr.is(EXP_PWD)) {
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
                ret = new SQLParam(LAST_LOGON_DATE, NULL_DATE);
                sqlParams.add(ret);
                log.ok("passwordExpired: {0} => NULL_DATE", LAST_LOGON_DATE);
                final SQLParam add = new SQLParam(PWD_DATE, NULL_DATE);
                sqlParams.add(add);
                log.ok("append also {0} => NULL_DATE", PWD_DATE);
            } else if (create) {
                ret = new SQLParam(LAST_LOGON_DATE, currentDate);
                sqlParams.add(ret);
                log.ok("create account with not expired password {0} => {1}", LAST_LOGON_DATE, currentDate);
            }

        } else if (attr.is(OWNER)) {
            //         cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
            final String owner = AttributeUtil.getAsStringValue(attr);
            ret = new SQLParam(OWNER, owner, Types.VARCHAR);
            sqlParams.add(ret);
            log.ok("{0} = > {1}, Types.VARCHAR", OWNER, owner);
        } else if (attr.is(START_DATE) || attr.is(OperationalAttributes.ENABLE_DATE_NAME)) {
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
                ret = new SQLParam(START_DATE, tms, Types.TIMESTAMP);
                sqlParams.add(ret);
                log.ok("{0} => {1} , Types.TIMESTAMP", START_DATE, tms);
            }

        } else if (attr.is(END_DATE) || attr.is(OperationalAttributes.DISABLE_DATE_NAME)) {
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
                ret = new SQLParam(END_DATE, NULL_DATE);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_DATE : continue", END_DATE);
            } else {
                final String dateString = AttributeUtil.getAsStringValue(attr);
                if (SYSDATE.equalsIgnoreCase(dateString)) {
                    ret = new SQLParam(END_DATE, SYSDATE);
                    sqlParams.add(ret);
                    log.ok("sysdate value in {0} => {1} : continue", END_DATE, SYSDATE);
                } else {
                    Timestamp tms = stringToTimestamp(dateString);
                    ret = new SQLParam(END_DATE, tms, Types.TIMESTAMP);
                    sqlParams.add(ret);
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
                ret = new SQLParam(DESCR, NULL_CHAR, Types.VARCHAR);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_CHAR", DESCR);
            } else {
                final String descr = AttributeUtil.getAsStringValue(attr);
                ret = new SQLParam(DESCR, descr, Types.VARCHAR);
                sqlParams.add(ret);
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
                ret = new SQLParam(PWD_ACCESSES_LEFT, NULL_NUMBER, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_NUMBER", PWD_ACCESSES_LEFT);
            } else {
                final Integer accessLeft = AttributeUtil.getIntegerValue(attr);
                ret = new SQLParam(PWD_ACCESSES_LEFT, accessLeft, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("{0} => {1}, Types.INTEGER", DESCR, accessLeft);
            }

        } else if (attr.is(PWD_LIFESPAN_ACCESSES)) {
            /*  ------ adapter code ----------
            if (accountAttrChanges.containsKey(PWD_LIFESPAN_ACCESSES)) {
               if ( (accountAttrChanges.get(PWD_LIFESPAN_ACCESSES) == null)  ||
               ( ((String)accountAttrChanges.get(PWD_LIFESPAN_ACCESSES)).length() == 0) ) {
                   nullFields.append(",x_password_lifespan_accesses => FND_USER_PKG.null_number");                                
               } else {
                   cstmt1.setInt(11, (new Integer((String)accountAttrChanges.get(PWD_LIFESPAN_ACCESSES)).intValue()) );
               }
            } */
            if (AttributeUtil.getSingleValue(attr) == null) {
                ret = new SQLParam(PWD_LIFESPAN_ACCESSES, NULL_NUMBER, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_NUMBER", PWD_LIFESPAN_ACCESSES);
            } else {
                final Integer lifeAccess = AttributeUtil.getIntegerValue(attr);
                ret = new SQLParam(PWD_LIFESPAN_ACCESSES, lifeAccess, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("{0} => {1}, Types.INTEGER", PWD_LIFESPAN_ACCESSES, lifeAccess);
            }

        } else if (attr.is(PWD_LIFESPAN_DAYS)) {
            /*  ------ adapter code ----------
            if (accountAttrChanges.containsKey(PWD_LIFESPAN_DAYS)) {
                if ( (accountAttrChanges.get(PWD_LIFESPAN_DAYS) == null) ||
                ( ((String)accountAttrChanges.get(PWD_LIFESPAN_DAYS)).length() == 0) ) {
                    nullFields.append(",x_password_lifespan_days => FND_USER_PKG.null_number");                
                } else {
                    cstmt1.setInt(12, (new Integer((String)accountAttrChanges.get(PWD_LIFESPAN_DAYS)).intValue()) );
                }
            } 
            */
            if (AttributeUtil.getSingleValue(attr) == null) {
                ret = new SQLParam(PWD_LIFESPAN_DAYS, NULL_NUMBER, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_NUMBER", PWD_LIFESPAN_DAYS);
            } else {
                final Integer lifeDays = AttributeUtil.getIntegerValue(attr);
                ret = new SQLParam(PWD_LIFESPAN_DAYS, lifeDays, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("{0} => {1}, Types.INTEGER", PWD_LIFESPAN_DAYS, lifeDays);
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
                ret = new SQLParam(EMP_ID, NULL_NUMBER, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_NUMBER", EMP_ID);
            } else {
                final Integer empId = AttributeUtil.getIntegerValue(attr);
                ret = new SQLParam(EMP_ID, empId, Types.INTEGER);
                sqlParams.add(ret);
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
                ret = new SQLParam(EMAIL, NULL_CHAR, Types.VARCHAR);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_CHAR", EMAIL);
            } else {
                final String email = AttributeUtil.getAsStringValue(attr);
                ret = new SQLParam(EMAIL, email, Types.VARCHAR);
                sqlParams.add(ret);
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
                ret = new SQLParam(FAX, NULL_CHAR, Types.VARCHAR);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_CHAR", FAX);
            } else {
                final String fax = AttributeUtil.getAsStringValue(attr);
                ret = new SQLParam(FAX, fax, Types.VARCHAR);
                sqlParams.add(ret);
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
                ret = new SQLParam(CUST_ID, NULL_NUMBER, Types.VARCHAR);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_NUMBER", CUST_ID);
            } else {
                final Integer custId = AttributeUtil.getIntegerValue(attr);
                ret = new SQLParam(CUST_ID, custId, Types.INTEGER);
                sqlParams.add(ret);
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
                ret = new SQLParam(SUPP_ID, NULL_NUMBER, Types.VARCHAR);
                sqlParams.add(ret);
                log.ok("NULL {0} => NULL_NUMBER", SUPP_ID);
            } else {
                final Integer suppId = AttributeUtil.getIntegerValue(attr);
                ret = new SQLParam(SUPP_ID, suppId, Types.INTEGER);
                sqlParams.add(ret);
                log.ok("{0} => {1}, Types.INTEGER", SUPP_ID, suppId);
            }
        }
    }


    /**
     * @return the builder is empty
     */
    public boolean isEmpty() {
        return sqlParamsMap.isEmpty();
    }
}
