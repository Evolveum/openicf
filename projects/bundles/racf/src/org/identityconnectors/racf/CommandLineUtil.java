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

import java.io.StringReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.rw3270.RW3270Connection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import expect4j.Closure;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

class CommandLineUtil {
    private static final String         OUTPUT_COMPLETE_PATTERN     = "\\sREADY\\s{74}";
    private static final String         OUTPUT_COMPLETE             = " READY";
    private static final String         OUTPUT_CONTINUING_PATTERN   = "\\s[*]{3}\\s{76}";
    private static final String         OUTPUT_CONTINUING           = " ***";
    private static final String         RACF                        = "RACF";
    private static final String         CATALOG                     = "CATALOG";
    private static final String         DELETE_SEGMENT              = "DELETE SEGMENT";
    private static final String         NO_ENTRIES                  = "NO ENTRIES MEET SEARCH CRITERIA";
    private static final String         NAME_NOT_FOUND              = "NAME NOT FOUND";
    private static final String         UNABLE_TO_LOCATE_USER       = "UNABLE TO LOCATE USER";
    private static final int            COMMAND_TIMEOUT             = 60000;
    
    private Map<String, MapTransform>   _segmentParsers;
    
    private RacfConnector               _connector;
    private static final List<String>   POSSIBLE_ATTRIBUTES         = Arrays.asList(
                    "ADSP", "AUDITOR", "SPECIAL", "GRPACC", "OIDCARD", "OPERATIONS");
    
    private boolean _debug = false;

    public CommandLineUtil(RacfConnector connector) {
        try {
            _connector = connector;
            
            // Create a map of segment names to parsers
            //
            String[] segmentNames = ((RacfConfiguration)_connector.getConfiguration()).getSegmentNames();
            String[] segmentParsers = ((RacfConfiguration)_connector.getConfiguration()).getSegmentParsers();
            _segmentParsers = new HashMap<String, MapTransform>();
            if (segmentNames!=null)
                for (int i=0; i<segmentNames.length; i++) {
                    String name = segmentNames[i];
                    MapTransform transform = asMapTransform(segmentParsers[i]);
                    _segmentParsers.put(name, transform);
                }
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    private RW3270Connection getRW3270Connection() {
        return _connector.getConnection().getRacfConnection();
    }
    
    private static MapTransform asMapTransform(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader(xml)));
        NodeList elements = document.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
            if (elements.item(i) instanceof Element) {
                return new MapTransform((Element) elements.item(i));
            }
        return null;
    }

    private Uid createOrUpdateViaCommandLine(ObjectClass objectClass, String name, CharArrayBuffer command) {
        checkCommand(command);
        return new Uid(name);
    }

    private void checkCommand(String command) {
        CharArrayBuffer buffer = new CharArrayBuffer();
        buffer.append(command);
        try {
            checkCommand(buffer);
        } finally {
            buffer.clear();
        }
    }

    private void checkCommand(CharArrayBuffer command) {
        String output = getCommandOutput(command);
        if (output.trim().length()>0) {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ERROR_IN_COMMAND, new String(command.getArray()), output));
        }
    }
    
    public String getCommandOutput(String command) {
        CharArrayBuffer buffer = new CharArrayBuffer();
        buffer.append(command);
        try {
            return getCommandOutput(buffer);
        } finally {
            buffer.clear();
        }
    }
    
    public String getCommandOutput(CharArrayBuffer buffer) {
        char[] command = buffer.getArray();
        try {
            
            RW3270Connection connection = _connector.getConnection().getRacfConnection();
            connection.clearAndUnlock();
            connection.resetStandardOutput();
            System.out.println("execute:"+new String(command));
            connection.send(command);
            connection.send("[enter]");
            waitFor(COMMAND_TIMEOUT);
            String output = _buffer.toString();
            // Strip command from start, if present
            //
            int index = indexOf(output,command);
            if (index>-1) {
                // Round up to line length
                //
                index += command.length;
                index += connection.getWidth();
                index -= index%connection.getWidth();
                output = output.substring(index);
            }
            
            // Remove OUTPUT_COMPLETE from end of output
            //
            output = output.substring(0, output.lastIndexOf(OUTPUT_COMPLETE));
            
            output = output.replaceAll("(.{"+connection.getWidth()+"})", "$1\n");
            //System.out.println("output:'"+output+"'");
            return output;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        } finally {
            Arrays.fill(command, 0, command.length, ' ');
        }
    }
    
    /**
     * See if a substring, defined by a character array, is contained in a string
     * @param string
     * @param substring
     * @return
     */
    private int indexOf(String string, char[] substring) {
        for (int i=string.length()-substring.length-1; i>-1; i--)
            if (isAt(string, i, substring))
                return i;
        return -1;
    }
    
    private boolean isAt(String string, int start, char[] substring) {
        for (int i=0; i<substring.length; i++) {
            if (string.charAt(start+i)!=substring[i])
                return false;
        }
        return true;
    }

    
    /**
     * Given a set of Attributes, produce a partial TSO command line
     * to apply the values, including segment names.
     * 
     * @param attrs
     * @return
     */
    private char[] mapAttributesToString(Map<String, Attribute> attributes) {
        Name name = (Name)attributes.remove(Name.NAME);
        Uid uid = (Uid)attributes.remove(Uid.NAME);
        Attribute expired = attributes.remove(ATTR_CL_EXPIRED);
        Attribute enabled = attributes.remove(ATTR_CL_ENABLED);
        Attribute enableDate = attributes.remove(ATTR_CL_RESUME_DATE);
        Attribute disableDate = attributes.remove(ATTR_CL_REVOKE_DATE);
        Attribute attributesAttribute = attributes.remove(ATTR_CL_ATTRIBUTES);

        _connector.throwErrorIfNull(attributes, "NETVIEW*CTL");
        _connector.throwErrorIfNull(attributes, "NETVIEW*MSGRECVR");
        _connector.throwErrorIfNull(attributes, "NETVIEW*NGMFADMN");

        _connector.throwErrorIfNull(attributes, "CICS*OPPRTY");
        _connector.throwErrorIfNull(attributes, "CICS*TIMEOUT");
        _connector.throwErrorIfNull(attributes, "CICS*XRFSOFF");
        
        _connector.throwErrorIfNull(attributes, "TSO*SIZE");
        _connector.throwErrorIfNull(attributes, "TSO*MAXSIZE");
        _connector.throwErrorIfNull(attributes, "TSO*USERDATA");
        
        _connector.throwErrorIfNullOrEmpty(name);
        _connector.throwErrorIfNullOrEmpty(uid);
        _connector.throwErrorIfNullOrEmpty(expired);
        _connector.throwErrorIfNullOrEmpty(enabled);
        _connector.throwErrorIfNullOrEmpty(enableDate);
        _connector.throwErrorIfNullOrEmpty(disableDate);
        _connector.throwErrorIfNullOrEmpty(attributesAttribute);
        
        // Build up a map containing the segment attribute values
        //
        Map<String, Map<String, char[]>> attributeValues = new HashMap<String, Map<String,char[]>>();
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            String[] attributeName = entry.getKey().split(RacfConnector.SEPARATOR_REGEX);
            
            if (!attributeValues.containsKey(attributeName[0])) {
                attributeValues.put(attributeName[0], new HashMap<String, char[]>());
            }
            Map<String, char[]> map = attributeValues.get(attributeName[0]);
            map.put(attributeName[1], getAsStringValue(attributeName[0], attributeName[1], entry.getValue()));
        }
        
        // Build the attributes portion of the command
        //
        CharArrayBuffer commandAttributes = new CharArrayBuffer();
        for (Map.Entry<String, Map<String, char[]>> segment : attributeValues.entrySet()) {
            if (segment.getValue().containsKey(DELETE_SEGMENT)) {
                commandAttributes.append(" NO"+segment.getKey());
            } else {
                // We ignore CATALOG, since it isn't a real segment
                //
                if (!CATALOG.equalsIgnoreCase(segment.getKey())) {
                    if (!RACF.equalsIgnoreCase(segment.getKey()))
                        commandAttributes.append(" "+segment.getKey()+"(");
                    for (Map.Entry<String, char[]> entry : segment.getValue().entrySet()) {
                        char[] value = entry.getValue();
                        if (value==null) {
                            commandAttributes.append(" NO"+entry.getKey());
                        } else {
                            commandAttributes.append(" "+entry.getKey()+"(");
                            commandAttributes.append(value);
                            commandAttributes.append(")");
                        }
                    }
                    if (!RACF.equalsIgnoreCase(segment.getKey()))
                        commandAttributes.append(")");
                }
            }
        }
        
        // The various ATTRIBUTES are specified individually on the command line,
        // not as part of a larger value
        // TODO: docs list "UAUDIT", but our RACF doesn't support this 
        List<String> possibleAttributes = new LinkedList<String>(POSSIBLE_ATTRIBUTES);
        if (attributesAttribute!=null) {
            for (Object attributeValue : attributesAttribute.getValue()) {
                commandAttributes.append(" "+attributeValue);
                possibleAttributes.remove(attributeValue);
            }
            for (String attributeValue : possibleAttributes) {
                commandAttributes.append(" NO"+attributeValue);
            }
        }
        if (expired!=null) {
            if (AttributeUtil.getBooleanValue(expired))
                commandAttributes.append(" EXPIRED");
            else
                commandAttributes.append(" NOEXPIRED");
        }
        if (enabled!=null) {
            if (AttributeUtil.getBooleanValue(enabled))
                commandAttributes.append(" RESUME");
            else
                commandAttributes.append(" REVOKE");
        }
        if (enableDate!=null) {
            if (enableDate.getValue()==null)
                commandAttributes.append(" NORESUME");
            else
                commandAttributes.append(" RESUME("+AttributeUtil.getStringValue(enableDate)+")");
        }
        if (disableDate!=null) {
            if (disableDate.getValue()==null)
                commandAttributes.append(" NOREVOKE");
            else
                commandAttributes.append(" REVOKE("+AttributeUtil.getStringValue(disableDate)+")");
        }
        char[] result = commandAttributes.getArray();
        commandAttributes.clear();
        return result;
    }
    
    private char[] getAsStringValue(String segmentName, String attributeName, Attribute attribute) {
        if (_connector.isNullOrEmpty(attribute))
            return null;
        List<Object> values = attribute.getValue();
        if (values.size()==1 && values.get(0) instanceof GuardedString) {
            GuardedString currentGS = (GuardedString)values.get(0);
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            currentGS.access(accessor);
            char[] currentArray = accessor.getArray();
            return currentArray;
        } else {
            StringBuffer buffer = new StringBuffer();
            for (Object value : values) {
                value = computeValue(value.toString(), segmentName, attributeName);
                buffer.append(" "+value);
            }
            return buffer.substring(1).toCharArray();
        }
    }

    private String computeValue(String value, String segmentName, String attributeName) {
        
        // DATA and IC must always be quoted, other values must be quoted
        // if they contain special characters
        //
        boolean quoteNeeded = "DATA".equalsIgnoreCase(attributeName) || "IC".equalsIgnoreCase(attributeName);
        if (!quoteNeeded) {
            for (char character : new char[] {'(', ')', ' ', ',', ';', '\''})
                for (char valueChar : value.toCharArray())
                    quoteNeeded = quoteNeeded || character==valueChar;
        }
        if (quoteNeeded) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("'");
            buffer.append(value.replaceAll("'", "''"));
            buffer.append("'");
            return buffer.toString();
        } else {
            return value;
        }
    }
    
    
    public Uid createViaCommandLine(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        String name = ((Name)attributes.get(Name.NAME)).getNameValue();
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            Attribute groups = attributes.remove(ATTR_CL_GROUPS);
            Attribute owners = attributes.remove(ATTR_CL_GROUP_CONN_OWNERS);
            Attribute expired = attributes.remove(ATTR_CL_EXPIRED);
            Attribute password = attributes.get(ATTR_CL_PASSWORD);

            _connector.throwErrorIfNull(groups);
            _connector.throwErrorIfNullOrEmpty(expired);
            _connector.throwErrorIfNullOrEmpty(password);
            _connector.checkConnectionConsistency(groups, owners);
            if (expired!=null && password==null) 
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.EXPIRED_NO_PASSWORD));
            if (userExists(name))
                throw new AlreadyExistsException();
            validateCatalogAttributes(attributes);

            // Some attributes cannot be specified during create, only modify.
            // Save these off to the side.
            //
            Set<Attribute> changes = new HashSet<Attribute>();
            Attribute enable      = attributes.remove(ATTR_CL_ENABLED);
            Attribute enableDate  = attributes.remove(ATTR_CL_RESUME_DATE);
            Attribute disableDate = attributes.remove(ATTR_CL_REVOKE_DATE);
            if (expired!=null) {
                changes.add(expired);
                changes.add(password);
            }
            if (enable!=null)
                changes.add(enable);
            if (enableDate!=null)
                changes.add(enableDate);
            if (disableDate!=null)
                changes.add(disableDate);
            
            // Create the user
            //
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ADDUSER ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attributes));
            Uid uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            setCatalogAttributes(attributes);
            buffer.clear();
            if (groups!=null) {
                List groupsValue = groups.getValue();
                List ownersValue = owners==null?null:owners.getValue();
                for (int i=0; i<groupsValue.size(); i++) {
                    createConnection(name, (String)groupsValue.get(i), (String)(ownersValue==null?null:ownersValue.get(i)));
                }
            }
            
            // Now, process the deferred attributes
            //
            if (changes.size()>0) {
                changes.add(uid);
                updateViaCommandLine(objectClass, changes, options);
            }
            return uid;
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            Attribute accounts = attributes.remove(ATTR_CL_MEMBERS);
            Attribute owners = attributes.remove(ATTR_CL_GROUP_CONN_OWNERS);

            _connector.throwErrorIfNull(accounts);
            _connector.checkConnectionConsistency(accounts, owners);
            
            if (groupExists(name))
                throw new AlreadyExistsException();
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ADDGROUP ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attributes));
            Uid uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            buffer.clear();
            if (accounts!=null) {
                List accountsValue = accounts.getValue();
                List ownersValue = owners==null?null:owners.getValue();
                for (int i=0; i<accountsValue.size(); i++) {
                    createConnection((String)accountsValue.get(i), name, (String)(ownersValue==null?null:ownersValue.get(i)));
                }
            }
            return uid;
        } else if (objectClass.is(RacfConnector.RACF_CONNECTION_NAME)) {
            String[] info = _connector.extractRacfIdAndGroupIdFromLdapId(name);
            String user = info[0];
            String group = info[1];
            Attribute owner = AttributeUtil.find(ATTR_LDAP_OWNER, attrs);
            createConnection(user, group, (String)(owner==null?null:AttributeUtil.getStringValue(owner)));
            return new Uid(name);
        } else {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS, objectClass));
        }
    }

    private void createConnection(String user, String group, String owner) {
        String command = "CONNECT "+user+" GROUP("+group+")";
        if (owner!=null)
            command += " OWNER("+owner+")";
        checkCommand(command);
    }
    
    public void deleteViaCommandLine(ObjectClass objectClass, Uid uid) {
        String uidString = uid.getUidValue();
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            String command = "DELUSER "+uidString;
            try {
                checkCommand(command);
            } catch (ConnectorException e) {
                if (e.toString().contains("INVALID USERID"))
                    throw new UnknownUidException();
                if (!userExists(uidString))
                    throw new UnknownUidException();
                throw e;
            }
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            String command = "DELGROUP "+uidString;
            
            // To make this more efficient, we just issue the command.
            // If it fails, we check to see if
            //  . group does not exist
            //  . group has members
            // Each of which will generate a nice error. Otherwise, throw a
            // generic error
            //
            try {
                checkCommand(command);
            } catch (ConnectorException e) {
                if (e.toString().contains("INVALID GROUP"))
                    throw new UnknownUidException();
                int memberCount = memberCount(uidString);
                if (memberCount>0)
                    throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.GROUP_NOT_EMPTY));
                throw e;
            }
        } else if (objectClass.is(RacfConnector.RACF_CONNECTION_NAME)) {
            String[] info = _connector.extractRacfIdAndGroupIdFromLdapId(uidString);
            String user = info[0];
            String group = info[1];
            String command = "REMOVE "+user+" GROUP("+group+")";
            checkCommand(command);
        } else {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_UID_TYPE, uidString));
        }
    }

    private boolean groupExists(String name) {
        return objectExists(name, "GROUP");
    }
    
    private boolean userExists(String name) {
        return objectExists(name, "USER");
    }
    
    private boolean objectExists(String name, String type) {
        validateName(name, ((RacfConfiguration)_connector.getConfiguration()));
        String command = "SEARCH CLASS("+type+") FILTER("+name+")";
        String objects = getCommandOutput(command);
        
        return !(objects.contains(NO_ENTRIES));
    }
    
    private int memberCount(String name) {
        String output = getCommandOutput("LISTGRP "+name);
        boolean notFound = (output.toUpperCase().contains(NAME_NOT_FOUND));
        if (notFound)
            throw new UnknownUidException();
        try {
            MapTransform transform = _segmentParsers.get("GROUP.RACF");
            if (transform==null)
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_SEGMENT, ATTR_CL_MEMBERS));
            Map<String, Object> attributes = (Map<String, Object>)transform.transform(output);
            List<Object> members = (List<Object>)attributes.get(ATTR_CL_MEMBERS);
            if (members==null)
                return 0;
            return members.size();
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    static private Pattern _namePattern = Pattern.compile("[a-zA-Z0-9*%@]+");
    static void validateName(Attribute attribute, RacfConfiguration config) {
        if (!attribute.is(Name.NAME))
            return;
        
        String name = ((Name)attribute).getNameValue();
        validateName(name, config);
    }
    
    static void validateName(String name, RacfConfiguration config) {
        if (name.length()>8)
            throw new ConnectorException(config.getMessage(RacfMessages.BAD_NAME_FILTER, name));
        
        if (!_namePattern.matcher(name).matches())
            throw new ConnectorException(config.getMessage(RacfMessages.BAD_NAME_FILTER, name));
    }
    
    public List<String> getMembersOfGroupViaCommandLine(String group) {
        validateName(group, ((RacfConfiguration)_connector.getConfiguration()));
        String command = "LISTGRP "+group;
        String output = getCommandOutput(command);
        try {
            MapTransform transform = _segmentParsers.get("GROUP.RACF");
            if (transform==null)
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_SEGMENT, ATTR_CL_MEMBERS));
            Map<String, Object> attributes = (Map<String, Object>)transform.transform(output);
            List<Object> members = (List<Object>)attributes.get(ATTR_CL_MEMBERS);
            List<String> membersAsString = new LinkedList<String>();
            if (members!=null) {
                for (Object member : members)
                    membersAsString.add((String)member);
            }
            return membersAsString;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    public List<String> getGroupsForUserViaCommandLine(String user) {
        validateName(user, ((RacfConfiguration)_connector.getConfiguration()));
        String command = "LISTUSER "+user;
        String output = getCommandOutput(command);
        MapTransform transform = _segmentParsers.get("ACCOUNT.RACF");
        if (transform==null)
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_SEGMENT, ATTR_CL_GROUPS));
        try {
            Map<String, Object> map = (Map<String, Object>)transform.transform(output);
            List<String> groups = (List<String>)map.get(ATTR_CL_GROUPS);
            String defaultGroup = (String)map.get(ATTR_CL_DFLTGRP);
            List<String> groupsAsString = new LinkedList<String>();
            groupsAsString.add(defaultGroup);
            for (Object group : groups)
                if (!group.equals(defaultGroup))
                    groupsAsString.add((String)group);
            return groupsAsString;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    Pattern _errorMessage = Pattern.compile("[^\\s]{9}");
    
    public List<String> getUsersViaCommandLine(String query) {
        List<String> result = new LinkedList<String>(getObjectsViaCommandLine("USER", query));
        // We eliminate certain users
        //
        for (String user : new String[] {"irrcerta", "irrmulti", "irrsitec"}) 
            result.remove(user);
        return result;
    }
    
    public List<String> getGroupsViaCommandLine(String query) {
        return getObjectsViaCommandLine("GROUP", query);
    }
    
    private List<String> getObjectsViaCommandLine(String className, String query) {
        String command = null;
        if (query!=null && query.length()>0)
            command = "SEARCH CLASS("+className+") FILTER("+query+")";
        else
            command = "SEARCH CLASS("+className+")";
        String objects = getCommandOutput(command);
        
        if (objects.contains(NO_ENTRIES)) {
            return new LinkedList<String>();
        }
        
        // Error messages all start with a 9 character error code,
        // and groups/users are at most 8 characters long. This allow us to
        // determine if there are any error messages in the text
        //
        Matcher matcher = _errorMessage.matcher(objects); 
        if (matcher.find()) {
            String error = objects.substring(matcher.start()).trim();
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ERROR_IN_GET_GROUPS, error));
        } else {
            String[] objectsArray = objects.trim().split("\\s+");
            for (int i=0; i<objectsArray.length; i++)
                objectsArray[i] = objectsArray[i];
            return Arrays.asList(objectsArray);
        }
    }
    
    public Uid updateViaCommandLine(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        Uid uid = (Uid)attributes.get(Uid.NAME);
        String name = uid.getUidValue();
        
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            Attribute groups = attributes.remove(ATTR_CL_GROUPS);
            Attribute groupOwners = attributes.remove(ATTR_CL_GROUP_CONN_OWNERS);
            Attribute expired = attributes.get(ATTR_CL_EXPIRED);
            Attribute password = attributes.get(ATTR_CL_PASSWORD);

            _connector.throwErrorIfNull(groups);
            _connector.throwErrorIfNull(groupOwners);

            // RACF makes it difficult to specify ENABLE_DATE/DISABLE_DATE
            // except in its own command
            //
            Attribute enable      = attributes.get(ATTR_CL_ENABLED);
            Attribute enableDate  = attributes.remove(ATTR_CL_RESUME_DATE);
            Attribute disableDate = attributes.remove(ATTR_CL_REVOKE_DATE);
            
            if (enableDate!=null && disableDate!=null)
                attributes.remove(ATTR_CL_ENABLED);

            
            if (expired!=null && password==null) 
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.EXPIRED_NO_PASSWORD));
            
            if (!userExists(name))
                throw new UnknownUidException();

            validateCatalogAttributes(attributes);
            setCatalogAttributes(attributes);
            
            CharArrayBuffer buffer = new CharArrayBuffer();
            
            buffer.append("ALTUSER ");
            buffer.append(name);
            char[] attributeString = mapAttributesToString(attributes);
            buffer.append(attributeString);
            try {
                if (groups!=null)
                    _connector.setGroupMembershipsForUser(name, groups, groupOwners);
                if (attributeString.length>0)
                    uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            } finally {
                buffer.clear();
            }
            
            if (enableDate!=null || disableDate!=null) {
                // If we are given a ENABLE_DATE/DISABLE_DATE, but not an ENABLE status
                // we have to fetch ENABLE status, since we will alter it as part of
                // setting ENABLE_DATE/DISABLE_DATE
                //
                if (enable==null) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] { OperationalAttributes.ENABLE_NAME });
                    OperationOptions operationOptions = new OperationOptions(map);
                    LocalHandler handler = new LocalHandler();
                    _connector.executeQuery(ObjectClass.ACCOUNT, name, handler, operationOptions);
                    ConnectorObject object = handler.iterator().next();
                    enable = object.getAttributeByName(OperationalAttributes.ENABLE_NAME);
                }
                Boolean currentEnableState = enable==null?null:AttributeUtil.getBooleanValue(enable);
                Boolean lastEnableState = currentEnableState;
                
                if (enableDate!=null) {
                    lastEnableState = Boolean.FALSE;
                    buffer = new CharArrayBuffer();
                    
                    buffer.append("ALTUSER ");
                    buffer.append(name);
                    buffer.append(" REVOKE");
                    Map<String, Attribute> enableDateAttributes = new HashMap<String, Attribute>();
                    enableDateAttributes.put(enableDate.getName(), enableDate);
                    attributeString = mapAttributesToString(enableDateAttributes);
                    buffer.append(attributeString);
                    createOrUpdateViaCommandLine(objectClass, name, buffer);
                }
                
                if (disableDate!=null) {
                    lastEnableState = Boolean.TRUE;
                    buffer = new CharArrayBuffer();
                    
                    buffer.append("ALTUSER ");
                    buffer.append(name);
                    buffer.append(" RESUME");
                    Map<String, Attribute> disableDateAttributes = new HashMap<String, Attribute>();
                    disableDateAttributes.put(disableDate.getName(), disableDate);
                    attributeString = mapAttributesToString(disableDateAttributes);
                    buffer.append(attributeString);
                    createOrUpdateViaCommandLine(objectClass, name, buffer);
                }
                
                if (!lastEnableState.equals(currentEnableState)) {
                    buffer = new CharArrayBuffer();
                    
                    buffer.append("ALTUSER ");
                    buffer.append(name);
                    if (currentEnableState)
                        buffer.append(" RESUME");
                    else
                        buffer.append(" REVOKE");
                    createOrUpdateViaCommandLine(objectClass, name, buffer);
                }
            }
            return uid;
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            Attribute members = attributes.remove(ATTR_CL_MEMBERS);
            Attribute groupOwners = attributes.remove(ATTR_CL_GROUP_CONN_OWNERS);
            
            _connector.throwErrorIfNull(members);
            _connector.throwErrorIfNull(groupOwners);
            
            if (!groupExists(name))
                throw new UnknownUidException();
            
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ALTGROUP ");
            buffer.append(name);
            char[] attributeString = mapAttributesToString(attributes);
            buffer.append(attributeString);
            try {
                if (members!=null)
                    _connector.setGroupMembershipsForGroups(name, members, groupOwners);
                if (attributeString.length>0)
                    return createOrUpdateViaCommandLine(objectClass, name, buffer);
                else
                    return uid;
            } finally {
                buffer.clear();
            }
        } else {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS, objectClass));
        }
    }

    private void setCatalogAttributes(Map<String, Attribute> attributes) {
        if (hasSingleStringValue(attributes.get(ATTR_CL_CATALOG_ALIAS))) {
            String alias     = AttributeUtil.getStringValue(attributes.get(ATTR_CL_CATALOG_ALIAS));
            String masterCat = AttributeUtil.getStringValue(attributes.get(ATTR_CL_MASTER_CATALOG));
            String userCat   = AttributeUtil.getStringValue(attributes.get(ATTR_CL_USER_CATALOG));
            
            String command = "DEFINE ALIAS (NAME('"+alias+"') RELATE('"+userCat+"')) CATALOG('"+masterCat+"')";
            checkCommand(getCommandOutput(command));
        }
    }

    private void getCatalogAttributes(String identifier, Map<String, Object> attributesFromCommandLine) {
        String command = "LISTC ENT('"+identifier+"') ALL";
        String output = getCommandOutput(command);
        MapTransform transform = _segmentParsers.get("ACCOUNT."+CATALOG);
        try {
            attributesFromCommandLine.putAll((Map<String, Object>)transform.transform(output));
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    private boolean hasSingleStringValue(Attribute attribute) {
        if (attribute==null)
            return false;
        List<Object> values = attribute.getValue();
        if (values==null || values.size()!=1)
            return false;
        return (values.get(0) instanceof String);
    }
    
    private void validateCatalogAttributes(Map<String, Attribute> attributes) {
        boolean alias     = hasSingleStringValue(attributes.get(ATTR_CL_CATALOG_ALIAS));
        boolean masterCat = hasSingleStringValue(attributes.get(ATTR_CL_MASTER_CATALOG));
        boolean userCat   = hasSingleStringValue(attributes.get(ATTR_CL_USER_CATALOG));
        
        // All must be either present or missing
        //
        if ((alias||masterCat||userCat)!=(alias&&masterCat&&userCat)) {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.INCONSISTENT_CATALOG_ARGS));
        }
    }

    public Map<String, Object> getAttributesFromCommandLine(ObjectClass objectClass, String racfName, Set<String> attributesToGet) {
        String objectClassPrefix = null;
        String listCommand = null;
        if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            objectClassPrefix = "GROUP.";
            listCommand = "LISTGRP";
        } else if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            objectClassPrefix = "ACCOUNT.";
            listCommand = "LISTUSER";
        } else {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS));
        }
        // Determine the set of segment names, if any
        // We use a TreeSet to force an ordering
        //
        Set<String> segmentsNeeded = new TreeSet<String>();
        
        if (attributesToGet!=null) {
            for (String attributeToGet : attributesToGet) {
                int index = attributeToGet.indexOf(RacfConnector.SEPARATOR);
                if (index!=-1) {
                    String prefix = attributeToGet.substring(0, index);
                    segmentsNeeded.add(prefix);
                    
                    if (!_segmentParsers.containsKey(objectClassPrefix+prefix)) {
                        throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_SEGMENT, attributeToGet));
                    }
                }
            }
        }
        // If we are asking for segment information, ensure that command-line login
        // information was specified
        //
        if (segmentsNeeded.size()>0 && ((RacfConfiguration)_connector.getConfiguration()).getUserName()==null)
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ATTRS_NO_CL));
        
        Map<String, Object> attributesFromCommandLine = new HashMap<String, Object>();
        if (segmentsNeeded.size()>0 || ((RacfConfiguration)_connector.getConfiguration()).getUserName()==null) {
            try {
                boolean racfNeeded = segmentsNeeded.remove(RACF);
                boolean catalogNeeded = segmentsNeeded.remove(CATALOG);
                StringBuffer buffer = new StringBuffer();
                buffer.append(listCommand+" "+racfName);
                if (!racfNeeded)
                    buffer.append(" NORACF");
                for (String segment : segmentsNeeded)
                    buffer.append(" "+segment);

                String output = getCommandOutput(buffer.toString());
                
                // Split out the various segments
                //
                StringBuffer segmentPatternString = new StringBuffer();
                if (racfNeeded)
                    segmentPatternString.append("(.+?)");
                for (String segment : segmentsNeeded) {
                    segmentPatternString.append("(NO )?"+segment.toUpperCase()+" INFORMATION (.+?)");
                }
                Pattern segmentsPattern = Pattern.compile(segmentPatternString.toString()+"$", Pattern.DOTALL);
                Matcher segmentsMatcher = segmentsPattern.matcher(output);
                if (segmentsMatcher.find()) {
                    // Deal with RACF first
                    //
                    int offset = 0;
                    if (racfNeeded) {
                        MapTransform transform = _segmentParsers.get(objectClassPrefix+"RACF");
                        try {
                            attributesFromCommandLine.putAll((Map<String, Object>)transform.transform(segmentsMatcher.group(1)));
                        } catch (Exception e) {
                            if (_debug) System.out.println(_buffer2.toString().replaceAll("(.{80})", "$1\n"));
                            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNPARSEABLE_RESPONSE, "LISTUSER", output));
                        }
                        offset = 1;
                    }
                    // Parse the other segments, and add the attributes to the set of
                    // attributes received
                    //
                    int i=0;
                    for (String segment : segmentsNeeded) {
                        String noValue = segmentsMatcher.group(2*i+offset+1);
                        String segmentValue = segmentsMatcher.group(2*i+offset+2);
                        if (StringUtil.isBlank(noValue)) {
                            MapTransform transform = _segmentParsers.get(objectClassPrefix+segment);
                            attributesFromCommandLine.putAll((Map<String, Object>)transform.transform(segmentValue));
                        }
                        i++;
                    }
                } else if (output.toUpperCase().contains(UNABLE_TO_LOCATE_USER)) {
                    throw new UnknownUidException();
                } else {
                    if (_debug) System.out.println(_buffer2.toString().replaceAll("(.{80})", "$1\n"));
                    throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNPARSEABLE_RESPONSE, "LISTUSER", output));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw ConnectorException.wrap(e);
            }
        }
        
        // Remap ACCOUNT attributes as needed
        //
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (attributesToGet.contains(ATTR_CL_CATALOG_ALIAS) ||
                    attributesToGet.contains(ATTR_CL_USER_CATALOG) ||
                    attributesToGet.contains(ATTR_CL_MASTER_CATALOG)) {
                getCatalogAttributes(racfName, attributesFromCommandLine);
            }
            // __ENABLE__ is indicated by the REVOKED ATTRIBUTE
            // We also remove REVOKED from attribute list, since we display it separately
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_ATTRIBUTES)) {
                List<Object> value = (List<Object>)attributesFromCommandLine.get(ATTR_CL_ATTRIBUTES);
                boolean revoked = value.remove("REVOKED");
                attributesFromCommandLine.put(OperationalAttributes.ENABLE_NAME, !revoked);
            }
            // Last Access date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_LAST_ACCESS)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_LAST_ACCESS);
                Long converted = _connector.convertFromRacfTimestamp(value);
                attributesFromCommandLine.put(PredefinedAttributes.LAST_LOGIN_DATE_NAME, converted);
            }
            // password change date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_PASSDATE)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_PASSDATE);
                Long converted = _connector.convertFromRacfTimestamp(value);
                attributesFromCommandLine.put(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME, converted);
                // password change date is 00.000 if expired
                //
                Boolean expired = "00.000".equals(value);
                attributesFromCommandLine.put(OperationalAttributes.PASSWORD_EXPIRED_NAME, expired);
            }
            // Revoke date must be converted
            //
            long now = new Date().getTime(); 
            if (attributesFromCommandLine.containsKey(ATTR_CL_REVOKE_DATE)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_REVOKE_DATE);
                Long converted = _connector.convertFromResumeRevokeFormat(value);
                if (converted==null || converted<now)
                    attributesFromCommandLine.put(OperationalAttributes.DISABLE_DATE_NAME, null);
                else
                    attributesFromCommandLine.put(OperationalAttributes.DISABLE_DATE_NAME, converted);
            }
            // Resume date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_RESUME_DATE)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_RESUME_DATE);
                Long converted = _connector.convertFromResumeRevokeFormat(value);
                if (converted==null || converted<now)
                    attributesFromCommandLine.put(OperationalAttributes.ENABLE_DATE_NAME, null);
                else
                    attributesFromCommandLine.put(OperationalAttributes.ENABLE_DATE_NAME, converted);
            }
            // Groups must be filled in if null
            //
            if (!attributesFromCommandLine.containsKey(ATTR_CL_GROUPS)) {
                attributesFromCommandLine.put(ATTR_CL_GROUPS, new LinkedList<Object>());
            }
            // Group Owners must be filled in if null
            //
            if (!attributesFromCommandLine.containsKey(ATTR_CL_GROUP_CONN_OWNERS)) {
                attributesFromCommandLine.put(ATTR_CL_GROUP_CONN_OWNERS, new LinkedList<Object>());
            }
        }

        // Remap GROUP attributes as needed
        //
        if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            if (attributesFromCommandLine.containsKey(ATTR_CL_SUPGROUP)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_SUPGROUP);
                if ("NONE".equals(value))
                    attributesFromCommandLine.put(ATTR_CL_SUPGROUP, null);
            }
            // Groups must be filled in if null
            //
            if (!attributesFromCommandLine.containsKey(ATTR_CL_GROUPS)) {
                attributesFromCommandLine.put(ATTR_CL_GROUPS, new LinkedList<Object>());
            }
            // Members must be filled in if null
            //
            if (!attributesFromCommandLine.containsKey(ATTR_CL_MEMBERS)) {
                attributesFromCommandLine.put(ATTR_CL_MEMBERS, new LinkedList<Object>());
            }
            // Group Owners must be filled in if null
            //
            if (!attributesFromCommandLine.containsKey(ATTR_CL_GROUP_CONN_OWNERS)) {
                attributesFromCommandLine.put(ATTR_CL_GROUP_CONN_OWNERS, new LinkedList<Object>());
            }
        }
        // If we didn't fetch RACF segment, fill in name
        //
        if (!attributesFromCommandLine.containsKey(ATTR_CL_USERID))
            attributesFromCommandLine.put(ATTR_CL_USERID, racfName);
        
        return attributesFromCommandLine;
    }

    private StringBuffer _buffer = new StringBuffer();
    private StringBuffer _buffer2 = new StringBuffer();
    private boolean _timedOut = false;

    private void appendScreen(String display) {
        int index = display.lastIndexOf(OUTPUT_COMPLETE);
        if (index>-1) {
            _buffer.append(display.substring(0, Math.min(display.length(), index+getRW3270Connection().getWidth())));
        } else {
            _buffer.append(display);
        }
    }
    
    private void handleScreenOfOutput(ExpectState state) throws InterruptedException {

        // If we are at commandComplete, we're done
        //
        Object commandComplete = state.getVar("commandComplete");
        if (Boolean.TRUE.equals(commandComplete)) {
            return;
        }
        
        // If we found an error earlier, we must just return
        //
        Object errorDetected = state.getVar("errorDetected");
        if (errorDetected!=null)
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ERROR_IN_RACF_COMMAND, errorDetected.toString().trim()));

        // Get the screen contents
        //
        getRW3270Connection().waitForUnlock();
        String display = getRW3270Connection().getDisplay();
        
        // If display ends with "***", we may have
        //      READY
        //      ***
        if (removeTrailingSpaces(display).endsWith(OUTPUT_CONTINUING)) {
            String display2 = display.substring(0, display.lastIndexOf(OUTPUT_CONTINUING));
            if (removeTrailingSpaces(display2).endsWith(OUTPUT_COMPLETE)) {
                // We have 
                //      READY
                //      ***
                // So, make sure we don't double save
                state.addVar("commandComplete", Boolean.TRUE);
            }
            // We have 
            //      ***
            // So, shorten the display, and send [enter], and continue
            //
            _buffer2.append(display2.toString());
            _buffer2.append("-0------------------------------------------------------------------------------");
            appendScreen(display2);
            getRW3270Connection().sendEnter();
            state.exp_continue();
            return;
        }

        // In this case, we should end with READY
        //
        if (removeTrailingSpaces(display).endsWith(OUTPUT_COMPLETE)) {
            _buffer2.append(display.toString());
            _buffer2.append("-1------------------------------------------------------------------------------");
            appendScreen(display);
        } else {
            // Nothing special, so keep going
            state.exp_continue();
        }
    }
    
    private void waitFor(Integer timeout) {
        _buffer.setLength(0);
        _buffer2.setLength(0);
        try {
            _timedOut = false;
            List<Match> matches = new LinkedList<Match>();
            
            // Match the continue expression
            //
            matches.add(new RegExpMatch(OUTPUT_CONTINUING_PATTERN, new Closure() {
                public void run(ExpectState state) throws Exception {
                    handleScreenOfOutput(state);
                    return;
                }
            }));
            
            // Match the command complete expression
            //
            matches.add(new RegExpMatch(OUTPUT_COMPLETE_PATTERN, new Closure() {
                public void run(ExpectState state) throws Exception {
                    handleScreenOfOutput(state);
                    return;
                }
            }));
            
            // Match the error expression, so
            //  send the abort command
            //  continue execution, to see if we can recover
            //
            
            matches.add(new RegExpMatch("IKJ\\d+\\w REENTER THIS OPERAND", new Closure() {
                public void run(ExpectState state) throws Exception {
                    state.addVar("errorDetected", state.getBuffer());
                    getRW3270Connection().waitForUnlock();
                    getRW3270Connection().sendPAKeys(1);
                    state.exp_continue();
                }
            }));
            
            matches.add(new RegExpMatch("IKJ\\d+\\w ENTER (\\w\\s)+-", new Closure() {
                public void run(ExpectState state) throws Exception {
                    state.addVar("errorDetected", state.getBuffer());
                    getRW3270Connection().waitForUnlock();
                    getRW3270Connection().sendPAKeys(1);
                    state.exp_continue();
                }
            }));
            
            if (timeout != null)
                matches.add(new TimeoutMatch(timeout, new Closure() {
                    public void run(ExpectState state) throws Exception {
                        _timedOut = true;
                    }
                }));
            getRW3270Connection().waitFor(matches.toArray(new Match[0]));
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        
        if (_timedOut) {
            throw new ConnectorException(_connector.getConfiguration().getConnectorMessages().format(
                    "IsAlive", "timed out waiting for ''{0}'':''{1}''",
                    OUTPUT_COMPLETE, _buffer.toString()));
        }
    }
    
    private String removeTrailingSpaces(String string) {
        if (StringUtil.isBlank(string))
            return "";
        StringBuffer buffer = new StringBuffer();
        buffer.append(string);
        while (buffer.charAt(buffer.length()-1)==' ' || buffer.charAt(buffer.length()-1)=='|')
            buffer.setLength(buffer.length()-1);
        return buffer.toString();
    }
    private static class CharArrayBuffer {
        private char[]  _array;
        private int     _position;
        
        public CharArrayBuffer() {
            _array = new char[1024];
            _position = 0;
        }
        
        public void append(String string ) {
            append(string.toCharArray());
        }
        
        public void append(char character) {
            append(new char[] { character });
        }
        
        public void append(char[] string) {
            if (_position+string.length> _array.length) {
                char[] oldArray = _array;
                _array = new char[_array.length+1024];
                System.arraycopy(oldArray, 0, _array, 0, _position);
                Arrays.fill(oldArray, 0, oldArray.length, ' ');
            }
            System.arraycopy(string, 0, _array, _position, string.length);
            _position += string.length;
        }
        
        public void clear() {
            Arrays.fill(_array, 0, _array.length, ' ');
        }
        
        public char[] getArray() {
            char[] result = new char[_position];
            System.arraycopy(_array, 0, result, 0, _position);
            return result;
        }
    }

    public static class LocalHandler implements ResultsHandler, Iterable<ConnectorObject> {
        private List<ConnectorObject> objects = new LinkedList<ConnectorObject>();

        public boolean handle(ConnectorObject object) {
            objects.add(object);
            return true;
        }

        public Iterator<ConnectorObject> iterator() {
            return objects.iterator();
        }

        public int size() {
            return objects.size();
        }
    }

}
