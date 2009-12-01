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

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;


/**
 * Multi value filter.
 * @author David Adam
 *
 */
public class ContainsAllValuesNode extends AttributeNode {

    private List<? extends Object> values;
    
    public ContainsAllValuesNode(NativeAttribute nativeAttr, boolean isNot, List<? extends Object> values) {
        super(nativeAttr, isNot);
        this.values = values;
    }

    @Override
    public boolean evaluate(SolarisEntry entry) {
        Set<? extends Object> filterValues = CollectionUtil.newSet(values);
        
        NativeAttribute filterAttrName = getAttributeName();
        Attribute result = entry.searchForAttribute(filterAttrName);
        
        return (result != null && result.getValue() != null && result.getValue().containsAll(filterValues)) ^ isNot();
    }

}
