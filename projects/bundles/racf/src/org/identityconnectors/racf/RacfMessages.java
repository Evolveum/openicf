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

import java.util.ListResourceBundle;

public class RacfMessages extends ListResourceBundle {
    public static final String SUFFIX_NULL                    = "SUFFIX_NULL";
    public static final String HOST_NULL                      = "HOST_NULL";
    public static final String LDAP_PORT_NULL                 = "LDAP_PORT_NULL";
    public static final String ILLEGAL_LDAP_PORT              = "ILLEGAL_LDAP_PORT";
    public static final String TELNET_PORT_NULL               = "TELNET_PORT_NULL";
    public static final String ILLEGAL_TELNET_PORT            = "ILLEGAL_TELNET_PORT";
    public static final String SSL_NULL                       = "SSL_NULL";
    public static final String USERNAME_NULL                  = "USERNAME_NULL";
    public static final String PASSWORD_NULL                  = "PASSWORD_NULL";
    public static final String CONNECTION_CLASS_NULL          = "CONNECTION_CLASS_NULL";
    public static final String USERNAMES_NULL                 = "USERNAMES_NULL";
    public static final String PASSWORDS_NULL                 = "PASSWORDS_NULL";
    public static final String INTERVAL_NULL                  = "INTERVAL_NULL";
    public static final String SUPPORTED_SEGMENTS_NULL        = "SUPPORTED_SEGMENTS_NULL";
    public static final String UNINITIALIZED_CONNECTOR        = "UNINITIALIZED_CONNECTOR";
    public static final String MUST_BE_SINGLE_VALUE           = "MUST_BE_SINGLE_VALUE";
    public static final String UNKNOWN_ATTRIBUTE              = "UNKNOWN_ATTRIBUTE";
    public static final String PATTERN_FAILED                 = "PATTERN_FAILED";
    public static final String UNSUPPORTED_SCRIPTING_LANGUAGE = "UNSUPPORTED_SCRIPTING_LANGUAGE";
    public static final String NEED_LDAP                      = "NEED_LDAP";
    public static final String NEED_COMMAND_LINE              = "NEED_COMMAND_LINE";
    public static final String ATTRS_NO_LDAP                  = "ATTRS_NO_LDAP";
    public static final String ATTRS_NO_CL                    = "ATTRS_NO_CL";
    public static final String CONNECTION_DEAD                = "CONNECTION_DEAD";
    public static final String ERROR_IN_COMMAND               = "ERROR_IN_COMMAND";
    public static final String UNSUPPORTED_OBJECT_CLASS       = "UNSUPPORTED_OBJECT_CLASS";
    public static final String GROUP_NOT_EMPTY                = "GROUP_NOT_EMPTY";
    public static final String UNKNOWN_UID_TYPE               = "UNKNOWN_UID_TYPE";
    public static final String ERROR_IN_GET_USERS             = "ERROR_IN_GET_USERS";
    public static final String ERROR_IN_GET_GROUPS            = "ERROR_IN_GET_GROUPS";
    public static final String INCONSISTENT_CATALOG_ARGS      = "INCONSISTENT_CATALOG_ARGS";
    public static final String UNKNOWN_SEGMENT                = "UNKNOWN_SEGMENT";
    public static final String UNPARSEABLE_RESPONSE           = "UNPARSEABLE_RESPONSE";
    public static final String EXPIRED_NO_PASSWORD            = "EXPIRED_NO_PASSWORD";
    public static final String ERROR_IN_RACF_COMMAND          = "ERROR_IN_RACF_COMMAND";
    public static final String PAST_DISABLE_DATE              = "PAST_DISABLE_DATE";
    public static final String PAST_ENABLE_DATE               = "PAST_ENABLE_DATE";
    public static final String NO_VALUE_FOR_ATTRIBUTE         = "NO_VALUE_FOR_ATTRIBUTE";
    public static final String ATTRIBUTE_NOT_READABLE         = "ATTRIBUTE_NOT_READABLE";
    public static final String ATTRIBUTE_NOT_UPDATEABLE       = "ATTRIBUTE_NOT_UPDATEABLE";
    public static final String OWNER_INCONSISTENT             = "OWNER_INCONSISTENT";
    public static final String BAD_CONNECTION_INFO            = "BAD_CONNECTION_INFO";
    public static final String BAD_NAME_FILTER                = "BAD_NAME_FILTER";
    public static final String BAD_ATTRIBUTE_VALUE            = "BAD_ATTRIBUTE_VALUE";
    

    private static final String[][] _contents = {
        { SUFFIX_NULL,             "suffix may not be null" },
        { HOST_NULL,               "host name may not be null" },
        { LDAP_PORT_NULL,          "ldap port may not be null" },
        { ILLEGAL_LDAP_PORT,       "the value ''{0}'' is not a legal value for ldap port" },
        { TELNET_PORT_NULL,        "telnet port may not be null" },
        { ILLEGAL_TELNET_PORT,     "the value ''{0}'' is not a legal value for telnet port" },
        { SSL_NULL,                "ssl may not be null" },
        { USERNAME_NULL,           "user name may not be null" },
        { PASSWORD_NULL,           "password may not be null" },
        { CONNECTION_CLASS_NULL,   "connection class may not be null" },
        { PASSWORDS_NULL,          "passwords may not be null" },
        { INTERVAL_NULL,           "interval may not be null" },
        { SUPPORTED_SEGMENTS_NULL, "supported segments may not be null" },
        { UNINITIALIZED_CONNECTOR, "cannot create a new connection on an LDAP connector that has not been initialized" },
        { MUST_BE_SINGLE_VALUE,    "Must be single values" },
        { UNKNOWN_ATTRIBUTE,       "Unknown Attribute:{0}" },
        { PATTERN_FAILED,          "Pattern failed to match ''{0}''"},
        { UNSUPPORTED_SCRIPTING_LANGUAGE, "''{0}'' is not supported as a scripting langauge" },
        { NEED_LDAP,               "Must have LDAP connection for LDAP-style attributes" },
        { NEED_COMMAND_LINE,       "Must have command-line connection for command-line-style attributes" },
        { ATTRS_NO_LDAP,           "No segment name specified:{0}" },
        { ATTRS_NO_CL,             "Command Line attrs requested, but no command line connection" },
        { CONNECTION_DEAD,         "Connection dead" },
        { ERROR_IN_COMMAND,        "Error in command ''{0}''\n''{1}''" },
        { UNSUPPORTED_OBJECT_CLASS,"Unsupported ObjectClass:{0}" },
        { GROUP_NOT_EMPTY,         "Cannot delete group with members" },
        { UNKNOWN_UID_TYPE,        "Unknown type of Uid:{0}" },
        { ERROR_IN_GET_USERS,      "Error in Get Users:''{0}''" },
        { ERROR_IN_GET_GROUPS,     "Error in Get Groups:''{0}''" },
        { UNKNOWN_SEGMENT,         "Unknown segment for attribute ''{0}''" },
        { INCONSISTENT_CATALOG_ARGS, "Either all or none of the catalog arguments must be specified" },
        { UNPARSEABLE_RESPONSE,    "Unable to parse response from ''{0}'':''{1}''" },
        { EXPIRED_NO_PASSWORD,     "RACF requires that __PASSWORD__ be specified if __PASSWORD_EXPIRED__ is specified" },
        { ERROR_IN_RACF_COMMAND,   "Error in RACF command ''{0}''" },
        { PAST_DISABLE_DATE,       "__DISABLE_DATE__ is in the past" },
        { PAST_ENABLE_DATE,        "__ENABLE_DATE__ is in the past" },
        { NO_VALUE_FOR_ATTRIBUTE,  "No value specified for attribute ''{0}''" },
        { ATTRIBUTE_NOT_READABLE,  "Attribute ''{0}'' is not readable" },
        { ATTRIBUTE_NOT_UPDATEABLE, "Attribute ''{0}'' is not updateable" },
        { OWNER_INCONSISTENT,      "The owner and relationship target attributes must be the same size" },
        { BAD_CONNECTION_INFO,     "Either LDAP or Command Line connection information must be specified" },
        { BAD_NAME_FILTER,         "''{0}'' is not a valid filter value" },
        { BAD_ATTRIBUTE_VALUE,     "''{0}'' is not a valid ATTRIBUTE value" },
    };

    @Override
    protected Object[][] getContents() {
        return _contents;
    }
}
