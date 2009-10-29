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
package org.identityconnectors.solaris.operation.nis;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public class AbstractNISOp {
    // Temporary file names
    final static  String tmpPwdfile1 = "/tmp/wspasswd.$$";
    final static String tmpPwdfile2 = "/tmp/wspasswd_work.$$";
    final static String tmpPwdfile3 = "/tmp/wspasswd_out.$$";
    
    final static String pwdMutexFile = "/tmp/WSpwdlock";
    final static String tmpPwdMutexFile = "/tmp/WSpwdlock.$$";
    final static String pwdPidFile = "/tmp/WSpwdpid.$$";
    
    // This is a major string to look for if you want to do rejects on shadow file errors
    final static String ERROR_MODIFYING = "Error modifying ";
    
    final static Set<NativeAttribute> allowedNISattributes;
    static {
        allowedNISattributes = new HashSet<NativeAttribute>();
        allowedNISattributes.add(NativeAttribute.ID);
        allowedNISattributes.add(NativeAttribute.GROUP_PRIM);
        allowedNISattributes.add(NativeAttribute.DIR);
        allowedNISattributes.add(NativeAttribute.COMMENT);
        allowedNISattributes.add(NativeAttribute.SHELL);
    }
    
    protected final static GuardedString getPassword(SolarisEntry entry) {
        GuardedString password = null;
        for (Attribute passAttr : entry.getAttributeSet()) {
            if (passAttr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
                password = (GuardedString) passAttr.getValue().get(0);
                break;
            }
        }
        return password;
    }
}
