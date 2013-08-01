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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.command;

import java.util.Arrays;
import java.util.List;

import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.test.SolarisTestCommon;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class CommandBuilderTest {
    @Test
    public void test() {
        // expecting that sudo is turned off by default.
        SolarisConnection conn = SolarisTestCommon.getSolarisConn();
        String actual = conn.buildCommand(true, "command", "arg1", "arg2", "arg3");
        String expected = "command arg1 arg2 arg3";
        List<String> parsedActual = parseList(actual);
        List<String> parsedExpected = parseList(expected);
        AssertJUnit.assertEquals(parsedExpected, parsedActual);
    }

    private List<String> parseList(String actual) {
        return Arrays.asList(actual.split("[\\s]+"));
    }
}
