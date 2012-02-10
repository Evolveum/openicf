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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.Map.Entry;


import org.forgerock.openicf.salesforce.SalesforceConnection;
import org.forgerock.openicf.salesforce.annotation.ICResource;
import org.forgerock.openicf.salesforce.meta.MetaAttribute;
import org.forgerock.openicf.salesforce.meta.MetaResource;
import org.forgerock.openicf.salesforce.translators.IFieldTranslator;

public abstract class AbstractResource<T extends Serializable> {

    public static final String SELF_REFERENCE = "this";

    private boolean modified = false;

    private MetaResource resource;
    private WeakReference<T> entityRef;
    private WeakReference<SalesforceConnection> connectionRef;

    public AbstractResource(T entity, SalesforceConnection connection) {
        this.entityRef = new WeakReference<T>(entity);
        this.connectionRef = new WeakReference<SalesforceConnection>(connection);

        // Create meta resource
        ICResource res = getClass().getAnnotation(ICResource.class);
        resource = new MetaResource(res, (Class<? extends AbstractResource<?>>) getClass());

        registerTranslators();
    }

    /**
     * This method can be used to initialize lazy loaded fields.
     */
    public void initLazyLoadedFields(Set<MetaAttribute> attributes) {
        // noop
    }

    private void registerTranslators() {
        SalesforceConnection conn = getConnection();

        for (Entry<String, Class<? extends IFieldTranslator<?, ?>>> entry : resource.getFieldTranslators().entrySet()) {
            conn.registerTranslator(getClass(), entry.getKey(), entry.getValue());
        }
    }

    public String getIdentifier() {
        String result = null;

        return result;
    }

    /**
     * Returns the weak referenced entity. If the entity has been
     * garbage collected, this method may return null.
     *
     * @return T the entity.
     */
    public T getEntity() {
        T result = entityRef.get();
        if (result == null) {
            throw new IllegalStateException("Entity was null. Probably garbage collected..");
        }

        return result;
    }

    /**
     * Returns the weak referenced unsaved entity. If the entity has been
     * garbage collected, this method may return null.
     *
     * @return
     */
    public T getUnsavedEntity() {
        return getEntity();
    }

    /**
     * Returns the weak referenced connection. If the connection has
     * been garbage collected, this method may return null.
     *
     * @return {@link SalesforceConnection} the connection.
     */
    protected SalesforceConnection getConnection() {
        SalesforceConnection result = connectionRef.get();
        if (result == null) {
            throw new IllegalStateException("Connection was null. Must have been garbage collected.");
        }

        return result;
    }


    public Object getProperty(String property) {
        Object value = internalGetProperty(property);
        IFieldTranslator translator = internalGetFieldTranslator(property);
        return translator == null ? value : translator.fromInternal(value);
    }

    public void setProperty(String property, Object value) {
        Object translatedValue = null;
        IFieldTranslator translator = internalGetFieldTranslator(property);
        if (translator != null) {
            translatedValue = translator.fromExternal(value);
        } else {
            translatedValue = value;
        }

        internalSetProperty(property, translatedValue);
    }

    private Object internalGetProperty(String property) throws RuntimeException {
        return null;
    }

    private void internalSetProperty(String property, Object value) throws RuntimeException {

    }

    /**
     * This method is called during the {@link #setProperty(String, Object)} method
     * to verify if the property should actually be changed or not. Subclasses may
     * override this method to provide additional behavior.
     * <p>
     * If this method returns <code>false</code>, then the property will not be
     * changed.
     * </p>
     *
     * @param property the property being changed.
     * @param oldValue the old value of the property.
     * @param newValue the new value of the property.
     * @return <code>true</code> if a change occurred, <code>false</code> otherwise.
     */
    protected boolean verifyChange(String property, Object oldValue, Object newValue) {
        if ((oldValue == null) && (newValue == null)) {
            return false;
        }
        if (((oldValue == null) && (newValue != null)) || ((oldValue != null) && (newValue == null))) {
            return true;
        }

        return !oldValue.equals(newValue);
    }

    /**
     * This method is called before any modification is performed on
     * the underlying entity. Clients that requires special processing
     * in this case should override this method.
     *
     * @param property the property being changed.
     */
    protected void beforeModification(String property) {
        // noop
    }

    /**
     * Call this method after all modifications has been
     * committed to the entity. This method will reset the
     * internal modification flag. Sub-classes can override
     * this method to provide special processing, but should
     * make sure to call super in order to update the state.
     */
    public void modificationFinished() {
        modified = false;
    }

    private IFieldTranslator<?, ?> internalGetFieldTranslator(String property) {
        SalesforceConnection conn = getConnection();
        if (conn != null) {
            return conn.getTranslator(getClass(), property);
        }

        return null;
    }

}
