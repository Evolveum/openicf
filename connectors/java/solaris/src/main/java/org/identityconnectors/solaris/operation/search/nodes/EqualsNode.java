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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
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
 *
 * @author David Adam
 *
 */
public class EqualsNode extends MultiAttributeNode {

    private final List<? extends Object> values;

    /**
     * user this getter, if based on the context you are sure, that only a
     * single value makes sense.
     */
    public String getSingleValue() {
        return (values != null && values.size() > 0) ? (String) values.get(0) : null;
    }

    /**
     * @param nativeAttribute
     *            the attribute name that is filtered
     * @param isNot
     *            in case it is true this means inverse filter evaluation. (For
     *            instance equals filter becomes 'not equals').
     * @param values
     *            filter value that is compared to the actual attribute
     */
    public EqualsNode(final NativeAttribute nativeAttribute, final boolean isNot,
            final List<? extends Object> values) {
        super(nativeAttribute, isNot);
        this.values = values;
    }

    @Override
    protected boolean evaluateImpl(List<? extends Object> multiValue) {
        return multiValue.equals(values) ^ isNot();
    }

}
