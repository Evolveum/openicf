
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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2011 Radovan Semancik (Evolveum)
 * Portions Copyrighted 2013-2014 ForgeRock AS
 */
package org.identityconnectors.ldap.schema;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.ldap.LdapAttributeType;
import org.identityconnectors.ldap.LdapConnection;
import org.identityconnectors.ldap.LdapConnector;
import org.identityconnectors.ldap.LdapNativeSchema;
import org.identityconnectors.ldap.LdapUtil;
import org.identityconnectors.ldap.ObjectClassMappingConfig;
import org.identityconnectors.ldap.LdapConnection.ServerType;

class LdapSchemaBuilder {

    private static final Log log = Log.getLog(LdapSchemaBuilder.class);

    private final LdapConnection conn;
    private final LdapNativeSchema nativeSchema;
    private Schema schema;
    private Set<String> ignoredAttrs;

    public LdapSchemaBuilder(LdapConnection conn) {
        this.conn = conn;
        this.nativeSchema = conn.createNativeSchema();
        this.ignoredAttrs = CollectionUtil.newCaseInsensitiveSet();
    }
    
    public Set<String> getIgnoredAttrs() {
    	return ignoredAttrs;
    }

    public Schema getSchema() {
        if (schema == null) {
            buildSchema();
        }
        return schema;
    }

    private void buildSchema() {
        SchemaBuilder schemaBld = new SchemaBuilder(LdapConnector.class);

        for (ObjectClassMappingConfig oclassConfig : conn.getConfiguration().getObjectClassMappingConfigs().values()) {
            ObjectClass oclass = oclassConfig.getObjectClass();

            ObjectClassInfoBuilder objClassBld = new ObjectClassInfoBuilder();
            objClassBld.setType(oclass.getObjectClassValue());
            objClassBld.setContainer(oclassConfig.isContainer());
            objClassBld.addAllAttributeInfo(createAttributeInfos(oclassConfig.getLdapClasses()));
            objClassBld.addAllAttributeInfo(oclassConfig.getOperationalAttributes());

            ObjectClassInfo oci = objClassBld.build();
            schemaBld.defineObjectClass(oci);

            // XXX hack: these decisions should be made outside of LdapSchemaBuilder.
            if (!oci.is(ObjectClass.ACCOUNT_NAME)) {
                schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oci);
            }
            // Since we are not sure we can detect Sun DSEE correctly, only disable sync() for servers known not to support it.
            if (conn.getServerType() == ServerType.OPENDS) {
                schemaBld.removeSupportedObjectClass(SyncOp.class, oci);
            }
        }

        for (String ldapClass : nativeSchema.getStructuralObjectClasses()) {
            ObjectClassInfoBuilder objClassBld = new ObjectClassInfoBuilder();
            objClassBld.setType(ldapClass);
            objClassBld.setContainer(true); // Any LDAP object class can contain sub-entries.
            objClassBld.addAllAttributeInfo(createAttributeInfos(nativeSchema.getEffectiveObjectClasses(ldapClass)));

            ObjectClassInfo oci = objClassBld.build();
            schemaBld.defineObjectClass(oci);

            schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oci);
            // Since we are not sure we can detect Sun DSEE correctly, only disable sync() for servers known not to support it.
            if (conn.getServerType() == ServerType.OPENDS) {
                schemaBld.removeSupportedObjectClass(SyncOp.class, oci);
            }
        }
        
        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildRunWithUser(), CreateOp.class, UpdateOp.class, DeleteOp.class);
        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildRunWithPassword(), CreateOp.class, UpdateOp.class, DeleteOp.class);
        
        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildPageSize(), SearchOp.class);
        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsCookie(), SearchOp.class);
        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp.class);
        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildSortKeys(), SearchOp.class);
        
        ObjectClassInfoBuilder objClassBld = new ObjectClassInfoBuilder();
        objClassBld.setType(LdapUtil.SERVER_INFO_NAME);
        ObjectClassInfo oci = objClassBld.build();
        schemaBld.defineObjectClass(oci);
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oci);
        schemaBld.removeSupportedObjectClass(ResolveUsernameOp.class, oci);
        schemaBld.removeSupportedObjectClass(CreateOp.class, oci);
        schemaBld.removeSupportedObjectClass(UpdateAttributeValuesOp.class, oci);
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oci);
        schemaBld.removeSupportedObjectClass(SyncOp.class, oci);

        schema = schemaBld.build();
    }

    private Set<AttributeInfo> createAttributeInfos(Collection<String> ldapClasses) {
        Set<AttributeInfo> result = new HashSet<AttributeInfo>();

        Set<String> requiredAttrs = getRequiredAttributes(ldapClasses);
        Set<String> optionalAttrs = getOptionalAttributes(ldapClasses);
        // OpenLDAP's ipProtocol has MUST ( ... $ description ) MAY ( description )
        optionalAttrs.removeAll(requiredAttrs);
        
        requiredAttrs.removeAll(ignoredAttrs);
        optionalAttrs.removeAll(ignoredAttrs);

        addAttributeInfos(ldapClasses, requiredAttrs, EnumSet.of(Flags.REQUIRED), null, result);
        addAttributeInfos(ldapClasses, optionalAttrs, null, null, result);

        return result;
    }

    private Set<String> getRequiredAttributes(Collection<String> ldapClasses) {
        Set<String> result = new HashSet<String>();
        for (String ldapClass : ldapClasses) {
            result.addAll(nativeSchema.getRequiredAttributes(ldapClass));
        }
        return result;
    }

    private Set<String> getOptionalAttributes(Collection<String> ldapClasses) {
        Set<String> result = new HashSet<String>();
        for (String ldapClass : ldapClasses) {
            result.addAll(nativeSchema.getOptionalAttributes(ldapClass));
        }
        return result;
    }

    private void addAttributeInfos(Collection<String> ldapClasses, Set<String> attrs, Set<Flags> add, Set<Flags> remove, Set<AttributeInfo> toSet) {
        for (String attr : attrs) {
            addAttributeInfo(ldapClasses, attr, attr, add, remove, toSet);
        }
    }

    private void addAttributeInfo(Collection<String> ldapClasses, String ldapAttrName, String realName, Set<Flags> add, Set<Flags> remove, Set<AttributeInfo> toSet) {
        LdapAttributeType attrDesc = nativeSchema.getAttributeDescription(ldapAttrName);
        if (attrDesc != null) {
            toSet.add(attrDesc.createAttributeInfo(realName, add, remove));
        } else {
            log.warn("Could not find attribute {0} in object classes {1}", ldapAttrName, ldapClasses);
        }
    }
}
