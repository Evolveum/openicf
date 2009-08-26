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
 */
package org.identityconnectors.ldap;

import static java.util.Collections.unmodifiableSet;
import static org.identityconnectors.common.CollectionUtil.newCaseInsensitiveMap;
import static org.identityconnectors.common.CollectionUtil.newCaseInsensitiveSet;
import static org.identityconnectors.ldap.LdapUtil.addStringAttrValues;
import static org.identityconnectors.ldap.LdapUtil.attrNameEquals;
import static org.identityconnectors.ldap.LdapUtil.getStringAttrValue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

/**
 * Implements {@link LdapNativeSchema} by reading it from the server.
 */
public class ServerNativeSchema implements LdapNativeSchema {

    // The LDAP directory attributes to expose as framework attributes.
    private static final Set<String> LDAP_DIRECTORY_ATTRS;

    private final LdapConnection conn;
    private final DirContext schemaCtx;

    private final Set<String> structuralLdapClasses = newCaseInsensitiveSet();
    private final Map<String, Set<String>> ldapClass2MustAttrs = newCaseInsensitiveMap();
    private final Map<String, Set<String>> ldapClass2MayAttrs = newCaseInsensitiveMap();
    private final Map<String, Set<String>> ldapClass2Sup = newCaseInsensitiveMap();

    private final Map<String, LdapAttributeType> attrName2Type = newCaseInsensitiveMap();

    static {
        LDAP_DIRECTORY_ATTRS = newCaseInsensitiveSet();
        LDAP_DIRECTORY_ATTRS.add("createTimestamp");
        LDAP_DIRECTORY_ATTRS.add("modifyTimestamp");
        LDAP_DIRECTORY_ATTRS.add("creatorsName");
        LDAP_DIRECTORY_ATTRS.add("modifiersName");
        LDAP_DIRECTORY_ATTRS.add("entryDN");
    }

    public ServerNativeSchema(LdapConnection conn) throws NamingException {
        this.conn = conn;
        schemaCtx = conn.getInitialContext().getSchema("");
        try {
            initObjectClasses();
            initAttributeDescriptions();
        } finally {
            schemaCtx.close();
        }
    }

    public Set<String> getStructuralObjectClasses() {
        return unmodifiableSet(structuralLdapClasses);
    }

    public Set<String> getRequiredAttributes(String ldapClass) {
        return getAttributes(ldapClass, true);
    }

    public Set<String> getOptionalAttributes(String ldapClass) {
        return getAttributes(ldapClass, false);
    }

    private Set<String> getAttributes(String ldapClass, boolean required) {
        Set<String> result = newCaseInsensitiveSet();
        Queue<String> queue = new LinkedList<String>();
        Set<String> visited = new HashSet<String>();
        queue.add(ldapClass);

        while (!queue.isEmpty()) {
            String current = queue.remove();
            if (!visited.contains(current)) {
                visited.add(current);
                Set<String> attrs = required ? ldapClass2MustAttrs.get(current) : ldapClass2MayAttrs.get(current);
                if (attrs != null) {
                    result.addAll(attrs);
                }
                Set<String> supClasses = ldapClass2Sup.get(current);
                if (supClasses != null) {
                    queue.addAll(supClasses);
                }
            }
        }

        return result;
    }

    public Set<String> getEffectiveObjectClasses(String ldapClass) {
        Set<String> result = newCaseInsensitiveSet();
        Queue<String> classQueue = new LinkedList<String>();
        classQueue.add(ldapClass);

        while (!classQueue.isEmpty()) {
            String classToVisit = classQueue.remove();
            if (!result.contains(classToVisit)) {
                result.add(classToVisit);
                Set<String> supClasses = ldapClass2Sup.get(classToVisit);
                if (supClasses != null) {
                    classQueue.addAll(supClasses);
                }
            }
        }

        return result;
    }

    public LdapAttributeType getAttributeDescription(String ldapAttrName) {
        return attrName2Type.get(ldapAttrName);
    }

    private void initObjectClasses() throws NamingException {
        DirContext objClassCtx = (DirContext) schemaCtx.lookup("ClassDefinition");
        NamingEnumeration<NameClassPair> objClassEnum = objClassCtx.list("");
        while (objClassEnum.hasMore()) {
            String objClassName = objClassEnum.next().getName();
            Attributes attrs = objClassCtx.getAttributes(objClassName);

            boolean abstractAttr = "true".equals(getStringAttrValue(attrs, "ABSTRACT"));
            boolean structuralAttr = "true".equals(getStringAttrValue(attrs, "STRUCTURAL"));
            boolean auxiliaryAttr = "true".equals(getStringAttrValue(attrs, "AUXILIARY"));
            boolean structural = structuralAttr || !(abstractAttr || auxiliaryAttr);

            Set<String> mustAttrs = newCaseInsensitiveSet();
            addStringAttrValues(attrs, "MUST", mustAttrs);
            Set<String> mayAttrs = newCaseInsensitiveSet();
            addStringAttrValues(attrs, "MAY", mayAttrs);

            // The objectClass attribute must not be required, since it is handled internally by the connector.
            if (mustAttrs.remove("objectClass")) {
                mayAttrs.add("objectClass");
            }

            Set<String> supClasses = newCaseInsensitiveSet();
            addStringAttrValues(attrs, "SUP", supClasses);
            if (structural && supClasses.isEmpty()) {
                // Hack for OpenDS, whose "referral" object class does not specify SUP.
                supClasses.add("top");
            }

            Set<String> names = newCaseInsensitiveSet();
            addStringAttrValues(attrs, "NAME", names);
            for (String name : names) {
                if (structural) {
                    structuralLdapClasses.addAll(names);
                }
                ldapClass2MustAttrs.put(name, mustAttrs);
                ldapClass2MayAttrs.put(name, mayAttrs);
                ldapClass2Sup.put(name, supClasses);
            }
        }
    }

    private void initAttributeDescriptions() throws NamingException {
        DirContext attrsCtx = (DirContext) schemaCtx.lookup("AttributeDefinition");
        NamingEnumeration<NameClassPair> attrsEnum = attrsCtx.list("");
        while (attrsEnum.hasMore()) {
            String attrName = attrsEnum.next().getName();
            Attributes attrs = attrsCtx.getAttributes(attrName);

            boolean singleValue = "true".equals(getStringAttrValue(attrs, "SINGLE-VALUE"));
            boolean noUserModification = "true".equals(getStringAttrValue(attrs, "NO-USER-MODIFICATION"));
            String usage = getStringAttrValue(attrs, "USAGE");
            boolean userApplications = "userApplications".equals(usage) || usage == null;

            Set<String> names = newCaseInsensitiveSet();
            addStringAttrValues(attrs, "NAME", names);
            for (String name : names) {
                // The objectClass attribute must not be writable, since it is handled internally by the connector.
                boolean objectClass = attrNameEquals(name, "objectClass");
                boolean binary = conn.isBinarySyntax(attrName);

                Class<?> type;
                if (binary) {
                    type = byte[].class;
                } else {
                    type = String.class;
                }
                Set<Flags> flags = EnumSet.noneOf(Flags.class);
                if (!singleValue) {
                    flags.add(Flags.MULTIVALUED);
                }
                if (noUserModification || objectClass) {
                    flags.add(Flags.NOT_CREATABLE);
                    flags.add(Flags.NOT_UPDATEABLE);
                }
                // XXX perhaps this should be true for binary attributes too.
                if (!userApplications) {
                    flags.add(Flags.NOT_RETURNED_BY_DEFAULT);
                }
                attrName2Type.put(name, new LdapAttributeType(type, flags));
            }
        }

        for (String dirAttrName : LDAP_DIRECTORY_ATTRS) {
            attrName2Type.put(dirAttrName, new LdapAttributeType(String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE, Flags.NOT_RETURNED_BY_DEFAULT)));
        }
    }
}
