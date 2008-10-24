/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.db2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Configuration to access DB2 database. We will support most consistent way how to connect to database.
 * We will support/require these properties when connecting to db2 :
 * <ul>
 * 		<li> host : Name or IP of DB2 instance host. This is required property</li>
 * 		<li> port : Port db2 listener is listening to. Default to 50000 </li>
 * 		<li> subprotocol : db2,db2iSeries. Default to db2 </li>
 * 		<li> jdbcDriver  : Classname of jdbc driver. Default to {@link DB2Connection#APP_DRIVER} </li>
 * 		<li> adminAccount : Administrative account when connecting to DB2 in non user contexts. E.g listing of users. </li>
 * 		<li> adminPassword : Password for admin account. </li>
 * </ul>
 * 
 * Later we will support get connection from datasource. 
 * @author kitko
 *
 */
public class DB2Configuration extends AbstractConfiguration {
	
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
	/** Name of database we will connect to */
	private String databaseName;
	/** Class name of jdbc driver */
	private String jdbcDriver = DB2Connection.APP_DRIVER;
	/** Whether we should remove all grants on update authority */
	private boolean removeAllGrants;
	/** DB2 host name*/
	private String host;
	/** DB2 listening port */
	private String port = "50000";
	
	/** List of db2 keywords */
	private static Collection<String> excludeNames;
	
	@ConfigurationProperty(order = 1, helpMessageKey = "db2.adminAccount.help", displayMessageKey = "db2.adminAccount.display")
	public String getAdminAccount(){
		return adminAccount;
	}
	
	public void setAdminAccount(String account){
		this.adminAccount = account;
	}
	
	@ConfigurationProperty(order = 1, helpMessageKey = "db2.adminPassword.help", displayMessageKey = "db2.adminPassword.display", confidential=true)
	public GuardedString getAdminPassword(){
		return adminPassword;
	}
	
	public void setAdminPassword(GuardedString adminPassword){
		this.adminPassword = adminPassword; 
	}
	
	
	public static Collection<String> getExcludeNames(){
		return Collections.unmodifiableCollection(readExcludeNames());
	}
	
	@ConfigurationProperty
	public String getJdbcSubProtocol(){
		return jdbcSubProtocol;
	}
	
	public void setJdbcSubProtocol(String subProtocol){
		this.jdbcSubProtocol = subProtocol;
	}
	
	@ConfigurationProperty
	public void setDatabaseName(String databaseName){
		this.databaseName = databaseName;
	}
	
	@ConfigurationProperty
	public String getDatabaseName(){
		return databaseName;
	}
	
	@ConfigurationProperty
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}
	
	@ConfigurationProperty
	public boolean isRemoveAllGrants() {
		return removeAllGrants;
	}

	public void setRemoveAllGrants(boolean removeAllGrants) {
		this.removeAllGrants = removeAllGrants;
	}
	
	/**
	 * @return the host
	 */
	@ConfigurationProperty
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
	@ConfigurationProperty
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	private static Collection<String> readExcludeNames() {
		if(excludeNames == null){
			synchronized (DB2Configuration.class) {
				if(excludeNames == null){
					//We will read exclude names from resource named "exclude.names"
					String names = IOUtil.getResourceAsString(DB2Configuration.class, "exclude.names");
					if(names == null){
						throw new IllegalStateException("Cannot load exclude names for DB2 connector");
					}
					excludeNames = new HashSet<String>();
					StringTokenizer tokenizer = new StringTokenizer(names,",\n",false);
					while(tokenizer.hasMoreTokens()){
						excludeNames.add(tokenizer.nextToken());
					}
				}
			}
		}
		return excludeNames;
	}

	@Override
	public void validate() {
		Assertions.blankCheck(host, "host");
		Assertions.nullCheck(port, "port");
		Assertions.blankCheck(adminAccount, "adminAccount");
		Assertions.blankCheck(jdbcDriver,"jdbcDriver");
		Assertions.blankCheck(databaseName,"databaseName");
		Assertions.blankCheck(jdbcSubProtocol,"jdbcSubProtocol");
		//check driver is not classpath
		try {
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("db2 jdbc driver class not found",e);
		}
		
	}
	

}
