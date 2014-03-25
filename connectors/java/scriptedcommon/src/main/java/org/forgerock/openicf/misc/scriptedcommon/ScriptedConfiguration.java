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

import static org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase.LOGGER;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.identityconnectors.common.ReflectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.StatefulConfiguration;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
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
 *
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
    // Scripts
    // =======================================================================

    /**
     * Should password be passed to scripts in clear text?
     */
    private boolean clearTextPasswordToScript = true;

    /**
     * Return the clearTextPasswordToScript boolean
     *
     * @return value
     */
    public boolean getClearTextPasswordToScript() {
        return clearTextPasswordToScript;
    }

    /**
     * Set the clearTextPasswordToScript value
     *
     * @param value
     */
    public void setClearTextPasswordToScript(boolean value) {
        this.clearTextPasswordToScript = value;
    }

    /**
     * Authenticate script filename
     */
    private String authenticateScriptFileName = null;

    /**
     * Return the Authenticate script FileName
     *
     * @return value
     */
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
     * ScriptOnConnector script FileName
     */
    private String scriptOnConnectorScriptFileName = null;

    /**
     * Return the ScriptOnConnector script FileName
     *
     * @return scriptOnConnectorScriptFileName value
     */
    public String getScriptOnConnectorScriptFileName() {
        return scriptOnConnectorScriptFileName;
    }

    /**
     * Set the ScriptOnConnector script FileName
     *
     * @param value
     */
    public void setScriptOnConnectorScriptFileName(String value) {
        this.scriptOnConnectorScriptFileName = value;
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

    /**
     * Gets the currently configured warning level. See WarningMessage for level
     * details.
     */
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

    public void setRecompileGroovySource(boolean recompile) {
        config.setRecompileGroovySource(recompile);
    }

    public boolean getRecompileGroovySource() {
        return config.getRecompileGroovySource();
    }

    public void setMinimumRecompilationInterval(int time) {
        config.setMinimumRecompilationInterval(time);
    }

    public int getMinimumRecompilationInterval() {
        return config.getMinimumRecompilationInterval();
    }

    /**
     * Returns the list of disabled global AST transformation class names.
     *
     * @return a list of global AST transformation fully qualified class names
     */
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

    public void release() {
        groovyScriptEngine = null;
        loggerCache.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        logger.info("Load and compile every scripts");
        loadScript(getAuthenticateScriptFileName());
        loadScript(getCreateScriptFileName());
        loadScript(getDeleteScriptFileName());
        loadScript(getResolveUsernameScriptFileName());
        loadScript(getSchemaScriptFileName());
        loadScript(getScriptOnConnectorScriptFileName());
        loadScript(getScriptOnResourceScriptFileName());
        loadScript(getSearchScriptFileName());
        loadScript(getSyncScriptFileName());
        loadScript(getTestScriptFileName());
        loadScript(getUpdateScriptFileName());
        logger.info("Scripts are loaded");
    }

    private static ConcurrentMap<String, Logger> loggerCache =
            new ConcurrentHashMap<String, Logger>(11);

    Object evaluate(String scriptName, Binding binding, Object delegate) {
        try {
            Script scr = getGroovyScriptEngine().createScript(scriptName, binding);
            binding.setVariable(LOGGER, getLogger(scr.getClass(), 12));
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

    Logger getLogger(final Class<?> clazz, int depth) {
        final String key = clazz.getName();
        Logger logger = loggerCache.get(key);
        if (null == logger) {
            logger = new LoggerImpl(clazz, getConnectorMessages(), depth);
            Logger l = loggerCache.putIfAbsent(key, logger);
            if (l != null) {
                logger = l;
            }
        }
        return logger;
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

    private GroovyScriptEngine groovyScriptEngine = null;

    GroovyScriptEngine getGroovyScriptEngine() {
        if (null == groovyScriptEngine) {
            synchronized (this) {
                if (null == groovyScriptEngine) {

                    final CompilerConfiguration compilerConfiguration =
                            new CompilerConfiguration(config);
                    compilerConfiguration.addCompilationCustomizers(getImportCustomizer(null));

                    final GroovyClassLoader loader =
                            new GroovyClassLoader(getClass().getClassLoader(),
                                    compilerConfiguration, true);

                    groovyScriptEngine =
                            new GroovyScriptEngine(getRoots(compilerConfiguration, loader), loader);
                }
            }
        }
        return groovyScriptEngine;
    }

    protected URL[] getRoots(CompilerConfiguration compilerConfiguration, GroovyClassLoader loader) {
        if (false) {
            // TODO allow to add the Connector ROOT
            URL[] urls = Arrays.copyOf(loader.getURLs(), loader.getURLs().length + 1);
            urls[urls.length - 1] = getClass().getProtectionDomain().getCodeSource().getLocation();
            return urls;
        } else {
            return loader.getURLs();
        }
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

    protected static class LoggerImpl implements Logger {

        private static final String DFLT = "__IGNORE__";
        private final Class<?> clazz;
        private final ConnectorMessages connectorMessages;
        private final Log log;
        private final Integer methodDepth;

        protected LoggerImpl(final Class<?> clazz, final ConnectorMessages messages, int depth) {
            this.clazz = clazz;
            log = Log.getLog(this.clazz);
            connectorMessages = messages;
            methodDepth = depth;
        }

        protected Log getLog() {
            return log;
        }

        /**
         * Logs based on the parameters given. Uses the format parameter inside
         * {@link java.text.MessageFormat}.
         *
         * @param level
         *            the logging level at which to write the message.
         * @param ex
         *            [optional] exception stack trace to log.
         * @param format
         *            [optional] create a message of a particular format.
         * @param args
         *            [optional] parameters to the format string.
         */
        public void log(final Log.Level level, final Throwable ex, final String key,
                final String format, final Object... args) {
            if (getLog().isLoggable(level)) {
                String message = format;
                if (key != null) {
                    String messageLocal = connectorMessages.format(key, DFLT, args);
                    if (!DFLT.equals(messageLocal)) {
                        // MessageKey was found
                        message = messageLocal;
                    } else {
                        // MessageKey not was found
                        if (format != null && args != null) {
                            message = MessageFormat.format(format, args);
                        } else if (format == null && ex != null) {
                            message = ex.getLocalizedMessage();
                        } else if (format == null) {
                            message = key;
                        }
                    }
                } else {
                    if (format != null && args != null) {
                        message = MessageFormat.format(format, args);
                    } else if (format == null && ex != null) {
                        message = ex.getLocalizedMessage();
                    }
                }
                final String methodName = ReflectionUtil.getMethodName(methodDepth);
                getLog().log(clazz, methodName, level, message, ex);
            }
        }

        public boolean isDebug() {
            return getLog().isOk();
        }

        public boolean isInfo() {
            return getLog().isInfo();
        }

        public boolean isWarning() {
            return getLog().isWarning();
        }

        public boolean isError() {
            return getLog().isError();
        }

        public void debug(Throwable ex, String format, Object... args) {
            log(Log.Level.OK, ex, null, format, args);
        }

        public void debugLocale(Throwable ex, String key, Object... args) {
            log(Log.Level.OK, ex, key, null, args);
        }

        public void debugLocale(Throwable ex, String key, String defaultMessage, Object... args) {
            log(Log.Level.OK, ex, key, defaultMessage, args);
        }

        public void info(Throwable ex, String format, Object... args) {
            log(Log.Level.INFO, ex, null, format, args);
        }

        public void infoLocale(Throwable ex, String key, Object... args) {
            log(Log.Level.INFO, ex, key, null, args);
        }

        public void infoLocale(Throwable ex, String key, String defaultMessage, Object... args) {
            log(Log.Level.INFO, ex, key, defaultMessage, args);
        }

        public void warn(Throwable ex, String format, Object... args) {
            log(Log.Level.WARN, ex, null, format, args);
        }

        public void warnLocale(Throwable ex, String key, Object... args) {
            log(Log.Level.WARN, ex, key, null, args);
        }

        public void warnLocale(Throwable ex, String key, String defaultMessage, Object... args) {
            log(Log.Level.WARN, ex, key, defaultMessage, args);
        }

        public void error(Throwable ex, String format, Object... args) {
            log(Log.Level.ERROR, ex, null, format, args);
        }

        public void errorLocale(Throwable ex, String key, Object... args) {
            log(Log.Level.ERROR, ex, key, null, args);
        }

        public void errorLocale(Throwable ex, String key, String defaultMessage, Object... args) {
            log(Log.Level.ERROR, ex, key, defaultMessage, args);
        }

        public void debug(String format, Object... args) {
            log(Log.Level.OK, null, null, format, args);
        }

        public void debugLocale(String key, Object... args) {
            log(Log.Level.OK, null, key, null, args);
        }

        public void debugLocale(String key, String defaultMessage, Object... args) {
            log(Log.Level.OK, null, key, defaultMessage, args);
        }

        public void info(String format, Object... args) {
            log(Log.Level.INFO, null, null, format, args);
        }

        public void infoLocale(String key, Object... args) {
            log(Log.Level.INFO, null, key, null, args);
        }

        public void infoLocale(String key, String defaultMessage, Object... args) {
            log(Log.Level.INFO, null, key, defaultMessage, args);
        }

        public void warn(String format, Object... args) {
            log(Log.Level.WARN, null, null, format, args);
        }

        public void warnLocale(String key, Object... args) {
            log(Log.Level.WARN, null, key, null, args);
        }

        public void warnLocale(String key, String defaultMessage, Object... args) {
            log(Log.Level.WARN, null, key, defaultMessage, args);
        }

        public void error(String format, Object... args) {
            log(Log.Level.ERROR, null, null, format, args);
        }

        public void errorLocale(String key, Object... args) {
            log(Log.Level.ERROR, null, key, null, args);
        }

        public void errorLocale(String key, String defaultMessage, Object... args) {
            log(Log.Level.ERROR, null, key, defaultMessage, args);
        }
    }
}
