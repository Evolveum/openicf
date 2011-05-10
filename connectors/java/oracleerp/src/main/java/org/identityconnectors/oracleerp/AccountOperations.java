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

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;

/**
 * Account Names
 *
 * @author petr
 * @version 1.0
 * @since 1.0
 */
class AccountOperations  {

    /**
     * The map of column name parameters mapping
     */
    public static final List<CallParam> CALL_PARAMS = new ArrayList<CallParam>();

    /**
     * The map of column name parameters mapping
     */
    public static final Map<String, CallParam> PARAM_MAP = CollectionUtil.<CallParam>newCaseInsensitiveMap();


    /**
     * The column names to get
     */
    public static final Set<String> FND_USER_COLS = CollectionUtil.newCaseInsensitiveSet();

    /**
     * The column names to get
     */
    public static final Set<String> PER_PEOPLE_COLS = CollectionUtil.newCaseInsensitiveSet();

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
        FND_USER_COLS.add(PWD_LIFESPAN_ACCESSES); //11
        FND_USER_COLS.add(PWD_LIFESPAN_DAYS); //12
        FND_USER_COLS.add(EMP_ID); //13
        FND_USER_COLS.add(EMAIL); //14
        FND_USER_COLS.add(FAX); //15
        FND_USER_COLS.add(CUST_ID); //16
        FND_USER_COLS.add(SUPP_ID); //17
        FND_USER_COLS.add(PERSON_PARTY_ID); //18);

        PER_PEOPLE_COLS.add(EMP_NUM);
        PER_PEOPLE_COLS.add(NPW_NUM);
        PER_PEOPLE_COLS.add(FULL_NAME);

        CALL_PARAMS.add(new CallParam(USER_NAME, "x_user_name => {0}", Types.VARCHAR)); //1
        CALL_PARAMS.add(new CallParam(OWNER, "x_owner => upper({0})", Types.VARCHAR)); //2   write only
        CALL_PARAMS.add(new CallParam(UNENCRYPT_PWD, "x_unencrypted_password => {0}", Types.NULL));//3 write only
        CALL_PARAMS.add(new CallParam(SESS_NUM, "x_session_number => {0}", Types.INTEGER)); //4
        CALL_PARAMS.add(new CallParam(START_DATE, "x_start_date => {0}", Types.TIMESTAMP)); //5
        CALL_PARAMS.add(new CallParam(END_DATE, "x_end_date => {0}", Types.TIMESTAMP)); //6
        CALL_PARAMS.add(new CallParam(LAST_LOGON_DATE, "x_last_logon_date => {0}", Types.DATE)); //7
        CALL_PARAMS.add(new CallParam(DESCR, "x_description => {0}", Types.VARCHAR)); //8
        CALL_PARAMS.add(new CallParam(PWD_DATE, "x_password_date => {0}", Types.DATE)); //9
        CALL_PARAMS.add(new CallParam(PWD_ACCESSES_LEFT, "x_password_accesses_left => {0}", Types.INTEGER)); //10
        CALL_PARAMS.add(new CallParam(PWD_LIFESPAN_ACCESSES, "x_password_lifespan_accesses => {0}", Types.INTEGER)); //11
        CALL_PARAMS.add(new CallParam(PWD_LIFESPAN_DAYS, "x_password_lifespan_days => {0}", Types.INTEGER)); //12
        CALL_PARAMS.add(new CallParam(EMP_ID, "x_employee_id => {0}", Types.INTEGER)); //13
        CALL_PARAMS.add(new CallParam(EMAIL, "x_email_address => {0}", Types.VARCHAR)); //14
        CALL_PARAMS.add(new CallParam(FAX, "x_fax => {0}", Types.VARCHAR)); //15
        CALL_PARAMS.add(new CallParam(CUST_ID, "x_customer_id => {0}", Types.INTEGER)); //16
        CALL_PARAMS.add(new CallParam(SUPP_ID, "x_supplier_id => {0}", Types.INTEGER)); //17

        // Init call map
        for (CallParam cp : CALL_PARAMS) {
            PARAM_MAP.put(cp.name, cp);
        }
    }

    /**
     * The account
     */
    private AccountOperations() {
        //Not instanceable
        throw new AssertionError();
    }

    /**
     * Call param class
     * @author Petr Jung
     * @version $Revision 1.0$
     * @since 1.0
     */
    public static final class CallParam {
        /** name */
        final public String name;
        /** expression */
        final public String expression;
        /** sqlType */
        final public int sqlType;
        /**
         * Construcrot of final class
         * @param name
         * @param expression
         * @param sqlType
         */
        public CallParam(String name, String expression, int sqlType) {
            this.name = name;
            this.expression = expression;
            this.sqlType = sqlType;
        }
    }
}
