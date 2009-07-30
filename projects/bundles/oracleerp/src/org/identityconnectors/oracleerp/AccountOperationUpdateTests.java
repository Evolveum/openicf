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

import static org.identityconnectors.oracleerp.OracleERPUtil.OWNER;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
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
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testUpdateWithoutName() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        generateNameAttribute(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        final Set<Attribute> update = getAttributeSet(ACCOUNT_MODIFY_ATTRS);
        replaceNameAttribute(update, uid.getUidValue());
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
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test(expected=IllegalStateException.class)
    public void testUpdate() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);
        
        final Set<Attribute> create = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        generateNameAttribute(create);
        Uid uid = c.create(ObjectClass.ACCOUNT, create, null);
        assertNotNull(uid);
        
        final Set<Attribute> update = getAttributeSet(ACCOUNT_MODIFY_ATTRS);
        // Name is generated to the new name
        generateNameAttribute(update);
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        /*
         * This should work, after right renaming the account
         * TODO implement renaming test, when rename account will be supported
        assertNotNull(uid);
        
        List<ConnectorObject> results = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid));
        assertTrue("expect 1 connector object", results.size() == 1);
        
        final ConnectorObject co = results.get(0);
        final Set<Attribute> returned = co.getAttributes();
        System.out.println(returned);
        
        // Date text representations are not the same, skiped due to extra test
        testAttrSet(update, returned, OperationalAttributes.PASSWORD_NAME, OWNER);
        */
    }    
}
