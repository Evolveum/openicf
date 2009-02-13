package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;

/** Helper immutable holder of oracle connection information */
class OracleDriverConnectionInfo {
    private String host;
    private String port;
    private String driver;
    private String database;
    private String user;
    private GuardedString password;
    private String url;
    String getHost() {
        return host;
    }
    String getPort() {
        return port;
    }
    String getDriver() {
        return driver;
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
    String getUrl(){
        return url;
    }
    private OracleDriverConnectionInfo(){}
    
    static class OracleDriverConnectionInfoBuilder{
        private String host;
        private String port;
        private String driver;
        private String database;
        private String user;
        private GuardedString password;
        private String url;
        String getHost() {
            return host;
        }
        OracleDriverConnectionInfoBuilder setHost(String host) {
            this.host = host;
            return this;
        }
        String getPort() {
            return port;
        }
        OracleDriverConnectionInfoBuilder setPort(String port) {
            this.port = port;
            return this;
        }
        String getDriver() {
            return driver;
        }
        OracleDriverConnectionInfoBuilder setDriver(String driver) {
            this.driver = driver;
            return this;
        }
        String getDatabase() {
            return database;
        }
        OracleDriverConnectionInfoBuilder setDatabase(String database) {
            this.database = database;
            return this;
        }
        String getUser() {
            return user;
        }
        OracleDriverConnectionInfoBuilder setUser(String user) {
            this.user = user;
            return this;
        }
        GuardedString getPassword() {
            return password;
        }
        OracleDriverConnectionInfoBuilder setPassword(GuardedString password) {
            this.password = password;
            return this;
        }
        String getUrl(){
            return url;
        }
        OracleDriverConnectionInfoBuilder setUrl(String url){
            this.url = url;
            return this;
        }
        OracleDriverConnectionInfoBuilder(){
        }
        OracleDriverConnectionInfo build(){
            OracleDriverConnectionInfo info = new OracleDriverConnectionInfo();
            info.database = database;
            info.driver = driver;
            info.host = host;
            info.password = password;
            info.port = port;
            info.user = user;
            info.url = url;
            return info;
        }
        
    }
    
}
