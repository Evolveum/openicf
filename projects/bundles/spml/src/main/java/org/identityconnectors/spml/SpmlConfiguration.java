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
package org.identityconnectors.spml;

import java.util.Arrays;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


public class SpmlConfiguration extends AbstractConfiguration {
    private String             _userName;
    private GuardedString      _password;

    private String             _url;
    private String[]           _objectClassNames;
    private String[]           _spmlClassNames;
    private String[]           _targetNames;
    private String[]           _nameAttributes;

    private String             _preSendCommand;
    private String             _postReceiveCommand;
    private String             _preDisconnectCommand;
    private String             _postConnectCommand;
    private String             _mapSetNameCommand;
    private String             _mapAttributeCommand;
    private String             _mapQueryNameCommand;
    private String             _schemaCommand;

    private String             _scriptingLanguage;

    public SpmlConfiguration() {
    }

    public SpmlConfiguration(String url, String[] connectorObjectClass, String[] spmlObjectClass, String[] targetClass, String[] nameAttributes, String userName, GuardedString password) {
        this();
        _url = url;
        _userName = userName;
        _nameAttributes = arrayCopy(nameAttributes);
        _password = password;
        _objectClassNames = arrayCopy(connectorObjectClass);
        _spmlClassNames = arrayCopy(spmlObjectClass);
        _targetNames = arrayCopy(targetClass);
    }
    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }

    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }

    private boolean isNull(String string) {
        return string==null || string.length()==0;
    }

    private boolean isNull(GuardedString string) {
        if (string==null)
            return true;
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        string.access(accessor);
        char[] password = accessor.getArray();
        boolean isNull = password.length==0;
        Arrays.fill(password, ' ');
        return isNull;
    }

    public void validate() {
        if (isNull(_url))
            throw new ConnectorException(getMessage(SpmlMessages.URL_NULL));
        if (isNull(_userName))
            throw new ConnectorException(getMessage(SpmlMessages.USERNAME_NULL));
        if (isNull(_password))
            throw new ConnectorException(getMessage(SpmlMessages.PASSWORD_NULL));
        if (_objectClassNames==null)
            throw new ConnectorException(getMessage(SpmlMessages.OBJECT_CLASS_NULL));
        if (_spmlClassNames==null)
            throw new ConnectorException(getMessage(SpmlMessages.SPML_CLASS_NULL));
        if (_targetNames==null)
            throw new ConnectorException(getMessage(SpmlMessages.TARGET_NULL));
        if (_nameAttributes==null)
            throw new ConnectorException(getMessage(SpmlMessages.NAME_NULL));
        if (_scriptingLanguage==null)
            throw new ConnectorException(getMessage(SpmlMessages.LANGUAGE_NULL));
        if (_objectClassNames.length!=_spmlClassNames.length || _objectClassNames.length!=_targetNames.length || _objectClassNames.length!=_nameAttributes.length)
            throw new ConnectorException(getMessage(SpmlMessages.SPML_CLASS_LENGTH));
    }

    @ConfigurationProperty(order=1, required=true)
    public String getUserName() {
        return _userName;
    }

    public void setUserName(String userName) {
        _userName = userName;
    }

    @ConfigurationProperty(order=2, confidential=true, required=true)
    public GuardedString getPassword() {
        return _password;
    }

    public void setPassword(GuardedString password) {
        _password = password;
    }

    @ConfigurationProperty(order=3, required=true)
    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = url;
    }

    @ConfigurationProperty(order=8)
    public String getScriptingLanguage() {
        return _scriptingLanguage;
    }

    public void setScriptingLanguage(String language) {
        _scriptingLanguage = language;
    }

    @ConfigurationProperty(order=9)
    public String getPostConnectCommand() {
        return _postConnectCommand;
    }

    public void setPostConnectCommand(String loginCommand) {
        _postConnectCommand = loginCommand;
    }

    @ConfigurationProperty(order=10)
    public String getPreDisconnectCommand() {
        return _preDisconnectCommand;
    }

    public void setPreDisconnectCommand(String loginCommand) {
        _preDisconnectCommand = loginCommand;
    }

    @ConfigurationProperty(order=11)
    public String getPreSendCommand() {
        return _preSendCommand;
    }

    public void setPreSendCommand(String sendCommand) {
        _preSendCommand = sendCommand;
    }

    @ConfigurationProperty(order=12)
    public String getPostReceiveCommand() {
        return _postReceiveCommand;
    }

    public void setPostReceiveCommand(String receiveCommand) {
        _postReceiveCommand = receiveCommand;
    }

    @ConfigurationProperty(order=13)
    public String getMapSetNameCommand() {
        return _mapSetNameCommand;
    }

    public void setMapSetNameCommand(String setNameCommand) {
        _mapSetNameCommand = setNameCommand;
    }

    @ConfigurationProperty(order=14)
    public String getMapAttributeCommand() {
        return _mapAttributeCommand;
    }

    public void setMapAttributeCommand(String attributeCommand) {
        _mapAttributeCommand = attributeCommand;
    }

    @ConfigurationProperty(order=15)
    public String getMapQueryNameCommand() {
        return _mapQueryNameCommand;
    }

    public void setMapQueryNameCommand(String queryNameCommand) {
        _mapQueryNameCommand = queryNameCommand;
    }

    @ConfigurationProperty(order=16)
    public String getSchemaCommand() {
        return _schemaCommand;
    }

    public void setSchemaCommand(String schemaCommand) {
        _schemaCommand = schemaCommand;
    }

    @ConfigurationProperty(order=17)
    public String[] getNameAttributes() {
        return arrayCopy(_nameAttributes);
    }

    public void setNameAttributes(String[] attribute) {
        _nameAttributes = arrayCopy(attribute);
    }

    @ConfigurationProperty(order=18)
    public String[] getObjectClassNames() {
        return arrayCopy(_objectClassNames);
    }

    public void setObjectClassNames(String[] classNames) {
        _objectClassNames = arrayCopy(classNames);
    }

    @ConfigurationProperty(order=19)
    public String[] getSpmlClassNames() {
        return arrayCopy(_spmlClassNames);
    }

    public void setSpmlClassNames(String[] classNames) {
        _spmlClassNames = arrayCopy(classNames);
    }

    @ConfigurationProperty(order=20)
    public String[] getTargetNames() {
        return arrayCopy(_targetNames);
    }

    public void setTargetNames(String[] targetNames) {
        _targetNames = arrayCopy(targetNames);
    }

    String[] arrayCopy(String[] array) {
        if (array==null)
            return null;
        String [] result = new String[array.length];
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }
}
