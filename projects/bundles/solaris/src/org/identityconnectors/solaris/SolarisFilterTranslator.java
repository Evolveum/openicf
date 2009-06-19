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
package org.identityconnectors.solaris;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

public class SolarisFilterTranslator extends
        AbstractFilterTranslator<String> {

    // private ObjectClass objectClass;
    // private OperationOptions operationOptions;

    // public SolarisFilterTranslator(ObjectClass objectClass,
    // OperationOptions operationOptions) {
    // this.objectClass = objectClass;
    // this.operationOptions = operationOptions;
    // }

    public SolarisFilterTranslator() {
    }

    @Override
    protected String createAndExpression(String leftExpression,
            String rightExpression) {
        // anding of two groups is not supported in basic regular expression
        // defined by POSIX.
        return super.createAndExpression(leftExpression, rightExpression);
    }

    @Override
    protected String createOrExpression(String leftExpression,
            String rightExpression) {
        // or-ing works in basic regular expression defined by POSIX:
        // for example: cat /etc/passwd | grep '\(Donald\)\|\(Gray\)'
        // matches string: 'Older Donald Smith Gray went to walk.'
        return String.format("\\(%s\\)\\|\\(%s\\)", leftExpression,
                        rightExpression);
    }

    @Override
    protected String createContainsAllValuesExpression(
            ContainsAllValuesFilter filter, boolean not) {
        return super.createContainsAllValuesExpression(filter, not);
    }

    @Override
    protected String createContainsExpression(ContainsFilter filter,
            boolean not) {
        if (!not && isNamingAttribute(filter.getAttribute())) {
            return String.format("*%s*", filter.getValue());
        }

        return super.createContainsExpression(filter, not);
    }

    @Override
    protected String createEndsWithExpression(EndsWithFilter filter,
            boolean not) {
        if (!not && isNamingAttribute(filter.getAttribute())) {
            return String.format("*%s", filter.getValue());
        }

        return super.createEndsWithExpression(filter, not);
    }

    @Override
    protected String createStartsWithExpression(StartsWithFilter filter,
            boolean not) {
        if (!not && isNamingAttribute(filter.getAttribute())) {
            return String.format("%s*", filter.getValue());
        }

        return super.createStartsWithExpression(filter, not);
    }

    @Override
    protected String createEqualsExpression(EqualsFilter filter,
            boolean not) {
        if (!not && isNamingAttribute(filter.getAttribute())) {
            return (String) filter.getAttribute().getValue().get(0);
        }

        return super.createEqualsExpression(filter, not);
    }

    @Override
    protected String createGreaterThanExpression(
            GreaterThanFilter filter, boolean not) {
        return super.createGreaterThanExpression(filter, not);
    }

    @Override
    protected String createGreaterThanOrEqualExpression(
            GreaterThanOrEqualFilter filter, boolean not) {
        return super.createGreaterThanOrEqualExpression(filter, not);
    }

    @Override
    protected String createLessThanExpression(LessThanFilter filter,
            boolean not) {
        return super.createLessThanExpression(filter, not);
    }

    @Override
    protected String createLessThanOrEqualExpression(
            LessThanOrEqualFilter filter, boolean not) {
        return super.createLessThanOrEqualExpression(filter, not);
    }

    private boolean isNamingAttribute(Attribute attribute) {
        if (attribute.is(Name.NAME)) {
            return true;
        }

        return false;
    }
}
