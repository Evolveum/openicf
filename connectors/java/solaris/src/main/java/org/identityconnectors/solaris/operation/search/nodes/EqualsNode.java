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

import org.identityconnectors.solaris.attr.NativeAttribute;



/**
 * Single value filter.
 * @author David Adam
 *
 */
public class EqualsNode extends MultiAttributeNode {

    private List<? extends Object> values;

    /** user this getter, if based on the context you are sure, that only a single value makes sense. */
    public String getSingleValue() {
        return (values != null && values.size() > 0) ? (String) values.get(0) : null;
    }
    
    public EqualsNode(NativeAttribute nativeAttr, boolean isNot, List<? extends Object> values) {
        super(nativeAttr, isNot);
        this.values = values;
    }

    @Override
    protected boolean evaluateImpl(List<? extends Object> multiValue) {
        return multiValue.equals(values) ^ isNot();
    }

}
