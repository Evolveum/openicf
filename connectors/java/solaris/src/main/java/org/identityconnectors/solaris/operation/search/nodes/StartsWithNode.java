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

import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Single value filter.
 *
 * @author David Adam
 *
 */
public class StartsWithNode extends SingleAttributeNode {

    private final String value;

    public String getValue() {
        return value;
    }

    /**
     * @param nativeAttribute
     *            the attribute name that is filtered
     * @param isNot
     *            in case it is true this means inverse filter evaluation. (For
     *            instance equals filter becomes 'not equals').
     * @param value
     *            filter value that is compared to the actual attribute
     */
    public StartsWithNode(final NativeAttribute nativeAttribute, final boolean isNot,
            final String value) {
        super(nativeAttribute, isNot);
        this.value = value;
    }

    @Override
    protected boolean evaluateImpl(String singleValue) {
        return singleValue.startsWith(value) ^ isNot();
    }

}
