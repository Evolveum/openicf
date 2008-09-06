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

import java.util.ListResourceBundle;

public class RacfMessages extends ListResourceBundle {
    public static final String SUFFIX_NULL                    = "SUFFIX_NULL";
    public static final String HOST_NULL                      = "HOST_NULL";
    public static final String PORT_NULL                      = "PORT_NULL";
    public static final String USERNAME_NULL                  = "USERNAME_NULL";
    public static final String PASSWORD_NULL                  = "PASSWORD_NULL";
    public static final String USERNAMES_NULL                 = "USERNAMES_NULL";
    public static final String PASSWORDS_NULL                 = "PASSWORDS_NULL";
    public static final String POOLNAMES_NULL                 = "POOLNAMES_NULL";
    public static final String INTERVAL_NULL                  = "INTERVAL_NULL";
    public static final String PASSWORDS_LENGTH               = "PASSWORDS_LENGTH";
    public static final String UNINITIALIZED_CONNECTOR        = "UNINITIALIZED_CONNECTOR";
    public static final String MUST_BE_SINGLE_VALUE           = "MUST_BE_SINGLE_VALUE";
    public static final String UNKNOWN_ATTRIBUTE              = "UNKNOWN_ATTRIBUTE";
    public static final String PATTERN_FAILED                 = "PATTERN_FAILED";
    public static final String UNSUPPORTED_SCRIPTING_LANGUAGE = "UNSUPPORTED_SCRIPTING_LANGUAGE";
    public static final String NEED_LDAP                      = "NEED_LDAP";
    public static final String NEED_COMMAND_LINE              = "NEED_COMMAND_LINE";

    private static final String[][] _contents = {
        { SUFFIX_NULL,             "suffix may not be null" },
        { HOST_NULL,               "host name may not be null" },
        { PORT_NULL,               "port may not be null" },
        { USERNAME_NULL,           "user name may not be null" },
        { PASSWORD_NULL,           "password may not be null" },
        { USERNAMES_NULL,          "user names may not be null" },
        { PASSWORDS_NULL,          "passwords may not be null" },
        { POOLNAMES_NULL,          "passwords may not be null" },
        { INTERVAL_NULL,           "interval may not be null" },
        { PASSWORDS_LENGTH,        "passwords, usernames and poolNames must be the same length" },
        { UNINITIALIZED_CONNECTOR, "cannot create a new connection on an LDAP connector that has not been initialized" },
        { MUST_BE_SINGLE_VALUE,    "Must be single values" },
        { UNKNOWN_ATTRIBUTE,       "Unknown Attribute:{0}" },
        { PATTERN_FAILED,          "Pattern failed to match ''{0}''"},
        { UNSUPPORTED_SCRIPTING_LANGUAGE, "''{0}'' is not supported as a scripting langauge" },
        { NEED_LDAP,               "Must have LDAP connection for LDAP-style attributes" },
        { NEED_COMMAND_LINE,       "Must have command-line connection for command-line-style attributes" },
    };

    @Override
    protected Object[][] getContents() {
        return _contents;
    }
}
