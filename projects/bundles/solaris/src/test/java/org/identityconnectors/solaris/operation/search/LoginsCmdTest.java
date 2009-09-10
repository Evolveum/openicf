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

import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.Pair;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.junit.Test;

public class LoginsCmdTest {

    @Test
    public void test() {
        Pair<SolarisConnection, CommandBuilder> pair = SolarisTestCommon.getSolarisConn();
        SolarisEntry result = LoginsCmd.getAttributesFor("root", pair.first, pair.second);
        Assert.assertTrue(result.getAttributeSet().size() >= 5);
        Set<Attribute> attrSet = result.getAttributeSet();
        for (Attribute attribute : attrSet) {
            if (attribute.getName().equals(NativeAttribute.NAME)) {
                Assert.assertNotNull(attribute.getValue());
                Assert.assertTrue(attribute.getValue().size() == 1);
                Assert.assertEquals("root", attribute.getValue().get(0));
                break;
            }
        }
    }
    
    @Test
    public void testEnum() {
        Assert.assertTrue(LoginsCmd.isProvided(NativeAttribute.NAME));
        // GROUP NAME attribute is not provided by Logins command.
        Assert.assertFalse(LoginsCmd.isProvided(NativeAttribute.G_NAME));
    }
}
