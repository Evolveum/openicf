/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
 */

package org.forgerock.openicf.connectors.box;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link BoxConnector} with the framework.
 *
 */
public class BoxConnectorTests {

    /**
     * Setup logging for the {@link BoxConnectorTests}.
     */
    private static final Log logger = Log.getLog(BoxConnectorTests.class);

    private ConnectorFacade facade;


    @Test(enabled = false)
    public void exampleTest1() {
        logger.info("Running Test 1...");
        getFacade().test();
    }

    @Test
    public void testSchema() {
        logger.info("Running Test 1...");
        getFacade().schema();
    }

    @Test(enabled = false)
    public void exampleTest2() {
        logger.info("Running Test 2...");
        Set<Attribute> createAttribute = new HashSet<Attribute>();
        createAttribute.add(new Name("Test User"));
        createAttribute.add(AttributeBuilder.build("login", "norelpy@forgerock.com"));

        Uid uid = getFacade().create(ObjectClass.ACCOUNT, createAttribute, null);
        Assert.assertNotNull(uid);

       // getFacade().delete(ObjectClass.ACCOUNT, uid, null);

        // Another example using TestHelpers
        // List<ConnectorObject> results =
        // TestHelpers.searchToList(theConnector, ObjectClass.GROUP, filter);
    }

    protected ConnectorFacade getFacade() {
        if (null == facade) {

            PropertyBag propertyBag = TestHelpers.getProperties(BoxConnector.class);

            APIConfiguration impl =
                    TestHelpers.createTestConfiguration(BoxConnector.class, propertyBag,
                            "configuration");
            impl.setProducerBufferSize(0);
            impl.getResultsHandlerConfiguration().setEnableAttributesToGetSearchResultsHandler(
                    false);
            impl.getResultsHandlerConfiguration().setEnableCaseInsensitiveFilter(false);
            impl.getResultsHandlerConfiguration().setEnableFilteredResultsHandler(false);
            impl.getResultsHandlerConfiguration().setEnableNormalizingResultsHandler(false);

            facade = ConnectorFacadeFactory.getInstance().newInstance(impl);
        }
        return facade;
    }

}
