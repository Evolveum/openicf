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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;

class AttributeHelper {
    /** 
     * generates the command line switch for the attribute 
     * @param argument the argument used after the switch
     * @param cmdSwitch the command line switch used
     */
    public static String formatCommandSwitch(Attribute argument, String cmdSwitch) {
        return formatParam(
                    cmdSwitch, 
                    AttributeUtil.getStringValue(argument)
               );
    }
    
    private static String formatParam(String key, String value) {
        return String.format("%s \"%s\"", key, value);
    }
    
    /**
     * <code>"__Attribute__"</code> denotes special parts of the command, that will be filled in.
     * @param command
     * @param fillInAttributes
     * @return
     */
    public static String fillInCommand(String command, String... fillInAttributes) {
        if (command == null) {
            return null;
        }
        if (fillInAttributes.length == 0) {
            return command;
        } else {
            // try to fill in the spaces with the given arguments.
            String result = command;
            for (String value : fillInAttributes) {
                result = result.replaceFirst("__[^_]+__", value);
            }
            return result;
        }
    }
}
