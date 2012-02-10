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

import java.util.Arrays;
import java.util.List;

import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

import org.forgerock.openicf.salesforce.annotation.ICAttribute;
import org.forgerock.openicf.salesforce.translators.EmptyTranslator;
import org.forgerock.openicf.salesforce.translators.IFieldTranslator;


public class MetaAttribute {

    private int priority;
    private ICAttribute meta;
    private MetaResource parent;
    private List<Flags> flags;

    public MetaAttribute(int priority, MetaResource parent, ICAttribute attribute) {
        this.priority = priority;
        this.parent = parent;
        this.meta = attribute;
        this.flags = Arrays.asList(attribute.flags());
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    public ICAttribute getMetaData() {
        return meta;
    }

    public MetaResource getParent() {
        return parent;
    }

    public boolean isQueriable() {
        return meta.queryable();
    }

    public boolean is(String name) {
        return getName().equals(name);
    }

    public String getName() {
        return meta.name();
    }

    public String getProperty() {
        if (meta.local().length() > 0) {
            String result = meta.local();
            return result.startsWith("this.") ? result : "this." + result;
        }
        return meta.property();
    }

    public Class<?> getAttributeType() {
        return meta.type();
    }

    public List<Flags> getFlags() {
        return flags;
    }

    public Class<? extends IFieldTranslator<?, ?>> getFieldTranslator() {
        Class<? extends IFieldTranslator<?, ?>> result = meta.translator();
        if (result.getName().equals(EmptyTranslator.class.getName())) {
            return null;
        }

        return result;
    }

    /**
     * @return
     */
    public boolean isMultiValued() {
        return flags.contains(Flags.MULTIVALUED);
    }

}
