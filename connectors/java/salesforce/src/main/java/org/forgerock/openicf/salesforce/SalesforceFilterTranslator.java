/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openicf.salesforce;

import org.forgerock.openicf.salesforce.meta.MetaAttribute;
import org.forgerock.openicf.salesforce.meta.MetaResource;
import org.forgerock.openicf.salesforce.query.IQuery;
import org.forgerock.openicf.salesforce.query.QueryImpl;
import org.forgerock.openicf.salesforce.query.QueryPartImpl;
import org.forgerock.openicf.salesforce.resources.ResourceRegistry;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.StringUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete representation
 * of which filters can be applied at the connector level (natively). If the
 * Salesforce doesn't support a certain expression type, that factory
 * method should return null. This level of filtering is present only to allow any
 * native constructs that may be available to help reduce the result set for the framework,
 * which will (strictly) reapply all filters specified after the connector does the initial
 * filtering.<p><p>Note: The generic query type is most commonly a String, but does not have to be.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class SalesforceFilterTranslator extends AbstractFilterTranslator<IQuery> {


    private MetaResource resource;
    private ResourceRegistry registry;

    public SalesforceFilterTranslator(MetaResource resource, ResourceRegistry registry) {
        this.resource = resource;
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createContainsExpression(ContainsFilter filter, boolean not) {
        Object obj = getValue(filter.getAttribute());
        String value = "'%" + obj.toString() + "%'";
        return createQuery(filter.getAttribute(), not ? "NOT LIKE" : "LIKE", value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createEndsWithExpression(EndsWithFilter filter, boolean not) {
        Object obj = getValue(filter.getAttribute());
        String value = "'%" + obj.toString() + "'";
        return createQuery(filter.getAttribute(), not ? "NOT LIKE" : "LIKE", value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createStartsWithExpression(StartsWithFilter filter, boolean not) {
        Object obj = getValue(filter.getAttribute());
        String value = "'" + obj.toString() + "%'";
        return createQuery(filter.getAttribute(), not ? "NOT LIKE" : "LIKE", value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createEqualsExpression(EqualsFilter filter, boolean not) {
        Attribute attribute = filter.getAttribute();
        if (attribute instanceof Uid) {
            try {
                Integer.parseInt(AttributeUtil.getStringValue(attribute));
            } catch (NumberFormatException ex) {
                return createQuery(new Name(AttributeUtil.getStringValue(attribute)), not ? "<>" : "=");
            }
        }
        return createQuery(attribute, not ? "<>" : "=");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createAndExpression(IQuery leftExpression, IQuery rightExpression) {
        leftExpression.and(rightExpression);
        return leftExpression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createOrExpression(IQuery leftExpression, IQuery rightExpression) {
        leftExpression.or(rightExpression);
        return leftExpression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        return createQuery(filter.getAttribute(), not ? "<" : ">");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        return createQuery(filter.getAttribute(), not ? "<=" : ">=");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createLessThanExpression(LessThanFilter filter, boolean not) {
        return createQuery(filter.getAttribute(), not ? ">" : "<");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IQuery createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        return createQuery(filter.getAttribute(), not ? ">=" : "<=");
    }

    private IQuery createQuery(Attribute attribute, String operator) {
        Object actualValue = getValue(attribute);
        String formattedValue = formatValue(actualValue);
        return createQuery(attribute, operator, formattedValue);
    }

    private IQuery createQuery(Attribute attribute, String operator, String value) {
        MetaAttribute metaAttr = resource.find(attribute.getName());
        if (metaAttr.isQueriable()) {
            IQuery query = new QueryImpl(resource.getICResource());
            query.set(new QueryPartImpl(attribute.getName(), operator, value));
            return query;
        }

        return null;
    }

    /**
     * @param attribute
     * @return
     */
    private Object getValue(Attribute attribute) {
        MetaAttribute meta = resource.find(attribute.getName());

        // Verify meta data
        if (meta == null) {
            String msg = String.format("No such attribute '%s' in the resource '%s'", attribute.getName(), resource.getName());
            throw new IllegalArgumentException(msg);
        }

        AttributeInfo info = registry.getAttributeInfo(meta);

        try {
            if (info.isMultiValued()) {
                return attribute.getValue();
            } else if (Integer.class.equals(info.getType()) || int.class.equals(info.getType())) {
                return AttributeUtil.getIntegerValue(attribute);
            } else if (String.class.equals(info.getType()) || Character.class.equals(info.getType()) || char.class.equals(info.getType())) {
                return AttributeUtil.getStringValue(attribute);
            } else if (Long.class.equals(info.getType()) || long.class.equals(info.getType())) {
                return AttributeUtil.getLongValue(attribute);
            } else if (BigDecimal.class.equals(info.getType())) {
                return AttributeUtil.getBigDecimalValue(attribute);
            } else if (Boolean.class.equals(info.getType()) || boolean.class.equals(info.getType())) {
                return AttributeUtil.getBooleanValue(attribute);
            } else if (Double.class.equals(info.getType()) || double.class.equals(info.getType())) {
                return AttributeUtil.getDoubleValue(attribute);
            } else {
                return AttributeUtil.getAsStringValue(attribute);
            }
        } catch (ClassCastException ex) {
            return AttributeUtil.getAsStringValue(attribute);
        }
    }

    private String formatValue(Object obj) {
        return formatValue(obj, false);
    }

    private String formatValue(Object obj, boolean asList) {
        if (obj instanceof List<?>) {
            List<Object> list = (List<Object>) obj;

            if (list.size() == 1 && !asList) {
                return formatValue(list.get(0));
            }

            String result = "(";
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) result += ", ";
                result += formatValue(list.get(i));
            }
            return result + ")";
        } else if (obj instanceof String) {
            return "'" + (String) obj + "'";
        } else {
            return obj.toString();
        }
    }
}
