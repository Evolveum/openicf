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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (objectClass.equals(RacfConnector.RACF_CONNECTION)) {
            try {
                //TODO: handle ATTR_LDAP_GROUPS
                Name name = AttributeUtil.getNameFromAttributes(attrs);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(name.getNameValue(), null);
                return new Uid(name.getNameValue());
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        } else if (objectClass.equals(ObjectClass.ACCOUNT)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            try {
                String id = name.getNameValue();
                Uid uid = _connector.createUidFromName(objectClass, id);
                Set<Attribute> newAttributes = new HashSet<Attribute>(attrs);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(uid.getUidValue(), createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                Attribute groupMembership = AttributeUtil.find(PredefinedAttributes.GROUPS_NAME, attrs);
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForUser(id, groupMembership);
                return uid;
            } catch (NamingException e) {
                if (e.toString().indexOf("INVALID USER")==-1)
                    throw new ConnectorException(e);
                else
                    throw new AlreadyExistsException();
            }
        } else if (objectClass.equals(ObjectClass.GROUP)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            try {
                String id = name.getNameValue();
                Uid uid = _connector.createUidFromName(objectClass, id);
                Set<Attribute> newAttributes = new HashSet<Attribute>(attrs);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(uid.getUidValue(), createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                Attribute groupMembership = AttributeUtil.find(ATTR_LDAP_MEMBERS, attrs);
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForGroups(id, groupMembership);
                return uid;
            } catch (NamingException e) {
                //TODO: may need to handle Groups with different test
                //
                if (e.toString().indexOf("INVALID USER")==-1)
                    throw new ConnectorException(e);
                else
                    throw new AlreadyExistsException();
            }
        } else {
            throw new IllegalArgumentException("TODO");
        }
    }

    public void deleteViaLdap(Uid uid) {
        try {
            ((RacfConnection)_connector.getConnection()).getDirContext().destroySubcontext(uid.getUidValue());
        } catch (NamingException e) {
            //TODO: may need to handle Groups with different test
            //
            if (e.toString().indexOf("INVALID USER")==-1)
                throw new ConnectorException(e);
            else
                throw new UnknownUidException();
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
            return groups;
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
                    userNames.add(name);
                }
            }
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
                groupNames.add(name);
            }
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

    public Uid updateViaLdap(ObjectClass objclass, Set<Attribute> attrs) {
        Uid uid = AttributeUtil.getUidAttribute(attrs);
        if (uid!=null) {
            try {
                //TODO: handle ATTR_LDAP_GROUPS
                ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(uid.getUidValue(), DirContext.REPLACE_ATTRIBUTE, createLdapAttributesFromConnectorAttributes(objclass, attrs));
                Attribute groupMembership = AttributeUtil.find(PredefinedAttributes.GROUPS_NAME, attrs);
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForUser(_connector.createAccountNameFromUid(uid), groupMembership);
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        }
        return uid;
    }

    protected void addObjectClass(ObjectClass objectClass, Set<Attribute> attrs) {
        if (objectClass.equals(ObjectClass.ACCOUNT))
            attrs.add(AttributeBuilder.build("objectclass", "racfUser"));
        else if (objectClass.equals(ObjectClass.GROUP))
            attrs.add(AttributeBuilder.build("objectclass", "racfGroup"));
    }

    private Attributes createLdapAttributesFromConnectorAttributes(ObjectClass objectClass, Set<Attribute> attributes) {
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
        // Go through, and validate the attributes.
        //
        for (Attribute attribute : attributes) {
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
            } else if (attribute.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)) {
                //TODO: determine if this works in absence of setting password 
                if (!AttributeUtil.getBooleanValue(attribute))
                    racfAttributes.add("noExpired");
                setRacfAttributes = true;
            } else if (attribute.is(OperationalAttributes.PASSWORD_NAME)) {
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