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

import java.util.HashMap;
import java.util.Map;

public enum NativeAttribute {
    /** USER/GROUP attribute */
    NAME(true),
    /** USER/GROUP attribute */
    ID(true),
    /** USER attribute: primary group */
    GROUP_PRIM(true),
    /** USER attribute */
    COMMENT(true), 
    /** USER attribute: secondary groups */
    GROUPS_SEC(false),
    /** USER attribute */
    DIR(true),
    /** USER attribute */
    SHELL(true),
    /**
     * USER attribute. Other nick for {@link NativeAttribute.PWSTAT} in the
     * adapter for this attribute was PASSWORD_FORCE_CHANGE.
     * 
     * Solaris resource allows only update of this attribute to true. E.g. we
     * can turn on enforcing of changing user's password upon next login. But
     * there is no way to turn this off.
     * 
     * The only way to deactivate force_change is to have the user log in, and 
     * change her password.
     */
    PWSTAT(true),
//    /** USER attribute */ // not used see SVIDRA#
//    PW_LAST_CHANGE,
    /** USER attribute */
    MIN_DAYS_BETWEEN_CHNG(true),
    /** USER attribute */
    MAX_DAYS_BETWEEN_CHNG(true),
    /** USER attribute */
    DAYS_BEFORE_TO_WARN(true),
    /** USER attribute */
    USER_EXPIRE(true), 
    /** USER attribute */
    USER_INACTIVE(true),
    /** USER attribute */
    LOCK(true),
    
    /** USER attribute */
    ROLES(false), 
    /** USER attribute */
    AUTHS(false), 
    /** USER attribute */
    PROFILES(false),
    /** USER attribute: time when the user was last logged in */
    LAST_LOGIN(true),
    
    
    /** GROUP attribute */
    USERS(false);
    
    private boolean isSingleValue;

    private NativeAttribute(boolean isSingleValue) {
        this.isSingleValue = isSingleValue;
    }

    private static final Map<String, NativeAttribute> stringToNative = new HashMap<String, NativeAttribute>();
    static {
        for (NativeAttribute accAttr : values()) {
            stringToNative.put(accAttr.getName(), accAttr);
        }
    }
    
    /**
     * is able to transform string value into NativeAttribute, given, that we
     * respect the name defined by {@see NativeAttribute#getName()}.
     */
    public static NativeAttribute forAttributeName(String nativeAttr) {
        return stringToNative.get(nativeAttr);
    }
    
    /** @return String representation of the native attribute name. */
    public String getName() {
        return this.toString();
    }
    
    /** @return true if the attribute is singleValue, false if it is multivalue */
    public boolean isSingleValue() {
        return isSingleValue;
    }
}