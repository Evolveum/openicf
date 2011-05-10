package org.identityconnectors.db2;

import org.identityconnectors.common.security.GuardedString;

class Type2ConnectionInfo {
    private String driver;
    private String aliasName;
    private String subprotocol;
    private String user;
    private GuardedString password;
    private Type2ConnectionInfo(){}
    String getDriver() {
        return driver;
    }
    String getAliasName() {
        return aliasName;
    }
    String getSubprotocol() {
        return subprotocol;
    }
    String getUser() {
        return user;
    }
    GuardedString getPassword() {
        return password;
    }
    
    static class Type2ConnectionInfoBuilder{
        private String driver;
        private String aliasName;
        private String subprotocol;
        private String user;
        private GuardedString password;
        String getDriver() {
            return driver;
        }
        Type2ConnectionInfoBuilder setDriver(String driver) {
            this.driver = driver;
            return this;
        }
        String getAliasName() {
            return aliasName;
        }
        Type2ConnectionInfoBuilder setAliasName(String aliasName) {
            this.aliasName = aliasName;
            return this;
        }
        String getSubprotocol() {
            return subprotocol;
        }
        Type2ConnectionInfoBuilder setSubprotocol(String subprotocol) {
            this.subprotocol = subprotocol;
            return this;
        }
        String getUser() {
            return user;
        }
        Type2ConnectionInfoBuilder setUser(String user) {
            this.user = user;
            return this;
        }
        GuardedString getPassword() {
            return password;
        }
        Type2ConnectionInfoBuilder setPassword(GuardedString password) {
            this.password = password;
            return this;
        }
        
        Type2ConnectionInfo build(){
            Type2ConnectionInfo info = new Type2ConnectionInfo();
            info.aliasName = aliasName;
            info.driver = driver;
            info.password = password;
            info.subprotocol = subprotocol;
            info.user = user;
            return info;
        }
        
    }
}
