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

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

class CommandLineUtil {
    private static final String         OUTPUT_COMPLETE_PATTERN = "\\sREADY\\s{74}";
    private static final String         OUTPUT_COMPLETE         = " READY";
    private static final String         DEFAULT_GROUP_NAME      = "DFLTGRP";
    private static final String         LONG_DEFAULT_GROUP_NAME = "RACF.DFLTGRP";
    private static final String         OUTPUT_CONTINUING       = "\\s\\*\\*\\*\\s{76}";
    private static final String         RACF                    = "RACF";
    private static final String         DELETE_SEGMENT          = "Delete Segment";
    private static final int            COMMAND_TIMEOUT         = 60000;
    
    private static final String                _membersOfGroup =
        "<MapTransform>" +
        "  <PatternNode key='MEMBERS' pattern='USER\\(S\\)=[^\\n]*\\n((\\s{7}\\w+([^\\n]+\\n){3})*)' optional='true' reset='false'>" +
        "     <SubstituteTransform pattern='\\s{7}(\\w+)(?:[^\\n]+\\n){3}' substitute='$1 '/>" +
        "     <SplitTransform splitPattern='\\s+'/>" +
        "  </PatternNode>\n" +
        "</MapTransform>";
    private MapTransform                _membersOfGroupTransform;
    private Map<String, MapTransform>   _segmentParsers;
    private final Pattern               _connectionPattern  = Pattern.compile("racfuserid=(.*)\\+racfgroupid=(.*),.*");
    private final ScriptExecutorFactory _groovyFactory;
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
            _membersOfGroupTransform = (MapTransform)Transform.newTransform(_membersOfGroup);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
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
            return new String[] {matcher.group(1), matcher.group(1)};
        else
            return null;
    }

    public boolean isUserid(String uidString) {
        return uidString.toUpperCase().contains("PROFILETYPE=USER");
    }

    public boolean isGroupid(String uidString) {
        return uidString.toUpperCase().contains("PROFILETYPE=GROUP");
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
            throw new ConnectorException("TODO: error in command "+new String(command.getArray())+"\n"+"\""+output+"\"");
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
            connection.resetStandardOutput();
            connection.send("[clear]");
            connection.send(command);
            connection.send("[enter]");
            System.out.println("execute:'"+new String(command)+"'");
            connection.waitFor(OUTPUT_CONTINUING, OUTPUT_COMPLETE_PATTERN, COMMAND_TIMEOUT);
            String output = connection.getStandardOutput();
            // Remove OUTPUT_COMPLETE from end of output
            //
            output = output.substring(0, output.lastIndexOf(OUTPUT_COMPLETE));
            // Strip command from start, if present
            //
            int index = indexOf(output,command);
            if (index>-1) {
                // Round up to line length
                //
                index += connection.getWidth();//= connection.getWidth()+index/connection.getWidth()*connection.getWidth();
                output = output.substring(index);
            }
            output = output.replaceAll("(.{"+connection.getWidth()+"})", "$1\n");
            return output;
        } catch (Exception e) {
            e.printStackTrace();
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
        for (int i=0; i<string.length()-substring.length; i++)
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
    public char[] mapAttributesToString(Set<Attribute> attrs) {
        Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
        Name name = (Name)attributes.remove(Name.NAME);
        Uid uid = (Uid)attributes.remove(Uid.NAME);
        
        // Build up a map containing the segment attribute values
        //
        Map<String, Map<String, char[]>> attributeValues = new HashMap<String, Map<String,char[]>>();
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            String[] attributeName = entry.getKey().split("\\.");
            if (attributeName.length==1) {
                // Should be an OperationalAttribute.
                // Supported OperationalAttributes are:
                //   Password
                if (entry.getValue().is(OperationalAttributes.PASSWORD_NAME))
                    attributeName = new String[] { RACF, "PASSWORD" };
                else
                    throw new IllegalArgumentException("TODO");
            }
            
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
        if (objectClass.equals(ObjectClass.ACCOUNT)) {
            Set<Attribute> attributes = new HashSet<Attribute>(attrs); 
            String name = AttributeUtil.getNameFromAttributes(attrs).getNameValue();
            Attribute groups = AttributeUtil.find(PredefinedAttributes.GROUPS_NAME, attrs);
            attributes.remove(groups);
            
            if (userExists(name))
                throw new AlreadyExistsException();
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ADDUSER ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attributes));
            Uid uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            buffer.clear();
            if (groups!=null) {
                for (Object groupName : groups.getValue()) {
                    String command = "CONNECT "+name+" GROUP("+_connector.extractRacfIdFromLdapId((String)groupName)+")";
                    checkCommand(command);
                }
            }
            return uid;
        } else if (objectClass.equals(ObjectClass.GROUP)) {
            Set<Attribute> attributes = new HashSet<Attribute>(attrs); 
            String name = AttributeUtil.getNameFromAttributes(attrs).getNameValue();
            Attribute accounts = AttributeUtil.find(RacfConnector.ACCOUNTS_NAME, attrs);
            attributes.remove(accounts);
            if (groupExists(name))
                throw new AlreadyExistsException();
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ADDGROUP ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attrs));
            Uid uid = createOrUpdateViaCommandLine(objectClass, name, buffer);
            buffer.clear();
            if (accounts!=null) {
                for (Object accountName : accounts.getValue()) {
                    String command = "CONNECT "+accountName+" GROUP("+name+")";
                    checkCommand(command);
                }
            }
            return uid;
        } else if (objectClass.equals(RacfConnector.RACF_CONNECTION)) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            String[] info = extractRacfIdAndGroupIdFromLdapId(name.getNameValue());
            String command = "CONNECT "+info[0]+" GROUP("+info[1]+")";
            checkCommand(command);
            return new Uid(name.getNameValue());
        } else {
            throw new IllegalArgumentException("TODO");
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
            int status = groupExistsAndIsEmpty(name);
            if (status==-1)
                throw new UnknownUidException();
            if (status>0)
                throw new ConnectorException("TODO: cannot delete grop with members");
            String command = "DELGROUP "+name;
            checkCommand(command);
        } else if (isConnection(uidString)) {
            String[] info = extractRacfIdAndGroupIdFromLdapId(uidString);
            String command = "REMOVE "+info[0]+" GROUP("+info[1]+")";
            checkCommand(command);
        } else {
            throw new IllegalArgumentException("TODO");
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

    private int groupExistsAndIsEmpty(String name) {
        // TSO gets into a funny state if you attempt to delete an nonexisting group
        // so, check and see if the group already exists
        //
        String output = getCommandOutput("LISTGRP "+name);
        boolean notFound = (output.toUpperCase().contains("NAME NOT FOUND"));
        if (notFound)
            return -1;
        try {
            Map<String, Object> attributes = (Map<String, Object>)_membersOfGroupTransform.transform(output);
            List<Object> members = (List<Object>)attributes.get("MEMBERS");
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
            Map<String, Object> attributes = (Map<String, Object>)_membersOfGroupTransform.transform(output);
            List<Object> members = (List<Object>)attributes.get("MEMBERS");
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
        MapTransform transform = _segmentParsers.get(RACF);
        try {
            Map<String, Object> map =(Map<String, Object>)transform.transform(output);
            List<Object> groups = (List<Object>)map.get("RACF.GROUPS");
            List<String> groupsAsString = new LinkedList<String>();
            for (Object group : groups)
                groupsAsString.add(_connector.createUidFromName(ObjectClass.GROUP, (String)group).getUidValue());
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
            throw new ConnectorException("TODO error in getUsers:"+error);
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
            throw new ConnectorException("TODO error in getGroups:"+error);
        } else {
            String[] groupsArray = groups.trim().split("\\s+");
            for (int i=0; i<groupsArray.length; i++)
                groupsArray[i] = _connector.createUidFromName(ObjectClass.GROUP, groupsArray[i]).getUidValue();
            return Arrays.asList(groupsArray);
        }
    }
    
    public Uid updateViaCommandLine(ObjectClass objectClass, Set<Attribute> attrs, OperationOptions options) {
        if (objectClass.equals(ObjectClass.ACCOUNT)) {
            String name = AttributeUtil.getNameFromAttributes(attrs).getNameValue();
            if (!userExists(name))
                throw new UnknownUidException();
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ALTUSER ");
            buffer.append(name);
            buffer.append(mapAttributesToString(attrs));
            Attribute groupMembership = AttributeUtil.find(PredefinedAttributes.GROUPS_NAME, attrs);
            try {
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForUser(name, groupMembership);
                return createOrUpdateViaCommandLine(objectClass, name, buffer);
            } finally {
                buffer.clear();
            }
        } else if (objectClass.equals(ObjectClass.GROUP)) {
            String name = AttributeUtil.getNameFromAttributes(attrs).getNameValue();
            if (!groupExists(name))
                throw new UnknownUidException();
            CharArrayBuffer buffer = new CharArrayBuffer();
            buffer.append("ALTUSER ");
            buffer.append(name);
            Attribute groupMembership = AttributeUtil.find(RacfConnector.ACCOUNTS_NAME, attrs);
            try {
                if (groupMembership!=null)
                    _connector.setGroupMembershipsForGroups(name, groupMembership);
                return createOrUpdateViaCommandLine(objectClass, name, buffer);
            } finally {
                buffer.clear();
            }
        } else {
            throw new IllegalArgumentException("TODO");
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
    
    private String getAffinity() {
        return "XYZZY";
    }

    public Map<String, Object> getAttributesFromCommandLine(String name, boolean ldapAvailable, Set<String> attributesToGet) {
        // Determine the set of segment names, if any
        //
        Set<String> segmentsNeeded = new HashSet<String>();
        
        // If we have no LDAP, minimally, we need the RACF segment
        //
        if (!ldapAvailable)
            segmentsNeeded.add(RACF);
        
        if (attributesToGet!=null) {
            for (String attributeToGet : attributesToGet) {
                int index = attributeToGet.indexOf('.');
                if (index!=-1) {
                    String prefix = attributeToGet.substring(0, index);
                    segmentsNeeded.add(prefix);
                    
                    if (!_segmentParsers.containsKey(prefix)) {
                        throw new ConnectorException("Bad Attribute name (no such segment):"+attributeToGet);
                    }
                }
            }
        }

        // If we are asking for segment information, ensure that command-line login
        // information was specified
        //
        if (segmentsNeeded.size()>0 && ((RacfConfiguration)_connector.getConfiguration()).getUserName()==null)
            throw new ConnectorException("Segment attributes requested, but no login information given");
        
        Map<String, Object> attributesFromCommandLine = new HashMap<String, Object>();
        if (segmentsNeeded.size()>0 || ((RacfConfiguration)_connector.getConfiguration()).getUserName()==null) {
            try {
                StringBuffer buffer = new StringBuffer();
                buffer.append("LISTUSER ");
                buffer.append(_connector.extractRacfIdFromLdapId(name));
                for (String segment : segmentsNeeded)
                    if (!RACF.equals(segment))
                        buffer.append(" "+segment);

                String output = getCommandOutput(buffer.toString());
                // Parse each of the segments, and add the attributes to the set of
                // attributes received
                //
                for (String segment : segmentsNeeded) {
                    MapTransform transform = _segmentParsers.get(segment);
                    attributesFromCommandLine.putAll((Map<String, Object>)transform.transform(output));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw ConnectorException.wrap(e);
            }
        }
        // Default group name must be a stringified Uid
        //
        if (attributesFromCommandLine.containsKey(LONG_DEFAULT_GROUP_NAME)) {
            Object value = attributesFromCommandLine.get(LONG_DEFAULT_GROUP_NAME);
            attributesFromCommandLine.put(LONG_DEFAULT_GROUP_NAME, _connector.createUidFromName(ObjectClass.GROUP, (String)value).getUidValue());
        }
        return attributesFromCommandLine;
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
