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

import junit.framework.Assert;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.Test;


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
    
    private <E> void genericTest(AccountAttribute attr, List<E> createValue, List<E> updateValue, String username) {
        getFacade().create(ObjectClass.ACCOUNT, CollectionUtil.newSet(
                AttributeBuilder.build(Name.NAME, username), 
                AttributeBuilder.build(attr.getName(), createValue)), null);
        try {
        
            // check if create value was set
            ToListResultsHandler handler = new ToListResultsHandler();
            getFacade().search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler, new OperationOptionsBuilder().setAttributesToGet(attr.getName()).build());
            Assert.assertTrue(handler.getObjects().size() > 0);
            Assert.assertEquals(createValue, handler.getObjects().get(0).getAttributeByName(attr.getName()).getValue());
            
            // update the value
            getFacade().update(ObjectClass.ACCOUNT, new Uid(username), CollectionUtil.newSet(AttributeBuilder.build(attr.getName(), updateValue)), null);
            // check if update value was set
            handler = new ToListResultsHandler();
            getFacade().search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, username)), handler, new OperationOptionsBuilder().setAttributesToGet(attr.getName()).build());
            Assert.assertTrue(handler.getObjects().size() > 0);
            Assert.assertEquals(updateValue, handler.getObjects().get(0).getAttributeByName(attr.getName()).getValue());
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
    
}
