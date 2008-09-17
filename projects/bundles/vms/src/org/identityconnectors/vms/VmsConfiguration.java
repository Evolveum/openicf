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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


public class VmsConfiguration extends AbstractConfiguration {
    private String         _userName;
    private GuardedString  _password;

    private String         _hostNameOrIpAddr;
    private String         _hostLineTerminator;
    private String         _hostShellPrompt;
    private Integer        _hostPortNumber;
    private String         _connectScript;
    private Boolean        _isSSH;

    private ResourceBundle _bundle = null; 
    private Locale         _lastLocale = null; 

    private String         _localHostShellPrompt = "BOOMBOOM";
    
    private static final String CATALOG = "org.identityconnectors.vms.VmsMessages";

    public void validate() {
    	if (isNull(_hostLineTerminator))
            throw new IllegalArgumentException(getMessage(VmsMessages.TERMINATOR_NULL));
        if (_isSSH)
            throw new IllegalArgumentException(getMessage(VmsMessages.SSH_NULL));
        if (isNull(_hostShellPrompt))
            throw new IllegalArgumentException(getMessage(VmsMessages.SHELL_PROMPT_NULL));
        if (isNull(_connectScript))
            throw new IllegalArgumentException(getMessage(VmsMessages.CONN_SCRIPT_NULL));
        if (isNull(_hostNameOrIpAddr))
            throw new IllegalArgumentException(getMessage(VmsMessages.HOST_NULL));
        if (_hostPortNumber==null)
            throw new IllegalArgumentException(getMessage(VmsMessages.PORT_NULL));
        if (_hostPortNumber<1 || _hostPortNumber>65535)
            throw new IllegalArgumentException(getMessage(VmsMessages.PORT_RANGE_ERROR, _hostPortNumber));
        if (isNull(_userName))
            throw new IllegalArgumentException(getMessage(VmsMessages.USERNAME_NULL));
        if (isNull(_password))
            throw new IllegalArgumentException(getMessage(VmsMessages.PASSWORD_NULL));
    }
    
    private boolean isNull(String string) {
    	return string==null || string.length()==0;
    }

    private static GuardedString _nullGuardedString = new GuardedString(new char[0]);
    private boolean isNull(GuardedString string) {
    	return string==null || string.equals(_nullGuardedString);
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

    public String getHostLineTerminator() {
    	if (_hostLineTerminator!=null)
            return _hostLineTerminator.replaceAll("\n", "\\n").replaceAll("\r", "\\r");
        else
            return null;
    }

    public String getRealHostLineTerminator() {
    	return _hostLineTerminator;
    }

    public void setHostLineTerminator(String hostLineTerminator) {
        if (hostLineTerminator!=null)
            _hostLineTerminator = hostLineTerminator.replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r");
        else
            _hostLineTerminator = null;
    }

    public String getHostShellPrompt() {
    	return _hostShellPrompt;
    }

    String getLocalHostShellPrompt() {
    	return _localHostShellPrompt;
    }

    public void setHostShellPrompt(String hostShellPrompt) {
        _hostShellPrompt = hostShellPrompt;
    }

    public Integer getHostPortNumber() {
        return _hostPortNumber;
    }

    public void setHostPortNumber(Integer hostPortNumber) {
        _hostPortNumber = hostPortNumber;
    }

    public String getConnectScript() {
        return _connectScript;
    }

    public void setConnectScript(String connectScript) {
        _connectScript = connectScript;
    }

    @ConfigurationProperty(displayMessageKey="SSH.display", helpMessageKey="SSH.help")
    public Boolean getSSH() {
        return _isSSH;
    }

    public void setSSH(Boolean isSSH) {
        _isSSH = isSSH;
    }
}
