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
package org.identityconnectors.flatfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.EqualsHashCodeBuilder;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class FlatFileConnectorTests {

    // =======================================================================
    // Constants..
    // =======================================================================
    private static final String FILENAME = "test.csv";

    private static final String CHANGE_NUMBER = "changeNumber";

    private static final String ACCOUNTID = "accountid";

    private static final String FIRSTNAME = "firstname";

    private static final String LASTNAME = "lastname";

    private static final String EMAIL = "email";

    private static final char FIELD_DELIMITER = ',';

    private static final char TEXT_QUALIFIER = '"';

    private static TestAccount HEADER = new TestAccount(ACCOUNTID, FIRSTNAME,
            LASTNAME, EMAIL, CHANGE_NUMBER);

    private static Set<TestAccount> TEST_ACCOUNTS = new HashSet<TestAccount>();
    static {
        TEST_ACCOUNTS.add(new TestAccount("jpc4323435", "jPenelope", "jCruz",
                "jxPenelope.Cruz@mail.com", "0"));
        TEST_ACCOUNTS.add(new TestAccount("jkb3234416", "jKevin", "jBacon",
                "jxKevin.Bacon@mail.com", "1"));
        TEST_ACCOUNTS.add(new TestAccount("jpc4323436", "jPenelope", "jCruz2",
                "jyPenelope.Cruz@mail.com", "2"));
        TEST_ACCOUNTS.add(new TestAccount("jkb3234417", "jKevin", "jBacon,II",
                "jyKevin.Bacon@mail.com", "3"));
        TEST_ACCOUNTS.add(new TestAccount("jpc4323437", "jPenelope", "jCruz3",
                "jzPenelope.Cruz@mail.com", "4"));
        TEST_ACCOUNTS.add(new TestAccount("jkb3234419", "jKevin", "jBacon,III",
                "jzKevin.Bacon@mail.com", "5"));
        TEST_ACCOUNTS.add(new TestAccount("billy@bob.com", "jBilly", "jBob",
                "jaBilly.Bob@mail.com", "6"));
        TEST_ACCOUNTS.add(new TestAccount("bil@bob@bob.com", "jBillyBob",
                "jBobby", "jaBillyBob.Bobby@mail.com", "7"));

    };

    // =======================================================================
    // Setup/Tear down..
    // =======================================================================
    @Before
    public void createData() throws Exception {
        // create a csv data file..
        File f = new File(FILENAME);
        // make sure to delete..
        // f.deleteOnExit();
        // write out file data..
        OutputStream fout = new FileOutputStream(f);
        Writer w = new OutputStreamWriter(fout, getUTF8Charset());
        PrintWriter wrt = new PrintWriter(w);
        // write out each user..
        wrt.println(HEADER.toLine(FIELD_DELIMITER, TEXT_QUALIFIER));
        for (TestAccount user : TEST_ACCOUNTS) {
            String line = user.toLine(FIELD_DELIMITER, TEXT_QUALIFIER);
            wrt.println(line);
        }
        wrt.close();
    }

    Charset getUTF8Charset() {
        return Charset.forName("UTF-8");
    }

    // =======================================================================
    // Tests
    // =======================================================================
    @Test
    public void search() {
        // create the connector configuration..
        FlatFileConfiguration config = new FlatFileConfiguration();
        config.setFile(new File(FILENAME));
        config.setUniqueAttributeName(ACCOUNTID);
        config.validate();
        // create a new connector..
        FlatFileConnector cnt = new FlatFileConnector();
        cnt.init(config);
        // don't bother filtering the framework can do it..
        Set<TestAccount> actual = new HashSet<TestAccount>();
        List<ConnectorObject> 
            results = TestHelpers.searchToList(cnt,ObjectClass.ACCOUNT,new NoFilter());
        for (ConnectorObject obj : results) {
            actual.add(new TestAccount(obj));
        }
        cnt.dispose();
        // print out the actual..
        // System.out.println("Actual: " + actual);
        // System.out.println("Expected: " + TEST_ACCOUNTS);
        // attempt to see if they compare..
        Assert.assertEquals(TEST_ACCOUNTS, actual);
    }

    static class NoFilter implements Filter {
        public boolean accept(ConnectorObject obj) {
            return true;
        }
    }

    @Test
    public void functional() throws Exception {
        // create the connector configuration..
        FlatFileConfiguration config = new FlatFileConfiguration();
        config.setFile(new File(FILENAME));
        config.setUniqueAttributeName(ACCOUNTID);
        config.validate();
        final Set<TestAccount> actual = new HashSet<TestAccount>();
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(
                FlatFileConnector.class, config);
        ConnectorFacade facade = factory.newInstance(impl);
        facade.search(ObjectClass.ACCOUNT,new NoFilter(), new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                actual.add(new TestAccount(obj));
                return true;
            }
        },null);
        // print out the actual..
        // System.out.println("Actual: " + actual);
        // System.out.println("Expected: " + TEST_ACCOUNTS);
        // attempt to see if they compare..
        Assert.assertEquals(TEST_ACCOUNTS, actual);
    }

    // =======================================================================
    // Helper Methods..
    // =======================================================================

    private static class TestAccount {
        private String _changeNumber;

        private String _accountId;

        private String _firstName;

        private String _lastName;

        private String _email;

        public TestAccount(String accountId, String firstName, String lastName,
                String email, String changeNumber) {
            _accountId = accountId;
            _firstName = firstName;
            _lastName = lastName;
            _email = email;
            _changeNumber = changeNumber;
        }

        public TestAccount(ConnectorObject obj) {
            _accountId = obj.getUid().getUidValue();
            // go through each of the other variables..
            for (Attribute attr : obj.getAttributes()) {
                if (CHANGE_NUMBER.equals(attr.getName())) {
                    _changeNumber = AttributeUtil.getStringValue(attr);
                } else if (FIRSTNAME.equals(attr.getName())) {
                    _firstName = AttributeUtil.getStringValue(attr);
                } else if (LASTNAME.equals(attr.getName())) {
                    _lastName = AttributeUtil.getStringValue(attr);
                } else if (EMAIL.equals(attr.getName())) {
                    _email = AttributeUtil.getStringValue(attr);
                }
            }
        }

        public String getAccountId() {
            return _accountId;
        }

        public String getFirstName() {
            return _firstName;
        }

        public String getLastName() {
            return _lastName;
        }

        public String getEmail() {
            return _email;
        }

        public String getChangeNumber() {
            return _changeNumber;
        }

        EqualsHashCodeBuilder getEqHash() {
            EqualsHashCodeBuilder ret = new EqualsHashCodeBuilder();
            ret.append(getAccountId());
            ret.append(getFirstName());
            ret.append(getLastName());
            ret.append(getEmail());
            ret.append(getChangeNumber());
            return ret;
        }

        @Override
        public boolean equals(Object obj) {
            boolean ret = false;
            if (obj instanceof TestAccount) {
                ret = getEqHash().equals(((TestAccount) obj).getEqHash());
            }
            return ret;
        }

        @Override
        public int hashCode() {
            return getEqHash().hashCode();
        }

        @Override
        public String toString() {
            // poor man's to string..
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", _accountId);
            map.put("changeNumber", _changeNumber);
            map.put("email", _email);
            map.put("firstName", _firstName);
            map.put("lastName", _lastName);
            return map.toString();
        }

        /**
         * Create a string representation of a field, using the textQualifier if
         * the fieldDelimiter is contained in the field's value.
         * 
         * @param field
         *            String value of field to convert/externalize.
         * @param fieldDelimiter
         *            delimiter used between fields
         * @param textQualifier
         *            text qualifier to use as needed
         * @return String representation of the field suitable for writing.
         */
        static String getField(final String field, final char fieldDelimiter,
                final char textQualifier) {
            String result = field;
            if ((field != null) && (field.indexOf(fieldDelimiter) > -1)) {
                result = textQualifier + field + textQualifier;
            }
            return result;
        }

        public String toLine(final char fieldd, final char textq) {
            StringBuffer buf = new StringBuffer();

            buf.append(getField(getAccountId(), fieldd, textq));
            buf.append(fieldd);
            buf.append(getField(getFirstName(), fieldd, textq));
            buf.append(fieldd);
            buf.append(getField(getLastName(), fieldd, textq));
            buf.append(fieldd);
            buf.append(getField(getEmail(), fieldd, textq));
            buf.append(fieldd);
            buf.append(getField(getChangeNumber(), fieldd, textq));

            return buf.toString();
        }
    }

}
