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
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
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
 * @author David Adam
 */
public class PasswdCommandTest extends SolarisTestBase {
    @Test @Ignore
    public void testMinDays() {
        // TODO
    }
    
    @Test @Ignore
    public void testMaxDays() {
        // TODO
    }

    /**
     * Test for {@link NativeAttribute#LOCK}.
     * 
     * The connector supports only one-way lock of an account, once it is locked
     * it cannot be unlocked. 
     * <br>
     * The resource itself supports unlocking but this would be a change w.r.t.
     * what the adapter did.
     */
    @Test
    public void testLock() {
        String username = getUsername();
        GuardedString password = new GuardedString(SAMPLE_PASSWD.toCharArray());
        
        // authentication involves login, so try to authenticate
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, username, password, null);
        } catch (Exception ex) {
            Assert.fail("failed to authenticate freshly created user: " + username);
        }
        
        // lock the account, then authenticate should fail
        getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(AccountAttribute.LOCK.getName())), null);
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, username, password, null);
            Assert.fail("Locked account should not able to login.");
        } catch (Exception ex) {
            // OK
        }
    }
    
    @Test @Ignore
    public void testDaysBeforeWarn() {
        // TODO
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
