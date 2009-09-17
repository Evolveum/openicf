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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.nodes.EqualsNode;
import org.identityconnectors.solaris.operation.search.nodes.Node;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David Adam
 * 
 */
public class SolarisFilterTranslatorTest {
    @Test
    public void testBasicEquals() {
        SolarisFilterTranslator sft = new SolarisFilterTranslator(ObjectClass.ACCOUNT);
        
        final String username = "foobar";
        final String connectorAttrName = Name.NAME;
        
        Node result = sft.createEqualsExpression(new EqualsFilter(AttributeBuilder.build(connectorAttrName, username)), false);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof EqualsNode);
        EqualsNode eq = (EqualsNode) result;
        Assert.assertEquals(AccountAttribute.fromString(connectorAttrName).getNative(), eq.getAttributeName());
        Assert.assertEquals(username, eq.getValue());
    }
    
    @Test
    public void testNodeTraverser() {
        SolarisFilterTranslator sft = new SolarisFilterTranslator(ObjectClass.ACCOUNT);
        final String typeOne = Name.NAME;
        Node rightExpression = sft.createEqualsExpression(new EqualsFilter(AttributeBuilder.build(typeOne, "boo")), false);
        final AccountAttribute typeTwo = AccountAttribute.DIR;
        Node leftExpression = sft.createEqualsExpression(new EqualsFilter(AttributeBuilder.build(typeTwo.getName(), "bar")), false);
        
        Node andNode = sft.createAndExpression(leftExpression, rightExpression);
        
        Set<NativeAttribute> result = new HashSet<NativeAttribute>();
        //Node.Traverser.collectAttributeNames(andNode);
        andNode.collectAttributeNames(result);
        Assert.assertEquals(EnumSet.of(AccountAttribute.fromString(typeOne).getNative(), typeTwo.getNative()), result);
    }
}
