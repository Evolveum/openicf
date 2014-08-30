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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */

package org.identityconnectors.solaris.attr;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;

/**
 * Every {@link AccountAttribute} has been assigned a {@link NativeAttribute}.
 *
 * @author David Adam
 *
 */
public enum AccountAttribute implements ConnectorAttribute {
    // @formatter:off
    DIR("dir", NativeAttribute.DIR),
    SHELL("shell", NativeAttribute.SHELL),
    /** primary group. */
    GROUP(PredefinedAttributes.GROUPS_NAME, NativeAttribute.GROUP_PRIM),
    SECONDARY_GROUP("secondary_group", NativeAttribute.GROUPS_SEC),

    /**
     * Both {@link org.identityconnectors.framework.common.objects.Uid} and
     * {@link @Name} are mapped to {@link NativeAttribute#NAME}.
     *
     * Provided this they must be always identical.
     */
    NAME(Name.NAME, NativeAttribute.NAME),
    /**
     * This is the solaris native 'uid', *NOT* the one defined by the framework
     * ({@link org.identityconnectors.framework.common.objects.Uid}, that should
     * be <b>immutable</b>, whereas Solaris native uid is <b>mutable</b>).
     */
    UID("uid", NativeAttribute.ID),

    /**
     * The date on which the user account will be disabled.
     */
    EXPIRE("expire", NativeAttribute.USER_EXPIRE),
    /**
     * The number of days after a password expires until the account is permanently disabled.
     */
    INACTIVE("inactive", NativeAttribute.USER_INACTIVE),
    COMMENT(PredefinedAttributes.DESCRIPTION, NativeAttribute.COMMENT),
    TIME_LAST_LOGIN(PredefinedAttributes.LAST_LOGIN_DATE_NAME, NativeAttribute.LAST_LOGIN),
    AUTHORIZATION("authorization", NativeAttribute.AUTHS),
    PROFILE("profile", NativeAttribute.PROFILES),
    ROLES("role", NativeAttribute.ROLES),

    /*
     * password related attributes.
     */
    MAX("max", NativeAttribute.MAX_DAYS_BETWEEN_CHNG),
    MIN("min", NativeAttribute.MIN_DAYS_BETWEEN_CHNG),
    WARN("warn", NativeAttribute.DAYS_BEFORE_TO_WARN),
    
    /**
     * Lock-out, means that the account was disabled due to too many failed login attempts.
     * Enable/disable is simulated using expire date.
     */
    LOCK(OperationalAttributes.LOCK_OUT_NAME, NativeAttribute.LOCK),
    
    PASSWD_FORCE_CHANGE("force_change", NativeAttribute.PWSTAT),
    // @formatter:on
    
    REGISTRY("registry", NativeAttribute.REGISTRY),
    SYSTEM("SYSTEM", NativeAttribute.SYSTEM);

    private String name;
    private NativeAttribute nativeAttr;

    private static final Map<String, AccountAttribute> STRING_TO_ACCOUNT =
            new HashMap<String, AccountAttribute>();
    static {
        for (AccountAttribute accAttr : values()) {
            STRING_TO_ACCOUNT.put(accAttr.getName(), accAttr);
        }
    }

    public static ConnectorAttribute forAttributeName(String accountAttr) {
        return STRING_TO_ACCOUNT.get(accountAttr);
    }

    private AccountAttribute(String name, NativeAttribute nativeAttr) {
        this.name = name;
        this.nativeAttr = nativeAttr;
    }

    public String getName() {
        return name;
    }

    public NativeAttribute getNative() {
        return nativeAttr;
    }
}
