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
package org.identityconnectors.spml;

public interface SpmlMessages {
    public static final String PROTOCOL_NULL                 = "PROTOCOL_NULL";
    public static final String LANGUAGE_NULL                 = "LANGUAGE_NULL";
    public static final String NAME_NULL                     = "NAME_NULL";
    public static final String URL_NULL                      = "URL_NULL";
    public static final String USERNAME_NULL                 = "USERNAME_NULL";
    public static final String PASSWORD_NULL                 = "PASSWORD_NULL";
    public static final String OBJECT_CLASS_NULL             = "OBJECT_CLASS_NULL";
    public static final String SPML_CLASS_NULL               = "SPML_CLASS_NULL";
    public static final String TARGET_NULL                   = "TARGET_NULL";
    public static final String SPML_CLASS_LENGTH             = "SPML_CLASS_LENGTH";
    public static final String CLASSMAP_NULL                 = "CLASSMAP_NULL";
    public static final String UNSUPPORTED_OBJECTCLASS       = "UNSUPPORTED_OBJECTCLASS";
    public static final String ITERATION_CANCELED            = "ITERATION_CANCELED";
    public static final String ILLEGAL_MODIFICATION          = "ILLEGAL_MODIFICATION";
    public static final String POSTCONNECT_SCRIPT_ERROR		 = "POSTCONNECT_SCRIPT_ERROR";
    public static final String PRESEND_SCRIPT_ERROR		 	 = "PRESEND_SCRIPT_ERROR";
    public static final String POSTRECEIVE_SCRIPT_ERROR 	 = "POSTRECEIVE_SCRIPT_ERROR";
    public static final String PREDISCONNECT_SCRIPT_ERROR 	 = "PREDISCONNECT_SCRIPT_ERROR";
    public static final String MAPSETNAME_SCRIPT_ERROR 		 = "MAPSETNAME_SCRIPT_ERROR";
    public static final String MAPSCHEMA_SCRIPT_ERROR 		 = "MAPSCHEMA_SCRIPT_ERROR";
    public static final String MAPATTRIBUTE_SCRIPT_ERROR	 = "MAPATTRIBUTE_SCRIPT_ERROR";
    public static final String MAPQUERYNAME_SCRIPT_ERROR	 = "MAPQUERYNAME_SCRIPT_ERROR";    
}
