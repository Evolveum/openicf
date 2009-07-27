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
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.racf.CommandLineUtil.LocalHandler;

class LdapUtil {

    private final Pattern               _connectionPattern  = Pattern.compile("racfuserid=(.*)\\+racfgroupid=([^,]*),.*");

    private RacfConnector               _connector;
    private Schema                      _schema;

    public LdapUtil(RacfConnector connector) {
        _connector = connector;
        _schema = _connector.schema();
    }

    public Uid createViaLdap(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = CollectionUtil.newCaseInsensitiveMap();
        attributes.putAll(AttributeUtil.toMap(attrs));
        if (objectClass.is(RacfConnector.RACF_CONNECTION_NAME)) {
            try {
                Name name = AttributeUtil.getNameFromAttributes(attrs);
                Map<String, Attribute> newAttributes = AttributeUtil.toMap(attrs);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(name.getNameValue(), createLdapAttributesFromConnectorAttributes(newAttributes));
                return new Uid(name.getNameValue().toUpperCase());
            } catch (NamingException e) {
                throw new ConnectorException(e);
            }
        } else if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            try {
                Attribute groups = attributes.remove(ATTR_LDAP_GROUPS);
                Attribute owners = attributes.remove(ATTR_LDAP_CONNECT_OWNER);
                Attribute expired = attributes.remove(ATTR_LDAP_EXPIRED);
                Attribute password = attributes.get(ATTR_LDAP_PASSWORD);

                _connector.throwErrorIfNull(groups);
                _connector.throwErrorIfNullOrEmpty(expired);
                _connector.throwErrorIfNullOrEmpty(password);
                _connector.checkConnectionConsistency(groups, owners);
                
                if (expired!=null && password==null) 
                    throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.EXPIRED_NO_PASSWORD));
                if (userExists(name.getNameValue()))
                    throw new AlreadyExistsException();
                // Some attributes cannot be specified during create, only modify.
                // Save these off to the side.
                //
                Set<Attribute> changes = new HashSet<Attribute>();
                Attribute enable      = attributes.remove(ATTR_LDAP_ENABLED);
                Attribute enableDate  = attributes.remove(ATTR_LDAP_RESUME_DATE);
                Attribute disableDate = attributes.remove(ATTR_LDAP_REVOKE_DATE);
                if (expired!=null)
                    changes.add(expired);
                if (password!=null)
                    changes.add(password);
                if (enable!=null)
                    changes.add(enable);
                if (enableDate!=null)
                    changes.add(enableDate);
                if (disableDate!=null)
                    changes.add(disableDate);
                
                String id = name.getNameValue();
                Uid uid = new Uid(id.toUpperCase());
                Map<String, Attribute> newAttributes = CollectionUtil.newCaseInsensitiveMap();
                newAttributes.putAll(attributes);
                addObjectClass(objectClass, newAttributes);
                ((RacfConnection)_connector.getConnection()).getDirContext().createSubcontext(id, createLdapAttributesFromConnectorAttributes(objectClass, newAttributes));
                if (groups!=null)
                    _connector.setGroupMembershipsForUser(id, groups, owners);
                // Now, process the deferred attributes
                //
                if (changes.size()>0) {
                    changes.add(uid);
                    updateViaLdap(objectClass, changes, options);
                }
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
                Attribute groupOwners = attributes.remove(ATTR_LDAP_CONNECT_OWNER);

                String id = name.getNameValue();
                Uid uid = new Uid(id.toUpperCase());
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
        return getConnectionInfo("racfuserid="+RacfConnector.extractRacfIdFromLdapId(user), 2);
    }

    public List<String> getMembersOfGroupViaLdap(String group) {
        return getConnectionInfo("racfgroupid="+RacfConnector.extractRacfIdFromLdapId(group), 1);
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
            if (!e.toString().contains("NO ENTRIES MEET SEARCH CRITERIA"))
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

    private boolean userExists(String user) {
        List<String> users = getUsersViaLdap("racfid="+RacfConnector.extractRacfIdFromLdapId(user));
        return users.size()==1;
    }

    private boolean groupExists(String group) {
        List<String> groups = getGroupsViaLdap("racfid="+RacfConnector.extractRacfIdFromLdapId(group));
        return groups.size()==1;
    }

    public Map<String, Object> getAttributesFromLdap(ObjectClass objectClass, String ldapName, Set<String> originalAttributesToGet) throws NamingException {
        Map<String, Object> attributesRead = CollectionUtil.newCaseInsensitiveMap();
        Set<String> attributesToGet = new HashSet<String>(originalAttributesToGet);
        
        // A few attributes need to be done via a separate LDAP query, so we save them
        //
        boolean owners = attributesToGet.remove(ATTR_LDAP_CONNECT_OWNER);
        boolean groups = attributesToGet.remove(ATTR_LDAP_GROUPS);
        boolean members = attributesToGet.remove(ATTR_LDAP_GROUP_USERIDS);
        
        // Since Enable is indicated by ATTRIBUTES attribute containing REVOKE
        // we must ensure we fetch the Attribute
        //
        boolean enable = attributesToGet.remove(OperationalAttributes.ENABLE_NAME);
        if (enable && !attributesToGet.contains(ATTR_LDAP_ATTRIBUTES))
            attributesToGet.add(ATTR_LDAP_ATTRIBUTES);
        
        SearchResult ldapObject = getAttributesFromLdap(ldapName, attributesRead, attributesToGet);
        Uid uid = new Uid(ldapObject.getNameInNamespace().toUpperCase());
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
                    connectAttributesToGet.add(ATTR_LDAP_CONNECT_OWNER);
                    for (String group : groupsForUser) {
                        group = RacfConnector.extractRacfIdFromLdapId(group);
                        String root = "racfuserid="+user+"+racfgroupid="+group+",profileType=Connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
                        ownersForUser.add(getConnectOwner(root));
                    }
                    attributesRead.put(ATTR_LDAP_CONNECT_OWNER, ownersForUser);
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
                    attributesRead.put(ATTR_LDAP_GROUP_USERIDS, usersForGroup);
                if (owners) {
                    List<String> ownersForGroup = new ArrayList<String>();
                    Set<String> connectAttributesToGet = new HashSet<String>();
                    connectAttributesToGet.add(ATTR_LDAP_OWNER);
                    for (String user : usersForGroup) {
                        user = RacfConnector.extractRacfIdFromLdapId(user);
                        String root = "racfuserid="+user+"+racfgroupid="+group+",profileType=Connect,"+((RacfConfiguration)_connector.getConfiguration()).getSuffix();
                        ownersForGroup.add(getConnectOwner(root));
                    }
                    attributesRead.put(ATTR_LDAP_CONNECT_OWNER, ownersForGroup);
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
            // Groups must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_GROUPS);

            // Groups must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_GROUPS)) {
                attributesRead.put(ATTR_LDAP_GROUPS, new LinkedList<Object>());
            }
            
            // Default Group must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_DEFAULT_GROUP);

            // Owner Group must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_OWNER);

            // Group Owners must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_CONNECT_OWNER);
            
            // Group Owners must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_CONNECT_OWNER)) {
                attributesRead.put(ATTR_LDAP_CONNECT_OWNER, new LinkedList<Object>());
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
            // Superior Group must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_SUP_GROUP);
            
            // Owner Group must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_OWNER);

            // Groups must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_SUB_GROUPS)) {
                attributesRead.put(ATTR_LDAP_SUB_GROUPS, new LinkedList<Object>());
            }
            // Members must be upcased
            //
            upcaseAttribute(attributesRead, ATTR_LDAP_GROUP_USERIDS);
            
            // Members must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_GROUP_USERIDS)) {
                attributesRead.put(ATTR_LDAP_GROUP_USERIDS, new LinkedList<Object>());
            }
            
            // Group Owners must be filled in if null
            //
            if (!attributesRead.containsKey(ATTR_LDAP_CONNECT_OWNER)) {
                attributesRead.put(ATTR_LDAP_CONNECT_OWNER, new LinkedList<Object>());
            }
        }
        return attributesRead;
    }
    
    private void upcaseAttribute(Map<String, Object> attributesRead, String attributename) {
        if (attributesRead.containsKey(attributename)) {
            Object value = attributesRead.get(attributename);
            if (value instanceof List) {
                List list = (List)value;
                for (int i=0; i<list.size(); i++)
                    list.set(i, list.get(i).toString().toUpperCase());
            } else if (value!=null)
                value = value.toString().toUpperCase();
            attributesRead.put(attributename, value);
        }
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
            // Some attributes expect Lists, but may return a singleton,
            // these must be mapped back into Lists
            //
            if (ATTR_LDAP_GROUPS.equalsIgnoreCase(attribute.getID())) {
                Object value = getValueFromAttribute(attribute);
                if (!(value instanceof List)) {
                    List newValue = new LinkedList();
                    newValue.add(value);
                    value = newValue;
                }
                attributesRead.put(attribute.getID(), value);
            } else {
                attributesRead.put(attribute.getID(), getValueFromAttribute(attribute));
            }
        }
        return ldapObject;
    }
    
    private Object getValueFromAttribute(javax.naming.directory.Attribute attribute) throws NamingException {
        switch (attribute.size()) {
        case 0:
            return null;
        case 1:
            return attribute.get();
        default:
        {
            List<Object> values = new LinkedList<Object>();
            NamingEnumeration ne = attribute.getAll();
            while (ne.hasMore()) {
                values.add(ne.next());
            }
            return values;
        }
        }
    }

    public Uid updateViaLdap(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = CollectionUtil.newCaseInsensitiveMap();
        attributes.putAll(AttributeUtil.toMap(attrs));
        Uid uid = AttributeUtil.getUidAttribute(attrs);
        
        if (uid!=null) {
            if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
                try {
                    Attribute groups = attributes.remove(ATTR_LDAP_GROUPS);
                    Attribute groupOwners = attributes.remove(ATTR_LDAP_CONNECT_OWNER);
                    Attribute expired = attributes.get(ATTR_LDAP_EXPIRED);
                    Attribute password = attributes.get(ATTR_LDAP_PASSWORD);

                    _connector.throwErrorIfNull(groups);
                    _connector.throwErrorIfNull(groupOwners);

                    if (expired!=null && password==null) 
                        throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.EXPIRED_NO_PASSWORD));
                    
                    if (!userExists(uid.getUidValue()))
                        throw new UnknownUidException();
                    
                    ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(uid.getUidValue(), DirContext.REPLACE_ATTRIBUTE, createLdapAttributesFromConnectorAttributes(objectClass, attributes));

                    if (groups!=null)
                        _connector.setGroupMembershipsForUser(uid.getUidValue(), groups, groupOwners);
                } catch (NamingException e) {
                    if (e.toString().contains("INVALID USER"))
                        throw new AlreadyExistsException();
                    else
                        throw new ConnectorException(e);
                }
            } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
                try {
                    Attribute members = attributes.remove(ATTR_LDAP_GROUP_USERIDS);
                    Attribute groupOwners = attributes.remove(ATTR_LDAP_CONNECT_OWNER);
                    
                    ((RacfConnection)_connector.getConnection()).getDirContext().modifyAttributes(uid.getUidValue(), DirContext.REPLACE_ATTRIBUTE, 
                            createLdapAttributesFromConnectorAttributes(objectClass, attributes));
                    if (members!=null)
                        _connector.setGroupMembershipsForGroups(uid.getUidValue(), members, groupOwners);
                } catch (NamingException e) {
                    if (e.toString().contains("INVALID GROUP"))
                        throw new AlreadyExistsException();
                    else
                        throw new ConnectorException(e);
                }
            } else {
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS, objectClass));
            }
        }
        return uid;
    }

    protected void addObjectClass(ObjectClass objectClass, Map<String, Attribute> attrs) {
        List<Object> values = new ArrayList<Object>();
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            for (String userObjectClass : ((RacfConfiguration)_connector.getConfiguration()).getUserObjectClasses())
                values.add(userObjectClass);
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            for (String groupObjectClass : ((RacfConfiguration)_connector.getConfiguration()).getGroupObjectClasses())
                values.add(groupObjectClass);
        }
        attrs.put("objectclass", AttributeBuilder.build("objectclass", values));
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
        Set<String> racfAttributes = CollectionUtil.newCaseInsensitiveSet();
        boolean setRacfAttributes = false;
        boolean negateAttributes = false;
        
        for (Attribute attribute : attributes.values()) {
            String attributeName = attribute.getName().toLowerCase();
            if (attribute.getValue()==null)
                throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.BAD_ATTRIBUTE_VALUE, null));
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                // Ignore Name, Uid
                //
            } else if (attribute.is("objectclass")) {
                BasicAttribute objectClassAttribute = new BasicAttribute("objectclass");
                for (Object value : attribute.getValue())
                    objectClassAttribute.add(value);
                basicAttributes.put(objectClassAttribute);
            } else if (attribute.is(ATTR_LDAP_ATTRIBUTES)) {
                for (Object value : attribute.getValue()) {
                    if (value==null) {
                        throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.BAD_ATTRIBUTE_VALUE, null));
                    } else {
                        String string = value.toString();
                        if (!RacfConnector.POSSIBLE_ATTRIBUTES.contains(string))
                            throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.BAD_ATTRIBUTE_VALUE, string));
                        else
                            racfAttributes.add(string);
                    }
                }
                negateAttributes = true;
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
                if (AttributeUtil.getBooleanValue(attribute))
                    racfAttributes.add("Expired");
                else
                    racfAttributes.add("noExpired");
                setRacfAttributes = true;
            } else if (attribute.is(ATTR_LDAP_ENABLED)) {
                if (AttributeUtil.getBooleanValue(attribute))
                    racfAttributes.add("RESUME");
                else
                    racfAttributes.add("REVOKE");
                setRacfAttributes = true;
            } else if (attribute.is(ATTR_LDAP_RESUME_DATE) || attribute.is(ATTR_LDAP_REVOKE_DATE)) {
                basicAttributes.put(attribute.getName(), AttributeUtil.getStringValue(attribute));
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
                basicAttributes.put(makeBasicAttribute(attributeName, attribute.getValue()));
            }
        }
        if (setRacfAttributes) {
            // Since RACF LDAP does't obey replace semantics, we need to patch in
            // any values that should be removed
            //
            //TODO: an alternative implementation would be to read the user object
            //      and get the set of current values for ATTRIBUTE. This has the
            //      advantage of getting the exact set of values, but the disadvantage
            //      that it requires an extra read of the user.
            //TODO: this probably also applies to 'RacfConnectAttributes', which we
            //      are not currently supporting
            List<Object> finalValue = new LinkedList<Object>();
            if (negateAttributes) {
                for (String attrValue : RacfConnector.POSSIBLE_ATTRIBUTES)
                    if (!racfAttributes.contains(attrValue))
                        finalValue.add("NO"+attrValue);
            }
            finalValue.addAll(racfAttributes);
            basicAttributes.put(makeBasicAttribute(ATTR_LDAP_ATTRIBUTES, finalValue));
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
            } else {
                basicAttributes.put(makeBasicAttribute(attributeName, attribute.getValue()));
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