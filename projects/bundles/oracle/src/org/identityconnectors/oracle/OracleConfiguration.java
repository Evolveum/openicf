/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.*;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.oracle.OracleDriverConnectionInfo.OracleDriverConnectionInfoBuilder;

/**
 * Set of configuration properties for connecting to Oracle database
 * @author kitko
 *
 */
public class OracleConfiguration extends AbstractConfiguration implements Cloneable{
    private String host;
    private String port = OracleSpecifics.LISTENER_DEFAULT_PORT;
    private String driver;
    private String driverClassName;
    private String database;
    private String user;
    private GuardedString password;
    private String dataSource;
    private String url;
    private String[] dsJNDIEnv;
    private ConnectionType connType;
    
    /** Type of connection we will use to connect to Oracle */
    static enum ConnectionType{
        /** Connecting using datasource */
        DATASOURCE,
        /** Connecting using type 4 driver (host,port,databaseName)*/
        THIN,
        /** Connecting using type 2 driver (using TNSNAMES.ora) */
        OCI,
        /** Custom driver with custom URL */
        CUSTOM_DRIVER
    }
    
    /**
     * Default clone implementation.
     * @throws ConnectorException when super.clone fails
     */
    public OracleConfiguration clone() throws ConnectorException{
        try {
            return (OracleConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ConnectorException("Clone of OracleConfiguration super class failed",e);
        }
    }
    
    
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * @return the driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * @param driver the driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * @param database the database to set
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public GuardedString getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(GuardedString password) {
        this.password = password;
    }

    /**
     * @return the dataSource
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * @return the dsJNDIEnv
     */
    public String[] getDsJNDIEnv() {
        return dsJNDIEnv;
    }

    /**
     * @param dsJNDIEnv the dsJNDIEnv to set
     */
    public void setDsJNDIEnv(String[] dsJNDIEnv) {
        this.dsJNDIEnv = dsJNDIEnv;
    }
    
    

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 
     */
    public OracleConfiguration() {
    }

    @Override
    public void validate() {
        if(dataSource != null){
            Assertions.blankCheck(dataSource,"datasource");
            connType = ConnectionType.DATASOURCE;
            //just datasource is required
        }
        else{
            Assertions.blankCheck(driver, "driver");
            if(OracleSpecifics.THIN_DRIVER.equals(driver)){
                Assertions.blankCheck(host,"host");
                Assertions.blankCheck(port,"port");
                Assertions.blankCheck(user,"user");
                Assertions.nullCheck(password, "password");
                Assertions.blankCheck(database,"database");
                driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot load driver class : + " + driverClassName,e);
                }
                connType = ConnectionType.THIN;
            }
            else if(OracleSpecifics.OCI_DRIVER.equals(driver)){
                Assertions.blankCheck(database,"database");
                Assertions.blankCheck(user,"user");
                Assertions.nullCheck(password, "password");
                driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot load driver class : + " + driverClassName,e);
                }
                connType = ConnectionType.OCI;
            }
            else{
                //This should be custom driver class
                Class<?> driverClass = null;
                try {
                    driverClass = Class.forName(driver);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot load driver class : + " + driver,e);
                }
                if(!Driver.class.isAssignableFrom(driverClass)){
                    throw new IllegalArgumentException("Specified driver class is not java.sql.Driver");
                }
                driverClassName = driverClass.getName();
                Assertions.blankCheck(url,"url");
                Assertions.blankCheck(user,"user");
                Assertions.nullCheck(password, "password");
                connType = ConnectionType.CUSTOM_DRIVER;    
            }
        }
        
    }
    
    Connection createAdminConnection(){
        return createConnection(user,password);
    }
    
    
    Connection createConnection(String user,GuardedString password){
        validate();
        if(ConnectionType.DATASOURCE.equals(connType)){
            if(user != null){
                return OracleSpecifics.createDataSourceConnection(dataSource,user,password,JNDIUtil.arrayToHashtable(dsJNDIEnv, getConnectorMessages()));
            }
            else{
                return OracleSpecifics.createDataSourceConnection(dataSource,JNDIUtil.arrayToHashtable(dsJNDIEnv,getConnectorMessages()));
            }
        }
        else if(ConnectionType.THIN.equals(connType)){
            return OracleSpecifics.createThinDriverConnection(new OracleDriverConnectionInfoBuilder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build()
                    );
        }
        else if(ConnectionType.OCI.equals(connType)){
            return OracleSpecifics.createOciDriverConnection(new OracleDriverConnectionInfoBuilder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build()
                    );
        }
        else if(ConnectionType.CUSTOM_DRIVER.equals(connType)){
            return OracleSpecifics.createDriverConnection(new OracleDriverConnectionInfoBuilder().
                    setUrl(url).setDriver(driverClassName).setUser(user).setPassword(password).build()
            );
        }
        throw new IllegalStateException("Invalid state of OracleConfiguration");
    }
    
    

}
