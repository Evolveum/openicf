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
 * Portions Copyrighted 2011 Viliam Repan (lazyman)
 *
 * $Id$
 */
package com.evolveum.polygon.csvfile;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.polygon.csvfile.CSVFileConfiguration;
import com.evolveum.polygon.csvfile.CSVFileConnector;
import com.evolveum.polygon.csvfile.util.CSVSchemaException;
import com.evolveum.polygon.csvfile.util.TestUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchOpTest extends AbstractCsvTest {

    private static final Log LOG = Log.getLog(SearchOpTest.class);

    private CSVFileConnector connector;

    public SearchOpTest() {
        super(LOG);
    }

    @Override
    public void customBeforeMethod(Method method) throws Exception {
        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setEncoding("utf-8");
        config.setFilePath(TestUtils.getTestFile("search.csv"));
        config.setUniqueAttribute("uid");
        config.setPasswordAttribute("password");
        config.setMultivalueDelimiter(";");
        config.setUsingMultivalue(true);

        connector = new CSVFileConnector();
        connector.init(config);
    }

    @Override
    public void customAfterMethod(Method method) throws Exception {
        connector.dispose();
        connector = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullObjectClass() {
        connector.executeQuery(null, "", new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                throw new UnsupportedOperationException("Not implemented.");
            }
        }, null);
    }

    @Test(expectedExceptions = ConnectorException.class)
    public void badObjectClass() {
        connector.executeQuery(ObjectClass.GROUP, "", new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                throw new UnsupportedOperationException("Not implemented.");
            }
        }, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullObjectClassFilter() {
        connector.createFilterTranslator(null, null);
    }

    @Test(expectedExceptions = ConnectorException.class)
    public void badObjectClassFilter() {
        connector.createFilterTranslator(ObjectClass.GROUP, null);
    }

    @Test
    public void createFilterTranslator() {
        FilterTranslator<String> filter = connector.createFilterTranslator(ObjectClass.ACCOUNT, null);
        assertNotNull(filter);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullHandler() {
        connector.executeQuery(ObjectClass.ACCOUNT, null, null, null);
    }

    @Test
    public void correctButStoppedHandler() {
        final List<ConnectorObject> results = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                results.add(co);
                return false;
            }
        };
        connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);

        assertEquals(1, results.size());
        testEntryOne(results.get(0));
    }

    @Test
    public void correctAllQuery() {
        final List<ConnectorObject> results = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                results.add(co);
                return true;
            }
        };
        connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);

        assertEquals(2, results.size());
        testEntryOne(results.get(0));
        testEntryTwo(results.get(1));
    }

    private void testEntryOne(ConnectorObject object) {
        assertNotNull(object);
        assertNotNull(object.getUid());
        assertEquals("vilo", object.getUid().getUidValue());
        assertEquals(object.getAttributes().size(), 6);
        testAttribute(object, "__NAME__", "vilo");
        testAttribute(object, "__UID__", "vilo");
//        testAttribute(object, "uid", "vilo"); //we're not returning unique attribute, it's already there as __UID__
        testAttribute(object, "firstName", "viliam");
        testAttribute(object, "lastName", "repan");
        testAttribute(object, "__PASSWORD__", new GuardedString("Z29vZA==".toCharArray()));
        testAttribute(object, "UserID", "macko", "usko");
    }

    private void testEntryTwo(ConnectorObject object) {
        assertNotNull(object);
        assertNotNull(object.getUid());
        assertEquals("miso", object.getUid().getUidValue());
        assertEquals(object.getAttributes().size(), 5);
        testAttribute(object, "__NAME__", "miso");
        testAttribute(object, "__UID__", "miso");
//        testAttribute(object, "uid", "miso"); //we're not returning unique attribute, it's already there as __UID__
        testAttribute(object, "firstName", "michal");
        testAttribute(object, "lastName");
        testAttribute(object, "__PASSWORD__", new GuardedString("bad=".toCharArray()));
        testAttribute(object, "UserID", "mmiso");
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

    @Test(expectedExceptions = CSVSchemaException.class)
    public void testMissingColumn() throws Exception {
        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setEncoding("utf-8");
        config.setFilePath(TestUtils.getTestFile("missing-column.csv"));
        config.setUniqueAttribute("uid");
        config.setPasswordAttribute("password");

        CSVFileConnector connector = new CSVFileConnector();
        connector.init(config);

        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                return true;
            }
        };
        connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);
    }

    @Test(expectedExceptions = ConnectorIOException.class)
    public void nonExistingFile() throws Exception {
        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setEncoding("utf-8");
        config.setFilePath(new File("C:\\non-existing-file.csv"));
        config.setUniqueAttribute("uid");
        config.setPasswordAttribute("password");

        CSVFileConnector connector = new CSVFileConnector();
        connector.init(config);

        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                return true;
            }
        };
        connector.executeQuery(ObjectClass.ACCOUNT, null, handler, null);
    }

    @Test
    public void bigCsvSearch() throws Exception {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();

        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setEncoding("utf-8");
        config.setFilePath(TestUtils.getTestFile("big.csv"));
        config.setUniqueAttribute("id");
        config.setPasswordAttribute("password");

        APIConfiguration impl = TestHelpers.createTestConfiguration(CSVFileConnector.class, config);
        final ConnectorFacade connector = factory.newInstance(impl);

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        final List<ConnectorObject> cascadedObjects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            int i = 0;

            @Override
            public boolean handle(ConnectorObject co) {
                objects.add(co);
                i++;

                if (i == 10) {
                    ResultsHandler handler = new ResultsHandler() {

                        public boolean handle(ConnectorObject obj) {
                            cascadedObjects.add(obj);
                            return true;
                        }
                    };
                    connector.search(ObjectClass.ACCOUNT, null, handler, null);
                }

                return true;
            }
        };
        connector.search(ObjectClass.ACCOUNT, null, handler, null);

        AssertJUnit.assertEquals(objects, cascadedObjects);
    }

    @Test
    public void testMultivalueEqualsSearch() throws Exception {
        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setEncoding("utf-8");
        config.setFilePath(TestUtils.getTestFile("search.csv"));
        config.setUniqueAttribute("uid");
        config.setPasswordAttribute("password");
        config.setMultivalueDelimiter(";");
        config.setUsingMultivalue(true);

        ConnectorFacade connector = getFacade(config);
        Filter filter;
        // filter = new EqualsFilter(AttributeBuilder.build("UserID", "macko", "usko"));
        filter = new ContainsFilter(AttributeBuilder.build("UserID", "macko"));
        // filter = new ContainsAllValuesFilter(AttributeBuilder.build("UserID", "macko", "usko"));

        final List<ConnectorObject> results = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject co) {
                results.add(co);
                return true;
            }
        };

        connector.search(ObjectClass.ACCOUNT, filter, handler, null);

        AssertJUnit.assertEquals(1, results.size());
    }
}
