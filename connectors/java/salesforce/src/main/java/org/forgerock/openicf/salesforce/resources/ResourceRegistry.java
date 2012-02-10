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

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openicf.salesforce.SalesforceConnection;
import org.forgerock.openicf.salesforce.SalesforceConnector;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

import org.forgerock.openicf.salesforce.annotation.ICResource;
import org.forgerock.openicf.salesforce.meta.MetaAttribute;
import org.forgerock.openicf.salesforce.meta.MetaResource;

public class ResourceRegistry {

    private Map<String, MetaResource> resources;

    public ResourceRegistry() {
        resources = new HashMap<String, MetaResource>();
    }

    public void initializeResources() {
        //TODO implement
    }

    public void initializeTestResources(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            ICResource resource = clazz.getAnnotation(ICResource.class);
            if (resource == null) continue;
            resources.put(resource.name().toLowerCase(), new MetaResource(resource, clazz));
        }
    }

    public void dispose() {
        if (resources != null) {
            resources.clear();
            resources = null;
        }
    }

    public AbstractResource<?> createResource(MetaResource resource, SalesforceConnection connection, Object entity) {
        Class<?> clazz = resource.getInstanceType();

        try {
            Constructor<?> constr = clazz.getConstructor(resource.getEntityType(), SalesforceConnection.class);
            return (AbstractResource<?>) constr.newInstance(entity, connection);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create resource", ex);
        }
    }

    public MetaResource findResource(String name) {
        return resources.get(name.toLowerCase());
    }

    public MetaAttribute findAttribute(String resourceName, String attributeName) {
        MetaResource resource = findResource(resourceName);
        return resource.find(attributeName);
    }

    public Set<MetaAttribute> findAttributes(String resourceName, String... attributeNames) {
        MetaResource resource = findResource(resourceName);
        return findAttributes(resource, attributeNames);
    }

    public Set<MetaAttribute> findAttributes(MetaResource resource, String... attributeNames) {
        if (attributeNames == null || attributeNames.length == 0) {
            return Collections.emptySet();
        }

        Set<MetaAttribute> results = new HashSet<MetaAttribute>(attributeNames.length);
        for (String name : attributeNames) {
            MetaAttribute attr = resource.find(name);
            if (attr != null) {
                results.add(attr);
            }
        }

        return results;
    }

    public Set<MetaAttribute> findAttributesIncludingSpecials(String resourceName, String... attributeNames) {
        MetaResource resource = findResource(resourceName);
        return findAttributesIncludingSpecials(resource, attributeNames);
    }

    public Set<MetaAttribute> findAttributesIncludingSpecials(MetaResource resource, String... attributeNames) {
        Set<MetaAttribute> results = new HashSet<MetaAttribute>();
        for (MetaAttribute attribute : resource.getAttributes()) {
            if (AttributeUtil.isSpecialName(attribute.getName())) {
                results.add(attribute);
            }
        }

        results.addAll(findAttributes(resource, attributeNames));
        return results;
    }

    public AttributeInfo getAttributeInfo(MetaAttribute attribute) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder(attribute.getName());
        builder.setType(attribute.getAttributeType());
        builder.setFlags(new HashSet<Flags>(attribute.getFlags()));
        return builder.build();
    }

    public Schema createFromRegistry() {
        SchemaBuilder builder = new SchemaBuilder(SalesforceConnector.class);
        for (MetaResource resource : resources.values()) {
            ObjectClassInfoBuilder classInfo = new ObjectClassInfoBuilder();
            classInfo.setType(resource.getName());
            classInfo.setContainer(resource.isContainer());
            for (MetaAttribute attr : resource.getAttributes()) {
                AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder(attr.getName());
                attrBuilder.setType(attr.getAttributeType());
                attrBuilder.setFlags(new HashSet<AttributeInfo.Flags>(attr.getFlags()));
                classInfo.addAttributeInfo(attrBuilder.build());
            }
            builder.defineObjectClass(classInfo.build());
        }

        return builder.build();
    }

}
