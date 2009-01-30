/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.googleapps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.junit.Test;


import org.identityconnectors.common.EqualsHashCodeBuilder;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;

import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.test.TestHelpers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.junit.After;
import org.junit.Before;

/**
 * Tests for the GoogleAppsConnector.
 *
 * Important note: Google Apps does not re-creating an account for a period
 * of 5 days after deletion. If you add unit test, you must ensure to
 * you generate unique user id's that won't be resued in a 5 day period.
 * These tests use the Java GUID generator to generate unique id's.
 *
 *
 *
 * @author  Warren Strange
 */
public class GoogleAppsConnectorTests {

    /*
     * Example test properties. On your computer, these are defined in your
     * home directory in a file called
     * ~/.connector-googleapps.properties
     *
     * This is done to isolate passwords and other environment specific properties
     */
   
    private GoogleAppsConnection conn;
    private GoogleAppsConnector gapps;
    // Setup/Teardown

    /**
     * Creates the google apps connector
     *
     * For testing we are controlling this (and not defering to the framework)
     */
    @Before
    public void createConnector() throws Exception {


        GoogleAppsConfiguration config = newConfiguration();
        gapps = new GoogleAppsConnector();
        gapps.init(config);
        gapps.test();

    //System.out.println("Set up google apps connector");

    }

    @After
    public void destroyConnector() {
        // no-op as there are no consumed resources
    }

    /**
     *
     * Test basic schema. There really isn't much to do here.
     * We could test for the name or type of a certain attribute
     * but that seems self fulfilling :-), and it means schema
     * updates occur in two places (the schema, and here).
     */
    @Test
    public void testSchema() {
        Schema schema = gapps.schema();
        System.out.println("Schema information: " + schema.toString());
        //Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(1, objectInfos.size());
    }

    /**
     * Test basic CRUD operations
     * 
     * Note on Google Apps:
     * Once you delete an account, you
     * must wait 5 days before reusing the same account id. When 
     * designing tests we need to make sure we do not trigger this
     * condition. 
     */
    @Test
    public void testCreateReadUpdateDeleteUser() {
        TestAccount tst = new TestAccount();
        System.out.println("Creating test account " + tst.toString());
        Set<Attribute> attrSet = tst.toAttributeSet(true);
        assertNotNull(attrSet);


        // create the account
        Uid uid = gapps.create(ObjectClass.ACCOUNT, attrSet, null);
        assertNotNull(uid);
        assertEquals(uid.getUidValue(), tst.getAccountId());


        // try second create of the same user - should fail
        try {
            Uid uid2 = gapps.create(ObjectClass.ACCOUNT, attrSet,null);
            fail("Second create of the same user was supposed to fail. It did not ");
        } catch (Exception e) {
            // do nothing - expected. 
            // todo: What specifically are we expecting?
            System.out.println("OK - Got an exception which we expected (ignore)");
        }

        // search for the user we just created

        TestAccount test2 = fetchAccount(tst.getAccountId());

        assertEquals(tst, test2);

        // test modification of the account
        // to test partial update we dont modify the given name
        String newFamily = "NewFamily";
        //String newGiven = "NewGiven";
        String password = "Newpassword";

        tst.setFamilyName(newFamily);
        //tst.setGivenName(newGiven);
        tst.setPassword(password);
        // update the account
        gapps.update(ObjectClass.ACCOUNT, uid, tst.toAttributeSet(true), null);


        
        // compare the two accounts to see if we got back what we expected
        test2 = fetchAccount(tst.getAccountId());

        assertEquals( tst, test2);


        // delete the test account
        gapps.delete(ObjectClass.ACCOUNT, uid, null);


    }

    private TestAccount fetchAccount(String id) {
        Filter filter = FilterBuilder.equalTo(new Name(id));

        OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setAttributesToGet(GoogleAppsConnector.ATTR_NICKNAME_LIST);

        List<ConnectorObject> r = TestHelpers.searchToList(gapps, ObjectClass.ACCOUNT, filter, builder.build());

        if (r == null || r.size() < 1) {
            return null;
        }

        assertEquals(r.size(), 1); // should only be one
        ConnectorObject obj = r.get(0);

        System.out.println("Object fetched=" + obj);


        return TestAccount.fromConnectorObject(obj);
    }

    /**
     *  Test search for all users.
     *
     * Becuase we are using a public google apps accounts we don't know for sure
     * how many accounts will be returned - so we assume 1 or more is a successs.
     * There will alwayws be an admin account (i.e. at least one)
     *
     */
    @Test
    public void testSearchAll() {
        Filter filter = null; // return all results
        List<ConnectorObject> r = TestHelpers.searchToList(gapps, ObjectClass.ACCOUNT, filter);

        assertNotNull(r);
        assertTrue(r.size() > 0);

        System.out.println("Google Apps accounts:" + r);

    }

    /**
     * Test nicknames. These are aliases for the user's email.
     *
     */
    @Test
    public void testNicknames() {
        int NUMBER_NICKS = 3;

        TestAccount test1 = new TestAccount();

        String suffix = "n" + test1.getAccountId();
       
        List<String> nicks = test1.getNicknames();
        nicks.clear();
        // create a few nicknames
        for (int i = 0; i < NUMBER_NICKS; ++i) {
            nicks.add("n" + i + suffix);
        }

        Uid uid = null;
        try {
           
            uid = gapps.create(ObjectClass.ACCOUNT, test1.toAttributeSet(true), null);


            // read it back  - did we get the same nicknames?
            TestAccount test2 = fetchAccount(test1.getAccountId());
            assertEquals(test2.getNicknames().size(), NUMBER_NICKS);
            assertEquals(test1.getNicknames(), test2.getNicknames());

            // try deleting a nickname - should have one less names
            String n = "n0" + suffix;
            nicks.remove(n);
           

            // update the account
            gapps.update(ObjectClass.ACCOUNT, uid, test1.toAttributeSet(false), null);

            test2 = fetchAccount(test1.getAccountId());
            assertEquals(NUMBER_NICKS - 1, test2.getNicknames().size());
            assertFalse(test2.getNicknames().contains(n));


            // add a new nick name in
            nicks.add("newnick" + suffix);
           

            // update the account
            gapps.update(ObjectClass.ACCOUNT, uid, test1.toAttributeSet(false), null);

            test2 = fetchAccount(test1.getAccountId());
            assertEquals( test2.getNicknames(), test1.getNicknames());

            // now delete all the nicks
            nicks.clear();

            // update the account
            gapps.update(ObjectClass.ACCOUNT, uid, test1.toAttributeSet(false), null);

            test2 = fetchAccount(test1.getAccountId());
            assertEquals( test2.getNicknames(), test1.getNicknames());
            assertEquals( test2.getNicknames().size(), 0);


        } finally {
            // delete the test account
            gapps.delete(ObjectClass.ACCOUNT, uid, null);
        }

    }

    // Helper Methods/Classes
    public static class TestAccount {

        private static final String ACCOUNTID = "accountid";
        private static final String PASSWORD = "password";
        private static final String FIRSTNAME = "givenName";
        private static final String LASTNAME = "familyName";        // Fields..
        private static final String QUOTA = "quota";
        private static final String NICKNAMES = "nicknames";
        private String accountId;
        private String password;
        private String givenName;
        private String familyName;
        private Integer quota;
        private List<String> nicknames;

        /**
         * Create a unique test account fully populated..
         * The account id must be unique to avoid google apps
         * recreate problems.
         */
        public TestAccount() {


            //String x = "test" + System.currentTimeMillis() +  r.nextInt(1000);
            String x = "test" + UUID.randomUUID();

            setGivenName("First");

            setFamilyName("Last");
            setPassword("test123");
            setAccountId(x);
            setAccountId(x);
            setQuota(new Integer(25600));
            List<String> nicks = new ArrayList<String>();
            //nicks.add("alias" + x);
            nicks.add("testalias");
            setNicknames(nicks);
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setPassword(GuardedString password) {
            this.password = getPlainPassword(password);
        }

        private String getPlainPassword(GuardedString password) {
            final StringBuffer buf = new StringBuffer();
            password.access(new GuardedString.Accessor() {

                public void access(char[] clearChars) {
                    buf.append(clearChars);
                }
            });
            return buf.toString();
        }

        public Set<Attribute> toAttributeSet(boolean passwdColDefined) {
            Set<Attribute> ret = new HashSet<Attribute>();
            for (Map.Entry<String, Object> entry : toMap(passwdColDefined).entrySet()) {

                if (entry.getValue() instanceof Collection) {
                    ret.add(AttributeBuilder.build(entry.getKey(), (Collection) entry.getValue()));
                } else {
                    ret.add(AttributeBuilder.build(entry.getKey(), entry.getValue()));
                }
            }

            ret.add(AttributeBuilder.build(Name.NAME, getAccountId()));

            return ret;
        }

        public static TestAccount fromConnectorObject(ConnectorObject obj) {
            TestAccount t = new TestAccount();
            t.getNicknames().clear();
            for (Attribute attr : obj.getAttributes()) {
                String name = attr.getName();
                if (Name.NAME.equalsIgnoreCase(name)) {
                    t.setAccountId(AttributeUtil.getStringValue(attr));
                } else if (FIRSTNAME.equalsIgnoreCase(name)) {
                    t.setGivenName(AttributeUtil.getStringValue(attr));
                } else if (LASTNAME.equalsIgnoreCase(name)) {
                    t.setFamilyName(AttributeUtil.getStringValue(attr));
                } else if (QUOTA.equalsIgnoreCase(name)) {
                    t.setQuota(AttributeUtil.getIntegerValue(attr));
                } else if (NICKNAMES.equalsIgnoreCase(name)) {
                    // must cast list to array of strings
                    List<String> nicks = t.getNicknames();
                    for (Object o : attr.getValue()) {
                        nicks.add((String) o);
                    }
                }
            }
            return t;
        }

        /**
         * @param passwdColDefined
         *            if it should include the password as an attribute
         */
        Map<String, Object> toMap(boolean passwdColDefined) {
            Map<String, Object> map = new HashMap<String,Object>();
            // todo: Do we want NAME in the attr, or just UID?
          
            map.put(Name.NAME, getAccountId());
            map.put(Uid.NAME,  getAccountId());

            if (passwdColDefined) {
                if (getPassword() == null) {
                    map.put(OperationalAttributes.PASSWORD_NAME, null);
                } else {
                    map.put(OperationalAttributes.PASSWORD_NAME, new GuardedString(getPassword().toCharArray()));
                }
            } else {
                map.put(PASSWORD, getPassword());
            }

            map.put(FIRSTNAME, getGivenName());
            map.put(LASTNAME, getFamilyName());
            map.put(QUOTA, getQuota());
            map.put(NICKNAMES, getNicknames());
            return map;
        }

        EqualsHashCodeBuilder getEqualsHashCodeBuilder() {
            EqualsHashCodeBuilder bld = new EqualsHashCodeBuilder();
            bld.appendBean(this);
            return bld;
        }

        @Override
        public String toString() {
            return toMap(true).toString();
        }

        @Override
        public int hashCode() {
            return getEqualsHashCodeBuilder().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            boolean ret = false;
            if (obj instanceof TestAccount) {
                TestAccount tstObj = (TestAccount) obj;

                ret = tstObj.getAccountId().equals(accountId) &&
                        tstObj.getFamilyName().equals(familyName) &&
                        tstObj.getGivenName().equals(givenName) &&
                        tstObj.getNicknames().equals(getNicknames());


            }
            return ret;
        }

        public Integer getQuota() {
            return quota;
        }

        public void setQuota(Integer quota) {
            this.quota = quota;
        }

        public String getGivenName() {
            return givenName;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public List<String> getNicknames() {
            return nicknames;
        }

        public void setNicknames(List<String> nicknames) {
            this.nicknames = nicknames;
        }
    }

    static class TrueFilter implements Filter {

        public boolean accept(ConnectorObject obj) {
            return true;
        }
    }

    /**
     * @return
     */
    public GoogleAppsConfiguration newConfiguration() {
        GoogleAppsConfiguration config = new GoogleAppsConfiguration();


       //System.out.println("Props=" + TestHelpers.getProperties());
        
        config.setConnectionUrl(TestHelpers.getProperty("connector.connectionUrl", null));
        config.setLogin(TestHelpers.getProperty("connector.login", null));
        config.setPassword(TestHelpers.getProperty("connector.password", null));

        //System.out.println("Apps config=" + config);
        return config;
    }

    static String getResourceAsString(String res) {
        return IOUtil.getResourceAsString(GoogleAppsConnectorTests.class, res);
    }
}
