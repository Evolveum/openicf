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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Set;

import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 *
 * @author petr
 * @since 1.0
 */
@Test(groups = { "integration" })
public class OracleERPOperationSchemaTests extends OracleERPTestsBase {

    /**
     * Test method for {@link OracleERPConnector#schema()}.
     */
    @Test
    public void testSchema() {
        Schema schema = getFacade(getConfiguration(CONFIG_SYSADM)).schema();
        // Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(9, objectInfos.size());
    }
}
