/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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

import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

/**
 * Main implementation of the Account Object Class
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class Account {
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
     * The Account function call predefined attributeNames, columnName and call parameterName names bindings
     */
    static final Account[] UI = { new Account(USER_ID), //0
            new Account(Name.NAME, USER_NAME, "x_user_name => {0}"), //1
            new Account(null, OWNER, "x_owner => upper({0})"), //2   unreadable   
            new Account(null, UNENCRYPT_PWD, "x_unencrypted_password => {0}"),//3 unreadable     
            new Account(SESS_NUM, "x_session_number => {0}"), //4     
            new Account(START_DATE, "x_start_date => {0}"), //5     
            new Account(END_DATE, "x_end_date => {0}"), //6     
            new Account(LAST_LOGON_DATE, "x_last_logon_date => {0}"), //7     
            new Account(DESCR, "x_description => {0}"), //8     
            new Account(PWD_DATE, "x_password_date => {0}"), //9     
            new Account(PWD_ACCESSES_LEFT, "x_password_accesses_left => {0}"), //10     
            new Account(PWD_LIFE_ACCESSES, "x_password_lifespan_accesses => {0}"), //11    
            new Account(PWD_LIFE_DAYS, "x_password_lifespan_days => {0}"), //12     
            new Account(EMP_ID, "x_employee_id => {0}"), //13     
            new Account(EMAIL, "x_email_address => {0}"), //14     
            new Account(FAX, "x_fax => {0}"), //15     
            new Account(CUST_ID, "x_customer_id => {0}"), //16     
            new Account(SUPP_ID, "x_supplier_id => {0}"), //17     
            //    new Account(EMP_NUM), //18     
            //    new Account(PERSON_FULLNAME), //19     
            //    new Account(NPW_NUM), //20
            new Account(PERSON_PARTY_ID), //21     
            //    new Account(RESP), //22
            //    new Account(RESPKEYS), //23     
            //    new Account(SEC_ATTRS), //24     
            //    new Account(EXP_PWD), //25  
            new Account(Uid.NAME, USER_NAME, null) //26
    };

    /**
     * The map of attribute names and Account values
     */
    static final Map<String, String> UM = CollectionUtil.<String> newCaseInsensitiveMap();

    /**
     * Initialization of the map
     */
    static {
        for (Account ui : UI) {
            if (ui.attrName != null) {
                UM.put(ui.attrName, ui.columnName);
            }
        }
    }

    String attrName;
    String columnName;
    String parameterName;

    /**
     * @param attrName
     * @param columnName
     * @param parameterName
     */
    public Account(String attrName, String columnName, String parameterName) {
        this.attrName = attrName;
        this.columnName = columnName;
        this.parameterName = parameterName;
    }

    /**
     * @param attrName
     * @param columnName
     * @param parameterName
     */
    public Account(String columnName, String parameterName) {
        this(columnName, columnName, parameterName);
    }

    /**
     * @param attrName
     * @param columnName
     * @param parameterName
     */
    public Account(String columnName) {
        this(columnName, columnName, null);
    }

    /**
     * Get the Account Object Class Info
     * 
     * @return ObjectClassInfo value
     */
    public static ObjectClassInfo getSchema() {
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
        // aoc.addAttributeInfo(OperationalAttributeInfos.RESET_PASSWORD); 
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
     * Evaluate the User Values Map
     * 
     * @param oclass
     *            the object class
     * @param attrs
     *            the set of attributes
     * @param options
     *            the operation options
     * @param create
     *            true/false for create/update
     * @return a map of userValues
     */
    static Map<String, SQLParam> getUserValuesMap(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options,
            boolean create) {

        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }

        final Map<String, SQLParam> userValues = new HashMap<String, SQLParam>();

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
                userValues.put(Account.USER_NAME, new SQLParam(AttributeUtil.getAsStringValue(attr).toUpperCase(),
                        Types.VARCHAR));
            } else {
                final SQLParam currentDate = new SQLParam(new java.sql.Date(System.currentTimeMillis()), Types.DATE);
                if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                    /*
                    cstmt1.setString(3, (String)accountAttrChanges.get(UNENCRYPT_PWD));
                    //only set 'password_date' if password changed
                    if ((String)accountAttrChanges.get(UNENCRYPT_PWD) != null) {
                        cstmt1.setDate(9, new java.sql.Date(
                                (new java.util.Date().getTime()) ));
                    }*/
                    if (AttributeUtil.getSingleValue(attr) != null) {
                        userValues.put(Account.UNENCRYPT_PWD, new SQLParam(AttributeUtil.getGuardedStringValue(attr)));
                        userValues.put(Account.PWD_DATE, currentDate);
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
                        userValues.put(Account.PWD_DATE, new SQLParam(Account.NULL_DATE));
                    } else if (create) {
                        userValues.put(Account.LAST_LOGON_DATE, currentDate);
                    }

                } else if (attr.is(Account.OWNER)) {
                    //         cstmt1.setString(2, (String)accountAttrChanges.get(OWNER));
                    userValues.put(Account.OWNER, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
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
                        continue;
                    }
                    final String dateString = AttributeUtil.getAsStringValue(attr);
                    if (Account.SYSDATE.equalsIgnoreCase(dateString)) {
                        userValues.put(Account.END_DATE, new SQLParam(Account.SYSDATE));
                        continue;
                    }
                    Timestamp tms = OracleERPUtil.stringToTimestamp(dateString);
                    userValues.put(Account.END_DATE, new SQLParam(tms, Types.TIMESTAMP));
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
                    } else {
                        userValues
                                .put(Account.DESCR, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
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
                        userValues.put(Account.PWD_ACCESSES_LEFT, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.PWD_ACCESSES_LEFT, new SQLParam(AttributeUtil.getIntegerValue(attr),
                                Types.INTEGER));
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
                        userValues.put(Account.PWD_LIFE_ACCESSES, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.PWD_LIFE_ACCESSES, new SQLParam(AttributeUtil.getIntegerValue(attr),
                                Types.INTEGER));
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
                        userValues.put(Account.PWD_LIFE_DAYS, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues.put(Account.PWD_LIFE_DAYS, new SQLParam(AttributeUtil.getIntegerValue(attr),
                                Types.INTEGER));
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
                        userValues.put(Account.EMP_ID, new SQLParam(Account.NULL_NUMBER, Types.VARCHAR));
                    } else {
                        userValues
                                .put(Account.EMP_ID, new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
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
                    } else {
                        userValues
                                .put(Account.EMAIL, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
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
                    } else {
                        userValues.put(Account.FAX, new SQLParam(AttributeUtil.getAsStringValue(attr), Types.VARCHAR));
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
                    } else {
                        userValues.put(Account.CUST_ID,
                                new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
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
                    } else {
                        userValues.put(Account.SUPP_ID,
                                new SQLParam(AttributeUtil.getIntegerValue(attr), Types.INTEGER));
                    }
                } else if (AttributeUtil.isSpecial(attr)) {
                    log.ok("Unhandled special attribute {0}", attr.getName());
                } else {
                    log.ok("Unhandled attribute {0}", attr.getName());
                }
            }
        }
        //Check required attributes
        Assertions.nullCheck(userValues.get(Account.USER_NAME), Name.NAME);
        Assertions.nullCheck(userValues.get(Account.UNENCRYPT_PWD), OperationalAttributes.PASSWORD_NAME);
        Assertions.nullCheck(userValues.get(Account.OWNER), Account.OWNER);
        return userValues;
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
    static String getUserCallSQL(Map<String, SQLParam> userValues, boolean create, String schemaId) {
        final String fn = (create) ? Account.CREATE_FNC : Account.UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            SQLParam val = userValues.get(ui.columnName);
            if (val == null) {
                continue; //skip all non setup values
            }
            if (!first)
                body.append(", ");
            if (isDefault(val)) {
                body.append(MessageFormat.format(ui.parameterName, val.getValue()));
            } else {
                body.append(MessageFormat.format(ui.parameterName, Account.Q)); // Non default values will be binded
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
    static List<SQLParam> getUserSQLParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            final SQLParam val = userValues.get(ui.columnName);
            if (val == null) {
                continue; //skip all non setup values
            }
            if (!isDefault(val)) {
                ret.add(new SQLParam(val));
            }
        }
        return ret;
    }

    /**
     * @param val
     * @return true/false if predefined default value
     */
    static boolean isDefault(SQLParam val) {
        return Account.SYSDATE.equals(val.getValue()) 
                || Account.NULL_NUMBER.equals(val.getValue()) 
                || Account.NULL_DATE.equals(val.getValue())
                || Account.NULL_CHAR.equals(val.getValue());
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
    public static String getAllSQL(Map<String, SQLParam> userValues, boolean create, String schemaId) {
        final String fn = (create) ? Account.CREATE_FNC : Account.UPDATE_FNC;
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
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
    public static List<SQLParam> getAllSQLParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();

        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            final SQLParam val = userValues.get(ui.columnName);
            ret.add(val);
        }
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
    static String getUserUpdateNullsSQL(Map<String, SQLParam> userValues, String schemaId) {
        StringBuilder body = new StringBuilder("x_user_name => ?, x_owner => upper(?)");
        for (Account ui : Account.UI) {
            if (ui.parameterName == null) {
                continue; //skip all non call parameterName values                    
            }
            SQLParam val = userValues.get(ui.columnName);
            if (val == null) {
                continue; //skip all non setup values
            }
            if (isDefault(val)) {
                body.append(", ");
                body.append(MessageFormat.format(ui.parameterName, val.getValue())); // Update just default 
            }
        }

        final String sql = OracleERPUtil.CURLY_BEGIN
                + MessageFormat.format(Account.SQL_CALL, schemaId, Account.UPDATE_FNC, body.toString())
                + OracleERPUtil.CURLY_END;
        log.ok("getUpdateDefaultsSQL {0}", sql);
        return sql;
    }

    /**
     * Return the create/update parameters
     * S
     * @param userValues
     *            the Map of user values
     * 
     * @return a <CODE>List</CODE> sql object list
     */
    static List<SQLParam> getUserUpdateNullsParams(Map<String, SQLParam> userValues) {
        final List<SQLParam> ret = new ArrayList<SQLParam>();
        ret.add(userValues.get(Account.USER_NAME)); //1
        ret.add(userValues.get(Account.OWNER)); //2
        return ret;
    }

    /**
     * Test for null attribute values
     * 
     * @param userValues
     *            the Map of user values
     * @return <code>boolean<code> true if the update null attributes is needed
     */
    static boolean isUpdateNeeded(Map<String, SQLParam> userValues) {
        for (SQLParam value : userValues.values()) {
            if (isDefault(value)) {
                return true;
            }
        }
        return false;
    }    
}
