/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openicf.connectors;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.openicf.connectors.RESTTestBase.createConnectorFacade;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fest.assertions.core.Condition;
import org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConnector;
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector;
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.impl.api.local.LocalConnectorFacadeImpl;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author Laszlo Hordos
 */
public class ScriptedRESTSampleTest {
    /**
     * Setup logging for the
     * {@link org.forgerock.openicf.connectors.ScriptedSQLConnectorTest}.
     */
    private static final Log logger = Log.getLog(ScriptedRESTSampleTest.class);

    protected static final String REST_TEST_NAME = "REST_SAMPLE";
    protected static final String CREST_TEST_NAME = "CREST_SAMPLE";

    private ConnectorFacade facadeInstance;


    @BeforeClass
    public void setUp() throws Exception {
        String username =
                TestHelpers.getProperties(ScriptedConnectorBase.class, getTestName())
                        .getStringProperty("configuration.username");
        if ("__configureme__".equals(username)) {
            throw new SkipException("REST Sample tests are skipped. Create private configuration!");
        }
    }

    @Test
    public void validate() throws Exception {
        final ConnectorFacade facade = getFacade();
        facade.validate();
    }

    @Test
    public void testTest() throws Exception {
        final ConnectorFacade facade = getFacade();
        facade.test();
    }

    @Test
    public void testSchema() throws Exception {
        final ConnectorFacade facade = getFacade();
        Schema schema = facade.schema();
        Assert.assertEquals(schema.getObjectClassInfo().size(), 2);
    }

    @Test
    public void testAuthenticate() throws Exception {
        if (CREST_TEST_NAME.equals(getTestName())){
            throw new SkipException("CREST Sample does not implement this operation");
        }
        final ConnectorFacade facade = getFacade();
        Set<Attribute> createAttributes = createUserAttributes(1, "Bob", "Fleming");
        createAttributes.add(AttributeBuilder.build("password", "Passw0rd"));

        Uid uid = facade.create(ObjectClass.ACCOUNT, createAttributes, null);
        try {
            facade.authenticate(ObjectClass.ACCOUNT, "fleming", new GuardedString("Passw0rd"
                    .toCharArray()), null);
        } finally {
            if (null != uid) {
                facade.delete(ObjectClass.ACCOUNT, uid, null);
            }
        }
    }

    @Test
    public void testCreateUpdateDeleteUser() throws Exception {
        final ConnectorFacade facade = getFacade();
        Set<Attribute> expectedAttributes = createUserAttributes(1, "John", "Doe");

        Set<Attribute> createAttributes = new HashSet<Attribute>(expectedAttributes);
        createAttributes.add(AttributeBuilder.build("password", "Passw0rd"));

        Uid uid = facade.create(ObjectClass.ACCOUNT, createAttributes, null);
        ConnectorObject co = facade.getObject(ObjectClass.ACCOUNT, uid, null);

        assertThat(AttributeUtil.filterUid(co.getAttributes())).containsAll(expectedAttributes);

        Filter filter =
                FilterBuilder.and(FilterBuilder.startsWith(AttributeBuilder.build(
                        "telephoneNumber", "555")), FilterBuilder.equalTo(AttributeBuilder.build(
                        "familyName", "Doe")));
        assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, filter)).hasSize(1);

        Set<Attribute> updateAttributes = new HashSet<Attribute>();
        for (Attribute attr : expectedAttributes) {
            if (attr.is("givenName")) {
                updateAttributes.add(AttributeBuilder.build("givenName", "Johny"));
            } else {
                updateAttributes.add(attr);
            }
        }

        uid = facade.update(ObjectClass.ACCOUNT, co.getUid(), updateAttributes, null);

        co = facade.getObject(ObjectClass.ACCOUNT, uid, null);

        assertThat(AttributeUtil.filterUid(co.getAttributes())).containsAll(updateAttributes);

        //Test query-all
        OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setAttributesToGet(Uid.NAME);
        assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, null, builder.build()))
                .isNotEmpty();
        assertThat(TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, null, null)).isNotEmpty();


        facade.delete(ObjectClass.ACCOUNT, uid, null);
        Assert.assertNull(facade.getObject(ObjectClass.ACCOUNT, uid, null));
    }

    @Test(dependsOnMethods = { "testCreateUpdateDeleteUser" })
    public void testCreateUpdateDeleteGroup() throws Exception {
        final ConnectorFacade facade = getFacade();
        Set<Attribute> createAttributes = createGroupAttributes("Sample_Test_Group");

        Uid uid = facade.create(ObjectClass.GROUP, createAttributes, null);
        ConnectorObject co = facade.getObject(ObjectClass.GROUP, uid, null);

        assertThat(
                TestHelpers.searchToList(facade, ObjectClass.GROUP, FilterBuilder
                        .equalTo(AttributeBuilder.build("displayName", "Sample_Test_Group"))))
                .hasSize(1);

        assertThat(AttributeUtil.filterUid(co.getAttributes())).containsAll(createAttributes);

        Set<Attribute> expectedAttributes = createUserAttributes(1, "Group", "member");

        expectedAttributes.add(AttributeBuilder.build("password", "Passw0rd"));

        Uid memberUid = facade.create(ObjectClass.ACCOUNT, expectedAttributes, null);

        Set<Attribute> updateAttributes = new HashSet<Attribute>();
        for (Attribute attr : createAttributes) {
            if (attr.is("members")) {
                Map<String, Object> member = new HashMap<String, Object>(1);
                member.put("_id", "member");
                updateAttributes.add(AttributeBuilder.build("members", member));
            } else {
                updateAttributes.add(attr);
            }
        }

        uid = facade.update(ObjectClass.GROUP, co.getUid(), updateAttributes, null);

        co = facade.getObject(ObjectClass.GROUP, uid, null);

        assertThat(AttributeUtil.filterUid(co.getAttributes())).containsAll(updateAttributes);

        facade.delete(ObjectClass.GROUP, uid, null);
        Assert.assertNull(facade.getObject(ObjectClass.GROUP, uid, null));
        facade.delete(ObjectClass.ACCOUNT, memberUid, null);
    }

    @Test(dependsOnMethods = { "testCreateUpdateDeleteGroup" })
    public void testSync() throws Exception {
        final ConnectorFacade facade = getFacade();
        SyncToken userToken = facade.getLatestSyncToken(ObjectClass.ACCOUNT);
        assertThat(Integer.parseInt((String) userToken.getValue())).isGreaterThan(3);
        SyncToken groupToken = facade.getLatestSyncToken(ObjectClass.GROUP);
        assertThat(Integer.parseInt((String) groupToken.getValue())).isGreaterThan(3);

        Set<Attribute> createAttributes = createUserAttributes(1, "John", "Doe");
        Uid uid = facade.create(ObjectClass.ACCOUNT, createAttributes, null);
        
         final List<SyncDelta> deltas = new LinkedList<SyncDelta>();
        facade.sync(ObjectClass.ACCOUNT, new SyncToken(0), new SyncResultsHandler() {
            public boolean handle(SyncDelta delta) {
                deltas.add(delta);
                return true;
            }
        }, null);

        assertThat(deltas).areAtLeast(1, new Condition<SyncDelta>() {
            public boolean matches(SyncDelta value) {
                return value.getDeltaType().equals(SyncDeltaType.CREATE);
            }
        }).areAtLeast(1, new Condition<SyncDelta>() {
            public boolean matches(SyncDelta value) {
                return value.getDeltaType().equals(SyncDeltaType.UPDATE);
            }
        }).areAtLeast(1, new Condition<SyncDelta>() {
            public boolean matches(SyncDelta value) {
                return value.getDeltaType().equals(SyncDeltaType.DELETE);
            }
        });
        final SyncResultsHandler handler = mock(SyncResultsHandler.class);
        when(handler.handle(any(SyncDelta.class))).thenReturn(true);

        facade.sync(ObjectClass.GROUP, new SyncToken(0), handler, null);
        verify(handler, atLeast(3)).handle(any(SyncDelta.class));
        facade.delete(ObjectClass.ACCOUNT, uid, null);
    }

    private Set<Attribute> createUserAttributes(int index, String firstName, String lastName) {
        Set<Attribute> createAttributes = new HashSet<Attribute>();
        createAttributes.add(new Name(lastName.toLowerCase()));

        createAttributes.add(AttributeBuilder.build("givenName", firstName));
        createAttributes.add(AttributeBuilder.build("familyName", lastName));
        createAttributes.add(AttributeBuilder.build("displayName", firstName + " " + lastName));
        createAttributes.add(AttributeBuilder.build("emailAddress", lastName.toLowerCase()
                + "@example.com"));

        createAttributes.add(AttributeBuilder.build("telephoneNumber", "555-1234-" + index));
        return createAttributes;
    }

    private Set<Attribute> createGroupAttributes(String name) {
        Set<Attribute> createAttributes = new HashSet<Attribute>();
        createAttributes.add(new Name(name.toLowerCase()));
        createAttributes.add(AttributeBuilder.build("displayName", name.toLowerCase()));
        return createAttributes;
    }

    protected String getTestName(){
        return REST_TEST_NAME;
    }
    
    protected ConnectorFacade getFacade() {
        return getFacade(getTestName());
    } 
    
    protected ConnectorFacade getFacade(String environment) {
        if (null == facadeInstance) {
            if (CREST_TEST_NAME.equals(environment)) {
                facadeInstance = createConnectorFacade(ScriptedCRESTConnector.class, environment);
            } else {
                facadeInstance = createConnectorFacade(ScriptedRESTConnector.class, environment);
            }
        }
        return facadeInstance;
    }

    @AfterClass
    public synchronized void afterClass() {
        ((LocalConnectorFacadeImpl) facadeInstance).dispose();
        facadeInstance = null;
    }
}
