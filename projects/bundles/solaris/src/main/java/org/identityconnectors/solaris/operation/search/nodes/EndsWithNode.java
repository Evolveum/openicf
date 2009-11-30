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
package org.identityconnectors.solaris.operation.search.nodes;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;



/**
 * Single value filter
 * @author David Adam
 *
 */
public class EndsWithNode extends AttributeNode {

    private String value;

    public String getValue() {
        return value;
    }
    
    public EndsWithNode(NativeAttribute nativeAttr, boolean isNot, String value) {
        super(nativeAttr, isNot);
        this.value = value;
    }

    @Override
    public boolean evaluate(SolarisEntry entry) {
        String filterAttrName = getAttributeName().getName();
        for (Attribute attr : entry.getAttributeSet()) {
            if (attr.getName().equals(filterAttrName)) {
                String stringValue = AttributeUtil.getStringValue(attr);
                if (stringValue.endsWith(getValue())) {
                    return true ^ isNot();
                }
            }
        }
        return false ^ isNot();
    }

}
