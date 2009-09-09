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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;

class AuthsCmd implements Command {
    /**
     * @param name username
     * @param op operation that called the command
     * @return the auths attribute for given user 
     */
    public static Attribute getAuthsAttributeFor(String username, SolarisConnection conn, CommandBuilder bldr) {
        final String out = conn.executeCommand(bldr.build("auths", username));
        List<String> auths = null;
        if (!out.endsWith("No such user") && !out.contains("No auth") && !out.endsWith("not found")) {
            auths = Arrays.asList(out.split(","));
        } else if (out.contains("No auth")) {
            auths = Collections.emptyList();
        } else {
            throw new RuntimeException("'auths' command for user '" + username + "' failed. Buffer: <" + out + ">");
        }
        return AttributeBuilder.build(NativeAttribute.AUTHS.getName(), auths);
    }
}
