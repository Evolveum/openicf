/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openicf.connectors.sap;

import java.util.HashMap;
import java.util.Map;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete
 * representation of which filters can be applied at the connector level
 * (natively). If the SAP doesn't support a certain expression type, that
 * factory method should return null. This level of filtering is present only to
 * allow any native constructs that may be available to help reduce the result
 * set for the framework, which will (strictly) reapply all filters specified
 * after the connector does the initial filtering.<p><p>Note: The generic query
 * type is most commonly a String, but does not have to be.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 * @version $Revision$ $Date$
 */
public class SAPFilterTranslator extends AbstractFilterTranslator<Map> {

    @Override
    protected Map createContainsExpression(ContainsFilter filter, boolean not) {
        return createMap("CONTAINS", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createEndsWithExpression(EndsWithFilter filter, boolean not) {
        return createMap("ENDSWITH", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createStartsWithExpression(StartsWithFilter filter, boolean not) {
        return createMap("STARTSWITH", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createEqualsExpression(EqualsFilter filter, boolean not) {
        return createMap("EQUALS", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createAndExpression(Map leftExpression, Map rightExpression) {
        Map map = new HashMap();
        map.put("operation", "AND");
        map.put("left", leftExpression);
        map.put("right", rightExpression);
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createOrExpression(Map leftExpression, Map rightExpression) {
        Map map = new HashMap();
        map.put("operation", "OR");
        map.put("left", leftExpression);
        map.put("right", rightExpression);
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        return createMap("GREATERTHAN", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        return createMap("GREATERTHANOREQUAL", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createLessThanExpression(LessThanFilter filter, boolean not) {
        return createMap("LESSTHAN", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        return createMap("LESSTHANOREQUAL", filter, not);
    }

    /**
     * {@inheritDoc}
     */
    private Map createMap(String operation, AttributeFilter filter, boolean not) {
        Map map = new HashMap();
        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        } else {
            map.put("not", not);
            map.put("operation", operation);
            map.put("left", name);
            map.put("right", value);
            return map;
        }
    }
}
