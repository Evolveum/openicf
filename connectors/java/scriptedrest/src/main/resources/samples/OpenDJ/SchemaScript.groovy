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

// This sample shows how to create 2 basic ObjectTypes: __ACCOUNT__ and __GROUP__.
// It works with OpenDJ 2.6 REST sample attribute:
// http://docs.forgerock.org/en/opendj/2.6.0/admin-guide/index/appendix-rest2ldap.html


log.info("Entering "+action+" Script");

// Declare the __ACCOUNT__ attributes
// _id
idAIB = new AttributeInfoBuilder("_id",String.class);
idAIB.setRequired(true);
idAIB.setCreateable(true);
idAIB.setMultiValued(false);
idAIB.setUpdateable(false);

// userName
userNameAIB = new AttributeInfoBuilder("userName",String.class);
userNameAIB.setCreateable(false);
userNameAIB.setMultiValued(false);
userNameAIB.setUpdateable(false);

// displayName
displayNameAIB = new AttributeInfoBuilder("displayName",String.class);
displayNameAIB.setRequired(true);
displayNameAIB.setMultiValued(false);

// group displayName
grpDisplayNameAIB = new AttributeInfoBuilder("displayName",String.class);
grpDisplayNameAIB.setMultiValued(false);
grpDisplayNameAIB.setCreateable(false);
grpDisplayNameAIB.setUpdateable(false);

// familyName
familyNameAIB = new AttributeInfoBuilder("familyName",String.class);
familyNameAIB.setRequired(true);
familyNameAIB.setMultiValued(false);

// givenName
givenNameAIB = new AttributeInfoBuilder("givenName",String.class);
givenNameAIB.setMultiValued(false);

// telephoneNumber
telephoneNumberAIB = new AttributeInfoBuilder("telephoneNumber",String.class);
telephoneNumberAIB.setMultiValued(false);

// emailAddress
emailAddressAIB = new AttributeInfoBuilder("emailAddress",String.class);
emailAddressAIB.setMultiValued(false);

// members
membersAIB = new AttributeInfoBuilder("members",String.class);
membersAIB.setMultiValued(true);

// groups
groupsAIB = new AttributeInfoBuilder("groups",String.class);
groupsAIB.setMultiValued(true);

//created
createdAIB = new AttributeInfoBuilder("created",String.class);
createdAIB.setCreateable(false);
createdAIB.setMultiValued(false);
createdAIB.setUpdateable(false);

//lastModified
lastModifiedAIB = new AttributeInfoBuilder("lastModified",String.class);
lastModifiedAIB.setCreateable(false);
lastModifiedAIB.setMultiValued(false);
lastModifiedAIB.setUpdateable(false);

accAttrsInfo = new HashSet<AttributeInfo>();
accAttrsInfo.add(idAIB.build());
accAttrsInfo.add(userNameAIB.build());
accAttrsInfo.add(displayNameAIB.build());
accAttrsInfo.add(familyNameAIB.build());
accAttrsInfo.add(givenNameAIB.build());
accAttrsInfo.add(telephoneNumberAIB.build());
accAttrsInfo.add(emailAddressAIB.build());
accAttrsInfo.add(groupsAIB.build());
accAttrsInfo.add(createdAIB.build());
accAttrsInfo.add(lastModifiedAIB.build());
accAttrsInfo.add(AttributeInfoBuilder.build("managerId", String.class));
accAttrsInfo.add(AttributeInfoBuilder.build("managerDisplayName", String.class));
// Create the __ACCOUNT__ Object class
final ObjectClassInfo ociAccount = new ObjectClassInfoBuilder().setType("__ACCOUNT__").addAllAttributeInfo(accAttrsInfo).build();
builder.defineObjectClass(ociAccount);

// __GROUP__ attributes
grpAttrsInfo = new HashSet<AttributeInfo>();
grpAttrsInfo.add(idAIB.build());
grpAttrsInfo.add(grpDisplayNameAIB.build());
grpAttrsInfo.add(createdAIB.build());
grpAttrsInfo.add(lastModifiedAIB.build());
grpAttrsInfo.add(membersAIB.build());
// Create the __GROUP__ Object class
final ObjectClassInfo ociGroup = new ObjectClassInfoBuilder().setType("__GROUP__").addAllAttributeInfo(grpAttrsInfo).build();
builder.defineObjectClass(ociGroup);

log.info("Schema script done");