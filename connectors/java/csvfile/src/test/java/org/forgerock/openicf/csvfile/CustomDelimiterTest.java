/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * Portions Copyrighted 2012 Viliam Repan (lazyman)
 *
 * $Id$
 */
package org.forgerock.openicf.csvfile;

import org.forgerock.openicf.csvfile.util.TestUtils;
import org.forgerock.openicf.csvfile.util.Utils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class CustomDelimiterTest extends AbstractCsvTest {

    private static final Log LOG = Log.getLog(CustomDelimiterTest.class);

    private CSVFileConfiguration config;
    private CSVFileConnector connector;

    public CustomDelimiterTest() {
        super(LOG);
    }

    @Override
    public void customAfterMethod(Method method) throws Exception {
        connector.dispose();
        connector = null;
    }

    @Test
    public void pipeFieldDelimiter() throws Exception {
        beforeMethod("pipe.csv", "pipe-backup.csv", "|");

        connector = new CSVFileConnector();
        connector.init(config);

        final List<ConnectorObject> results = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                results.add(co);
                return true;
            }
        };
        connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);
        testResults(results);
    }

    @Test
    public void dotFieldDelimiter() throws Exception {
        beforeMethod("pipe-1.csv", "pipe-backup-1.csv", ".");

        connector = new CSVFileConnector();
        connector.init(config);

        final List<ConnectorObject> results = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                results.add(co);
                return true;
            }
        };
        connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);
        testResults(results);
    }

    private void testResults(List<ConnectorObject> results) {
        assertEquals(2, results.size());

        testEntryOne(results.get(0));
        testEntryTwo(results.get(1));
    }

    private void testEntryOne(ConnectorObject object) {
        assertNotNull(object);
        assertNotNull(object.getUid());
        assertEquals("vilo", object.getUid().getUidValue());
        assertEquals(object.getAttributes().size(), 5);
        testAttribute(object, "__NAME__", "vilo");
        testAttribute(object, "__UID__", "vilo");
        testAttribute(object, "firstName", "viliam");
        testAttribute(object, "lastName", "repan");
        testAttribute(object, "__PASSWORD__", new GuardedString("Z29vZA==".toCharArray()));
    }

    private void testEntryTwo(ConnectorObject object) {
        assertNotNull(object);
        assertNotNull(object.getUid());
        assertEquals("miso", object.getUid().getUidValue());
        assertEquals(object.getAttributes().size(), 5);
        testAttribute(object, "__NAME__", "miso");
        testAttribute(object, "__UID__", "miso");
        testAttribute(object, "firstName", "michal");
        testAttribute(object, "lastName", "mlok");
        testAttribute(object, "__PASSWORD__", new GuardedString("asdf".toCharArray()));
    }

    private void testAttribute(ConnectorObject object, String name, Object... values) {
        Attribute attribute = object.getAttributeByName(name);
        if (values.length == 0) {
            assertNull(attribute);
        } else {
            assertNotNull(attribute, "Attribute '" + name + "' not found.");
            List<Object> attrValues = attribute.getValue();
            assertNotNull(attrValues);
            assertEquals(attrValues.size(), values.length);
            assertEquals(attrValues.toArray(), values, "Attribute '" + name + "' doesn't have same values.");
        }
    }

    private void beforeMethod(String fileName, String backupFileName, String delimiter)
            throws IOException, URISyntaxException {

        File file = TestUtils.getTestFile(fileName);
        File backup = TestUtils.getTestFile(backupFileName);
        Utils.copyAndReplace(backup, file);

        config = new CSVFileConfiguration();
        config.setEncoding("utf-8");
        config.setUniqueAttribute("uid");
        config.setPasswordAttribute("password");
        config.setFilePath(TestUtils.getTestFile(fileName));

        config.setFieldDelimiter(delimiter);
    }
}
