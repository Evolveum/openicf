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
package org.forgerock.openicf.salesforce.meta;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.openicf.salesforce.annotation.ICAttribute;
import org.forgerock.openicf.salesforce.annotation.ICResource;
import org.forgerock.openicf.salesforce.translators.IFieldTranslator;


public class MetaResource {

    private Class<?> clazz;
    private ICResource meta;
    private Map<String, MetaAttribute> attributes;

    public MetaResource(ICResource resource, Class<?> clazz) {
        this.meta = resource;
        this.clazz = clazz;
        this.attributes = new HashMap<String, MetaAttribute>(resource.attributes().length);

        processAttributes();
    }

    /**
     *
     */
    private void processAttributes() {
        ICAttribute[] attrs = meta.attributes();
        for (int i = 0; i < attrs.length; i++) {
            ICAttribute a = attrs[i];
            attributes.put(a.name(), new MetaAttribute(i, this, a));
        }
    }

    public ICResource getICResource() {
        return meta;
    }

    public Class<?> getInstanceType() {
        return clazz;
    }

    /**
     * @return the instanceName
     */
    public String getInstanceName() {
        return clazz.getName();
    }

    public String getSimpleInstanceName() {
        return getInstanceName().substring(getInstanceName().lastIndexOf(".") + 1);
    }

    public String getName() {
        return meta.name();
    }

    public Class<? extends Serializable> getEntityType() {
        return meta.type();
    }

    public boolean isContainer() {
        return meta.isContainer();
    }

    public MetaAttribute find(String name) {
        return attributes.get(name);
    }

    public Collection<MetaAttribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes.values());
    }

    public Map<String, Class<? extends IFieldTranslator<?, ?>>> getFieldTranslators() {
        Map<String, Class<? extends IFieldTranslator<?, ?>>> results = new HashMap<String, Class<? extends IFieldTranslator<?, ?>>>();
        for (MetaAttribute ma : attributes.values()) {
            Class<? extends IFieldTranslator<?, ?>> clazz = ma.getFieldTranslator();
            if (clazz != null) {
                results.put(ma.getProperty(), clazz);
            }
        }

        return results;
    }

}
