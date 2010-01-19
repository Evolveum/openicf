package org.identityconnectors.racf;

import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.rw3270.RW3270Configuration;

/**
 * Configuration proxy 
 * Prefer to use external configuration proxy class, when there will be needed more configurations.S
 */
public class RW3270ConfigurationProxy implements RW3270Configuration {
    private RacfConfiguration _config;
    private int _index;

    public RW3270ConfigurationProxy(RacfConfiguration config, int index) {
        _config = config;
        _index = index;
    }

    public Script getConnectScript() {
        return _config.getConnectScript();
    }

    public String getConnectionClassName() {
        return _config.getConnectionClassName();
    }

    public String[] getConnectionProperties() {
        return _config.getConnectionProperties();
    }

    public Script getDisconnectScript() {
        return _config.getDisconnectScript();
    }

    public String getHostNameOrIpAddr() {
        return _config.getHostNameOrIpAddr();
    }

    public Integer getHostTelnetPortNumber() {
        return _config.getHostTelnetPortNumber();
    }

    public GuardedString getPassword() {
        return _config.getPasswords()[_index];
    }

    public String getUserName() {
        return _config.getUserNames()[_index];
    }

    public void setConnectScript(Script script) {
        _config.setConnectScript(script);
    }

    public void setConnectionClassName(String className) {
        _config.setConnectionClassName(className);
    }

    public void setConnectionProperties(String[] properties) {
        _config.setConnectionProperties(properties);
    }

    public void setDisconnectScript(Script script) {
        _config.setDisconnectScript(script);
    }

    public void setHostNameOrIpAddr(String nameOrIpAddr) {
        _config.setHostNameOrIpAddr(nameOrIpAddr);
    }

    public void setHostTelnetPortNumber(Integer port) {
        _config.setHostTelnetPortNumber(port);
    }

    public void setPassword(GuardedString password) {
        GuardedString[] passwords = _config.getPasswords();
        passwords[_index] = password;
        _config.setPasswords(passwords);
    }

    public void setUserName(String name) {
        String[] userNames = _config.getUserNames();
        userNames[_index] = name;
        _config.setUserNames(userNames);
    }

    public ConnectorMessages getConnectorMessages() {
        return _config.getConnectorMessages();
    }

    public void setConnectorMessages(ConnectorMessages messages) {
        _config.setConnectorMessages(messages);
    }

    public void validate() {
        _config.validate();
    }
}