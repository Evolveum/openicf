package org.identityconnectors.oracle;

import java.util.Arrays;

import org.identityconnectors.common.security.GuardedString;

/** Helper immutable holder of oracle connection information */
final class OracleDriverConnectionInfo {
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
        OracleDriverConnectionInfoBuilder setPassword(GuardedString password) {
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
