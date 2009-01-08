/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.db2;

import java.sql.Connection;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.*;

/**
 * Configuration to access DB2 database. We will support most consistent way how to connect to database.
 * We will support 3 ways how to connect to DB2.
 * <ol>
 * 		<li>Using java.sql.DataSource when using dataSource jndi name, see <a href="#dataSource">dataSource properties</a></li>
 * 		<li>Using type 4 driver, when using host,port and database name, see <a href="#databaseName">databaseName properties</a></li>
 * 		<li>Using type 2 driver, when using local alias, see <a href="#aliasName">aliasName properties</a></li>
 * </ol>
 * 
 * 
 * The above specified order is critical. This means, we will not use any combination, just one of the case in specified order.
 *   
 * 
 *  <h4><a name="dataSource"/>Getting connection from DataSource. Used when <code>dataSource</code> property is set</h4>
 *   We will support these properties when connecting to DB2 using dataSource
 *   <ul>
 *   	<li>dataSource : Name of jndi name of dataSource : required. It must be logical or absolute name of dataSource.
 *   		No prefix will be added when trying to do lookup
 *   	</li>
 *   	<li>
 *   		dsJNDIEnv : JNDI environment entries needed to lookup datasource. In most cases should be empty, needed only when lookuping datasource
 *   		from different server as server where connectors are running.
 *   	</li>
 *   	<li>adminAccount : Administrative account : optional, default we will get connection from DS without user/password parameters</li>
 *   	<li>adminPassword : Administrative password : optional, default we will get connection from DS without user/password parameters</li></li>
 *   </ul>	
 *   
 * <h4><a name="databaseName"/>Getting connection from DriverManager using Type 4 driver. Used when <code>host,port,databaseName</code> property are set</h4>
 * We will support/require these properties when connecting to db2 :
 * <ul>
 * 		<li> host : Name or IP of DB2 instance host. This is required property</li>
 * 		<li> port : Port db2 listener is listening to. Default to 50000 </li>
 * 		<li> databaseName : Name of local/remote database</li>
 * 		<li> subprotocol : db2,db2iSeries. Default to db2 </li>
 * 		<li> jdbcDriver  : Classname of jdbc driver, default to com.ibm.db2.jcc.DB2Driver</li>
 * 		<li> adminAccount : Administrative account when connecting to DB2 in non user contexts. E.g listing of users. </li>
 * 		<li> adminPassword : Password for admin account. </li>
 * </ul>
 * 
 * <h4><a name="aliasName"/>Getting connection from DriverManager using Type 2 driver.  Used when <code>databaseName - local alias</code> property is set</h4>
 * We will require these properties when connecting to db2 using local alias
 * <ul>
 * 		<li> databaseName : Name of local alias created using <code>"db2 catalag database command"</code></li>
 * 		<li> jdbcDriver  : Classname of jdbc driver, default to COM.ibm.db2.jdbc.app.DB2Driver</li>
 * 		<li> subprotocol : db2,db2iSeries. Default to db2 </li>
 * 		<li> adminAccount : Administrative account when connecting to DB2 in non user contexts. E.g listing of users. </li>
 * 		<li> adminPassword : Password for admin account. </li>
 * </ul>
 * 
 * Note that IBM ships two drivers for DB2. We have tested only these two drivers, no other driver was tested.
 * <ul>
 *      <li> IBM DB2 Driver for JDBC and SQLJ</li>
 *      This driver can be used as type4 and type2 driver. In this way driver classname is same, we just need specify different properties.
 *      DatabaseName property is used like remote database in case of type4 and like local alias in case of type2.
 *      <li>Legacy based cli driver</li>
 *      This driver is deprecated now, although it is still included in DB2 9x version. DB2 does not develop this driver any more and it seems
 *      it will be removed in next major version release. However this driver was recommended driver for Websphere.
 * </ul>
 * IBM Net Driver was deprecated in version 8, is not included in version 9. This driver is not supported. 
 * 
 * @author kitko
 *
 */
public class DB2Configuration extends AbstractConfiguration implements Cloneable{
    
    /**
     * Constructor needed for connector framework.
     * Will initialize fields to default values
     */
    public DB2Configuration(){
    }
    
    /**
     * Default clone implementation.
     * @throws ConnectorException when super.clone fails
     */
    public DB2Configuration clone() throws ConnectorException{
        try {
            return (DB2Configuration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ConnectorException("Clone of DB2Configuration super class failed",e);
        }
    }
    
	
	/** Type of connection we will use to connect to DB2 */
	static enum ConnectionType{
		/** Connecting using datasource */
		DATASOURCE,
		/** Connecting using type 4 driver (host,port,databaseName)*/
		TYPE4,
		/** Connecting using type 2 driver (local alias) */
		TYPE2;
	}
	
	/** Name of admin user which will be used to connect to DB2 database.
	 *  Note that DB2 always uses external authentication service, default to underlying OS
	 */
	private String adminAccount;
	/** Password for admin account */
	private GuardedString adminPassword;
	/** Subprotocol driverManager will use to find driver and connect to db2.
	 * 	Probably this will be <b>db2</b>. So final URL will look like : 
	 *  <p>jdbc:db2:server/databaseName </p> 
	 */
	private String jdbcSubProtocol = "db2";
	/** Name of database we will connect to.
	 *  This is the name of local/remote database, not name of local alias. 
	 */
	private String databaseName;
	/**
	 * Full jndi name of datasource
	 */
	private String dataSource;
	/** Class name of jdbc driver */
	private String jdbcDriver = DB2Specifics.JCC_DRIVER;
	/** DB2 host name*/
	private String host;
	/** DB2 listening port */
	private String port = DB2Specifics.DEFAULT_TYPE4_PORT;
	/** Type/manner of connection to DB */
	private ConnectionType connType;
	/** JNDI environment entries for lookuping DS */
	private String[] dsJNDIEnv;
	/**
	 * Replace all grants on update.
	 * Current version of IDM does not support UpdateAttributeValuesOp operations, just update.
	 * This switch is only for backward compatibility and can be removed when IDM will properly call
	 * UpdateAttributeValuesOp operations.
	 * <br/>
	 * When set to true, we will remove all grants and set new passed grants on update. <br/>
	 * When set to false we will do add.
	 */
	private boolean replaceAllGrantsOnUpdate = true;
	
	/**
	 * @return admin account
	 */
	@ConfigurationProperty(order = 0, helpMessageKey = "db2.adminAccount.help", displayMessageKey = "db2.adminAccount.display")
	public String getAdminAccount(){
		return adminAccount;
	}
	
	/**
	 * Sets admin account
	 * @param account
	 */
	public void setAdminAccount(String account){
		this.adminAccount = account;
	}
	
	/**
	 * @return admin password
	 */
	@ConfigurationProperty(order = 1, helpMessageKey = "db2.adminPassword.help", displayMessageKey = "db2.adminPassword.display", confidential=true)
	public GuardedString getAdminPassword(){
		return adminPassword;
	}
	
	/**
	 * Sets admin password
	 * @param adminPassword
	 */
	public void setAdminPassword(GuardedString adminPassword){
		this.adminPassword = adminPassword; 
	}
	
	/**
	 * @return subprotocol when connecting using type 4 driver
	 */
	@ConfigurationProperty(order=2,displayMessageKey="db2.jdbcSubProtocol.display",helpMessageKey="db2.jdbcSubProtocol.help")
	public String getJdbcSubProtocol(){
		return jdbcSubProtocol;
	}
	
	/**
	 * @return databasename
	 */
	@ConfigurationProperty(order=3,displayMessageKey="db2.databaseName.display",helpMessageKey="db2.databaseName.help")
	public String getDatabaseName(){
		return databaseName;
	}
	
	/**
	 * Sets subprotocol
	 * @param subProtocol
	 */
	public void setJdbcSubProtocol(String subProtocol){
		this.jdbcSubProtocol = subProtocol;
	}
	
	/**
	 * Sets database name
	 * @param databaseName
	 */
	public void setDatabaseName(String databaseName){
		this.databaseName = databaseName;
	}
	
	/**
	 * @return classname of jdbc driver
	 */
	@ConfigurationProperty(order=4,displayMessageKey="db2.jdbcDriver.display",helpMessageKey="db2.jdbcDriver.help")
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	/**
	 * Sets classname of jdbc driver
	 * @param jdbcDriver
	 */
	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}
	
	/**
	 * @return the host
	 */
	@ConfigurationProperty(order=5,displayMessageKey="db2.host.display",helpMessageKey="db2.host.help")
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
	@ConfigurationProperty(order=6,displayMessageKey="db2.port.display",helpMessageKey="db2.port.help")
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
	 * @return the dataSource
	 */
	@ConfigurationProperty(order=8,displayMessageKey="db2.dataSource.display",helpMessageKey="db2.dataSource.help")
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
	@ConfigurationProperty(order=9,displayMessageKey="db2.dsJNDIEnv.display",helpMessageKey="db2.dsJNDIEnv.help")
	public String[] getDsJNDIEnv() {
		if(dsJNDIEnv == null){
			return new String[0];
		}
		String[] res = new String[dsJNDIEnv.length];
		System.arraycopy(dsJNDIEnv,0,res,0,dsJNDIEnv.length);
		return res;
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
     * Replace all grants on update switch
     * Current version of IDM does not support UpdateAttributeValuesOp operations, just update.
     * This switch is only for backward compatibility and can be removed when IDM will properly call
     * UpdateAttributeValuesOp operations.
     * <br/>
     * When set to true, we will remove all grants and set new passed grants on update. <br/>
     * When set to false we will do add.
	 * @return replaceAllGrantsOnUpdate
     * 
     **/
     @ConfigurationProperty(order=10,displayMessageKey="db2.replaceAllGrantsOnUpdate.display",helpMessageKey="db2.replaceAllGrantsOnUpdate.help")
	public boolean isReplaceAllGrantsOnUpdate() {
        return replaceAllGrantsOnUpdate;
    }

    /**
     * @param replaceAllGrantsOnUpdate the replaceAllGrantsOnUpdate to set
     */
    public void setReplaceAllGrantsOnUpdate(boolean replaceAllGrantsOnUpdate) {
        this.replaceAllGrantsOnUpdate = replaceAllGrantsOnUpdate;
    }

    /**
	 * @return the connType
	 */
	ConnectionType getConnType() {
		return connType;
	}

	void setConnType(ConnectionType connType) {
		this.connType = connType;
	}
	
	
	@Override
	public void validate() {
		setConnType(null);
		new DB2ConfigurationValidator(this).validate();
	}
	
	Connection createAdminConnection(){
		return createConnection(adminAccount,adminPassword);
	}
	
	Connection createUserConnection(String user,GuardedString password){
		return createConnection(user,password);
	}
	
	private Connection createConnection(String user,GuardedString password){
		validate();
		if(ConnectionType.DATASOURCE.equals(connType)){
			if(user != null){
				return DB2Specifics.createDataSourceConnection(dataSource,user,password,JNDIUtil.arrayToHashtable(dsJNDIEnv, getConnectorMessages()));
			}
			else{
				return DB2Specifics.createDataSourceConnection(dataSource,JNDIUtil.arrayToHashtable(dsJNDIEnv,getConnectorMessages()));
			}
		}
		else if(ConnectionType.TYPE4.equals(connType)){
			return DB2Specifics.createType4Connection(jdbcDriver, host, port, jdbcSubProtocol,databaseName, user, password);
		}
		else if(ConnectionType.TYPE2.equals(connType)){
			return DB2Specifics.createType2Connection(jdbcDriver, databaseName, jdbcSubProtocol, user, password);
		}
		throw new IllegalStateException("Invalid state of DB2Configuration");
	}
	
	
	 
	
	
	

}
