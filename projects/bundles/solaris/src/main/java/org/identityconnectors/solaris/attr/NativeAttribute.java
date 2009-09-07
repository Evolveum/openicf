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

package org.identityconnectors.solaris.attr;

public enum NativeAttribute {
    /** USER attribute */
    NAME,
    /** USER attribute */
    UID,
    /** USER attribute: primary group */
    GROUP_PRIM,
    /** USER attribute */
    COMMENT, 
    /** USER attribute: secondary groups */
    GROUPS_SEC,
    /** USER attribute */
    DIR,
    /** USER attribute */
    SHELL,
    /** USER attribute */
    PWSTAT,
    /** USER attribute */
    PW_LAST_CHANGE,
    /** USER attribute */
    MIN_DAYS_BETWEEN_CHNG,
    /** USER attribute */
    MAX_DAYS_BETWEEN_CHNG,
    /** USER attribute */
    DAYS_BEFORE_TO_WARN,
    /** USER attribute */
    USER_EXPIRE, 
    /** USER attribute */
    USER_INACTIVE,
    /** USER attribute */
    LOCK,
    
    /** USER attribute */
    ROLES, 
    /** USER attribute */
    AUTHS, 
    /** USER attribute */
    PROFILES,
    /** USER attribute: time when the user was last logged in */
    LAST_LOGIN,
    
    
    
    /** GROUP attribute */
    G_NAME,
    /** GROUP attribute */
    GID,
    /** GROUP attribute */
    G_USERS;
}
