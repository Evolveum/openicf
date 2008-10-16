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
package org.identityconnectors.vms;

import java.util.ListResourceBundle;

public class VmsMessages extends ListResourceBundle {
    public static final String INVALID_ATTR_MULTIPLICITY     = "INVALID_ATTR_MULTIPLICITY";
    public static final String INVALID_ATTR_VALUE            = "INVALID_ATTR_VALUE";
    public static final String UNKNOWN_ATTR_NAME             = "UNKNOWN_ATTR_NAME";
    public static final String EXCEPTION_IN_ATTR             = "EXCEPTION_IN_ATTR";
    public static final String ERROR_IN_CREATE               = "ERROR_IN_CREATE";
    public static final String ERROR_IN_MODIFY               = "ERROR_IN_MODIFY";
    public static final String ERROR_IN_GETDATE               = "ERROR_IN_GETDATE";
    public static final String ERROR_IN_DELETE               = "ERROR_IN_DELETE";
    public static final String ERROR_IN_SCRIPT               = "ERROR_IN_SCRIPT";
    public static final String BAD_SCRIPT_LANGUAGE           = "BAD_SCRIPT_LANGUAGE";
    public static final String ERROR_IN_SEARCH               = "ERROR_IN_SEARCH";
    public static final String ERROR_IN_CREATE2              = "ERROR_IN_CREATE2";
    public static final String ERROR_IN_MODIFY2              = "ERROR_IN_MODIFY2";
    public static final String ERROR_IN_DELETE2              = "ERROR_IN_DELETE2";
    public static final String PROTOCOL_NULL                 = "PROTOCOL_NULL";
    public static final String HOST_NULL                     = "HOST_NULL";
    public static final String PORT_NULL                     = "PORT_NULL";
    public static final String PORT_RANGE_ERROR              = "PORT_RANGE_ERROR";
    public static final String LOCALE_NULL               	 = "LOCALE_NULL";
    public static final String TERMINATOR_NULL               = "TERMINATOR_NULL";
    public static final String SHELL_PROMPT_NULL             = "SHELL_PROMPT_NULL";
    public static final String SSH_NULL                      = "SSH_NULL";
    public static final String CONN_SCRIPT_NULL              = "CONN_SCRIPT_NULL";
    public static final String USERNAME_NULL                 = "USERNAME_NULL";
    public static final String PASSWORD_NULL                 = "PASSWORD_NULL";
    public static final String TIMEZONE_NULL                 = "TIMEZONE_NULL";
    public static final String DATEFORMAT1_NULL              = "DATEFORMAT1_NULL";
    public static final String DATEFORMAT2_NULL              = "DATEFORMAT2_NULL";
    public static final String TIMEOUT_IN_MATCH              = "TIMEOUT_IN_MATCH";
    public static final String UNSUPPORTED_OBJECT_CLASS		 = "UNSUPPORTED_OBJECT_CLASS";
    public static final String TEST_FAILED					 = "TEST_FAILED";

    private static final String[][] _contents = {
        { INVALID_ATTR_MULTIPLICITY,     "Invalid multiplicity for Attribute ''{0}''" },
        { INVALID_ATTR_VALUE,            "Invalid value ''{0}'' for Attribute ''{1}''" },
        { UNKNOWN_ATTR_NAME,             "Unknown Attribute name ''{0}''" },
        { EXCEPTION_IN_ATTR,             "Exception checking Attribute ''{0}''" },
        { ERROR_IN_CREATE,               "Unexpected error performing Create" },
        { ERROR_IN_MODIFY,               "Unexpected error performing Modify" },
        { ERROR_IN_GETDATE,              "Unexpected error performing GetVmsDate" },
        { ERROR_IN_DELETE,               "Unexpected error performing Delete" },
        { ERROR_IN_SEARCH,               "Unexpected error performing Search" },
        { ERROR_IN_SCRIPT,               "Unexpected error running script" },
        { BAD_SCRIPT_LANGUAGE,           "{0} is not a supported scripting language, only DCO is supported" },
        { ERROR_IN_CREATE2,              "Unexpected error performing Create:{0}" },
        { ERROR_IN_MODIFY2,              "Unexpected error performing Modify:{0}" },
        { ERROR_IN_DELETE2,              "Unexpected error performing Delete:{0}" },
        { PROTOCOL_NULL,                 "protocol may not be null" },
        { PORT_NULL,                     "port may not be null" },
        { HOST_NULL,                     "host name may not be null" },
        { PORT_RANGE_ERROR,              "host port ''{0,number,#########}'' must be between 1 and 65535" },
        { LOCALE_NULL,               	 "VMS locale may not be null" },
        { TERMINATOR_NULL,               "line terminator may not be null" },
        { SHELL_PROMPT_NULL,             "shell prompt may not be null" },
        { SSH_NULL,                      "SSH may not be null" },
        { CONN_SCRIPT_NULL,              "connect script may not be null" },
        { USERNAME_NULL,                 "user name may not be null" },
        { PASSWORD_NULL,                 "password may not be null" },
        { TIMEZONE_NULL,                 "Vms timezone may not be null" },
        { DATEFORMAT1_NULL,              "Vms DateFormat without seconds may not be null" },
        { DATEFORMAT2_NULL,              "Vms DateFormat with seconds may not be null" },
        { TIMEOUT_IN_MATCH,              "timeout waiting for pattern ''{0}''" },
        { UNSUPPORTED_OBJECT_CLASS,		 "Object Class ''{0}'' is not supported" },
        { TEST_FAILED,					 "Test failed" },
    };

    @Override
    protected Object[][] getContents() {
        return _contents;
    }

}
