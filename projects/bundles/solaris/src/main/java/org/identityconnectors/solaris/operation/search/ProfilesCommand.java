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
            final String[] tokens = out.split("\n");
            profiles = new ArrayList<String>(tokens.length);
            for (String t : tokens) {
                profiles.add(t.trim());
            }
        } else {
            profiles = Collections.emptyList();
        }
        return AttributeBuilder.build(NativeAttribute.PROFILES.getName(), profiles);
        
        // FIXME: 'profiles' output is: 
        /* root@pc:~# profiles root

        root :
                  All
                  Console User
                  Suspend To RAM
                  Suspend To Disk
                  Brightness
                  CPU Power Management
                  Network Autoconf
                  Network Wifi Info
                  Basic Solaris User
                  
        <end of output>
        the problem is that root : is not part of the profile (maybe?) The adapter let it be in the profiles?...
*/
    }
}
