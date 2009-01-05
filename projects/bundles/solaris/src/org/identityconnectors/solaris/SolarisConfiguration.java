package org.identityconnectors.solaris;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;

public class SolarisConfiguration extends AbstractConfiguration {

    private String _userName;
    private GuardedString _password;
    private String _hostNameOrIpAddr;

    public String get_userName() {
        return _userName;
    }

    public void set_userName(String name) {
        _userName = name;
    }

    public GuardedString get_password() {
        return _password;
    }

    public void set_password(GuardedString _password) {
        this._password = _password;
    }

    public String get_hostNameOrIpAddr() {
        return _hostNameOrIpAddr;
    }

    public void set_hostNameOrIpAddr(String nameOrIpAddr) {
        _hostNameOrIpAddr = nameOrIpAddr;
    }

    public SolarisConfiguration() {
    }
    
    @Override
    public void validate() {
        // TODO Auto-generated method stub
    }
}
