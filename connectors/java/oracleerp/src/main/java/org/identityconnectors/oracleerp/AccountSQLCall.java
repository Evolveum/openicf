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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.AccountOperations.CALL_PARAMS;
import static org.identityconnectors.oracleerp.AccountOperations.PARAM_MAP;
import static org.identityconnectors.oracleerp.OracleERPUtil.CREATE_FNC;
import static org.identityconnectors.oracleerp.OracleERPUtil.CUST_ID;
import static org.identityconnectors.oracleerp.OracleERPUtil.DESCR;
import static org.identityconnectors.oracleerp.OracleERPUtil.EMAIL;
import static org.identityconnectors.oracleerp.OracleERPUtil.EMP_ID;
import static org.identityconnectors.oracleerp.OracleERPUtil.END_DATE;
import static org.identityconnectors.oracleerp.OracleERPUtil.EXP_PWD;
import static org.identityconnectors.oracleerp.OracleERPUtil.FAX;
import static org.identityconnectors.oracleerp.OracleERPUtil.LAST_LOGON_DATE;
import static org.identityconnectors.oracleerp.OracleERPUtil.NULL_CHAR;
import static org.identityconnectors.oracleerp.OracleERPUtil.NULL_DATE;
import static org.identityconnectors.oracleerp.OracleERPUtil.NULL_NUMBER;
import static org.identityconnectors.oracleerp.OracleERPUtil.OWNER;
import static org.identityconnectors.oracleerp.OracleERPUtil.PWD_ACCESSES_LEFT;
import static org.identityconnectors.oracleerp.OracleERPUtil.PWD_DATE;
import static org.identityconnectors.oracleerp.OracleERPUtil.PWD_LIFESPAN_ACCESSES;
import static org.identityconnectors.oracleerp.OracleERPUtil.PWD_LIFESPAN_DAYS;
import static org.identityconnectors.oracleerp.OracleERPUtil.Q;
import static org.identityconnectors.oracleerp.OracleERPUtil.START_DATE;
import static org.identityconnectors.oracleerp.OracleERPUtil.SUPP_ID;
import static org.identityconnectors.oracleerp.OracleERPUtil.SYSDATE;
import static org.identityconnectors.oracleerp.OracleERPUtil.UNENCRYPT_PWD;
import static org.identityconnectors.oracleerp.OracleERPUtil.UPDATE_FNC;
import static org.identityconnectors.oracleerp.OracleERPUtil.USER_NAME;
import static org.identityconnectors.oracleerp.OracleERPUtil.stringToTimestamp;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
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
 * Main implementation of the AccountSQLBuilder Class.
 *
 * @author petr
 * @since 1.0
 */
final class AccountSQLCall {
    /**
     * The return sql of the builder class.
     */
    final private String callSql;

    String getCallSql() {
        return callSql;
    }

    /** The sql params. */
    final private List<SQLParam> sqlParams;

    List<SQLParam> getSqlParams() {
        return sqlParams;
    }

    AccountSQLCall(final String callSQL, final List<SQLParam> sqlParams) {
        this.callSql = callSQL;
        this.sqlParams = CollectionUtil.newReadOnlyList(sqlParams);
    }

    /**
     * Builder class for accountSQLCall.
     *
     * @author petr
     *
     */
    public static class AccountSQLCallBuilder {

        private static final Log LOG = Log.getLog(AccountSQLCallBuilder.class);

        /**
         * The create/update SQL string.
         */
        final private boolean create;

        /**
         * The OracleERPConfiguration.
         */
        final private OracleERPConfiguration cfg;

        /**
         * The resourcePasswordAttrValue.
         */
        private GuardedString resourcePassword = null;
        /**
         * The passwordAttrValue.
         */
        private GuardedString password = null;

        /**
         * The sqlPraram map.
         */
        private Map<String, Object> sqlParamsMap = CollectionUtil.<Object> newCaseInsensitiveMap();

        /**
         * The sqlPraram map.
         */
        private Map<String, String> sqlNullMap = CollectionUtil.<String> newCaseInsensitiveMap();

        /**
         * The current date.
         */
        private final java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());

        /**
         * The account.
         *
         * @param create
         *            true/false for create/update
         * @param cfg
         *            the connector configuration
         */
        public AccountSQLCallBuilder(final OracleERPConfiguration cfg, boolean create) {
            super();
            this.create = create;
            this.cfg = cfg;
        }

        /**
         * Add the attribute to the builder.
         *
         * @param oclass
         * @param attr
         * @param options
         */
        public void setAttribute(ObjectClass oclass, Attribute attr, OperationOptions options) {
            LOG.ok("Account: getSQLParam");

            // At first, couldn't do anything to null fields except make sql
            // call
            // updating tables directly. Bug#9005 forced us to find oracle
            // constants
            // to do this.
            // I couldn't null out fields with Oracle constants thru
            // callablestatement,
            // instead, collect all null fields and make a preparedstatement
            // call to
            // api with null fields.

            // Handle the special attributes first to use them in decission
            // later

            if (attr.is(Name.NAME)) {
                // cstmt1.setString(1, identity.toUpperCase());
                final String userName = AttributeUtil.getAsStringValue(attr).toUpperCase();
                setSqlValue(USER_NAME, userName);
            } else if (attr.is(Uid.NAME)) {
                // cstmt1.setString(1, identity.toUpperCase());
                final String userName = AttributeUtil.getAsStringValue(attr).toUpperCase();
                setSqlValue(USER_NAME, userName);
            } else if (StringUtil.isNotBlank(cfg.getPasswordAttribute())
                    && attr.is(cfg.getPasswordAttribute())) {
                /* @formatter:off */
                /*
                 * cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
                 * only set 'password_date' if password changed
                 * if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                 *   cstmt1.setDate(9, new java.sql.Date(
                 *                    (new java.util.Date().getTime()) ));
                 */
                /* @formatter:on*/
                if (AttributeUtil.getSingleValue(attr) != null) {
                    resourcePassword = AttributeUtil.getGuardedStringValue(attr);
                }
            } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                /* @formatter:off */
                /*
                 *
                 * cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
                 * only set 'password_date' if password changed
                 * if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                 *   cstmt1.setDate(9, new java.sql.Date(
                 *                    (new java.util.Date().getTime()) ));
                 */
                 /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) != null) {
                    password = AttributeUtil.getGuardedStringValue(attr);
                }
            } else if (attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME) || attr.is(EXP_PWD)) {
                /* @formatter:off */
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
                 * Handle expiring password differently in create vs update On
                 * create if expirePassword is false/null, set last_logon_date
                 * to today On update if expirePassword is false/null, do
                 * nothing On both is if expirePassword is true, null out
                 * last_logon_date, and password_date
                 */
                /* @formatter:on */
                boolean passwordExpired = false;
                if (AttributeUtil.getSingleValue(attr) != null) {
                    passwordExpired = AttributeUtil.getBooleanValue(attr);
                }
                if (passwordExpired) {
                    setNullValue(LAST_LOGON_DATE, NULL_DATE);
                    LOG.ok("passwordExpired: {0} => NULL_DATE", LAST_LOGON_DATE);
                    setNullValue(PWD_DATE, NULL_DATE);
                    LOG.ok("append also {0} => NULL_DATE", PWD_DATE);
                } else if (create) {
                    setSqlValue(LAST_LOGON_DATE, currentDate);
                    LOG.ok("create account with not expired password {0} => {1}", LAST_LOGON_DATE,
                            currentDate);
                }

            } else if (attr.is(OWNER)) {
                // cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
                final String owner = AttributeUtil.getAsStringValue(attr);
                setSqlValue(OWNER, owner);
                LOG.ok("{0} = > {1}, Types.VARCHAR", OWNER, owner);
            } else if (attr.is(START_DATE) || attr.is(OperationalAttributes.ENABLE_DATE_NAME)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * start_date 'not null' type
                 * if (accountAttrChanges.containsKey(START_DATE)) {
                 *   if (accountAttrChanges.get(START_DATE) != null) {
                 *      cstmt1.setTimestamp(5, java.sql.Timestamp.valueOf((String)accountAttrChanges.get(START_DATE)) );
                 *   }
                 * }
                 */
                /* @formatter:on */
                final String dateString = AttributeUtil.getAsStringValue(attr);
                if (dateString != null) {
                    Timestamp tms = stringToTimestamp(dateString);// stringToTimestamp(dateString);
                    setSqlValue(START_DATE, tms);
                    LOG.ok("{0} => {1} , Types.TIMESTAMP", START_DATE, tms);
                }

            } else if (attr.is(END_DATE) || attr.is(OperationalAttributes.DISABLE_DATE_NAME)) {
                /* @formatter:off */
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
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(END_DATE, NULL_DATE);
                    LOG.ok("NULL {0} => NULL_DATE : continue", END_DATE);
                } else {
                    final String dateString = AttributeUtil.getAsStringValue(attr);
                    if (SYSDATE.equalsIgnoreCase(dateString)) {
                        setNullValue(END_DATE, SYSDATE);
                        LOG.ok("sysdate value in {0} => {1} : continue", END_DATE, SYSDATE);
                    } else {
                        Timestamp tms = stringToTimestamp(dateString);
                        setSqlValue(END_DATE, tms);
                        LOG.ok("{0} => {1}, Types.TIMESTAMP", END_DATE, tms);
                    }
                }
            } else if (attr.is(DESCR)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(DESCR)) {
                 *   if (accountAttrChanges.get(DESCR) == null) {
                 *      nullFields.append(",x_description => FND_USER_PKG.null_char");
                 *   } else {
                 *      cstmt1.setString(8, (String)accountAttrChanges.get(DESCR));
                 *   }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(DESCR, NULL_CHAR);
                    LOG.ok("NULL {0} => NULL_CHAR", DESCR);
                } else {
                    final String descr = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(DESCR, descr);
                    LOG.ok("{0} => {1}, Types.VARCHAR", DESCR, descr);
                }

            } else if (attr.is(PWD_ACCESSES_LEFT)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(PWD_ACCESSES_LEFT)) {
                 *   if ( (accountAttrChanges.get(PWD_ACCESSES_LEFT) == null) ||
                 *      ( ((String)accountAttrChanges.get(PWD_ACCESSES_LEFT)).length() == 0) ) {
                 *      nullFields.append(",x_password_accesses_left => FND_USER_PKG.null_number");
                 *   } else {
                 *      cstmt1.setInt(10, (new Integer((String)accountAttrChanges.get(PWD_ACCESSES_LEFT)).intValue()) );
                 *   }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(PWD_ACCESSES_LEFT, NULL_NUMBER);
                    LOG.ok("NULL {0} => NULL_NUMBER", PWD_ACCESSES_LEFT);
                } else {
                    final String accessLeft = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(PWD_ACCESSES_LEFT, new Integer(accessLeft));
                    LOG.ok("{0} => {1}, Types.INTEGER", DESCR, accessLeft);
                }

            } else if (attr.is(PWD_LIFESPAN_ACCESSES)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(PWD_LIFESPAN_ACCESSES)) {
                 *      if ( (accountAttrChanges.get(PWD_LIFESPAN_ACCESSES) == null)  ||
                 *          ( ((String)accountAttrChanges.get(PWD_LIFESPAN_ACCESSES)).length() == 0) ) {
                 *           nullFields.append(",x_password_lifespan_accesses => FND_USER_PKG.null_number");
                 *      } else {
                 *          cstmt1.setInt(11, (new Integer((String)accountAttrChanges.get(PWD_LIFESPAN_ACCESSES)).intValue()) );
                 *      }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(PWD_LIFESPAN_ACCESSES, NULL_NUMBER);
                    LOG.ok("NULL {0} => NULL_NUMBER", PWD_LIFESPAN_ACCESSES);
                } else {
                    final String lifeAccess = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(PWD_LIFESPAN_ACCESSES, new Integer(lifeAccess));
                    LOG.ok("{0} => {1}, Types.INTEGER", PWD_LIFESPAN_ACCESSES, lifeAccess);
                }

            } else if (attr.is(PWD_LIFESPAN_DAYS)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(PWD_LIFESPAN_DAYS)) {
                 *   if ( (accountAttrChanges.get(PWD_LIFESPAN_DAYS) == null) ||
                 *      ( ((String)accountAttrChanges.get(PWD_LIFESPAN_DAYS)).length() == 0) ) {
                 *          nullFields.append(",x_password_lifespan_days => FND_USER_PKG.null_number");
                 *      } else {
                 *          cstmt1.setInt(12, (new Integer((String)accountAttrChanges.get(PWD_LIFESPAN_DAYS)).intValue()) );
                 *   }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(PWD_LIFESPAN_DAYS, NULL_NUMBER);
                    LOG.ok("NULL {0} => NULL_NUMBER", PWD_LIFESPAN_DAYS);
                } else {
                    final String lifeDays = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(PWD_LIFESPAN_DAYS, new Integer(lifeDays));
                    LOG.ok("{0} => {1}, Types.INTEGER", PWD_LIFESPAN_DAYS, lifeDays);
                }

            } else if (attr.is(EMP_ID)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(EMP_ID)) {
                 *  if ( (accountAttrChanges.get(EMP_ID) == null)  ||
                 *      ( ((String)accountAttrChanges.get(EMP_ID)).length() == 0) ) {
                 *          nullFields.append(",x_employee_id => FND_USER_PKG.null_number");
                 *      } else {
                 *          cstmt1.setInt(13, (new Integer((String)accountAttrChanges.get(EMP_ID)).intValue()) );
                 *      }
                 *  }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(EMP_ID, NULL_NUMBER);
                    LOG.ok("NULL {0} => NULL_NUMBER", EMP_ID);
                } else {
                    final String empId = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(EMP_ID, new Integer(empId));
                    LOG.ok("{0} => {1}, Types.INTEGER", EMP_ID, empId);
                }

            } else if (attr.is(EMAIL)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(EMAIL)) {
                 *   if (accountAttrChanges.get(EMAIL) == null) {
                 *      nullFields.append(",x_email_address => FND_USER_PKG.null_char");
                 *   } else {
                 *      cstmt1.setString(14, (String)accountAttrChanges.get(EMAIL));
                 *   }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(EMAIL, NULL_CHAR);
                    LOG.ok("NULL {0} => NULL_CHAR", EMAIL);
                } else {
                    final String email = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(EMAIL, email);
                    LOG.ok("{0} => {1}, Types.VARCHAR", EMAIL, email);
                }

            } else if (attr.is(FAX)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(FAX)) {
                 *   if (accountAttrChanges.get(FAX) == null) {
                 *      nullFields.append(",x_fax => FND_USER_PKG.null_char");
                 *   } else {
                 *      cstmt1.setString(15, (String)accountAttrChanges.get(FAX));
                 *   }
                 *  }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(FAX, NULL_CHAR);
                    LOG.ok("NULL {0} => NULL_CHAR", FAX);
                } else {
                    final String fax = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(FAX, fax);
                    LOG.ok("{0} => {1}, Types.VARCHAR", FAX, fax);
                }

            } else if (attr.is(CUST_ID)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(CUST_ID)) {
                 *  if ( (accountAttrChanges.get(CUST_ID) == null) ||
                 *   ( ((String)accountAttrChanges.get(CUST_ID)).length() == 0) ) {
                 *     nullFields.append(",x_customer_id => FND_USER_PKG.null_number");
                 *  } else {
                 *     cstmt1.setInt(16, (new Integer((String)accountAttrChanges.get(CUST_ID)).intValue()) );
                 *  }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(CUST_ID, NULL_NUMBER);
                    LOG.ok("NULL {0} => NULL_NUMBER", CUST_ID);
                } else {
                    final String custId = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(CUST_ID, new Integer(custId));
                    LOG.ok("{0} => {1}, Types.INTEGER", CUST_ID, custId);
                }

            } else if (attr.is(SUPP_ID)) {
                /* @formatter:off */
                /* ------ adapter code ----------
                 * if (accountAttrChanges.containsKey(SUPP_ID)) {
                 *   if ( (accountAttrChanges.get(SUPP_ID) == null) ||
                 *   ( ((String)accountAttrChanges.get(SUPP_ID)).length() == 0) ) {
                 *       nullFields.append(",x_supplier_id => FND_USER_PKG.null_number");
                 *   } else {
                 *       cstmt1.setInt(17, (new Integer((String)accountAttrChanges.get(SUPP_ID)).intValue()) );
                 *   }
                 * }
                 */
                /* @formatter:on */
                if (AttributeUtil.getSingleValue(attr) == null) {
                    setNullValue(SUPP_ID, NULL_NUMBER);
                    LOG.ok("NULL {0} => NULL_NUMBER", SUPP_ID);
                } else {
                    final String suppId = AttributeUtil.getAsStringValue(attr);
                    setSqlValue(SUPP_ID, new Integer(suppId));
                    LOG.ok("{0} => {1}, Types.INTEGER", SUPP_ID, suppId);
                }
            }
        }

        /**
         * Build the AccountSQLCall object.
         *
         * @return a AccountSQLCall
         */
        public AccountSQLCall build() {
            final List<SQLParam> sqlParams = new ArrayList<SQLParam>();

            // Update the password attribute, if defined and value is present
            if (resourcePassword != null) {
                setSqlValue(UNENCRYPT_PWD, resourcePassword);
                setSqlValue(PWD_DATE, currentDate);
            } else if (password != null) {
                setSqlValue(UNENCRYPT_PWD, password);
                setSqlValue(PWD_DATE, currentDate);
            }

            // Check required columns
            Assertions.nullCheck(sqlParamsMap.get(USER_NAME), Name.NAME);
            Assertions.nullCheck(sqlParamsMap.get(OWNER), OWNER);
            if (create) {
                Assertions.nullCheck(sqlParamsMap.get(UNENCRYPT_PWD),
                        OperationalAttributes.PASSWORD_NAME);
            }

            final String fn = (create) ? CREATE_FNC : UPDATE_FNC;
            LOG.ok("getUserCallSQL: {0}", fn);
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
                if (!first) {
                    body.append(", ");
                }
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

            LOG.ok("getUserCallSQL done");
            return new AccountSQLCall(sql, sqlParams);
        }

        /**
         * Add parameter, and replace if exist.
         *
         * @param columnName
         * @param value
         */
        private void setSqlValue(String columnName, Object value) {
            // We deal about null values later
            if (value == null) {
                return;
            }
            // Lets find a sqlParam type
            final CallParam cp = PARAM_MAP.get(columnName);
            if (cp == null) {
                throw new IllegalArgumentException("could not add unknown column " + columnName);
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

            StringBuilder sb =
                    new StringBuilder("{ call ").append(cfg.app()).append("fnd_user_pkg.").append(
                            fn).append(" ( ").append(body).append(" ) }");

            return sb.toString();
        }

        /**
         * Add parameter, if exist, it must be the same value.
         *
         * @param columnName
         * @param value
         */
        private void setNullValue(String columnName, String value) {
            // We deal about null values later
            if (value == null) {
                return;
            }
            // Lets find a sqlParam type
            final CallParam cp = PARAM_MAP.get(columnName);
            if (cp == null) {
                throw new IllegalArgumentException("could not add unknown column " + columnName);
            }

            sqlNullMap.put(columnName, value);
            return;
        }

        /**
         * Get the sql param.
         *
         * @param columnName
         * @return sqlParam value
         */
        private SQLParam getSqlParam(String columnName) {

            // Lets find a sqlParam type
            final CallParam cp = PARAM_MAP.get(columnName);
            if (cp == null) {
                throw new IllegalArgumentException("could not get param for unknown column "
                        + columnName);
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
         * Get the sql param.
         *
         * @param columnName
         * @return sqlParam value
         */
        private String getNullParam(String columnName) {
            // Check the old value exist
            return sqlNullMap.get(columnName);
        }

        /**
         * @return the builder is empty.
         */
        public boolean isEmpty() {
            return sqlParamsMap.isEmpty();
        }
    }

}
