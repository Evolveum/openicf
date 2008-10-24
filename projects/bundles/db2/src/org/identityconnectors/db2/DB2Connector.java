package org.identityconnectors.db2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;

@ConnectorClass(
        displayNameKey = "DatabaseTable",
        configurationClass = DB2Configuration.class)
public class DB2Connector implements AuthenticateOp,SchemaOp,CreateOp,SearchOp<String>, PoolableConnector {
	
	private final static Log log = Log.getLog(DB2Connector.class);
	private DB2Connection adminConn;
	private DB2Configuration cfg;

	public void authenticate(String username, GuardedString password,OperationOptions options) {
		log.info("authenticate user: {0}", username);
		//just try to create connection with passed credentials
		Connection conn = null;
		try{
			conn = createConnection(username, password);
		}
		catch(RuntimeException e){
			if(e.getCause() instanceof SQLException){
				SQLException sqlE = (SQLException) e.getCause();
				if("28000".equals(sqlE.getSQLState()) && -4214 ==sqlE.getErrorCode()){
					//Wrong user or password, log it here and rethrow
					log.info(e,"Invalid user/passord for user: {0}",username);
					throw new InvalidCredentialException("invalid user/password",e.getCause());
				}
			}
			throw e;
		}
		finally{
			SQLUtil.closeQuietly(conn);
		}
		log.info("User {0} authenticated",username);
	}
	
	public Schema schema() {
        //The Name is supported attribute
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(Name.NAME,true,true,true,true));
        //Password is operationalAttribute 
        attrInfoSet.add(OperationalAttributeInfos.PASSWORD);

        // Use SchemaBuilder to build the schema. Currently, only ACCOUNT type is supported.
        SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        return schemaBld.build();
    } 

	public void checkAlive() {
		adminConn.test();
	}

	public void dispose() {
		adminConn.dispose();
	}

	public Configuration getConfiguration() {
		return cfg;
	}

	public void init(Configuration cfg) {
		this.cfg = (DB2Configuration) cfg;
		this.adminConn = new DB2Connection(createAdminConnection());
	}
	
	private Connection createAdminConnection(){
		return createConnection(cfg.getAdminAccount(),cfg.getAdminPassword());
	}
	
	private Connection createConnection(String user,GuardedString password){
		String driver = cfg.getJdbcDriver();
		String host = cfg.getHost();
		String port = cfg.getPort();
		String subProtocol = cfg.getJdbcSubProtocol();
		String databaseName = cfg.getDatabaseName();
		return DB2Connection.createDB2Connection(driver, host, port, subProtocol, databaseName, user, password);
	}

	public Uid create(ObjectClass oclass, Set<Attribute> attrs,
			OperationOptions options) {
		return new Uid("xx");
	}

	public FilterTranslator<String> createFilterTranslator(ObjectClass oclass,
			OperationOptions options) {
		return new MyFilterTranslator();
	}
	
	private static class MyFilterTranslator implements FilterTranslator<String>{
		public List<String> translate(Filter filter) {
			return Arrays.asList("xx");
		}
		
	}

	public void executeQuery(ObjectClass oclass, String query,ResultsHandler handler, OperationOptions options) {
		Set<Attribute> attributeSet = new HashSet<Attribute>();
		attributeSet.add(new Name("xx"));
		attributeSet.add(new Uid("xx"));
		handler.handle(new ConnectorObject(ObjectClass.ACCOUNT,attributeSet));
	}

}
