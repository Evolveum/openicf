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
package org.identityconnectors.solaris.operation;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * Switches for {@link CreateNativeUser} and {@link UpdateNativeUser}.
 *
 * @author David Adam
 *
 */
public class CommandSwitches {

    // create and update operation switches (identical for both operations)
    static final Map<NativeAttribute, String> COMMON_SWITCHES;

    // set of parameters that allow to pass null values (to erase existing
    // values),
    // such as an empty string.
    // @formatter:off
    private static final Set<NativeAttribute> PASS_NULL_PARAMS = EnumSet.of(
            NativeAttribute.GROUPS_SEC,
            NativeAttribute.COMMENT,
            NativeAttribute.USER_EXPIRE,
            NativeAttribute.AUTHS,
            NativeAttribute.PROFILES,
            NativeAttribute.ROLES);
    // @formatter:on

    static {
        Map<NativeAttribute, String> switchMap =
                new EnumMap<NativeAttribute, String>(NativeAttribute.class);
        // values based on SVIDResourceAdapter's paramToFlagMap
        switchMap.put(NativeAttribute.ID, "-u");
        switchMap.put(NativeAttribute.GROUP_PRIM, "-g");
        switchMap.put(NativeAttribute.GROUPS_SEC, "-G");
        switchMap.put(NativeAttribute.DIR, "-d");
        switchMap.put(NativeAttribute.SHELL, "-s");
        switchMap.put(NativeAttribute.COMMENT, "-c");
        switchMap.put(NativeAttribute.USER_INACTIVE, "-f");
        switchMap.put(NativeAttribute.USER_EXPIRE, "-e");
        switchMap.put(NativeAttribute.AUTHS, "-A");
        switchMap.put(NativeAttribute.PROFILES, "-P");
        switchMap.put(NativeAttribute.ROLES, "-R");
        // switchMap.put(NativeAttribute.PASSWD, null); // password doesn't have
        // ANY switch.
        COMMON_SWITCHES = CollectionUtil.asReadOnlyMap(switchMap);

        // TODO what is the value for these attributes?
        // _CU_switches.put(NativeAttribute.NAME, null); // Create only
        // _CU_switches.put(NativeAttribute.LAST_LOGIN, null);
        // _CU_switches.put(NativeAttribute.USERS, null);
    }

    /**
     * creates command line switches construction.
     *
     * <b>Contract:</b>
     * <p>
     * {@link NativeAttribute#LOCK} and {@link NativeAttribute#PWSTAT} are
     * command-line switches without any argument.
     *
     * @param entry
     *            the account that is source of values for the switches
     * @param conn
     * @param switches
     *            line switches
     * @return the formatted -switch "value" pairs, separated by space. Returns
     *         a zero-length string, in case no switch matched the attributes in
     *         the given entry.
     */
    public static String formatCommandSwitches(SolarisEntry entry, SolarisConnection conn,
            Map<NativeAttribute, String> switches) {
        StringBuilder buffer = new StringBuilder();

        for (Attribute attr : entry.getAttributeSet()) {
            NativeAttribute nAttrName = NativeAttribute.forAttributeName(attr.getName());
            // assuming Single values only
            List<Object> values = attr.getValue();

            if (nAttrName.isSingleValue()) {
                if (values == null) {
                    // TODO: does this make sense? the attributes may be
                    // optional ...
                    // workaround for contract tests
                    // (UpdateApitOpTests#testUpdateToNull()):
                    // because Unix cannot accept null arguments in update, we
                    // need
                    // to throw an exception to satisfy the contract.
                    throw new ConnectorException(String.format(
                            "Attribute '%s' has a null value, expecting singleValue", attr
                                    .getName()));
                }
                // this provides validation for single value attributes.
                Object value = AttributeUtil.getSingleValue(attr);

                // if the value is null, it means that there's an attempt to
                // clear or remove the attribute on the resource.
                // Some command line switches allow to pass empty argument,
                // these are in Set CommandSwitches#PASS_NULL_PARAMS.
                if (value == null || StringUtil.isBlank(value.toString()) || values.isEmpty()) {
                    if (nAttrName == NativeAttribute.USER_INACTIVE) {
                        values = CollectionUtil.newList((Object) "-1");
                    } else if (PASS_NULL_PARAMS.contains(nAttrName)) {
                        values = CollectionUtil.newList((Object) "");
                    }
                }
            } else {
                if (values == null) {
                    values = CollectionUtil.newList();
                }
            }

            // append command line switch
            String cmdSwitchForAttr = switches.get(nAttrName);
            if (cmdSwitchForAttr == null) {
                continue;
            }

            // Special case passwd -f and -l because unlike the other flags it
            // shouldn't have a value
            switch (nAttrName) {
            case LOCK:
                buffer.append(cmdSwitchForAttr).append(" ");
                break;
            case PWSTAT:
                boolean isPasswordForceChange = (Boolean) values.get(0);
                if (isPasswordForceChange) {
                    buffer.append(cmdSwitchForAttr).append(" ");
                } else {
                    throw new ConnectorException(
                            "Solaris allows to set 'force_change' attribute only to 'true' value. Anything else is invalid and will be ignored.");
                }
                break;
            default:
                buffer.append(cmdSwitchForAttr).append(" ");

                buffer.append("\"");
                boolean first = true;
                for (Object itValue : values) {
                    if (!first) {
                        buffer.append(",");
                    }
                    buffer.append(itValue.toString());
                    first = false;
                }
                buffer.append("\" ");
                break;
            }
        }
        return buffer.toString().trim();
    }
}
