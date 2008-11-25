package org.identityconnectors.db2;

import java.util.*;

import org.identityconnectors.common.*;
import org.identityconnectors.db2.DB2Configuration.ConnectionType;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/** Validator of DB2Configuration.
 *  It validates DB2Configuration  as specified in DB2Configuration javadoc.
 *  It should be just private class of DB2Configuration, but it is too long.
 * @author kitko
 *
 */
class DB2ConfigurationValidator {
	DB2Configuration cfg;
	
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	DB2ConfigurationValidator(DB2Configuration cfg) {
		super();
		this.cfg = cfg;
	}

	private interface ConfigChecker{
		void checkRequired();
		void checkEmpty();
		ConnectionType getType();
	}
	
	private class DataSourceChecker implements ConfigChecker{
		public void checkRequired() {
			Assertions.blankCheck(cfg.getDataSource(),"dataSource");
			//User and password can be specified, then they will be use instead of stored user/password in AS ds configuration.
			//User and password must be specified always together
			if(StringUtil.isNotEmpty(cfg.getAdminAccount())){
				Assertions.nullCheck(cfg.getAdminPassword(),"adminPassword");
			}
		}
		public void checkEmpty() {
			Asserts.blank(cfg.getDataSource(),"DataSource property cannot be set");
		}
		public ConnectionType getType() {
			return ConnectionType.DATASOURCE;
		}
	}
	
	private class Type4DriverChecker implements ConfigChecker{
		public void checkRequired() {
			Assertions.blankCheck(cfg.getHost(), "host");
			Assertions.blankCheck(cfg.getPort(), "port");
			Assertions.blankCheck(cfg.getAdminAccount(), "adminAccount");
			Assertions.nullCheck(cfg.getAdminPassword(), "adminPassword");
			Assertions.blankCheck(cfg.getJdbcDriver(),"jdbcDriver");
			Assertions.blankCheck(cfg.getDatabaseName(),"databaseName");
			Assertions.blankCheck(cfg.getJdbcSubProtocol(),"jdbcSubProtocol");
			try {
				Class.forName(cfg.getJdbcDriver());
			} catch (ClassNotFoundException e) {
				throw new ConnectorException("Cannot load jdbc driver class " + cfg.getJdbcDriver(),e);
			}
		}
		public void checkEmpty() {
			Asserts.blank(cfg.getHost(),"Host property cannot be set");
			Asserts.blank(cfg.getPort(),"Port property cannot be set");
			Asserts.blank(cfg.getDatabaseName(),"DatabaseName property cannot be set");
			Asserts.blank(cfg.getJdbcDriver(),"JdbcDriver property cannot be set");
		}
		public ConnectionType getType() {
			return ConnectionType.TYPE4;
		}
	}
	
	private class Type2DriverChecker implements ConfigChecker{
		public void checkRequired() {
			Assertions.blankCheck(cfg.getAliasName(), "aliasName");
			Assertions.blankCheck(cfg.getAdminAccount(), "adminAccount");
			Assertions.nullCheck(cfg.getAdminPassword(), "adminPassword");
			Assertions.blankCheck(cfg.getJdbcDriver(),"jdbcDriver");
			Assertions.blankCheck(cfg.getJdbcSubProtocol(),"jdbcSubProtocol");
			try {
				Class.forName(cfg.getJdbcDriver());
			} catch (ClassNotFoundException e) {
				throw new ConnectorException("Cannot load jdbc driver class : " + cfg.getJdbcDriver(),e);
			}
		}
		public void checkEmpty() {
			Asserts.blank(cfg.getAliasName(),"AliasName property cannot be set");
		}
		public ConnectionType getType() {
			return ConnectionType.TYPE2;
		}
	}
	
	private static class Asserts{
		
		static String blankArgument(String s,String argument){
			if(s != null && s.length() > 0){
				throw new IllegalArgumentException("Passed argument [" + argument + "] is not blank");
			}
			return s;
		}
		static String blank(String s,String msg){
			if(s != null && s.length() > 0){
				throw new IllegalArgumentException(msg);
			}
			return s;
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
				emptyChecker.checkEmpty();
			}
		}
	}
	
	void validate(){
		//We will use all checkers to check for required fields and check whether other fields are empty
		List<RuntimeException> reqChecksEx = new ArrayList<RuntimeException>(2);
		runCheck(reqChecksEx, new DataSourceChecker(), new Type4DriverChecker(),new Type2DriverChecker());
		runCheck(reqChecksEx, new Type4DriverChecker(), new Type2DriverChecker());
		runCheck(reqChecksEx, new Type2DriverChecker());
		if(cfg.getConnType() == null){
			//Build exception from messages
			StringBuilder stackBuilder = new StringBuilder();
			StringBuilder msgBuilder = new StringBuilder();
			stackBuilder.append(LINE_SEPARATOR);
			for(Throwable ex : reqChecksEx){
				stackBuilder.append(LINE_SEPARATOR);
				stackBuilder.append(ex.getMessage());
				stackBuilder.append(LINE_SEPARATOR);
				msgBuilder.append(ex.getMessage());
				msgBuilder.append(LINE_SEPARATOR);
				for(StackTraceElement el : ex.getStackTrace()){
					stackBuilder.append(el);
					stackBuilder.append(LINE_SEPARATOR);
				}
			}
			final ConnectorException connectorException = new ConnectorException("Validate of DB2Configuration failed : " + msgBuilder,new Exception(stackBuilder.toString()));
			throw connectorException;
		}
	}
	
	
}
