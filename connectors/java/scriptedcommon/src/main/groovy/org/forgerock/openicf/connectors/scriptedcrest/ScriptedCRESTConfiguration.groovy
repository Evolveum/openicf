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

package org.forgerock.openicf.connectors.scriptedcrest

import org.apache.http.HttpHost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.nio.client.methods.HttpAsyncMethods
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.forgerock.json.resource.Context
import org.forgerock.json.resource.ResourceName
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.common.Assertions
import org.identityconnectors.common.StringUtil
import org.identityconnectors.framework.spi.ConfigurationProperty

import java.util.concurrent.Future

import static org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase.LOGGER

/**
 * A ScriptedCRESTConfiguration.
 *
 * @author Laszlo Hordos
 */
class ScriptedCRESTConfiguration extends ScriptedConfiguration {

    URI serviceAddress = null;

    URI proxyAddress = null;

    @ConfigurationProperty(required = true)
    URI getServiceAddress() {
        return serviceAddress
    }

    void setServiceAddress(URI serviceAddress) {
        this.serviceAddress = serviceAddress
        host = null;
        resourceName = null;
    }

    URI getProxyAddress() {
        return proxyAddress
    }

    void setProxyAddress(URI proxyAddress) {
        this.proxyAddress = proxyAddress
        proxy = null;
    }

    @Override
    void validate() {
        Assertions.nullCheck(serviceAddress, "serviceAddress")
        super.validate()
    }

    @Override
    void release() {
        synchronized (this) {
            super.release()
            if (null != httpClient) {
                httpClient.close();
                httpClient = null;
            }
        }
    }

    private HttpHost host = null;

    private HttpHost proxy = null;

    private ResourceName resourceName = null;


    ResourceName getResourceName() {
        resourceName = ResourceName.valueOf(serviceAddress?.path);
    }

    private HttpHost getHttpHost() {
        host = new HttpHost(serviceAddress?.host, serviceAddress?.port, serviceAddress?.scheme);
    }

    private HttpHost getProxyHost() {
        if (null != proxyAddress) {
            return new HttpHost(proxyAddress?.host, proxyAddress?.port, proxyAddress?.scheme);
        }
        return null;
    }


    private CloseableHttpAsyncClient httpClient = null;

    boolean isClosed() {
        return null == httpClient || !httpClient.isRunning();
    }

    public <T> Future<T> execute(
            final Context context,
            final HttpUriRequest request,
            final HttpAsyncResponseConsumer<T> responseConsumer,
            final FutureCallback<T> callback) {

        HttpClientContext localContext = HttpClientContext.create();
        if (null != beforeRequest) {
            Closure beforeRequestClone = beforeRequest.rehydrate(this, this, this);
            beforeRequestClone.setResolveStrategy(Closure.DELEGATE_FIRST);
            beforeRequestClone(context, localContext, request)
        }

        return getHttpAsyncClient().execute(
                HttpAsyncMethods.create(getHttpHost(), request),
                responseConsumer,
                localContext, callback);
    }

    String customizerScriptFileName = null;

    private interface OnExecuteDelegate<T> {

        public Future<T> execute(
                Context context,
                HttpUriRequest request, FutureCallback<T> callback)

    }

    private interface ScriptsDelegate {
        //Connector lifecycle
        void init(Closure paramClosure);

        void release(Closure paramClosure);
        //Before call
        void beforeRequest(Closure paramClosure);

        //FutureCallback
        void onComplete(Closure paramClosure);

        void onFail(Closure paramClosure);
    }


    private Closure init = null;
    private Closure release = null;
    private Closure beforeRequest = null;
    private Closure onComplete = null;
    private Closure onFail = null;

    private static final String CUSTOMIZER_SCRIPT = "/org/forgerock/openicf/connectors/groovy/CustomizerScript.groovy";

    HttpAsyncClient getHttpAsyncClient() {
        if (null == httpClient) {
            synchronized (this) {
                if (null == httpClient) {
                    initialize()
                    Closure clone = init.rehydrate(this, this, this);
                    clone.setResolveStrategy(Closure.DELEGATE_FIRST);
                    HttpAsyncClientBuilder builder = HttpAsyncClients.custom()
                    clone(builder)
                    httpClient = builder.build();
                    httpClient.start();
                }
            }
        }
        return httpClient;
    }

    private void initialize() throws Exception {
        Class customizerClass = null;
        if (StringUtil.isBlank(customizerScriptFileName)) {
            URL url = getClass().getResource(CUSTOMIZER_SCRIPT);
            def source = new GroovyCodeSource(ResourceGroovyMethods.getText(url, getSourceEncoding()), url.toExternalForm(), "/groovy/script")
            source.cachable = false;
            customizerClass = getGroovyScriptEngine().getGroovyClassLoader().parseClass(source);
        } else {
            customizerClass = getGroovyScriptEngine().loadScriptByName(customizerScriptFileName)
        }

        customizerClass.metaClass.customize << { Closure cl ->
            init = null
            release = null
            beforeRequest = null
            onComplete = null
            onFail = null

            ScriptsDelegate delegate = new ScriptsDelegate() {

                void init(Closure paramClosure) {
                    init = paramClosure
                }

                void release(Closure paramClosure) {
                    release = paramClosure
                }

                void beforeRequest(Closure paramClosure) {
                    beforeRequest = paramClosure
                }

                void onComplete(Closure paramClosure) {
                    onComplete = paramClosure
                }

                void onFail(Closure paramClosure) {
                    onFail = paramClosure
                }
            }

            cl.setDelegate(new Reference(delegate));
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.call();
        }
        Binding binding = new Binding()
        Script scr = InvokerHelper.createScript(customizerClass, binding);
        binding.setVariable(LOGGER, getLogger(scr.getClass()));
        scr.run();
    }
}
