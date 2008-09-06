/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.racf;

public interface RacfConstants {
    // Single-valued read-write Attributes
    //
    public static final String ATTR_DATA                  = "racfInstallationData";
    public static final String ATTR_MODEL                 = "racfDatasetModel";
    public static final String ATTR_OWNER                 = "racfOwner";
    public static final String ATTR_PASSWORD              = "racfPassword";
    public static final String ATTR_PROGRAMMER_NAME       = "racfProgrammerName";
    public static final String ATTR_DEFAULT_GROUP         = "racfDefaultGroup";
    public static final String ATTR_SECURITY_LEVEL        = "racfSecurityLevel";
    public static final String ATTR_SECURITY_CAT_LIST     = "racfSecurityCategoryList";
    public static final String ATTR_REVOKE_DATE           = "racfRevokeDate";
    public static final String ATTR_RESUME_DATE           = "racfResumeDate";
    public static final String ATTR_LOGON_DAYS            = "racfLogonDays";
    public static final String ATTR_LOGON_TIME            = "racfLogonTime";
    public static final String ATTR_CLASS_NAME            = "racfClassName";
    public static final String ATTR_CONNECT_GROUP         = "racfConnectGroupName";
    public static final String ATTR_SECURITY_LABEL        = "racfSecurityLabel";
    public static final String ATTR_DPF_DATA_APP          = "SAFDfpDataApplication";
    public static final String ATTR_DPF_DATA_CLASS        = "SAFDfpDataClass";
    public static final String ATTR_DPF_MGMT_CLASS        = "SAFDfpManagementClass";
    public static final String ATTR_DPF_STORAGE_CLASS     = "SAFDfpStorageClass";
    public static final String ATTR_TSO_ACCOUNT_NUMBER    = "SAFAccountNumber";
    public static final String ATTR_TSO_DEFAULT_CMD       = "SAFDefaultCommand";
    public static final String ATTR_TSO_DESTINATION       = "SAFDestination";
    public static final String ATTR_TSO_MESSAGE_CLASS     = "SAFMessageClass";
    public static final String ATTR_TSO_DEFAULT_LOGIN     = "SAFDefaultLoginProc";
    public static final String ATTR_TSO_LOGIN_SIZE        = "SAFLoginSize";
    public static final String ATTR_TSO_MAX_REGION_SIZE   = "SAFMaximumRegionSize";
    public static final String ATTR_TSO_DEFAULT_SYSOUT    = "SAFDefaultSysoutClass";
    public static final String ATTR_TSO_USERDATA          = "SAFUserData";
    public static final String ATTR_TSO_DEFAULT_UNIT      = "SAFDefaultUnit";
    public static final String ATTR_TSO_SECURITY_LABEL    = "SAFTsoSecurityLabel";
    public static final String ATTR_LANG_PRIMARY          = "racfPrimaryLanguage";
    public static final String ATTR_LANG_SECONDARY        = "racfSecondaryLanguage";
    public static final String ATTR_CICS_OPER_ID          = "racfOperatorIdentification";
    public static final String ATTR_CICS_OPER_CLASS       = "racfOperatorClass";
    public static final String ATTR_CICS_OPER_PRIORITY    = "racfOperatorPriority";
    public static final String ATTR_CICS_OPER_RESIGNON    = "racfOperatorReSignon";
    public static final String ATTR_CICS_TERM_TIMEOUT     = "racfTerminalTimeout";
    public static final String ATTR_OP_STORAGE            = "racfStorageKeyword";
    public static final String ATTR_OP_AUTH               = "racfAuthKeyword";
    public static final String ATTR_OP_MFORM              = "racfMformKeyword";
    public static final String ATTR_OP_LEVEL              = "racfLevelKeyword";
    public static final String ATTR_OP_MONITOR            = "racfMonitorKeyword";
    public static final String ATTR_OP_ROUTCODE           = "racfRoutcodeKeyword";
    public static final String ATTR_OP_LOG_CMD_RESPONSE   = "racfLogCommandResponseKeyword";
    public static final String ATTR_OP_MGID               = "racfMGIDKeyword";
    public static final String ATTR_OP_DOM                = "racfDOMKeyword";
    public static final String ATTR_OP_KEY                = "racfKEYKeyword";
    public static final String ATTR_OP_CMDSYS             = "racfCMDSYSKeyword";
    public static final String ATTR_OP_UD                 = "racfUDKeyword";
    public static final String ATTR_OP_MSCOPE_SYSTEMS     = "racfMscopeSystems";
    public static final String ATTR_OP_ALTGROUP           = "racfAltGroupKeyword";
    public static final String ATTR_OP_AUTO               = "racfAutoKeyword";
    public static final String ATTR_WA_USER_NAME          = "racfWorkAttrUserName";
    public static final String ATTR_WA_BUILDING           = "racfBuilding";
    public static final String ATTR_WA_DEPARTMENT         = "racfDepartment";
    public static final String ATTR_WA_ROOM               = "racfRoom";
    public static final String ATTR_WA_ADDRESS_LINE1      = "racfAddressLine1";
    public static final String ATTR_WA_ADDRESS_LINE2      = "racfAddressLine2";
    public static final String ATTR_WA_ADDRESS_LINE3      = "racfAddressLine3";
    public static final String ATTR_WA_ADDRESS_LINE4      = "racfAddressLine4";
    public static final String ATTR_WA_ACCOUNT_NUMBER     = "racfWorkAttrAccountNumber";
    public static final String ATTR_OMVS_UID              = "racfOmvsUid";
    public static final String ATTR_OMVS_HOME             = "racfOmvsHome";
    public static final String ATTR_OMVS_INIT_PROGRAM     = "racfOmvsInitialProgram";
    public static final String ATTR_OMVS_MAX_CPUTIME      = "racfOmvsMaximumCPUTime";
    public static final String ATTR_OMVS_MAX_ADDR_SPACE   = "racfOmvsMaximumAddressSpaceSize";
    public static final String ATTR_OMVS_MAX_FILES        = "racfOmvsMaximumFilesPerProcess";
    public static final String ATTR_OMVS_MAX_THREADS      = "racfOmvsMaximumThreadsPerProcess";
    public static final String ATTR_OMVS_MAX_MEMORY_MAP   = "racfOmvsMaximumMemoryMapArea";
    public static final String ATTR_NV_NINITIALCMD        = "racfNetviewInitialCommand";
    public static final String ATTR_NV_DEFAULT_CONSOLE    = "racfDefaultConsoleName";
    public static final String ATTR_NV_CTL                = "racfCTLKeyword";
    public static final String ATTR_NV_MESSAGE_RECEIVER   = "racfMessageReceiverKeyword";
    public static final String ATTR_NV_OPERATOR_CLASS     = "racfNetviewOperatorClass";
    public static final String ATTR_NV_DOMAINS            = "racfDomains";
    public static final String ATTR_NV_NGMFADM            = "racfNGMFADMKeyword";
    public static final String ATTR_NV_DCE_UUID           = "racfDCEUUID";
    public static final String ATTR_NV_DCE_PRINCIPAL      = "racfDCEPrincipal";
    public static final String ATTR_NV_DCE_HOME_CELL      = "racfDCEHomeCell";
    public static final String ATTR_NV_DCE_HOME_CELL_UUID = "racfDCEHomeCellUUID";
    public static final String ATTR_NV_DCE_AUTOLOGIN      = "racfDCEAutoLogin";
    public static final String ATTR_OVM_UID               = "racfOvmUid";
    public static final String ATTR_OVM_HOME              = "racfOvmHome";
    public static final String ATTR_OVM_INITIAL_PROGRAM   = "racfOvmInitialProgram";
    public static final String ATTR_OVM_FILESYSTEM_ROOT   = "racfOvmFileSystemRoot";
    public static final String ATTR_LN_SHORT_NAME         = "racfLNotesShortName";
    public static final String ATTR_NDS_USER_NAME         = "racfNDSUserName";
    public static final String ATTR_KERB_NAME             = "krbPrincipalName";
    public static final String ATTR_KERB_MAX_TICKET_LIFE  = "maxTicketAge";
    public static final String ATTR_KERB_ENCRYPT          = "racfEncryptType";
    public static final String ATTR_PROXY_BINDDN          = "racfLDAPBindDN";
    public static final String ATTR_PROXY_BINDPW          = "racfLDAPBindPw";
    public static final String ATTR_PROXY_HOST            = "racfLDAPHost";
    
    // Attributes on Group
    //
    public static final String ATTR_SUP_GROUP             = "racfSuperiorGroup";
    public static final String ATTR_TERM_UACC             = "racfGroupNoTermUAC";
    public static final String ATTR_UNIVERSAL             = "racfGroupUniversal";
    
    // Attributes on Connection
    //
    public static final String ATTR_CONNECT_ATTRS         = "racfConnectAttributes";
    public static final String ATTR_CONNECT_AUTHORITY     = "racfConnectGroupAuthority";
    public static final String ATTR_CONNECT_DATE          = "racfConnectAuthDate";
    public static final String ATTR_CONNECT_COUNT         = "racfConnectCount";
    public static final String ATTR_CONNECT_OWNER         = "racfConnectOwner";
    public static final String ATTR_CONNECT_RESUME        = "racfConnectResumeDate";
    public static final String ATTR_CONNECT_REVOKE        = "racfConnectRevokeDate";
    public static final String ATTR_CONNECT_UACC          = "racfConnectGroupUACC";

    // Multi-values read-write attributes
    //
    public static final String ATTR_ATTRIBUTES            = "racfAttributes";

    // Read-only Attributes
    //
    public static final String ATTR_AUTHORIZATION_DATE    = "racfAuthorizationDate";
    public static final String ATTR_PASSWORD_INTERVAL     = "racfPasswordInterval";
    public static final String ATTR_PASSWORD_CHANGE       = "racfPasswordChangeDate";
    public static final String ATTR_LAST_ACCESS           = "racfLastAccess";
    public static final String ATTR_KERB_KEY_VERSION      = "racfCurKeyVersion";
    
    public static final String ATTR_SUB_GROUP             = "racfSubGroupName";
    public static final String ATTR_GROUP_USERIDS         = "racfGroupUserids";
    
    // Identity 'attributes'
    //
    public static final String RACF_ID                    = "racfid";
    public static final String RACF_USERID                = "racfuserid";
    public static final String RACF_GROUPID               = "racfgroupid";
    
}
