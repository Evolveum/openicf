/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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

import static org.identityconnectors.framework.common.objects.AttributeUtil.createSpecialName;
import static org.identityconnectors.racf.RacfConstants.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
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
DeleteOp, SearchOp<String>, UpdateOp, SchemaOp, ScriptOnConnectorOp {
    
    public static final ObjectClass    RACF_CONNECTION     = new ObjectClass("RacfConnection");
    public static final String         ACCOUNTS_NAME       = createSpecialName("ACCOUNTS");
    public static final AttributeInfo  ACCOUNTS            = AttributeInfoBuilder.build(ACCOUNTS_NAME,
            String.class, EnumSet.of(Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT));


    private RacfConnection              _connection;
    private RacfConfiguration           _configuration;
    private CommandLineUtil             _clUtil;
    private LdapUtil                    _ldapUtil;
    
    public RacfConnector() {
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
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
        splitUpAttributes(attrs, ldapAttrs, commandLineAttrs);
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
    
    private void splitUpAttributes(Set<Attribute> attrs, Set<Attribute> ldapAttrs, Set<Attribute> commandLineAttrs) {
        for (Attribute attribute : attrs) {
            if (AttributeUtil.isSpecial(attribute)) {
                commandLineAttrs.add(attribute);
                ldapAttrs.add(attribute);
            } else if (attribute.getName().contains(".")) {
                commandLineAttrs.add(attribute);
            } else {
                ldapAttrs.add(attribute);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        if (isLdapConnectionAvailable()) {
            _ldapUtil.deleteViaLdap(uid);
        } else {
            _clUtil.deleteViaCommandLine(uid);
        }
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        if (isLdapConnectionAvailable()) {
            if (oclass==ObjectClass.ACCOUNT)
                return new RacfUserFilterTranslator();
            if (oclass==ObjectClass.GROUP)
                return new RacfGroupFilterTranslator();
            if (oclass==RACF_CONNECTION)
                return new RacfConnectFilterTranslator();
            else
                return null;
        } else {
            if (oclass==ObjectClass.ACCOUNT)
                return new RacfCommandLineFilterTranslator();
            if (oclass==ObjectClass.GROUP)
                return new RacfCommandLineFilterTranslator();
            else
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        List<String> names = null;

        if (objectClass.equals(ObjectClass.ACCOUNT))
            names = getUsers(query);
        else if (objectClass.equals(ObjectClass.GROUP))
            names = getGroups(query);
        
        try {
            Set<String> attributesToGet = null;
            if (options!=null && options.getAttributesToGet()!=null) {
                attributesToGet = CollectionUtil.newReadOnlySet(options.getAttributesToGet());
            }
            SearchControls subTreeControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 4095, 0, null, true, true);
            for (String name : names) {
                SearchResult searchResult = null;
                if (isLdapConnectionAvailable()) {
                    NamingEnumeration<SearchResult> results = _connection.getDirContext().search(name, "(objectclass=*)", subTreeControls);
                    searchResult = results.next();
                }
                //**
                ConnectorObject object = buildObject(objectClass, searchResult, _clUtil.getAttributesFromCommandLine(name, isLdapConnectionAvailable(), attributesToGet), attributesToGet);
                handler.handle(object);
            }
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    private ConnectorObject buildObject(ObjectClass objectClass, SearchResult user, Map<String, Object> attributesFromCommandLine, Set<String> attributesToGet) throws NamingException {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        if (user!=null) {
            builder.setUid(user.getNameInNamespace());
            builder.setName(user.getNameInNamespace());
            Attributes attributes = user.getAttributes();
            NamingEnumeration<? extends javax.naming.directory.Attribute> attributeEnum = attributes.getAll();
            while (attributeEnum.hasMore()) {
                javax.naming.directory.Attribute attribute = attributeEnum.next();
                Object value = attribute.get();
                if (value instanceof Collection)
                    builder.addAttribute(attribute.getID(), (Collection<? extends Object>)value);
                else
                    builder.addAttribute(attribute.getID(), value);
            }
            builder.addAttribute(PredefinedAttributes.GROUPS_NAME, getGroupsForUser(user.getNameInNamespace()));
        }
        if (attributesFromCommandLine!=null) {
            if (user==null) {
                String name = (String)attributesFromCommandLine.get("RACF.USERID");
                Uid uid = createUidFromName(objectClass, name);
                builder.setUid(uid);
                builder.setName(name);
            }
            for (Map.Entry<String, Object> entry : attributesFromCommandLine.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                
                // Because naming has to follow <segment>.<attribute> for
                // command-line attributes, we need to look for predefined
                // attributes, and rename them.
                //
                if (name.equals("RACF.GROUPS")) {
                    List<String> newValue = new LinkedList<String>();
                    for (Object singleValue : (List<Object>)value) {
                        // Convert from plain RACF group name, to UID form expected
                        // by the code.
                        //
                        newValue.add(createUidFromName(ObjectClass.GROUP, (String)singleValue).getUidValue());
                    }
                    value = newValue;
                    name = PredefinedAttributes.GROUPS_NAME;
                }
                if (value instanceof Collection)
                    builder.addAttribute(name, (Collection<? extends Object>)value);
                else
                    builder.addAttribute(name, value);
            }
        }
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

    void setGroupMembershipsForUser(String name, Attribute groups) {
        List<Object> newGroups = groups.getValue(); 
        List<String> currentGroups = getGroupsForUser(name);
        for (String currentGroup : currentGroups) {
            if (!newGroups.contains(currentGroup)) {
                // Group is being eliminated
                //
                String connectionName = "racfuserid="+name+"+racfgroupid="+currentGroup+
                                        ",profileType=connect,"+_configuration.getSuffix();
                delete(RACF_CONNECTION, new Uid(connectionName), null);
            }
        }
        
        for (Object newGroup : newGroups) {
            if (!currentGroups.contains(newGroup)) {
                // Group is being added
                //
                String connectionName = "racfuserid="+name+"+racfgroupid="+newGroup+
                                        ",profileType=connect,"+_configuration.getSuffix();
                Set<Attribute> attributes = new HashSet<Attribute>();
                attributes.add(AttributeBuilder.build(Name.NAME, connectionName));
                create(RACF_CONNECTION, attributes, new OperationOptions(new HashMap<String, Object>()));
            }
        }
    }

    void setGroupMembershipsForGroups(String name, Attribute members) {
        List<Object> newMembers = members.getValue(); 
        List<String> currentMembers = getMembersOfGroup(name);
        for (String currentMember : currentMembers) {
            if (!newMembers.contains(currentMember)) {
                // Member is being eliminated
                //
                String connectionName = "racfuserid="+currentMember+"+racfgroupid="+name+
                                        ",profileType=connect,"+_configuration.getSuffix();
                delete(RACF_CONNECTION, new Uid(connectionName), null);
            }
        }
        
        for (Object newMember : newMembers) {
            if (!currentMembers.contains(newMember)) {
                // Member is being added
                //
                String connectionName = "racfuserid="+newMember+"+racfgroupid="+name+
                                        ",profileType=connect,"+_configuration.getSuffix();
                Set<Attribute> attributes = new HashSet<Attribute>();
                attributes.add(AttributeBuilder.build(Name.NAME, connectionName));
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
        if (objectClass.equals(ObjectClass.ACCOUNT))
            return new Uid("racfid="+name.toUpperCase()+",profileType=user,"+_configuration.getSuffix());
        else if (objectClass.equals(ObjectClass.GROUP))
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
        splitUpAttributes(attrs, ldapAttrs, commandLineAttrs);
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

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

        // RACF Users
        //
        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();

        // Required Attributes
        //
        attributes.add(Name.INFO);
        attributes.add(AttributeInfoBuilder.build(ATTR_DEFAULT_GROUP,           String.class));

        // Optional Attributes (have RACF default values)
        //
        attributes.add(AttributeInfoBuilder.build(ATTR_DATA,                    String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_MODEL,                   String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OWNER,                   String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_PASSWORD,                String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_PROGRAMMER_NAME,         String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_DEFAULT_GROUP,           String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_SECURITY_LEVEL,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_SECURITY_CAT_LIST,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_REVOKE_DATE,             String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_RESUME_DATE,             String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_LOGON_DAYS,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_LOGON_TIME,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CLASS_NAME,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CONNECT_GROUP,           String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_SECURITY_LABEL,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_DPF_DATA_APP,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_DPF_DATA_CLASS,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_DPF_MGMT_CLASS,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_DPF_STORAGE_CLASS,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_ACCOUNT_NUMBER,      String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_DEFAULT_CMD,         String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_DESTINATION,         String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_MESSAGE_CLASS,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_DEFAULT_LOGIN,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_LOGIN_SIZE,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_MAX_REGION_SIZE,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_DEFAULT_SYSOUT,      String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_USERDATA,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_DEFAULT_UNIT,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_TSO_SECURITY_LABEL,      String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_LANG_PRIMARY,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_LANG_SECONDARY,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CICS_OPER_ID,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CICS_OPER_CLASS,         String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CICS_OPER_PRIORITY,      String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CICS_OPER_RESIGNON,      String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_CICS_TERM_TIMEOUT,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_STORAGE,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_AUTH,                 String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_MFORM,                String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_LEVEL,                String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_MONITOR,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_ROUTCODE,             String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_LOG_CMD_RESPONSE,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_MGID,                 String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_DOM,                  String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_KEY,                  String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_CMDSYS,               String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_UD,                   String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_MSCOPE_SYSTEMS,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_ALTGROUP,             String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OP_AUTO,                 String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_USER_NAME,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_BUILDING,             String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_DEPARTMENT,           String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_ROOM,                 String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_ADDRESS_LINE1,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_ADDRESS_LINE2,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_ADDRESS_LINE3,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_ADDRESS_LINE4,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_WA_ACCOUNT_NUMBER,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_UID,                String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_HOME,               String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_INIT_PROGRAM,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_MAX_CPUTIME,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_MAX_ADDR_SPACE,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_MAX_FILES,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_MAX_THREADS,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OMVS_MAX_MEMORY_MAP,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_NINITIALCMD,          String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DEFAULT_CONSOLE,      String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_CTL,                  String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_MESSAGE_RECEIVER,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_OPERATOR_CLASS,       String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DOMAINS,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_NGMFADM,              String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DCE_UUID,             String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DCE_PRINCIPAL,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DCE_HOME_CELL,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DCE_HOME_CELL_UUID,   String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NV_DCE_AUTOLOGIN,        String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OVM_UID,                 String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OVM_HOME,                String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OVM_INITIAL_PROGRAM,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_OVM_FILESYSTEM_ROOT,     String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_LN_SHORT_NAME,           String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_NDS_USER_NAME,           String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_KERB_NAME,               String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_KERB_MAX_TICKET_LIFE,    String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_KERB_ENCRYPT,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_PROXY_BINDDN,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_PROXY_BINDPW,            String.class));
        attributes.add(AttributeInfoBuilder.build(ATTR_PROXY_HOST,              String.class));

        // Multi-valued attributes
        //
        attributes.add(buildMultivaluedAttribute(ATTR_ATTRIBUTES,               String.class, false));

        // Operational Attributes
        //
        attributes.add(OperationalAttributeInfos.PASSWORD);
        attributes.add(PredefinedAttributeInfos.GROUPS);

        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        
        //----------------------------------------------------------------------
        
        // RACF Groups
        //
        Set<AttributeInfo> groupAttributes = new HashSet<AttributeInfo>();
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_DATA,               String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_MODEL,              String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_OWNER,              String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_SUP_GROUP,          String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_TERM_UACC,          String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_UNIVERSAL,          String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_DPF_DATA_APP,       String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_DPF_DATA_CLASS,     String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_DPF_MGMT_CLASS,     String.class));
        groupAttributes.add(AttributeInfoBuilder.build(ATTR_DPF_STORAGE_CLASS,  String.class));

        // Read-only Multi-valued Attributes
        //
        groupAttributes.add(buildMVROAttribute(ATTR_SUB_GROUP,                  String.class, false));
        groupAttributes.add(buildMVROAttribute(ATTR_GROUP_USERIDS,              String.class, false));

        attributes.add(ACCOUNTS);

        schemaBuilder.defineObjectClass(ObjectClass.GROUP_NAME, groupAttributes);

        return schemaBuilder.build();
    }

    private AttributeInfo buildMVROAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
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

    private AttributeInfo buildReadonlyAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setCreateable(false);
        builder.setUpdateable(false);
        return builder.build();
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
    }
}
