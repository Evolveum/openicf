/**
 * 
 */
package org.identityconnectors.oracle;


import java.sql.Connection;
import java.sql.SQLException;

import static org.identityconnectors.oracle.OracleMessages.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.oracle.OracleDriverConnectionInfo.Builder;

/**
 * Set of configuration properties for connecting to Oracle database
 * @author kitko
 *
 */
public final class OracleConfiguration extends AbstractConfiguration implements Cloneable{
    private String host;
    private String port;
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
        cs = new OracleCaseSensitivityBuilder(getConnectorMessages()).build();
        caseSensitivityString = "default";
        port = OracleSpecifics.LISTENER_DEFAULT_PORT;
        driver = OracleSpecifics.THIN_DRIVER;
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
        FULL_URL
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
     * @return the dataSource
     */
    @ConfigurationProperty(order = 0,displayMessageKey=MSG_DATASOURCE_DISPLAY,helpMessageKey=MSG_DATASOURCE_HELP)
    public String getDataSource() {
        return dataSource;
    }

    /**
     * @return the dsJNDIEnv
     */
    @ConfigurationProperty(order = 1,displayMessageKey=MSG_DSJNDIENV_DISPLAY,helpMessageKey=MSG_DSJNDIENV_HELP)
    public String[] getDsJNDIEnv() {
		if(dsJNDIEnv == null){
			return new String[0];
		}
		String[] res = new String[dsJNDIEnv.length];
		System.arraycopy(dsJNDIEnv,0,res,0,dsJNDIEnv.length);
		return res;
    }

    /**
     * @return the url
     */
    @ConfigurationProperty(order = 2,displayMessageKey=MSG_URL_DISPLAY,helpMessageKey=MSG_URL_HELP)
    public String getUrl() {
        return url;
    }

    /**
     * @return the driver
     */
    @ConfigurationProperty(order = 3,displayMessageKey=MSG_DRIVER_DISPLAY,helpMessageKey=MSG_DRIVER_HELP)
    public String getDriver() {
        return driver;
    }
    
    
    /**
     * @return the host
     */
    @ConfigurationProperty(order = 4,displayMessageKey=MSG_HOST_DISPLAY,helpMessageKey=MSG_HOST_HELP)
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    @ConfigurationProperty(order = 5,displayMessageKey=MSG_PORT_DISPLAY,helpMessageKey=MSG_PORT_HELP)
    public String getPort() {
        return port;
    }

    /**
     * @return the database
     */
    @ConfigurationProperty(order = 6,displayMessageKey=MSG_DATABASE_DISPLAY,helpMessageKey=MSG_DATABASE_HELP)
    public String getDatabase() {
        return database;
    }
    
    /**
     * @return the user
     */
    @ConfigurationProperty(order = 7,displayMessageKey=MSG_USER_DISPLAY,helpMessageKey=MSG_USER_HELP)
    public String getUser() {
        return user;
    }
    
    /**
     * @return the password
     */
    @ConfigurationProperty(order = 8,displayMessageKey=MSG_PASSWORD_DISPLAY,helpMessageKey=MSG_PASSWORD_HELP,confidential=true)
    public GuardedString getPassword() {
        return password;
    }
    
    /**
     * @return caseSensitivityString
     */
    @ConfigurationProperty(order = 9,displayMessageKey=MSG_CS_DISPLAY,helpMessageKey=MSG_CS_HELP,required=true)
    public String getCaseSensitivity(){
        return caseSensitivityString;
    }
    
    
    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }


    /**
     * @param driver the driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }


    /**
     * @param database the database to set
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    
    String getUserOwner(){
    	//if we were logged as system, owner will be SYSTEM
    	if("".equals(cs.getAttributeFormatterAndNormalizer(OracleUserAttributeCS.SYSTEM_USER).getQuatesChar())){
    		return user.toUpperCase();
    	}
    	return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(GuardedString password) {
    	//We cannot have empty password for oracle, so simplify test to set to null when empty
    	this.password = password;
    	if(this.password != null){
    		this.password.access(new GuardedString.Accessor(){
				public void access(char[] clearChars) {
					if(clearChars.length == 0){
						OracleConfiguration.this.password = null;
					}
				}
    		});
    	}
    }


    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * @param dsJNDIEnv the dsJNDIEnv to set
     */
    public void setDsJNDIEnv(String[] dsJNDIEnv) {
		if(dsJNDIEnv == null){
			this.dsJNDIEnv = null;
		}
		else{
			this.dsJNDIEnv = new String[dsJNDIEnv.length];
			System.arraycopy(dsJNDIEnv,0,this.dsJNDIEnv,0,dsJNDIEnv.length);
		}
    }
    
    


    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    
    /** Sets case sensitivity from string map 
     * @param cs */
    public void setCaseSensitivity(String cs){
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
    	LocalizedAssert la = new LocalizedAssert(getConnectorMessages(),true);
        la.assertNotBlank(caseSensitivityString, "oracle.cs.display");
        this.cs = new OracleCaseSensitivityBuilder(getConnectorMessages()).parseMap(caseSensitivityString).build();
        if(StringUtil.isNotBlank(dataSource)){
			la.assertBlank(host, MSG_HOST_DISPLAY);
			la.assertBlank(database,MSG_DATABASE_DISPLAY);
			la.assertBlank(driver,MSG_DRIVER_DISPLAY);
			la.assertBlank(port,MSG_PORT_DISPLAY);
			//If user is not blank, then also password must not be blank
			//Most of datasource configuration will not allow to pass user and password when retrieving connection from ds,
			//But for some configuration it is valid to specify user/password and override configuration at application server level
			if((StringUtil.isNotBlank(user) && password == null) || (StringUtil.isBlank(user) && password != null)){
				throw new IllegalArgumentException(getConnectorMessages().format(MSG_USER_AND_PASSWORD_MUST_BE_SET_BOTH_OR_NONE, null));
			}
            connType = ConnectionType.DATASOURCE;
        }
        else{
        	la.assertNotBlank(driver, MSG_DRIVER_DISPLAY);
            if(StringUtil.isNotBlank(url)){
                la.assertNotBlank(user,MSG_USER_DISPLAY);
                la.assertNotNull(password, MSG_PASSWORD_DISPLAY);
    			la.assertBlank(host, "MSG_HOST_DISPLAY");
    			la.assertBlank(database,MSG_DATABASE_DISPLAY);
    			la.assertBlank(port,MSG_PORT_DISPLAY);
                if(OracleSpecifics.THIN_DRIVER.equals(driver)){
                    driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                }
                else if(OracleSpecifics.OCI_DRIVER.equals(driver)){
                    driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                }
                else{
                	driverClassName = driver;
                }
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(getConnectorMessages().format(MSG_CANNOT_LOAD_DRIVER, null, driverClassName) ,e);
                }
                connType = ConnectionType.FULL_URL;
            }
            else if(OracleSpecifics.THIN_DRIVER.equals(driver) || OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME.equals(driver)){
            	la.assertNotBlank(host, MSG_HOST_DISPLAY);
            	la.assertNotBlank(port, MSG_PORT_DISPLAY);
            	la.assertNotBlank(user, MSG_USER_DISPLAY);
            	la.assertNotNull(password, MSG_PASSWORD_DISPLAY);
            	la.assertNotBlank(database, MSG_DATABASE_DISPLAY);
                driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                	throw new IllegalArgumentException(getConnectorMessages().format(MSG_CANNOT_LOAD_DRIVER, null, driverClassName) ,e);
                }
                connType = ConnectionType.THIN;
            }
            else if(OracleSpecifics.OCI_DRIVER.equals(driver)){
            	la.assertNotBlank(user, MSG_USER_DISPLAY);
            	la.assertNotNull(password, MSG_PASSWORD_DISPLAY);
            	la.assertNotBlank(database, MSG_DATABASE_DISPLAY);
                driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                	throw new IllegalArgumentException(getConnectorMessages().format(MSG_CANNOT_LOAD_DRIVER, null, driverClassName) ,e);
                }
                connType = ConnectionType.OCI;
            }
            else{
            	throw new IllegalArgumentException(getConnectorMessages().format(MSG_SET_DRIVER_OR_URL,null));
            }
        }
        
    }
    
    Connection createUserConnection(String user, GuardedString password){
    	user = cs.formatToken(OracleUserAttributeCS.USER, user);
    	password = cs.formatToken(OracleUserAttributeCS.PASSWORD, password);
    	return createConnection(user,password);
    }
    
    Connection createAdminConnection(){
    	String user = cs.normalizeAndFormatToken(OracleUserAttributeCS.SYSTEM_USER, this.user);
    	GuardedString password = cs.normalizeAndFormatToken(OracleUserAttributeCS.SYSTEM_PASSWORD, this.password);
        return createConnection(user,password);
    }
    
    
    private Connection createConnection(String user,GuardedString password){
        validate();
        Connection connection = null;
        boolean disableAutoCommit = true;
        if(ConnectionType.DATASOURCE.equals(connType)){
        	disableAutoCommit = false;
            if(StringUtil.isNotBlank(user)){
            	//This could fail, but we cannot invoke method without user/password if user and password were specified
            	connection = OracleSpecifics.createDataSourceConnection(dataSource,user,password,JNDIUtil.arrayToHashtable(dsJNDIEnv, getConnectorMessages()), getConnectorMessages());
            }
            else{
            	connection =  OracleSpecifics.createDataSourceConnection(dataSource,JNDIUtil.arrayToHashtable(dsJNDIEnv,getConnectorMessages()), getConnectorMessages());
            }
        }
        else if(ConnectionType.THIN.equals(connType)){
        	connection =  OracleSpecifics.createThinDriverConnection(new Builder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build(), getConnectorMessages()
                    );
        }
        else if(ConnectionType.OCI.equals(connType)){
        	connection =  OracleSpecifics.createOciDriverConnection(new Builder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build(), getConnectorMessages()
                    );
        }
        else if(ConnectionType.FULL_URL.equals(connType)){
        	connection =  OracleSpecifics.createCustomDriverConnection(new Builder().
                    setUrl(url).setDriver(driverClassName).setUser(user).setPassword(password).build(), getConnectorMessages()
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
