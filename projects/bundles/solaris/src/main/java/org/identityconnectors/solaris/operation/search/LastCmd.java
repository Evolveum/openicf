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

package org.identityconnectors.solaris.operation.search;

import java.util.Collections;
import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;

class LastCmd implements Command {
    public static Attribute getLastAttributeFor(String username, SolarisConnection conn, CommandBuilder bldr) {
        final String out = conn.executeCommand(bldr.build("last -1", username));
        List<String> lastResult = null;
        if (out != null && username != null && !out.contains("wtmp begins")) {
            // we only need the first line of the output
            final String[] tokens = out.split("\n");
            final String last = tokens[0];
            if (last != null && last.length() > 56) {
                String value = last.substring(40, 56);
                lastResult = CollectionUtil.newList(value);
            } else {
                lastResult = Collections.emptyList();
            }
        } else if (out != null && out.contains("wtmp begins")) {
            lastResult = Collections.emptyList();
        } else {
            throw new RuntimeException("'last' command failed for user: '" + username + "', buffer content: <" + out + ">");
        }
        return AttributeBuilder.build(NativeAttribute.LAST_LOGIN.getName(), lastResult);
    }
}
