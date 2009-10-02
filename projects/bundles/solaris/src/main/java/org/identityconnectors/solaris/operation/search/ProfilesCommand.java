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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

class ProfilesCommand {
    public static Attribute getProfilesAttributeFor(String username, SolarisConnection conn) {
        final String out = conn.executeCommand(conn.buildCommand("profiles", username));
        List<String> profiles = null;
        if (!out.endsWith("No such user") && !out.endsWith("No profiles") && !out.endsWith("not found") && out.trim().length() > 0) {
            final String[] lines = out.split("\n");
            profiles = new ArrayList<String>(lines.length);
            for (String line : lines) {
                if (!line.trim().endsWith(":")) { /* this is handling output for OpenSolaris 2008.11, which contains an line:
                                              {$username} :
                                              that is not needed. This line is missing in Solaris 10 or older versions of the OS.*/
                    profiles.add(line.trim());
                }
            }
        } else {
            profiles = Collections.emptyList();
        }
        return AttributeBuilder.build(NativeAttribute.PROFILES.getName(), profiles);
    }
}
