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

package org.identityconnectors.solaris.attr;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.framework.common.objects.Name;

public enum GroupAttribute implements ConnectorAttribute {
    GROUPNAME(Name.NAME, NativeAttribute.G_NAME), 
    GID("gid", NativeAttribute.GID), 
    USERS("users", NativeAttribute.G_USERS);
    
    private String n;
    private NativeAttribute nattr;
    private static final Map<NativeAttribute, GroupAttribute> nativeToGroup = new HashMap<NativeAttribute, GroupAttribute>();
    static {
        for (GroupAttribute accAttr : values()) {
            nativeToGroup.put(accAttr.getNative(), accAttr);
        }
    }

    public static GroupAttribute fromNative(NativeAttribute nativeAttr) {
        return nativeToGroup.get(nativeAttr);
    }

    private GroupAttribute(String name, NativeAttribute nativeAttr) {
        n = name;
        nattr = nativeAttr;
    }
    
    public String getName() {
        return n;
    }

    public NativeAttribute getNative() {
        return nattr;
    }

}
