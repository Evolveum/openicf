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
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Multi value filter.
 *
 * @author David Adam
 *
 */
public class ContainsAllValuesNode extends MultiAttributeNode {

    private final List<? extends Object> values;

    /**
     * @param nativeAttribute
     *            the attribute name that is filtered
     * @param isNot
     *            in case it is true this means inverse filter evaluation. (For
     *            instance equals filter becomes 'not equals').
     * @param values
     *            filter value that is compared to the actual attribute
     */
    public ContainsAllValuesNode(final NativeAttribute nativeAttribute, final boolean isNot,
            final List<? extends Object> values) {
        super(nativeAttribute, isNot);
        this.values = values;
    }

    @Override
    protected boolean evaluateImpl(List<? extends Object> multiValue) {
        Set<? extends Object> filterValues = CollectionUtil.newSet(values);
        return (multiValue != null && multiValue.containsAll(filterValues)) ^ isNot();
    }
}
