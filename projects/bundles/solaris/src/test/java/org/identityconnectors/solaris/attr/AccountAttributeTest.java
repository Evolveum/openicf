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
package org.identityconnectors.solaris.attr;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.operation.PasswdCommandTest;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.Test;

/**
 * Hub for {@link AccountAttribute} tests.
 * 
 * Password related account attributes are tested in {@link PasswdCommandTest}, these attributes are:
 * <ul>
 * <li>{@link AccountAttribute#LOCK}</li>
 * <li>{@link AccountAttribute#MAX}</li>
 * <li>{@link AccountAttribute#MIN}</li>
 * <li>{@link AccountAttribute#WARN}</li>
 * </ul>
 * 
 * The role-based access control related attributes are tested in {@link RBACAttributeTests}, these attributes are:
 * <ul>
 * <li>{@link AccountAttribute#ROLES}</li>
 * <li>{@link AccountAttribute#AUTHORIZATION}</li>
 * <li>{@link AccountAttribute#PROFILE}</li>
 * </ul>
 * 
 * @author David Adam
 *
 */
public class AccountAttributeTest extends SolarisTestBase {
    private static final Log log = Log.getLog(AccountAttributeTest.class);

    @Test
    public void testDir() {
        genericTest(AccountAttribute.DIR, CollectionUtil.newList("/home/hercules"), CollectionUtil.newList("/"), "hercules");
    }
    
    @Test
    public void testComment() {
        genericTest(AccountAttribute.COMMENT, CollectionUtil.newList("myComment"), CollectionUtil.newList("my comment 2"), "comentus");
    }
    
    @Test
    public void testShell() {
        genericTest(AccountAttribute.SHELL, CollectionUtil.newList("/bin/ksh"), CollectionUtil.newList("/bin/csh"), "shellusr");
    }
    
    @Test
    public void testGroup() {
        genericTest(AccountAttribute.GROUP, CollectionUtil.newList("root"), CollectionUtil.newList(getGroupName()), "cmark");
    }
    
    @Test
    public void testSecondaryGroup() {
        genericTest(AccountAttribute.SECONDARY_GROUP, CollectionUtil.newList("root"), CollectionUtil.newList("root", getGroupName()), "cmark");
    }
    
    private static final Equalable dateComparator = new Equalable() {
        // compare two strings, but ignore the '/' delimiters of the date. For example:
        // equals("1/1/2010", "112010") == true
        // equals("1/2/2010", "112010") == false
        public boolean equals(List<? extends Object> o1, List<? extends Object> o2) {
            Assert.assertTrue(o1.size() == 1 && o2.size() == 1);
            String first = o1.get(0).toString();
            String second = o2.get(0).toString();
            first = reformat(first);
            second = reformat(second);
            return first.equals(second);
        }

        private String reformat(String dateString) {
            dateString = dateString.replaceAll("/", "").trim();
            if (dateString.startsWith("0") && dateString.length() > 1) {
                dateString = dateString.substring(1);
            }
            return dateString;
        }
    };
    
    @Test
    public void testInactive() {
        genericTest(AccountAttribute.INACTIVE, CollectionUtil.newList(13), CollectionUtil.newList(3), "cmark");
    }
    
    /**
     * Inactive attribute must be some future date, positive test.
     */
    @Test 
    public void testExpire() {
        // fetch the current date, and generate two dates in the future.
        String currentDate = getConnection().executeCommand("date");
        // assuming that last 4-digit number of the output is the date.
        Matcher m = Pattern.compile("\\d\\d\\d\\d").matcher(currentDate);
        int thisYear = -1;
        if (m.find()) {
            thisYear = Integer.valueOf(currentDate.substring(m.start(), m.end()));
        } else {
            Assert.fail("wrong date received, no 4-digit year present: " + currentDate);
        }
        
        String createDate = formatTestDate(thisYear + 3);
        String updateDate = formatTestDate(thisYear + 4);
        
        genericTest(AccountAttribute.EXPIRE, CollectionUtil.newList(createDate), CollectionUtil.newList(updateDate), "cmark", dateComparator);
    }
    
    /**
     * create a first January date for given year in Unix format, acceptable for
     * {@link NativeAttribute#USER_INACTIVE} tests.
     */
    private String formatTestDate(int yearInt) {
        String year = Integer.valueOf(yearInt).toString();
        // user last 2 digits of the year for the date.
        String shortYear = year.substring(year.length() - 2);
        
        return String.format("01/02/%s", shortYear);
    }

    /**
     * Negative test: for inactive any past date should result in failure.
     */
    @Test
    public void testExpireNegative() {
        try {
            genericTest(AccountAttribute.EXPIRE, CollectionUtil.newList("01/01/84"/* a past date */), CollectionUtil.newList("01/01/80"), "cmark", dateComparator);
            Assert.fail("past date should fail");
        } catch (ConnectorException ex) {
            // OK
        }
    }

    /**
     * check behaviour of {@link AccountAttribute#PASSWD_FORCE_CHANGE}
     * attribute.
     * 
     * If it is true, the user should change her password on the next login,
     * thus the "new password:" prompt signalizes this fact.
     */
    @Test
    public void testResetPassword() {
        if (getConnection().isNis()) {
            // Workaround: skipping. TODO Solaris NIS scripts in connector doesn't support forcing password change on Solaris NIS.
            log.info("skipping test 'testUserDeletion' for Solaris NIS configuration.");
            return;
        }
        final Pair<String, String> credentials = createResetPasswordUser(true);
        final String username = credentials.first;
        final String password = credentials.second;
        try {
            // check if user exists
            String loginsCmd = (!getConnection().isNis()) ? "logins -oxma -l " + username : "ypmatch \"" + username + "\" passwd";
            String out = getConnection().executeCommand(loginsCmd);
            Assert.assertTrue("user " + username + " is missing, buffer: <" + out + ">", out.contains(username));

            try {
                getFacade().authenticate(ObjectClass.ACCOUNT, username, new GuardedString(password.toCharArray()), null);
                Assert.fail("expected to wait for 'new password:' prompt failed.");
            } catch (ConnectorException ex) {
                if (!ex.getMessage().contains("New Password:")) {
                    Assert.fail("expected to wait for 'new password:' prompt failed with exception: " + ex.getMessage());
                } else {
                    log.ok("test testResetPassword passed");
                }
            }
        } finally {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        }
    }
        
    /** returns the username and password used for creation. */
    private Pair<String, String> createResetPasswordUser(boolean isForceChange) {
        final String username = "bugsBunny";
        final String password = "bugsPasswd";
        Set<Attribute> attrs = CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), 
                AttributeBuilder.buildPassword(password.toCharArray()),
                AttributeBuilder.build(AccountAttribute.PASSWD_FORCE_CHANGE.getName(), isForceChange));
        // cleanup the user if it's there from previous runs
        try {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        } catch (Exception ex) {
            // OK
        }
        
        getFacade().create(ObjectClass.ACCOUNT, attrs, null);
        enableTrustedLogin(username);
        return new Pair<String, String>(username, password);
    }

    @Test
    public void testTimeLastLogin() {
        if (getConnection().isNis()) {
            log.info("skipping test 'testTimeLastLogin' for NIS configuration, as it doesn't support attribute: " + AccountAttribute.TIME_LAST_LOGIN.getName());
            return;
        }
        String username = "connuser";
        String password = "blueray1";

        getFacade().create(ObjectClass.ACCOUNT,
                CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), AttributeBuilder.buildPassword(password.toCharArray())), null);
        try{
            // this involves doing 'login'
            getFacade().authenticate(ObjectClass.ACCOUNT, username, new GuardedString(password.toCharArray()), null);
            // get the date
            String out = getConnection().executeCommand("date");
            Assert.assertTrue(StringUtil.isNotBlank(out));
            String month = out.split(" ")[1];

            // check that last attribute contains the month of last login.
            ToListResultsHandler handler = new ToListResultsHandler();
            getFacade().search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler,
                    new OperationOptionsBuilder().setAttributesToGet(AccountAttribute.TIME_LAST_LOGIN.getName()).build());
            Assert.assertTrue(handler.getObjects().size() >= 1);
            ConnectorObject co = handler.getObjects().get(0);
            Attribute lastAttr = co.getAttributeByName(AccountAttribute.TIME_LAST_LOGIN.getName());
            String lastValue = AttributeUtil.getStringValue(lastAttr);
            String msg = String.format("expected to found the current login's month '%s' in the value of %s attribute command '%s', but it is missing.",
                    AccountAttribute.TIME_LAST_LOGIN.getName(), month, lastValue);
            Assert.assertTrue(msg, StringUtil.isNotBlank(lastValue) && lastValue.contains(month));
        } finally {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        }
    }
    
    @Override
    public boolean createGroup() {
        return true;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
    
    private <E> void genericTest(AccountAttribute attr, List<E> createValue, List<E> updateValue, String username) {
        genericTest(attr, createValue, updateValue, username, new Equalable() {
            public boolean equals(List<? extends Object> o1, List<? extends Object> o2) {
                return o1.equals(o2);
            }
        });
    }
    
    private <E> void genericTest(AccountAttribute attr, List<E> createValue, List<E> updateValue, String username, Equalable eq) {
        // the account should be brand new
        ToListResultsHandler handler = new ToListResultsHandler();
        getFacade().search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler, new OperationOptionsBuilder().setAttributesToGet(CollectionUtil.newSet(Name.NAME)).build());
        if (handler.getObjects().size() >= 1) {
            throw new RuntimeException("Please provide a brand new accountname, account '" + username + "' already exits");
        }

        // create a new account with create value
        try {
            // create can throw exceptions even because the password aging is
            // disabled, but even than we need to do a cleanup of the created
            // account.
            getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(
                AttributeBuilder.build(Name.NAME, username), 
                AttributeBuilder.build(attr.getName(), createValue)), null);
        
        
            // check if create value was set
            handler = new ToListResultsHandler();
            getFacade().search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler, new OperationOptionsBuilder().setAttributesToGet(attr.getName()).build());
            Assert.assertTrue(handler.getObjects().size() > 0);
            Assert.assertTrue(eq.equals(createValue, handler.getObjects().get(0).getAttributeByName(attr.getName()).getValue()));
            
            // update the value
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(attr.getName(), updateValue)), null);
            // check if update value was set
            handler = new ToListResultsHandler();
            getFacade().search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler, new OperationOptionsBuilder().setAttributesToGet(attr.getName()).build());
            Assert.assertTrue(handler.getObjects().size() > 0);
            Assert.assertTrue(eq.equals(updateValue, handler.getObjects().get(0).getAttributeByName(attr.getName()).getValue()));
        } finally {
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
            } catch (Exception ex) {
                // OK
            }
        }
    }
    
    private interface Equalable {
        public boolean equals(List<? extends Object> o1, List<? extends Object> o2);
    }
}
