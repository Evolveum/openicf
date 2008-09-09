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
package org.identityconnectors.spml;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


public class SpmlConfiguration extends AbstractConfiguration {
    private String             _userName;
    private GuardedString      _password;

    private String             _protocol;
    private String             _hostNameOrIpAddr;
    private Integer            _hostPortNumber;
    private String             _file;
    private String[]           _objectClassNames;
    private String[]           _spmlClassNames;
    private String[]           _targetNames;
    private String             _psoTarget;
    private String             _nameAttribute;

    private String             _preSendCommand;
    private String             _postReceiveCommand;
    private String             _preDisconnectCommand;
    private String             _postConnectCommand;
    private String             _mapSetNameCommand;
    private String             _mapAttributeCommand;
    private String             _mapQueryNameCommand;

    private ResourceBundle     _bundle = null; 
    private Locale             _lastLocale = null; 

    private static final String CATALOG = "org.identityconnectors.spml.SpmlMessages";

    public SpmlConfiguration() {
    }

    public SpmlConfiguration(String protocol, String hostName, Integer port, String file, String[] connectorObjectClass, String[] spmlObjectClass, String[] targetClass, String nameAttribute, String userName, GuardedString password) {
        this();
        _protocol = protocol;
        _hostNameOrIpAddr = hostName;
        _hostPortNumber = port;
        _file = file;
        _userName = userName;
        _nameAttribute = nameAttribute;
        _password = password;
        _objectClassNames = arrayCopy(connectorObjectClass);
        _spmlClassNames = arrayCopy(spmlObjectClass);
        _targetNames = arrayCopy(targetClass);
    }

    //TODO: use ConnectorMessages instead
    private ResourceBundle getBundle() {
        if (_bundle==null || CurrentLocale.get()!=_lastLocale) {
            _lastLocale = CurrentLocale.get();
            if (_lastLocale==null)
                _lastLocale = Locale.getDefault();
            _bundle = ResourceBundle.getBundle(CATALOG, _lastLocale); 
        }
        return _bundle;
    }

    public String getMessage(String key) {
        return getBundle().getString(key);
    }

    public String getMessage(String key, Object... objects) {
        return MessageFormat.format(getBundle().getString(key), objects);
    }

    public void validate() {
        if (_nameAttribute==null)
            throw new ConnectorException(getMessage(SpmlMessages.NAME_NULL));
        if (_protocol==null)
            throw new ConnectorException(getMessage(SpmlMessages.PROTOCOL_NULL));
        if (_hostNameOrIpAddr==null)
            throw new ConnectorException(getMessage(SpmlMessages.HOST_NULL));
        if (_hostPortNumber==null)
            throw new ConnectorException(getMessage(SpmlMessages.PORT_NULL));
        if (_hostPortNumber<1 || _hostPortNumber>65535)
            throw new ConnectorException(getMessage(SpmlMessages.PORT_RANGE_ERROR, _hostPortNumber));
        if (_file==null)
            throw new ConnectorException(getMessage(SpmlMessages.FILE_NULL));
        if (_userName==null)
            throw new ConnectorException(getMessage(SpmlMessages.USERNAME_NULL));
        if (_password==null)
            throw new ConnectorException(getMessage(SpmlMessages.PASSWORD_NULL));
        if (_objectClassNames==null)
            throw new ConnectorException(getMessage(SpmlMessages.OBJECT_CLASS_NULL));
        if (_spmlClassNames==null)
            throw new ConnectorException(getMessage(SpmlMessages.SPML_CLASS_NULL));
        if (_targetNames==null)
            throw new ConnectorException(getMessage(SpmlMessages.TARGET_NULL));
        if (_objectClassNames.length!=_spmlClassNames.length || _objectClassNames.length!=_targetNames.length)
            throw new ConnectorException(getMessage(SpmlMessages.SPML_CLASS_LENGTH));
        if (_psoTarget==null)
            throw new ConnectorException(getMessage(SpmlMessages.PSO_TARGET_NULL));
        if (!Arrays.asList(_objectClassNames).contains(ObjectClass.ACCOUNT_NAME))
            throw new ConnectorException(getMessage(SpmlMessages.NO_ACCOUNT_CLASS));
            
    }

    public String getUserName() {
        return _userName;
    }

    public void setUserName(String userName) {
        _userName = userName;
    }

    @ConfigurationProperty(confidential=true)
    public GuardedString getPassword() {
        return _password;
    }

    public void setPassword(GuardedString password) {
        _password = password;
    }

    public String getHostNameOrIpAddr() {
        return _hostNameOrIpAddr;
    }

    public void setHostNameOrIpAddr(String hostNameOrIpAddr) {
        _hostNameOrIpAddr = hostNameOrIpAddr;
    }

    public Integer getHostPortNumber() {
        return _hostPortNumber;
    }

    public void setHostPortNumber(Integer hostPortNumber) {
        _hostPortNumber = hostPortNumber;
    }

    public String getProtocol() {
        return _protocol;
    }

    public void setProtocol(String protocol) {
        _protocol = protocol;
    }

    public String getFile() {
        return _file;
    }

    public void setFile(String file) {
        _file = file;
    }

    public String getPreSendCommand() {
        return _preSendCommand;
    }

    public void setPreSendCommand(String sendCommand) {
        _preSendCommand = sendCommand;
    }

    public String getPostReceiveCommand() {
        return _postReceiveCommand;
    }

    public void setPostReceiveCommand(String receiveCommand) {
        _postReceiveCommand = receiveCommand;
    }

    public String getPreDisconnectCommand() {
        return _preDisconnectCommand;
    }

    public void setPreDisconnectCommand(String loginCommand) {
        _preDisconnectCommand = loginCommand;
    }

    public String getPostConnectCommand() {
        return _postConnectCommand;
    }

    public void setPostConnectCommand(String loginCommand) {
        _postConnectCommand = loginCommand;
    }

    public String getPsoTarget() {
        return _psoTarget;
    }

    public void setPsoTarget(String target) {
        _psoTarget = target;
    }

    public String getNameAttribute() {
        return _nameAttribute;
    }

    public void setNameAttribute(String attribute) {
        _nameAttribute = attribute;
    }

    public String getMapSetNameCommand() {
        return _mapSetNameCommand;
    }

    public void setMapSetNameCommand(String setNameCommand) {
        _mapSetNameCommand = setNameCommand;
    }

    public String getMapAttributeCommand() {
        return _mapAttributeCommand;
    }

    public void setMapAttributeCommand(String attributeCommand) {
        _mapAttributeCommand = attributeCommand;
    }

    public String getMapQueryNameCommand() {
        return _mapQueryNameCommand;
    }

    public void setMapQueryNameCommand(String queryNameCommand) {
        _mapQueryNameCommand = queryNameCommand;
    }

    public String[] getObjectClassNames() {
        return arrayCopy(_objectClassNames);
    }

    public void setObjectClassNames(String[] classNames) {
        _objectClassNames = arrayCopy(classNames);
    }

    public String[] getSpmlClassNames() {
        return arrayCopy(_spmlClassNames);
    }

    public void setSpmlClassNames(String[] classNames) {
        _spmlClassNames = arrayCopy(classNames);
    }

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
