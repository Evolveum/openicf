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
package org.identityconnectors.contract.data;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.contract.exceptions.ObjectNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * unit test for GroovyDataProvider
 * 
 * @author David Adam
 * 
 * Note: if getting MissingMethodException, update imports in configfile.groovy
 * 
 */
public class GroovyDataProviderTest {
    private static final String NON_EXISTING_PROPERTY = "abcdefghi123asiosfjds";
    private static GroovyDataProvider gdp;
    public static final String CONFIG_FILE_PATH = "configfileTest.groovy";

    @Before
    public void setUp() {
        gdp = new GroovyDataProvider(CONFIG_FILE_PATH, null, null);
    }

    @After
    public void tearDown() {
        gdp = null;
    }

    @Test
    public void testSimpleStr() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Assert
                .assertEquals(
                        "If you think you can do a thing or think you can't do a thing, you're right. (H. Ford)",
                        gdp.get("aSimpleString", "string", true));
    }
    
    @Test
    public void testProperDefaulting() {
        Object o = get("nonexistingAttribute");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof String);
        String nonExistingAttribute = (String) o;

        Object o2 = get("nonexistingAttribute");
        Assert.assertNotNull(o2);
        Assert.assertTrue(o2 instanceof String);
        String nonExistingAttribute2 = (String) o2;

        final String message = "if we query the same attribute twice, it should return the same default value";
        Assert.assertTrue(message, nonExistingAttribute
                .equals(nonExistingAttribute2));

        Object o3 = get("anotherNonExistingAttribute");
        Assert.assertNotNull(o3);
        Assert.assertTrue(o3 instanceof String);
        String anotherNonExistingAttribute = (String) o3;

        Assert.assertTrue(
                "different properties should return different 'generated' values!",
                !anotherNonExistingAttribute.equals(nonExistingAttribute));

        Object o4 = get("anotherNonExistingAttribute");
        Assert.assertNotNull(o4);
        Assert.assertTrue(o4 instanceof String);
        String anotherNonExistingAttribute2 = (String) o4;

        Assert.assertTrue(message, anotherNonExistingAttribute
                .equals(anotherNonExistingAttribute2));
    }

    /** helper method of {@link GroovyDataProviderTest#testProperDefaulting()};
     * in case of missing value should return default one.
     */
    private Object get(String string) {
        return gdp.get(String.class, string, "foocomponent");
    }
    
    @Test @Ignore
    public void testProperDefaultingMulti() {
        Object o = getMulti("nonexistingAttribute");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof List && ((List) o).get(0) instanceof String);
        List nonExistingAttribute = (List) o;

        Object o2 = getMulti("nonexistingAttribute");
        Assert.assertNotNull(o2);
        Assert.assertTrue(o2 instanceof List && ((List) o2).get(0) instanceof String);
        List nonExistingAttribute2 = (List) o2;

        final String message = "if we query the same attribute twice, it should return the same default value";
        Assert.assertTrue(message, nonExistingAttribute
                .equals(nonExistingAttribute2));

        Object o3 = getMulti("anotherNonExistingAttribute");
        Assert.assertNotNull(o3);
        Assert.assertTrue(o3 instanceof List && ((List) o3).get(0) instanceof String);
        List anotherNonExistingAttribute = (List) o3;

        //TODO fix problem with uniform unique values.
        Assert.assertTrue(
                "different properties should return different 'generated' values!",
                !anotherNonExistingAttribute.equals(nonExistingAttribute));

        Object o4 = getMulti("anotherNonExistingAttribute");
        Assert.assertNotNull(o4);
        Assert.assertTrue(o4 instanceof List && ((List) o4).get(0) instanceof String);
        List anotherNonExistingAttribute2 = (List) o4;

        Assert.assertTrue(message, anotherNonExistingAttribute
                .equals(anotherNonExistingAttribute2));
    }

    /** helper method of {@link GroovyDataProviderTest#testProperDefaultingMulti()
     * in case of missing value should return default one. 
     */
    private Object getMulti(String string) {
        return gdp.get(String.class, string, "foocomponent", 0, true);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testNonExistingProperty() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Object o = getProperty(gdp, NON_EXISTING_PROPERTY);
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof ConfigObject);
        if (o instanceof ConfigObject) {
            ConfigObject co = (ConfigObject) o;
            Assert.assertEquals(0, co.size());
        }
    }

    @Test
    public void testSimpleMapAcquire() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Object o = getProperty(gdp, "sampleMap");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof Map);
        printMap(o);
    }

    @Test
    public void testDotInNameMapAcquire() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Object o = getProperty(gdp, "sampleMap.foo.bar");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof Map);
        printMap(o);
    }

    @Test
    public void testRecursiveAcquire() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        // query for a property with non-existing prefix foo
        // the DataProvider should try to evaluate substrings of the property
        // name (divided by .)
        // and find "abc"
        Object o = getProperty(gdp, "foo.abc");
        Assert.assertNotNull(o);
        Assert.assertEquals("abc", o.toString());
        printMap(o);
    }

    @Test
    public void testDotNameString() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Object o = getProperty(gdp, "eggs.spam.sausage");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof String);
        Assert.assertEquals("the spanish inquisition", o.toString());
    }

    @Test
    public void testRandom() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Object o = getProperty(gdp, "random");
        Object o2 = getProperty(gdp, "random");
        Assert.assertNotNull(o);
        Assert.assertEquals(o, o2);
    }

    @Test
    public void testRandomHierarchicalName() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());
        Object o = getProperty(gdp, "foo.bla.horror.random");
        Object o2 = getProperty(gdp, "foo.bla.horror.random");
        Assert.assertNotNull(o);
        Assert.assertEquals(o, o2);
    }

    @Test
    public void configFileMerger() {
        ConfigSlurper cs = new ConfigSlurper();

        ConfigObject co1 = cs.parse("a = '1'\n b = '2'");
        assert "1" == co1.getProperty("a");
        ConfigObject co2 = cs.parse("c='3'\n d='4'");
        assert "3" == co2.getProperty("c");

        ConfigObject f = GroovyDataProvider.mergeConfigObjects(co1, co2);
        assert "1" == f.getProperty("a");
        assert "3" == f.getProperty("c");
    }

    @Test
    public void configFileMergerAdvanced() {
        ConfigSlurper cs = new ConfigSlurper();

        ConfigObject lowPriorityConfig = cs.parse("a = '1'\n c = '2'");
        assert "1" == lowPriorityConfig.getProperty("a");
        ConfigObject highPriorityConfig = cs.parse("c='3'\n d='4'");
        assert "3" == highPriorityConfig.getProperty("c");

        ConfigObject f = GroovyDataProvider.mergeConfigObjects(
                lowPriorityConfig, highPriorityConfig);
        assert "1" == f.getProperty("a");
        assert "3" == f.getProperty("c");
    }

    @Test
    public void testNewRandomGenerator() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());

        Object o = getProperty(gdp, "randomNewAge");
        Object o2 = getProperty(gdp, "remus");

        Assert.assertNotNull(o);
        Assert.assertNotNull(o2);

        Assert.assertTrue(o instanceof Long);
        Assert.assertTrue(o2 instanceof Integer);

        System.out.println("long value: " + o.toString() + "   int value: "
                + o2.toString());
    }

    @Test
    public void testMapAttributesNew() throws Exception {
        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());

        {
            Object o = getProperty(gdp, "attributeMap.string");
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof String);
            Assert.assertTrue(o.toString() == "Good morning!");
        }

        {
            Object o = getProperty(gdp, "attributeMapSecond.stringSec");
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof String);
            Assert.assertTrue(o.toString() == "Good morning Mrs. Smith!");
        }

        Assert.assertTrue(new File(CONFIG_FILE_PATH)
                .exists());

        {
            Object o = getProperty(gdp, "Delete.account.__NAME__.string");
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof String);
            Assert.assertTrue(o.toString() == "blaf");
        }

        {
            Object o = getProperty(gdp, "account.__NAME__.string");
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof String);
            Assert.assertTrue(o.toString() == "blaf blaf");
        }

    }

    @Test
    public void literalsMacroReplacementTest() throws Exception {
        {
            Object o = getProperty(gdp, "Tfloat");
            Assert.assertNotNull(o);
            System.out.println(o.getClass().getName() + " " + o.toString());
            Assert.assertTrue(o instanceof Float);
        }
    }

    @Test
    public void multiStringListTest() throws Exception {

        // multi.Tstring=[Lazy.random("AAAAA##") , Lazy.random("AAAAA##")]
        Object o = getProperty(gdp, "multi.Tstring");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof List);
        if (o instanceof List) {
            List l = (List) o;
            printList(l);
            System.out.println();
        }
    }

    @Test
    public void multiStringRecursiveTest() throws Exception {

        Object o = getProperty(gdp, "multi.recursive.Tstring", false);
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof List);
        if (o instanceof List) {
            List l = (List) o;

            boolean recursiveListPresent = false;
            boolean recursiveListPresent2 = false;
            for (Object object : l) {
                if (object instanceof List) {
                    recursiveListPresent = true;

                    List lRec = (List) object;
                    for (Object object2 : lRec) {
                        if (object2 instanceof List) {
                            recursiveListPresent2 = true;
                        }
                    }
                }
            }
            Assert.assertTrue(recursiveListPresent);
            Assert.assertTrue(recursiveListPresent2);

            printList(l);
            System.out.println("\n");
        }
    }

    @Test
    public void testByteArray() throws Exception {
        Object o = getProperty(gdp, "byteArray.test");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof byte[]);
        if (o instanceof byte[]) {
            byte[] barr = (byte[]) o;
            System.out.println("Byte Array values:");
            for (byte b : barr) {
                System.out.print(Byte.toString(b) + ", ");
            }
            System.out.println();
        }
    }

    @Test
    public void characterTest() throws Exception {
        Object o = getProperty(gdp, "charTest");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof Character);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testNonExistingDefault() throws Exception {
        // should not return default vale
        Object o = getProperty(gdp, "connector.login");
    }

    @Test
    public void testNestedPropertyQuery() throws Exception {
        Object o = getProperty(gdp, "SchemaXX.sample");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof String
                && o.toString() == "Mysterious universe");
    }

    @Test
    public void testAtAtPropertyNamesQuery() throws Exception {
        Object o = getProperty(gdp, "Schema.__NAME__.attribute.account");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof String && o.toString() == "Ahoj ship!");
    }
    
    /**
     * tests {@link GroovyDataProvider#getShortTypeName(Class)} method.
     */
    @Test
    public void testGetShortTypeName() {
        Assert.assertTrue(GroovyDataProvider.getShortTypeName(String.class).equals("Tstring"));
        byte[] barr = new byte[0];
        Assert.assertTrue(GroovyDataProvider.getShortTypeName(barr.getClass()).equals("Tbytearray"));
        Assert.assertTrue(GroovyDataProvider.getShortTypeName(GuardedString.class).equals("Tstring"));
    }
    
    @Test
    /**
     * test {@link GroovyDataProvider#get(String)}
     * test {@link GroovyDataProvider#get(String, int)}
     */
    public void testSimpleGet() {
        DataProvider dp = (DataProvider) gdp;
        Assert.assertTrue(dp.get("aaa.bbb.xxx").equals("ahoj"));
        Assert.assertTrue(dp.get("param", 9).equals("foobar"));
    }
    
    @Test
    /**
     * Test queries for non-existing parameter "foo.bar"
     * and expects a List to return based on one defined in multi.Tstring.
     * (resides in bootsrap.groovy)
     */
    public void testDefaultValues() {
        DataProvider dp = (DataProvider) gdp;
        Object o = dp.get(String.class, "bar", "foo", -1, true);
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof List && ((List) o).size() > 0);
        List l = (List) o;
        Assert.assertTrue(l.get(0) instanceof String);
    }
    
    /**
     * Tests the resolving of Lazy values inside map
     */
    @Test
    public void testLazyMap() {
        Object o = ((DataProvider) gdp).get("mapWithLazyCalls");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof Map);
        Map m = (Map) o;
        int cntr = 0;
        for (Iterator iterator = m.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry current = (Map.Entry) iterator.next();
            if (cntr == 0 || cntr == 1) {
                Assert.assertTrue(current.getValue() instanceof String);
                //System.out.println("k: " + current.getKey().toString() + " v: " + current.getValue().toString());
            } else {
                Assert.fail("should not be more than two items in the map");
            }
        }
    }
    
    /**
     * test acquiring a map all at once (good for unit tests)
     */
    @Test
    public void testAcquireMap() {
        Object o = ((DataProvider) gdp).get("abcAccount.all");
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof Map);
        Map m = (Map) o;
        Assert.assertTrue(m.get("__NAME__") instanceof String);
        Assert.assertTrue(((String) m.get("__NAME__")).startsWith("CONUSR-"));
        Assert.assertTrue(m.get("__PASSWORD__") instanceof String);
        Assert.assertTrue(((String) m.get("__PASSWORD__")).equals("tstpwd"));
    }

    /* ************* UTILITY METHODS ***************** */
    /**
     * recursively print a list
     */
    private void printList(List l) {
        System.out.print(" [ ");
        for (Object object : l) {
            if (object instanceof List) {
                List newL = (List) object;
                printList(newL);
            } else {
                System.out.print(object.toString() + ", ");
            }
        }
        System.out.print(" ] ");
    }

    private void printMap(Object o) {
        if (o instanceof Map) {
            Map m = (Map) o;
            Set tmpSet = m.entrySet();
            for (Object object : tmpSet) {
                System.out.print(object + " ");
            }
            System.out.println("\n");
        }
    }

    /**
     * just call the get, and do some println for neat output.
     * 
     * @param gdp2
     * @param propertyName
     * @return
     * @throws Exception
     */
    private Object getProperty(GroovyDataProvider gdp2, String propertyName)
            throws Exception {

        return getProperty(gdp2, propertyName, true);
    }

    /**
     * helper method that retrieves the parameter from the properties file and
     * tries to display useful information about its contents on System.out
     */
    private Object getProperty(GroovyDataProvider gdp2, String propertyName,
            boolean printOut) throws Exception {
        // doing the acquire of property!
        Object o = gdp2.get(propertyName, "string", true);

        // just informational output
        if (o instanceof Map) {
            Map m = (Map) o;
            String s = (m.size() > 0) ? "is present" : "is missing";
            if (printOut) {
                System.out.println("property " + propertyName + " " + s);
            }
        } else {
            String s = o.toString();
            if (printOut) {
                System.out.println("property " + propertyName + " : "
                        + ((s.length() > 0) ? s : "EMPTY STRING"));
            }
        }

        return o;
    }
    /* *********************** SNAPSHOT GENERATING FEATURE TESTS ************************** */
    
    /** Test of Lazy.get() and Lazy.random()*/
    @Test
    public void getPropertyTest() {
        Assert.assertTrue(gdp.get("rumulus", null, false).equals(
                gdp.get("rumulus", null, false)));
        Assert.assertTrue(gdp.get("remus", null, false).equals(
                gdp.get("rumulus", null, false)));
    }

    /** Test of left sides for the snapshot output */
    @Test
    public void testSnapshotGenerating() throws IOException {
        Object o = gdp.writeDataToFile();

        // read the file line by line
        List<String> lines = readLines(CONFIG_FILE_PATH);

        // parse and control the properties in the written file
        // FOR NOW just left side from the assigment is controlled.
        parseAndControl(lines);

        // output, might comment this out
        // System.out.println(o.toString());
    }

    /**
     * method controls, if single parameters are correctly quoted, and multi
     * params.
     * 
     * Correct quotation means: foo."bar"."boo" = "baa" bar = "baa"
     * 
     * @param lines
     */
    private void parseAndControl(List<String> lines) {
        for (String currentLine : lines) {
            // divide the line based on "=" delimiter
            if (currentLine.contains(gdp.ASSIGNMENT_MARK)) {
                String[] arr = currentLine.split(gdp.ASSIGNMENT_MARK);
                if (arr.length == 2) {
                    String leftPart = arr[0];
                    Assert.assertTrue(!leftPart.equals(""));

                    // split the left side based on "." separators
                    String[] subparts = leftPart.split(gdp.PROPERTY_SEPARATOR);
                    for (int i = 0; i < subparts.length; i++) {
                        if (i == 0) {
                            Assert.assertTrue(!subparts[i]
                                    .startsWith(gdp.PROPERTY_SEPARATOR));
                            Assert.assertTrue(!subparts[i]
                                    .endsWith(gdp.PROPERTY_SEPARATOR));
                        } else {
                            Assert.assertTrue(subparts[i]
                                    .startsWith(gdp.PROPERTY_SEPARATOR));
                            Assert.assertTrue(subparts[i]
                                    .endsWith(gdp.PROPERTY_SEPARATOR));
                        }
                    }
                }
            }
        }// for
    }

    /** read lines from given file line by line */
    private List<String> readLines(String configFilePath) throws IOException {
        List<String> result = new ArrayList<String>();

        // get the lines of the original property file
        BufferedReader input = new BufferedReader(new FileReader(
                CONFIG_FILE_PATH));
        try {
            String line = null; // not declared within while loop

            while ((line = input.readLine()) != null) {
                result.add(line);
            }
        } finally {
            input.close();
        }
        return result;
    }
}
