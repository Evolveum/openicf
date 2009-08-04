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
package org.identityconnectors.solaris.constants;

import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.solaris.operation.search.PatternBuilder;


/**
 * List of allowed ACCOUNT attribute names.
 * also take a look at {@link AccountAttributesForPassword}.
 * @author David Adam
 */
public enum AccountAttributes implements SolarisAttribute {    
    /** home directory */
    DIR("dir", UpdateSwitches.DIR, null, null /* TODO */), 
    SHELL("shell", UpdateSwitches.SHELL, null, null /* TODO */),
    /** primary group */
    GROUP("group", UpdateSwitches.GROUP, null, null /* TODO */),
    SECONDARY_GROUP("secondary_group", UpdateSwitches.SECONDARY_GROUP, null, null /* TODO */),
    UID("uid", UpdateSwitches.UID, null, null /* TODO */),
    NAME(Name.NAME, UpdateSwitches.UNKNOWN, null, null /* TODO */),
    EXPIRE("expire", UpdateSwitches.EXPIRE, null, null /* TODO */),
    INACTIVE("inactive", UpdateSwitches.INACTIVE, "logins -oxma -l __username__", PatternBuilder.buildPattern(14, 1/*uid*/, 13/*inactive col.*/)), 
    COMMENT("comment", UpdateSwitches.COMMENT, null, null /* TODO */),
    TIME_LAST_LOGIN("time_last_login", UpdateSwitches.UNKNOWN, null, null /* TODO */),
    AUTHORIZATION("authorization", UpdateSwitches.AUTHORIZATION, null, null /* TODO */),
    PROFILE("profile", UpdateSwitches.PROFILE, null, null /* TODO */),
    ROLES("role", UpdateSwitches.ROLE, null, null /* TODO */);
    
    private static final Map<String, AccountAttributes> map = CollectionUtil.newCaseInsensitiveMap();
    
    static {
        for (AccountAttributes value : AccountAttributes.values()) {
            map.put(value.attrName, value);
        }
    }
    
    /** name of the attribute, it comes from the adapter's schema (prototype xml) */
    private String attrName;

    /** the command line switch used by the solaris connector */
    private UpdateSwitches cmdSwitch;
    /** command that is used to acquire the raw data for the attribute */
    private String command;
    /** regular expression to extract Uid and Attribute from the raw data gathered by {@link GroupAttributes#command} */
    private String regexp;

    
    /**
     * initialize the constants for objectclass __ACCOUNT__'s attributes
     * 
     * @param attrName
     *            the name of attribute (most of the time identical with one
     *            defined in adapter
     * @param cmdSwitch
     *            the command line switch generated for this attribute, when set
     *            in create/update operations
     * @param command
     *            the command that is used in search to get value/uid pairs of
     *            this attribute
     * @param regexp
     *            the regular expression used for parsing the command's output,
     *            to get the respective columns.
     */
    private AccountAttributes(String attrName, UpdateSwitches cmdSwitch, String command, String regexp) {
        this.attrName = attrName;
        this.cmdSwitch = cmdSwitch;
        this.command = command;
        this.regexp = regexp;
    }
    
    /**
     * Translate the Account's attribute name to item from list of allowed
     * account attributes.
     * @return the name of attribute, or null if it doesn't exist.
     */
    public static AccountAttributes fromAttributeName(String s) {
        return map.get(s);
    }
    
    /** 
     * generates the command line switch for the attribute 
     * @param argument the argument used after the switch
     */
    public static String formatCommandSwitch(Attribute argument) {
        return AttributeHelper.formatCommandSwitch(argument, getCmdSwitch(argument));
    }
    
    private static String getCmdSwitch(Attribute argument) {
        return fromAttributeName(argument.getName()).getCmdSwitch();
    }
    
    /** @return the command line switch */
    public String getCmdSwitch() {
        return cmdSwitch.getCmdSwitch();
    }
    
    /** @return the name of the ACCOUNT attribute */
    public String getName() {
        return attrName;
    }
    
    /**
     * {@see SolarisAttribute#getRegExpForUidAndAttribute()}
     */
    public String getRegExpForUidAndAttribute() {
        return regexp;
    }

    /**
     * {@see SolarisAttribute#getCommand(String...)}
     */
    public String getCommand(String... fillInAttributes) {
        return AttributeHelper.fillInCommand(command, fillInAttributes);
    }
}
