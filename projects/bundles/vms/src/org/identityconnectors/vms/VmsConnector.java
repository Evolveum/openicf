/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.vms;

import static org.identityconnectors.vms.VmsConstants.*;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
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
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.patternparser.Transform;


@ConnectorClass(
    displayNameKey="VMSConnector",
    configurationClass= VmsConfiguration.class)
public class VmsConnector implements PoolableConnector, CreateOp,
DeleteOp, SearchOp<String>, UpdateOp, SchemaOp {
    private Log                         _log = Log.getLog(VmsConnector.class);
    private VmsConfiguration            _configuration;
    private VmsConnection               _connection;
    private Schema                      _schema;
    private Map<String, AttributeInfo>  _attributeMap;

    private ScriptExecutorFactory       _groovyFactory;
    private String                      _changeOwnPasswordCommandScript;
    private ScriptExecutor              _changeOwnPasswordCommandExecutor;
    private String                      _authorizeCommandScript;
    private ScriptExecutor              _authorizeCommandExecutor;
    private String                      _listCommandScript;
    private ScriptExecutor              _listCommandExecutor;

    private final DateFormat VMS_DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
    private final static String VMS_DELTA_FORMAT = "{0,number,##}-{1,number,##}:{2,number,##}:{3,number,##}.{4,number,00}";

    private static final String SEPARATOR       = "Username: ";
    private static final String UAF_PROMPT      = "UAF>";
    private static final Transform TRANSFORM    = new MapTransform(VmsAuthorizeInfo.getInfo());

    public static final int    LONG_WAIT       = 60000;
    public static final int    SHORT_WAIT      = 5000;
    

    // VMS messages we search for
    //
    private static final String USER_ADDED       = "%UAF-I-ADDMSG,";     // user record successfully added
    private static final String USER_EXISTS      = "%UAF-E-UAEERR,";     // invalid user name, user name already exists
    private static final String USER_REMOVED     = "%UAF-I-REMMSG,";     // record removed from system authorization file
    private static final String USER_UPDATED     = "%UAF-I-MDFYMSG,";    // user record(s) updated
    private static final String BAD_USER         = "%UAF-W-BADUSR,";     // user name does not exist
    private static final String BAD_SPEC         = "%UAF-W-BADSPC,";     // no user matches specification

    public VmsConnector() {
        _changeOwnPasswordCommandScript = 
            "connection.resetStandardOutput();\n" +
            "connection.send(\"SET PASSWORD\");\n" +
            "connection.waitFor(\"ld password:\", SHORT_WAIT);\n" +
            "connection.send(CURRENT_PASSWORD);\n" +
            "connection.waitFor(\"ew password:\", SHORT_WAIT);\n" +
            "connection.send(NEW_PASSWORD);\n" +
            "connection.waitFor(\"erification:\", SHORT_WAIT);\n" +
            "connection.send(NEW_PASSWORD);\n" +
            "connection.waitFor(SHELL_PROMPT, SHORT_WAIT);\n" +
            "return connection.getStandardOutput();"
            ;

        _authorizeCommandScript = 
            "connection.resetStandardOutput();\n" +
            "connection.send(\"SET DEFAULT SYS\\$SYSTEM:\");\n" +
            "connection.waitFor(SHELL_PROMPT, SHORT_WAIT);\n" +
            "connection.send(\"RUN AUTHORIZE\");\n" +
            "connection.waitFor(UAF_PROMPT, SHORT_WAIT);\n" +
            "connection.send(COMMAND);\n" +
            "connection.waitFor(UAF_PROMPT, SHORT_WAIT);\n" +
            "connection.send(\"EXIT\");\n" +
            "connection.waitFor(SHELL_PROMPT, SHORT_WAIT);\n" +
            "return connection.getStandardOutput();"
            ;

        _listCommandScript = 
            "connection.resetStandardOutput();\n" +
            "connection.send(\"SET DEFAULT SYS\\$SYSTEM:\");\n" +
            "connection.waitFor(SHELL_PROMPT, SHORT_WAIT);\n" +
            "connection.send(\"RUN AUTHORIZE\");\n" +
            "connection.waitFor(UAF_PROMPT, SHORT_WAIT);\n" +
            "for (command in COMMANDS) {\n" +
            "    connection.send(command.toString());\n" +
            "    connection.waitFor(UAF_PROMPT, LONG_WAIT);\n" +
            "}\n" +
            "connection.send(\"EXIT\");\n" +
            "connection.waitFor(SHELL_PROMPT, SHORT_WAIT);\n" +
            "return connection.getStandardOutput();"
            ;

        _groovyFactory = ScriptExecutorFactory.newInstance("GROOVY");
        _authorizeCommandExecutor = _groovyFactory.newScriptExecutor(getClass().getClassLoader(), _authorizeCommandScript, true);
        _changeOwnPasswordCommandExecutor = _groovyFactory.newScriptExecutor(getClass().getClassLoader(), _changeOwnPasswordCommandScript, true);
        _listCommandExecutor = _groovyFactory.newScriptExecutor(getClass().getClassLoader(), _listCommandScript, true);
    }

    /**
     * Since the exclamation point is a comment delimiter in DCL (and
     * AUTHORIZE input), we must quote any strings in which it appears.
     * We also need to quote if any whitespace is contained
     * 
     * @param unquoted
     * @return
     */
    protected char[] quoteWhenNeeded(char[] unquoted) {
        return quoteWhenNeeded(unquoted, false);
    }
    protected char[] quoteWhenNeeded(char[] unquoted, boolean needsQuote) {
        boolean quote = needsQuote;
        for (char character : unquoted) {
            if (character == '!' | character == ' ' | character == '\t')
                quote = true;
        }
        if (!quote) {
            char[] result = new char[unquoted.length];
            System.arraycopy(unquoted, 0, result, 0, result.length);
            return result;
        }
        
        CharArrayBuffer buffer = new CharArrayBuffer();
        buffer.append("\"");
        for (char character : unquoted) {
            if (character == '"')
                buffer.append("\\\"");
            else
                buffer.append(character);
        }
        buffer.append("\"");
        char[] result = buffer.getArray();
        buffer.clear();
        return result;
    }
    
    protected char[] asCharArray(Object object) {
        if (object instanceof GuardedString) {
            GuardedString guarded = (GuardedString)object;
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            guarded.access(accessor);
            char[] result = accessor.getArray();
            return result;
        } else if (object!=null) {
            return object.toString().toCharArray();
        } else {
            return null;
        }
    }

    protected char[] listToVmsValueList(List<Object> values) {
        if (values.size()==1) {
            if (values.get(0)==null) {
                return null;
            } else {
                char[] valueArray = asCharArray(values.get(0));
                char [] result = quoteWhenNeeded(valueArray);
                Arrays.fill(valueArray, 0, valueArray.length, ' ');
                return result;
            }
        } else {
            CharArrayBuffer buffer = new CharArrayBuffer();
            char separator = '(';
            for (Object value : values) {
                buffer.append(separator);
                char[] valueArray = asCharArray(value);
                char [] result = quoteWhenNeeded(valueArray);
                Arrays.fill(valueArray, 0, valueArray.length, ' ');
                buffer.append(result);
                Arrays.fill(result, 0, result.length, ' ');
                separator = ',';
            }
            buffer.append(")");
            char[] contents = buffer.getArray();
            buffer.clear();
            return contents;
        }
    }

    protected boolean isPresent(String base, String subString) {
        if (base==null)
            return false;
        else
            return base.contains(subString);
    }

    private void appendAttributes(Set<? extends Attribute> attrs, CharArrayBuffer addCommand) {
        Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));

        // Enable/disable is handled by FLAGS=([NO]DISUSER)
        //
        Attribute enable = attrMap.remove(OperationalAttributes.ENABLE_NAME);
        if (enable!=null) {
            Attribute flags = attrMap.remove(VmsConstants.ATTR_FLAGS);
            List<Object> flagsValue = new LinkedList<Object>();
            if (flags!= null)
                flagsValue = flags.getValue();
            if (AttributeUtil.getBooleanValue(enable))
                flagsValue.add(VmsConstants.NO+FLAG_DISUSER);
            else
                flagsValue.add(VmsConstants.FLAG_DISUSER);
            attrMap.put(VmsConstants.ATTR_FLAGS, AttributeBuilder.build(VmsConstants.ATTR_FLAGS, flagsValue));
        }
        
        // Administrative user doesn't need current password
        //
        Attribute currentPassword = attrMap.remove(OperationalAttributes.CURRENT_PASSWORD_NAME);
        
        // Password expiration date is handled by the /PWDEXPIRED qualifier
        //
        Attribute expiration = attrMap.remove(OperationalAttributes.PASSWORD_EXPIRED_NAME);
        if (expiration!=null) {
            if (AttributeUtil.getBooleanValue(expiration)) 
                addCommand.append("/"+VmsConstants.ATTR_PWDEXPIRED);
            else
                addCommand.append("/"+VmsConstants.NO+VmsConstants.ATTR_PWDEXPIRED);
        }
        // Password expiration date is handled by the /PWDLIFETIME qualifier
        // VMS uses a delta time for lifetime, not an expiration date, so we need to convert
        //
        Attribute expirationDate = attrMap.remove(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME);
        if (expirationDate!=null) {
            long expirationTime = AttributeUtil.getLongValue(expirationDate).longValue();
            if (expirationTime==0) {
                addCommand.append("/"+VmsConstants.NO+ATTR_PWDLIFETIME);
            } else {
                long now = new Date().getTime();
                String value = remapToDelta(expirationTime-now);
                addCommand.append("/"+VmsConstants.ATTR_PWDLIFETIME+"="+new String(quoteWhenNeeded(value.toCharArray(), true)));
            }
        }
        
        for (Attribute attribute : attrMap.values()) {
            String name = attribute.getName();
            List<Object> values = new LinkedList<Object>(attribute.getValue());
            if (!name.equals(Name.NAME) && isNeedsValidation(attribute)) {
                VmsAttributeValidator.validate(name, values, _configuration);
                if (values.size()==0) {
                    addCommand.append("/"+remapName(attribute));
                } else {
                    if (isDateTimeAttribute(name))
                        remapToDateTime(values);
                    if (isDeltaAttribute(name))
                        remapToDelta(values);
                    char[] value = listToVmsValueList(values);
                    addCommand.append("/"+remapName(attribute)+"=");
                    addCommand.append(value);
                    Arrays.fill(value, 0, value.length, ' ');
                }
            }
        }
    }
    
    private boolean isDateTimeAttribute(String attributeName) {
        return false;
    }
    
    private boolean isDeltaAttribute(String attributeName) {
        if (PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME.equals(attributeName))
            return true;
        return false;
    }
    
    private void remapToDateTime(List<Object> values) {
        for (int i=0; i<values.size(); i++) {
            Object value = values.get(i);
            values.set(i, VMS_DATE_FORMAT.format(new Date((Long)value)));
        }
    }

    static void remapToDelta(List<Object> values) {
        for (int i=0; i<values.size(); i++) {
            Object value = values.get(i);
            value = remapToDelta((Long)value);
            values.set(i, value);
        }
    }

    Pattern _deltaPattern = Pattern.compile("(?:(\\d+)\\s)?(\\d+):(\\d+)(?:(\\d+))?(?:.(\\d+))?");
    private long remapFromDelta(String delta) {
        Matcher matcher = _deltaPattern.matcher(delta);
        if (matcher.matches()) {
            String daysS         = matcher.group(1);
            String hoursS        = matcher.group(2);
            String minutesS      = matcher.group(3);
            String secondsS      = matcher.group(4);
            String centisecondsS = matcher.group(5);
            
            long days = daysS==null?0:Long.parseLong(daysS);
            long hours = Long.parseLong(hoursS);
            long minutes = Long.parseLong(minutesS);
            long seconds = secondsS==null?0:Long.parseLong(secondsS);
            long centiseconds = centisecondsS==null?0:Long.parseLong(centisecondsS);
            long result = ((((((((days*24)+hours)*60)+minutes)*60)+seconds)*100)+centiseconds)*10;
            return result;
        }
        return 0;
    }
    
    private static String remapToDelta(Long longValue) {
        // Convert to hundredths of a second
        longValue /= 10;
        long centiseconds = longValue % 100;
        // Convert to seconds
        longValue /= 100;
        long seconds = longValue % 60;
        // Convert to minutes
        longValue /= 60;
        long minutes = longValue % 60;
        // Convert to hours
        longValue /= 60;
        long hours = longValue % 24;
        // Convert to days
        longValue /= 24;
        long days = longValue;
        String value = MessageFormat.format(VMS_DELTA_FORMAT, new Object[] {days, hours, minutes, seconds, centiseconds});
        return value;
    }

    private Date getDateValue(Attribute attribute) {
        return new Date(AttributeUtil.getLongValue(attribute));
    }

    private String remapName(Attribute attribute) {
        if (attribute.is(OperationalAttributes.PASSWORD_NAME)) 
            return "PASSWORD";
        if (attribute.is(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME)) 
            return "EXPIRATION";
        if (attribute.is(PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME)) 
            return "PWDLIFETIME";
        else
            return attribute.getName();
    }

    private boolean isNeedsValidation(Attribute attribute) {
        if (attribute.is(OperationalAttributes.CURRENT_PASSWORD_NAME)) 
            return false;
        if (attribute instanceof Uid)
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Uid create(ObjectClass objectClass, final Set<Attribute> attrs, final OperationOptions options) {
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        CharArrayBuffer addCommand = new CharArrayBuffer();
        String accountId = name.getNameValue();
        _log.info("create(''{0}'')", accountId);
        addCommand.append("ADD "+accountId);
        appendAttributes(attrs, addCommand);

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("SHELL_PROMPT", _configuration.getHostShellPrompt());
        variables.put("SHORT_WAIT", SHORT_WAIT);
        variables.put("UAF_PROMPT", UAF_PROMPT);
        char[] commandContents = addCommand.getArray();
        addCommand.clear();
        variables.put("COMMAND", commandContents);
        variables.put("connection", _connection);

        String result = "";
        try {
            result = (String)_authorizeCommandExecutor.execute(variables);
            Arrays.fill(commandContents, 0, commandContents.length, ' ');
        } catch (Exception e) {
            Arrays.fill(commandContents, 0, commandContents.length, ' ');
            _log.error(e, "error in create");
            throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_CREATE), e);
        }

        if (isPresent(result, USER_ADDED)) {
            _log.info("user created");
            return new Uid(accountId);
        } else if (isPresent(result, USER_EXISTS)) {
            throw new AlreadyExistsException();
        } else {
            throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_CREATE2, result));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        _log.info("delete(''{0}'')", uid.getUidValue());
        String removeCommand = "REMOVE "+uid.getUidValue();
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("SHELL_PROMPT", _configuration.getHostShellPrompt());
        variables.put("SHORT_WAIT", SHORT_WAIT);
        variables.put("UAF_PROMPT", UAF_PROMPT);
        variables.put("COMMAND", removeCommand);
        variables.put("connection", _connection);

        String result = "";
        try {
            result = (String)_authorizeCommandExecutor.execute(variables);
        } catch (Exception e) {
            _log.error(e, "error in delete");
            throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_DELETE), e);
        }

        if (isPresent(result, USER_REMOVED)) {
            _log.info("user deleted");
            return;
        } else if (isPresent(result, BAD_USER)) {
            throw new UnknownUidException();
        } else {
            throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_DELETE2, result));
        }
    }

    VmsConnection getConnection() {
        return _connection;
    }
    
    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new VmsFilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        try {
            if ( query == null ) {
                query = "*";
            }
            _log.info("executeQuery(''{0}'')", query);
            List<String> filterStrings = new ArrayList<String>();
            filterStrings.add(query);
            Set<String> attributesToGet = null;
            if (options!=null && options.getAttributesToGet()!=null)
                attributesToGet = CollectionUtil.newReadOnlySet(options.getAttributesToGet());
            filterUsers(handler, filterStrings, attributesToGet==null?null:CollectionUtil.newReadOnlySet(attributesToGet));
        } catch (Exception e) {
            _log.error(e, "error in executeQuery");
            throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_SEARCH), e);
        }
    }

    private void filterUsers(ResultsHandler handler, List<String> filters, Set<String> searchAttributesToGet) throws Exception {
        List<String> commands = new LinkedList<String>();
        for (String filter : filters)
            commands.add("SHOW "+filter);
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("SHELL_PROMPT", _configuration.getHostShellPrompt());
        variables.put("SHORT_WAIT", SHORT_WAIT);
        variables.put("LONG_WAIT", LONG_WAIT);
        variables.put("UAF_PROMPT", UAF_PROMPT);
        variables.put("COMMANDS", commands);
        variables.put("connection", _connection);

        String users = (String)_listCommandExecutor.execute(variables);

        String[] userArray = users.split(SEPARATOR);
        List<String> attributesToGet = null;
        if (searchAttributesToGet!=null)
            attributesToGet = CollectionUtil.newList(searchAttributesToGet);
        for (int i=1; i<userArray.length; i++) {
            String user = SEPARATOR+userArray[i].replaceAll("\r\n", "\n");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>)TRANSFORM.transform(user);
                ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                builder.setUid((String)attributes.get(Name.NAME));
                // ENABLE is handled by checking to see if the DisUser Flag is
                // present
                //
                List<String> flags = (List<String>)attributes.get(VmsConstants.ATTR_FLAGS);
                if (flags.size()>0) {
                    int size = flags.size();
                    if (includeInAttributes(OperationalAttributes.ENABLE_NAME, attributesToGet)) {
                        if (flags.contains(VmsConstants.FLAG_DISUSER))
                            builder.addAttribute(OperationalAttributes.ENABLE_NAME, Boolean.FALSE);
                        else
                            builder.addAttribute(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
                    }
                } else {
                    if (includeInAttributes(OperationalAttributes.ENABLE_NAME, attributesToGet))
                        builder.addAttribute(OperationalAttributes.ENABLE_NAME, Boolean.TRUE);
                }
                
                // PASSWORD_EXPIRED is handled by seeing the last password change plus
                // the password lifetime is before the current time.
                // If the password is pre-expired, we always set this to TRUE 
                //
                if (includeInAttributes(OperationalAttributes.PASSWORD_EXPIRED_NAME, attributesToGet)) {
                    String lastChange = (String)attributes.get(VmsConstants.ATTR_PWDCHANGE);
                    if (lastChange.contains("(pre-expired)")) {
                        builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRED_NAME, Boolean.TRUE);
                    } else {
                        Date expiredDate = getPasswordExpirationDate(attributes); 
                        builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRED_NAME, expiredDate.before(new Date()));
                    }
                }
                
                // PASSWORD_EXPIRATION_DATE is handled by seeing if there is an expiration
                // date, and if so, converting it to milliseconds
                //
                if (includeInAttributes(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, attributesToGet)) {
                    String lifetime = (String)attributes.get(VmsConstants.ATTR_PWDLIFETIME);
                    String change = (String)attributes.get(VmsConstants.ATTR_PWDCHANGE);
                    if (lifetime.equalsIgnoreCase("(none)")) {
                        builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, 0);
                    } else if (change.contains("(pre-expired)")) {
                        builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, new Date().getTime());
                    } else {
                        Date expiredDate = getPasswordExpirationDate(attributes); 
                        builder.addAttribute(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, expiredDate.getTime());
                    }
                }
                
                // All other attributes are handled normally
                //
                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    Object value = entry.getValue();
                    String key = entry.getKey();
                    if (includeInAttributes(key, attributesToGet)) {
                        if (value instanceof Collection)
                            builder.addAttribute(key, (Collection<?>)value);
                        else
                            builder.addAttribute(key, value);
                    }
                }
                ConnectorObject next = builder.build();
                handler.handle(next);
            } catch (Exception e) {
                _log.error(e, "error parsing users");
                throw new ConnectorException(e);
            }
        }
    }

    private Date getPasswordExpirationDate(Map<String, Object> attributes) throws ParseException {
        String lastChange = (String)attributes.get(VmsConstants.ATTR_PWDCHANGE);
        long lastChangeDate = VMS_DATE_FORMAT.parse(lastChange).getTime();
        String lifetime = (String)attributes.get(VmsConstants.ATTR_PWDLIFETIME);
        long lifetimeLong = remapFromDelta(lifetime);
        Date expiredDate = new Date(lastChangeDate+lifetimeLong);
        return expiredDate;
    }
    
    private boolean includeInAttributes(String attribute, List<String> attributesToGet) {
        if (attribute.equalsIgnoreCase(Name.NAME))
            return true;
        if (attributesToGet!=null) {
            return attributesToGet.contains(attribute);
        } else {
            if (_schema==null)
                schema();
            if (_attributeMap.containsKey(attribute))
                return _attributeMap.get(attribute).isReturnedByDefault();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objclass, Set<Attribute> attributes, OperationOptions options) {
        Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attributes));
        
        // Operational Attributes are handled specially
        //
        Uid uid = (Uid)attrMap.remove(Uid.NAME);
        Name name = (Name)attrMap.remove(Name.NAME);
        Attribute currentPassword = attrMap.remove(OperationalAttributes.CURRENT_PASSWORD_NAME);
        Attribute newPassword = attrMap.remove(OperationalAttributes.PASSWORD_NAME);

        
        
        // If the only attributes are
        //    UID
        //    Name
        //    Password
        //    CurrentPassword
        // AND we are changing OUR OWN password,
        // we don't need to use AUTHORIZE.
        // Instead, we can use set password command
        //
        if (attrMap.size()==0 &&
                uid!=null &&
                name!=null &&
                currentPassword!=null &&
                newPassword!=null &&
                name.getNameValue().equalsIgnoreCase(_configuration.getUserName())) {
            _log.info("update[changePassword](''{0}'')", _configuration.getUserName());
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("SHELL_PROMPT", _configuration.getHostShellPrompt());
            variables.put("SHORT_WAIT", SHORT_WAIT);
            variables.put("UAF_PROMPT", UAF_PROMPT);
            GuardedString currentGS = AttributeUtil.getGuardedStringValue(currentPassword);
            GuardedString newGS = AttributeUtil.getGuardedStringValue(newPassword);
            GuardedStringAccessor accessor = new GuardedStringAccessor();
            currentGS.access(accessor);
            char[] currentArray = accessor.getArray();
            newGS.access(accessor);
            char[] newArray = accessor.getArray();
            variables.put("CURRENT_PASSWORD", currentArray);
            variables.put("NEW_PASSWORD", newArray);
            variables.put("connection", _connection);

            String result = "";
            try {
                result = (String)_changeOwnPasswordCommandExecutor.execute(variables);
                Arrays.fill(currentArray, 0, currentArray.length, ' ');
                Arrays.fill(newArray, 0, newArray.length, ' ');
            } catch (Exception e) {
                Arrays.fill(currentArray, 0, currentArray.length, ' ');
                Arrays.fill(newArray, 0, newArray.length, ' ');
                _log.error(e, "error in create");
                throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY), e);
            }

            if (result.indexOf("%SET-")>-1) {
                String errortext = result.substring(result.indexOf("%SET-")).split("\n")[0];
                throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY2, errortext));
            }
            String accountId = AttributeUtil.getUidAttribute(attributes).getUidValue();
            return new Uid(accountId);

        } else {
            CharArrayBuffer modifyCommand = new CharArrayBuffer();
            String accountId = AttributeUtil.getUidAttribute(attributes).getUidValue();
            modifyCommand.append("MODIFY "+accountId);
            appendAttributes(attributes, modifyCommand);

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("SHELL_PROMPT", _configuration.getHostShellPrompt());
            variables.put("SHORT_WAIT", SHORT_WAIT);
            variables.put("UAF_PROMPT", UAF_PROMPT);
            char[] commandContents = modifyCommand.getArray();
            modifyCommand.clear();
            variables.put("COMMAND", commandContents);
            variables.put("connection", _connection);

            String result = "";
            try {
                result = (String)_authorizeCommandExecutor.execute(variables);
                Arrays.fill(commandContents, 0, commandContents.length, ' ');
            } catch (Exception e) {
                Arrays.fill(commandContents, 0, commandContents.length, ' ');
                _log.error(e, "error in create");
                throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY), e);
            }
            
            if (isPresent(result, USER_UPDATED)) {
                return new Uid(accountId);
            } else if (isPresent(result, BAD_SPEC)) {
                throw new UnknownUidException();
            } else {
                throw new ConnectorException(_configuration.getMessage(VmsMessages.ERROR_IN_MODIFY2, result));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        if (_schema!=null)
            return _schema;
        
        final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());
        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();

        // Required Attributes
        //
        attributes.add(buildRequiredAttribute(Name.NAME,            String.class));
        attributes.add(buildRequiredAttribute(ATTR_UIC,             String.class));

        // Optional Attributes (have VMS default values)
        //
        attributes.add(AttributeInfoBuilder.build(ATTR_OWNER,       String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_ACCOUNT,     String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_CLI,         String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_CLITABLES,   String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_LGICMD,      String.class, false));
        //attributes.add(AttributeInfoBuilder.build(ATTR_EXPIRATION,  String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_PWDMINIMUM,  Integer.class, false));
        //attributes.add(AttributeInfoBuilder.build(ATTR_PWDLIFETIME, String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_MAXJOBS,     Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_MAXACCTJOBS, Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_SHRFILLM,    Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_PBYTLM,      Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_MAXDETACH,   Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_BIOLM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_JTQUOTA,     Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_DIOLM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_WSDEFAULT,   Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_PRIORITY,    Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_WSQUOTA,     Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_QUEPRIO,     Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_WSEXTENT,    Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_CPUTIME,     String.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_ENQLM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_PGFLQUOTA,   Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_TQELM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_ASTLM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_BYTLM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_FILLM,       Integer.class, false));
        attributes.add(AttributeInfoBuilder.build(ATTR_PRCLM,       Integer.class, false));

        // Multi-valued attributes
        //
        attributes.add(buildMultivaluedAttribute(ATTR_FLAGS,        String.class, false));
        attributes.add(buildMultivaluedAttribute(ATTR_PRIMEDAYS,    String.class, false));
        attributes.add(buildMultivaluedAttribute(ATTR_PRIVILEGES,   String.class, false));
        attributes.add(buildMultivaluedAttribute(ATTR_DEFPRIVILEGES,String.class, false));
        attributes.add(buildMultivaluedAttribute(ATTR_FLAGS,        String.class, false));

        // Read-only attributes
        //
        attributes.add(buildReadonlyAttribute(ATTR_SECDAYS,         String.class, false));

        // Write-only attributes
        //
        attributes.add(buildWriteonlyAttribute(ATTR_ALGORITHM,      String.class, false, false));

        // Operational Attributes
        //
        attributes.add(OperationalAttributeInfos.PASSWORD);
        attributes.add(OperationalAttributeInfos.CURRENT_PASSWORD);
        attributes.add(OperationalAttributeInfos.ENABLE);
        attributes.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
        attributes.add(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
        
        // Predefined Attributes
        //
        attributes.add(PredefinedAttributeInfos.PASSWORD_CHANGE_INTERVAL);

        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        _schema = schemaBuilder.build();
        _attributeMap = AttributeInfoUtil.toMap(attributes);
        return _schema;
    }

    private AttributeInfo buildMultivaluedAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValue(true);
        return builder.build();
    }

    private AttributeInfo buildRequiredAttribute(String name, Class<?> clazz) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(true);
        builder.setMultiValue(false);
        return builder.build();
    }

    private AttributeInfo buildReadonlyAttribute(String name, Class<?> clazz, boolean required) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValue(true);
        builder.setCreateable(false);
        builder.setUpdateable(false);
        return builder.build();
    }

    private AttributeInfo buildWriteonlyAttribute(String name, Class<?> clazz, boolean required, boolean multi) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValue(multi);
        builder.setReadable(false);
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (_connection != null) {
            _connection.dispose();
            _connection = null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void checkAlive() {
        _connection.test();
    }

    /**
     * {@inheritDoc}
     */
    public Configuration getConfiguration() {
        return _configuration;
    }

    /**
     * {@inheritDoc}
     */
    public void init(Configuration cfg) {
        _configuration = (VmsConfiguration)cfg;
        _connection = VmsConnectionFactory.newConnection(_configuration);
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
