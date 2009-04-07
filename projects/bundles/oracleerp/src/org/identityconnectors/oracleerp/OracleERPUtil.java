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

import java.sql.Timestamp;

import org.identityconnectors.framework.common.objects.ObjectClass;

/**
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
public class OracleERPUtil {

    static final String MSG = "Oracle ERP: ";    
    
    private OracleERPUtil() {
        throw new AssertionError();
    }

    // Validate messages constants
    static final String MSG_NAME_BLANK = "name.blank";
    static final String MSG_PWD_BLANK = "pwd.blank";    
    static final String MSG_USER_BLANK = "user.blank";
    static final String MSG_USER_MODEL_BLANK = "user.model.blank";
    static final String MSG_USER_MODEL_NOT_FOUND="user.model.not.found";
    static final String MSG_PASSWORD_BLANK = "password.blank";
    static final String MSG_HOST_BLANK = "host.blank";
    static final String MSG_PORT_BLANK = "port.blank";
    static final String MSG_DATABASE_BLANK = "database.blank";
    static final String MSG_JDBC_DRIVER_BLANK = "jdbc.driver.blank";
    static final String MSG_JDBC_DRIVER_NOT_FOUND = "jdbc.driver.not.found";
    static final String MSG_ACCOUNT_OBJECT_CLASS_REQUIRED = "acount.object.class.required";
    static final String MSG_AUTHENTICATE_OP_NOT_SUPPORTED = "auth.op.not.supported";
    static final String MSG_AUTH_FAILED = "auth.op.failed";
    static final String MSG_INVALID_ATTRIBUTE_SET = "invalid.attribute.set";
    static final String MSG_UID_BLANK = "uid.blank";
    static final String MSG_RESULT_HANDLER_NULL = "result.handler.null";    
    
    /**
     * object class name definitions
     * responsibilities, responsibilityNames, applications, securityGroups, auditorResps
     */
    
    static final String RESPS = "responsibilities";
    static final String RESP = "responsibility";
    public static final ObjectClass RESP_OC = new ObjectClass(RESP);    

    static final String DIRECT_RESPS = "directResponsibilities";
    static final String DIRECT_RESP = "directResponsibility";
    public static final ObjectClass DIRECT_RESP_OC = new ObjectClass(DIRECT_RESP);    

    static final String INDIRECT_RESPS = "indirectResponsibilities";
    static final String INDIRECT_RESP = "indirectResponsibility";
    public static final ObjectClass INDIRECT_RESP_OC = new ObjectClass(INDIRECT_RESP); 
    
    static final String RESP_NAMES = "responsibilityNames";
    static final String RESP_NAME = "responsibilityName";
    public static final ObjectClass RESP_NAME_OC = new ObjectClass(RESP_NAME);        
    
    static final String APPS = "applications";
    static final String APP = "application";
    public static final ObjectClass APP_OC = new ObjectClass(APP);    
    
    static final String SEC_GROUPS = "securityGroups";
    static final String SEC_GROUP = "securityGroup";
    public static final ObjectClass SEC_GROUP_OC = new ObjectClass(SEC_GROUP);    
    
    static final String PATTERN = "searchPattern";
   

    // Auditor Data Object
    static final String AUDITOR_RESPS = "auditorResps";
    static final String AUDITOR_RESP = "auditorResp";
    /**
     * Auditor responsibilities has menus, forms, functions, 
     * Auditor attributes: activeRespsOnly
     */
    public static final ObjectClass AUDITOR_RESP_OC =  new ObjectClass(AUDITOR_RESP);

    static final String MENUS = "menus";
    static final String MENU = "menu";
    /**
     * Menu has forms
     * Menu attributes: id, name, userMenu
     */
    public static final ObjectClass MENU_OC =  new ObjectClass(MENU);

    static final String FORMS = "forms";
    static final String FORM = "form";
    /**
     * Form has functions
     * Form attributes: id, name, writable, userForm 
     */
    public static final ObjectClass FORM_OC =  new ObjectClass(FORM);
    
    static final String FUNCTIONS = "functions";
    static final String FUNCTION = "function";
    /**
     * Function
     * Function attributes: id, name, writeble, userFunction
     */
    public static final ObjectClass FUNCTION_OC =  new ObjectClass(FUNCTION);
    
    
    static final String ACTIVE_RESPS_ONLY = "activeRespsOnly";
    static final String SOB_NAME = "setOfBooksName";
    static final String SOB_ID = "setOfBooksId";
    static final String OU_NAME = "organizationalUnitName";
    static final String OU_ID = "organizationalUnitId";
    
    static final String DEFAULT_USER_NAME = "APPL"; // Default user name    
    
    
    
    // new version 11.5.10 does not use responsibility table, it uses 2 new views
    static final String RESPS_TABLE = "fnd_user_resp_groups";    
    static final String RESPS_DIRECT_VIEW = "fnd_user_resp_groups_direct";
    static final String RESPS_INDIRECT_VIEW = "fnd_user_resp_groups_indirect";
    static final String RESPS_ALL_VIEW = "fnd_user_resp_groups_all";    
    

    /**
     * 
     */
    public static final int ORACLE_TIMEOUT = 1800;

    static final String CURLY_BEGIN = "{ ";
    static final String CURLY_END = " }";    

    /**
     * @param dateString
     * @return the timestamp
     */
    public static Timestamp stringToTimestamp(final String dateString) {
        Timestamp tms;
        try {
            tms = Timestamp.valueOf(dateString);
        } catch (IllegalArgumentException expected) {
            tms = new Timestamp(new Long(dateString));
        }
        return tms;
    }
    
    /**
     * @param sqlSelect 
     * @param whereAnd
     * @return
     */
    public static String whereAnd(String sqlSelect, String whereAnd) {
        int iofw = sqlSelect.toUpperCase().indexOf("WHERE");
        return (iofw == -1) ? sqlSelect + " WHERE " + whereAnd : sqlSelect.substring(0, iofw) + "WHERE ("+sqlSelect.substring(iofw + 5) +") AND ( " + whereAnd + " )";
    }
    
    /**
     * Only accounts where clause
     */
    public static final String ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE =
        "(START_DATE - SYSDATE <= 0) AND ((END_DATE IS NULL) OR (END_DATE - SYSDATE > 0))";
}
