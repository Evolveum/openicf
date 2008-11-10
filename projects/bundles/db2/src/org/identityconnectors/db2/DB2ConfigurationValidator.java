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
		void checkRequiredAndEmpty(ConfigChecker...checkers);
		void checkEmpty();
		ConnectionType getType();
	}
	
	private abstract class AbstractChecker implements ConfigChecker{
		public final void checkRequiredAndEmpty(ConfigChecker...emptyCheckers){
			checkRequired();
			cfg.setConnType(getType());
			for(ConfigChecker emptyChecker : emptyCheckers){
				emptyChecker.checkEmpty();
			}
		}
	}
	
	private class DataSourceChecker extends AbstractChecker implements ConfigChecker{
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
	
	private class Type4DriverChecker extends AbstractChecker implements ConfigChecker{
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
				throw new ConnectorException("Cannot load jdbc driver class",e);
			}
		}
		public void checkEmpty() {
			Asserts.blank(cfg.getHost(),"Host property cannot be set");
			Asserts.blank(cfg.getPort(),"Port property cannot be set");
			Asserts.blank(cfg.getDatabaseName(),"DatabaseName property cannot be set");
			Asserts.blank(cfg.getJdbcDriver(),"JdbcDriver property cannot be set");
			Asserts.blank(cfg.getJdbcSubProtocol(),"JdbcSubProtocol property cannot be set");
		}
		public ConnectionType getType() {
			return ConnectionType.TYPE4;
		}
	}
	
	private class Type2DriverChecker extends AbstractChecker implements ConfigChecker{
		public void checkRequired() {
			Assertions.blankCheck(cfg.getAliasName(), "aliasName");
			Assertions.blankCheck(cfg.getAdminAccount(), "adminAccount");
			Assertions.nullCheck(cfg.getAdminPassword(), "adminPassword");
			Assertions.blankCheck(cfg.getJdbcDriver(),"jdbcDriver");
			try {
				Class.forName(cfg.getJdbcDriver());
			} catch (ClassNotFoundException e) {
				throw new ConnectorException("Cannot load jdbc driver class",e);
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
	
	void validate(){
		//We will use all checkers to check for required fields and check whether other fields are empty
		List<Throwable> reqChecks = new ArrayList<Throwable>(2);
		try{
			new DataSourceChecker().checkRequiredAndEmpty(new Type4DriverChecker(),new Type2DriverChecker());
		}
		catch(Throwable e){
			reqChecks.add(e);
		}
		if(cfg.getConnType() == null){
			try{
				new Type4DriverChecker().checkRequiredAndEmpty(new Type2DriverChecker());
			}
			catch(Throwable e){
				reqChecks.add(e);
			}
		}
		if(cfg.getConnType() == null){
			try{
				new Type2DriverChecker().checkRequiredAndEmpty();
			}
			catch(Throwable e){
				reqChecks.add(e);
			}
		}
		if(cfg.getConnType() == null){
			//Build exception from messages
			StringBuilder builder = new StringBuilder();
			for(Throwable ex : reqChecks){
				builder.append(LINE_SEPARATOR);
				builder.append(ex.getMessage());
			}
			final ConnectorException connectorException = new ConnectorException("Validate of DB2Configuration failed",new Exception(builder.toString()));
			throw connectorException;
		}
	}
	
	
}
