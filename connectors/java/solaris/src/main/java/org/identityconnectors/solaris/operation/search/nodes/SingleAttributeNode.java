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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

public abstract class SingleAttributeNode extends AttributeNode {

    /**
     * @param nativeAttribute
     *            the attribute name that is filtered
     * @param isNot
     *            in case it is true this means inverse filter evaluation. (For
     *            instance equals filter becomes 'not equals').
     */
    public SingleAttributeNode(final NativeAttribute nativeAttribute, final boolean isNot) {
        super(nativeAttribute, isNot);
    }

    @Override
    public boolean evaluate(SolarisEntry entry) {
        NativeAttribute filterAttrName = getAttributeName();
        Attribute result = entry.searchForAttribute(filterAttrName);
        if (result == null) {
            return isNot();
        }

        String singleValue = (String) AttributeUtil.getSingleValue(result);

        return (singleValue != null && evaluateImpl(singleValue));
    }

    protected abstract boolean evaluateImpl(String singleValue);
}
