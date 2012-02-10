/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.openicf.salesforce.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openicf.salesforce.SalesforceConnector;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ForceAttributeUtilsTest {
    @Test
    public void testParseDescribe() throws Exception {
        Map<String, Object> testable = ForceAttributeUtilsTest.openTestable("/services/data/v23.0/sobjects/User/describe/GET.json");
        SchemaBuilder schemaBuilder = new SchemaBuilder(SalesforceConnector.class);
        ForceAttributeUtils.parseDescribe(testable, schemaBuilder);
        Schema schema = schemaBuilder.build();
        Assert.assertNotNull(schema.findObjectClassInfo("__ACCOUNT__"));
    }

    public static Map<String, Object> openTestable(String reference) throws Exception {
        Assert.assertNotNull(reference);
        InputStream inputStream = null;
        Map<String, Object> result = null;
        try {
            inputStream = ForceAttributeUtils.class.getResourceAsStream(reference);
            Assert.assertNotNull(inputStream, "Can not open InputStream of " + reference);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.readValue(inputStream, Map.class);
        } finally {
            if (null != inputStream) {
                inputStream.close();
            }
        }
        Assert.assertNotNull(result, "Can not parse to Map " + reference);
        return result;
    }
}
