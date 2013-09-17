/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
package org.forgerock.openicf.connectors.rsaauthenticationmanager;

import com.rsa.admin.data.PrincipalDTO;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

import com.rsa.common.search.Filter;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete representation
 * of which filters can be applied at the connector level (natively).
 *
 * If the RSAAuthenticationManager8 doesn't support a certain expression type, that factory
 * method will return Filter.empty(). This level of filtering is present only to allow any
 * native constructs that may be available to help reduce the result set for the framework,
 * which will (strictly) reapply all filters specified after the connector does the initial
 * filtering.<p><p>Note: The generic query type is most commonly a String, but does not have to be.
 *
 * @author Alex Babeanu (ababeanu@nulli.com)
 * www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1
 * @since 1.0
 */
public class RSAAuthenticationManager8FilterTranslator extends AbstractFilterTranslator<Filter> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createContainsExpression(ContainsFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.contains(name, value));
        } else {
            return Filter.contains(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createEndsWithExpression(EndsWithFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.endsWith(name, value));
        } else {
            return Filter.endsWith(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createStartsWithExpression(StartsWithFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.startsWith(name, value));
        } else {
            return Filter.startsWith(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createEqualsExpression(EqualsFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        
        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.equal(name, value));
        } else {
            return Filter.equal(name, value);
        } 
        /*
        if (StringUtil.isBlank(value)) {
            return null;
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.equal(name, value));
        } else {
            return Filter.equal(name, value);

        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createAndExpression(Filter leftExpression, Filter rightExpression) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        return Filter.and(leftExpression, rightExpression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createOrExpression(Filter leftExpression, Filter rightExpression) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        return Filter.or(leftExpression, rightExpression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.greater(name, value));
        } else {
            return Filter.greater(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.greaterThanEqual(name, value));
        } else {
            return Filter.greaterThanEqual(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createLessThanExpression(LessThanFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.less(name, value));
        } else {
            return Filter.less(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Filter createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        /*
         * Uses RSA Proprietatry Filters.
         */
        String name = filter.getAttribute().getName();
        if ((name.equalsIgnoreCase(Name.NAME) || (name.equalsIgnoreCase(Uid.NAME))))
            name = PrincipalDTO.LOGINUID;
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());

        if (StringUtil.isBlank(value)) {
            return Filter.empty();
        } else if (not) {
            //"not contains" or "doesn't contain"
            return Filter.not(Filter.lessThanEqual(name, value));
        } else {
            return Filter.lessThanEqual(name, value);
        }
    }
}
