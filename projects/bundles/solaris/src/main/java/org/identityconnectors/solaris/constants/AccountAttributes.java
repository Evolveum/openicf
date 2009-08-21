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
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.operation.search.PatternBuilder;
import org.identityconnectors.solaris.operation.search.SearchPerformer.SearchCallback;


/**
 * List of allowed ACCOUNT attribute names.
 * also take a look at {@link AccountAttributesForPassword}.
 * @author David Adam
 */
public enum AccountAttributes implements SolarisAttribute {
    
    /*
     * NOTE:
     * "logins -oxa" 
     * -oxma gives the full set of groups 
     * 
     */
    
    /** home directory */
    DIR("dir", UpdateSwitches.DIR, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 6/*dir col.*/)), 
    SHELL("shell", UpdateSwitches.SHELL, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 7/*shell col.*/)),
    /** primary group */
    GROUP("group", UpdateSwitches.GROUP, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 3/*groupName col.*/)),
    SECONDARY_GROUP("secondary_group", UpdateSwitches.SECONDARY_GROUP, CommandConstants.Logins.CMD_EXTENDED, null, SecondaryGroupParser.Builder.getInstance()),
    /** ! this is the solaris native 'uid', *NOT* the one defined by the framework. */
    UID("uid", UpdateSwitches.UID, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 2/*solaris uid*/)),
    NAME(Name.NAME, UpdateSwitches.UNKNOWN, CommandConstants.Logins.CMD,  PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL)),
    // FIXME: introduce AccountId attribute as in the adapter, maybe?
    /** ! this is the UID defined by the framework */
    FRAMEWORK_UID(Uid.NAME, AccountAttributes.NAME),    
    EXPIRE("expire", UpdateSwitches.EXPIRE, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 14/*expired*/)),
    INACTIVE("inactive", UpdateSwitches.INACTIVE, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 13/*inactive col.*/)), 
    COMMENT("comment", UpdateSwitches.COMMENT, CommandConstants.Logins.CMD, PatternBuilder.buildPattern(CommandConstants.Logins.COL_COUNT, CommandConstants.Logins.UID_COL, 5/*comment col.*/)),
    TIME_LAST_LOGIN("time_last_login", UpdateSwitches.UNKNOWN, "last  -1 __username__", "[\\d]?\\d[\\s]+\\d\\d:\\d\\d" /* parses the date of last login */),
    AUTHORIZATION("authorization", UpdateSwitches.AUTHORIZATION, "auths __username__", null /* TODO */),
    PROFILE("profile", UpdateSwitches.PROFILE, null, null /* TODO */),
    ROLES("role", UpdateSwitches.ROLE, "roles __user__", PatternBuilder.buildAcceptAllPattern() /* TODO */);
    
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
    /** a callback method that is used for special search, that requires to parse multiple attributes.
     * Mostly this attribute is really optional. */
    private SearchCallback callback;

    
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
        this(attrName, cmdSwitch, command, regexp, null);
    }
    
    /** 
     * copy an existing constant, just use a different name. 
     * {@see AccountAttributes#AccountAttributes(String, UpdateSwitches, String, String)}
     * 
     * @param attrName
     * @param attrToClone
     */
    private AccountAttributes(String attrName, AccountAttributes attrToClone) {
        this(attrName, attrToClone.cmdSwitch, attrToClone.command, attrToClone.regexp);
    }
    
    /**
     * {@see AccountAttributes#AccountAttributes(String, UpdateSwitches, String, String)}
     * @param callback an optional attribute that is used for special searches.
     */
    private AccountAttributes(String attrName, UpdateSwitches cmdSwitch, String command, String regexp, SearchCallback callback) {
        this.attrName = attrName;
        this.cmdSwitch = cmdSwitch;
        this.command = command;
        this.regexp = regexp;
        this.callback = callback;
    }
    
    /**
     * Translate the Account's attribute name to item from list of allowed
     * account attributes.
     * @return the name of attribute, or null if it doesn't exist.
     */
    public static AccountAttributes fromAttributeName(String s) {
        return AttributeHelper.getFromMap(map, s, "AccountAttributes");
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
    
    /** holds the commands that are executed to acquire the attribute */
    public static class CommandConstants {
        /**
         * logins command
         */
        public static class Logins {
            /*
             * comparison of 'logins' command's switches: root@prgvm8092:~#
             * #logins -oxa -l root
             * > root:0:root:0:Super-User:/root:/usr/bin/bash:PS:092408:-1:-1:-1:-1:0 
             * # logins -oxma -l root
             * root:0:root:0:Super-User:other:1:bin:2:sys:3:adm:4:uucp:5:mail:6:tty
             * :7:lp:8:nuucp:9:daemon:12:/root:/usr/bin/bash:PS:092408:-1:-1:-1:-1:0
             */
            /** the command that is executed to acquire the attributes. */
            static final String CMD = "logins -oxa";
            /** extended version of logins command */
            static final String CMD_EXTENDED = "logins -oxma";
            /** the total number of columns in output of the command (delimited by ":") */
            static final int COL_COUNT = 14;
            /** the column which contains the native solaris UID */
            static final int UID_COL = 1;
            public static final String DEFAULT_OUTPUT_DELIMITER = ":";
        }
    }
    
    /**
     * {@see SolarisAttribute#getCallbackMethod()}
     */
    public SearchCallback getCallbackMethod() {
        return callback;
    }
}
