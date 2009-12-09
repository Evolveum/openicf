package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;

/** Helper immutable holder of oracle jdbc connection information */
final class OracleDriverConnectionInfo {
    private final String host;
    private final String port;
    private final String driver;
    private final String database;
    private final String user;
    private final GuardedString password;
    private final String url;
    private final String protocol;
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
    String getProtocol(){
        return protocol;
    }
    private OracleDriverConnectionInfo(Builder builder){
        this.database = builder.getDatabase();
        this.driver = builder.getDriver();
        this.host = builder.getHost();
        this.password = builder.getPassword();
        this.port = builder.getPort();
        this.user = builder.getUser();
        this.url = builder.getUrl();
        this.protocol = builder.getProtocol();
    }
    
    static final class Builder{
        private String host;
        private String port;
        private String driver;
        private String database;
        private String user;
        private GuardedString password;
        private String url;
        private String protocol;
        String getHost() {
            return host;
        }
        String getProtocol() {
            return protocol;
        }
        Builder setHost(String host) {
            this.host = host;
            return this;
        }
        String getPort() {
            return port;
        }
        Builder setPort(String port) {
            this.port = port;
            return this;
        }
        String getDriver() {
            return driver;
        }
        Builder setDriver(String driver) {
            this.driver = driver;
            return this;
        }
        String getDatabase() {
            return database;
        }
        Builder setDatabase(String database) {
            this.database = database;
            return this;
        }
        String getUser() {
            return user;
        }
        Builder setUser(String user) {
            this.user = user;
            return this;
        }
        GuardedString getPassword() {
            return password;
        }
        Builder setPassword(GuardedString password) {
            this.password = password;
            return this;
        }
        String getUrl(){
            return url;
        }
        Builder setUrl(String url){
            this.url = url;
            return this;
        }
        Builder setProtocol(String protocol){
            this.protocol = protocol;
            return this;
        }
        Builder setvalues(OracleDriverConnectionInfo info){
            this.database = info.database;
            this.driver = info.driver;
            this.host = info.driver;
            this.password = info.password;
            this.port = info.port;
            this.url = info.url;
            this.user = info.user;
            this.protocol = info.protocol;
            return this;
        }
        Builder(){
        }
        OracleDriverConnectionInfo build(){
        	return new OracleDriverConnectionInfo(this);
        }
    }
    
}
