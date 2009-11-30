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

import junit.framework.Assert;

import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.junit.Test;

public class ContainsNodeTest {
    @Test
    public void test() {
        // not negated result
        Node swn = new ContainsNode(NativeAttribute.NAME, false, "FooBar");
        boolean result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, "XFooBarBaz").build());
        Assert.assertTrue(result);
        result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, "XFooBaz").build());
        Assert.assertFalse(result);
        
        // negated result
        swn = new ContainsNode(NativeAttribute.NAME, true, "FooBar");
        result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, "XFooBarBaz").build());
        Assert.assertFalse(result);
        result = swn.evaluate(new SolarisEntry.Builder("FooBarBaz").addAttr(NativeAttribute.NAME, "XFooBaz").build());
        Assert.assertTrue(result);
        
    }
}
