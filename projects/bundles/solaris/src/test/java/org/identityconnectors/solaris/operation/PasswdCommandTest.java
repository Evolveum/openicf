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
package org.identityconnectors.solaris.operation;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Set of password attribute related tests. These attributes are:
 * {@link NativeAttribute#MAX_DAYS_BETWEEN_CHNG},
 * {@link NativeAttribute#MAX_DAYS_BETWEEN_CHNG}, {@link NativeAttribute#LOCK},
 * {@link NativeAttribute#DAYS_BEFORE_TO_WARN}.
 * 
 * Prerequisite of this test is to have password aging enabled in /etc/default/passwd:
 * MAXWEEKS=2
 * MINWEEKS=1
 * HISTORY=10
 * {@link http://blogs.sun.com/gbrunett/entry/solaris_10_password_history}
 * 
 * @author David Adam
 */
public class PasswdCommandTest extends SolarisTestBase {
    private static Log log = Log.getLog(PasswdCommandTest.class);
    
    @Test @Ignore // FIXME need further settings on the resource
    public void testMinDays() {
        if (getConnection().isNis()) {
            log.ok("skipping testMinDays for NIS accounts.");
            return;
        }
        genericTest(AccountAttribute.MIN, CollectionUtil.newList(1), CollectionUtil.newList(2), "batman");
    }
    
    @Test
    public void testMaxDays() {
        if (getConnection().isNis()) {
            log.ok("skipping testMaxDays for NIS accounts.");
            return;
        }
        genericTest(AccountAttribute.MAX, CollectionUtil.newList(2), CollectionUtil.newList(1), "batman");
    }

    /**
     * Test for {@link NativeAttribute#LOCK}.
     * 
     * If an account is locked, authentication should fail.
     */
    @Test
    public void testLock() {
        String username = getUsername();
        enableTrustedLogin(username);
        GuardedString password = new GuardedString(SAMPLE_PASSWD.toCharArray());
        
        // authentication involves login, so try to authenticate
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, username, password, null);
        } catch (Exception ex) {
            Assert.fail("failed to authenticate freshly created user: " + username);
        }
        
        // lock the account, then authenticate should fail
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.LOCK.getName(), Boolean.TRUE)), null);
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, username, password, null);
            Assert.fail("Locked account should not able to login.");
        } catch (Exception ex) {
            // OK
        }
    }
    
    /**
     * Test for {@link NativeAttribute#LOCK}
     * 
     * If an account is unlocked authentication should succeed. {@see PasswdCommandTest#testLock()}
     */
    @Test @Ignore // FIXME need further settings on the resource
    public void testUnLock() {
        String username = "connuser";
        GuardedString passwd = new GuardedString("foo123".toCharArray());
        // create a locked account, login should fail
        try {
            getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, username), AttributeBuilder.buildPassword(passwd), AttributeBuilder.build(AccountAttribute.LOCK.getName(), Boolean.TRUE)), null);
        
            enableTrustedLogin(username);
            try {
                getFacade().authenticate(ObjectClass.ACCOUNT, username, passwd, null);
                Assert.fail("expecting to fail when we attempt to authenticate a locked account.");
            } catch (Exception ex) {
                // OK
            }
            // unlock the account, authenticate should succeed.
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.LOCK.getName(), Boolean.FALSE)), null);
            try {
                getFacade().authenticate(ObjectClass.ACCOUNT, username, passwd, null);
            } catch (Exception ex) {
                Assert.fail("authentication of an unlocked account should pass, but received a failure.");
            }
            
        } finally {
            getFacade().delete(ObjectClass.ACCOUNT, new Uid(username), null);
        }
    }
    
    @Test
    public void testFailLock() {
        try {
            getFacade().update(ObjectClass.ACCOUNT, new Uid(getUsername()), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.LOCK.getName())), null);
            Assert.fail("passing null option to Lock should cause failure. It must have a boolean value.");
        } catch (Exception ex) {
            // OK
        }
        try {
            getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(AttributeBuilder.build(Name.NAME, "fooconn"), AttributeBuilder.buildPassword("foo134".toCharArray()), AttributeBuilder.build(AccountAttribute.LOCK.getName())), null);
            Assert.fail("passing null option to Lock should cause failure. It must have a boolean value.");
        } catch (Exception ex) {
            // OK
        } finally {
            try {
                getFacade().delete(ObjectClass.ACCOUNT, new Uid("foo134"), null);
            } catch (Exception ex) {
                // OK
            }
        }
    }
    
    @Test @Ignore // FIXME need further settings on the resource
    public void testDaysBeforeWarn() {
        if (getConnection().isNis()) {
            log.ok("skipping testDaysBeforeWarn for NIS accounts.");
            return;
        }
        genericTest(AccountAttribute.WARN, CollectionUtil.newList(2), CollectionUtil.newList(4), "batman");
    }

    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        // TODO Auto-generated method stub
        return 1;
    }
    
    private String getUsername() {
        return getUsername(0);
    }
}
