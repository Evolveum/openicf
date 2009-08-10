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

import static org.identityconnectors.common.StringUtil.isBlank;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class OracleERPConfigurationTests extends OracleERPTestsBase { 

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigSysadm() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_SYSADM);
        assertNotNull("null config", config);
        
        assertNotNull("null driver", config.getDriver());        
        if(isBlank(config.getUrl())) {
            assertNotNull("null host", config.getHost());
            assertNotNull("null user", config.getUser());
            assertNotNull("null port", config.getPort());
        }
        assertNotNull("null password", config.getPassword());
        assertNotNull("null account includede", config.getAccountsIncluded());
        assertFalse("isActiveAccountsOnl", config.isActiveAccountsOnly());
        assertNotNull("getAuditResponsibility", config.getAuditResponsibility());
        assertTrue("isManageSecuringAttrs", config.isManageSecuringAttrs());
        assertFalse("isNoSchemaId", config.isNoSchemaId());
        assertTrue("isReturnSobOrgAttrs", config.isReturnSobOrgAttrs());
        assertNotNull("null getUserActions", config.getUserAfterActions());        
        assertNotNull("null getConnectionUrl", config.getConnectionUrl());
        
        config.validate();
    }    
    
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigTst() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_TST);
        assertNotNull("null config", config);
        
        assertNotNull("null driver", config.getDriver());        
        if(isBlank(config.getUrl())) {
            assertNotNull("null host", config.getHost());
            assertNotNull("null user", config.getUser());
            assertNotNull("null port", config.getPort());
        }
        assertNotNull("null password", config.getPassword());
        assertNotNull("null account includede", config.getAccountsIncluded());
        assertTrue("isActiveAccountsOnl", config.isActiveAccountsOnly());
        assertNotNull("getAuditResponsibility", config.getAuditResponsibility());
        assertFalse("isManageSecuringAttrs", config.isManageSecuringAttrs());
        assertFalse("isNoSchemaId", config.isNoSchemaId());
        assertFalse("isReturnSobOrgAttrs", config.isReturnSobOrgAttrs());
        assertNotNull("null getUserActions", config.getUserAfterActions());        
        assertNotNull("null getConnectionUrl", config.getConnectionUrl());
        
        config.validate();
    }      


    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigUser() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_USER);
        assertNotNull("null config", config);
        
        assertNotNull("null driver", config.getDriver());        
        if(isBlank(config.getUrl())) {
            assertNotNull("null host", config.getHost());
            assertNotNull("null user", config.getUser());
            assertNotNull("null port", config.getPort());
        }
        assertNotNull("null password", config.getPassword());
        assertNotNull("null account includede", config.getAccountsIncluded());
        assertFalse("isActiveAccountsOnl", config.isActiveAccountsOnly());
        assertNotNull("getAuditResponsibility", config.getAuditResponsibility());
        assertTrue("isManageSecuringAttrs", config.isManageSecuringAttrs());
        assertFalse("isNoSchemaId", config.isNoSchemaId());
        assertFalse("isReturnSobOrgAttrs", config.isReturnSobOrgAttrs());
        assertNotNull("null getUserActions", config.getUserAfterActions());        
        assertNotNull("null getConnectionUrl", config.getConnectionUrl());
        
        config.validate();
    }          
 
    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testValidateSysConfiguration() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_TST);
        assertNotNull(config);
        config.validate();
    }
}
