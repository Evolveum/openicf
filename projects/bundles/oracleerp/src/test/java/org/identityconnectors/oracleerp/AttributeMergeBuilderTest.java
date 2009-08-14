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
package org.identityconnectors.oracleerp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;

/**
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
public class AttributeMergeBuilderTest {

    private static final String TEST_NAME1 = "name1";
    private static final String TEST_NAME2 = "name2";
    private static final String TEST_NAME3 = "name3";
    private static final String[] TO_GET_NAMES = { TEST_NAME1, TEST_NAME2, TEST_NAME3 };
    private static final String[] TO_GET_NAMES2 = { TEST_NAME2};
    private static final Set<String> TO_GET_SET = CollectionUtil.newSet(TO_GET_NAMES);
    private static final Set<String> TO_GET_SET2 = CollectionUtil.newSet(TO_GET_NAMES2);
    private static final String[] TEST_VALUE1 = null;
    private static final String[] TEST_VALUE2 = { "value1", "value2" };
    private static final String[] TEST_VALUE3 = { "value2", "value3" };
    private static final String[] TEST_RESULT3 = { "value1", "value2", "value3" };
    private static final String[] TEST_VALUE4 = { null };
    private static final String[] TEST_RESULT4 = { "value2", "value3", "value1" };

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#AttributeMergeBuilder()}.
     */
    @Test
    public void testEmptyBuild() {
        AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        assertNotNull("null builder", bld.build());
        assertTrue("null builder", bld.build().size() == 0);
    }

    /**
     * Test method for
     * {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#addAttribute(java.lang.String, java.util.Collection)}
     * .
     */
    @Test
    public void testAddAttributeStringCollectionOfObject() {
        final Attribute attrR2 = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT3);
        final Attribute attrR3 = AttributeBuilder.build(TEST_NAME3, (Object[]) TEST_RESULT4);
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        bld.addAttribute(TEST_NAME2, CollectionUtil.newReadOnlyList(TEST_VALUE2));
        bld.addAttribute(TEST_NAME3, CollectionUtil.newReadOnlyList(TEST_VALUE3));
        bld.addAttribute(TEST_NAME2, CollectionUtil.newReadOnlyList(TEST_VALUE3));
        bld.addAttribute(TEST_NAME3, CollectionUtil.newReadOnlyList(TEST_VALUE2));
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 2, actualList.size());
        assertTrue(TEST_NAME2, actualList.contains(attrR2));
        assertTrue(TEST_NAME3, actualList.contains(attrR3));
    }

    /**
     * Test method for
     * {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#addAttribute(java.lang.String, java.lang.Object[])}
     * .
     */
    @Test
    public void testAddAttributeStringObjectArray() {
        final Attribute attrR2 = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT3);
        final Attribute attrR3 = AttributeBuilder.build(TEST_NAME3, (Object[]) TEST_RESULT4);
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE2);
        bld.addAttribute(TEST_NAME3, (Object[]) TEST_VALUE3);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE3);
        bld.addAttribute(TEST_NAME3, (Object[]) TEST_VALUE2);
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 2, actualList.size());
        assertTrue(TEST_NAME2, actualList.contains(attrR2));
        assertTrue(TEST_NAME3, actualList.contains(attrR3));
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#build()}.
     */
    @Test
    public void testMergeBuild() {
        final Attribute attrR = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT3);
        final Attribute attrRE = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT4);
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE2);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE3);
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 1, actualList.size());
        assertTrue(TEST_NAME2, actualList.contains(attrR));
        assertFalse(TEST_NAME2, actualList.contains(attrRE));
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#build()}.
     */
    @Test
    public void testMergeNullBuild() {
        final Attribute exp2 = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT3);
        final Attribute attrRE = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT4);
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE2);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE3);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE1);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE4);
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 1, actualList.size());
        assertTrue(TEST_NAME2, actualList.contains(exp2));
        assertFalse(TEST_NAME2, actualList.contains(attrRE));
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#build()}.
     */
    @Test
    public void testMergeNullFirstBuild() {
        final Attribute attrR = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT3);
        final Attribute attrRE = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT4);
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE1);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE2);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE3);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE4);
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 1, actualList.size());
        assertTrue(TEST_NAME2, actualList.contains(attrR));
        assertFalse(TEST_NAME2, actualList.contains(attrRE));
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#build()}.
     */
    @Test
    public void testMergeNullAttribBuild() {
        final Attribute attrR = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT3);
        final Attribute attrRE = AttributeBuilder.build(TEST_NAME2, (Object[]) TEST_RESULT4);
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE2);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE3);
        bld.addAttribute(TEST_NAME1, (Object[]) TEST_VALUE1);
        bld.addAttribute(TEST_NAME1, (Object[]) TEST_VALUE4);
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 2, actualList.size());
        assertTrue(TEST_NAME2, actualList.contains(attrR));
        assertFalse(TEST_NAME2, actualList.contains(attrRE));
    }

    /**
     * Test method for {@link org.identityconnectors.oracleerp.AttributeMergeBuilder#build()}.
     */
    @Test
    public void testMergeToGetNames() {
        final AttributeMergeBuilder bld = new AttributeMergeBuilder(TO_GET_SET2);
        bld.addAttribute(TEST_NAME1, (Object[]) TEST_VALUE1);
        bld.addAttribute(TEST_NAME2, (Object[]) TEST_VALUE2);
        bld.addAttribute(TEST_NAME3, (Object[]) TEST_VALUE3);
        final List<Attribute> actualList = bld.build();
        assertNotNull("null builder", actualList);
        assertEquals("size", 1, actualList.size());
    }
    
    
}
