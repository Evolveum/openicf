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

import org.identityconnectors.framework.common.objects.ObjectClass;

/**
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
public class OracleERPUtil {

    static final String MSG = "Oracle ERP: ";    
    
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
    
    
    
    static final String MENU_IDS = "menuIds";

    static final String APP_ID_FORM_IDS = "formIds";
    static final String RO_APP_ID_FORM_IDS = "readOnlyFormIds";
    static final String RW_APP_ID_FORM_IDS = "readWriteOnlyFormIds";

    static final String FORM_NAMES = "formNames";
    static final String RO_FORM_NAMES = "readOnlyFormNames";
    static final String RW_FORM_NAMES = "readWriteOnlyFormNames";

    static final String FUNCTION_IDS = "functionIds";
    static final String RO_FUNCTION_IDS = "readOnlyFunctionIds";
    static final String RW_FUNCTION_IDS = "readWriteOnlyFunctionIds";

    static final String FUNCTION_NAMES = "fFunctionNames";
    static final String RO_FUNCTION_NAMES = "readOnlyFunctionNames";
    static final String RW_FUNCTION_NAMES = "readWriteOnlyFunctionNames";

    static final String USER_MENU_NAMES = "userMenuNames";
    static final String USER_FUNCTION_NAMES = "userFunctionNames";

    static final String USER_FORM_NAMES = "userFormNames";
    static final String RO_USER_FORM_NAMES = "readOnlyUserFormNames";
    static final String RW_USER_FORM_NAMES = "readWriteOnlyUserFormNames";

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
}
