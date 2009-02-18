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
package org.identityconnectors.vms;

public interface VmsMessages {
    public static final String INVALID_ATTR_MULTIPLICITY     = "INVALID_ATTR_MULTIPLICITY";
    public static final String INVALID_ATTR_VALUE            = "INVALID_ATTR_VALUE";
    public static final String UNKNOWN_ATTR_NAME             = "UNKNOWN_ATTR_NAME";
    public static final String EXCEPTION_IN_ATTR             = "EXCEPTION_IN_ATTR";
    public static final String ERROR_IN_CREATE               = "ERROR_IN_CREATE";
    public static final String ERROR_IN_MODIFY               = "ERROR_IN_MODIFY";
    public static final String ERROR_IN_GETDATE              = "ERROR_IN_GETDATE";
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
    public static final String LOCALE_NULL                   = "LOCALE_NULL";
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
    public static final String UNSUPPORTED_OBJECT_CLASS      = "UNSUPPORTED_OBJECT_CLASS";
    public static final String TEST_FAILED                   = "TEST_FAILED";
    public static final String NULL_ATTRIBUTE_VALUE          = "NULL_ATTRIBUTE_VALUE";

}
