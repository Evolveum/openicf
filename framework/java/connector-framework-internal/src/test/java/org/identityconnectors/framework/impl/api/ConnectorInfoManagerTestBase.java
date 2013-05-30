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
package org.identityconnectors.framework.impl.api;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.fest.assertions.api.Assertions.assertThat;

import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import java.io.InputStream;
import java.net.URISyntaxException;
import static org.identityconnectors.common.IOUtil.makeURL;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.Version;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.common.FrameworkUtilTestHelpers;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.impl.api.local.ConnectorPoolManager;


public abstract class ConnectorInfoManagerTestBase {

    protected static String bundlesDirectory;

    private static ConnectorInfo findConnectorInfo(ConnectorInfoManager manager, String version,
            String connectorName) {
        for (ConnectorInfo info : manager.getConnectorInfos()) {
            ConnectorKey key = info.getConnectorKey();
            if (version.equals(key.getBundleVersion())
                    && connectorName.equals(key.getConnectorName())) {
                // intentionally ineffecient to test more code
                return manager.findConnectorInfo(key);
            }
        }
        return null;
    }

    @BeforeMethod
	public void before() {
        // LocalConnectorInfoManagerImpl needs to know the framework version.
        // In case the framework doesn't know its version (for instance, because
        // we are running against the classes, not a JAR file, so META-INF/MANIFEST.MF
        // is not available), fake the version here.
        FrameworkUtilTestHelpers.setFrameworkVersion(Version.parse("2.0"));
    }

    @AfterTest
	public void after() {
        shutdownConnnectorInfoManager();
        FrameworkUtilTestHelpers.setFrameworkVersion(null);
    }

    @Test
    public void testClassLoading() throws Exception {
        final ClassLoader startLocal =
            Thread.currentThread().getContextClassLoader();
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info1 =
            findConnectorInfo(manager,
                              "1.0.0.0",
                              "org.identityconnectors.testconnector.TstConnector");
        assertNotNull(info1);
        ConnectorInfo info2 =
            findConnectorInfo(manager,
                             "2.0.0.0",
                             "org.identityconnectors.testconnector.TstConnector");
        assertNotNull(info2);

        APIConfiguration apiConfig1 = info1.createDefaultAPIConfiguration();
        ConfigurationProperties props = apiConfig1.getConfigurationProperties();
        ConfigurationProperty property  = props.getProperty("numResults");
        property.setValue(1);

        ConnectorFacade facade1 =
            ConnectorFacadeFactory.getInstance().newInstance(apiConfig1);

        ConnectorFacade facade2 =
            ConnectorFacadeFactory.getInstance().newInstance(info2.createDefaultAPIConfiguration());

        Set<Attribute> attrs = CollectionUtil.<Attribute>newReadOnlySet();
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, null).getUidValue(), "1.0");
        assertEquals(facade2.create(ObjectClass.ACCOUNT, attrs, null).getUidValue(), "2.0");

        final int [] count = new int[1];
        facade1.search(ObjectClass.ACCOUNT, null, new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                count[0]++;
                // make sure thread local classloader is restored
                assertThat(startLocal).isSameAs(Thread.currentThread().getContextClassLoader());
                return true;
            }
        }, null);
        assertEquals(count[0], 1);
        assertThat(startLocal).as("Thread local classloader is restored").isSameAs(
                Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testNativeLibraries() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "2.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();
        ConnectorFacade facade =
            ConnectorFacadeFactory.getInstance().newInstance(api);

        try {
            // The connector will do a System.loadLibrary().
            facade.authenticate(ObjectClass.ACCOUNT, "username", new GuardedString("password".toCharArray()), null);
        } catch (UnsatisfiedLinkError e) {
            // If this particular exception occurs, then the bundle class loader has
            // correctly pointed to the native library (but the library could not be
            // loaded, since it is not a valid library--we want to keep our tests
            // platform-independent).
            assertTrue(e.getMessage().contains("file too short") ||
                    e.getMessage().contains("no suitable image found"));
        } catch (RuntimeException e) {
            // Remote framework serializes UnsatisfiedLinkError as RuntimeException.
            assertTrue(e.getMessage().contains("file too short") ||
                    e.getMessage().contains("no suitable image found"));
        }
    }

    /**
     * Attempt to test the information from the configuration.
     *
     * @throws Exception if there is an issue.
     */
    @Test
    public void testAPIConfiguration() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();

        ConfigurationProperties props = api.getConfigurationProperties();
        ConfigurationProperty property  = props.getProperty("tstField");
        assertNotNull(property);

        Set<Class<? extends APIOperation>> operations =
            property.getOperations();
        assertEquals(operations.size(), 1);
        assertEquals(operations.iterator().next(), SyncApiOp.class);

        CurrentLocale.clear();
        assertEquals(property.getHelpMessage(null), "Help for test field.");
        assertEquals(property.getDisplayName(null), "Display for test field.");
        assertEquals(property.getGroup(null), "Group for test field.");
        assertEquals(info.getMessages().format("TEST_FRAMEWORK_KEY", "empty"),
                "Test Framework Value");

        Locale xlocale = new Locale("es");
        CurrentLocale.set(xlocale);
        assertEquals(property.getHelpMessage(null), "tstField.help_es");
        assertEquals(property.getDisplayName(null), "tstField.display_es");

        Locale esESlocale = new Locale("es","ES");
        CurrentLocale.set(esESlocale);
        assertEquals(property.getHelpMessage(null), "tstField.help_es-ES");
        assertEquals(property.getDisplayName(null), "tstField.display_es-ES");

        Locale esARlocale = new Locale("es","AR");
        CurrentLocale.set(esARlocale);
        assertEquals(property.getHelpMessage(null), "tstField.help_es");
        assertEquals(property.getDisplayName(null), "tstField.display_es");

        CurrentLocale.clear();

        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);
        // call the various create/update/delete commands..
        facade.schema();
    }

    /**
     * Attempt to test the information from the configuration.
     *
     * @throws Exception if there is an issue.
     */
    @Test
    public void testValidate() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();

        ConfigurationProperties props = api.getConfigurationProperties();
        ConfigurationProperty property = props.getProperty("failValidation");
        property.setValue(false);
        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);
        facade.validate();
        property.setValue(true);
        facade = facf.newInstance(api);
        //validate and also test that locale is propogated
        //properly
        try {
            CurrentLocale.set(new Locale("en"));
            facade.validate();

            fail("exception expected");
        } catch (ConnectorException e) {
            assertThat(e).hasMessage("validation failed en");
        } finally {
            CurrentLocale.clear();
        }
        //validate and also test that locale is propogated
        //properly
        try {
            CurrentLocale.set(new Locale("es"));
            facade.validate();

            fail("exception expected");
        } catch (ConnectorException e) {
            assertThat(e).hasMessage("validation failed es");
        } finally {
            CurrentLocale.clear();
        }
    }

    /**
     * Main purpose of this is to test searching with
     * many results and that we can properly handle
     * stopping in the middle of this. There's a bunch of
     * code in the remote stuff that is there to handle this
     * in particular that we want to excercise.
     */
    @Test
    public void testSearchWithManyResults() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();

        ConfigurationProperties props = api.getConfigurationProperties();
        ConfigurationProperty property = props.getProperty("numResults");

        //1000 is several times the remote size between pauses
        property.setValue(1000);

        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);

        final List<ConnectorObject> results = new ArrayList<ConnectorObject>();

        facade.search(ObjectClass.ACCOUNT, null, new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                results.add(obj);
                return true;
            }
        }, null);

        assertEquals(results.size(), 1000);
        for (int i = 0; i < results.size(); i++) {
            ConnectorObject obj = results.get(i);
            assertEquals(obj.getUid().getUidValue(), String.valueOf(i));
        }

        results.clear();

        facade.search(ObjectClass.ACCOUNT, null, new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                if (results.size() < 500) {
                    results.add(obj);
                    return true;
                } else {
                    return false;
                }
            }
        }, null);

        assertEquals(results.size(), 500);
        for (int i = 0; i < results.size(); i++) {
            ConnectorObject obj = results.get(i);
            assertEquals(obj.getUid().getUidValue(), String.valueOf(i));
        }
    }

    @Test
	public void testSearchStress() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();

        ConfigurationProperties props = api.getConfigurationProperties();
        ConfigurationProperty property  = props.getProperty("numResults");

        property.setValue(10000);

        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);
        long start = System.currentTimeMillis();
        facade.search(ObjectClass.ACCOUNT, null, new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                return true;
            }
        },null);
        long end = System.currentTimeMillis();
        System.out.println("Test took: "+(end-start)/1000);
    }

    //@Test(groups = {"broken"}, threadPoolSize = 4, invocationCount = 1000, timeOut = 1000)
    public void testSchemaStress() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();


        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);

        facade.schema();
    }

    //@Test(groups = {"broken"}, threadPoolSize = 4, invocationCount = 1000, timeOut = 1000, dataProvider = "Data-Provider-Function")
    public void testCreateStress(Set<Attribute> attrs) throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();


        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);
        facade.create(ObjectClass.ACCOUNT,attrs,null);
    }

    @DataProvider(name = "Data-Provider-Function")
    public Object[][] scriptProvider() {
        Set<Attribute> attrs = new HashSet<Attribute>();
        for ( int j = 0; j < 50; j++) {
            attrs.add(AttributeBuilder.build("myattributename"+j, "myattributevalue"+j));
        }
        return new Object[][] { { attrs } };
    }

    /**
     * Main purpose of this is to test sync with
     * many results and that we can properly handle
     * stopping in the middle of this. There's a bunch of
     * code in the remote stuff that is there to handle this
     * in particular that we want to excercise.
     */
    @Test
    public void testSyncWithManyResults() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration api = info.createDefaultAPIConfiguration();

        ConfigurationProperties props = api.getConfigurationProperties();
        ConfigurationProperty property = props.getProperty("numResults");

        //1000 is several times the remote size between pauses
        property.setValue(1000);

        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);

        SyncToken latest = facade.getLatestSyncToken(ObjectClass.ACCOUNT);
        assertEquals(latest.getValue(), "mylatest");

        final List<SyncDelta> results = new ArrayList<SyncDelta>();

        facade.sync(ObjectClass.ACCOUNT, null, new SyncResultsHandler() {
            public boolean handle(SyncDelta obj) {
                results.add(obj);
                return true;
            }
        }, null);

        assertEquals(results.size(), 1000);
        for (int i = 0; i < results.size(); i++) {
            SyncDelta obj = results.get(i);
            assertEquals(obj.getObject().getUid().getUidValue(), String.valueOf(i));
        }

        results.clear();

        facade.sync(ObjectClass.ACCOUNT, null, new SyncResultsHandler() {
            public boolean handle(SyncDelta obj) {
                if (results.size() < 500) {
                    results.add(obj);
                    return true;
                } else {
                    return false;
                }
            }
        }, null);

        assertEquals(results.size(), 500);
        for (int i = 0; i < results.size(); i++) {
            SyncDelta obj = results.get(i);
            assertEquals(obj.getObject().getUid().getUidValue(), String.valueOf(i));
        }
    }

    //TODO: this needs to overridden for C# testing
    @Test
    public void testScripting() throws Exception
    {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info =
            findConnectorInfo(manager,
                    "1.0.0.0",
                    "org.identityconnectors.testconnector.TstConnector");
        APIConfiguration api = info.createDefaultAPIConfiguration();


        ConnectorFacadeFactory facf = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = facf.newInstance(api);

        ScriptContextBuilder builder = new ScriptContextBuilder();
        builder.addScriptArgument("arg1", "value1");
        builder.addScriptArgument("arg2", "value2");
        builder.setScriptLanguage("GROOVY");

        //test that they can run the script and access the
        //connector object
        {
            String SCRIPT =
                "return connector.concat(arg1,arg2)";
            builder.setScriptText(SCRIPT);
            String result = (String)facade.runScriptOnConnector(builder.build(),
                    null);

            assertEquals(result, "value1value2");
        }

        //test that they can access a class in the class loader
        {
            String SCRIPT =
                "return org.identityconnectors.testcommon.TstCommon.getVersion()";
            builder.setScriptText(SCRIPT);
            String result = (String)facade.runScriptOnConnector(builder.build(),
                    null);
            assertEquals(result, "1.0");
        }

        //test that they cannot access a class in internal
        {
            String clazz = ConfigurationPropertyImpl.class.getName();

            String SCRIPT =
                "return new "+clazz+"()";
            builder.setScriptText(SCRIPT);
            try {
                facade.runScriptOnConnector(builder.build(), null);
                fail("exception expected");
            } catch (Exception e) {
                String expectedMessage =
                        "unable to resolve class org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl";
                assertThat(e).hasMessageContaining(expectedMessage);
            }
        }

        // test that they can access a class in common
        {
            String clazz = AttributeBuilder.class.getName();
            String SCRIPT = "return " + clazz + ".build(\"myattr\")";
            builder.setScriptText(SCRIPT);
            Attribute attr = (Attribute) facade.runScriptOnConnector(builder.build(), null);
            assertEquals(attr.getName(), "myattr");
        }
    }

    @Test
    public void testConnectionPooling() throws Exception {
        ConnectorPoolManager.dispose();
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info1 =
            findConnectorInfo(manager,
                              "1.0.0.0",
                              "org.identityconnectors.testconnector.TstConnector");
        assertNotNull(info1);

        //reset connection count
        {
            //trigger TstConnection.init to be called
            APIConfiguration config =
                info1.createDefaultAPIConfiguration();
            config.getConfigurationProperties().getProperty("resetConnectionCount").setValue(true);
            ConnectorFacade facade1 =
                ConnectorFacadeFactory.getInstance().newInstance(config);
            facade1.schema(); //force instantiation
        }

        APIConfiguration config = info1.createDefaultAPIConfiguration();

        config.getConnectorPoolConfiguration().setMinIdle(0);
        config.getConnectorPoolConfiguration().setMaxIdle(0);

        ConnectorFacade facade1 = ConnectorFacadeFactory.getInstance().newInstance(config);

        OperationOptionsBuilder builder = new OperationOptionsBuilder();
        builder.setOption("testPooling", "true");
        OperationOptions options = builder.build();
        Set<Attribute> attrs = CollectionUtil.<Attribute>newReadOnlySet();
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "1");
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "2");
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "3");
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "4");
        config = info1.createDefaultAPIConfiguration();
        config.getConnectorPoolConfiguration().setMinIdle(1);
        config.getConnectorPoolConfiguration().setMaxIdle(2);
        facade1 = ConnectorFacadeFactory.getInstance().newInstance(config);
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "5");
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "5");
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "5");
        assertEquals(facade1.create(ObjectClass.ACCOUNT, attrs, options).getUidValue(), "5");
    }

    @Test
    public void testTimeout() throws Exception {
        ConnectorInfoManager manager =
            getConnectorInfoManager();
        ConnectorInfo info1 =
            findConnectorInfo(manager,
                              "1.0.0.0",
                              "org.identityconnectors.testconnector.TstConnector");

        APIConfiguration config = info1.createDefaultAPIConfiguration();
        config.setTimeout(CreateApiOp.class, 5000);
        config.setTimeout(SearchApiOp.class, 5000);
        ConfigurationProperties props = config.getConfigurationProperties();
        ConfigurationProperty property = props.getProperty("numResults");
        //1000 is several times the remote size between pauses
        property.setValue(2);
        OperationOptionsBuilder opBuilder = new OperationOptionsBuilder();
        opBuilder.setOption("delay", 10000);

        ConnectorFacade facade1 =
            ConnectorFacadeFactory.getInstance().newInstance(config);

        Set<Attribute> attrs = CollectionUtil.<Attribute>newReadOnlySet();
        try {
            facade1.create(ObjectClass.ACCOUNT, attrs, opBuilder.build()).getUidValue();
            fail("expected timeout");
        } catch (OperationTimeoutException e) {
            //expected
        }

        try {
            facade1.search(ObjectClass.ACCOUNT, null, new ResultsHandler() {
                public boolean handle(ConnectorObject obj) {
                    return true;
                }
            }, opBuilder.build());
            fail("expected timeout");
        } catch (OperationTimeoutException e) {
            //expected
        }
    }

    //private final static String TEST_BUNDLES_DIR_PROPERTY = "testbundles.dir";

    protected final File getTestBundlesDir() throws URISyntaxException {
        URL testOutputDirectory = ConnectorInfoManagerTestBase.class.getResource("/");
        File testBundlesDir = new File(testOutputDirectory.toURI());
        if (!testBundlesDir.isDirectory()) {
            throw new ConnectorException(testBundlesDir.getPath() + " does not exist");
        }
        return testBundlesDir;
    }

    protected final List<URL> getTestBundles() throws Exception {
        File testBundlesDir = getTestBundlesDir();
        List<URL> rv = new ArrayList<URL>();
        rv.add(makeURL(testBundlesDir,"testbundlev1.jar"));
        rv.add(makeURL(testBundlesDir,"testbundlev2.jar"));
        return rv;
    }

    /**
     * To be overridden by subclasses to get different ConnectorInfoManagers
     *
     * @return
     * @throws Exception
     */
    protected abstract ConnectorInfoManager getConnectorInfoManager() throws Exception;

    protected abstract void shutdownConnnectorInfoManager();
}
