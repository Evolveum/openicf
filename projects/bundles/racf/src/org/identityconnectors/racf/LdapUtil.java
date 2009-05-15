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
package org.identityconnectors.racf;

import static org.identityconnectors.racf.RacfConstants.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.LimitExceededException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;

class LdapUtil {
    private final Pattern               _connectionPattern  = Pattern.compile("racfuserid=(.*)\\+racfgroupid=(.*),.*");

    private RacfConnector               _connector;
    private Schema                      _schema;

    public LdapUtil(RacfConnector connector) {
        _connector = connector;
        _schema = _connector.schema();
    }

    public Uid createViaLdap(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        if (objectClass.equals(RacfConnector.RACF_CONNECTION)) {
            try {
                Name name = AttributeUtil.getNameFromAttributes(attrs);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(name.getNameValue(), null);
                return new Uid(name.getNameValue());
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        } else if (objectClass.equals(ObjectClass.ACCOUNT)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            try {
                Attribute enabled = attributes.remove(ATTR_LDAP_ENABLED);
                Attribute groups = attributes.remove(ATTR_LDAP_GROUPS);
                Attribute groupOwners = attributes.remove(ATTR_LDAP_GROUP_OWNERS);
                
                // Enabled is mapped into racfAttribute:RESUME or REVOKE
                //
                if (enabled!=null) {
                    Attribute attributesAttribute = attributes.get(ATTR_LDAP_ATTRIBUTES);
                    if (attributesAttribute==null) {
                        attributesAttribute = AttributeBuilder.build(ATTR_LDAP_ATTRIBUTES, new LinkedList<Object>());
                    }
                    List<Object> value = attributesAttribute.getValue();
                    if (value==null)
                        value = new LinkedList<Object>();
                    if (AttributeUtil.getBooleanValue(enabled))
                        value.add("RESUME");
                    else
                        value.add("REVOKE");
                    attributesAttribute = AttributeBuilder.build(ATTR_LDAP_ATTRIBUTES, value);
                    attributes.put(ATTR_LDAP_ATTRIBUTES, attributesAttribute);
                }

                String id = name.getNameValue();
                Uid uid = _connector.createUidFromName(objectClass, id);
                Map<String, Attribute> newAttributes = new HashMap<String, Attribute>(attributes);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(_connector.createUidFromName(objectClass, name.getNameValue()).getUidValue(), createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                if (groups!=null)
                    _connector.setGroupMembershipsForUser(id, groups, groupOwners);
                return uid;
            } catch (NamingException e) {
                if (e.toString().contains("INVALID USER"))
                    throw new ConnectorException(e);
                else
                    throw new AlreadyExistsException();
            }
        } else if (objectClass.equals(ObjectClass.GROUP)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            try {
                Attribute members = attributes.remove(ATTR_LDAP_MEMBERS);
                Attribute groupOwners = attributes.remove(ATTR_LDAP_GROUP_OWNERS);

                String id = name.getNameValue();
                Uid uid = _connector.createUidFromName(objectClass, id);
                Map<String, Attribute> newAttributes = new HashMap<String, Attribute>(attributes);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(_connector.createUidFromName(objectClass, name.getNameValue()).getUidValue(), createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                if (members!=null)
                    _connector.setGroupMembershipsForGroups(id, members, groupOwners);
                return uid;
            } catch (NamingException e) {
                if (e.toString().contains("INVALID GROUP"))
                    throw new ConnectorException(e);
                else
                    throw new AlreadyExistsException();
            }
        } else {
            throw new IllegalArgumentException("TODO");
        }
    }

    public void deleteViaLdap(ObjectClass objectClass, Uid uid) {
        try {
            ((RacfConnection)_connector.getConnection()).getDirContext().destroySubcontext(_connector.createUidFromName(objectClass, uid.getUidValue()).getUidValue());
        } catch (NamingException e) {
            if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
                if (e.toString().contains("INVALID USER"))
                    throw new UnknownUidException();
                else
                    throw new ConnectorException(e);
            } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
                if (e.toString().contains("INVALID GROUP"))
                    throw new UnknownUidException();
                else
                    throw new ConnectorException(e);
            } else {
                throw ConnectorException.wrap(e);
            }
        }
    }

    public List<String> getMembersOfGroupViaLdap(String group) {
        SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, null, true, true);
        List<String> groups = new LinkedList<String>();
        try {
            String search = "profileType=connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
            NamingEnumeration<SearchResult> connections = ((RacfConnection)_connector.getConnection()).getDirContext().search(search, "racfgroupid="+group, subTreeControls);
            while (connections.hasMore()) {
                SearchResult userRoot = connections.next();
                String name = userRoot.getNameInNamespace();
                Matcher matcher = _connectionPattern.matcher(name);
                if (matcher.matches()) {
                    groups.add(matcher.group(2));
                } else {
                    throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.PATTERN_FAILED, name));
                }
            }
            return groups;
        } catch (LimitExceededException e) {
            //TODO: cope with this
            throw ConnectorException.wrap(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    public List<String> getGroupsForUserViaLdap(String user) {
        SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, null, true, true);
        List<String> groups = new LinkedList<String>();
        try {
            String search = "profileType=connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
            NamingEnumeration<SearchResult> connections = ((RacfConnection)_connector.getConnection()).getDirContext().search(search, "racfuserid="+user, subTreeControls);
            while (connections.hasMore()) {
                SearchResult userRoot = connections.next();
                String name = userRoot.getNameInNamespace();
                Matcher matcher = _connectionPattern.matcher(name);
                if (matcher.matches()) {
                    groups.add(matcher.group(2));
                } else {
                    throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.PATTERN_FAILED, name));
                }
            }
            //TODO: need to include defaultGroup as 0th element, so that it is known
            return groups;
        } catch (LimitExceededException e) {
            //TODO: cope with this
            throw ConnectorException.wrap(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    public List<String> getUsersViaLdap(String query) {
        SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, null, true, true);
        List<String> userNames = new LinkedList<String>(); 
        try {
            String search = "profileType=user,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
            NamingEnumeration<SearchResult> users = ((RacfConnection)_connector.getConnection()).getDirContext().search(search, getLdapFilterForFilterString(query), subTreeControls);
            while (users.hasMore()) {
                SearchResult userRoot = users.next();
                String name = userRoot.getNameInNamespace();
                if (name.startsWith("racfid=irrcerta,") ||
                        name.startsWith("racfid=irrmulti,") ||
                        name.startsWith("racfid=irrsitec,")) {
                    // Ignore
                } else {
                    userNames.add(_connector.extractRacfIdFromLdapId(name));
                }
            }
        } catch (LimitExceededException e) {
            //TODO: cope with this
            throw ConnectorException.wrap(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
        return userNames;
    }

    public List<String> getGroupsViaLdap(String query) {
        SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, null, true, true);
        List<String> groupNames = new LinkedList<String>();
        try {
            String search = "profileType=group,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
            NamingEnumeration<SearchResult> groups = ((RacfConnection)_connector.getConnection()).getDirContext().search(search, getLdapFilterForFilterString(query), subTreeControls);
            while (groups.hasMore()) {
                SearchResult userRoot = groups.next();
                String name = userRoot.getNameInNamespace();
                groupNames.add(_connector.extractRacfIdFromLdapId(name));
            }
        } catch (LimitExceededException e) {
            //TODO: cope with this
            throw ConnectorException.wrap(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
        return groupNames;
    }

    private String getLdapFilterForFilterString(String filter) {
        String filterText = "(objectclass=*)";
        if (filter != null)
            filterText = filter;
        return filterText;
    }

    public Uid updateViaLdap(ObjectClass objectClass, Set<Attribute> attrs) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        Uid uid = AttributeUtil.getUidAttribute(attrs);
        //TODO: need to process null values, which correspond to DirContext.REMOVE_ATTRIBUTE
        if (uid!=null) {
            if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
                try {
                    Attribute groups = attributes.remove(ATTR_LDAP_GROUPS);
                    Attribute groupOwners = attributes.remove(ATTR_LDAP_GROUP_OWNERS);
                    
                    ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(_connector.createUidFromName(objectClass, uid.getUidValue()).getUidValue(), DirContext.REPLACE_ATTRIBUTE, createLdapAttributesFromConnectorAttributes(objectClass, attributes));
                    if (groups!=null)
                        _connector.setGroupMembershipsForUser(uid.getUidValue(), groups, groupOwners);
                } catch (NamingException e) {
                    throw new ConnectorException(e);
                }
            } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
                try {
                    Attribute members = attributes.remove(ATTR_LDAP_MEMBERS);
                    Attribute groupOwners = attributes.remove(ATTR_LDAP_GROUP_OWNERS);
                    
                    ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(_connector.createUidFromName(objectClass, uid.getUidValue()).getUidValue(), DirContext.REPLACE_ATTRIBUTE, createLdapAttributesFromConnectorAttributes(objectClass, attributes));
                    if (members!=null)
                        _connector.setGroupMembershipsForGroups(uid.getUidValue(), members, groupOwners);
                } catch (NamingException e) {
                    throw new ConnectorException(e);
                }
            } else {
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS, objectClass));
            }
        }
        return uid;
    }

    protected void addObjectClass(ObjectClass objectClass, Map<String, Attribute> attrs) {
        if (objectClass.equals(ObjectClass.ACCOUNT))
            attrs.put("objectclass", AttributeBuilder.build("objectclass", "racfUser"));
        else if (objectClass.equals(ObjectClass.GROUP))
            attrs.put("objectclass", AttributeBuilder.build("objectclass", "racfGroup"));
    }

    private Attributes createLdapAttributesFromConnectorAttributes(ObjectClass objectClass, Map<String, Attribute> attributes) {
        Attributes basicAttributes = new BasicAttributes();
        Set<ObjectClassInfo> objectClassInfos = _schema.getObjectClassInfo();
        ObjectClassInfo accountInfo = null;
        for (ObjectClassInfo objectClassInfo : objectClassInfos) {
            if (objectClassInfo.is(objectClass.getObjectClassValue()))
                accountInfo = objectClassInfo;
        }
        Set<AttributeInfo> attributeInfos = accountInfo.getAttributeInfo();
        List<Object> racfAttributes = new LinkedList<Object>();
        boolean setRacfAttributes = false;
        //TODO: need to deal with ENABLE/DISABLE_DATE like command-line code
        // Go through, and validate the attributes.
        //
        Attribute enabled       = attributes.get(ATTR_LDAP_ENABLED);
        Attribute enableDate    = attributes.get(OperationalAttributes.ENABLE_DATE_NAME);
        Attribute disableDate   = attributes.get(OperationalAttributes.DISABLE_DATE_NAME);
        
        for (Attribute attribute : attributes.values()) {
            String attributeName = attribute.getName().toLowerCase();
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                // Ignore Name, Uid
                //
            } else if (attribute.is("objectclass")) {
                // TODO: skip this for now
                //
            } else if (attribute.is(RacfConstants.ATTR_LDAP_ATTRIBUTES)) {
                racfAttributes.addAll(attribute.getValue());
                setRacfAttributes = true;
            } else if (attribute.is(RacfConstants.ATTR_LDAP_AUTHORIZATION_DATE) ||
                    attribute.is(RacfConstants.ATTR_LDAP_PASSWORD_INTERVAL) ||
                    attribute.is(RacfConstants.ATTR_LDAP_RACF_ID) ||
                    attribute.is(RacfConstants.ATTR_LDAP_LAST_ACCESS) ||
                    attribute.is(RacfConstants.ATTR_LDAP_PASSWORD_CHANGE) ||
                    attribute.is(RacfConstants.ATTR_LDAP_SUB_GROUP) ||
                    attribute.is(RacfConstants.ATTR_LDAP_GROUP_USERIDS)) {
                // Ignore read-only attrs
                //
            } else if (attribute.is(PredefinedAttributes.GROUPS_NAME)) {
                // Groups handled separately
                //
            } else if (attribute.is(OperationalAttributes.CURRENT_PASSWORD_NAME)) {
                // Ignore current password
                //
            } else if (attribute.is(ATTR_LDAP_EXPIRED)) {
                Attribute password = attributes.get(ATTR_LDAP_PASSWORD);
                if (password==null) {
                    // TODO: throw error, I expect
                }
                //TODO: determine if this works in absence of setting password 
                if (AttributeUtil.getBooleanValue(attribute))
                    racfAttributes.add("noExpired");
                else
                    racfAttributes.add("Expired");
                setRacfAttributes = true;
            } else if (attribute.is(ATTR_LDAP_PASSWORD)) {
                // remap password
                //
                List<Object> value = attribute.getValue();
                if (value==null || value.size()!=1) {
                    throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.MUST_BE_SINGLE_VALUE));
                }
                GuardedString password = AttributeUtil.getGuardedStringValue(attribute);
                GuardedStringAccessor accessor = new GuardedStringAccessor();
                password.access(accessor);
                basicAttributes.put(ATTR_LDAP_PASSWORD, new String(accessor.getArray()));
            } else {
                AttributeInfo attributeInfo = getAttributeInfo(attributeInfos, attributeName);
                if (attributeInfo==null)
                    throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_ATTRIBUTE, attributeName));
                if (attributeInfo.isMultiValued())
                    basicAttributes.put(attributeName, attribute.getValue());
                else
                    basicAttributes.put(attributeName, AttributeUtil.getSingleValue(attribute));
            }
        }
        if (setRacfAttributes)
            basicAttributes.put(ATTR_LDAP_ATTRIBUTES, racfAttributes);

        return basicAttributes;
    }

    private AttributeInfo getAttributeInfo(Set<AttributeInfo> attributeInfos, String name) {
        for (AttributeInfo attributeInfo: attributeInfos) {
            if (attributeInfo.getName().equalsIgnoreCase(name))
                return attributeInfo;
        }
        return null;
    }

    private static class GuardedStringAccessor implements GuardedString.Accessor {
        private char[] _array;

        public void access(char[] clearChars) {
            _array = new char[clearChars.length];
            System.arraycopy(clearChars, 0, _array, 0, _array.length);
        }

        public char[] getArray() {
            return _array;
        }

        public void clear() {
            Arrays.fill(_array, 0, _array.length, ' ');
        }
    }
}
/*
// Since DFLTGRP comes in as a stringified UID, it must be converted
//
if (DEFAULT_GROUP_NAME.equalsIgnoreCase(attributeName) ||
        SUPGROUP.equalsIgnoreCase(attributeName) ||
        OWNER.equalsIgnoreCase(attributeName)
    ) {
    value = _connector.extractRacfIdFromLdapId(new String(value)).toCharArray();
}

            // Owner must be a stringified Uid
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_OWNER)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_OWNER);
                attributesFromCommandLine.put(ATTR_CL_OWNER, getOwnerUid(value));
            }
            // Superior group must be a stringified Uid
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_SUPGROUP)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_SUPGROUP);
                if ("NONE".equals(value))
                    attributesFromCommandLine.put(ATTR_CL_SUPGROUP, null);
                else
                    attributesFromCommandLine.put(ATTR_CL_SUPGROUP, _connector.createUidFromName(RacfConnector.RACF_GROUP, (String)value).getUidValue());
            }
            // Group members must be Uids
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_MEMBERS)) {
                List<Object> members = (List<Object>)attributesFromCommandLine.get(ATTR_CL_MEMBERS);
                List<String> membersAsString = new LinkedList<String>();
                if (members!=null) {
                    for (Object member : members)
                        membersAsString.add(_connector.createUidFromName(ObjectClass.ACCOUNT, (String)member).getUidValue());
                }
                attributesFromCommandLine.put(ATTR_CL_MEMBERS, membersAsString);
            }
            // Groups must be stringified Uids
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_GROUPS)) {
                List<Object> members = (List<Object>)attributesFromCommandLine.remove(ATTR_CL_GROUPS);
                List<String> membersAsString = new LinkedList<String>();
                if (members!=null) {
                    for (Object member : members)
                        membersAsString.add(_connector.createUidFromName(RacfConnector.RACF_GROUP, (String)member).getUidValue());
                }
                attributesFromCommandLine.put(ATTR_CL_GROUPS, membersAsString);
            }




            // Owner must be a stringified Uid
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_OWNER)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_OWNER);
                attributesFromCommandLine.put(ATTR_CL_OWNER, getOwnerUid(value));
            }
            // Default group name must be a stringified Uid
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_DFLTGRP)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_DFLTGRP);
                attributesFromCommandLine.put(ATTR_CL_DFLTGRP, _connector.createUidFromName(RacfConnector.RACF_GROUP, (String)value).getUidValue());
            }
            // Groups must be stringified Uids
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_GROUPS)) {
                List<Object> members = (List<Object>)attributesFromCommandLine.remove(ATTR_CL_GROUPS);
                List<String> membersAsString = new LinkedList<String>();
                if (members!=null) {
                    for (Object member : members)
                        membersAsString.add(_connector.createUidFromName(RacfConnector.RACF_GROUP, (String)member).getUidValue());
                }
                attributesFromCommandLine.put(ATTR_CL_GROUPS, membersAsString);
            }

*/