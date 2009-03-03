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

import java.util.*;

import org.identityconnectors.common.*;
import org.identityconnectors.db2.DB2Configuration.ConnectionType;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import static org.identityconnectors.db2.DB2Messages.*;

/** Validator of DB2Configuration.
 *  It validates DB2Configuration  as specified in DB2Configuration javadoc.
 *  It should be just private class of DB2Configuration, but it is too long.
 * @author kitko
 *
 */
class DB2ConfigurationValidator {
	private DB2Configuration cfg;
	private LocalizedAssert asserts;
	
	
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	DB2ConfigurationValidator(DB2Configuration cfg) {
		super();
		this.cfg = cfg;
		this.asserts = new LocalizedAssert(cfg.getConnectorMessages());
	}

	private interface ConfigChecker{
		void checkRequired();
		void checkEmpty(ConfigChecker reqChecker);
		ConnectionType getType();
	}
	
	private class DataSourceChecker implements ConfigChecker{
		public void checkRequired() {
		    asserts.assertNotBlank(cfg.getDataSource(),"dataSource");
			//User and password can be specified, then they will be used instead of stored user/password in AS ds configuration.
			//User and password must be specified always together
			if(StringUtil.isNotEmpty(cfg.getAdminAccount())){
			    asserts.assertNotNull(cfg.getAdminPassword(),"adminPassword");
			}
		}
		public void checkEmpty(ConfigChecker reqChecker) {
			asserts.assertBlank(cfg.getDataSource(),"dataSource");
		}
		public ConnectionType getType() {
			return ConnectionType.DATASOURCE;
		}
		
		DataSourceChecker(){}
	}
	
	private class Type4DriverChecker implements ConfigChecker{
		public void checkRequired() {
		    asserts.assertNotBlank(cfg.getHost(), "host");
		    asserts.assertNotBlank(cfg.getPort(), "port");
		    asserts.assertNotBlank(cfg.getAdminAccount(), "adminAccount");
		    asserts.assertNotNull(cfg.getAdminPassword(), "adminPassword");
		    asserts.assertNotBlank(cfg.getJdbcDriver(),"jdbcDriver");
		    asserts.assertNotBlank(cfg.getDatabaseName(),"databaseName");
		    asserts.assertNotBlank(cfg.getJdbcSubProtocol(),"jdbcSubProtocol");
			try {
				Class.forName(cfg.getJdbcDriver());
			} catch (ClassNotFoundException e) {
				throw new ConnectorException(cfg.getConnectorMessages().format(JDBC_DRIVER_CLASS_NOT_FOUND,null,cfg.getJdbcDriver()),e);
			}
		}
		public void checkEmpty(ConfigChecker reqChecker) {
			if(!(reqChecker instanceof Type2DriverChecker)){
			    asserts.assertBlank(cfg.getJdbcDriver(),"jdbcDriver");
			    asserts.assertBlank(cfg.getDatabaseName(),"database");
			}
			//User and password can be set for all types of connections
			//Asserts.isBlankMsg(cfg.getAdminAccount(), "AdminAccount cannot be set");
			//Asserts.isNullMsg(cfg.getAdminPassword(), "AdminPassword cannot be set");
			asserts.assertBlank(cfg.getHost(),"host");
			asserts.assertBlank(cfg.getPort(),"port");
		}
		public ConnectionType getType() {
			return ConnectionType.TYPE4;
		}
	}
	
	private class Type2DriverChecker implements ConfigChecker{
		public void checkRequired() {
		    asserts.assertNotBlank(cfg.getDatabaseName(), "databaseName");
		    asserts.assertNotBlank(cfg.getAdminAccount(), "adminAccount");
		    asserts.assertNotNull(cfg.getAdminPassword(), "adminPassword");
		    asserts.assertNotBlank(cfg.getJdbcDriver(),"jdbcDriver");
		    asserts.assertNotBlank(cfg.getJdbcSubProtocol(),"jdbcSubProtocol");
			try {
				Class.forName(cfg.getJdbcDriver());
			} catch (ClassNotFoundException e) {
                throw new ConnectorException(cfg.getConnectorMessages().format(JDBC_DRIVER_CLASS_NOT_FOUND,null,cfg.getJdbcDriver()),e);
			}
		}
		public void checkEmpty(ConfigChecker reqChecker) {
			if(!(reqChecker instanceof Type4DriverChecker)){
			    asserts.assertBlank(cfg.getJdbcDriver(),"jdbcDriver");
                asserts.assertBlank(cfg.getDatabaseName(),"database");
			}
		}
		public ConnectionType getType() {
			return ConnectionType.TYPE2;
		}
	}
	
	private void runCheck(List<RuntimeException> reqEx,ConfigChecker reqChecker,ConfigChecker ...emptyCheckers){
		if(cfg.getConnType() != null){
			return;
		}
		try{
			reqChecker.checkRequired();
			cfg.setConnType(reqChecker.getType());
		}
		catch(RuntimeException e){
			reqEx.add(e);
		}
		if(cfg.getConnType() != null){
			for(ConfigChecker emptyChecker : emptyCheckers){
				emptyChecker.checkEmpty(reqChecker);
			}
		}
	}
	
	void validate(){
		//We will use all checkers to check for required fields and check whether other fields are empty
		List<RuntimeException> reqChecksEx = new ArrayList<RuntimeException>(2);
		runCheck(reqChecksEx, new DataSourceChecker(), new Type4DriverChecker(),new Type2DriverChecker());
		runCheck(reqChecksEx, new Type4DriverChecker(), new DataSourceChecker(),new Type2DriverChecker());
		runCheck(reqChecksEx, new Type2DriverChecker(), new DataSourceChecker(), new Type4DriverChecker());
		if(cfg.getConnType() == null){
			//Build exception from messages
			StringBuilder stackBuilder = new StringBuilder();
			StringBuilder msgBuilder = new StringBuilder();
			stackBuilder.append(LINE_SEPARATOR);
			for(Throwable ex : reqChecksEx){
				stackBuilder.append(LINE_SEPARATOR);
				stackBuilder.append(ex.getMessage());
				stackBuilder.append(LINE_SEPARATOR);
				if(msgBuilder.length() > 0){
	                msgBuilder.append(" | ");
				}
				msgBuilder.append(ex.getMessage());
				for(StackTraceElement el : ex.getStackTrace()){
					stackBuilder.append(el);
					stackBuilder.append(LINE_SEPARATOR);
				}
			}
            final ConnectorException connectorException = new ConnectorException(
                    cfg.getConnectorMessages().format(VALIDATE_FAIL,
                            null, msgBuilder), new Exception(stackBuilder
                            .toString()));
			throw connectorException;
		}
		if(cfg.getPort() != null){
		    Integer.parseInt(cfg.getPort());
		}
	}
	
	
}
