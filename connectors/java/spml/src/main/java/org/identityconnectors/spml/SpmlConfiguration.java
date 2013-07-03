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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
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
    private String userName;
    private GuardedString password;

    private String url;
    private String[] objectClassNames;
    private String[] spmlClassNames;
    private String[] targetNames;
    private String[] nameAttributes;

    private String preSendCommand;
    private String postReceiveCommand;
    private String preDisconnectCommand;
    private String postConnectCommand;
    private String mapSetNameCommand;
    private String mapAttributeCommand;
    private String mapQueryNameCommand;
    private String schemaCommand;

    private String scriptingLanguage;

    public SpmlConfiguration() {
    }

    public SpmlConfiguration(String url, String[] connectorObjectClass, String[] spmlObjectClass,
            String[] targetClass, String[] nameAttributes, String userName, GuardedString password) {
        this();
        this.url = url;
        this.userName = userName;
        this.nameAttributes = arrayCopy(nameAttributes);
        this.password = password;
        objectClassNames = arrayCopy(connectorObjectClass);
        spmlClassNames = arrayCopy(spmlObjectClass);
        targetNames = arrayCopy(targetClass);
    }

    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }

    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }

    private boolean isNull(String string) {
        return string == null || string.length() == 0;
    }

    private boolean isNull(GuardedString string) {
        if (string == null) {
            return true;
        }
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        string.access(accessor);
        char[] password = accessor.getArray();
        boolean isNull = password.length == 0;
        Arrays.fill(password, ' ');
        return isNull;
    }

    public void validate() {
        if (isNull(url)) {
            throw new ConnectorException(getMessage(SpmlMessages.URL_NULL));
        }
        if (isNull(userName)) {
            throw new ConnectorException(getMessage(SpmlMessages.USERNAME_NULL));
        }
        if (isNull(password)) {
            throw new ConnectorException(getMessage(SpmlMessages.PASSWORD_NULL));
        }
        if (objectClassNames == null) {
            throw new ConnectorException(getMessage(SpmlMessages.OBJECT_CLASS_NULL));
        }
        if (spmlClassNames == null) {
            throw new ConnectorException(getMessage(SpmlMessages.SPML_CLASS_NULL));
        }
        if (targetNames == null) {
            throw new ConnectorException(getMessage(SpmlMessages.TARGET_NULL));
        }
        if (nameAttributes == null) {
            throw new ConnectorException(getMessage(SpmlMessages.NAME_NULL));
        }
        if (scriptingLanguage == null) {
            throw new ConnectorException(getMessage(SpmlMessages.LANGUAGE_NULL));
        }
        if (objectClassNames.length != spmlClassNames.length
                || objectClassNames.length != targetNames.length
                || objectClassNames.length != nameAttributes.length) {
            throw new ConnectorException(getMessage(SpmlMessages.SPML_CLASS_LENGTH));
        }
    }

    @ConfigurationProperty(order = 1, required = true)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @ConfigurationProperty(order = 2, confidential = true, required = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 3, required = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ConfigurationProperty(order = 8)
    public String getScriptingLanguage() {
        return scriptingLanguage;
    }

    public void setScriptingLanguage(String language) {
        scriptingLanguage = language;
    }

    @ConfigurationProperty(order = 9)
    public String getPostConnectCommand() {
        return postConnectCommand;
    }

    public void setPostConnectCommand(String loginCommand) {
        postConnectCommand = loginCommand;
    }

    @ConfigurationProperty(order = 10)
    public String getPreDisconnectCommand() {
        return preDisconnectCommand;
    }

    public void setPreDisconnectCommand(String loginCommand) {
        preDisconnectCommand = loginCommand;
    }

    @ConfigurationProperty(order = 11)
    public String getPreSendCommand() {
        return preSendCommand;
    }

    public void setPreSendCommand(String sendCommand) {
        preSendCommand = sendCommand;
    }

    @ConfigurationProperty(order = 12)
    public String getPostReceiveCommand() {
        return postReceiveCommand;
    }

    public void setPostReceiveCommand(String receiveCommand) {
        postReceiveCommand = receiveCommand;
    }

    @ConfigurationProperty(order = 13)
    public String getMapSetNameCommand() {
        return mapSetNameCommand;
    }

    public void setMapSetNameCommand(String setNameCommand) {
        mapSetNameCommand = setNameCommand;
    }

    @ConfigurationProperty(order = 14)
    public String getMapAttributeCommand() {
        return mapAttributeCommand;
    }

    public void setMapAttributeCommand(String attributeCommand) {
        mapAttributeCommand = attributeCommand;
    }

    @ConfigurationProperty(order = 15)
    public String getMapQueryNameCommand() {
        return mapQueryNameCommand;
    }

    public void setMapQueryNameCommand(String queryNameCommand) {
        mapQueryNameCommand = queryNameCommand;
    }

    @ConfigurationProperty(order = 16)
    public String getSchemaCommand() {
        return schemaCommand;
    }

    public void setSchemaCommand(String schemaCommand) {
        this.schemaCommand = schemaCommand;
    }

    @ConfigurationProperty(order = 17)
    public String[] getNameAttributes() {
        return arrayCopy(nameAttributes);
    }

    public void setNameAttributes(String[] attribute) {
        nameAttributes = arrayCopy(attribute);
    }

    @ConfigurationProperty(order = 18)
    public String[] getObjectClassNames() {
        return arrayCopy(objectClassNames);
    }

    public void setObjectClassNames(String[] classNames) {
        objectClassNames = arrayCopy(classNames);
    }

    @ConfigurationProperty(order = 19)
    public String[] getSpmlClassNames() {
        return arrayCopy(spmlClassNames);
    }

    public void setSpmlClassNames(String[] classNames) {
        spmlClassNames = arrayCopy(classNames);
    }

    @ConfigurationProperty(order = 20)
    public String[] getTargetNames() {
        return arrayCopy(targetNames);
    }

    public void setTargetNames(String[] targetNames) {
        this.targetNames = arrayCopy(targetNames);
    }

    String[] arrayCopy(String[] array) {
        if (array == null) {
            return null;
        }
        String[] result = new String[array.length];
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }
}
