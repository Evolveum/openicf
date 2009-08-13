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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.objects.*;
import org.junit.Test;




/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class AccountOperationAutenticateTests extends OracleERPTestsBase { 
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testAuthenticate() {
        final OracleERPConnector c = getConnector(CONFIG_TST); 
       
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        generateNameAttribute(attrs);
        final GuardedString password = AttributeUtil.getGuardedStringValue(AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs));
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        
        assertEquals(uid, c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null));               
    }
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test(expected=InvalidCredentialException.class)
    public void testAuthenticateDissabled() {
        final OracleERPConnector c = getConnector(CONFIG_TST); 
       
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        generateNameAttribute(attrs);
        final GuardedString password = AttributeUtil.getGuardedStringValue(AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs));
        Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        
        assertEquals(uid, c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null)); 

        //Dissable
        final Set<Attribute> update = new HashSet<Attribute>();       
        update.add(uid);
        update.add(AttributeBuilder.buildEnabled(false));
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        assertNotNull(uid);
        
        c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null); 
    }   
    
    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test(expected=InvalidPasswordException.class)
    public void testAuthenticateExpired() {
        final OracleERPConnector c = getConnector(CONFIG_TST); 
       
        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        generateNameAttribute(attrs);
        final GuardedString password = AttributeUtil.getGuardedStringValue(AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs));
        Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        assertNotNull(uid);
        
        assertEquals(uid, c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null)); 

        //Dissable
        final Set<Attribute> update = new HashSet<Attribute>();       
        update.add(uid);
        update.add(AttributeBuilder.buildPasswordExpired(true));
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        assertNotNull(uid);
        
        c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null); 
    }      
}
