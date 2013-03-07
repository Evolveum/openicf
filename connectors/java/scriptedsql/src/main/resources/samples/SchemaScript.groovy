/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
 * 
 * @author Gael Allioux <gael.allioux@forgerock.com>
 *
 */
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

// Parameters:
// The connector sends the following:
// action: a string describing the action ("SCHEMA" here)
// log: a handler to the Log facility
// builder: SchemaBuilder instance for the connector
//
// The connector will make the final call to builder.build()
// so the scipt just need to declare the different object types.

// This sample shows how to create 3 basic ObjectTypes: __ACCOUNT__, __GROUP__ and organization.
// Each of them contains one required attribute and normal String attributes


log.info("Entering "+action+" Script");

// Declare the __ACCOUNT__ attributes
// Make the uid required
uidAIB = new AttributeInfoBuilder("uid",String.class);
uidAIB.setRequired(true);

accAttrsInfo = new HashSet<AttributeInfo>();
accAttrsInfo.add(uidAIB.build());
accAttrsInfo.add(AttributeInfoBuilder.build("firstname", String.class));
accAttrsInfo.add(AttributeInfoBuilder.build("lastname", String.class));
accAttrsInfo.add(AttributeInfoBuilder.build("fullname", String.class));
accAttrsInfo.add(AttributeInfoBuilder.build("email", String.class));
accAttrsInfo.add(AttributeInfoBuilder.build("organization", String.class));
// Create the __ACCOUNT__ Object class
final ObjectClassInfo ociAccount = new ObjectClassInfoBuilder().setType("__ACCOUNT__").addAllAttributeInfo(accAttrsInfo).build();
builder.defineObjectClass(ociAccount);


// Declare the __GROUP__ attributes
// Make the gid required
gidAIB = new AttributeInfoBuilder("gid",String.class);
gidAIB.setRequired(true);

grpAttrsInfo = new HashSet<AttributeInfo>();
grpAttrsInfo.add(gidAIB.build());
grpAttrsInfo.add(AttributeInfoBuilder.build("name", String.class));
grpAttrsInfo.add(AttributeInfoBuilder.build("description", String.class));
// Create the __GROUP__ Object class
final ObjectClassInfo ociGroup = new ObjectClassInfoBuilder().setType("__GROUP__").addAllAttributeInfo(grpAttrsInfo).build();
builder.defineObjectClass(ociGroup);


// Declare the organization attributes
// Make the name required
nAIB = new AttributeInfoBuilder("name",String.class);
nAIB.setRequired(true);

orgAttrsInfo = new HashSet<AttributeInfo>();
orgAttrsInfo.add(nAIB.build());
orgAttrsInfo.add(AttributeInfoBuilder.build("description", String.class));
// Create the organization Object class
final ObjectClassInfo ociOrg = new ObjectClassInfoBuilder().setType("organization").addAllAttributeInfo(orgAttrsInfo).build();
builder.defineObjectClass(ociOrg);

log.info("Schema script done");