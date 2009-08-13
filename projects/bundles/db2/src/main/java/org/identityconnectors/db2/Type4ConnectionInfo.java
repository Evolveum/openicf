package org.identityconnectors.db2;

import org.identityconnectors.common.security.GuardedString;

class Type4ConnectionInfo {
    private String driver;
    private String host;
    private String port;
    private String subprotocol;
    private String database;
    private String user;
    private GuardedString password;
    
    private Type4ConnectionInfo(){}
    
    String getDriver() {
        return driver;
    }
    String getHost() {
        return host;
    }
    String getPort() {
        return port;
    }
    String getSubprotocol() {
        return subprotocol;
    }
    String getDatabase() {
        return database;
    }
    String getUser() {
        return user;
    }
    GuardedString getPassword() {
        return password;
    }
    
    static class Type4ConnectionInfoBuilder{
        private String driver;
        private String host;
        private String port;
        private String subprotocol;
        private String database;
        private String user;
        private GuardedString password;
        String getDriver() {
            return driver;
        }
        Type4ConnectionInfoBuilder setDriver(String driver) {
            this.driver = driver;
            return this;
        }
        String getHost() {
            return host;
        }
        Type4ConnectionInfoBuilder setHost(String host) {
            this.host = host;
            return this;
        }
        String getPort() {
            return port;
        }
        Type4ConnectionInfoBuilder setPort(String port) {
            this.port = port;
            return this;
        }
        String getSubprotocol() {
            return subprotocol;
        }
        Type4ConnectionInfoBuilder setSubprotocol(String subprotocol) {
            this.subprotocol = subprotocol;
            return this;
        }
        String getDatabase() {
            return database;
        }
        Type4ConnectionInfoBuilder setDatabase(String database) {
            this.database = database;
            return this;
        }
        String getUser() {
            return user;
        }
        Type4ConnectionInfoBuilder setUser(String user) {
            this.user = user;
            return this;
        }
        GuardedString getPassword() {
            return password;
        }
        Type4ConnectionInfoBuilder setPassword(GuardedString password) {
            this.password = password;
            return this;
        }
        Type4ConnectionInfo build(){
            Type4ConnectionInfo info = new Type4ConnectionInfo();
            info.database = database;
            info.driver = driver;
            info.host = host;
            info.password = password;
            info.port = port;
            info.subprotocol = subprotocol;
            info.user = user;
            return info;
        }
    }
    
}
