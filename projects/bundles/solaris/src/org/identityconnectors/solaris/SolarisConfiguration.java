package org.identityconnectors.solaris;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;

public class SolarisConfiguration extends AbstractConfiguration {

    private String userName;
    private GuardedString password;
    private String hostNameOrIpAddr;
    private String port;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        userName = name;
    }

    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString _password) {
        this.password = _password;
    }

    public String getHostNameOrIpAddr() {
        return hostNameOrIpAddr;
    }

    public void setHostNameOrIpAddr(String nameOrIpAddr) {
        hostNameOrIpAddr = nameOrIpAddr;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String _port) {
        this.port = _port;
    }

    public SolarisConfiguration() {
    }

    @Override
    public void validate() {
        // TODO Auto-generated method stub
    }
}
