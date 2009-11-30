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
package org.identityconnectors.solaris.operation.search.nodes;

import java.util.List;

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.junit.Test;

public class ContainsAllValuesNodeTest {
    @Test
    public void test() {
        final List<String> originalList = CollectionUtil.newList("foo", "bar", "baz");
        // not negated result
        Node swn = new ContainsAllValuesNode(NativeAttribute.NAME, false, originalList);
        boolean result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, originalList).build());
        Assert.assertTrue(result);
        result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, CollectionUtil.newList("ahoj", "ship")).build());
        Assert.assertFalse(result);
        
        // negated result
        swn = new ContainsAllValuesNode(NativeAttribute.NAME, true, originalList);
        result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, originalList).build());
        Assert.assertFalse(result);        
        result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, CollectionUtil.newList("ahoj", "ship")).build());
        Assert.assertTrue(result);
        
    }
}
