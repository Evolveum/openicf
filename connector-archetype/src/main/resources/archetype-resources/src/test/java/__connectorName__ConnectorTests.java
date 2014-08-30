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
 * http://opensource.org/licenses/CDDL-1.0
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://opensource.org/licenses/CDDL-1.0
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
#if ($ALL_OPERATIONS == 'Y' || $ALL_OPERATIONS == 'y')
        #set( $all_operations_safe = true)
#end
#if ( $poolableConnector == 'Y' || $poolableConnector == 'y' )
    #set( $poolable_connector_safe = true)
#end
#if ( $attributeNormalizer == 'Y' || $attributeNormalizer == 'y')
    #set( $attribute_normalizer_safe = true)
#end
#if ( $all_operations_safe || $OP_AUTHENTICATE == 'Y' || $OP_AUTHENTICATE == 'y' )
    #set( $op_authenticate_safe = true)
#end
#if ( $all_operations_safe || $OP_CREATE == 'Y' || $OP_CREATE == 'y')
    #set( $op_create_safe = true)
#end
#if ( $all_operations_safe || $OP_DELETE == 'Y' || $OP_DELETE == 'y' )
    #set( $op_delete_safe = true)
#end
#if ( $all_operations_safe || $OP_RESOLVEUSERNAME == 'Y' || $OP_RESOLVEUSERNAME == 'y')
    #set( $op_resolveusername_safe = true)
#end
#if ( $all_operations_safe || $OP_SCHEMA == 'Y' || $OP_SCHEMA == 'y' )
    #set( $op_schema_safe = true)
#end
#if ( $all_operations_safe || $OP_SCRIPTONCONNECTOR == 'Y' || $OP_SCRIPTONCONNECTOR == 'y')
    #set( $op_scriptonconnector_safe = true)
#end
#if ( $all_operations_safe || $OP_SCRIPTONRESOURCE == 'Y' || $OP_SCRIPTONRESOURCE == 'y' )
    #set( $op_scriptonresource_safe = true)
#end
#if ( $all_operations_safe || $OP_SEARCH == 'Y' || $OP_SEARCH == 'y')
    #set( $op_search_safe = true)
#end
#if ( $all_operations_safe || $OP_SYNC == 'Y' || $OP_SYNC == 'y' )
    #set( $op_sync_safe = true)
#end
#if ( $all_operations_safe || $OP_TEST == 'Y' || $OP_TEST == 'y')
    #set( $op_test_safe = true)
#end
#if ( $all_operations_safe || $OP_UPDATEATTRIBUTEVALUES == 'Y' || $OP_UPDATEATTRIBUTEVALUES == 'y' )
    #set( $op_updateattributevalues_safe = true)
#end
#if ( $all_operations_safe || $OP_UPDATE == 'Y' || $OP_UPDATE == 'y')
    #set( $op_update_safe = true)
#end

package ${package};

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link ${connectorName}Connector} with the framework.
 *
 * @author ${symbol_dollar}author${symbol_dollar}
 * @version ${symbol_dollar}Revision${symbol_dollar} ${symbol_dollar}Date${symbol_dollar}
 */
public class ${connectorName}ConnectorTests {

    /**
    * Setup logging for the {@link ${connectorName}ConnectorTests}.
    */
    private static final Log logger = Log.getLog(${connectorName}ConnectorTests.class);

    private ConnectorFacade connectorFacade = null;

    /*
    * Example test properties.
    * See the Javadoc of the TestHelpers class for the location of the public and private configuration files.
    */
    private static final PropertyBag PROPERTIES = TestHelpers.getProperties(${connectorName}Connector.class);

    @BeforeClass
    public static void setUp() {
        //
        //other setup work to do before running tests
        //

        //Configuration config = new ${connectorName}Configuration();
        //Map<String, ? extends Object> configData = (Map<String, ? extends Object>) PROPERTIES.getProperty("configuration",Map.class)
        //TestHelpers.fillConfiguration(
    }

    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        //
    }

    @Test
    public void exampleTest1() {
        logger.info("Running Test 1...");
        //You can use TestHelpers to do some of the boilerplate work in running a search
        //TestHelpers.search(theConnector, ObjectClass.ACCOUNT, filter, handler, null);
    }

    @Test
    public void exampleTest2() {
        logger.info("Running Test 2...");
        //Another example using TestHelpers
        //List<ConnectorObject> results = TestHelpers.searchToList(theConnector, ObjectClass.GROUP, filter);
    }

#if ( $op_authenticate_safe )
    @Test
    public void authenticateTest() {
        logger.info("Running Authentication Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        Uid uid =
                facade.authenticate(ObjectClass.ACCOUNT, "username", new GuardedString("Passw0rd"
                        .toCharArray()), builder.build());
        Assert.assertEquals(uid.getUidValue(), "username");
    }
#end

#if ( $op_create_safe )
    @Test
    public void createTest() {
        logger.info("Running Create Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        Set<Attribute> createAttributes = new HashSet<Attribute>();
        createAttributes.add(new Name("Foo"));
        createAttributes.add(AttributeBuilder.buildPassword("Password".toCharArray()));
        createAttributes.add(AttributeBuilder.buildEnabled(true));
        Uid uid = facade.create(ObjectClass.ACCOUNT, createAttributes, builder.build());
        Assert.assertEquals(uid.getUidValue(), "foo");
    }
#end

#if ( $op_delete_safe )
    @Test
    public void deleteTest() {
        logger.info("Running Delete Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        facade.delete(ObjectClass.ACCOUNT, new Uid("username"), builder.build());
    }
#end

#if ( $op_resolveusername_safe )
    @Test
    public void resolveUsernameTest() {
        logger.info("Running ResolveUsername Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        Uid uid = facade.resolveUsername(ObjectClass.ACCOUNT, "username", builder.build());
        Assert.assertEquals(uid.getUidValue(), "username");
    }
#end

#if ( $op_schema_safe )
    @Test
    public void schemaTest() {
        logger.info("Running Schema Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        Schema schema = facade.schema();
        Assert.assertNotNull(schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME));
    }
#end

#if ( $op_scriptonconnector_safe )
    @Test
    public void runScriptOnConnectorTest() {
        logger.info("Running RunScriptOnConnector Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setRunAsUser("admin");
        builder.setRunWithPassword(new GuardedString("Passw0rd".toCharArray()));

        final ScriptContextBuilder scriptBuilder =
                new ScriptContextBuilder("Groovy", "return argument");
        scriptBuilder.addScriptArgument("argument", "value");

        Object result = facade.runScriptOnConnector(scriptBuilder.build(), builder.build());
        Assert.assertEquals(result, "value");
    }
#end

#if ( $op_scriptonresource_safe )
    @Test
    public void runScriptOnResourceTest() {
        logger.info("Running RunScriptOnResource Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setRunAsUser("admin");
        builder.setRunWithPassword(new GuardedString("Passw0rd".toCharArray()));

        final ScriptContextBuilder scriptBuilder = new ScriptContextBuilder("bash", "whoami");

        Object result = facade.runScriptOnResource(scriptBuilder.build(), builder.build());
        Assert.assertEquals(result, "admin");
    }
#end

#if ( $op_search_safe )
    @Test
    public void getObjectTest() {
        logger.info("Running GetObject Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setAttributesToGet(Name.NAME);
        ConnectorObject co =
                facade.getObject(ObjectClass.ACCOUNT, new Uid(
                        "3f50eca0-f5e9-11e3-a3ac-0800200c9a66"), builder.build());
        Assert.assertEquals(co.getName().getNameValue(), "Foo");
    }

    @Test
    public void searchTest() {
        logger.info("Running Search Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setPageSize(10);
        final ResultsHandler handler = new ToListResultsHandler();

        SearchResult result =
                facade.search(ObjectClass.ACCOUNT, FilterBuilder.equalTo(new Name("Foo")), handler,
                        builder.build());
        Assert.assertEquals(result.getPagedResultsCookie(), "0");
        Assert.assertEquals(((ToListResultsHandler) handler).getObjects().size(), 1);
    }
#end

#if ( $op_sync_safe )
    @Test
    public void getLatestSyncTokenTest() {
        logger.info("Running GetLatestSyncToken Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        SyncToken token = facade.getLatestSyncToken(ObjectClass.ACCOUNT);
        Assert.assertEquals(token.getValue(), 10);
    }

    @Test
    public void syncTest() {
        logger.info("Running Sync Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setPageSize(10);
        final SyncResultsHandler handler = new SyncResultsHandler() {
            public boolean handle(SyncDelta delta) {
                return false;
            }
        };

        SyncToken token =
                facade.sync(ObjectClass.ACCOUNT, new SyncToken(10), handler, builder.build());
        Assert.assertEquals(token.getValue(), 10);
    }
#end

#if ( $op_test_safe )
    @Test
    public void testTest() {
        logger.info("Running Test Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        facade.test();
    }

    @Test
    public void validateTest() {
        logger.info("Running Validate Test");
        final ConnectorFacade facade = createConnectorFacade(BasicConnector.class, null);
        facade.validate();
    }
#end

#if ( $op_update_safe )
    @Test
    public void updateTest() {
        logger.info("Running Update Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        Set<Attribute> updateAttributes = new HashSet<Attribute>();
        updateAttributes.add(new Name("Foo"));

        Uid uid = facade.update(ObjectClass.ACCOUNT, new Uid("Foo"), updateAttributes, builder.build());
        Assert.assertEquals(uid.getUidValue(),"foo");
    }
#end

#if ( $op_updateattributevalues_safe )
    @Test
    public void addAttributeValuesTest() {
        logger.info("Running AddAttributeValues Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        Set<Attribute> updateAttributes = new HashSet<Attribute>();
        // add 'group2' to existing groups
        updateAttributes.add(AttributeBuilder.build(PredefinedAttributes.GROUPS_NAME, "group2"));

        Uid uid = facade.addAttributeValues(ObjectClass.ACCOUNT, new Uid("Foo"), updateAttributes, builder.build());
        Assert.assertEquals(uid.getUidValue(),"foo");
    }

    @Test
    public void removeAttributeValuesTest() {
        logger.info("Running RemoveAttributeValues Test");
        final ConnectorFacade facade = createConnectorFacade(${connectorName}Connector.class, null);
        final OperationOptionsBuilder builder = new OperationOptionsBuilder();
        Set<Attribute> updateAttributes = new HashSet<Attribute>();
        // remove 'group2' from existing groups
        updateAttributes.add(AttributeBuilder.build(PredefinedAttributes.GROUPS_NAME, "group2"));

        Uid uid = facade.removeAttributeValues(ObjectClass.ACCOUNT, new Uid("Foo"), updateAttributes, builder.build());
        Assert.assertEquals(uid.getUidValue(),"foo");
    }
#end

    protected ConnectorFacade getFacade(${connectorName}Configuration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(${connectorName}Connector.class, config);
        return factory.newInstance(impl);
    }

    protected ConnectorFacade getFacade(Class<? extends Connector> clazz, String environment) {
        if (null == connectorFacade) {
            synchronized (this) {
                if (null == connectorFacade) {
                    connectorFacade = createConnectorFacade(clazz, environment);
                }
            }
        }
        return connectorFacade;
    }

    public ConnectorFacade createConnectorFacade(Class<? extends Connector> clazz,
        String environment) {
        PropertyBag propertyBag = TestHelpers.getProperties(clazz, environment);

        APIConfiguration impl =
        TestHelpers.createTestConfiguration(clazz, propertyBag, "configuration");
        impl.setProducerBufferSize(0);
        impl.getResultsHandlerConfiguration().setEnableAttributesToGetSearchResultsHandler(false);
        impl.getResultsHandlerConfiguration().setEnableCaseInsensitiveFilter(false);
        impl.getResultsHandlerConfiguration().setEnableFilteredResultsHandler(false);
        impl.getResultsHandlerConfiguration().setEnableNormalizingResultsHandler(false);

        //impl.setTimeout(CreateApiOp.class, 25000);
        //impl.setTimeout(UpdateApiOp.class, 25000);
        //impl.setTimeout(DeleteApiOp.class, 25000);

        return ConnectorFacadeFactory.getInstance().newInstance(impl);
    }
}
