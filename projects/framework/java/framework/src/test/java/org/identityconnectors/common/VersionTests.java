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
package org.identityconnectors.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.identityconnectors.common.Version;
import org.junit.Test;

public class VersionTests {

    @Test
    public void testParse() {
        assertEquals(new Version(1), Version.parse("1"));
        assertEquals(new Version(1), Version.parse("1-alpha"));
        assertEquals(new Version(1, 2), Version.parse("1.2"));
        assertEquals(new Version(1, 2), Version.parse("1.2-alpha"));
        assertEquals(new Version(1, 2, 3), Version.parse("1.2.3"));
        assertEquals(new Version(1, 2, 3), Version.parse("1.2.3-alpha"));
        assertEquals(new Version(1, 2, 3, 4), Version.parse("1.2.3.4"));
        assertEquals(new Version(1, 2, 3, 4), Version.parse("1.2.3.4-alpha"));
    }

    @Test
    public void testCompare() {
        assertCompEq(new Version(1), Version.parse("1"));
        assertCompEq(new Version(1, 2), Version.parse("1.2"));
        assertCompEq(new Version(1, 2, 3), Version.parse("1.2.3"));
        assertCompEq(new Version(1, 2, 3, 4), Version.parse("1.2.3.4"));

        assertCompEq(new Version(1), Version.parse("1.0"));
        assertCompEq(new Version(1), Version.parse("1.0.0"));
        assertCompEq(new Version(1), Version.parse("1.0.0.0"));
        assertCompEq(new Version(1, 2), Version.parse("1.2.0"));
        assertCompEq(new Version(1, 2), Version.parse("1.2.0.0"));
        assertCompEq(new Version(1, 2, 3), Version.parse("1.2.3.0"));

        assertCompLt(new Version(1, 2), Version.parse("1.3"));
        assertCompLt(new Version(1, 2, 3), Version.parse("1.2.4"));
        assertCompLt(new Version(1, 2, 3, 4), Version.parse("1.2.3.5"));

        assertCompLt(new Version(2), Version.parse("3"));
        assertCompLt(new Version(2), Version.parse("2.1"));
        assertCompLt(new Version(2, 3), Version.parse("2.3.4"));
        assertCompLt(new Version(2, 3, 4), Version.parse("2.3.4.5"));

        assertCompGt(new Version(1, 2), Version.parse("1.1"));
        assertCompGt(new Version(1, 2, 3), Version.parse("1.2.2"));
        assertCompGt(new Version(1, 2, 3, 4), Version.parse("1.2.3.3"));

        assertCompGt(new Version(2), Version.parse("1"));
        assertCompGt(new Version(2), Version.parse("1.0"));
        assertCompGt(new Version(2, 3), Version.parse("2.2.0"));
        assertCompGt(new Version(2, 3, 4), Version.parse("2.3.3.0"));
    }

    @Test
    public void testQualifiedIgnored() {
        assertEquals(Version.parse("1.2.3"), Version.parse("1.2.3-alpha"));
    }

    @Test
    public void testComponents() {
        Version v = Version.parse("1.2.3.4");
        assertEquals(Integer.valueOf(1), v.getMajor());
        assertEquals(Integer.valueOf(2), v.getMinor());
        assertEquals(Integer.valueOf(3), v.getMicro());
        assertEquals(Integer.valueOf(4), v.getRevision());
    }

    @Test
    public void testCornerCases() {
        Version v = Version.parse("1.0");
        assertEquals(Integer.valueOf(1), v.getMajor());
        assertEquals(Integer.valueOf(0), v.getMinor());
        assertNull(v.getMicro());
        assertNull(v.getRevision());

        try {
            new Version();
            fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }

        try {
            Version.parse(" ");
            fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }

        try {
            Version.parse("foo");
            fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }
    }

    @Test
    public void testEqualsHashCode() {
        Version v1 = Version.parse("1.0");
        Version v2 = Version.parse("1.0.0.0");
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("Version[1.2.3]", Version.parse("1.2.3").toString());
    }

    private void assertCompEq(Version v1, Version v2) {
        assertTrue(v1.compareTo(v2) == 0);
    }

    private void assertCompLt(Version v1, Version v2) {
        assertTrue(v1.compareTo(v2) < 0);
    }

    private void assertCompGt(Version v1, Version v2) {
        assertTrue(v1.compareTo(v2) > 0);
    }
}
