/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.test.framework.common.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Test;


public class AttributeBuilderTests {

    @Test
    public void nullAttribute() {
        AttributeBuilder bld = new AttributeBuilder();
        bld.setName("adfaljk");
        assertNull(bld.build().getValue());
        assertNull(AttributeBuilder.build("fadsfd").getValue());
    }

    @Test
    public void buildBoolean() {
        AttributeBuilder bld = new AttributeBuilder();
        bld.setName("somename");
        bld.addValue(true, false);
        Attribute attr1 = bld.build();
        Attribute attr2 = bld.build();
        bld.addValue(false);
        Attribute attr3 = bld.build();

        List<Object> expected = new ArrayList<Object>();
        expected.add(true);
        expected.add(false);
        assertEquals(expected, attr1.getValue());

        testAttributes(attr1, attr2, attr3);
    }

    void testAttributes(Attribute attr1, Attribute attr2, Attribute attr3) {
        assertEquals(attr1, attr2);
        assertEquals(attr1, attr1);
        assertFalse(attr1.equals(null));

        Set<Attribute> set = new HashSet<Attribute>();
        set.add(attr1);
        set.add(attr2);
        set.add(attr3);
        assertTrue(set.size() == 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void uidFromBuilderInteger() {
        AttributeBuilder.build(Uid.NAME, 1);
    }
    @Test(expected = IllegalArgumentException.class)
    public void uidFromBuilderLong() {
        AttributeBuilder.build(Uid.NAME, 1l);
    }
    @Test(expected = IllegalArgumentException.class)
    public void uidFromBuilderDouble() {
        AttributeBuilder.build(Uid.NAME, 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameFromBuilder() {
        // basic name tests..
        Name actual = (Name) AttributeBuilder.build(Name.NAME, "daf");
        assertEquals(new Name("daf"), actual);
        AttributeBuilder bld = new AttributeBuilder();
        bld.setName(Name.NAME);
        bld.addValue("stuff");
        actual = (Name) bld.build();
        assertEquals(new Name("stuff"), actual);
        // throw the exception at the end..
        AttributeBuilder.build(Name.NAME);
    }

}
