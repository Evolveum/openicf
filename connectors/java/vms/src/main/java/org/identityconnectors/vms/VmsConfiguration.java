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
package org.identityconnectors.vms;

import java.util.Locale;
import java.util.TimeZone;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class VmsConfiguration extends AbstractConfiguration {
    private String userName;
    private GuardedString password;

    private String hostNameOrIpAddr;
    private String hostLineTerminator;
    private String hostShellPrompt;
    private Integer hostPortNumber;
    private String vmsLocale;
    private String vmsDateFormatWithSecs;
    private String vmsDateFormatWithoutSecs;
    private String vmsTimeZone;
    private Boolean isSSH;
    private Boolean supportsLongCommands;
    private Boolean disableUserLogins;

    private String localHostShellPrompt = "BOOMBOOM";

    public VmsConfiguration() {
        vmsLocale = Locale.getDefault().toString();
        vmsTimeZone = TimeZone.getDefault().getID();
        vmsDateFormatWithoutSecs = "dd-MMM-yyyy HH:mm";
        vmsDateFormatWithSecs = "dd-MMM-yyyy HH:mm:ss";
        hostShellPrompt = "[$] ";
        disableUserLogins = Boolean.TRUE;
    }

    public VmsConfiguration(VmsConfiguration other) {
        userName = other.userName;
        password = other.password;

        hostNameOrIpAddr = other.hostNameOrIpAddr;
        hostLineTerminator = other.hostLineTerminator;
        hostShellPrompt = other.hostShellPrompt;
        hostPortNumber = other.hostPortNumber;
        vmsLocale = other.vmsLocale;
        vmsDateFormatWithoutSecs = other.vmsDateFormatWithoutSecs;
        vmsDateFormatWithSecs = other.vmsDateFormatWithSecs;
        vmsTimeZone = other.vmsTimeZone;
        isSSH = other.isSSH;
        supportsLongCommands = other.supportsLongCommands;
        disableUserLogins = other.disableUserLogins;

        setConnectorMessages(other.getConnectorMessages());
    }

    public void validate() {
        if (isNull(vmsLocale)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.LOCALE_NULL));
        }
        if (isNull(vmsDateFormatWithSecs)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.DATEFORMAT1_NULL));
        }
        if (isNull(vmsTimeZone)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.TIMEZONE_NULL));
        }
        if (isNull(vmsDateFormatWithoutSecs)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.DATEFORMAT2_NULL));
        }
        if (isNull(hostLineTerminator)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.TERMINATOR_NULL));
        }
        if (isSSH == null) {
            throw new IllegalArgumentException(getMessage(VmsMessages.SSH_NULL));
        }
        if (supportsLongCommands == null) {
            throw new IllegalArgumentException(getMessage(VmsMessages.LONG_COMMANDS_NULL));
        }
        if (isNull(hostShellPrompt)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.SHELL_PROMPT_NULL));
        }
        if (isNull(hostNameOrIpAddr)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.HOST_NULL));
        }
        if (hostPortNumber == null) {
            throw new IllegalArgumentException(getMessage(VmsMessages.PORT_NULL));
        }
        if (hostPortNumber < 1 || hostPortNumber > 65535) {
            throw new IllegalArgumentException(getMessage(VmsMessages.PORT_RANGE_ERROR,
                    hostPortNumber));
        }
        if (isNull(userName)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.USERNAME_NULL));
        }
        if (isNull(password)) {
            throw new IllegalArgumentException(getMessage(VmsMessages.PASSWORD_NULL));
        }

    }

    private boolean isNull(String string) {
        return string == null || string.length() == 0;
    }

    private static GuardedString nullGuardedString = new GuardedString(new char[0]);

    private boolean isNull(GuardedString string) {
        return string == null || string.equals(nullGuardedString);
    }

    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }

    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }

    @ConfigurationProperty(order = 1, required = true)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @ConfigurationProperty(confidential = true, order = 2, required = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 3, required = true)
    public String getHostNameOrIpAddr() {
        return hostNameOrIpAddr;
    }

    public void setHostNameOrIpAddr(String hostNameOrIpAddr) {
        this.hostNameOrIpAddr = hostNameOrIpAddr;
    }

    @ConfigurationProperty(order = 4, required = true)
    public Integer getHostPortNumber() {
        return hostPortNumber;
    }

    public void setHostPortNumber(Integer hostPortNumber) {
        this.hostPortNumber = hostPortNumber;
    }

    @ConfigurationProperty(displayMessageKey = "SSH.display", helpMessageKey = "SSH.help",
            order = 5, required = true)
    public Boolean getSSH() {
        return isSSH;
    }

    public void setSSH(Boolean isSSH) {
        this.isSSH = isSSH;
    }

    @ConfigurationProperty(order = 6, required = true)
    public String getHostLineTerminator() {
        if (hostLineTerminator != null) {
            return hostLineTerminator.replaceAll("\n", "\\n").replaceAll("\r", "\\r");
        } else {
            return null;
        }
    }

    public String getRealHostLineTerminator() {
        return hostLineTerminator;
    }

    public void setHostLineTerminator(String hostLineTerminator) {
        if (hostLineTerminator != null) {
            this.hostLineTerminator =
                    hostLineTerminator.replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r");
        } else {
            this.hostLineTerminator = null;
        }
    }

    @ConfigurationProperty(order = 7, required = true)
    public String getHostShellPrompt() {
        return hostShellPrompt;
    }

    String getLocalHostShellPrompt() {
        return localHostShellPrompt;
    }

    public void setHostShellPrompt(String hostShellPrompt) {
        this.hostShellPrompt = hostShellPrompt;
    }

    @ConfigurationProperty(order = 10, required = true)
    public String getVmsLocale() {
        return vmsLocale;
    }

    public void setVmsLocale(String vmsLocale) {
        this.vmsLocale = vmsLocale;
    }

    @ConfigurationProperty(order = 11, required = true)
    public String getVmsTimeZone() {
        return vmsTimeZone;
    }

    public void setVmsTimeZone(String vmsTimeZone) {
        this.vmsTimeZone = vmsTimeZone;
    }

    @ConfigurationProperty(order = 12, required = true)
    public String getVmsDateFormatWithSecs() {
        return vmsDateFormatWithSecs;
    }

    public void setVmsDateFormatWithSecs(String vmsDateFormatWithSecs) {
        this.vmsDateFormatWithSecs = vmsDateFormatWithSecs;
    }

    @ConfigurationProperty(order = 13, required = true)
    public String getVmsDateFormatWithoutSecs() {
        return vmsDateFormatWithoutSecs;
    }

    public void setVmsDateFormatWithoutSecs(String vmsDateFormatWithoutSecs) {
        this.vmsDateFormatWithoutSecs = vmsDateFormatWithoutSecs;
    }

    @ConfigurationProperty(order = 14)
    public Boolean getDisableUserLogins() {
        if (disableUserLogins == null) {
            return Boolean.TRUE;
        }
        return disableUserLogins;
    }

    public void setDisableUserLogins(Boolean disableUserLogins) {
        this.disableUserLogins = disableUserLogins;
    }

    @ConfigurationProperty(displayMessageKey = "longCommands.display",
            helpMessageKey = "longCommands.help", order = 15, required = true)
    public Boolean getLongCommands() {
        return supportsLongCommands;
    }

    public void setLongCommands(Boolean supportsLongCommands) {
        this.supportsLongCommands = supportsLongCommands;
    }
}
