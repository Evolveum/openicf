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

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

public enum AccountAttribute implements ConnectorAttribute {
    DIR("dir", NativeAttribute.DIR), 
    SHELL("shell", NativeAttribute.SHELL),
    /** primary group */
    GROUP("group", NativeAttribute.GROUP_PRIM),
    SECONDARY_GROUP("secondary_group", NativeAttribute.GROUPS_SEC),
    
    /* Native attribute for NAME and FRAMEWORK_UID should be identical. */
    NAME(Name.NAME, NativeAttribute.NAME),
    FRAMEWORK_UID(Uid.NAME, NativeAttribute.NAME),
    /* This is the solaris native 'uid', *NOT* the one defined by the framework. */
    UID("uid", NativeAttribute.NAME),
    
    EXPIRE("expire", NativeAttribute.USER_EXPIRE),
    INACTIVE("inactive", NativeAttribute.USER_INACTIVE), 
    COMMENT("comment", NativeAttribute.COMMENT),
    TIME_LAST_LOGIN("time_last_login", NativeAttribute.LAST_LOGIN),
    AUTHORIZATION("authorization", NativeAttribute.AUTHS),
    PROFILE("profile", NativeAttribute.PROFILES),
    ROLES("role", NativeAttribute.ROLES),
    
    /*
     * password related attributes
     */
    MAX("max", NativeAttribute.MAX_DAYS_BETWEEN_CHNG),
    MIN("min", NativeAttribute.MIN_DAYS_BETWEEN_CHNG),
    WARN("warn", NativeAttribute.DAYS_BEFORE_TO_WARN),
    LOCK("lock", NativeAttribute.LOCK),
    PASSWD_FORCE_CHANGE("force_change", NativeAttribute.PWSTAT);

    private String n;
    private NativeAttribute nattr;

    private static final Map<NativeAttribute, AccountAttribute> nativeToAccount = new HashMap<NativeAttribute, AccountAttribute>();
    static {
        for (AccountAttribute accAttr : values()) {
            switch (accAttr) {
            /*
             * NAME, UID and FRAMEWORK_UID are mapped to the same native attribute
             */
            case NAME:
            case UID:
            case FRAMEWORK_UID:
                nativeToAccount.put(accAttr.getNative(), NAME);
                break;

            default:
                nativeToAccount.put(accAttr.getNative(), accAttr);
                break;
            }
        }
    }

    public static AccountAttribute fromNative(NativeAttribute nativeAttr) {
        return nativeToAccount.get(nativeAttr);
    }

    private AccountAttribute(String name, NativeAttribute nativeAttr) {
        n = name;
        nattr = nativeAttr;
    }
    
    public String getName() {
        return n;
    }

    public NativeAttribute getNative() {
        return nattr;
    }
}
