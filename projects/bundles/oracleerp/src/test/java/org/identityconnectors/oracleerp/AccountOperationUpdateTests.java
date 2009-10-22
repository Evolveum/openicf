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
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;



/**
 * Attempts to test the AccountOperationUpdate with the framework.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class AccountOperationUpdateTests extends OracleERPTestsBase { 


    /**
     * Test method .
     */
    @Test
    public void testUpdateWithoutName() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        final Set<Attribute> update = getAttributeSet(ACCOUNT_MODIFY_ATTRS);
        replaceNameByValue(update, uid.getUidValue());
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        // Date text representations are not the same, skiped due to extra test
        testAttrSet(update, returned, OperationalAttributes.PASSWORD_NAME, OWNER);
    }
    
    /**
     * Test method .
     */
    @Test
    public void testUpdateDeleteCreatedRespNameIssue() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByRandom(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        
        ConnectorObject co = results.get(0);
        Set<Attribute> returned = co.getAttributes();
                
        final Set<Attribute> update = getAttributeSet(ACCOUNT_ALL_ATTRS);
        replaceNameByValue(update, uid.getUidValue());
        
        //remove old directResponsibility
        final Attribute directResp = AttributeUtil.find(DIRECT_RESPS, update);
        update.remove(directResp);
        //add empty responsibility
        final Attribute emptyResp = AttributeBuilder.build(DIRECT_RESPS);
        update.add(emptyResp);
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        assertNotNull(uid);
        
        results = TestHelpers.searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        
        co = results.get(0);
        returned = co.getAttributes();
        
        //remove empty responsibility
        update.remove(emptyResp);
        final Attribute newResp = AttributeBuilder.build(DIRECT_RESPS, "Cash Forecasting||Cash Management||Standard||"+getCDS()+"||"+getCDS());
        //add end-dated responsibility, product of calling remove responsibility 
        update.add(newResp);
        testAttrSet(update, returned, OperationalAttributes.PASSWORD_NAME, OWNER, OperationalAttributes.PASSWORD_EXPIRED_NAME);
    }
    /**
     * Create string repre of the current date string
     * @return the date String
     */
    private String getCDS() {
        return new java.sql.Timestamp(System.currentTimeMillis()).toString().substring(0, 10);
    }
     
    /**
     * Test method .
     */
    @Test(expected=IllegalStateException.class)
    public void testUpdate() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        final Set<Attribute> update = getAttributeSet(ACCOUNT_MODIFY_ATTRS);
        // Name is generated to the new name
        replaceNameByRandom(update);
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
    }    
    
    
    /**
     * Test method .
     */
    @Test
    public void testUpdateDissable() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        final Set<Attribute> update = new HashSet<Attribute>();       
        
        
        //Dissable
        update.add(uid);
        update.add(AttributeBuilder.buildEnabled(false));
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        final Set<Attribute> enabledAttr = getAttributeSet(ACCOUNT_DISSABLED);
        testAttrSet(enabledAttr, returned);
    }
    
    /**
     * Test method .
     */
    @Test
    public void testUpdateEnable() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        final Set<Attribute> dissable = new HashSet<Attribute>();       
        dissable.add(uid);
        dissable.add(AttributeBuilder.buildEnabled(false));
        uid = c.update(ObjectClass.ACCOUNT, uid, dissable, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        final Set<Attribute> enable = new HashSet<Attribute>();       
        enable.add(uid);
        enable.add(AttributeBuilder.buildEnabled(true));
        uid = c.update(ObjectClass.ACCOUNT, uid, enable, null);
        assertNotNull(uid);
        
        List<ConnectorObject> results2 = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results2.size() == 1);
        
        final ConnectorObject co2 = results2.get(0);
        final Set<Attribute> returned2 = co2.getAttributes();
        System.out.println(returned2);
        
        final Set<Attribute> enabledAttr = getAttributeSet(ACCOUNT_ENABLED);
        testAttrSet(enabledAttr, returned2);        
    }    
}
