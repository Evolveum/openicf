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
package org.identityconnectors.racf;

public interface RacfConstants {
    // Single-valued read-write Attributes
    //
    public static final String ATTR_LDAP_ENABLED               = "fakeEnabled";
    public static final String ATTR_LDAP_EXPIRED               = "fakeExpired";
    
    public static final String ATTR_LDAP_DATA                  = "racfInstallationData";
    public static final String ATTR_LDAP_MODEL                 = "racfDatasetModel";
    public static final String ATTR_LDAP_OWNER                 = "racfOwner";
    public static final String ATTR_LDAP_PASSWORD              = "racfPassword";
    public static final String ATTR_LDAP_PASSWORD_ENVELOPE     = "racfPasswordEnvelope";
    public static final String ATTR_LDAP_PROGRAMMER_NAME       = "racfProgrammerName";
    public static final String ATTR_LDAP_DEFAULT_GROUP         = "racfDefaultGroup";
    public static final String ATTR_LDAP_SECURITY_LEVEL        = "racfSecurityLevel";
    public static final String ATTR_LDAP_SECURITY_CAT_LIST     = "racfSecurityCategoryList";
    public static final String ATTR_LDAP_REVOKE_DATE           = "racfRevokeDate";
    public static final String ATTR_LDAP_RESUME_DATE           = "racfResumeDate";
    public static final String ATTR_LDAP_LOGON_DAYS            = "racfLogonDays";
    public static final String ATTR_LDAP_LOGON_TIME            = "racfLogonTime";
    public static final String ATTR_LDAP_CLASS_NAME            = "racfClassName";
    public static final String ATTR_LDAP_SECURITY_LABEL        = "racfSecurityLabel";
    public static final String ATTR_LDAP_DPF_DATA_APP          = "SAFDfpDataApplication";
    public static final String ATTR_LDAP_DPF_DATA_CLASS        = "SAFDfpDataClass";
    public static final String ATTR_LDAP_DPF_MGMT_CLASS        = "SAFDfpManagementClass";
    public static final String ATTR_LDAP_DPF_STORAGE_CLASS     = "SAFDfpStorageClass";
    public static final String ATTR_LDAP_TSO_ACCOUNT_NUMBER    = "SAFAccountNumber";
    public static final String ATTR_LDAP_TSO_DEFAULT_CMD       = "SAFDefaultCommand";
    public static final String ATTR_LDAP_TSO_DESTINATION       = "SAFDestination";
    public static final String ATTR_LDAP_TSO_MESSAGE_CLASS     = "SAFMessageClass";
    public static final String ATTR_LDAP_TSO_DEFAULT_LOGIN     = "SAFDefaultLoginProc";
    public static final String ATTR_LDAP_TSO_LOGON_SIZE        = "SAFLogonSize";
    public static final String ATTR_LDAP_TSO_MAX_REGION_SIZE   = "SAFMaximumRegionSize";
    public static final String ATTR_LDAP_TSO_DEFAULT_SYSOUT    = "SAFDefaultSysoutClass";
    public static final String ATTR_LDAP_TSO_USERDATA          = "SAFUserData";
    public static final String ATTR_LDAP_TSO_DEFAULT_UNIT      = "SAFDefaultUnit";
    public static final String ATTR_LDAP_TSO_SECURITY_LABEL    = "SAFTsoSecurityLabel";
    public static final String ATTR_LDAP_LANG_PRIMARY          = "racfPrimaryLanguage";
    public static final String ATTR_LDAP_LANG_SECONDARY        = "racfSecondaryLanguage";
    public static final String ATTR_LDAP_CICS_OPER_ID          = "racfOperatorIdentification";
    public static final String ATTR_LDAP_CICS_OPER_CLASS       = "racfOperatorClass";
    public static final String ATTR_LDAP_CICS_OPER_PRIORITY    = "racfOperatorPriority";
    public static final String ATTR_LDAP_CICS_OPER_RESIGNON    = "racfOperatorReSignon";
    public static final String ATTR_LDAP_CICS_TERM_TIMEOUT     = "racfTerminalTimeout";
    public static final String ATTR_LDAP_OP_STORAGE            = "racfStorageKeyword";
    public static final String ATTR_LDAP_OP_AUTH               = "racfAuthKeyword";
    public static final String ATTR_LDAP_OP_MFORM              = "racfMformKeyword";
    public static final String ATTR_LDAP_OP_LEVEL              = "racfLevelKeyword";
    public static final String ATTR_LDAP_OP_MONITOR            = "racfMonitorKeyword";
    public static final String ATTR_LDAP_OP_ROUTCODE           = "racfRoutcodeKeyword";
    public static final String ATTR_LDAP_OP_LOG_CMD_RESPONSE   = "racfLogCommandResponseKeyword";
    public static final String ATTR_LDAP_OP_MGID               = "racfMGIDKeyword";
    public static final String ATTR_LDAP_OP_DOM                = "racfDOMKeyword";
    public static final String ATTR_LDAP_OP_KEY                = "racfKEYKeyword";
    public static final String ATTR_LDAP_OP_CMDSYS             = "racfCMDSYSKeyword";
    public static final String ATTR_LDAP_OP_UD                 = "racfUDKeyword";
    public static final String ATTR_LDAP_OP_MSCOPE_SYSTEMS     = "racfMscopeSystems";
    public static final String ATTR_LDAP_OP_ALTGROUP           = "racfAltGroupKeyword";
    public static final String ATTR_LDAP_OP_AUTO               = "racfAutoKeyword";
    public static final String ATTR_LDAP_WA_USER_NAME          = "racfWorkAttrUserName";
    public static final String ATTR_LDAP_WA_BUILDING           = "racfBuilding";
    public static final String ATTR_LDAP_WA_DEPARTMENT         = "racfDepartment";
    public static final String ATTR_LDAP_WA_ROOM               = "racfRoom";
    public static final String ATTR_LDAP_WA_ADDRESS_LINE1      = "racfAddressLine1";
    public static final String ATTR_LDAP_WA_ADDRESS_LINE2      = "racfAddressLine2";
    public static final String ATTR_LDAP_WA_ADDRESS_LINE3      = "racfAddressLine3";
    public static final String ATTR_LDAP_WA_ADDRESS_LINE4      = "racfAddressLine4";
    public static final String ATTR_LDAP_WA_ACCOUNT_NUMBER     = "racfWorkAttrAccountNumber";
    public static final String ATTR_LDAP_OMVS_UID              = "racfOmvsUid";
    public static final String ATTR_LDAP_OMVS_HOME             = "racfOmvsHome";
    public static final String ATTR_LDAP_OMVS_INIT_PROGRAM     = "racfOmvsInitialProgram";
    public static final String ATTR_LDAP_OMVS_MAX_CPUTIME      = "racfOmvsMaximumCPUTime";
    public static final String ATTR_LDAP_OMVS_MAX_ADDR_SPACE   = "racfOmvsMaximumAddressSpaceSize";
    public static final String ATTR_LDAP_OMVS_MAX_FILES        = "racfOmvsMaximumFilesPerProcess";
    public static final String ATTR_LDAP_OMVS_MAX_THREADS      = "racfOmvsMaximumThreadsPerProcess";
    public static final String ATTR_LDAP_OMVS_MAX_MEMORY_MAP   = "racfOmvsMaximumMemoryMapArea";
    public static final String ATTR_LDAP_OMVS_MAX_PROCESSES    = "racfOmvsMaximumProcessesPerUID";
    public static final String ATTR_LDAP_NV_INITIALCMD         = "racfNetviewInitialCommand";
    public static final String ATTR_LDAP_NV_DEFAULT_CONSOLE    = "racfDefaultConsoleName";
    public static final String ATTR_LDAP_NV_CTL                = "racfCTLKeyword";
    public static final String ATTR_LDAP_NV_MESSAGE_RECEIVER   = "racfMessageReceiverKeyword";
    public static final String ATTR_LDAP_NV_OPERATOR_CLASS     = "racfNetviewOperatorClass";
    public static final String ATTR_LDAP_NV_DOMAINS            = "racfDomains";
    public static final String ATTR_LDAP_NV_NGMFADM            = "racfNGMFADMKeyword";
    public static final String ATTR_LDAP_NV_DCE_UUID           = "racfDCEUUID";
    public static final String ATTR_LDAP_NV_DCE_PRINCIPAL      = "racfDCEPrincipal";
    public static final String ATTR_LDAP_NV_DCE_HOME_CELL      = "racfDCEHomeCell";
    public static final String ATTR_LDAP_NV_DCE_HOME_CELL_UUID = "racfDCEHomeCellUUID";
    public static final String ATTR_LDAP_NV_DCE_AUTOLOGIN      = "racfDCEAutoLogin";
    public static final String ATTR_LDAP_OVM_UID               = "racfOvmUid";
    public static final String ATTR_LDAP_OVM_HOME              = "racfOvmHome";
    public static final String ATTR_LDAP_OVM_INITIAL_PROGRAM   = "racfOvmInitialProgram";
    public static final String ATTR_LDAP_OVM_FILESYSTEM_ROOT   = "racfOvmFileSystemRoot";
    public static final String ATTR_LDAP_LN_SHORT_NAME         = "racfLNotesShortName";
    public static final String ATTR_LDAP_NDS_USER_NAME         = "racfNDSUserName";
    public static final String ATTR_LDAP_KERB_NAME             = "krbPrincipalName";
    public static final String ATTR_LDAP_KERB_MAX_TICKET_LIFE  = "maxTicketAge";
    public static final String ATTR_LDAP_KERB_ENCRYPT          = "racfEncryptType";
    public static final String ATTR_LDAP_PROXY_BINDDN          = "racfLDAPBindDN";
    public static final String ATTR_LDAP_PROXY_BINDPW          = "racfLDAPBindPw";
    public static final String ATTR_LDAP_PROXY_HOST            = "racfLDAPHost";
    
    // Attributes on Group
    //
    public static final String ATTR_LDAP_SUP_GROUP             = "racfSuperiorGroup";
    public static final String ATTR_LDAP_TERM_UACC             = "racfGroupNoTermUAC";
    public static final String ATTR_LDAP_UNIVERSAL             = "racfGroupUniversal";
    public static final String ATTR_LDAP_OVM_GROUP_ID          = "racfOvmGroupId";
    public static final String ATTR_LDAP_OMVS_GROUP_ID         = "racfOmvsGroupId";
    
    // Attributes on Connection
    //
    public static final String ATTR_LDAP_CONNECT_ATTRS         = "racfConnectAttributes";
    public static final String ATTR_LDAP_CONNECT_AUTHORITY     = "racfConnectGroupAuthority";
    public static final String ATTR_LDAP_CONNECT_DATE          = "racfConnectAuthDate";
    public static final String ATTR_LDAP_CONNECT_COUNT         = "racfConnectCount";
    public static final String ATTR_LDAP_CONNECT_OWNER         = "racfConnectOwner";
    public static final String ATTR_LDAP_CONNECT_RESUME        = "racfConnectResumeDate";
    public static final String ATTR_LDAP_CONNECT_REVOKE        = "racfConnectRevokeDate";
    public static final String ATTR_LDAP_CONNECT_UACC          = "racfConnectGroupUACC";

    // Multi-values read-write attributes
    //
    public static final String ATTR_LDAP_ATTRIBUTES            = "racfAttributes";
    public static final String ATTR_LDAP_GROUPS                = "racfConnectGroupName";
    public static final String ATTR_LDAP_SUB_GROUPS            = "racfSubGroupName";

    // Read-only Attributes
    //
    public static final String ATTR_LDAP_AUTHORIZATION_DATE    = "racfAuthorizationDate";
    public static final String ATTR_LDAP_PASSWORD_INTERVAL     = "racfPasswordInterval";
    public static final String ATTR_LDAP_PASSWORD_CHANGE       = "racfPasswordChangeDate";
    public static final String ATTR_LDAP_LAST_ACCESS           = "racfLastAccess";
    public static final String ATTR_LDAP_KERB_KEY_VERSION      = "racfCurKeyVersion";
    
    public static final String ATTR_LDAP_SUB_GROUP             = "racfSubGroupName";
    public static final String ATTR_LDAP_GROUP_USERIDS         = "racfGroupUserids";
    
    // Identity 'attributes'
    //
    public static final String ATTR_LDAP_RACF_ID               = "racfid";
    public static final String ATTR_LDAP_RACF_USERID           = "racfuserid";
    public static final String ATTR_LDAP_RACF_GROUPID          = "racfgroupid";
    

    public static final String ATTR_CL_USERID                   = "RACF*USERID";
    
    public static final String ATTR_CL_ATTRIBUTES               = "RACF*ATTRIBUTES";
    public static final String ATTR_CL_GROUPS                   = "RACF*GROUPS";
    public static final String ATTR_CL_GROUP_CONN_OWNERS        = "RACF*GROUP-CONN-OWNERS";
    public static final String ATTR_CL_MASTER_CATALOG           = "CATALOG*MASTER CATALOG";
    public static final String ATTR_CL_USER_CATALOG             = "CATALOG*USER CATALOG";
    public static final String ATTR_CL_CATALOG_ALIAS            = "CATALOG*CATALOG ALIAS";
    public static final String ATTR_CL_OWNER                    = "RACF*OWNER";
    public static final String ATTR_CL_NAME                     = "RACF*NAME";
    public static final String ATTR_CL_DATA                     = "RACF*DATA";
    public static final String ATTR_CL_DFLTGRP                  = "RACF*DFLTGRP";
    public static final String ATTR_CL_EXPIRED                  = "RACF*EXPIRED";
    public static final String ATTR_CL_ENABLED                  = "RACF*ENABLED";
    public static final String ATTR_CL_PASSWORD                 = "RACF*PASSWORD";
    public static final String ATTR_CL_LAST_ACCESS              = "RACF*LAST ACCESS";
    public static final String ATTR_CL_REVOKE_DATE              = "RACF*REVOKE DATE";
    public static final String ATTR_CL_RESUME_DATE              = "RACF*RESUME DATE";
    public static final String ATTR_CL_PASSWORD_INTERVAL        = "RACF*PASSWORD INTERVAL";
    public static final String ATTR_CL_PASSDATE                 = "RACF*PASSDATE";
    public static final String ATTR_CL_TSO_DELETE_SEGMENT       = "TSO*Delete Segment";
    public static final String ATTR_CL_TSO_ACCTNUM              = "TSO*ACCTNUM";
    public static final String ATTR_CL_TSO_HOLDCLASS            = "TSO*HOLDCLASS";
    public static final String ATTR_CL_TSO_JOBCLASS             = "TSO*JOBCLASS";
    public static final String ATTR_CL_TSO_MSGCLASS             = "TSO*MSGCLASS";
    public static final String ATTR_CL_TSO_PROC                 = "TSO*PROC";
    public static final String ATTR_CL_TSO_SIZE                 = "TSO*SIZE";
    public static final String ATTR_CL_TSO_MAXSIZE              = "TSO*MAXSIZE";
    public static final String ATTR_CL_TSO_SYSOUTCLASS          = "TSO*SYSOUTCLASS";
    public static final String ATTR_CL_TSO_UNIT                 = "TSO*UNIT";
    public static final String ATTR_CL_TSO_SECLABEL             = "TSO*ECLABEL";
    public static final String ATTR_CL_TSO_USERDATA             = "TSO*USERDATA";
    public static final String ATTR_CL_TSO_COMMAND              = "TSO*COMMAND";
    public static final String ATTR_CL_OMVS_UID                 = "OMVS*UID";
    public static final String ATTR_CL_OMVS_HOME                = "OMVS*HOME";
    public static final String ATTR_CL_OMVS_PROGRAM             = "OMVS*PROGRAM";
    public static final String ATTR_CL_OMVS_CPUTIMEMAX          = "OMVS*CPUTIMEMAX";
    public static final String ATTR_CL_OMVS_ASSIZEMAX           = "OMVS*ASSIZEMAX";
    public static final String ATTR_CL_OMVS_FILEPROCMAX         = "OMVS*FILEPROCMAX";
    public static final String ATTR_CL_OMVS_PROCUSERMAX         = "OMVS*PROCUSERMAX";
    public static final String ATTR_CL_OMVS_THREADSMAX          = "OMVS*THREADSMAX";
    public static final String ATTR_CL_OMVS_MMAPAREAMAX         = "OMVS*MMAPAREAMAX";
    public static final String ATTR_CL_CICS_TIMEOUT             = "CICS*TIMEOUT";
    public static final String ATTR_CL_CICS_OPPRTY              = "CICS*OPPRTY";
    public static final String ATTR_CL_CICS_OPIDENT             = "CICS*OPIDENT";
    public static final String ATTR_CL_CICS_OPCLASS             = "CICS*OPCLASS";
    public static final String ATTR_CL_CICS_XRFSOFF             = "CICS*XRFSOFF";
    public static final String ATTR_CL_CICS_RSLKEY              = "CICS*RSLKEY";
    public static final String ATTR_CL_CICS_TSLKEY              = "CICS*TSLKEY";
    public static final String ATTR_CL_NETVIEW_OPCLASS          = "NETVIEW*OPCLASS";
    public static final String ATTR_CL_NETVIEW_NGMFVSPN         = "NETVIEW*NGMFVSPN";
    public static final String ATTR_CL_NETVIEW_NGMFADMN         = "NETVIEW*NGMFADMN";
    public static final String ATTR_CL_NETVIEW_MSGRECVR         = "NETVIEW*MSGRECVR";
    public static final String ATTR_CL_NETVIEW_IC               = "NETVIEW*IC";
    public static final String ATTR_CL_NETVIEW_DOMAINS          = "NETVIEW*DOMAINS";
    public static final String ATTR_CL_NETVIEW_CTL              = "NETVIEW*CTL";
    public static final String ATTR_CL_NETVIEW_CONSNAME         = "NETVIEW*CONSNAME";
    

    public static final String ATTR_CL_MEMBERS                  = "RACF*MEMBERS";
    public static final String ATTR_CL_SUPGROUP                 = "RACF*SUPGROUP";
}
