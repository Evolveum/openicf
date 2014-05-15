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
 * Portions Copyrighted 2014 ForgeRock AS.
 */
package org.identityconnectors.framework.common.objects.filter;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Determines whether an {@link ConnectorObject object} contains an
 * {@link Attribute attribute} that matches a specific attribute value.
 */
public final class EqualsFilter extends AttributeFilter {

    /**
     * Public only as an artifact of the implementation. Please use
     * {@link FilterBuilder#equalTo(Attribute) FilterBuilder} to create an
     * instance of {@code EqualsFilter}.
     */
    public EqualsFilter(Attribute attr) {
        super(attr);
    }

    /**
     * Determines whether the specified {@link ConnectorObject} contains an
     * attribute that has the same name and contains a value that is equals the
     * value of the attribute that {@code FilterBuilder} placed into this
     * filter.
     * <p>
     * Note that in the case of a multi-valued attribute, equality of values
     * means that:
     * <ul>
     * <li>the value of the attribute in the connector object and the value of
     * the attribute in the filter must contain
     * <em>the same number of elements</em>; and that</li>
     * <li>each element within the value of the attribute in the connector
     * object must <em>equal the element that occupies the same position</em>
     * within the value of the attribute in the filter.</li>
     * </ul>
     *
     * @see Filter#accept(ConnectorObject)
     */
    @Override
    public boolean accept(ConnectorObject obj) {
        boolean ret = false;
        Attribute thisAttr = getAttribute();
        Attribute attr = obj.getAttributeByName(thisAttr.getName());
        if (attr != null) {
            ret = thisAttr.equals(attr);
        }
        return ret;
    }

    public <R, P> R accept(FilterVisitor<R, P> v, P p) {
        return v.visitEqualsFilter(p, this);
    }

    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("EQUALS: ").append(getAttribute());
        return bld.toString();
    }
}
