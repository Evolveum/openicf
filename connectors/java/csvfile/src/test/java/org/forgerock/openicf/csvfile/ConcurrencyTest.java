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
package org.forgerock.openicf.csvfile;

import org.forgerock.openicf.csvfile.util.TestUtils;
import org.forgerock.openicf.csvfile.util.Utils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Viliam Repan (lazyman)
 */
public class ConcurrencyTest extends AbstractCsvTest {

    private static final Log LOG = Log.getLog(SearchOpTest.class);

    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_FAMILY_NAME = "familyName";
    private static final String ATTR_COMPANY = "company";
    private static final String ATTR_CITY = "city";

    private ConnectorFacade connector;

    public ConcurrencyTest() {
        super(LOG);
    }

    @Override
    protected void customBeforeMethod(Method method) throws Exception {
        File file = TestUtils.getTestFile("concurrency.csv");
        File backup = TestUtils.getTestFile("concurrency-backup.csv");
        Utils.copyAndReplace(backup, file);

        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setFilePath(TestUtils.getTestFile("concurrency.csv"));
        config.setUniqueAttribute("uid");

        APIConfiguration impl = TestHelpers.createTestConfiguration(CSVFileConnector.class, config);

        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        connector = factory.newInstance(impl);
    }

    @Override
    public void customAfterMethod(Method method) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        factory.dispose();

        connector = null;
    }

    @Test(enabled = true)
    public void concurrency001_TwoWriters_OneAttributeEach__NoReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, ATTR_GIVEN_NAME, null, true),
                new ModifierThread(2, ATTR_FAMILY_NAME, null, true),
        };

        concurrencyUniversal("Test1", 30000L, 500L, mts, null);
    }

    @Test(enabled = true)
    public void concurrency002_FourWriters_OneAttributeEach__NoReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, ATTR_GIVEN_NAME, null, true),
                new ModifierThread(2, ATTR_FAMILY_NAME, null, true),
                new ModifierThread(3, ATTR_COMPANY, null, true),
                new ModifierThread(4, ATTR_CITY, null, true)
        };

        concurrencyUniversal("Test2", 60000L, 500L, mts, null);
    }

    @Test(enabled = true)
    public void concurrency003_OneWriter_TwoAttributes__OneReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, ATTR_GIVEN_NAME, ATTR_COMPANY, true)
        };


        Checker checker = new Checker() {

            @Override
            public void check(int iteration, Uid uid) throws Exception {
                ConnectorObject object = connector.getObject(ObjectClass.ACCOUNT, uid, null);
                String givenName = getAttributeValue(object, ATTR_GIVEN_NAME);
                String company = getAttributeValue(object, ATTR_COMPANY);

                LOG.info("[" + iteration + "] givenName = " + givenName + ", company = " + company);
                if (!ConcurrencyTest.equals(givenName, company)) {
                    String msg = "Inconsistent object state: GivenName = " + givenName + ", company = " + company;
                    LOG.error(msg);
                    throw new AssertionError(msg);
                }
            }
        };

        concurrencyUniversal("Test3", 60000L, 0L, mts, checker);
    }

    @Test(enabled = true)
    public void concurrency004_TwoWriters_TwoAttributesEach__OneReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, ATTR_GIVEN_NAME, ATTR_COMPANY,
                        true),

                new ModifierThread(2, ATTR_FAMILY_NAME, ATTR_CITY, true),
        };


        Checker checker = new Checker() {

            @Override
            public void check(int iteration, Uid uid) throws Exception {
                ConnectorObject object = connector.getObject(ObjectClass.ACCOUNT, uid, null);

                String givenName = getAttributeValue(object, ATTR_GIVEN_NAME);
                String familyName = getAttributeValue(object, ATTR_FAMILY_NAME);
                String company = getAttributeValue(object, ATTR_COMPANY);
                String city = getAttributeValue(object, ATTR_CITY);

                LOG.info("[" + iteration + "] givenName = " + givenName + ", company = " + company
                        + ", familyName = " + familyName + ", city = " + city);
                if (!ConcurrencyTest.equals(givenName, company)) {
                    String msg = "Inconsistent object state: GivenName = " + givenName + ", company = " + company;
                    LOG.error(msg);
                    throw new AssertionError(msg);
                }
                if (!ConcurrencyTest.equals(familyName, city)) {
                    String msg = "Inconsistent object state: FamilyName = " + familyName + ", city = " + city;
                    LOG.error(msg);
                    throw new AssertionError(msg);
                }
            }
        };

        concurrencyUniversal("Test4", 60000L, 0L, mts, checker);
    }

    public static boolean equals(String s1, String s2) {
        return (s1 != null ? s1.equals(s2) : s2 == null);
    }

    private String getAttributeValue(ConnectorObject object, String attrName) {
        Attribute attr = object.getAttributeByName(attrName);
        if (attr == null) {
            return null;
        }

        List<Object> values = attr.getValue();
        if (values == null || values.isEmpty()) {
            return null;
        }

        if (values.size() > 1) {
            throw new IllegalStateException("Attribute " + attrName + " has "
                    + values.size() + " values (must have only one).");
        }

        return (String) values.get(0);
    }

    private interface Checker {
        void check(int iteration, Uid uid) throws Exception;
    }

    private void concurrencyUniversal(String name, long duration, long waitStep, ModifierThread[] modifierThreads,
                                      Checker checker) throws Exception {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name("1"));
        attrs.add(AttributeBuilder.build(ATTR_GIVEN_NAME, "viliam"));
        attrs.add(AttributeBuilder.build(ATTR_FAMILY_NAME, "repan"));
        attrs.add(AttributeBuilder.build(ATTR_COMPANY, "evolveum"));
        attrs.add(AttributeBuilder.build(ATTR_CITY, "bratislava"));

        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);

        LOG.info("*** Object added: " + uid.getUidValue() + " ***");

        LOG.info("*** Starting modifier threads ***");

        for (ModifierThread mt : modifierThreads) {
            mt.setUid(uid);
            mt.start();
        }

        LOG.info("*** Waiting " + duration + " ms ***");
        long startTime = System.currentTimeMillis();
        int readIteration = 1;
        main:
        while (System.currentTimeMillis() - startTime < duration) {

            if (checker != null) {
                checker.check(readIteration, uid);
            }

            if (waitStep > 0L) {
                Thread.sleep(waitStep);
            }

            for (ModifierThread mt : modifierThreads) {
                if (!mt.isAlive()) {
                    LOG.error("At least one of threads died prematurely, finishing waiting.");
                    break main;
                }
            }

            readIteration++;
        }

        for (ModifierThread mt : modifierThreads) {
            mt.stop = true;             // stop the threads
            System.out.println("Thread " + mt.id + " has done " + (mt.counter - 1) + " iterations");
            LOG.info("Thread " + mt.id + " has done " + (mt.counter - 1) + " iterations");
        }

        // we do not have to wait for the threads to be stopped, just examine their results

        Thread.sleep(1000);         // give the threads a chance to finish (before repo will be shut down)

        for (ModifierThread mt : modifierThreads) {
            LOG.info("Modifier thread " + mt.id + " finished with an exception: ", mt.threadResult);
        }

        for (ModifierThread mt : modifierThreads) {
            AssertJUnit.assertTrue("Modifier thread " + mt.id + " finished with an exception: "
                    + mt.threadResult, mt.threadResult == null);
        }
    }

    class ModifierThread extends Thread {

        int id;
        Uid uid;                 // object to modify
        String attribute1;           // attribute to modify
        String attribute2;           // attribute to modify
        boolean checkValue;
        volatile Throwable threadResult;
        volatile int counter = 1;
        public volatile boolean stop = false;

        ModifierThread(int id, String attribute1, String attribute2, boolean checkValue) {
            this.id = id;
            this.attribute1 = attribute1;
            this.attribute2 = attribute2;
            this.setName("Modifier for " + attributeNames());
            this.checkValue = checkValue;
        }

        private String attributeNames() {
            return attribute1 + (attribute2 != null ? "/" + attribute2 : "");
        }

        @Override
        public void run() {
            try {
                while (!stop) {
                    runOnce();
                }
            } catch (Throwable t) {
                LOG.error(t, "Unexpected exception");
                threadResult = t;
            }
        }

        public void runOnce() {
            LOG.info(" --- Iteration number " + counter + " for " + attributeNames() + " ---");

            Set<Attribute> attrs = new HashSet<Attribute>();

            String dataWritten = "[" + attribute1 + ":" + Integer.toString(counter++) + "]";
            attrs.add(AttributeBuilder.build(attribute1, dataWritten));

            if (attribute2 != null) {
                attrs.add(AttributeBuilder.build(attribute2, dataWritten));
            }

            try {
                connector.update(ObjectClass.ACCOUNT, uid, attrs, null);
            } catch (Exception e) {
                String msg = "modifyObject failed while modifying attribute(s) " + attributeNames()
                        + " to value " + dataWritten;
                threadResult = new RuntimeException(msg, e);
                LOG.error(msg, e);
                threadResult = e;
                stop = true;
                return;     // finish processing
            }

            if (checkValue) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                ConnectorObject user;
                try {
                    user = connector.getObject(ObjectClass.ACCOUNT, uid, null);
                } catch (Exception e) {
                    String msg = "getObject failed while getting attribute(s) " + attributeNames();
                    threadResult = new RuntimeException(msg, e);
                    LOG.error(msg, e);
                    threadResult = e;
                    stop = true;
                    return;     // finish processing
                }

                // check the attribute

                String dataRead = getAttributeValue(user, attribute1);

                if (!dataWritten.equals(dataRead)) {
                    threadResult = new RuntimeException("Data read back (" + dataRead
                            + ") does not match the data written (" + dataWritten + ") on attribute " + attribute1);
                    LOG.error("compare failed", threadResult);
                    stop = true;
                    return;
                }

                if (attribute2 != null) {
                    dataRead = getAttributeValue(user, attribute2);

                    if (!dataWritten.equals(dataRead)) {
                        threadResult = new RuntimeException("Data read back (" + dataRead
                                + ") does not match the data written (" + dataWritten + ") on attribute " + attribute2);
                        LOG.error("compare failed", threadResult);
                        stop = true;
                        return;
                    }
                }
            }
        }

        public void setUid(Uid uid) {
            this.uid = uid;
        }

    }
}