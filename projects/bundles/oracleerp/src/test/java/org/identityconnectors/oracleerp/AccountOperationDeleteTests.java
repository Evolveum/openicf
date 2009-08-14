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

import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;



/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 *
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class AccountOperationDeleteTests extends OracleERPTestsBase {


    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testDeleteActiveOnly() {
        final OracleERPConnector c = getConnector(CONFIG_TST);

        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(attrs);
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        List<ConnectorObject> r1 = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), null);
        assertTrue("expect 1 connector object", r1.size() == 1);

        c.delete(ObjectClass.ACCOUNT, uid, null);

        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
        addDefaultAttributesToGet(oob, getAttributeInfos(c.schema(), ObjectClass.ACCOUNT_NAME));
        List<ConnectorObject> r2 = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), oob.build());
        //it is deleted, when active_accounts_only .. set up in CONFIG_TST
        assertTrue("expect 0 connector object", r2.size() == 0);
    }
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testDelete() {
        final OracleERPConnector c = getConnector(CONFIG_SYSADM);

        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(attrs);
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        List<ConnectorObject> r1 = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), null);
        assertTrue("expect 1 connector object", r1.size() == 1);

        c.delete(ObjectClass.ACCOUNT, uid, null);

        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
        addDefaultAttributesToGet(oob, getAttributeInfos(c.schema(), ObjectClass.ACCOUNT_NAME));
        List<ConnectorObject> r2 = TestHelpers
        .searchToList(c, ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), oob.build());
        //it is deleted, when no active_accounts_only .. set up in CONFIG_SYSADM
        assertTrue("expect 1 connector object", r2.size() == 1);
        Set<Attribute> returned = r2.get(0).getAttributes();
        assertFalse("Should not be enabled", AttributeUtil.getBooleanValue(AttributeUtil.find(OperationalAttributes.ENABLE_NAME, returned)));
    }

 }
