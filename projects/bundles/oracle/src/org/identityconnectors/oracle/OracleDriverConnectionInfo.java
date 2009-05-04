package org.identityconnectors.oracle;

import java.util.Arrays;

import org.identityconnectors.common.security.GuardedString;

/** Helper immutable holder of oracle connection information */
final class OracleDriverConnectionInfo {
    private final String host;
    private final String port;
    private final String driver;
    private final String database;
    private final String user;
    private final GuardedString password;
    private final String url;
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
    private OracleDriverConnectionInfo(Builder builder){
        this.database = builder.getDatabase();
        this.driver = builder.getDriver();
        this.host = builder.getHost();
        this.password = builder.getPassword();
        this.port = builder.getPort();
        this.user = builder.getUser();
        this.url = builder.getUrl();
    }
    
    static class Builder{
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
            if(user != null){
                if(!user.startsWith("\"")){
                    user = "\"" + user + "\"";
                }
            }
            this.user = user;
            return this;
        }
        GuardedString getPassword() {
            return password;
        }
        Builder setPassword(GuardedString password) {
            if(password != null){
                final GuardedString[] newPa = new GuardedString[1];
                password.access(new GuardedString.Accessor(){
                    public void access(char[] clearChars) {
                        if(clearChars.length > 0){
                            if(clearChars[0] != '"'){
                                char[] newChars = new char[clearChars.length + 2];
                                newChars[0] = '"';
                                System.arraycopy(clearChars, 0, newChars, 1, clearChars.length);
                                newChars[clearChars.length + 1] = '"';
                                newPa[0] = new GuardedString(newChars);
                                Arrays.fill(newChars, (char)0);
                            }
                        }
                    }
                });
                if(newPa[0] != null){
                    password = newPa[0];
                }
            }
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
        Builder(){
        }
        OracleDriverConnectionInfo build(){
        	return new OracleDriverConnectionInfo(this);
        }
        
    }
    
}
