/**
 * 
 */
package org.identityconnectors.oracle;


import static org.identityconnectors.oracle.OracleMessages.MSG_NORMALIZER_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_NORMALIZER_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_CS_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_CS_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_DATABASE_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_DATABASE_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_DATASOURCE_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_DATASOURCE_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_DRIVER_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_DRIVER_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_DROP_CASCADE_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_DSJNDIENV_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_DSJNDIENV_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_HOST_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_HOST_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_INVALID_SOURCE_TYPE;
import static org.identityconnectors.oracle.OracleMessages.MSG_PASSWORD_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_PASSWORD_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_PORT_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_PORT_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_SOURCE_TYPE_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_SOURCE_TYPE_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_URL_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_URL_HELP;
import static org.identityconnectors.oracle.OracleMessages.MSG_USER_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.MSG_USER_HELP;
import static org.identityconnectors.oracle.OracleMessages.ORACLE_EXTRA_ATTRS_POLICY_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.ORACLE_EXTRA_ATTRS_POLICY_HELP;

import java.sql.Connection;
import java.sql.SQLException;

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
 * Set of configuration properties for connecting to Oracle database.
 * We support 4 types how user can connect to oracle resource :
 * <ul>
 *  <li>Using dataSource</li>
 *  <li>Using custom driver with full jdbc url</li>
 *  <li>Using thin driver</li>
 *  <li>Using oci driver</li>
 * </ul>
 * 
 *  <h4><a name="dataSource"/>Getting connection from DataSource. Used when <code>dataSource</code> property is set</h4>
 *   We will support these properties when connecting to oracle using dataSource
 *   <ul>
 *      <li>dataSource : Name of jndi name of dataSource : required. It must be logical or absolute name of dataSource.
 *          No prefix will be added when trying to do lookup
 *      </li>
 *      <li>
 *          dsJNDIEnv : JNDI environment entries needed to lookup datasource. In most cases should be empty, needed only when lookuping datasource
 *          from different server as server where connectors are running.
 *      </li>
 *      <li>user : Administrative account : optional, default we will get connection from DS without user/password parameters</li>
 *      <li>password : Administrative password : optional, default we will get connection from DS without user/password parameters</li>
 *   </ul>  
 *
 * <h4><a name="url"/>Getting connection from DriverManager using full jdbc url. Used when <code>url</code> property is set</h4>
 * We will support/require these properties when connecting to oracle with url:
 * <ul>
 *      <li> url : Full jdbc url for connecting to oracle</li>
 *      <li> driver  : Classname of jdbc driver</li>
 *      <li> user : Administrative account when connecting to oracle</li>
 *      <li> password : Password for admin account. </li>
 * </ul>
 *
 *   
 * <h4><a name="databaseName"/>Getting connection from DriverManager using Thin driver. Used when driver has value 'thin'</h4>
 * We will support/require these properties when connecting to oracle :
 * <ul>
 *      <li> host : Name or IP of oracle instance host. This is required property</li>
 *      <li> port : Port oracle listener is listening to. Default to 1521 </li>
 *      <li> database : Name of local/remote database</li>
 *      <li> user : Administrative account when connecting to oracle</li>
 *      <li> adminPassword : Password for admin account. </li>
 * </ul>
 * 
 * <h4><a name="aliasName"/>Getting connection from DriverManager using Type 2 oci driver.  Used when driver has value 'oci'</h4>
 * We will require these properties when connecting to oracle using oci driver
 * <ul>
 *      <li> database : Name of local alias </li>
 *      <li> user : Administrative account when connecting to oracle</li>
 *      <li> password : Password for admin account. </li>
 * </ul>
 * 
 * Additionally to these 'connect properties', oracle connector supports following configuration properties :
 * <ul>
 *  <li>CaseSensitivityString : String format that influences built ddl statements</li>
 *  <li>sourceType : We can explicitly force connector to use the connect method. Can have values :
 *      DataSource, Thin Driver, OCI Driver, Custom Driver 
 *  </li>
 *  <li>extraAttributesPolicyString : String format that influences which policy is applied when extra not applicable attribute is passed into operation</li>
 *  <li>dropCascade : Flag used at delete user operation. When set to true, user is dropped with cascade flag</li>
 *  <li>normalizerString : String with the value of normalizer name. Can have values : FULL, INPUT, INPUT_AUTH</li>
 * </ul>
 * @author kitko
 *
 */
public final class OracleConfiguration extends AbstractConfiguration implements Cloneable{
    private static final Log log = Log.getLog(OracleConfiguration.class);
    /** Host name or ip of oracle resource */
    private String host;
    /** Port of oracle listener */
    private String port;
    /** Driver (thin,oci) or full classname */
    private String driver;
    /** Full classname of driver */
    private String driverClassName;
    /** Name of oracle database when using thin or oci driver */
    private String database;
    /** Name of admin user */
    private String user;
    /** Admin password */
    private GuardedString password;
    /** JNDI name of datasource */
    private String dataSource;
    /** Full url when using custom driver */
    private String url;
    /** Optional jndi entries to create initial context */
    private String[] dsJNDIEnv;
    /** Type/manner how we connect to oracle resource. The field is set by validator */
    private ConnectionType connType;
    /** CaseSensitivitySetup of builder. Field is set by validator using the caseSensitivityString */
    private OracleCaseSensitivitySetup cs;
    /** String format in special map format using which we build OracleCaseSensitivitySetup */
    private String caseSensitivityString;
    /** Source type is user defined ConnectionType */
    private String sourceType;
    /** String is special map format using which we build  ExtraAttributesPolicySetup */
    private String extraAttributesPolicyString;
    /** Extra attributes policy. Field is set by validator from extraAttributesPolicyString */
    private ExtraAttributesPolicySetup extraAttributesPolicySetup;
    /** Flag used at delete user operation. When set to true, user is dropped with cascade flag */
    private boolean dropCascade;
    /** String with the value of name of OracleNormalizerName enum */
    private String normalizerString;
    /** NormalizerName we create in validator using normalizerString. Normalizer created by {@link OracleNormalizerName#createNormalizer(OracleCaseSensitivitySetup)} is sent then to SPI operations */
    private OracleNormalizerName normalizerName;
    /**
     * Creates configuration
     */
    public OracleConfiguration() {
        caseSensitivityString = "default";
        cs = new OracleCaseSensitivityBuilder(getConnectorMessages()).build();
        port = OracleSpecifics.LISTENER_DEFAULT_PORT;
        dropCascade = true;
        normalizerString = OracleNormalizerName.INPUT.name();
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
    public String getCaseSensitivityString(){
        return caseSensitivityString;
    }
    
    /**
	 * @return the extraAttributesPolicy
	 */
	@ConfigurationProperty(order = 10, displayMessageKey = ORACLE_EXTRA_ATTRS_POLICY_DISPLAY, helpMessageKey = ORACLE_EXTRA_ATTRS_POLICY_HELP)
    public String getExtraAttributesPolicyString() {
		return extraAttributesPolicyString;
	}
	
	@ConfigurationProperty(order = 11, displayMessageKey = MSG_SOURCE_TYPE_DISPLAY, helpMessageKey = MSG_SOURCE_TYPE_HELP, required = false)
	public String getSourceType() {
    	return sourceType;
    }
	
	@ConfigurationProperty(order = 12, displayMessageKey = MSG_DROP_CASCADE_DISPLAY, helpMessageKey = MSG_DROP_CASCADE_DISPLAY, required = false)
	public boolean isDropCascade(){
		return dropCascade;
	}
	
	@ConfigurationProperty(order = 13, displayMessageKey = MSG_NORMALIZER_DISPLAY, helpMessageKey = MSG_NORMALIZER_HELP, required = false)
	public String getNormalizerString(){
		return normalizerString;
	}
	
	public void setNormalizerString(String normalizerString){
		this.normalizerString = normalizerString;
		this.connType = null;
	}

	/**
	 * @param extraAttributesPolicyString the extraAttributesPolicy to set
	 */
	public void setExtraAttributesPolicyString(String extraAttributesPolicyString) {
		this.extraAttributesPolicyString = extraAttributesPolicyString;
		this.connType = null;
	}
	
	public void setDropCascade(boolean dropCascade){
		this.dropCascade = dropCascade;
		this.connType = null;
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
    public void setCaseSensitivityString(String cs){
        this.caseSensitivityString = cs;
        this.connType = null;
    }
    
    OracleCaseSensitivitySetup getCSSetup(){
        return cs;
    }
    
    void setCSSetup(OracleCaseSensitivitySetup cs){
        this.cs = new LocalizedAssert(getConnectorMessages()).assertNotNull(cs, "cs");
    }
    
    void setExtraAttributesPolicySetup(ExtraAttributesPolicySetup setup){
    	this.extraAttributesPolicySetup = setup;
    }
    
    ExtraAttributesPolicySetup getExtraAttributesPolicySetup(){
    	return extraAttributesPolicySetup;
    }
    
    OracleNormalizerName getNormalizerName() {
		return normalizerName;
	}

	void setNormalizerName(OracleNormalizerName normalizer) {
		this.normalizerName = normalizer;
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
    	validate();
    	user = cs.formatToken(OracleUserAttribute.USER, user);
    	password = cs.formatToken(OracleUserAttribute.PASSWORD, password);
    	return createConnection(user,password);
    }
    
    Connection createAdminConnection(){
    	validate();
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
