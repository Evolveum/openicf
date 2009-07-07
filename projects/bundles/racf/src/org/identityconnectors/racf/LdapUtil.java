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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.identityconnectors.common.CollectionUtil;
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
    private final Pattern               _connectionPattern  = Pattern.compile("racfuserid=(.*)\\+racfgroupid=([^,]*),.*");

    private RacfConnector               _connector;
    private Schema                      _schema;

    public LdapUtil(RacfConnector connector) {
        _connector = connector;
        _schema = _connector.schema();
    }

    public Uid createViaLdap(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        if (objectClass.is(RacfConnector.RACF_CONNECTION_NAME)) {
            try {
                Name name = AttributeUtil.getNameFromAttributes(attrs);
                Map<String, Attribute> newAttributes = AttributeUtil.toMap(attrs);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(name.getNameValue(), createLdapAttributesFromConnectorAttributes(newAttributes));
                return new Uid(name.getNameValue());
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        } else if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
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
                    List<Object> value = new LinkedList<Object>(attributesAttribute.getValue());
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
                Uid uid = new Uid(id);
                Map<String, Attribute> newAttributes = CollectionUtil.newCaseInsensitiveMap();
                newAttributes.putAll(attributes);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(id, createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                if (groups!=null)
                    _connector.setGroupMembershipsForUser(id, groups, groupOwners);
                return uid;
            } catch (NamingException e) {
                if (e.toString().contains("INVALID USER"))
                    throw new AlreadyExistsException();
                else
                    throw new ConnectorException(e);
            }
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            try {
                Attribute members = attributes.remove(ATTR_LDAP_GROUP_USERIDS);
                Attribute groupOwners = attributes.remove(ATTR_LDAP_GROUP_OWNERS);

                String id = name.getNameValue();
                Uid uid = _connector.createUidFromName(objectClass, id);
                Map<String, Attribute> newAttributes = new HashMap<String, Attribute>(attributes);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(id, createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                if (members!=null)
                    _connector.setGroupMembershipsForGroups(id, members, groupOwners);
                return uid;
            } catch (NamingException e) {
                if (e.toString().contains("INVALID GROUP"))
                    throw new AlreadyExistsException();
                else
                    throw new ConnectorException(e);
            }
        } else {
            throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS, objectClass.getObjectClassValue()));
        }
    }

    public void deleteViaLdap(ObjectClass objectClass, Uid uid) {
        try {
            ((RacfConnection)_connector.getConnection()).getDirContext().destroySubcontext(uid.getUidValue());
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

    public List<String> getGroupsForUserViaLdap(String user) {
        return getConnectionInfo("racfuserid="+_connector.extractRacfIdFromLdapId(user), 2);
    }

    public List<String> getMembersOfGroupViaLdap(String group) {
        return getConnectionInfo("racfgroupid="+_connector.extractRacfIdFromLdapId(group), 1);
    }

    private List<String> getConnectionInfo(String query, int index) {
        SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, null, true, true);
        List<String> objects = new LinkedList<String>();
        try {
            String search = "profileType=connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
            NamingEnumeration<SearchResult> connections = ((RacfConnection)_connector.getConnection()).getDirContext().search(search, query, subTreeControls);
            while (connections.hasMore()) {
                SearchResult userRoot = connections.next();
                String name = userRoot.getNameInNamespace();
                Matcher matcher = _connectionPattern.matcher(name);
                if (matcher.matches()) {
                    objects.add("racfid="+matcher.group(index)+",profileType="+(index==1?"User,":"Group,")+((RacfConfiguration)_connector.getConfiguration()).getSuffix());
                } else {
                    throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.PATTERN_FAILED, name));
                }
            }
            return objects;
        } catch (LimitExceededException e) {
            //TODO: cope with this
            throw ConnectorException.wrap(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    public List<String> getUsersViaLdap(String query) {
        SearchControls subTreeControls = new SearchControls(SearchControls.ONELEVEL_SCOPE, 4095, 0, null, true, true);
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
        } catch (LimitExceededException e) {
            //TODO: cope with this
            throw ConnectorException.wrap(e);
        } catch (NamingException e) {
            if (!e.toString().contains("NO ENTRIES MEET SEARCH CRITERIA"))
                throw new ConnectorException(e);
        }
        return userNames;
    }

    public List<String> getGroupsViaLdap(String query) {
        SearchControls subTreeControls = new SearchControls(SearchControls.ONELEVEL_SCOPE, 4095, 0, null, true, true);
        List<String> groupNames = new LinkedList<String>();
        try {
            String search = "profileType=group,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
            NamingEnumeration<SearchResult> groups = ((RacfConnection)_connector.getConnection()).getDirContext().search(search, getLdapFilterForFilterString(query), subTreeControls);
            while (groups.hasMore()) {
                SearchResult userRoot = groups.next();
                String name = userRoot.getNameInNamespace();
                groupNames.add(name);
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
    

    public Map<String, Object> getAttributesFromLdap(ObjectClass objectClass, String ldapName, Set<String> originalAttributesToGet) throws NamingException {
        Map<String, Object> attributesRead = CollectionUtil.newCaseInsensitiveMap();
        Set<String> attributesToGet = new HashSet<String>(originalAttributesToGet);
        
        // A few attributes need to be done via a separate LDAP query, so we save them
        //
        boolean owners = attributesToGet.remove(ATTR_LDAP_OWNER);
        boolean groups = attributesToGet.remove(ATTR_LDAP_GROUPS);
        boolean members = attributesToGet.remove(ATTR_LDAP_GROUP_USERIDS);
        
        SearchResult ldapObject = getAttributesFromLdap(ldapName, attributesRead, attributesToGet);
        Uid uid = new Uid(ldapObject.getNameInNamespace());
        attributesRead.put(Uid.NAME, uid);
        attributesRead.put(Name.NAME, ldapObject.getNameInNamespace());
        
        // For Users, we need to do a separate query against the connections to pick up
        // Connection info
        //
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (owners || groups) {
                String user = RacfConnector.extractRacfIdFromLdapId(ldapName);
                // First, get the connections for the user
                //
                String query = "profileType=Connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
                SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, attributesToGet.toArray(new String[0]), true, true);
                NamingEnumeration<SearchResult> results = _connector.getConnection().getDirContext().search(query, "(racfuserid="+user+")", subTreeControls);
                List<String> groupsForUser = new ArrayList<String>();
                while (results.hasMore()) {
                    SearchResult result = results.next();
                    String connection = result.getNameInNamespace();
                    String[] ids = RacfConnector.extractRacfIdAndGroupIdFromLdapId(connection);
                    String group = ids[1];
                    groupsForUser.add(_connector.createUidFromName(RacfConnector.RACF_GROUP, group).getUidValue());
                }
                if (groups)
                    attributesRead.put(ATTR_LDAP_GROUPS, groupsForUser);
                if (owners) {
                    List<String> ownersForUser = new ArrayList<String>();
                    Set<String> connectAttributesToGet = new HashSet<String>();
                    connectAttributesToGet.add(ATTR_LDAP_OWNER);
                    for (String group : groupsForUser) {
                        group = RacfConnector.extractRacfIdFromLdapId(group);
                        String root = "racfuserid="+user+"+racfgroupid="+group+",profileType=Connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
                        ownersForUser.add(getConnectOwner(root));
                    }
                    attributesRead.put(ATTR_LDAP_GROUP_OWNERS, ownersForUser);
                }
            }
        }
        // For Groups, we need to do a separate query against the connections to pick up
        // Connection info
        //
        if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            if (members || owners) {
                String group = RacfConnector.extractRacfIdFromLdapId(ldapName);
                List<String> usersForGroup = getMembersOfGroupViaLdap(ldapObject.getNameInNamespace());
                if (members)
                    attributesRead.put(ATTR_CL_MEMBERS, usersForGroup);
                if (owners) {
                    List<String> ownersForGroup = new ArrayList<String>();
                    Set<String> connectAttributesToGet = new HashSet<String>();
                    connectAttributesToGet.add(ATTR_LDAP_OWNER);
                    for (String user : usersForGroup) {
                        user = RacfConnector.extractRacfIdFromLdapId(user);
                        String root = "racfuserid="+user+"+racfgroupid="+group+",profileType=Connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
                        ownersForGroup.add(getConnectOwner(root));
                    }
                    attributesRead.put(ATTR_LDAP_GROUP_OWNERS, ownersForGroup);
                }
            }
            //TODO: what about subgroups (queried as racfSubgroupName)
        }
        

        // Remap ACCOUNT attributes as needed
        //
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            // 'racfattributes' comes back as a String, we need to transform it into a list
            //
            Object racfAttributes = attributesRead.get(ATTR_LDAP_ATTRIBUTES);
            if (racfAttributes!=null) {
                List<String> realRacfAttributes = new ArrayList<String>();
                if (racfAttributes instanceof String) {
                    for (String attribute : ((String)racfAttributes).split("\\s+")) {
                        realRacfAttributes.add(attribute);
                    }
                } else if (racfAttributes instanceof List) {
                    for (Object attribute : ((List)racfAttributes)) {
                        for (String attributePart : attribute.toString().split("\\s+")) {
                            realRacfAttributes.add(attributePart);
                        }
                    }
                }
                // __ENABLE__ is indicated by the REVOKED ATTRIBUTE
                // We also remove REVOKED from attribute list, since we display it separately
                //
                boolean revoked = realRacfAttributes.remove("REVOKED");
                attributesRead.put(OperationalAttributes.ENABLE_NAME, !revoked);
                attributesRead.put(ATTR_LDAP_ATTRIBUTES, realRacfAttributes);
            }
            // Last Access date must be converted
            //
            if (attributesRead.containsKey(ATTR_LDAP_LAST_ACCESS)) {
                Object value = attributesRead.get(ATTR_LDAP_LAST_ACCESS);
                Long converted = _connector.convertFromRacfTimestamp(value);
                attributesRead.put(PredefinedAttributes.LAST_LOGIN_DATE_NAME, converted);
            }
            // TSO SIZE must be converted
            //
            if (attributesRead.containsKey(ATTR_LDAP_TSO_LOGON_SIZE)) {
                Object value = attributesRead.get(ATTR_LDAP_TSO_LOGON_SIZE);
                Integer converted = Integer.parseInt((String)value);
                attributesRead.put(ATTR_LDAP_TSO_LOGON_SIZE, converted);
            }
            // TSO MAXSIZE must be converted
            //
            if (attributesRead.containsKey(ATTR_LDAP_TSO_MAX_REGION_SIZE)) {
                Object value = attributesRead.get(ATTR_LDAP_TSO_MAX_REGION_SIZE);
                Integer converted = Integer.parseInt((String)value);
                attributesRead.put(ATTR_LDAP_TSO_MAX_REGION_SIZE, converted);
            }
            // password change date must be converted
            //
            if (attributesRead.containsKey(ATTR_LDAP_PASSWORD_CHANGE)) {
                Object value = attributesRead.get(ATTR_LDAP_PASSWORD_CHANGE);
                Long converted = _connector.convertFromRacfTimestamp(value);
                attributesRead.put(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME, converted);
                // password change date is 00.000 if expired
                //
                Boolean expired = "00.000".equals(value);
                attributesRead.put(OperationalAttributes.PASSWORD_EXPIRED_NAME, expired);
            }
            // Revoke date must be converted
            //
            long now = new Date().getTime(); 
            if (attributesRead.containsKey(ATTR_LDAP_REVOKE_DATE)) {
                Object value = attributesRead.get(ATTR_LDAP_REVOKE_DATE);
                Long converted = _connector.convertFromResumeRevokeFormat(value);
                if (converted==null || converted<now)
                    attributesRead.put(OperationalAttributes.DISABLE_DATE_NAME, null);
                else
                    attributesRead.put(OperationalAttributes.DISABLE_DATE_NAME, converted);
            }
            // Resume date must be converted
            //
            if (attributesRead.containsKey(ATTR_LDAP_RESUME_DATE)) {
                Object value = attributesRead.get(ATTR_LDAP_RESUME_DATE);
                Long converted = _connector.convertFromResumeRevokeFormat(value);
                if (converted==null || converted<now)
                    attributesRead.put(OperationalAttributes.ENABLE_DATE_NAME, null);
                else
                    attributesRead.put(OperationalAttributes.ENABLE_DATE_NAME, converted);
            }
            // Groups must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_GROUPS)) {
                attributesRead.put(ATTR_LDAP_GROUPS, new LinkedList<Object>());
            }
            // Group Owners must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_GROUP_OWNERS)) {
                attributesRead.put(ATTR_LDAP_GROUP_OWNERS, new LinkedList<Object>());
            }
        }

        // Remap GROUP attributes as needed
        //
        if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            if (attributesRead.containsKey(ATTR_LDAP_SUP_GROUP)) {
                Object value = attributesRead.get(ATTR_LDAP_SUP_GROUP);
                if ("NONE".equals(value))
                    attributesRead.put(ATTR_LDAP_SUP_GROUP, null);
            }
            // Groups must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_SUB_GROUPS)) {
                attributesRead.put(ATTR_LDAP_SUB_GROUPS, new LinkedList<Object>());
            }
            // Members must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_GROUP_USERIDS)) {
                attributesRead.put(ATTR_LDAP_GROUP_USERIDS, new LinkedList<Object>());
            }
            // Group Owners must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_GROUP_OWNERS)) {
                attributesRead.put(ATTR_LDAP_GROUP_OWNERS, new LinkedList<Object>());
            }
        }
        return attributesRead;
    }
    
    private String getConnectOwner(String query) throws NamingException {
        Map<String, Object> attributesRead = CollectionUtil.newCaseInsensitiveMap();
        Set<String> attributesToGet = new HashSet<String>();
        attributesToGet.add(ATTR_LDAP_CONNECT_OWNER);
        getAttributesFromLdap(query, attributesRead, attributesToGet);
        return (String)attributesRead.get(ATTR_LDAP_CONNECT_OWNER);
    }

    private SearchResult getAttributesFromLdap(String ldapName, Map<String, Object> attributesRead,
            Set<String> attributesToGet) throws NamingException {
        SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, attributesToGet.toArray(new String[0]), true, true);
        SearchResult ldapObject = null;
        NamingEnumeration<SearchResult> results = _connector.getConnection().getDirContext().search(ldapName, "(objectclass=*)", subTreeControls);
        ldapObject = results.next();
        Attributes attributes = ldapObject.getAttributes();
        NamingEnumeration<? extends javax.naming.directory.Attribute> attributeEnum = attributes.getAll();
        while (attributeEnum.hasMore()) {
            javax.naming.directory.Attribute attribute = attributeEnum.next();
            Object value = attribute.get();
            // Some attributes expect Lists, but may return a singleton,
            // these must be mapped back into Lists
            //
            if (ATTR_LDAP_GROUPS.equalsIgnoreCase(attribute.getID())) {
                if (!(value instanceof Collection)) {
                    List newValue = new ArrayList();
                    newValue.add(value);
                    value = newValue;
                }
            }
            if (value instanceof Collection)
                attributesRead.put(attribute.getID(), (Collection<? extends Object>)value);
            else
                attributesRead.put(attribute.getID(), value);
        }
        return ldapObject;
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
                    
                    ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(uid.getUidValue(), DirContext.REPLACE_ATTRIBUTE, createLdapAttributesFromConnectorAttributes(objectClass, attributes));
                    if (groups!=null)
                        _connector.setGroupMembershipsForUser(uid.getUidValue(), groups, groupOwners);
                } catch (NamingException e) {
                    throw new ConnectorException(e);
                }
            } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
                try {
                    Attribute members = attributes.remove(ATTR_LDAP_GROUP_USERIDS);
                    Attribute groupOwners = attributes.remove(ATTR_LDAP_GROUP_OWNERS);
                    
                    ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(uid.getUidValue(), DirContext.REPLACE_ATTRIBUTE, createLdapAttributesFromConnectorAttributes(objectClass, attributes));
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
        if (objectClass.is(ObjectClass.ACCOUNT_NAME))
            attrs.put("objectclass", AttributeBuilder.build("objectclass", "racfUser"));
        else if (objectClass.is(RacfConnector.RACF_GROUP_NAME))
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
        
        //?? See if we can make tests work
        //
        attributes.remove(ATTR_LDAP_NV_CTL);
        attributes.remove(ATTR_LDAP_NV_DCE_AUTOLOGIN);
        attributes.remove(ATTR_LDAP_NV_DCE_HOME_CELL);
        attributes.remove(ATTR_LDAP_NV_DCE_HOME_CELL_UUID);
        attributes.remove(ATTR_LDAP_NV_DCE_PRINCIPAL);
        attributes.remove(ATTR_LDAP_NV_DCE_UUID);
        attributes.remove(ATTR_LDAP_NV_DEFAULT_CONSOLE);
        attributes.remove(ATTR_LDAP_NV_DOMAINS);
        attributes.remove(ATTR_LDAP_NV_MESSAGE_RECEIVER);
        attributes.remove(ATTR_LDAP_NV_NGMFADM);
        attributes.remove(ATTR_LDAP_NV_INITIALCMD);
        attributes.remove(ATTR_LDAP_NV_OPERATOR_CLASS);
        // R001030 Entry contained attribute type not allowed by schema: krbprincipalname.
        attributes.remove(ATTR_LDAP_KERB_NAME);
        attributes.remove(ATTR_LDAP_KERB_MAX_TICKET_LIFE);
        attributes.remove(ATTR_LDAP_KERB_ENCRYPT);
        attributes.remove(ATTR_LDAP_KERB_KEY_VERSION);
        // R001030 Entry contained attribute type not allowed by schema: racfaddressline1
        attributes.remove(ATTR_LDAP_WA_ADDRESS_LINE1);
        attributes.remove(ATTR_LDAP_WA_ADDRESS_LINE2);
        attributes.remove(ATTR_LDAP_WA_ADDRESS_LINE3);
        attributes.remove(ATTR_LDAP_WA_ADDRESS_LINE4);
        attributes.remove(ATTR_LDAP_WA_ACCOUNT_NUMBER);
        attributes.remove(ATTR_LDAP_WA_BUILDING);
        attributes.remove(ATTR_LDAP_WA_DEPARTMENT);
        attributes.remove(ATTR_LDAP_WA_ROOM);
        attributes.remove(ATTR_LDAP_WA_USER_NAME);
        // R001030 Entry contained attribute type not allowed by schema: racfaltgroupkeyword
        attributes.remove(ATTR_LDAP_OP_ALTGROUP);
        attributes.remove(ATTR_LDAP_OP_AUTH);
        attributes.remove(ATTR_LDAP_OP_AUTO);
        attributes.remove(ATTR_LDAP_OP_CMDSYS);
        attributes.remove(ATTR_LDAP_OP_DOM);
        attributes.remove(ATTR_LDAP_OP_KEY);
        attributes.remove(ATTR_LDAP_OP_LEVEL);
        attributes.remove(ATTR_LDAP_OP_LOG_CMD_RESPONSE);
        attributes.remove(ATTR_LDAP_OP_MFORM);
        attributes.remove(ATTR_LDAP_OP_MGID);
        attributes.remove(ATTR_LDAP_OP_MONITOR);
        attributes.remove(ATTR_LDAP_OP_MSCOPE_SYSTEMS);
        attributes.remove(ATTR_LDAP_OP_ROUTCODE);
        attributes.remove(ATTR_LDAP_OP_STORAGE);
        attributes.remove(ATTR_LDAP_OP_UD);
        // R001030 Entry contained attribute type not allowed by schema: racfldapbinddn
        attributes.remove(ATTR_LDAP_PROXY_BINDDN);
        attributes.remove(ATTR_LDAP_PROXY_BINDPW);
        attributes.remove(ATTR_LDAP_PROXY_HOST);
        // R001030 Entry contained attribute type not allowed by schema: racflnotesshortname.
        attributes.remove(ATTR_LDAP_LN_SHORT_NAME);
        // R001030 Entry contained attribute type not allowed by schema: racfndsusername
        attributes.remove(ATTR_LDAP_NDS_USER_NAME);
        
        
        for (Attribute attribute : attributes.values()) {
            String attributeName = attribute.getName().toLowerCase();
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                // Ignore Name, Uid
                //
            } else if (attribute.is("objectclass")) {
                basicAttributes.put("objectclass", AttributeUtil.getSingleValue(attribute));
            } else if (attribute.is(ATTR_LDAP_ATTRIBUTES)) {
                racfAttributes.addAll(attribute.getValue());
                setRacfAttributes = true;
            } else if (attribute.is(ATTR_LDAP_AUTHORIZATION_DATE) ||
                    attribute.is(ATTR_LDAP_PASSWORD_INTERVAL) ||
                    attribute.is(ATTR_LDAP_RACF_ID) ||
                    attribute.is(ATTR_LDAP_LAST_ACCESS) ||
                    attribute.is(ATTR_LDAP_PASSWORD_CHANGE) ||
                    attribute.is(ATTR_LDAP_SUB_GROUP) ||
                    attribute.is(ATTR_LDAP_GROUP_USERIDS)) {
                // Ignore read-only attrs
                //
            } else if (attribute.is(PredefinedAttributes.GROUPS_NAME)) {
                // Groups handled separately
                //
            } else if (attribute.is(OperationalAttributes.CURRENT_PASSWORD_NAME)) {
                // Ignore current password
                //
            } else if (attribute.is(ATTR_LDAP_EXPIRED)) {
                // Skip this for now -- must be done via update
                if (false) {
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
                }
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
                if (attribute.getValue() instanceof Collection) {
                    basicAttributes.put(makeBasicAttribute(attributeName, attribute.getValue()));
                } else {
                    Object value = AttributeUtil.getSingleValue(attribute);
                    if (value !=null)
                        value = value.toString();
                    basicAttributes.put(attributeName, value);
                }
            }
        }
        if (setRacfAttributes) {
            basicAttributes.put(makeBasicAttribute(ATTR_LDAP_ATTRIBUTES, racfAttributes));
        }

        return basicAttributes;
    }

    private Attributes createLdapAttributesFromConnectorAttributes(Map<String, Attribute> attributes) {
        Attributes basicAttributes = new BasicAttributes();
        
        for (Attribute attribute : attributes.values()) {
            String attributeName = attribute.getName().toLowerCase();
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                // Ignore Name, Uid
                //
            } else if (attribute.is("objectclass")) {
                basicAttributes.put("objectclass", AttributeUtil.getSingleValue(attribute));
            } else {
                if (attribute.getValue() instanceof Collection) {
                    basicAttributes.put(makeBasicAttribute(attributeName, attribute.getValue()));
                } else {
                    Object value = AttributeUtil.getSingleValue(attribute);
                    if (value !=null)
                        value = value.toString();
                    basicAttributes.put(attributeName, value);
                }
            }
        }
        return basicAttributes;
    }

    private BasicAttribute makeBasicAttribute(String name, List<Object> racfAttributes) {
        BasicAttribute attribute = new BasicAttribute(name);
        for (Object value : racfAttributes) {
            if (value!=null)
                value = value.toString();
            attribute.add(value);
        }
        return attribute;
    }

    private AttributeInfo getAttributeInfo(Set<AttributeInfo> attributeInfos, String name) {
        for (AttributeInfo attributeInfo: attributeInfos) {
            if (attributeInfo.getName().equalsIgnoreCase(name))
                return attributeInfo;
        }
        return null;
    }
}