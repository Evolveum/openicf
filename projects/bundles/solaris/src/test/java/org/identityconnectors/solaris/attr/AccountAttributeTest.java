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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.solaris.operation.PasswdCommandTest;
import org.identityconnectors.solaris.test.SolarisTestBase;
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
 * {@link AccountAttribute
 * @author David Adam
 *
 */
public class AccountAttributeTest extends SolarisTestBase {

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
    
    @Override
    public boolean createGroup() {
        return true;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
    
}
