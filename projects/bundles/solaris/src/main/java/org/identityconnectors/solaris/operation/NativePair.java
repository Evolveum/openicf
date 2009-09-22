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
package org.identityconnectors.solaris.operation;

import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * @author David Adam
 *
 */
class NativePair {
    private final NativeAttribute nativeAttr;
    private final String value;

    public NativePair(NativeAttribute nativeAttr, String value) {
        this.nativeAttr = nativeAttr;
        this.value = value;
    }
    
    public NativeAttribute getNativeAttr() {
        return nativeAttr;
    }
    
    /** the value could be null, indicating no value at all. */
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("NativePair(attr='%s', value='%s'", nativeAttr.getName(), value);
    }
    
    @Override
    public int hashCode() {
        return nativeAttr.getName().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NativePair)) {
            return false;
        }
        
        NativePair other = (NativePair) obj;
        return (other.getNativeAttr().equals(nativeAttr)) ? true : false;
    }
}
