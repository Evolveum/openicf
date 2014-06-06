/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openicf.misc.scriptedcommon;

import static org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase.CONFIGURATION;
import static org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase.LOGGER;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;
import groovy.util.DelegatingScript;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Scripted Connector.
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 */
public class ScriptedConfiguration extends AbstractConfiguration implements StatefulConfiguration {

    /**
     * Setup logging for the {@link ScriptedConnectorBase}.
     */
    private static final Log logger = Log.getLog(ScriptedConfiguration.class);
    private static final String DOT_STAR = ".*";
    private static final String EMPTY_STRING = "";
    private final CompilerConfiguration config;

    {
        config = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
        config.setSourceEncoding("UTF-8");
    }

    // =======================================================================
    // Operation Script Files
    // =======================================================================

    /**
     * Authenticate script filename
     */
    private String authenticateScriptFileName = null;

    /**
     * Return the Authenticate script FileName
     *
     * @return value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = AuthenticateOp.class)
    public String getAuthenticateScriptFileName() {
        return authenticateScriptFileName;
    }

    /**
     * Set the Authenticate script FileName
     *
     * @param value
     */
    public void setAuthenticateScriptFileName(String value) {
        this.authenticateScriptFileName = value;
    }

    /**
     * Create script filename
     */
    private String createScriptFileName = null;

    /**
     * Return the Create script FileName
     *
     * @return value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts" , operations = CreateOp.class)
    public String getCreateScriptFileName() {
        return createScriptFileName;
    }

    /**
     * Set the Create script FileName
     *
     * @param value
     */
    public void setCreateScriptFileName(String value) {
        this.createScriptFileName = value;
    }

    /**
     * Update script FileName
     */
    private String updateScriptFileName = null;

    /**
     * Return the Update script FileName
     *
     * @return updateScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = UpdateOp.class)
    public String getUpdateScriptFileName() {
        return updateScriptFileName;
    }

    /**
     * Set the Update script FileName
     *
     * @param value
     */
    public void setUpdateScriptFileName(String value) {
        this.updateScriptFileName = value;
    }

    /**
     * Delete script FileName
     */
    private String deleteScriptFileName = null;

    /**
     * Return the Delete script FileName
     *
     * @return deleteScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = DeleteOp.class)
    public String getDeleteScriptFileName() {
        return deleteScriptFileName;
    }

    /**
     * Set the Delete script FileName
     *
     * @param value
     */
    public void setDeleteScriptFileName(String value) {
        this.deleteScriptFileName = value;
    }

    /**
     * Test script FileName
     */
    private String resolveUsernameScriptFileName = null;

    /**
     * Return the Test script FileName
     *
     * @return resolveUsernameScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = ResolveUsernameOp.class)
    public String getResolveUsernameScriptFileName() {
        return resolveUsernameScriptFileName;
    }

    /**
     * Set the Test script FileName
     *
     * @param value
     */
    public void setResolveUsernameScriptFileName(String value) {
        this.resolveUsernameScriptFileName = value;
    }

    /**
     * ScriptOnResource script FileName
     */
    private String scriptOnResourceScriptFileName = null;

    /**
     * Return the ScriptOnResource script FileName
     *
     * @return scriptOnResourceScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = ScriptOnResourceOp.class)
    public String getScriptOnResourceScriptFileName() {
        return scriptOnResourceScriptFileName;
    }

    /**
     * Set the ScriptOnResource script FileName
     *
     * @param value
     */
    public void setScriptOnResourceScriptFileName(String value) {
        this.scriptOnResourceScriptFileName = value;
    }

    /**
     * Search script FileName
     */
    private String searchScriptFileName = null;

    /**
     * Return the Search script FileName
     *
     * @return searchScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = SearchOp.class)
    public String getSearchScriptFileName() {
        return searchScriptFileName;
    }

    /**
     * Set the Search script FileName
     *
     * @param value
     */
    public void setSearchScriptFileName(String value) {
        this.searchScriptFileName = value;
    }

    /**
     * Sync script FileName
     */
    private String syncScriptFileName = null;

    /**
     * Return the Sync script FileName
     *
     * @return syncScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = SyncOp.class)
    public String getSyncScriptFileName() {
        return syncScriptFileName;
    }

    /**
     * Set the Sync script FileName
     *
     * @param value
     */
    public void setSyncScriptFileName(String value) {
        this.syncScriptFileName = value;
    }

    /**
     * Schema script FileName
     */
    private String schemaScriptFileName = null;

    /**
     * Return the Schema script FileName
     *
     * @return schemaScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = SchemaOp.class)
    public String getSchemaScriptFileName() {
        return schemaScriptFileName;
    }

    /**
     * Set the Schema script FileName
     *
     * @param value
     */
    public void setSchemaScriptFileName(String value) {
        this.schemaScriptFileName = value;
    }

    /**
     * Test script FileName
     */
    private String testScriptFileName = null;

    /**
     * Return the Test script FileName
     *
     * @return testScriptFileName value
     */
    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts", operations = TestOp.class)
    public String getTestScriptFileName() {
        return testScriptFileName;
    }

    /**
     * Set the Test script FileName
     *
     * @param value
     */
    public void setTestScriptFileName(String value) {
        this.testScriptFileName = value;
    }

    // =======================================================================
    // Groovy Engine configuration
    // =======================================================================

    /**
     * Gets extensions used to find a groovy files
     *
     * @return
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public String[] getScriptExtensions() {
        return config.getScriptExtensions()
                .toArray(new String[config.getScriptExtensions().size()]);
    }

    public void setScriptExtensions(String[] scriptExtensions) {
        config.setScriptExtensions(null != scriptExtensions ? new HashSet<String>(Arrays
                .asList(scriptExtensions)) : null);
    }

    /**
     * Gets the currently configured warning level. See WarningMessage for level
     * details.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public int getWarningLevel() {
        return config.getWarningLevel();
    }

    /**
     * Sets the warning level. See WarningMessage for level details.
     */
    public void setWarningLevel(int level) {
        config.setWarningLevel(level);
    }

    /**
     * Gets the currently configured source file encoding.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public String getSourceEncoding() {
        return config.getSourceEncoding();
    }

    /**
     * Sets the encoding to be used when reading source files.
     */
    public void setSourceEncoding(String encoding) {
        config.setSourceEncoding(encoding);
    }

    /**
     * Gets the target directory for writing classes.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public File getTargetDirectory() {
        return config.getTargetDirectory();
    }

    /**
     * Sets the target directory.
     */
    public void setTargetDirectory(File directory) {
        this.config.setTargetDirectory(directory);
    }

    /**
     * @return the classpath
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public String[] getClasspath() {
        if (null != config.getClasspath()) {
            return config.getClasspath().toArray(new String[config.getClasspath().size()]);
        }
        return null;
    }

    /**
     * Sets the classpath.
     */
    public void setClasspath(String[] classpath) {
        config.setClasspathList(Arrays.asList(classpath));
    }

    /**
     * Returns true if verbose operation has been requested.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public boolean getVerbose() {
        return config.getVerbose();
    }

    /**
     * Turns verbose operation on or off.
     */
    public void setVerbose(boolean verbose) {
        config.setVerbose(verbose);
    }

    /**
     * Returns true if debugging operation has been requested.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public boolean getDebug() {
        return config.getDebug();
    }

    /**
     * Turns debugging operation on or off.
     */
    public void setDebug(boolean debug) {
        config.setDebug(debug);
    }

    /**
     * Returns the requested error tolerance.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public int getTolerance() {
        return config.getTolerance();
    }

    /**
     * Sets the error tolerance, which is the number of non-fatal errors (per
     * unit) that should be tolerated before compilation is aborted.
     */
    public void setTolerance(int tolerance) {
        config.setTolerance(tolerance);
    }

    /**
     * Gets the name of the base class for scripts. It must be a subclass of
     * Script.
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public String getScriptBaseClass() {
        return config.getScriptBaseClass();
    }

    /**
     * Sets the name of the base class for scripts. It must be a subclass of
     * Script.
     */
    public void setScriptBaseClass(String scriptBaseClass) {
        config.setScriptBaseClass(scriptBaseClass);
    }

    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public boolean getRecompileGroovySource() {
        return config.getRecompileGroovySource();
    }

    public void setRecompileGroovySource(boolean recompile) {
        config.setRecompileGroovySource(recompile);
    }

    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public int getMinimumRecompilationInterval() {
        return config.getMinimumRecompilationInterval();
    }

    public void setMinimumRecompilationInterval(int time) {
        config.setMinimumRecompilationInterval(time);
    }

    /**
     * Returns the list of disabled global AST transformation class names.
     *
     * @return a list of global AST transformation fully qualified class names
     */
    @ConfigurationProperty(groupMessageKey = "groovy.engine")
    public String[] getDisabledGlobalASTTransformations() {
        if (null != config.getDisabledGlobalASTTransformations()) {
            return config.getDisabledGlobalASTTransformations().toArray(new String[0]);
        }
        return null;
    }

    /**
     * {@link CompilerConfiguration#setDisabledGlobalASTTransformations(java.util.Set)}
     */
    public void setDisabledGlobalASTTransformations(final String[] disabledGlobalASTTransformations) {
        if (null != disabledGlobalASTTransformations) {

            config.setDisabledGlobalASTTransformations(new HashSet<String>(Arrays
                    .asList(disabledGlobalASTTransformations)));
        } else {
            config.setDisabledGlobalASTTransformations(null);
        }
    }

    // =======================================================================
    // Other Configuration Properties
    // =======================================================================

    @ConfigurationProperty(groupMessageKey = "groovy.operation.scripts")
    public String getCustomizerScriptFileName() {
        return customizerScriptFileName;
    }

    public void setCustomizerScriptFileName(String customizerScriptFileName) {
        this.customizerScriptFileName = customizerScriptFileName;
    }

    private String customizerScriptFileName = null;

    // =======================================================================
    // Methods for Script writers
    // =======================================================================

    private final ConcurrentMap<String, Object> propertyBag =
            new ConcurrentHashMap<String, Object>();

    /**
     * Returns the Map shared between the Connector instances.
     *
     * Shared map to store initialised resources which should be shared between
     * the scripts.
     *
     * @return single instance of shared ConcurrentMap.
     */
    public ConcurrentMap<String, Object> getPropertyBag() {
        return propertyBag;
    }

    // =======================================================================
    // Interface Implementation
    // =======================================================================

    public void release() {
        synchronized (this) {
            groovyScriptEngine = null;
            loggerCache.clear();
            logger.ok("Shared state ScriptedConfiguration is successfully released");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        logger.info("Load and compile configured scripts");
        loadScript(getAuthenticateScriptFileName());
        loadScript(getCreateScriptFileName());
        loadScript(getDeleteScriptFileName());
        loadScript(getResolveUsernameScriptFileName());
        loadScript(getSchemaScriptFileName());
        loadScript(getScriptOnResourceScriptFileName());
        loadScript(getSearchScriptFileName());
        loadScript(getSyncScriptFileName());
        loadScript(getTestScriptFileName());
        loadScript(getUpdateScriptFileName());
        logger.info("Load and compile of scripts are successful");
    }

    // =======================================================================
    //
    // =======================================================================

    protected String getDefaultCustomizerScriptName() {
        return new StringBuilder("/").append(getClass().getPackage().getName().replace('.', '/'))
                .append("/CustomizerScript.groovy").toString();
    }

    protected Class getCustomizerClass() {
        Class customizerClass = null;
        if (StringUtil.isBlank(customizerScriptFileName)) {
            URL url = getClass().getResource(getDefaultCustomizerScriptName());
            if (null != url) {
                GroovyCodeSource source = null;
                try {
                    source =
                            new GroovyCodeSource(ResourceGroovyMethods.getText(url,
                                    getSourceEncoding()), url.toExternalForm(), "/groovy/script");
                } catch (IOException e) {
                    throw ConnectorException.wrap(e);
                }
                source.setCachable(false);
                customizerClass = getGroovyScriptEngine().getGroovyClassLoader().parseClass(source);
            }
        } else {
            customizerClass = loadScript(customizerScriptFileName);
        }
        return customizerClass;
    }

    protected Script createCustomizerScript(Class customizerClass, Binding binding) {
        binding.setVariable(CONFIGURATION, this);
        return InvokerHelper.createScript(customizerClass, binding);
    }

    private final ConcurrentMap<String, Log> loggerCache = new ConcurrentHashMap<String, Log>(11);

    Object evaluate(String scriptName, Binding binding, Object delegate) throws Exception {
        try {
            Script scr = getGroovyScriptEngine().createScript(scriptName, binding);
            binding.setVariable(LOGGER, getLogger(scr.getClass()));
            if (scr instanceof DelegatingScript && null != delegate) {
                ((DelegatingScript) scr).setDelegate(delegate);
            }
            return scr.run();
        } catch (ResourceException e) {
            throw ConnectorException.wrap(e);
        } catch (ScriptException e) {
            throw ConnectorException.wrap(e);
        }
    }

    Class loadScript(String scriptName) {
        if (StringUtil.isNotBlank(scriptName)) {
            try {
                return getGroovyScriptEngine().loadScriptByName(scriptName);
            } catch (ResourceException e) {
                throw ConnectorException.wrap(e);
            } catch (ScriptException e) {
                throw ConnectorException.wrap(e);
            }
        }
        return null;
    }

    protected Log getLogger(final Class<?> clazz) {
        final String key = clazz.getName();
        Log logger = loggerCache.get(key);
        if (null == logger) {
            logger = Log.getLog(clazz);
            Log l = loggerCache.putIfAbsent(key, logger);
            if (l != null) {
                logger = l;
            }
        }
        return logger;
    }

    private GroovyScriptEngine groovyScriptEngine = null;

    protected GroovyScriptEngine getGroovyScriptEngine() {
        if (null == groovyScriptEngine) {
            synchronized (this) {
                if (null == groovyScriptEngine) {

                    final CompilerConfiguration compilerConfiguration =
                            new CompilerConfiguration(config);
                    compilerConfiguration.addCompilationCustomizers(getImportCustomizer(null));

                    final GroovyClassLoader loader =
                            new GroovyClassLoader(getParentLoader(), compilerConfiguration, true);

                    groovyScriptEngine =
                            new GroovyScriptEngine(getRoots(compilerConfiguration, loader), loader);

                    initializeCustomizer();
                }
            }
        }
        return groovyScriptEngine;
    }

    /*
     * This must be called once from thread-safe location and inside the
     * synchronized to avoid deadlock.
     */
    private void initializeCustomizer() {
        try {
            Class customizerClass = getCustomizerClass();

            if (null != customizerClass) {
                Binding binding = new Binding();
                binding.setVariable(LOGGER, getLogger(customizerClass));
                createCustomizerScript(customizerClass, binding).run();
            }
        } catch (Throwable t) {
            logger.error(t, "Failed to customize the connector");
            throw ConnectorException.wrap(t);
        }
    }

    protected ClassLoader getParentLoader() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class c = contextLoader.loadClass(Script.class.getName());
            if (c == Script.class) {
                return contextLoader;
            }
        } catch (ClassNotFoundException e) {
            /* ignore */
        }
        return Script.class.getClassLoader();
    }

    protected URL[] getRoots(CompilerConfiguration compilerConfiguration, GroovyClassLoader loader) {
        // Do not allow this because it collides with the classes from
        // parent. For safety remove this from roots
        URL forbiddenLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        List<URL> safeRoots = new ArrayList<URL>();
        for (URL root : loader.getURLs()) {
            if (forbiddenLocation.equals(root)) {
                logger.info(
                        "The connector source location is removed from the roots. This url is not allowed: {0}",
                        forbiddenLocation);
            }
            safeRoots.add(root);
        }
        return safeRoots.toArray(new URL[safeRoots.size()]);
    }

    protected ImportCustomizer getImportCustomizer(ImportCustomizer parent) {
        final ImportCustomizer ic = null != parent ? parent : new ImportCustomizer();
        for (final String imp : getImports()) {
            ic.addStarImports(imp.replace(DOT_STAR, EMPTY_STRING));
        }
        ic.addImport("ICF", ICFObjectBuilder.class.getName());
        return ic;
    }

    protected String[] getImports() {
        return new String[] { ConnectorObject.class.getPackage().getName() + ".*" };
    }
}
