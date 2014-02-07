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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.identityconnectors.framework.api.operations.APIOperation;

/**
 * Delegating timeout proxy that selects the appropriate timeout handler
 * depending on the method.
 */
public class DelegatingTimeoutProxy implements InvocationHandler {

    /**
     * The underlying operation that we are providing a timeout for
     */
    private final Object target;

    /**
     * The timeout
     */
    private final long timeoutMillis;

    /**
     * The buffer size
     */
    private final int bufferSize;

    /**
     * Create a new MethodTimeoutProxy.
     *
     * @param target
     *            The object we are wrapping
     * @param timeoutMillis
     */
    public DelegatingTimeoutProxy(Object target, long timeoutMillis, int bufferSize) {
        this.target = target;
        this.timeoutMillis = timeoutMillis;
        this.bufferSize = bufferSize;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable {

        // do not timeout equals, hashCode, toString
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }

        // figure out the actual handler that we want to delegate to
        InvocationHandler handler = null;

        // if this is as stream handler method, we need the
        // buffered results proxy (if configured)
        if (isStreamHandlerMethod(method)) {
            if (timeoutMillis != APIOperation.NO_TIMEOUT || bufferSize != 0) {
                handler = new BufferedResultsProxy(target, bufferSize, timeoutMillis);
            }
        }
        // otherwise it's a basic timeout proxy
        else {
            if (timeoutMillis != APIOperation.NO_TIMEOUT) {
                // everything else is a general purpose timeout proxy
                handler = new MethodTimeoutProxy(target, timeoutMillis);
            }
        }

        // delegate to the timeout handler if specified
        if (handler != null) {
            return handler.invoke(proxy, method, args);
        }
        // otherwise, pass the call directly to the object
        else {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    private boolean isStreamHandlerMethod(Method method) {
        for (Class<?> paramType : method.getParameterTypes()) {
            if (StreamHandlerUtil.isAdaptableToObjectStreamHandler(paramType)) {
                return true;
            }
        }
        return false;
    }
}
