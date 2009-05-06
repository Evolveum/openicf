/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.*;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.oracle.OracleDriverConnectionInfo.Builder;
import static org.identityconnectors.oracle.OracleMessages.*;

/**
 * Set of configuration properties for connecting to Oracle database
 * @author kitko
 *
 */
public final class OracleConfiguration extends AbstractConfiguration implements Cloneable{
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
    private OracleCaseSensitivitySetup cs;
    private String caseSensitivityString;
    
    /**
     * Creates configuration
     */
    public OracleConfiguration() {
        //Set casesensitivity setup to default one
        cs = new OracleCaseSensitivityBuilder().build();
        caseSensitivityString = "default";
    }
    
    
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
    @ConfigurationProperty(order = 0,displayMessageKey=HOST_DISPLAY,helpMessageKey=HOST_HELP)
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
    @ConfigurationProperty(order = 1,displayMessageKey=PORT_DISPLAY,helpMessageKey=PORT_HELP)
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
    @ConfigurationProperty(order = 2,displayMessageKey=DRIVER_DISPLAY,helpMessageKey=DRIVER_HELP)
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
    @ConfigurationProperty(order = 3,displayMessageKey=DATABASE_DISPLAY,helpMessageKey=DATABASE_HELP)
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
    @ConfigurationProperty(order = 4,displayMessageKey=USER_DISPLAY,helpMessageKey=USER_HELP)
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
    @ConfigurationProperty(order = 5,displayMessageKey=PASSWORD_DISPLAY,helpMessageKey=PASSWORD_HELP)
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
    @ConfigurationProperty(order = 6,displayMessageKey=DATASOURCE_DISPLAY,helpMessageKey=DATASOURCE_HELP)
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
    @ConfigurationProperty(order = 6,displayMessageKey=DSJNDIENV_DISPLAY,helpMessageKey=DSJNDIENV_HELP)
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
    @ConfigurationProperty(order = 7,displayMessageKey=URL_DISPLAY,helpMessageKey=URL_HELP)
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
     * @return caseSensitivityString
     */
    @ConfigurationProperty(order = 8,displayMessageKey=CS_DISPLAY,helpMessageKey=CS_HELP)
    public String getCaseSensitivity(){
        return caseSensitivityString;
    }
    
    /** Sets case sensitivity from string map 
     * @param cs */
    public void setCaseSensitivity(String cs){
        new LocalizedAssert(getConnectorMessages()).assertNotBlank(cs, "cs");
        this.cs = new OracleCaseSensitivityBuilder().parseMap(cs).build();
        this.caseSensitivityString = cs;
    }
    
    OracleCaseSensitivitySetup getCSSetup(){
        return cs;
    }
    
    void setCSSetup(OracleCaseSensitivitySetup cs){
        this.cs = new LocalizedAssert(getConnectorMessages()).assertNotNull(cs, "cs");
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
        Connection connection = null;
        boolean disableAutoCommit = true;
        if(ConnectionType.DATASOURCE.equals(connType)){
        	disableAutoCommit = false;
            if(user != null){
            	connection = OracleSpecifics.createDataSourceConnection(dataSource,user,password,JNDIUtil.arrayToHashtable(dsJNDIEnv, getConnectorMessages()));
            }
            else{
            	connection =  OracleSpecifics.createDataSourceConnection(dataSource,JNDIUtil.arrayToHashtable(dsJNDIEnv,getConnectorMessages()));
            }
        }
        else if(ConnectionType.THIN.equals(connType)){
        	connection =  OracleSpecifics.createThinDriverConnection(new Builder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build()
                    );
        }
        else if(ConnectionType.OCI.equals(connType)){
        	connection =  OracleSpecifics.createOciDriverConnection(new Builder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build()
                    );
        }
        else if(ConnectionType.CUSTOM_DRIVER.equals(connType)){
        	connection =  OracleSpecifics.createCustomDriverConnection(new Builder().
                    setUrl(url).setDriver(driverClassName).setUser(user).setPassword(password).build()
            );
        }
        else{
        	throw new IllegalStateException("Invalid state of OracleConfiguration");
        }
        if(disableAutoCommit){
        	try {
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				throw new ConnectorException("Cannot switch off autocommit",e);
			}
        }
        try {
			connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		} catch (SQLException e) {
			throw new ConnectorException("Cannot set transaction isloation",e);
		}
        return connection;
    }
    

}
