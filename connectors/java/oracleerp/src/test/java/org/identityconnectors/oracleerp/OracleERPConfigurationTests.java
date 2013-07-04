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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.oracleerp;

import static org.identityconnectors.common.StringUtil.isBlank;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

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
        AssertJUnit.assertNotNull("null config", config);

        AssertJUnit.assertNotNull("null driver", config.getDriver());
        if (isBlank(config.getUrl())) {
            AssertJUnit.assertNotNull("null host", config.getHost());
            AssertJUnit.assertNotNull("null user", config.getUser());
            AssertJUnit.assertNotNull("null port", config.getPort());
        }
        AssertJUnit.assertNotNull("null password", config.getPassword());
        AssertJUnit.assertNotNull("null account includede", config.getAccountsIncluded());
        AssertJUnit.assertFalse("isActiveAccountsOnl", config.isActiveAccountsOnly());
        AssertJUnit.assertNotNull("getAuditResponsibility", config.getAuditResponsibility());
        AssertJUnit.assertTrue("isManageSecuringAttrs", config.isManageSecuringAttrs());
        AssertJUnit.assertFalse("isNoSchemaId", config.isNoSchemaId());
        AssertJUnit.assertTrue("isReturnSobOrgAttrs", config.isReturnSobOrgAttrs());
        AssertJUnit.assertNull("getUserAfterActionScript", config.getUserAfterActionScript());
        AssertJUnit.assertNotNull("null getConnectionUrl", config.getConnectionUrl());
        AssertJUnit.assertNotNull("null clientEncryptionLevel", config.getClientEncryptionLevel());
        AssertJUnit.assertNotNull("null clientEncryptionType", config.getClientEncryptionType());

        config.validate();
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigTst() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_TST);
        AssertJUnit.assertNotNull("null config", config);

        AssertJUnit.assertNotNull("null driver", config.getDriver());
        if (isBlank(config.getUrl())) {
            AssertJUnit.assertNotNull("null host", config.getHost());
            AssertJUnit.assertNotNull("null user", config.getUser());
            AssertJUnit.assertNotNull("null port", config.getPort());
        }
        AssertJUnit.assertNotNull("null password", config.getPassword());
        AssertJUnit.assertNotNull("null account includede", config.getAccountsIncluded());
        AssertJUnit.assertTrue("isActiveAccountsOnl", config.isActiveAccountsOnly());
        AssertJUnit.assertNotNull("getAuditResponsibility", config.getAuditResponsibility());
        AssertJUnit.assertFalse("isManageSecuringAttrs", config.isManageSecuringAttrs());
        AssertJUnit.assertFalse("isNoSchemaId", config.isNoSchemaId());
        AssertJUnit.assertFalse("isReturnSobOrgAttrs", config.isReturnSobOrgAttrs());
        AssertJUnit.assertNull("getUserAfterActionScript", config.getUserAfterActionScript());
        AssertJUnit.assertNotNull("null getConnectionUrl", config.getConnectionUrl());

        config.validate();
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigUser() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_USER);
        AssertJUnit.assertNotNull("null config", config);

        AssertJUnit.assertNotNull("null driver", config.getDriver());
        if (isBlank(config.getUrl())) {
            AssertJUnit.assertNotNull("null host", config.getHost());
            AssertJUnit.assertNotNull("null user", config.getUser());
            AssertJUnit.assertNotNull("null port", config.getPort());
        }
        AssertJUnit.assertNotNull("null password", config.getPassword());
        AssertJUnit.assertNotNull("null account includede", config.getAccountsIncluded());
        AssertJUnit.assertFalse("isActiveAccountsOnl", config.isActiveAccountsOnly());
        AssertJUnit.assertNotNull("getAuditResponsibility", config.getAuditResponsibility());
        AssertJUnit.assertTrue("isManageSecuringAttrs", config.isManageSecuringAttrs());
        AssertJUnit.assertFalse("isNoSchemaId", config.isNoSchemaId());
        AssertJUnit.assertFalse("isReturnSobOrgAttrs", config.isReturnSobOrgAttrs());
        AssertJUnit.assertNull("getUserAfterActionScript", config.getUserAfterActionScript());
        AssertJUnit.assertNotNull("null getConnectionUrl", config.getConnectionUrl());

        config.validate();
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testConfigUserPasswordAttribute() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_USER);
        config.setPasswordAttribute("test");
        AssertJUnit.assertNotNull("null config", config);
        AssertJUnit.assertEquals("getPasswordAttribute", "test", config.getPasswordAttribute());
        config.validate();
    }

    /**
     * Test method for {@link OracleERPConfiguration#getConnectionUrl()}.
     */
    @Test
    public void testValidateSysConfiguration() {
        final OracleERPConfiguration config = getConfiguration(CONFIG_TST);
        AssertJUnit.assertNotNull(config);
        config.validate();
    }
}
