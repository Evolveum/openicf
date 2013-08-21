/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.identityconnectors.solaris.operation;

import static org.identityconnectors.solaris.operation.SolarisScriptOnConnector.quoteForDCLWhenNeeded;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.solaris.operation.SolarisScriptOnConnector.Shell;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test the Script on Resource execution.
 *
 * @author Laszlo Hordos
 */
public class SolarisScriptOnConnectorTest extends SolarisTestBase {

    @DataProvider(name = "shell")
    public Iterator<Object[]> createData() {
        ArrayList<Object[]> test = new ArrayList<Object[]>(Shell.values().length);
        for (Shell shell : Shell.values()) {
            test.add(new Object[] { shell.name() });
        }
        return test.iterator();
    }

    @Test(dataProvider = "shell")
    public void testRunScriptOnResource(String scriptLanguage) throws Exception {
        ScriptContextBuilder builder = new ScriptContextBuilder(scriptLanguage, "echo $ARG_1");
        builder.addScriptArgument("ARG_1", "Acme");
        Object o = getFacade().runScriptOnResource(builder.build(), null);
        assertEquals(o, "Acme");

        builder = new ScriptContextBuilder("sh", "echo $ARG_1");
        o = getFacade().runScriptOnResource(builder.build(), null);
        assertNotEquals(o, "Acme");
    }

    @Test(expectedExceptions = ConnectorException.class,
            expectedExceptionsMessageRegExp = "ERROR, buffer content: <Acme>")
    public void testRunWrongScriptOnResource() throws Exception {
        ScriptContextBuilder builder = new ScriptContextBuilder("sh", "echo $ARG_1\nENDSSH");
        builder.addScriptArgument("ARG_1", "Acme");
        getFacade().runScriptOnResource(builder.build(), null);
        fail("Script must fail");
    }

    @Test
    public void testMultilineRunScriptOnResource() throws Exception {
        ScriptContextBuilder builder =
                new ScriptContextBuilder("sh", "echo $ARG_1\necho $ARG_2\r\necho $ARG_3\r");
        builder.addScriptArgument("ARG_1", "Unix");
        builder.addScriptArgument("ARG_2", "Windows");
        builder.addScriptArgument("ARG_3", "OSX");
        Object o = getFacade().runScriptOnResource(builder.build(), null);
        assertEquals(o, "Unix\r\nWindows\r\nOSX");
    }

    @Test
    public void testComaRunScriptOnResource() throws Exception {
        ScriptContextBuilder builder = new ScriptContextBuilder("sh", "echo 'Hello, World'");
        Object o = getFacade().runScriptOnResource(builder.build(), null);
        assertEquals(o, "Hello, World");
    }

    @Test
    public void testQuoteForDCLWhenNeeded() throws Exception {
        assertEquals(quoteForDCLWhenNeeded("poweroff"), "poweroff");
        assertEquals(quoteForDCLWhenNeeded("`poweroff`"), "'`poweroff`'");
        assertEquals(quoteForDCLWhenNeeded("poweroff' && poweroff && export ARG='poweroff"),
                "'poweroff'\\'' && poweroff && export ARG='\\''poweroff'");
        assertEquals(quoteForDCLWhenNeeded("'1' && poweroff && B='2'"),
                "''\\''1'\\'' && poweroff && B='\\''2'\\'''");
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }

    @Override
    public boolean createGroup() {
        return false;
    }
}
