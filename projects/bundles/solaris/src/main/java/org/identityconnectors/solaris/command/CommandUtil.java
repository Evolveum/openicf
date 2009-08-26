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
package org.identityconnectors.solaris.command;

import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.solaris.constants.AccountAttributes;
import org.identityconnectors.solaris.constants.AccountAttributesForPassword;
import org.identityconnectors.solaris.constants.GroupAttributes;

/**
 * contains utility methods for forming commands for Unix.
 * @author David Adam
 */
public class CommandUtil {

    /** Maximum number of characters per line in Solaris shells */
    final static int DEFAULT_LIMIT = 120;
    
    private static StringBuilder limitString(StringBuilder data, int limit) {
        StringBuilder result = new StringBuilder(limit);
        if (data.length() > limit) {
            result.append(data.substring(0, limit));
            result.append("\\\n"); // /<newline> separator of Unix command line cmds.
            
            final String remainder = data.substring(limit, data.length()); 
            if (remainder.length() > limit) {
                // TODO performance: might be a more effective way to handle this copying. Maybe skip copying and pass the respective part of stringbuffer directly to the recursive call.
                StringBuilder sbtmp = new StringBuilder();
                sbtmp.append(remainder);
                result.append(limitString(sbtmp, limit));
            } else {
                result.append(remainder);
            }
        } else {
            return data;
        }
        return result;
    }

    /**
     * Cut the command into pieces, so it doesn't have a longer line than the given DEFAULT_LIMIT
     * @param data
     * @return
     */
    static String limitString(StringBuilder data) {
        return limitString(data, DEFAULT_LIMIT /* == max length of line from SolarisResourceAdapter#getUpdateNativeUserScript(), line userattribparams */).toString();
    }
    
    /** 
     * use the attributes to generate the argument of a Solaris command.
     * 
     * @param attributes Attributes, whose *value* and *name* is used.
     * @param oclass the objectclass, whose attributes are scanned for matching.
     */
    public static String prepareCommand(Set<Attribute> attributes, ObjectClass oclass) {
        StringBuilder command = new StringBuilder();

        for (Attribute attr : attributes) {
            // skip special attributes such as __PASSWORD__ or __NAME__
            if (AttributeUtil.isSpecial(attr))
                continue;
            
            try {
                if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
                    String toAppend = null;
                    toAppend = AccountAttributes.formatCommandSwitch(attr);
                    if (toAppend == null) {
                        toAppend = AccountAttributesForPassword.formatCommandSwitch(attr);
                    }
                    
                    command.append(toAppend);
                    
                } else if (oclass.is(ObjectClass.GROUP_NAME)) {
                    command.append(GroupAttributes.formatCommandSwitch(attr));
                } else throw new IllegalArgumentException("unknown objectClass: '" + oclass.getDisplayNameKey() + "'");
            } catch (Exception ex) {
                // OK ignoring attribute
            } // try
            command.append(" ");
        }// for

        return command.toString();
    }
}