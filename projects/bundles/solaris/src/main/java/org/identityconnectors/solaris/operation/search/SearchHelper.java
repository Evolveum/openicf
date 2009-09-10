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

import java.util.Set;

import org.identityconnectors.solaris.attr.NativeAttribute;

class SearchHelper {
    /**
     * evaluate if logins command is necessary to launch.
     * 
     * @param attrsToGet
     *            attributes to get
     * @return true if {@link LoginsCmd} is required to be called, as it
     *         processes one of the attributes to get.
     */
    public static boolean isLoginsRequired(Set<NativeAttribute> attrsToGet) {
        for (NativeAttribute nativeAttribute : attrsToGet) {
            if (LoginsCmd.isProvided(nativeAttribute)) {
                return true;
            }
        }
        return false;
    }
}
