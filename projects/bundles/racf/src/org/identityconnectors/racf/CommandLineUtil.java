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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.patternparser.Transform;
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
    private static final String         DEFAULT_GROUP_NAME          = "DFLTGRP";
    private static final String         OUTPUT_CONTINUING_PATTERN   = "\\s\\*\\*\\*\\s{76}";
    private static final String         OUTPUT_CONTINUING           = " ***";
    private static final String         RACF                        = "RACF";
    private static final String         CATALOG                     = "CATALOG";
    private static final String         DELETE_SEGMENT              = "Delete Segment";
    private static final int            COMMAND_TIMEOUT             = 60000;
    
    private Map<String, MapTransform>   _segmentParsers;
    private final Pattern               _connectionPattern  = Pattern.compile("racfuserid=(.*)\\+racfgroupid=(.*),.*");
    private final Pattern               _racfTimestamp = Pattern.compile("(\\d+)\\.(\\d+)(?:/(\\d+):(\\d+):(\\d+))?");
    private final ScriptExecutorFactory _groovyFactory;
    private final SimpleDateFormat      _resumeRevokeFormat = new SimpleDateFormat("MMMM dd, yyyy");
    private final SimpleDateFormat      _dateFormat = new SimpleDateFormat("MM/dd/yy");
    
    private RacfConnector               _connector;

    public CommandLineUtil(RacfConnector connector) {
        try {
            _connector = connector;
            _groovyFactory = ScriptExecutorFactory.newInstance("GROOVY");
            
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
        return _connector.createUidFromName(objectClass, name);
    }

    public String[] extractRacfIdAndGroupIdFromLdapId(String uidString) {
        Matcher matcher = _connectionPattern.matcher(uidString);
        if (matcher.matches())
            return new String[] {matcher.group(1), matcher.group(2)};
        else
            return null;
    }

    public boolean isUserid(String uidString) {
        return uidString.toUpperCase().contains("PROFILETYPE=USER") && !isConnection(uidString);
    }

    public boolean isGroupid(String uidString) {
        return uidString.toUpperCase().contains("PROFILETYPE=GROUP") && !isConnection(uidString);
    }

    public boolean isConnection(String uidString) {
        return uidString.toUpperCase().contains("PROFILETYPE=CONNECT");
    }

    public void checkCommand(String command) {
        CharArrayBuffer buffer = new CharArrayBuffer();
        buffer.append(command);
        try {
            checkCommand(buffer);
        } finally {
            buffer.clear();
        }
    }

    public void checkCommand(CharArrayBuffer command) {
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
            
            // TODO: failure indication for DELGROUP
            //      IKJ56700A ENTER GROUP NAME(S) -
            //      (requires PA1 to recover)
            //      IKJ56718A REENTER THIS OPERAND+ -
            //      (requires PA1 to recover)
            //  failure indication for LISTGRP
            //      ICH51003I NAME NOT FOUND IN RACF DATA SET
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
                index += connection.getWidth();
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
    public char[] mapAttributesToString(Map<String, Attribute> attributes) {
        Name name = (Name)attributes.remove(Name.NAME);
        Uid uid = (Uid)attributes.remove(Uid.NAME);
        Attribute expired = attributes.remove(ATTR_CL_EXPIRED);
        Attribute enabled = attributes.remove(ATTR_CL_ENABLED);
        Attribute enableDate = attributes.remove(ATTR_CL_RESUME_DATE);
        Attribute disableDate = attributes.remove(ATTR_CL_REVOKE_DATE);
        Attribute attributesAttribute = attributes.remove(ATTR_CL_ATTRIBUTES);
        
        // Build up a map containing the segment attribute values
        //
        Map<String, Map<String, char[]>> attributeValues = new HashMap<String, Map<String,char[]>>();
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            String[] attributeName = entry.getKey().split(RacfConnector.SEPARATOR_REGEX);
            
            if (!attributeValues.containsKey(attributeName[0])) {
                attributeValues.put(attributeName[0], new HashMap<String, char[]>());
            }
            Map<String, char[]> map = attributeValues.get(attributeName[0]);
            map.put(attributeName[1], getAsStringValue(entry.getValue()));
        }
        
        // Build the attributes portion of the command
        //
        CharArrayBuffer commandAttributes = new CharArrayBuffer();
        for (Map.Entry<String, Map<String, char[]>> segment : attributeValues.entrySet()) {
            if (segment.getValue().containsKey(DELETE_SEGMENT)) {
                commandAttributes.append(" NO"+segment.getKey());
            } else {
                if (!RACF.equalsIgnoreCase(segment.getKey()))
                    commandAttributes.append(" "+segment.getKey()+"(");
                for (Map.Entry<String, char[]> entry : segment.getValue().entrySet()) {
                    char[] value = entry.getValue();
                    value = computeValue(value, segment.getKey(), entry.getKey());
                    commandAttributes.append(" "+entry.getKey()+"(");
                    commandAttributes.append(value);
                    commandAttributes.append(")");
                }
                if (!RACF.equalsIgnoreCase(segment.getKey()))
                    commandAttributes.append(")");
            }
        }
        
        // The various ATTRIBUTES are specified individually on the command line,
        // not as part of a larger value
        //
        if (attributesAttribute!=null) {
            for (Object attributeValue : attributesAttribute.getValue()) {
                commandAttributes.append(" "+attributeValue);
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
            Date date = new Date(AttributeUtil.getLongValue(enableDate));
            String dateValue = _dateFormat.format(date);
            commandAttributes.append(" RESUME("+dateValue+")");
        }
        if (disableDate!=null) {
            Date date = new Date(AttributeUtil.getLongValue(disableDate));
            String dateValue = _dateFormat.format(date);
            commandAttributes.append(" REVOKE("+dateValue+")");
        }
        char[] result = commandAttributes.getArray();
        commandAttributes.clear();
        return result;
    }
    
    private char[] getAsStringValue(Attribute attribute) {
        Object value = AttributeUtil.getSingleValue(attribute);
        if (value instanceof GuardedString) {
            GuardedString currentGS = (GuardedString)value;
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            currentGS.access(accessor);
            char[] currentArray = accessor.getArray();
            return currentArray;
        } else {
            return value.toString().toCharArray();
        }
    }

    public char[] computeValue(char[] value, String segmentName, String attributeName) {
        // Since DFLTGRP comes in as a stringified UID, it must be converted
        //
        if (DEFAULT_GROUP_NAME.equalsIgnoreCase(attributeName)) {
            value = _connector.extractRacfIdFromLdapId(new String(value)).toCharArray();
        }
        
        // DATA and IC must always be quoted, other values must be quoted
        // if they contain special characters
        //
        boolean quoteNeeded = "DATA".equalsIgnoreCase(segmentName) || "IC".equalsIgnoreCase(segmentName);
        if (!quoteNeeded) {
            for (char character : new char[] {'(', ')', ' ', ',', ';', '\''})
                for (char valueChar : value)
                    quoteNeeded = quoteNeeded || character==valueChar;
        }
        if (quoteNeeded) {
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("'");
            for (char character : value) {
                if (character=='\'')
                    buffer.append("''");
                else
                    buffer.append(character);
            }
            buffer.append("'");
            value = buffer.getArray();
            buffer.clear();
        }
        return value;
    }
    
    public Uid createViaCommandLine(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        String name = ((Name)attributes.get(Name.NAME)).getNameValue();
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            Attribute groups = attributes.remove(ATTR_CL_GROUPS);
            Attribute expired = attributes.remove(ATTR_CL_EXPIRED);
            Attribute password = attributes.get(ATTR_CL_PASSWORD);
            if (expired!=null && password==null) 
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.EXPIRED_NO_PASSWORD));
            
            if (userExists(name))
                throw new AlreadyExistsException();
            validateCatalogAttributes(attributes);
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ADDUSER ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attributes));
            Uid uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            setCatalogAttributes(attributes);
            buffer.clear();
            if (groups!=null) {
                for (Object groupName : groups.getValue()) {
                    String command = "CONNECT "+name+" GROUP("+_connector.extractRacfIdFromLdapId((String)groupName)+")";
                    checkCommand(command);
                }
            }
            if (expired!=null) {
                Map<String, Attribute> updateAttrs = new HashMap<String, Attribute>();
                updateAttrs.put(ATTR_CL_EXPIRED, expired);
                updateAttrs.put(ATTR_CL_PASSWORD, password);
                buffer = new CharArrayBuffer();
                buffer.append("ALTUSER ");
                buffer.append(name);
                buffer.append(mapAttributesToString(updateAttrs));
                createOrUpdateViaCommandLine(objectClass, name, buffer);
            }
            return uid;
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            Attribute accounts = attributes.remove(ATTR_CL_MEMBERS);
            if (groupExists(name))
                throw new AlreadyExistsException();
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ADDGROUP ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attributes));
            Uid uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            buffer.clear();
            if (accounts!=null) {
                for (Object accountName : accounts.getValue()) {
                    String command = "CONNECT "+accountName+" GROUP("+name+")";
                    checkCommand(command);
                }
            }
            return uid;
        } else if (objectClass.is(RacfConnector.RACF_CONNECTION_NAME)) {
            String[] info = extractRacfIdAndGroupIdFromLdapId(name);
            String user = _connector.extractRacfIdFromLdapId(info[0]);
            String group = _connector.extractRacfIdFromLdapId(info[1]);
            String command = "CONNECT "+user+" GROUP("+group+")";
            checkCommand(command);
            return new Uid(name);
        } else {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_OBJECT_CLASS, objectClass));
        }
    }
    
    public void deleteViaCommandLine(Uid uid) {
        String uidString = uid.getUidValue();
        if (isUserid(uidString)) {
            String name = _connector.extractRacfIdFromLdapId(uidString);
            if (!userExists(name))
                throw new UnknownUidException();
            String command = "DELUSER "+name;
            checkCommand(command);
        } else if (isGroupid(uidString)) {
            String name = _connector.extractRacfIdFromLdapId(uidString);
            int memberCount = memberCount(name);
            if (memberCount>0)
                throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.GROUP_NOT_EMPTY));
            String command = "DELGROUP "+name;
            checkCommand(command);
        } else if (isConnection(uidString)) {
            String[] info = extractRacfIdAndGroupIdFromLdapId(uidString);
            String user = _connector.extractRacfIdFromLdapId(info[0]);
            String group = _connector.extractRacfIdFromLdapId(info[1]);
            String command = "REMOVE "+user+" GROUP("+group+")";
            checkCommand(command);
        } else {
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_UID_TYPE, uidString));
        }
    }

    private boolean groupExists(String name) {
        // TSO gets into a funny state if you attempt to delete an nonexisting group
        // so, check and see if the group already exists
        //
        String output = getCommandOutput("LISTGRP "+name);
        boolean notFound = (output.toUpperCase().contains("NAME NOT FOUND"));
        return !notFound;
    }

    private int memberCount(String name) {
        // TSO gets into a funny state if you attempt to delete an nonexisting group
        // so, check and see if the group already exists
        //
        String output = getCommandOutput("LISTGRP "+name);
        boolean notFound = (output.toUpperCase().contains("NAME NOT FOUND"));
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

    private boolean userExists(String name) {
        // TSO gets into a funny state if you attempt to delete an nonexisting user
        // so, check and see if the user already exists
        //
        String output = getCommandOutput("LISTUSER "+name);
        boolean notFound = (output.toUpperCase().contains("UNABLE TO LOCATE USER"));
        return !notFound;
    }
    
    public List<String> getMembersOfGroupViaCommandLine(String group) {
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
                    membersAsString.add(_connector.createUidFromName(ObjectClass.ACCOUNT, (String)member).getUidValue());
            }
            return membersAsString;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    public List<String> getGroupsForUserViaCommandLine(String user) {
        String command = "LISTUSER "+user+" RACF";
        String output = getCommandOutput(command);
        MapTransform transform = _segmentParsers.get("ACCOUNT.RACF");
        if (transform==null)
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNKNOWN_SEGMENT, ATTR_CL_GROUPS));
        try {
            Map<String, Object> map =(Map<String, Object>)transform.transform(output);
            List<Object> groups = (List<Object>)map.get(ATTR_CL_GROUPS);
            List<String> groupsAsString = new LinkedList<String>();
            for (Object group : groups)
                groupsAsString.add(_connector.createUidFromName(RacfConnector.RACF_GROUP, (String)group).getUidValue());
            return groupsAsString;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    Pattern _errorMessage = Pattern.compile("[^\\s]{9}");
    
    public List<String> getUsersViaCommandLine(String query) {
        String command = null;
        if (query!=null && query.length()>0)
            command = "SEARCH CLASS(USER) FILTER("+query+")";
        else
            command = "SEARCH CLASS(USER)";
        String users = getCommandOutput(command);
        if (users.contains("NO ENTRIES MEET SEARCH CRITERIA")) {
            return new LinkedList<String>();
        }

        // Error messages all start with a 9 character error code,
        // and users are at most 8 characters long. This allow us to
        // determine if there are any error messages in the text
        //
        Matcher matcher = _errorMessage.matcher(users); 
        if (matcher.find()) {
            String error = users.substring(matcher.start()).trim();
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ERROR_IN_GET_USERS, error));
        } else {
            String[] usersArray = users.trim().split("\\s+");
            for (int i=0; i<usersArray.length; i++)
                usersArray[i] = _connector.createUidFromName(ObjectClass.ACCOUNT, usersArray[i]).getUidValue();
            List<String> result = new LinkedList<String>(Arrays.asList(usersArray));
            // We eliminate certain users
            //
            for (String user : new String[] {"irrcerta", "irrmulti", "irrsitec"}) 
                result.remove(_connector.createUidFromName(ObjectClass.ACCOUNT, user).getUidValue());
            return result;
        }
    }
    
    public List<String> getGroupsViaCommandLine(String query) {
        String command = null;
        if (query!=null && query.length()>0)
            command = "SEARCH CLASS(GROUP) FILTER("+query+")";
        else
            command = "SEARCH CLASS(GROUP)";
        String groups = getCommandOutput(command);
        // Error messages all start with a 9 character error code,
        // and groups are at most 8 characters long. This allow us to
        // determine if there are any error messages in the text
        //
        Matcher matcher = _errorMessage.matcher(groups); 
        if (matcher.find()) {
            String error = groups.substring(matcher.start()).trim();
            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ERROR_IN_GET_GROUPS, error));
        } else {
            String[] groupsArray = groups.trim().split("\\s+");
            for (int i=0; i<groupsArray.length; i++)
                groupsArray[i] = _connector.createUidFromName(RacfConnector.RACF_GROUP, groupsArray[i]).getUidValue();
            return Arrays.asList(groupsArray);
        }
    }
    
    public Uid updateViaCommandLine(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        Uid uid = (Uid)attributes.get(Uid.NAME);
        String name = _connector.extractRacfIdFromLdapId(uid.getUidValue());
        
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            Attribute groupMembership = attributes.remove(ATTR_CL_GROUPS);
            Attribute expired = attributes.get(ATTR_CL_EXPIRED);
            Attribute password = attributes.get(ATTR_CL_PASSWORD);
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
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForUser(name, groupMembership);
                if (attributeString.length>0)
                    return createOrUpdateViaCommandLine(objectClass, name, buffer);
                else
                    return uid;
            } finally {
                buffer.clear();
            }
        } else if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            Attribute groupMembership = attributes.remove(ATTR_CL_MEMBERS);
            if (!groupExists(name))
                throw new UnknownUidException();
            
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ALTGROUP ");
            buffer.append(name);
            char[] attributeString = mapAttributesToString(attributes);
            buffer.append(attributeString);
            try {
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForGroups(name, groupMembership);
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
        if (!"groovy".equalsIgnoreCase(request.getScriptLanguage()))
            throw new IllegalArgumentException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.UNSUPPORTED_SCRIPTING_LANGUAGE, request.getScriptLanguage()));
        ScriptExecutor executor = _groovyFactory.newScriptExecutor(getClass().getClassLoader(), request.getScriptText(), false);
        Map<String, Object> arguments = new HashMap<String, Object>(request.getScriptArguments());
        try {
            RW3270Connection connection = _connector.getConnection().getRacfConnection();
            arguments.put("rw3270Connection", connection);
            Object result = executor.execute(arguments);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw ConnectorException.wrap(e);
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

    public Map<String, Object> getAttributesFromCommandLine(ObjectClass objectClass, String name, boolean ldapAvailable, Set<String> attributesToGet) {
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
        
        // If we have no LDAP, minimally, we need the RACF segment
        //
        if (!ldapAvailable)
            segmentsNeeded.add(RACF);
        
        String racfName = _connector.extractRacfIdFromLdapId(name);
        
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
                if (segmentsMatcher.matches()) {
                    // Deal with RACF first
                    //
                    int offset = 0;
                    if (racfNeeded) {
                        MapTransform transform = _segmentParsers.get(objectClassPrefix+"RACF");
                        try {
                            attributesFromCommandLine.putAll((Map<String, Object>)transform.transform(segmentsMatcher.group(1)));
                        } catch (Exception e) {
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
                } else if (output.toUpperCase().contains("UNABLE TO LOCATE USER")) {
                    throw new UnknownUidException();
                } else {
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
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_ATTRIBUTES)) {
                List<Object> value = (List<Object>)attributesFromCommandLine.get(ATTR_CL_ATTRIBUTES);
                attributesFromCommandLine.put(OperationalAttributes.ENABLE_NAME, !value.contains("REVOKED"));
            }
            // Last Access date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_LAST_ACCESS)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_LAST_ACCESS);
                Long converted = convertFromRacfTimestamp(value);
                attributesFromCommandLine.put(PredefinedAttributes.LAST_LOGIN_DATE_NAME, converted);
            }
            // password change date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_PASSDATE)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_PASSDATE);
                Long converted = convertFromRacfTimestamp(value);
                attributesFromCommandLine.put(PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME, converted);
                // password change date is 00.000 if expired
                //
                Boolean expired = "00.000".equals(value);
                attributesFromCommandLine.put(OperationalAttributes.PASSWORD_EXPIRED_NAME, expired);
            }
            // Revoke date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_REVOKE_DATE)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_REVOKE_DATE);
                Long converted = convertFromResumeRevokeFormat(value);
                attributesFromCommandLine.put(OperationalAttributes.DISABLE_DATE_NAME, converted);
            }
            // Resume date must be converted
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_RESUME_DATE)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_RESUME_DATE);
                Long converted = convertFromResumeRevokeFormat(value);
                attributesFromCommandLine.put(OperationalAttributes.ENABLE_DATE_NAME, converted);
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
                    attributesFromCommandLine.put(ATTR_CL_GROUPS, membersAsString);
                }
            }
        }

        // Remap GROUP attributes as needed
        //
        if (objectClass.is(RacfConnector.RACF_GROUP_NAME)) {
            // Owner must be a stringified Uid
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_OWNER)) {
                Object value = attributesFromCommandLine.get(ATTR_CL_OWNER);
                attributesFromCommandLine.put(ATTR_CL_OWNER, _connector.createUidFromName(RacfConnector.RACF_GROUP, (String)value).getUidValue());
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
                    attributesFromCommandLine.put(ATTR_CL_MEMBERS, membersAsString);
                }
            }
            // Groups must be stringified Uids
            //
            if (attributesFromCommandLine.containsKey(ATTR_CL_GROUPS)) {
                List<Object> members = (List<Object>)attributesFromCommandLine.remove(ATTR_CL_GROUPS);
                List<String> membersAsString = new LinkedList<String>();
                if (members!=null) {
                    for (Object member : members)
                        membersAsString.add(_connector.createUidFromName(RacfConnector.RACF_GROUP, (String)member).getUidValue());
                    attributesFromCommandLine.put(ATTR_CL_GROUPS, membersAsString);
                }
            }
        }
        return attributesFromCommandLine;
    }
    
    
    private Long convertFromResumeRevokeFormat(Object value) {
        try {
            return _resumeRevokeFormat.parse(value.toString()).getTime();
        } catch (ParseException pe) {
            return null;
        }
    }
    
    private Long convertFromRacfTimestamp(Object value) {
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
    
    private StringBuffer _buffer = new StringBuffer();
    private boolean _timedOut = false;
    private boolean _outputComplete = false;
    
    private void waitFor(Integer timeout) {
        _buffer.setLength(0);
        _outputComplete = false;
        try {
            _timedOut = false;
            List<Match> matches = new LinkedList<Match>();
            
            // Match the continue expression, so
            //  save the partial output
            //  ask for more output
            //
            matches.add(new RegExpMatch(OUTPUT_CONTINUING_PATTERN, new Closure() {
                public void run(ExpectState state) throws Exception {
                    
                    // If it's not in column 0, it's not a real match, we we ignore it
                    //
                    if (state.getMatchedWhere()%80!=0) {
                        state.exp_continue();
                        return;
                    }

                    String display = getRW3270Connection().getDisplay();
                    int index = display.lastIndexOf(OUTPUT_CONTINUING);
                    
                    // If the current display has already been appended by the
                    // OUTPUT_COMPLETE handler, we don't want to do it a second time
                    //
                    if (!_outputComplete) {
                        if (index>-1) {
                            _buffer.append(display.substring(0, index));
                        } else {
                            _buffer.append(display);
                        }
                        getRW3270Connection().sendEnter();
                    }
                    
                    // If we saw
                    //      READY
                    //      ***
                    // we're done, otherwise, we continue
                    //
                    if (display.lastIndexOf(OUTPUT_COMPLETE)+getRW3270Connection().getWidth()!=index) {
                        state.exp_continue();
                    }
                }
            }));
            
            // Match the command complete expression, so
            //  if there was an error,
            //      throw exception
            //  else
            //      save the final output
            matches.add(new RegExpMatch(OUTPUT_COMPLETE_PATTERN, new Closure() {
                public void run(ExpectState state) throws Exception {
                    Object errorDetected = state.getVar("errorDetected");

                    // If it's not in column 0, it's not a real match, we we ignore it
                    //
                    if (false && errorDetected==null && state.getMatchedWhere()%80!=0) {
                        state.exp_continue();
                        return;
                    }

                    // This code will be exported to a script, but I want to get the tests back on-line
                    //
                    String display = getRW3270Connection().getDisplay();
                    boolean commandComplete = removeTrailingSpaces(display).endsWith(OUTPUT_COMPLETE);
                    boolean matched = display.lastIndexOf(OUTPUT_COMPLETE)>-1;
                    appendScreen(display);
                    if (!commandComplete && matched) {
                        getRW3270Connection().sendEnter();
                        state.exp_continue();
                    } else if (!commandComplete) {
                        // I think we could wind up here via a race condition
                        // if we had the "READY\n***" kind of command completion,
                        // and there was a delay between seeing the READY and the ***.
                        //
                        getRW3270Connection().sendEnter();
                        state.exp_continue();
                    } else {
                        _outputComplete = true;
                        if (errorDetected!=null) {
                            throw new ConnectorException(((RacfConfiguration)_connector.getConfiguration()).getMessage(RacfMessages.ERROR_IN_RACF_COMMAND, errorDetected.toString().trim()));
                        }
                    }
                }

                private void appendScreen(String display) {
                    int index = display.lastIndexOf(OUTPUT_COMPLETE);
                    if (index>-1) {
                        _buffer.append(display.substring(0, Math.min(display.length(), index+getRW3270Connection().getWidth())));
                    } else {
                        _buffer.append(display);
                    }
                }
            }));
            
            // Match the error expression, so
            //  send the abort command
            //  continue execution, to see if we can recover
            //
            
            matches.add(new RegExpMatch("IKJ56703A REENTER THIS OPERAND", new Closure() {
                public void run(ExpectState state) throws Exception {
                    state.addVar("errorDetected", state.getBuffer());
                    // Need to strip off the match
                    //
                    String data = state.getBuffer();
                    //_buffer.append(data);
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
        while (buffer.charAt(buffer.length()-1)==' ')
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
