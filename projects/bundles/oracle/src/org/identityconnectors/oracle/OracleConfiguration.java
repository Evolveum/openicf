/**
 * 
 */
package org.identityconnectors.oracle;


import java.sql.Connection;
import java.sql.SQLException;

import static org.identityconnectors.oracle.OracleMessages.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
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
    //Source type is user defined ConnectionType
    private String sourceType;
    private boolean ignoreCreateExtraOperAttrs;
    private static final Log log = Log.getLog(OracleConfiguration.class);
    /**
     * Creates configuration
     */
    public OracleConfiguration() {
        cs = new OracleCaseSensitivityBuilder(getConnectorMessages()).build();
        caseSensitivityString = "default";
        port = OracleSpecifics.LISTENER_DEFAULT_PORT;
    }
    
    /** Type of connection we will use to connect to Oracle */
    static enum ConnectionType{
        /** Connecting using datasource */
        DATASOURCE("DataSource"),
        /** Connecting using type 4 driver (host,port,databaseName)*/
        THIN("Thin Driver"),
        /** Connecting using type 2 driver (using TNSNAMES.ora) */
        OCI("OCI Driver"),
        /** Custom driver with custom URL */
        FULL_URL("Custom Driver");

    	private final String sourceType;
    	
    	String getSourceType(){
    		return sourceType;
    	}
    	
    	ConnectionType(String sourceType){
    		this.sourceType = sourceType;
    	}
    	
        
        static ConnectionType resolveType(String name, ConnectorMessages msg){
        	for(ConnectionType type : values()){
        		if(type.sourceType.equals(name)){
        			return type;
        		}
        	}
        	throw new IllegalArgumentException(msg.format(MSG_INVALID_SOURCE_TYPE,null));
        }
    }
    
    
    
    
    
    /**
     * Default clone implementation.
     * @throws ConnectorException when super.clone fails
     */
    protected OracleConfiguration clone() throws ConnectorException{
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
	 * @return the ignoreCreateExtraOperAttrs
	 */
	@ConfigurationProperty(order = 10, displayMessageKey = MSG_IGNORE_CREATE_EXTRA_OPER_ATTRS_DISPLAY, helpMessageKey = MSG_IGNORE_CREATE_EXTRA_OPER_ATTRS_HELP, required = true)
    public boolean isIgnoreCreateExtraOperAttrs() {
		return ignoreCreateExtraOperAttrs;
	}
	
	@ConfigurationProperty(order = 11, displayMessageKey = MSG_SOURCE_TYPE_DISPLAY, helpMessageKey = MSG_SOURCE_TYPE_HELP, required = false)
	public String getSourceType() {
    	return sourceType;
    }


	/**
	 * @param ignoreCreateExtraOperAttrs the ignoreExtraPassword to set
	 */
	public void setIgnoreCreateExtraOperAttrs(boolean ignoreCreateExtraOperAttrs) {
		this.ignoreCreateExtraOperAttrs = ignoreCreateExtraOperAttrs;
	}

	/**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
        this.connType = null;
    }
    
    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
        this.connType = null;
    }


    /**
     * @param driver the driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
        this.connType = null;
    }


    /**
     * @param database the database to set
     */
    public void setDatabase(String database) {
        this.database = database;
        this.connType = null;
    }

    
    String getUserOwner(){
    	//if we were logged as system, owner will be SYSTEM
    	if("".equals(cs.getAttributeFormatterAndNormalizer(OracleUserAttribute.SYSTEM_USER).getQuatesChar())){
    		return user.toUpperCase();
    	}
    	return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
        this.connType = null;
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
    	this.connType = null;
    }


    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
        this.connType = null;
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
		this.connType = null;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
        this.connType = null;
    }
    
    
    /** Sets case sensitivity from string map 
     * @param cs */
    public void setCaseSensitivity(String cs){
        this.caseSensitivityString = cs;
        this.connType = null;
    }
    
    OracleCaseSensitivitySetup getCSSetup(){
        return cs;
    }
    
    void setCSSetup(OracleCaseSensitivitySetup cs){
        this.cs = new LocalizedAssert(getConnectorMessages()).assertNotNull(cs, "cs");
    }
    
    public void setSourceType(String sourceType){
    	this.sourceType = sourceType;
    	this.connType = null;
    }

    
    ConnectionType getConnType() {
		return connType;
	}

	void setConnType(ConnectionType connType) {
		this.connType = connType;
	}
	

	String getDriverClassName() {
		return driverClassName;
	}

	void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	String getCaseSensitivityString() {
		return caseSensitivityString;
	}

	void setCaseSensitivityString(String caseSensitivityString) {
		this.caseSensitivityString = caseSensitivityString;
	}

	@Override
    public void validate() {
    	if(connType != null){
    		return;
    	}
    	try{
    		new OracleConfigurationValidator(this).validate();
    	}
    	catch(RuntimeException e){
    		//Just to be sure that failed validation does not set connType
    		setConnType(null);
    		throw e;
    	}
    }
    
    
    Connection createUserConnection(String user, GuardedString password){
    	user = cs.formatToken(OracleUserAttribute.USER, user);
    	password = cs.formatToken(OracleUserAttribute.PASSWORD, password);
    	return createConnection(user,password);
    }
    
    Connection createAdminConnection(){
    	String user = cs.normalizeAndFormatToken(OracleUserAttribute.SYSTEM_USER, this.user);
    	GuardedString password = cs.normalizeAndFormatToken(OracleUserAttribute.SYSTEM_PASSWORD, this.password);
        return createConnection(user,password);
    }
    
    
    private Connection createConnection(String user,GuardedString password){
        validate();
        Connection connection = null;
        if(ConnectionType.DATASOURCE.equals(connType)){
            if(StringUtil.isNotBlank(user) && password != null){
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
        	throw new IllegalStateException("Invalid state of OracleConfiguration, connectionType = " + connType);
        }
        try{
        	checkAndAdjustConnection(connType,connection);
        }catch(RuntimeException e){
        	SQLUtil.closeQuietly(connection);
        	throw e;
        }
        return connection;
    }

	private void checkAndAdjustConnection(ConnectionType type, Connection connection) {
		//Set autocommit to off
		//When using datasource with sharable connection , it could throw exception or log warning
		try{
	        if(connection.getAutoCommit()){
	        	log.info("connection.setAutoCommit(false)");
	        	connection.setAutoCommit(false);
	        }
        }catch(SQLException e){
			throw new ConnectorException("Cannot check or adjust connection autocommit flag",e);
        }
		//Set Transaction Isolation
		//When using datasource with sharable connection , it could throw exception or log warning
        try{
	        if(connection.getTransactionIsolation() == Connection.TRANSACTION_NONE || connection.getTransactionIsolation() == Connection.TRANSACTION_READ_UNCOMMITTED){
	        	log.info("connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)");
	        	connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
	        }
        }catch(SQLException e){
        	throw new ConnectorException("Cannot check or adjust transaction isolation settings", e);
        }
		
	}

}
