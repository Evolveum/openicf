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
package org.forgerock.openicf.salesforce.resources;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.forgerock.openicf.salesforce.SalesforceConnection;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;

import org.forgerock.openicf.salesforce.meta.MetaResource;


public class ResourceFactory {

    static final String CANNOT_CREATE = "Cannot create entity.";
    static final String NOT_ACCESSIBLE = CANNOT_CREATE + " Method '%s' is not accessible";
    static final String NOT_IMPLEMENTED = CANNOT_CREATE + " Method '%s' has not been implemented.";


    public <T> T createFromMetaResource(SalesforceConnection connection, MetaResource resource, String identifier) {
        Class<?> type = resource.getEntityType();
        String methodName = "create" + type.getSimpleName();
        RuntimeException exception = null;
        T result = null;
        try {
            Method createMethod = getClass().getMethod(methodName, SalesforceConnection.class, String.class);
            result = (T) createMethod.invoke(this, connection, identifier);
        } catch (SecurityException e) {
            exception = new RuntimeException(err(NOT_ACCESSIBLE, methodName), e);
        } catch (NoSuchMethodException e) {
            exception = new UnsupportedOperationException(err(NOT_IMPLEMENTED, methodName), e);
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = new RuntimeException(err(NOT_ACCESSIBLE, methodName), e);
        } catch (InvocationTargetException e) {
            exception = new RuntimeException(CANNOT_CREATE, e);
        }

        if (exception != null) {
            throw exception;
        }

        return result;
    }


    private void checkExists(Object result) {
        if (result != null && ((Long) result) > 0) {
            throw new AlreadyExistsException();
        }
    }

    private static Object singleResult(Collection<?> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.iterator().next();
    }


    private String err(String key, String... args) {
        return String.format(key, args);
    }
}
