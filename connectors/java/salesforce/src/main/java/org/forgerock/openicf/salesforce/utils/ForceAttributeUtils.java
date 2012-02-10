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
package org.forgerock.openicf.salesforce.utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;

import org.forgerock.openicf.salesforce.meta.MetaAttribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

public class ForceAttributeUtils {

    private ForceAttributeUtils() {
        // util class
    }

    public static void parseDescribe(Map describe, SchemaBuilder schemaBuilder) {
        Object name = describe.get("name");
        ObjectClassInfoBuilder ocBuilder = new ObjectClassInfoBuilder();
        if (name instanceof String) {
            if ("User".equalsIgnoreCase((String) name)) {
                ocBuilder.setType(ObjectClass.ACCOUNT_NAME);
            } else if ("Group".equalsIgnoreCase((String) name)) {
                ocBuilder.setType(ObjectClass.GROUP_NAME);
            } else {
                ocBuilder.setType((String) name);
            }
            ocBuilder.addAttributeInfo(Name.INFO);
        } else {
            return;
        }
        Object fields = describe.get("fields");

        if (fields instanceof List) {
            for (Map<String, Object> field : (List<Map<String, Object>>) fields) {
                Object fieldName = field.get("name");
                if (fieldName instanceof String) {
                    if ("Id".equalsIgnoreCase((String) fieldName)) {
                        //__UID__ Attribute
                        continue;
                    }
                    Object idLookup = field.get("idLookup");
                    if (Boolean.valueOf((Boolean) idLookup)) {
                        //__NAME__ Attribute
                        continue;
                    }
                    AttributeInfoBuilder attributeInfoBuilder = new AttributeInfoBuilder((String) fieldName);
                    //NOT_UPDATEABLE
                    Object updateable = field.get("updateable");
                    if ((updateable != null) && !Boolean.valueOf((Boolean) updateable)) {
                        attributeInfoBuilder.setUpdateable(false);
                    }
                    //NOT_CREATABLE
                    Object createable = field.get("createable");
                    if ((createable != null) && !Boolean.valueOf((Boolean) createable)) {
                        attributeInfoBuilder.setCreateable(false);
                    }
                    //REQUIRED
                    Object nillable = field.get("nillable");
                    if ((nillable != null) && !Boolean.valueOf((Boolean) nillable)) {
                        attributeInfoBuilder.setRequired(true);
                    }
                    /*
                    MULTIVALUED,
                    NOT_READABLE,
                    NOT_RETURNED_BY_DEFAULT
                    */
                    ocBuilder.addAttributeInfo(attributeInfoBuilder.build());
                }

            }
        }
        ObjectClassInfo objectClassInfo = ocBuilder.build();
        schemaBuilder.defineObjectClass(objectClassInfo);
        Object updateable = describe.get("updateable");
        if ((updateable != null) && !Boolean.valueOf((Boolean) updateable)) {
            schemaBuilder.removeSupportedObjectClass(UpdateOp.class, objectClassInfo);
        }
        //Object queryable = describe.get("queryable");
        //Object retrieveable = describe.get("retrieveable");
        Object searchable = describe.get("searchable");
        if ((searchable != null) && !Boolean.valueOf((Boolean) searchable)) {
            schemaBuilder.removeSupportedObjectClass(SearchOp.class, objectClassInfo);
        }
        Object createable = describe.get("createable");
        if ((createable != null) && !Boolean.valueOf((Boolean) createable)) {
            schemaBuilder.removeSupportedObjectClass(CreateOp.class, objectClassInfo);
        }
        Object deletable = describe.get("deletable");
        if ((deletable != null) && !Boolean.valueOf((Boolean) deletable)) {
            schemaBuilder.removeSupportedObjectClass(DeleteOp.class, objectClassInfo);
        }
    }

    public static Object getAttributeValue(Attribute data, MetaAttribute info) {
        if (info.isMultiValued()) {
            return data.getValue();
        } else if (Integer.class.equals(info.getAttributeType()) || int.class.equals(info.getAttributeType())) {
            return AttributeUtil.getIntegerValue(data);
        } else if (String.class.equals(info.getAttributeType()) || Character.class.equals(info.getAttributeType()) || char.class.equals(info.getAttributeType())) {
            return AttributeUtil.getStringValue(data);
        } else if (Long.class.equals(info.getAttributeType()) || long.class.equals(info.getAttributeType())) {
            return AttributeUtil.getLongValue(data);
        } else if (BigDecimal.class.equals(info.getAttributeType())) {
            return AttributeUtil.getBigDecimalValue(data);
        } else if (Boolean.class.equals(info.getAttributeType()) || boolean.class.equals(info.getAttributeType())) {
            return AttributeUtil.getBooleanValue(data);
        } else if (Double.class.equals(info.getAttributeType()) || double.class.equals(info.getAttributeType())) {
            return AttributeUtil.getDoubleValue(data);
        } else {
            return AttributeUtil.getAsStringValue(data);
        }
    }

}
