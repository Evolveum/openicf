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

import static org.identityconnectors.oracleerp.AccountOperations.*;
import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.oracleerp.AccountOperations.CallParam;

/**
 * Main implementation of the AccountSQLBuilder Class
 *
 * @author petr
 * @version 1.0
 * @since 1.0
 */
final class AccountSQLCall {
    /**
     * The return sql of the builder class
     */
    final private String callSql;
    String getCallSql() {
        return callSql;
    }

    List<SQLParam> getSqlParams() {
        return sqlParams;
    }

    /**
     * The sql params
     */
    final private List<SQLParam> sqlParams;

    AccountSQLCall(final String callSQL, final List<SQLParam> sqlParams) {
        this.callSql = callSQL;
        this.sqlParams = CollectionUtil.newReadOnlyList(sqlParams);
    }

    /**
     * Builder class for accountSQLCall
     *
     * @author petr
     *
     */
    public static class AccountSQLCallBuilder {
        
        private static final Log log = Log.getLog(AccountSQLCallBuilder.class);

        /**
         * The create/update SQL string
         */
        private boolean create = false;

        /**
         * The schema id
         */
        private String schemaId = "";

        /**
         * The sqlPraram map
         */
        private Map<String, Object> sqlParamsMap = CollectionUtil
                .<Object> newCaseInsensitiveMap();

        /**
         * The sqlPraram map
         */
        private Map<String, String> sqlNullMap = CollectionUtil
                .<String> newCaseInsensitiveMap();

        /**
         * The current date
         */
        private final java.sql.Date currentDate = new java.sql.Date(System
                .currentTimeMillis());

        /**
         * The account
         *
         * @param create
         *            true/false for create/update
         * @param schemaId
         *            the configuration schema id
         */
        public AccountSQLCallBuilder(final String schemaId, boolean create) {
            super();
            this.create = create;
            this.schemaId = schemaId;
        }

        /**
         * Build the AccountSQLCall object
         * @return a AccountSQLCall
         */
        public AccountSQLCall build() {
            final List<SQLParam> sqlParams = new ArrayList<SQLParam>();

            // Check required columns
            Assertions.nullCheck(sqlParamsMap.get(USER_NAME), Name.NAME);
            Assertions.nullCheck(sqlParamsMap.get(OWNER), OWNER);
            if (create) {
                Assertions.nullCheck(sqlParamsMap.get(UNENCRYPT_PWD),
                OperationalAttributes.PASSWORD_NAME);
            }

            final String fn = (create) ? CREATE_FNC : UPDATE_FNC;
            log.ok("getUserCallSQL: {0}", fn);
            StringBuilder body = new StringBuilder();
            boolean first = true;
            for (CallParam par : CALL_PARAMS) {
                final String columnName = par.name;
                final String parameterExpress = par.expression;

                SQLParam val = getSqlParam(columnName);
                String nullParam = getNullParam(columnName);
                if (nullParam == null && val == null) {
                    continue; // skip all non setup values
                }
                if (!first)
                    body.append(", ");
                if (val != null) {
                    // exact value has advantage
                    body.append(MessageFormat.format(parameterExpress, Q));
                    sqlParams.add(val);
                } else if (nullParam != null) {
                    body.append(MessageFormat.format(parameterExpress, nullParam));
                } else {
                    throw new IllegalStateException();
                }
                first = false;
            }

            final String sql = createCallSQL(fn, body);

            log.ok("getUserCallSQL done");
            return new AccountSQLCall(sql, sqlParams);
        }

        /**
         * Add the attribute to the builder
         *
         * @param oclass
         * @param attr
         * @param options
         */
        public void setAttribute(ObjectClass oclass, Attribute attr,
                OperationOptions options) {
            log.ok("Account: getSQLParam");

            // At first, couldn't do anything to null fields except make sql call
            // updating tables directly. Bug#9005 forced us to find oracle constants
            // to do this.
            // I couldn't null out fields with Oracle constants thru callablestatement,
            // instead, collect all null fields and make a preparedstatement call to
            // api with null fields.

            // Handle the special attributes first to use them in decission later

            if (attr.is(Name.NAME)) {
                // cstmt1.setString(1, identity.toUpperCase());
                final String userName = AttributeUtil.getAsStringValue(attr)
                        .toUpperCase();
                setSqlValue(USER_NAME, userName);
            } else if (attr.is(Uid.NAME)) {
                // cstmt1.setString(1, identity.toUpperCase());
                final String userName = AttributeUtil.getAsStringValue(attr)
                        .toUpperCase();
                setSqlValue(USER_NAME, userName);
            } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                /*
                 * cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
                 * only set 'password_date' if password changed
                 * if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                 *   cstmt1.setDate(9, new java.sql.Date(
                 *                    (new java.util.Date().getTime()) ));
                 */
                if (AttributeUtil.getSingleValue(attr) != null) {
                    final GuardedString password = AttributeUtil
                            .getGuardedStringValue(attr);
                    setSqlValue(UNENCRYPT_PWD, password);
                    setSqlValue(PWD_DATE, currentDate);
                }
            } else if (attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)
                    || attr.is(EXP_PWD)) {
                /* ------ adapter code ----------
                 * Boolean expirePassword = null;
                 * if ( ((String)accountAttrChanges.get("OPERATION")).equalsIgnoreCase("CREATE") ) {
                 *    if (accountAttrChanges.containsKey(EXP_PWD)) {
                 *      expirePassword = ((Boolean)accountAttrChanges.get(EXP_PWD));
                 *      if (expirePassword.booleanValue()) {
                 *          nullFields.append(",x_last_logon_date => FND_USER_PKG.null_date");
                 *          nullFields.append(",x_password_date => FND_USER_PKG.null_date");
                 *      } else {
                 *          cstmt1.setDate(7, new java.sql.Date(
                 *                               (new java.util.Date().getTime()) ));
                 *      }
                 *    } else {
                 *      cstmt1.setDate(7, new java.sql.Date(
                 *                              (new java.util.Date().getTime()) ));
                 *    }
                 * } else if ( ((String)accountAttrChanges.get("OPERATION")).equalsIgnoreCase("UPDATE") ) {
                 *   if (accountAttrChanges.containsKey(EXP_PWD)) {
                 *      expirePassword = ((Boolean)accountAttrChanges.get(EXP_PWD));
                 *      if (expirePassword.booleanValue()) {
                 *          nullFields.append(",x_last_logon_date => FND_USER_PKG.null_date");
                 *          nullFields.append(",x_password_date => FND_USER_PKG.null_date");
                 *      }
                 *    }
                 * }
                 *
                 * Handle expiring password differently in create vs update
                 * On create if expirePassword is false/null, set last_logon_date to today
                 * On update if expirePassword is false/null, do nothing
                 * On both is if expirePassword is true, null out last_logon_date, and password_date
                 */
                boolean passwordExpired = false;
                if (AttributeUtil.getSingleValue(attr) != null) {
                    passwordExpired = AttributeUtil.getBooleanValue(attr);
                }
                if (passwordExpired) {
                    setNullValue(LAST_LOGON_DATE, NULL_DATE);
                    log.ok("passwordExpired: {0} => NULL_DATE", LAST_LOGON_DATE);
                    setNullValue(PWD_DATE, NULL_DATE);
                    log.ok("append also {0} => NULL_DATE", PWD_DATE);
                } else if (create) {
                    setSqlValue(LAST_LOGON_DATE, currentDate);
                    log.ok("create account with not expired password {0} => {1}", LAST_LOGON_DATE, currentDate);
                }

            } else if (attr.is(OWNER)) {
                // cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
                final String owner = AttributeUtil.getAsStringValue(attr);
                setSqlValue(OWNER, owner);
                log.ok("{0} = > {1}, Types.VARCHAR", OWNER, owner);
            } else if (attr.is(START_DATE)
                    || attr.is(OperationalAttributes.ENABLE_DATE_NAME)) {
                /* ------ adapter code ----------
                 * start_date 'not null' type
                 * if (accountAttrChanges.containsKey(START_DATE)) {
                 *   if (accountAttrChanges.get(START_DATE) != null) {
                 *      cstmt1.setTimestamp(5, java.sql.Timestamp.valueOf((String)accountAttrChanges.get(START_DATE)) );
                 *   }
                 * }
                 */
                final String dateString = AttributeUtil.getAsStringValue(attr);
                if (dateString != null) {
                    Timestamp tms = stringToTimestamp(dateString);// stringToTimestamp(dateString);
                    setSqlValue(START_DATE, tms);
                    log.ok("{0} => {1} , Types.TIMESTAMP", START_DATE, tms);
                }

            } else if (attr.is(END_DATE)
                    || attr.is(OperationalAttributes.DISABLE_DATE_NAME)) {
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(END_DATE)) {
                 *   if (accountAttrChanges.get(END_DATE) == null) {
                 *     nullFields.append(",x_end_date => FND_USER_PKG.null_date");
                 *   } else if ( ((String)accountAttrChanges.get(END_DATE)).equalsIgnoreCase(SYSDATE)) {
                 *    // force sysdate into end_date
                 *     nullFields.append(",x_end_date => sysdate");
                 *  } else {
                 *     cstmt1.setTimestamp(6, java.sql.Timestamp.valueOf(
                 *             (String)accountAttrChanges.get(END_DATE)) );
                 *  }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(END_DATE, NULL_DATE);
                    log.ok("NULL {0} => NULL_DATE : continue", END_DATE);
                } else {
                    final String dateString = AttributeUtil
                            .getAsStringValue(attr);
                    if (SYSDATE.equalsIgnoreCase(dateString)) {
                        setNullValue(END_DATE, SYSDATE);
                        log.ok("sysdate value in {0} => {1} : continue",
                                END_DATE, SYSDATE);
                    } else {
                        Timestamp tms = stringToTimestamp(dateString);
                        setSqlValue(END_DATE, tms);
                        log.ok("{0} => {1}, Types.TIMESTAMP", END_DATE, tms);
                    }
                }
            } else if (attr.is(DESCR)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(DESCR)) {
                 *   if (accountAttrChanges.get(DESCR) == null) {
                 *      nullFields.append(",x_description => FND_USER_PKG.null_char");
                 *   } else {
                 *      cstmt1.setString(8, (String)accountAttrChanges.get(DESCR));
                 *   }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(DESCR, NULL_CHAR);
                    log.ok("NULL {0} => NULL_CHAR", DESCR);
                } else {
                    final String descr = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(DESCR, descr);
                    log.ok("{0} => {1}, Types.VARCHAR", DESCR, descr);
                }

            } else if (attr.is(PWD_ACCESSES_LEFT)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(PWD_ACCESSES_LEFT)) {
                 *   if ( (accountAttrChanges.get(PWD_ACCESSES_LEFT) == null) ||
                 *      ( ((String)accountAttrChanges.get(PWD_ACCESSES_LEFT)).length() == 0) ) {
                 *      nullFields.append(",x_password_accesses_left => FND_USER_PKG.null_number");
                 *   } else {
                 *      cstmt1.setInt(10, (new Integer((String)accountAttrChanges.get(PWD_ACCESSES_LEFT)).intValue()) );
                 *   }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(PWD_ACCESSES_LEFT, NULL_NUMBER);
                    log.ok("NULL {0} => NULL_NUMBER", PWD_ACCESSES_LEFT);
                } else {
                    final String accessLeft = AttributeUtil
                            .getAsStringValue(attr);
                    setSqlValue(PWD_ACCESSES_LEFT, new Integer(accessLeft));
                    log.ok("{0} => {1}, Types.INTEGER", DESCR, accessLeft);
                }

            } else if (attr.is(PWD_LIFESPAN_ACCESSES)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(PWD_LIFESPAN_ACCESSES)) {
                 *      if ( (accountAttrChanges.get(PWD_LIFESPAN_ACCESSES) == null)  ||
                 *          ( ((String)accountAttrChanges.get(PWD_LIFESPAN_ACCESSES)).length() == 0) ) {
                 *           nullFields.append(",x_password_lifespan_accesses => FND_USER_PKG.null_number");
                 *      } else {
                 *          cstmt1.setInt(11, (new Integer((String)accountAttrChanges.get(PWD_LIFESPAN_ACCESSES)).intValue()) );
                 *      }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(PWD_LIFESPAN_ACCESSES, NULL_NUMBER);
                    log.ok("NULL {0} => NULL_NUMBER", PWD_LIFESPAN_ACCESSES);
                } else {
                    final String lifeAccess = AttributeUtil
                            .getAsStringValue(attr);
                    setSqlValue(PWD_LIFESPAN_ACCESSES, new Integer(lifeAccess));
                    log.ok("{0} => {1}, Types.INTEGER", PWD_LIFESPAN_ACCESSES,
                            lifeAccess);
                }

            } else if (attr.is(PWD_LIFESPAN_DAYS)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(PWD_LIFESPAN_DAYS)) {
                 *   if ( (accountAttrChanges.get(PWD_LIFESPAN_DAYS) == null) ||
                 *      ( ((String)accountAttrChanges.get(PWD_LIFESPAN_DAYS)).length() == 0) ) {
                 *          nullFields.append(",x_password_lifespan_days => FND_USER_PKG.null_number");
                 *      } else {
                 *          cstmt1.setInt(12, (new Integer((String)accountAttrChanges.get(PWD_LIFESPAN_DAYS)).intValue()) );
                 *   }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(PWD_LIFESPAN_DAYS, NULL_NUMBER);
                    log.ok("NULL {0} => NULL_NUMBER", PWD_LIFESPAN_DAYS);
                } else {
                    final String lifeDays = AttributeUtil
                            .getAsStringValue(attr);
                    setSqlValue(PWD_LIFESPAN_DAYS, new Integer(lifeDays));
                    log.ok("{0} => {1}, Types.INTEGER", PWD_LIFESPAN_DAYS,
                            lifeDays);
                }

            } else if (attr.is(EMP_ID)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(EMP_ID)) {
                 *  if ( (accountAttrChanges.get(EMP_ID) == null)  ||
                 *      ( ((String)accountAttrChanges.get(EMP_ID)).length() == 0) ) {
                 *          nullFields.append(",x_employee_id => FND_USER_PKG.null_number");
                 *      } else {
                 *          cstmt1.setInt(13, (new Integer((String)accountAttrChanges.get(EMP_ID)).intValue()) );
                 *      }
                 *  }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(EMP_ID, NULL_NUMBER);
                    log.ok("NULL {0} => NULL_NUMBER", EMP_ID);
                } else {
                    final String empId = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(EMP_ID, new Integer(empId));
                    log.ok("{0} => {1}, Types.INTEGER", EMP_ID, empId);
                }

            } else if (attr.is(EMAIL)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(EMAIL)) {
                 *   if (accountAttrChanges.get(EMAIL) == null) {
                 *      nullFields.append(",x_email_address => FND_USER_PKG.null_char");
                 *   } else {
                 *      cstmt1.setString(14, (String)accountAttrChanges.get(EMAIL));
                 *   }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(EMAIL, NULL_CHAR);
                    log.ok("NULL {0} => NULL_CHAR", EMAIL);
                } else {
                    final String email = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(EMAIL, email);
                    log.ok("{0} => {1}, Types.VARCHAR", EMAIL, email);
                }

            } else if (attr.is(FAX)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(FAX)) {
                 *   if (accountAttrChanges.get(FAX) == null) {
                 *      nullFields.append(",x_fax => FND_USER_PKG.null_char");
                 *   } else {
                 *      cstmt1.setString(15, (String)accountAttrChanges.get(FAX));
                 *   }
                 *  }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(FAX, NULL_CHAR);
                    log.ok("NULL {0} => NULL_CHAR", FAX);
                } else {
                    final String fax = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(FAX, fax);
                    log.ok("{0} => {1}, Types.VARCHAR", FAX, fax);
                }

            } else if (attr.is(CUST_ID)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(CUST_ID)) {
                 *  if ( (accountAttrChanges.get(CUST_ID) == null) ||
                 *   ( ((String)accountAttrChanges.get(CUST_ID)).length() == 0) ) {
                 *     nullFields.append(",x_customer_id => FND_USER_PKG.null_number");
                 *  } else {
                 *     cstmt1.setInt(16, (new Integer((String)accountAttrChanges.get(CUST_ID)).intValue()) );
                 *  }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(CUST_ID, NULL_NUMBER);
                    log.ok("NULL {0} => NULL_NUMBER", CUST_ID);
                } else {
                    final String custId = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(CUST_ID, new Integer(custId));
                    log.ok("{0} => {1}, Types.INTEGER", CUST_ID, custId);
                }

            } else if (attr.is(SUPP_ID)) {
                /*  ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(SUPP_ID)) {
                 *   if ( (accountAttrChanges.get(SUPP_ID) == null) ||
                 *   ( ((String)accountAttrChanges.get(SUPP_ID)).length() == 0) ) {
                 *       nullFields.append(",x_supplier_id => FND_USER_PKG.null_number");
                 *   } else {
                 *       cstmt1.setInt(17, (new Integer((String)accountAttrChanges.get(SUPP_ID)).intValue()) );
                 *   }
                 * }
                 */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(SUPP_ID, NULL_NUMBER);
                    log.ok("NULL {0} => NULL_NUMBER", SUPP_ID);
                } else {
                    final String suppId = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(SUPP_ID, new Integer(suppId));
                    log.ok("{0} => {1}, Types.INTEGER", SUPP_ID, suppId);
                }
            }
        }

        /**
         * Add parameter, and replace if exist
         *
         * @param columnName
         * @param value
         */
        private void setSqlValue(String columnName, Object value) {
            // We deal about null values later
            if (value == null)
                return;
            // Lets find a sqlParam type
            final CallParam cp = PARAM_MAP.get(columnName);
            if (cp == null) {
                throw new IllegalArgumentException(
                        "could not add unknown column " + columnName);
            }
            sqlParamsMap.put(columnName, value);
        }
        

        /**
         * @param fn
         * @param body
         * @return The SQL string
         */
        private String createCallSQL(final String fn, StringBuilder body) {
            // The SQL call update function SQL template
            final String FND_USER_CALL = "fnd_user_pkg.";
            
            return "{ call " + schemaId + FND_USER_CALL + fn + " ( " + body.toString() + " ) }";
        }        

        /**
         * Add parameter, if exist, it must be the same value
         *
         * @param columnName
         * @param value
         */
        private void setNullValue(String columnName, String value) {
            // We deal about null values later
            if (value == null)
                return;
            // Lets find a sqlParam type
            final CallParam cp = PARAM_MAP.get(columnName);
            if (cp == null) {
                throw new IllegalArgumentException(
                        "could not add unknown column " + columnName);
            }

            sqlNullMap.put(columnName, value);
            return;
        }

        /**
         * Get the sql param
         *
         * @param columnName
         * @return sqlParam value
         */
        private SQLParam getSqlParam(String columnName) {

            // Lets find a sqlParam type
            final CallParam cp = PARAM_MAP.get(columnName);
            if (cp == null) {
                throw new IllegalArgumentException(
                        "could not get param for unknown column " + columnName);
            }
            // Check the old value exist
            final Object value = sqlParamsMap.get(columnName);
            if (value == null) {
                return null;
            }

            final int sqlType = cp.sqlType;
            return new SQLParam(columnName, value, sqlType);
        }

        /**
         * Get the sql param
         *
         * @param columnName
         * @return sqlParam value
         */
        private String getNullParam(String columnName) {
            // Check the old value exist
            return sqlNullMap.get(columnName);
        }

        /**
         * @return the builder is empty
         */
        public boolean isEmpty() {
            return sqlParamsMap.isEmpty();
        }
    }

}
