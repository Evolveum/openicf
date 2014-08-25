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
 * 
 * Portions Copyrighted 2013 Forgerock
 * Portions Copyrighted 2011 Radovan Semancik (Evolveum)
 */
package org.identityconnectors.ldap.schema;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static org.identityconnectors.common.CollectionUtil.newCaseInsensitiveMap;
import static org.identityconnectors.common.CollectionUtil.newCaseInsensitiveSet;
import static org.identityconnectors.common.CollectionUtil.newReadOnlyList;
import static org.identityconnectors.ldap.LdapEntry.isDNAttribute;
import static org.identityconnectors.ldap.LdapUtil.addBinaryOption;
import static org.identityconnectors.ldap.LdapUtil.getStringAttrValue;
import static org.identityconnectors.ldap.LdapUtil.quietCreateLdapName;
import static org.identityconnectors.ldap.ADLdapUtil.objectGUIDtoString;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.ldap.ADUserAccountControl;
import org.identityconnectors.ldap.LdapConnection;
import org.identityconnectors.ldap.LdapConstants;
import org.identityconnectors.ldap.LdapEntry;
import org.identityconnectors.ldap.ObjectClassMappingConfig;

/**
 * The authoritative description of the mapping between the LDAP schema
 * and the connector schema.
 *
 * @author Andrei Badea
 */
public class LdapSchemaMapping {

    private static final Log log = Log.getLog(LdapSchemaMapping.class);

    // XXX
    // - which attrs returned by default? Currently only userApplications.
    // - return binary attrs by default too?
    // - type mapping.
    // - operations.
    // - groups.

    // XXX should the naming attribute be present in the schema (e.g. "cn" for account)?

    // XXX need a method like getAttributesToReturn(String[] wanted);
    // XXX need to check that (extended) naming attributes really exist.

    public static final ObjectClass ANY_OBJECT_CLASS = new ObjectClass(ObjectClassUtil.createSpecialName("ANY"));

    /**
     * The LDAP attribute to map to {@link Name} by default.
     */
    static final String DEFAULT_LDAP_NAME_ATTR = "entryDN";

    private final LdapConnection conn;
    private final Map<String, Set<String>> ldapClass2Effective = newCaseInsensitiveMap();

    private Schema schema;

    public LdapSchemaMapping(LdapConnection conn) {
        this.conn = conn;
    }

    public Schema schema() {
        if (schema == null) {
        	LdapSchemaBuilder builder = new LdapSchemaBuilder(conn);
        	if (conn.getConfiguration().getPasswordAttribute() != null) {
        		// This attribute is already exposed as _PASSWORD_, exposing it also using its
        		// native name would lead to inconsistencies.
        		builder.getIgnoredAttrs().add(conn.getConfiguration().getPasswordAttribute());
        	}
        	schema = builder.getSchema();
        }
        return schema;
    }

    private Set<String> getEffectiveLdapClasses(String ldapClass) {
        Set<String> result = ldapClass2Effective.get(ldapClass);
        if (result == null) {
            result = conn.createNativeSchema().getEffectiveObjectClasses(ldapClass);
            ldapClass2Effective.put(ldapClass, result);
        }
        return result;
    }

    /**
     * Returns the LDAP object classes to which the given framework object
     * class is mapped.
     */
    public List<String> getLdapClasses(ObjectClass oclass) {
        if (oclass.equals(ANY_OBJECT_CLASS)) {
            return emptyList();
        }
        ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
        if (oclassConfig != null) {
            return oclassConfig.getLdapClasses();
        }
        if (!ObjectClassUtil.isSpecial(oclass)) {
            return newReadOnlyList(oclass.getObjectClassValue());
        }
        throw new ConnectorException("Object class " + oclass.getObjectClassValue() + " is not mapped to an LDAP object class");
    }

    /**
     * Returns the LDAP object class to which the given framework object
     * class is mapped in a transitive manner, i.e., together with any superior
     * object classes, any superiors thereof, etc..
     */
    public Set<String> getEffectiveLdapClasses(ObjectClass oclass) {
        Set<String> result = newCaseInsensitiveSet();
        for (String ldapClass : getLdapClasses(oclass)) {
            result.addAll(getEffectiveLdapClasses(ldapClass));
        }
        return unmodifiableSet(result);
    }

    public List<String> getUserNameLdapAttributes(ObjectClass oclass) {
        ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
        if (oclassConfig != null) {
            return oclassConfig.getShortNameLdapAttributes();
        }
        return emptyList();
    }

    public String getLdapAttribute(ObjectClass oclass, String attrName, boolean transfer) {
        String result = null;
        if (AttributeUtil.namesEqual(Uid.NAME, attrName)) {
            result = getLdapUidAttribute(oclass);
        } else if (AttributeUtil.namesEqual(Name.NAME, attrName)) {
            result = getLdapNameAttribute(oclass);
        } else if (AttributeUtil.namesEqual(OperationalAttributes.PASSWORD_NAME, attrName)){
    		result = getLdapPasswordAttribute(oclass);
    	} 

        if (result == null && !AttributeUtil.isSpecialName(attrName)) {
            result = attrName;
        }
        
        if (result == null && OperationalAttributes.OPERATIONAL_ATTRIBUTE_NAMES.contains(attrName)) {
            if (oclass.equals(ObjectClass.ACCOUNT)){
               switch (conn.getServerType()) {
                   case MSAD_GC:
                   case MSAD:
                       result = ADUserAccountControl.MS_USR_ACCT_CTRL_ATTR;
                       break;
                   case MSAD_LDS:
                       //result = ADUserAccountControl.MSDS_USR_ACCT_CTRL_ATTR;
                       if (OperationalAttributeInfos.ENABLE.is(attrName)) {
                           result = LdapConstants.MS_DS_USER_ACCOUNT_DISABLED;
                       } else if (OperationalAttributeInfos.PASSWORD_EXPIRED.is(attrName)) {
                           result = LdapConstants.MS_DS_USER_PASSWORD_EXPIRED;
                       } else if (OperationalAttributeInfos.LOCK_OUT.is(attrName)) {
                           result = LdapConstants.MS_DS_USER_ACCOUNT_AUTOLOCKED;
                       }
                       break;
                   default:
                       log.warn("Special Attribute {0} of object class {1} is not mapped to an LDAP attribute",
                        attrName, oclass.getObjectClassValue());
               }
            }
        }

        if (result != null && transfer && conn.needsBinaryOption(result)) {
            result = addBinaryOption(result);
        }

        if (result == null && !oclass.equals(ANY_OBJECT_CLASS)) {
            log.warn("Attribute {0} of object class {1} is not mapped to an LDAP attribute",
                    attrName, oclass.getObjectClassValue());
        }
        return result;
    }

    /**
     * Returns the name of the LDAP attribute which corresponds to the given
     * attribute of the given object class, or null.
     */
    public String getLdapAttribute(ObjectClass oclass, Attribute attr) {
        return getLdapAttribute(oclass, attr.getName(), false);
    }

    /**
     * Returns the names of the LDAP attributes which correspond to the given
     * attribute names of the given object class. If {@code transfer} is {@code true},
     * the binary option will be added to the attributes which need it.
     */
    public Set<String> getLdapAttributes(ObjectClass oclass, Set<String> attrs, boolean transfer) {
        Set<String> result = newCaseInsensitiveSet();
        for (String attr : attrs) {
            String ldapAttr = getLdapAttribute(oclass, attr, transfer);
            if (ldapAttr != null) {
                result.add(ldapAttr);
            }
        }
        return result;
    }

    /**
     * Returns the LDAP attribute which corresponds to {@link Uid}. Should
     * never return null.
     */
    public String getLdapUidAttribute(ObjectClass oclass) {
        return conn.getConfiguration().getUidAttribute();
    }

    /**
     * Returns the LDAP attribute which corresponds to {@link Name} for the
     * given object class. Might return {@code null} if, for example, the
     * object class was not configured explicitly in the configuration.
     */
    public String getLdapNameAttribute(ObjectClass oclass) {
        return DEFAULT_LDAP_NAME_ATTR;
    }
    
    public String getLdapPasswordAttribute(ObjectClass oclass) {
    	return conn.getConfiguration().getPasswordAttribute();
    }

    /**
     * Creates a {@link Uid} for the given entry. It is assumed that the entry
     * contains the attribute returned by {@link #getLdapUidAttribute}.
     */
    public Uid createUid(ObjectClass oclass, LdapEntry entry) {
        return createUid(getLdapUidAttribute(oclass), entry.getAttributes());
    }

    public Uid createUid(ObjectClass oclass, String entryDN) {
        String ldapUidAttr = getLdapUidAttribute(oclass);
        if (isDNAttribute(ldapUidAttr)) {
            try{
                //we do an exact search to get the DN as normalized by the server
                NamingEnumeration<SearchResult> ne = conn.getInitialContext().search(entryDN, "objectclass=*", new SearchControls(SearchControls.OBJECT_SCOPE,0,0,null,false,false));
                // TODO: ne might be null if entry can not be read back (ACI issues for instances)
                SearchResult sr = ne.next();
                return new Uid(sr.getNameInNamespace());
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        } else {
            try {
                Attributes attributes = conn.getInitialContext().getAttributes(entryDN, new String[] { ldapUidAttr });
                return createUid(ldapUidAttr, attributes);
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        }
    }

    public Uid createUid(String ldapUidAttr, Attributes attributes) {
        String value = null;
        if (LdapConstants.MS_GUID_ATTR.equalsIgnoreCase(ldapUidAttr)){
            javax.naming.directory.Attribute attr = attributes.get(ldapUidAttr);
            if (attr != null) {
                value = objectGUIDtoString(attr);
            }
        }
        else{
            value = getStringAttrValue(attributes, ldapUidAttr);
        }
        if (value != null) {
            return new Uid(value);
        }
        throw new ConnectorException("No attribute named " + ldapUidAttr + " found in the search result");
    }

    /**
     * Creates a {@link Name} for the given entry. It is assumed that the entry
     * contains the attribute returned by {@link #getLdapNameAttribute}.
     */
    public Name createName(ObjectClass oclass, LdapEntry entry) {
        String ldapNameAttr = getLdapNameAttribute(oclass);
        if (!isDNAttribute(ldapNameAttr)) {
            // Not yet implemented.
            throw new UnsupportedOperationException("Name can only be mapped to the entry DN");
        }
        return new Name(entry.getDN().toString());
    }

    /**
     * Returns an empty attribute instead of <code>null</code> when <code>emptyWhenNotFound</code>
     * is <code>true</code>.
     */
    public Attribute createAttribute(ObjectClass oclass, String attrName, LdapEntry entry, boolean emptyWhenNotFound) {
        String ldapAttrNameForTransfer = getLdapAttribute(oclass, attrName, true);
        javax.naming.directory.Attribute ldapAttr = null;
        if (ldapAttrNameForTransfer != null) {
            ldapAttr = entry.getAttributes().get(ldapAttrNameForTransfer);
        }

        if (ldapAttr == null) {
            return emptyWhenNotFound ? AttributeBuilder.build(attrName, emptyList()) : null;
        }

        AttributeBuilder builder = new AttributeBuilder();
        builder.setName(attrName);
        try {
            NamingEnumeration<?> valEnum = ldapAttr.getAll();
            while (valEnum.hasMore()) {
                builder.addValue(valEnum.next());
            }
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
        return builder.build();
    }

    public String create(ObjectClass oclass, Name name, javax.naming.directory.Attributes initialAttrs) {
        LdapName entryName = quietCreateLdapName(getEntryDN(oclass, name));

        BasicAttributes ldapAttrs = new BasicAttributes();
        NamingEnumeration<? extends javax.naming.directory.Attribute> initialAttrEnum = initialAttrs.getAll();
        while (initialAttrEnum.hasMoreElements()) {
            ldapAttrs.put(initialAttrEnum.nextElement());
        }
        BasicAttribute objectClass = new BasicAttribute("objectClass");
        for (String ldapClass : conn.getSchemaMapping().getEffectiveLdapClasses(oclass)) {
            objectClass.add(ldapClass);
        }
        ldapAttrs.put(objectClass);

        log.ok("Creating LDAP subcontext {0} with attributes {1}", entryName, ldapAttrs);
        try {
            conn.getInitialContext().createSubcontext(entryName, ldapAttrs).close();
            return entryName.toString();
        } catch (NameAlreadyBoundException e){
            throw new AlreadyExistsException(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    public javax.naming.directory.Attribute encodeAttribute(ObjectClass oclass, Attribute attr) {
        if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
            throw new IllegalArgumentException("This method should not be used for password attributes");
        }

        String ldapAttrName = getLdapAttribute(oclass, attr.getName(), true);
        if (ldapAttrName == null) {
            return null;
        }

        final BasicAttribute ldapAttr = new BasicAttribute(ldapAttrName);
        List<Object> value = attr.getValue();
        if (value != null) {
            for (Object each : value) {
                ldapAttr.add(each);
            }
        }
        return ldapAttr;
    }

    public GuardedPasswordAttribute encodePassword(ObjectClass oclass, Attribute attr) {
        assert attr.is(OperationalAttributes.PASSWORD_NAME);

        String pwdAttrName = conn.getConfiguration().getPasswordAttribute();
        List<Object> value = attr.getValue();
        if (value != null) {
            for (Object each : value) {
                GuardedString password = (GuardedString) each;
                return GuardedPasswordAttribute.create(pwdAttrName, password);
            }
        }
        return GuardedPasswordAttribute.create(pwdAttrName);
    }

    public String getEntryDN(ObjectClass oclass, Name name) {
        String ldapNameAttr = getLdapNameAttribute(oclass);
        if (!isDNAttribute(ldapNameAttr)) {
            // Not yet implemented.
            throw new UnsupportedOperationException("Name can only be mapped to the entry DN");
        }
        return name.getNameValue();
    }

    public String rename(ObjectClass oclass, String entryDN, Name newName) {
        String newEntryDN = getEntryDN(oclass, newName);
        try {
            conn.getInitialContext().rename(entryDN, newEntryDN);
            return newEntryDN;
        } catch (NameAlreadyBoundException e){
            throw new AlreadyExistsException(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    public void removeNonReadableAttributes(ObjectClass oclass, Set<String> attrNames) {
        ObjectClassInfo oci = schema().findObjectClassInfo(oclass.getObjectClassValue());
        if (oci == null) {
            return;
        }

        Set<String> attrs = newCaseInsensitiveSet();
        Set<String> readableAttrs = newCaseInsensitiveSet();
        for (AttributeInfo info : oci.getAttributeInfo()) {
            String attrName = info.getName();
            attrs.add(attrName);
            if (info.isReadable()) {
                readableAttrs.add(attrName);
            }
        }
        for (Iterator<String> i = attrNames.iterator(); i.hasNext();) {
            String attrName = i.next();
            // Only remove the attribute if it is a known one. Otherwise
            // we could remove attributes that are readable, but not in the schema
            // (e.g., LDAP operational attributes).
            if (attrs.contains(attrName) && !readableAttrs.contains(attrName)) {
                i.remove();
            }
        }
    }
}
