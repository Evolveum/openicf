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
package org.identityconnectors.solaris.operation.search;


import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

public class SolarisFilterTranslator extends
        AbstractFilterTranslator<Node> {

    public SolarisFilterTranslator() {
    }

    @Override
    protected Node createOrExpression(Node leftExpression,
            Node rightExpression) {
        return new OrFilter(leftExpression, rightExpression);
    }
    
    @Override
    protected Node createAndExpression(Node leftExpression, Node rightExpression) {
        return new AndFilter(leftExpression, rightExpression);
    } 

    @Override
    protected Node createContainsExpression(ContainsFilter filter, boolean not) {
        /* '.' == zero and more repetitions of any character */
        String regExp = String.format(".*(%s).*", filter.getValue());
        return new AttributeFilter(filter.getName(), regExp, not);
    }

    @Override
    protected Node createEndsWithExpression(EndsWithFilter filter,
            boolean not) {
        String regExp = String.format(".*%s", filter.getValue());
        return new AttributeFilter(filter.getName(), regExp, not);
    }

    @Override
    protected Node createStartsWithExpression(StartsWithFilter filter,
            boolean not) {
        String regExp = String.format("%s.*", filter.getValue());
        return new AttributeFilter(filter.getName(), regExp, not);
    }

    @Override
    protected Node createEqualsExpression(EqualsFilter filter, boolean not) {
        return new AttributeFilter(filter.getName(), (String) filter
                .getAttribute().getValue().get(0), not);
    }
}
