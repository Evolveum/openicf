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
package org.forgerock.openicf.salesforce.translators;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.openicf.salesforce.SalesforceConnection;
import org.forgerock.openicf.salesforce.annotation.Service;

public class TranslatorRegistry {

    private SalesforceConnection connection;

    /* Class: owner -> property -> translator */
    private Map<Class<?>, Map<String, IFieldTranslator<?, ?>>> translators;

    public TranslatorRegistry(SalesforceConnection connection) {
        this.connection = connection;

        translators = new HashMap<Class<?>, Map<String, IFieldTranslator<?, ?>>>();
    }

    public boolean register(Class<?> owner, String property, Class<? extends IFieldTranslator<?, ?>> type) {
        Map<String, IFieldTranslator<?, ?>> entry = translators.get(owner);
        if (entry == null) {
            entry = new HashMap<String, IFieldTranslator<?, ?>>();
            translators.put(owner, entry);
        }

        // Check for existing entry
        if (entry.containsKey(property)) {
            return false;
        }

        // Register new entry
        IFieldTranslator<?, ?> translator;
        try {
            translator = type.newInstance();
            entry.put(property, translator);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create translator for " + property, e);
        }
    }

    public IFieldTranslator<?, ?> getTranslator(Class<?> owner, String property) {
        Map<String, IFieldTranslator<?, ?>> entry = translators.get(owner);
        if (entry == null || !entry.containsKey(property)) {
            return null;
        }

        IFieldTranslator<?, ?> translator = entry.get(property);
        Class<?> clazz = translator.getClass();
        // Search for and inject service if applicable
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Service.class)) {
                try {
                    boolean access = field.isAccessible();
                    field.setAccessible(true);

                    Object value = field.get(translator);
                    if (value == null) {
                        field.set(translator, connection);
                    }
                    field.setAccessible(access);
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return translator;
    }

    public void dispose() {
        if (translators != null) {
            translators.clear();
            translators = null;
        }
    }

}
