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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.ConnectorAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.nodes.AndNode;
import org.identityconnectors.solaris.operation.search.nodes.ContainsAllValuesNode;
import org.identityconnectors.solaris.operation.search.nodes.ContainsNode;
import org.identityconnectors.solaris.operation.search.nodes.EndsWithNode;
import org.identityconnectors.solaris.operation.search.nodes.EqualsNode;
import org.identityconnectors.solaris.operation.search.nodes.Node;
import org.identityconnectors.solaris.operation.search.nodes.OrNode;
import org.identityconnectors.solaris.operation.search.nodes.StartsWithNode;




public class SolarisFilterTranslator extends
        AbstractFilterTranslator<Node> {

    private ObjectClass oclass;

    public SolarisFilterTranslator(ObjectClass oclass) {
        this.oclass = oclass;
    }
    
    /** multivalue attributes filter. */
    @Override
    protected Node createContainsAllValuesExpression(
            ContainsAllValuesFilter filter, boolean not) {
        Attribute attr = filter.getAttribute();
        return new ContainsAllValuesNode(translateFromConnectorAttribute(attr.getName()), not, attr.getValue());
    }

    private NativeAttribute translateFromConnectorAttribute(String connectorAttribute) {
        ConnectorAttribute connAttr = null;
        if (connectorAttribute.equals(Uid.NAME)) {
            connAttr = (oclass.is(ObjectClass.ACCOUNT_NAME)) ? AccountAttribute.NAME : GroupAttribute.GROUPNAME;
        } else if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            connAttr = AccountAttribute.forAttributeName(connectorAttribute);
        } else if (oclass.is(ObjectClass.GROUP_NAME)) {
            connAttr =  GroupAttribute.forAttributeName(connectorAttribute);
        } else {
            throw new IllegalArgumentException("ERROR: unable to find Solaris attribute for attribute name: " + connectorAttribute);
        }
        return connAttr.getNative();
    }

    @Override
    protected Node createOrExpression(Node leftExpression,
            Node rightExpression) {
        return new OrNode(leftExpression, rightExpression);
    }
    
    @Override
    protected Node createAndExpression(Node leftExpression, Node rightExpression) {
        return new AndNode(leftExpression, rightExpression);
    } 

    @Override
    protected Node createContainsExpression(ContainsFilter filter, boolean not) {
        return new ContainsNode(translateFromConnectorAttribute(filter.getName()), not, filter.getValue());
    }

    @Override
    protected Node createEndsWithExpression(EndsWithFilter filter,
            boolean not) {
        return new EndsWithNode(translateFromConnectorAttribute(filter.getName()), not, filter.getValue());
    }

    @Override
    protected Node createStartsWithExpression(StartsWithFilter filter,
            boolean not) {
        return new StartsWithNode(translateFromConnectorAttribute(filter.getName()), not, filter.getValue());
    }

    @Override
    protected Node createEqualsExpression(EqualsFilter filter, boolean not) {
        return new EqualsNode(translateFromConnectorAttribute(filter.getName()), not, filter.getAttribute().getValue());
    }
}
