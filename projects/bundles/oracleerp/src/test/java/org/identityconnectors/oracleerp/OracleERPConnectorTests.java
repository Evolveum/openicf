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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;




/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class OracleERPConnectorTests extends OracleERPTestsBase { 

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorSysadm() {
        OracleERPConnector conn = getConnector(CONFIG_SYSADM);
        assertNotNull("null connector instance", conn);        
        conn.test();
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorTst() {
        OracleERPConnector conn = getConnector(CONFIG_TST);
        assertNotNull("null connector instance",conn);        
        conn.test();
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorUser() {
        OracleERPConnector conn = getConnector(CONFIG_USER);
        assertNotNull("null connector instance",conn);        
        conn.test();
    }
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testConnectorUserWrongAccountIncluded() {
        OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        cfg.setAccountsIncluded("WHERE REAL='BLAF'");
        OracleERPConnector conn = getConnector(cfg);
        assertNotNull("null connector instance",conn);        
        conn.test();
    }
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorUserNullAccountIncluded() {
        OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        cfg.setAccountsIncluded(null);
        OracleERPConnector conn = getConnector(cfg);
        assertNotNull("null connector instance",conn);        
        conn.test();
    }    
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorUserAccountIncludedOk() {
        OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        OracleERPConnector conn = getConnector(cfg);
        assertNotNull("null connector instance",conn);        
        conn.test();
    }    
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConnectorUserAccountIncludedOk2() {
        OracleERPConfiguration cfg = getConfiguration(CONFIG_SYSADM);
        cfg.setAccountsIncluded("USER_NAME like 'JTU%'");
        OracleERPConnector conn = getConnector(cfg);
        assertNotNull("null connector instance",conn);        
        conn.test();
    }
}
