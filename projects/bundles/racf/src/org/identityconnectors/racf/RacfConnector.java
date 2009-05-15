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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;


@ConnectorClass(configurationClass= RacfConfiguration.class, displayNameKey="RACFConnector")
public class RacfConnector implements Connector, CreateOp, PoolableConnector,
DeleteOp, SearchOp<String>, UpdateOp, SchemaOp, ScriptOnConnectorOp, AttributeNormalizer {

    public static final String         SEPARATOR           ="*";
    public static final String         SEPARATOR_REGEX     ="\\*";
    public static final String         RACF_GROUP_NAME     ="RacfGroup";
    public static final ObjectClass    RACF_GROUP          = new ObjectClass(RACF_GROUP_NAME);
    public static final String         RACF_CONNECTION_NAME ="RacfConnection";
    public static final ObjectClass    RACF_CONNECTION     = new ObjectClass(RACF_CONNECTION_NAME);

    private Map<String, AttributeInfo>  _accountAttributes = null;
    private Map<String, AttributeInfo>  _groupAttributes = null;

    private RacfConnection              _connection;
    private RacfConfiguration           _configuration;
    private CommandLineUtil             _clUtil;
    private LdapUtil                    _ldapUtil;
    private final SimpleDateFormat      _dateFormat = new SimpleDateFormat("MM/dd/yy");
    private final SimpleDateFormat      _resumeRevokeFormat = new SimpleDateFormat("MMMM dd, yyyy");
    private final Pattern               _racfTimestamp = Pattern.compile("(\\d+)\\.(\\d+)(?:/(\\d+):(\\d+):(\\d+))?");
    private final Pattern               _connectionPattern  = Pattern.compile("racfuserid=([^+]+)\\+racfgroupid=([^,]+),.*");

    public RacfConnector() {
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        System.out.println("disposing connector");
        _connection.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public Configuration getConfiguration() {
        return this._configuration;
    }

    /**
     * {@inheritDoc}
     */
    public void init(Configuration configuration) {
        try {
            System.out.println("initializing connector");
            _configuration = (RacfConfiguration)configuration;
            _clUtil = new CommandLineUtil(this);
            _ldapUtil = new LdapUtil(this);
            _connection =  new RacfConnection(_configuration);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    RacfConnection getConnection() {
        return _connection;
    }

    /**
     * {@inheritDoc}
     */
    public Uid create(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Set<Attribute> ldapAttrs = new HashSet<Attribute>();
        Set<Attribute> commandLineAttrs = new HashSet<Attribute>();
        splitUpOutgoingAttributes(objectClass, attrs, ldapAttrs, commandLineAttrs);
        if (isLdapConnectionAvailable()) {
            Uid uid = _ldapUtil.createViaLdap(objectClass, ldapAttrs, options);
            if (hasNonSpecialAttributes(commandLineAttrs)) {
                if (_configuration.getUserName()==null)
                    throw new ConnectorException(_configuration.getMessage(RacfMessages.NEED_COMMAND_LINE));
                _clUtil.updateViaCommandLine(objectClass, commandLineAttrs, options);
            }
            return uid;
        } else {
            if (hasNonSpecialAttributes(ldapAttrs))
                throw new ConnectorException(_configuration.getMessage(RacfMessages.NEED_LDAP));
            return _clUtil.createViaCommandLine(objectClass, commandLineAttrs, options);
        }
    }

    private boolean hasNonSpecialAttributes(Set<Attribute> attrs) {
        for (Attribute attribute : attrs) {
            if (!AttributeUtil.isSpecial(attribute)) {
                return true;
            }
        }
        return false;
    }
    
    private void splitUpOutgoingAttributes(ObjectClass objectClass, Set<Attribute> attrs, Set<Attribute> ldapAttrs, Set<Attribute> commandLineAttrs) {
        // Attribute consistency checking
        //
        Map<String, Attribute> attributes = AttributeUtil.toMap(attrs);
        Attribute enableDate  = attributes.get(OperationalAttributes.ENABLE_DATE_NAME);
        Attribute disableDate = attributes.get(OperationalAttributes.DISABLE_DATE_NAME);
        Long now              = new Date().getTime();
        
        if (disableDate!=null) {
            Long time = AttributeUtil.getLongValue(disableDate);
            if (time<now)
                throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.PAST_DISABLE_DATE));
        }
        if (enableDate!=null) {
            Long time = AttributeUtil.getLongValue(enableDate);
            if (time<now)
                throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.PAST_ENABLE_DATE));
        }
        
        for (Attribute attribute : attrs) {
            // Remap special attributes as needed
            //
            if (attribute.is(OperationalAttributes.PASSWORD_NAME))  {
                if (isLdapConnectionAvailable())
                    attribute = AttributeBuilder.build(ATTR_LDAP_PASSWORD, attribute.getValue());
                else
                    attribute = AttributeBuilder.build(ATTR_CL_PASSWORD, attribute.getValue());
            } else if (attribute.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)) {
                if (isLdapConnectionAvailable())
                    attribute = AttributeBuilder.build(ATTR_LDAP_EXPIRED, attribute.getValue());
                else
                    attribute = AttributeBuilder.build(ATTR_CL_EXPIRED, attribute.getValue());
            } else if (attribute.is(OperationalAttributes.DISABLE_DATE_NAME)) {
                Date date = new Date(AttributeUtil.getLongValue(disableDate));
                String dateValue = _dateFormat.format(date);
                if (isLdapConnectionAvailable())
                    attribute = AttributeBuilder.build(ATTR_LDAP_REVOKE_DATE, dateValue);
                else
                    attribute = AttributeBuilder.build(ATTR_CL_REVOKE_DATE, dateValue);
            } else if (attribute.is(OperationalAttributes.ENABLE_DATE_NAME)) {
                Date date = new Date(AttributeUtil.getLongValue(enableDate));
                String dateValue = _dateFormat.format(date);
                if (isLdapConnectionAvailable())
                    attribute = AttributeBuilder.build(ATTR_LDAP_RESUME_DATE, dateValue);
                else
                    attribute = AttributeBuilder.build(ATTR_CL_RESUME_DATE, dateValue);
            } else if (attribute.is(OperationalAttributes.ENABLE_NAME)) {
                if (isLdapConnectionAvailable())
                    ; //TODO: attribute = AttributeBuilder.build(ATTR_LDAP_ENABLED, attribute.getValue());
                else
                    attribute = AttributeBuilder.build(ATTR_CL_ENABLED, attribute.getValue());
            } 

            // Put the attribute on the appropriate attribute list(s)
            //
            if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                commandLineAttrs.add(attribute);
                ldapAttrs.add(attribute);
            } else if (attribute.getName().contains(SEPARATOR) || 
                    (!isLdapConnectionAvailable() && objectClass.is(RACF_CONNECTION_NAME))) {
                // Even when we are in command-line form, use LDAP-style names for connection attributes,
                // since we don't get them via parsing
                //
                commandLineAttrs.add(attribute);
            } else {
                ldapAttrs.add(attribute);
            }
        }
    }

    private void splitUpIncomingAttributes(Set<String> attrs, Set<String> ldapAttrs, Set<String> commandLineAttrs) {
        for (String attribute : attrs) {
            // Remap special attributes as needed
            //
            if (attribute.equals(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add(ATTR_LDAP_PASSWORD_INTERVAL);
                else
                    commandLineAttrs.add(ATTR_CL_PASSWORD_INTERVAL);
            } else if (attribute.equals(PredefinedAttributes.LAST_LOGIN_DATE_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add(ATTR_LDAP_LAST_ACCESS);
                else
                    commandLineAttrs.add(ATTR_CL_LAST_ACCESS);
            } else if (attribute.equals(OperationalAttributes.ENABLE_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add("TODO");
                else
                    commandLineAttrs.add(ATTR_CL_ENABLED);
            } else if (attribute.equals(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add(ATTR_LDAP_PASSWORD_CHANGE);
                else
                    commandLineAttrs.add(ATTR_CL_PASSDATE);
            } else if (attribute.equals(OperationalAttributes.DISABLE_DATE_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add(ATTR_LDAP_REVOKE_DATE);
                else
                    commandLineAttrs.add(ATTR_CL_REVOKE_DATE);
            } else if (attribute.equals(OperationalAttributes.ENABLE_DATE_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add(ATTR_LDAP_RESUME_DATE);
                else
                    commandLineAttrs.add(ATTR_CL_RESUME_DATE);
            } else if (attribute.equals(OperationalAttributes.PASSWORD_NAME)) {
                throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.ATTRIBUTE_NOT_READABLE, OperationalAttributes.PASSWORD_NAME));
            } else if (attribute.equals(OperationalAttributes.PASSWORD_EXPIRED_NAME)) {
                if (isLdapConnectionAvailable())
                    ldapAttrs.add(OperationalAttributes.PASSWORD_EXPIRED_NAME);
                else
                    commandLineAttrs.add(ATTR_CL_EXPIRED);
            } else if (attribute.equals(Name.NAME)) {
                commandLineAttrs.add(attribute);
                ldapAttrs.add(attribute);
            } else if (attribute.contains(SEPARATOR)) {
                commandLineAttrs.add(attribute);
            } else {
                ldapAttrs.add(attribute);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        if (isLdapConnectionAvailable()) {
            _ldapUtil.deleteViaLdap(objectClass, uid);
        } else {
            _clUtil.deleteViaCommandLine(objectClass, uid);
        }
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        if (isLdapConnectionAvailable()) {
            if (oclass.is(ObjectClass.ACCOUNT_NAME))
                return new RacfUserFilterTranslator();
            if (oclass.is(RACF_GROUP_NAME))
                return new RacfGroupFilterTranslator();
            if (oclass.is(RACF_CONNECTION_NAME))
                return new RacfConnectFilterTranslator();
            else
                return null;
        } else {
            if (oclass.is(ObjectClass.ACCOUNT_NAME))
                return new RacfCommandLineFilterTranslator();
            if (oclass.is(RACF_GROUP_NAME))
                return new RacfCommandLineFilterTranslator();
            else
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        System.out.println("executeQuery:"+query);
        List<String> names = new LinkedList<String>();

        if (objectClass.is(ObjectClass.ACCOUNT_NAME))
            names = getUsers(query);
        else if (objectClass.is(RACF_GROUP_NAME))
            names = getGroups(query);

        try {
            TreeSet<String> attributesToGet = null;
            schema();

            if (options!=null && options.getAttributesToGet()!=null) {
                attributesToGet = new TreeSet<String>();
                for (String name : options.getAttributesToGet()) {
                    // Ignore request for "description"
                    //
                    if (!name.equals("description"))
                        attributesToGet.add(name);
                }
            } else {
                if (objectClass.is(ObjectClass.ACCOUNT_NAME))
                    attributesToGet = getDefaultAttributes(_accountAttributes);
                else if (objectClass.is(RACF_GROUP_NAME))
                    attributesToGet = getDefaultAttributes(_groupAttributes);
            }
            
            boolean wantUid     = (attributesToGet!=null && attributesToGet.remove(Uid.NAME));
            boolean getNameOnly = (attributesToGet!=null && attributesToGet.size()==1 && Name.NAME.equalsIgnoreCase(attributesToGet.first()));
            boolean getNothing  = (attributesToGet!=null && attributesToGet.size()==0);
            
            // It's an error to request attributes from a source we can't use
            //
            Set<String> ldapAttrs = new TreeSet<String>();
            Set<String> commandLineAttrs = new TreeSet<String>();
            splitUpIncomingAttributes(attributesToGet, ldapAttrs, commandLineAttrs);
            int ldapSize = ldapAttrs.size();
            if (ldapAttrs.contains(Name.NAME))
                ldapSize--;
            if (!isLdapConnectionAvailable() && ldapSize>0)
                throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.ATTRS_NO_LDAP));

            int commandLineSize = commandLineAttrs.size();
            if (commandLineAttrs.contains(Name.NAME))
                commandLineSize--;
            if (StringUtil.isBlank(_configuration.getUserName()) && commandLineSize>0)
                throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.ATTRS_NO_CL));
            
            SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, ldapAttrs.toArray(new String[0]), true, true);
            for (String name : names) {
                try {
                    // We can special case getting at most just name
                    //
                    ConnectorObject object = null;
                    if (getNameOnly || getNothing) {
                        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                        builder.setUid(name);
                        if (getNameOnly)
                            builder.setName(name);
                        object = builder.build();
                    } else {
                        SearchResult searchResult = null;
                        if (isLdapConnectionAvailable()) {
                            NamingEnumeration<SearchResult> results = _connection.getDirContext().search(name, "(objectclass=*)", subTreeControls);
                            searchResult = results.next();
                        }
                        object = buildObject(objectClass, searchResult, _clUtil.getAttributesFromCommandLine(objectClass, name, isLdapConnectionAvailable(), commandLineAttrs), attributesToGet, wantUid);
                    }
                    handler.handle(object);
                } catch (UnknownUidException uue) {
                    // Ignore this, user disappeared during query
                }
            }
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }
    
    private TreeSet<String> getDefaultAttributes(Map<String, AttributeInfo> infos) {
        TreeSet<String> results = new TreeSet<String>();
        for (Map.Entry<String, AttributeInfo> entry : infos.entrySet()) {
            if (entry.getValue().isReturnedByDefault())
                results.add(entry.getKey());
        }
        return results;
    }

    private ConnectorObject buildObject(ObjectClass objectClass, SearchResult user, Map<String, Object> attributesFromCommandLine, Set<String> attributesToGet, boolean wantUid) throws NamingException {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        Uid uid = null;
        if (user!=null) {
            uid = new Uid(user.getNameInNamespace());
            builder.setUid(uid);
            builder.setName(user.getNameInNamespace());
            Attributes attributes = user.getAttributes();
            NamingEnumeration<? extends javax.naming.directory.Attribute> attributeEnum = attributes.getAll();
            while (attributeEnum.hasMore()) {
                javax.naming.directory.Attribute attribute = attributeEnum.next();
                Object value = attribute.get();
                if (includeInAttributes(objectClass, attribute.getID(), attributesToGet)) {
                    if (value instanceof Collection)
                        builder.addAttribute(attribute.getID(), (Collection<? extends Object>)value);
                    else
                        builder.addAttribute(attribute.getID(), value);
                }
            }
            if (includeInAttributes(objectClass, ATTR_CL_GROUPS, attributesToGet)) {
                builder.addAttribute(ATTR_CL_GROUPS, getGroupsForUser(user.getNameInNamespace()));
            }
        }
        if (attributesFromCommandLine!=null) {
            if (user==null) {
                String name = (String)attributesFromCommandLine.get(ATTR_CL_USERID);
                uid = new Uid(name);
                builder.setUid(uid);
                builder.setName(name);
            }
            for (Map.Entry<String, Object> entry : attributesFromCommandLine.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (includeInAttributes(objectClass, name, attributesToGet)) {
                    if (value instanceof Collection)
                        builder.addAttribute(name, (Collection<? extends Object>)value);
                    else if (value==null)
                        builder.addAttribute(name);
                    else
                        builder.addAttribute(name, value);
                }
            }
        }
        if (wantUid)
            builder.addAttribute(uid);
        ConnectorObject next = builder.build();
        return next;
    }

    List<String> getMembersOfGroup(String group) {
        if (isLdapConnectionAvailable()) {
            return _ldapUtil.getMembersOfGroupViaLdap(group);
        } else {
            return _clUtil.getMembersOfGroupViaCommandLine(group);
        }
    }

    void setGroupMembershipsForUser(String name, Attribute groupsAttribute, Attribute ownersAttribute) {
        checkConnectionConsistency(groupsAttribute, ownersAttribute);

        List<Object> groups = groupsAttribute.getValue();
        List<Object> owners = ownersAttribute.getValue();
        
        List<String> currentGroups = getGroupsForUser(name);
        String defaultGroup = currentGroups.get(0);
        
        for (String currentGroup : currentGroups) {
            if (!groups.contains(currentGroup) && currentGroup!=defaultGroup) {
                // Group is being eliminated
                //
                String connectionName = createConnectionId(name, currentGroup);
                delete(RACF_CONNECTION, new Uid(connectionName), null);
            }
        }

        for (int i=0; i<groups.size(); i++) {
            Object newGroup = groups.get(i);
            Object newOwner = owners.get(i);
            if (!currentGroups.contains(newGroup)) {
                // Group is being added
                //
                String connectionName = createConnectionId(name, (String)newGroup);
                Set<Attribute> attributes = new HashSet<Attribute>();
                attributes.add(AttributeBuilder.build(Name.NAME, connectionName));
                attributes.add(AttributeBuilder.build(ATTR_LDAP_OWNER, newOwner));
                create(RACF_CONNECTION, attributes, new OperationOptions(new HashMap<String, Object>()));
            }
        }
    }

    private void checkConnectionConsistency(Attribute groupsAttribute,
            Attribute ownersAttribute) {
        // members and owners must be the same length
        //
        boolean badSize = false;
        try {
            if (ownersAttribute!=null)
                badSize = (groupsAttribute.getValue().size()!=ownersAttribute.getValue().size());
        } catch (NullPointerException npe) {
            badSize = true;
        }
        if (badSize)
            throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.OWNER_INCONSISTENT));
    }

    private String createConnectionId(String name, String currentGroup) {
        String connectionName = "racfuserid="+name+"+racfgroupid="+
            currentGroup+",profileType=connect,"+_configuration.getSuffix();
        return connectionName;
    }

    void setGroupMembershipsForGroups(String name, Attribute membersAttribute, Attribute ownersAttribute) {
        checkConnectionConsistency(membersAttribute, ownersAttribute);

        List<Object> members = membersAttribute.getValue();
        List<Object> owners  = ownersAttribute==null?null:ownersAttribute.getValue();
        
        List<String> currentMembers = getMembersOfGroup(name);
        
        for (String currentMember : currentMembers) {
            if (!members.contains(currentMember)) {
                String connectionName = createConnectionId(currentMember, name);
                delete(RACF_CONNECTION, new Uid(connectionName), null);
            }
        }
        for (int i=0; i<members.size(); i++) {
            Object newMember = members.get(i);
            Object newOwner  = ownersAttribute==null?null:owners.get(i);
            if (!currentMembers.contains(newMember)) {
                // Member is being added
                //
                String connectionName = createConnectionId((String)newMember, name);
                Set<Attribute> attributes = new HashSet<Attribute>();
                attributes.add(AttributeBuilder.build(Name.NAME, connectionName));
                if (newOwner!=null)
                    attributes.add(AttributeBuilder.build(ATTR_LDAP_OWNER, newOwner));
                create(RACF_CONNECTION, attributes, new OperationOptions(new HashMap<String, Object>()));
            }
        }
    }

    List<String> getGroupsForUser(String user) {
        if (isLdapConnectionAvailable()) {
            return _ldapUtil.getGroupsForUserViaLdap(user);
        } else {
            return _clUtil.getGroupsForUserViaCommandLine(user);
        }
    }
    private final static Pattern            _racfidPattern      = Pattern.compile("racfid=([^,]*),.*");


    /**
     * Extract the RACF account id from a RACF LDAP Uid.
     * 
     * @param uid
     * @return
     */
    String createAccountNameFromUid(Uid uid) {
        String uidString = uid.getUidValue();
        return extractRacfIdFromLdapId(uidString);
    }

    /**
     * Create a RACF LDAP Uid given a name.
     * 
     * @param name
     * @return
     */
    Uid createUidFromName(ObjectClass objectClass, String name) {
        if (objectClass.is(ObjectClass.ACCOUNT_NAME))
            return new Uid("racfid="+name.toUpperCase()+",profileType=user,"+_configuration.getSuffix());
        else if (objectClass.is(RACF_GROUP_NAME))
            return new Uid("racfid="+name.toUpperCase()+",profileType=group,"+_configuration.getSuffix());
        else 
            return null;
    }

    String extractRacfIdFromLdapId(String uidString) {
        Matcher matcher = _racfidPattern.matcher(uidString);
        if (matcher.matches())
            return matcher.group(1);
        else
            return null;
    }

    public String[] extractRacfIdAndGroupIdFromLdapId(String uidString) {
        Matcher matcher = _connectionPattern.matcher(uidString);
        if (matcher.matches())
            return new String[] {matcher.group(1), matcher.group(2)};
        else
            return null;
    }

    /**
     * Get the names of the users satisfying the query.
     * 
     * @param query -- a query to select users
     * @return a List<String> of user names
     */
    private List<String> getUsers(String query) {
        if (isLdapConnectionAvailable()) {
            return _ldapUtil.getUsersViaLdap(query);
        } else {
            return _clUtil.getUsersViaCommandLine(query);
        }
    }

    /**
     * Get the names of the groups satisfying the query.
     * 
     * @param query -- a query to select groups
     * @return a List<String> of group names
     */
    private List<String> getGroups(String query) {
        if (isLdapConnectionAvailable()) {
            return _ldapUtil.getGroupsViaLdap(query);
        } else {
            return _clUtil.getGroupsViaCommandLine(query);
        }
    }

    public Uid update(ObjectClass obj, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        return update(obj, AttributeUtil.addUid(attrs, uid), options);
    }

    /**
     * {@inheritDoc}
     */
    Uid update(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Set<Attribute> ldapAttrs = new HashSet<Attribute>();
        Set<Attribute> commandLineAttrs = new HashSet<Attribute>();
        if (AttributeUtil.getNameFromAttributes(attrs)!=null) {
            throw new IllegalArgumentException(_configuration.getMessage(RacfMessages.ATTRIBUTE_NOT_UPDATEABLE, Name.NAME));
        }
        // TODO: if PASSWORD is specified, but EXPIRED is not,
        //  we must reconstruct its value by reading the user.
        //
        splitUpOutgoingAttributes(objectClass, attrs, ldapAttrs, commandLineAttrs);
        if (isLdapConnectionAvailable()) {
            Uid uid = _ldapUtil.updateViaLdap(objectClass, ldapAttrs);
            if (hasNonSpecialAttributes(commandLineAttrs)) {
                if (_configuration.getUserName()==null)
                    throw new ConnectorException(_configuration.getMessage(RacfMessages.NEED_COMMAND_LINE));
                _clUtil.updateViaCommandLine(objectClass, commandLineAttrs, options);
            }
            return uid;
        } else {
            if (hasNonSpecialAttributes(ldapAttrs))
                throw new ConnectorException(_configuration.getMessage(RacfMessages.NEED_LDAP));
            return _clUtil.updateViaCommandLine(objectClass, commandLineAttrs, options);
        }
    }
    
    private Schema clSchema() {
        final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

        {
            // RACF Users
            //
            Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
    
            // Required Attributes
            //
            attributes.add(buildNonupdateAttribute(Name.NAME,                           String.class, true));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_DFLTGRP,                  String.class));
    
            // Optional Attributes (have RACF default values)
            //
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OWNER,                    String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NAME,                     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_DATA,                     String.class));
    
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_ACCTNUM,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_HOLDCLASS,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_JOBCLASS,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_MSGCLASS,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_PROC,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_SIZE,                 Integer.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_MAXSIZE,              Integer.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_SYSOUTCLASS,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_UNIT,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_USERDATA,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_TSO_COMMAND,              String.class));
    
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_UID,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_HOME,                String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_PROGRAM,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_CPUTIMEMAX,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_ASSIZEMAX,           String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_FILEPROCMAX,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_PROCUSERMAX,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_THREADSMAX,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_OMVS_MMAPAREAMAX,         String.class));
    
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_CICS_TIMEOUT,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_CICS_OPPRTY,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_CICS_OPIDENT,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_CICS_XRFSOFF,             String.class));
           
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NETVIEW_NGMFVSPN,         boolean.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NETVIEW_NGMFADMN,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NETVIEW_MSGRECVR,         boolean.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NETVIEW_IC,               String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NETVIEW_CTL,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_CL_NETVIEW_CONSNAME,         String.class));
    
            // Multi-valued attributes
            //
            attributes.add(buildMultivaluedAttribute(ATTR_CL_GROUP_CONN_OWNERS,         String.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_ATTRIBUTES,                String.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_NETVIEW_OPCLASS,           String.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_NETVIEW_DOMAINS,           String.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_CICS_OPCLASS,              Integer.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_CICS_RSLKEY,               Integer.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_CICS_TSLKEY,               Integer.class, false));
            attributes.add(buildMultivaluedAttribute(ATTR_CL_GROUPS,                    String.class, false));
    
            // Catalog Attributes (make non-default)
            //
            attributes.add(buildNonDefaultAttribute(ATTR_CL_MASTER_CATALOG,             String.class));
            attributes.add(buildNonDefaultAttribute(ATTR_CL_USER_CATALOG,               String.class));
            attributes.add(buildNonDefaultAttribute(ATTR_CL_CATALOG_ALIAS,              String.class));
    
            // Update-only attributes
            //
            attributes.add(buildUpdateonlyAttribute(ATTR_CL_TSO_DELETE_SEGMENT,         String.class, false));
    
            // Operational Attributes
            //
            attributes.add(buildReadonlyAttribute(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME, long.class, false));
            attributes.add(OperationalAttributeInfos.ENABLE);
            attributes.add(OperationalAttributeInfos.ENABLE_DATE);
            attributes.add(OperationalAttributeInfos.DISABLE_DATE);
            attributes.add(OperationalAttributeInfos.PASSWORD);
            attributes.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
            attributes.add(PredefinedAttributeInfos.LAST_LOGIN_DATE);
            attributes.add(PredefinedAttributeInfos.LAST_PASSWORD_CHANGE_DATE);
    
            _accountAttributes = AttributeInfoUtil.toMap(attributes);
            schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        }
        //----------------------------------------------------------------------

        // RACF Groups
        //
        {
            Set<AttributeInfo> groupAttributes = new HashSet<AttributeInfo>();
            groupAttributes.add(buildNonupdateAttribute(Name.NAME,                           String.class, true));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_CL_SUPGROUP,                 String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_CL_OWNER,                    String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_CL_DATA,                     String.class));
            groupAttributes.add(buildMultivaluedAttribute(ATTR_CL_MEMBERS,                   String.class, false));
            groupAttributes.add(buildMVROAttribute(ATTR_CL_GROUPS,                           String.class));
    
            _groupAttributes = AttributeInfoUtil.toMap(groupAttributes);
            schemaBuilder.defineObjectClass(RACF_GROUP_NAME, groupAttributes);
        }

        return schemaBuilder.build();
    }

    private Schema ldapSchema() {
        final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

        // RACF Users
        //
        {
            Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
    
            // Required Attributes
            //
            attributes.add(buildNonupdateAttribute(Name.NAME,                            String.class, true));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DEFAULT_GROUP,           String.class));
    
            // Optional Attributes (have RACF default values)
            //
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DATA,                    String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_MODEL,                   String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OWNER,                   String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_PASSWORD,                String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_PROGRAMMER_NAME,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DEFAULT_GROUP,           String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_SECURITY_LEVEL,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_SECURITY_CAT_LIST,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_REVOKE_DATE,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_RESUME_DATE,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_LOGON_DAYS,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_LOGON_TIME,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CLASS_NAME,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CONNECT_GROUP,           String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_SECURITY_LABEL,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_DATA_APP,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_DATA_CLASS,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_MGMT_CLASS,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_STORAGE_CLASS,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_ACCOUNT_NUMBER,      String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_DEFAULT_CMD,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_DESTINATION,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_MESSAGE_CLASS,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_DEFAULT_LOGIN,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_LOGIN_SIZE,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_MAX_REGION_SIZE,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_DEFAULT_SYSOUT,      String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_USERDATA,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_DEFAULT_UNIT,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TSO_SECURITY_LABEL,      String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_LANG_PRIMARY,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_LANG_SECONDARY,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CICS_OPER_ID,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CICS_OPER_CLASS,         String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CICS_OPER_PRIORITY,      String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CICS_OPER_RESIGNON,      String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_CICS_TERM_TIMEOUT,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_STORAGE,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_AUTH,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_MFORM,                String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_LEVEL,                String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_MONITOR,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_ROUTCODE,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_LOG_CMD_RESPONSE,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_MGID,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_DOM,                  String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_KEY,                  String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_CMDSYS,               String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_UD,                   String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_MSCOPE_SYSTEMS,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_ALTGROUP,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OP_AUTO,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_USER_NAME,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_BUILDING,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_DEPARTMENT,           String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_ROOM,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_ADDRESS_LINE1,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_ADDRESS_LINE2,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_ADDRESS_LINE3,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_ADDRESS_LINE4,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_WA_ACCOUNT_NUMBER,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_UID,                String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_HOME,               String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_INIT_PROGRAM,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_MAX_CPUTIME,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_MAX_ADDR_SPACE,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_MAX_FILES,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_MAX_THREADS,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OMVS_MAX_MEMORY_MAP,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_NINITIALCMD,          String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DEFAULT_CONSOLE,      String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_CTL,                  String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_MESSAGE_RECEIVER,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_OPERATOR_CLASS,       String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DOMAINS,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_NGMFADM,              String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DCE_UUID,             String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DCE_PRINCIPAL,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DCE_HOME_CELL,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DCE_HOME_CELL_UUID,   String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NV_DCE_AUTOLOGIN,        String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OVM_UID,                 String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OVM_HOME,                String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OVM_INITIAL_PROGRAM,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OVM_FILESYSTEM_ROOT,     String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_LN_SHORT_NAME,           String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_NDS_USER_NAME,           String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_KERB_NAME,               String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_KERB_MAX_TICKET_LIFE,    String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_KERB_ENCRYPT,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_PROXY_BINDDN,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_PROXY_BINDPW,            String.class));
            attributes.add(AttributeInfoBuilder.build(ATTR_LDAP_PROXY_HOST,              String.class));
    
            // Multi-valued attributes
            //
            attributes.add(buildMultivaluedAttribute(ATTR_LDAP_ATTRIBUTES,               String.class, false));
            attributes.add(buildReadOnlyMultivaluedAttribute(ATTR_LDAP_GROUPS,           String.class));
    
            // Operational Attributes
            //
            attributes.add(OperationalAttributeInfos.PASSWORD);
            attributes.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
            attributes.add(PredefinedAttributeInfos.PASSWORD_CHANGE_INTERVAL);
            attributes.add(PredefinedAttributeInfos.LAST_LOGIN_DATE);
            attributes.add(PredefinedAttributeInfos.LAST_PASSWORD_CHANGE_DATE);
    
            _accountAttributes = AttributeInfoUtil.toMap(attributes);
            schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        }
        
        //----------------------------------------------------------------------

        // RACF Groups
        //
        {
            Set<AttributeInfo> groupAttributes = new HashSet<AttributeInfo>();
            groupAttributes.add(buildNonupdateAttribute(Name.NAME,                       String.class, true));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DATA,               String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_MODEL,              String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_OWNER,              String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_SUP_GROUP,          String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_TERM_UACC,          String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_UNIVERSAL,          String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_DATA_APP,       String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_DATA_CLASS,     String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_MGMT_CLASS,     String.class));
            groupAttributes.add(AttributeInfoBuilder.build(ATTR_LDAP_DPF_STORAGE_CLASS,  String.class));
    
            // Read-only Multi-valued Attributes
            //
            groupAttributes.add(buildMVROAttribute(ATTR_LDAP_SUB_GROUP,                  String.class));
            groupAttributes.add(buildMVROAttribute(ATTR_LDAP_GROUP_USERIDS,              String.class));
    
            groupAttributes.add(buildNonDefaultMultivaluedAttribute(ATTR_LDAP_MEMBERS,   String.class, false));
            _groupAttributes = AttributeInfoUtil.toMap(groupAttributes);
            schemaBuilder.defineObjectClass(RACF_GROUP_NAME, groupAttributes);
        }
        return schemaBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        if (isLdapConnectionAvailable())
            return ldapSchema();
        else
            return clSchema();
    }

    private AttributeInfo buildMVROAttribute(String name, Class<?> clazz) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(false);
        builder.setMultiValued(true);
        builder.setCreateable(false);
        builder.setUpdateable(false);
        return builder.build();
    }

    private AttributeInfo buildMultivaluedAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(true);
        return builder.build();
    }
    
    private AttributeInfo buildReadOnlyMultivaluedAttribute(String name, Class<?> clazz) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(false);
        builder.setMultiValued(true);
        builder.setCreateable(false);
        builder.setUpdateable(false);
        return builder.build();
    }

    private AttributeInfo buildNonDefaultMultivaluedAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(true);
        builder.setReturnedByDefault(false);
        return builder.build();
    }

    private AttributeInfo buildNonupdateAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(false);
        builder.setUpdateable(false);
        builder.setCreateable(true);
        builder.setReadable(true);
        builder.setReturnedByDefault(true);
        return builder.build();
    }

    private AttributeInfo buildUpdateonlyAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(false);
        builder.setUpdateable(true);
        builder.setCreateable(false);
        builder.setReadable(false);
        builder.setReturnedByDefault(false);
        return builder.build();
    }

    private AttributeInfo buildNoncreateAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(false);
        builder.setUpdateable(true);
        builder.setCreateable(false);
        builder.setReadable(true);
        builder.setReturnedByDefault(true);
        return builder.build();
    }

    private AttributeInfo buildReadonlyAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(false);
        builder.setUpdateable(false);
        builder.setCreateable(false);
        builder.setReadable(true);
        builder.setReturnedByDefault(true);
        return builder.build();
    }

    private AttributeInfo buildNonDefaultAttribute(String name, Class<?> clazz) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(false);
        builder.setReturnedByDefault(false);
        builder.setCreateable(true);
        builder.setUpdateable(true);
        return builder.build();
    }
    
    private boolean includeInAttributes(ObjectClass objectClass, String attribute, Collection<String> attributesToGet) {
        if (attribute.equalsIgnoreCase(Name.NAME))
            return true;
        if (attributesToGet!=null) {
            return attributesToGet.contains(attribute);
        }
        return false;
    }

    /**
     * Run a script on the connector.
     * <p>
     * This needs to be locally implemented, because the RacfConnection is the LDAP 
     * connection, and scripts need to be run in the context of a RW3270 connection.
     * So, we need to borrow such a connection from the connection pool.
     * <p>
     * One additional argument is added to the set of script arguments:
     * <ul>
     * <li><b>rw3270Connection</b> -- an org.identityconnectors.rw3270.RW3270Connection that is logged in to the host
     * </li>
     * </ul>
     * <p>
     * If an exception occurs running the script, and attempt is made to reset the
     * connection. If the reset fails, the connection is not returned to the pool.
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        return _clUtil.runScriptOnConnector(request, options);
    }

    private boolean isLdapConnectionAvailable() {
        return _configuration.getLdapUserName()!=null;
    }

    /**
     * {@inheritDoc}
     */
    public void checkAlive() {
        _connection.test();
        if (false && !StringUtil.isBlank(_configuration.getUserName())) {
            String output = _clUtil.getCommandOutput("TIME");
            if (!output.contains("IJK"))
                throw new ConnectorException(_configuration.getMessage(RacfMessages.CONNECTION_DEAD));
        }
    }

    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        List<Object> values = attribute.getValue();
        List<Object> newValues = new LinkedList<Object>();
        if (values==null)
            return AttributeBuilder.build(attribute.getName().toUpperCase());
        for (Object value : values)
            if (value instanceof String)
                newValues.add(((String)value).toUpperCase());
            else
                newValues.add(value);
        if (attribute instanceof Name)
            return new Name(((String)newValues.get(0)).toUpperCase());
        else if (attribute instanceof Uid)
            return new Uid(((String)newValues.get(0)).toUpperCase());
        else
            return AttributeBuilder.build(attribute.getName().toUpperCase(), newValues);
    }

    Long convertFromResumeRevokeFormat(Object value) {
        try {
            return _resumeRevokeFormat.parse(value.toString()).getTime();
        } catch (ParseException pe) {
            return null;
        }
    }
    
    Long convertFromRacfTimestamp(Object value) {
        if (value==null)
            return null;
        
        Matcher matcher = _racfTimestamp.matcher(value.toString());
        if (matcher.matches()) {
            String year    = matcher.group(1);
            String day     = matcher.group(2);
            String hours   = matcher.group(3);
            String minutes = matcher.group(4);
            String seconds = matcher.group(5);
            
            int yearValue = Integer.parseInt(year)+2000;
            int dayValue  = Integer.parseInt(day);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, yearValue);
            calendar.set(Calendar.DAY_OF_YEAR, dayValue);
            
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            if (hours!=null) {
                int hoursValue   = Integer.parseInt(hours);
                int minutesValue = Integer.parseInt(minutes);
                int secondsValue = Integer.parseInt(seconds);
                calendar.set(Calendar.HOUR_OF_DAY, hoursValue);
                calendar.set(Calendar.MINUTE, minutesValue);
                calendar.set(Calendar.SECOND, secondsValue);
            }
            Date date = calendar.getTime();
            return date.getTime();
        } else {
            return null;
        }
    }
    
}
/*
private void processPasswordInterval(Map map) {
String interval = (String)map.get(PASSWORD_INTERVAL);
if ((interval != null) && interval.trim().equals("N/A")) {
  map.put(PASSWORD_INTERVAL, "NOINTERVAL");
}
}

private void addExpiredAttr(Map map) {
String passdate = (String)map.get("PASSDATE");
if ((passdate != null) && passdate.equals("00.000")) {
  map.put(EXPIRED, Boolean.valueOf(true));
} else {
  map.put(EXPIRED, Boolean.valueOf(false));
}
}

     
*/
