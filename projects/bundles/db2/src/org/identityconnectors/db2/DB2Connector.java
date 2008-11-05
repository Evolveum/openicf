package org.identityconnectors.db2;

import java.sql.*;
import java.util.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;

@ConnectorClass(
        displayNameKey = "DatabaseTable",
        configurationClass = DB2Configuration.class)
public class DB2Connector implements AuthenticateOp,SchemaOp,CreateOp,SearchOp<FilterWhereBuilder>,DeleteOp,UpdateOp,PoolableConnector {
	
	private final static Log log = Log.getLog(DB2Connector.class);
	private Connection adminConn;
	private DB2Configuration cfg;
    // DB2 limitation on account id size
    private static final int maxNameSize = 30;
    private static final String USER_AUTH_GRANTS = "grants";


	public Uid authenticate(String username, GuardedString password,OperationOptions options) {
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
		return new Uid(username);
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
		DB2Specifics.testConnection(adminConn);
	}

	public void dispose() {
		SQLUtil.closeQuietly(adminConn);
	}

	public Configuration getConfiguration() {
		return cfg;
	}

	public void init(Configuration cfg) {
		this.cfg = (DB2Configuration) cfg;
		this.adminConn = createAdminConnection();
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
		final Connection conn = DB2Specifics.createDB2Connection(driver, host, port, subProtocol, databaseName, user, password);
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			throw new ConnectorException("Cannot switch off autocommit",e);
		}
		return conn;
	}


    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new DB2FilterTranslator(oclass, options);
    }
	

	public void executeQuery(ObjectClass oclass, FilterWhereBuilder where,ResultsHandler handler, OperationOptions options) {
        /**
         * The query to get the user columns
         * The Password <> '' mean we want to avoid reading duplicates
         * Every base user has password set up
         */
        final String ALL_USER_QUERY = "SELECT GRANTEE FROM SYSIBM.SYSDBAUTH WHERE GRANTEETYPE = 'U' AND CONNECTAUTH = 'Y'";

        if (oclass == null || !ObjectClass.ACCOUNT.equals(oclass)) {
            throw new IllegalArgumentException("Unsupported objectclass '" + oclass + "'");
        }
        // Database query builder will create SQL query.
        // if where == null then all users are returned
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(ALL_USER_QUERY);
        query.setWhere(where);
        String sql = query.getSQL();
        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = adminConn.prepareStatement(sql);
            SQLUtil.setParams(statement, query.getParams());
            result = statement.executeQuery();
            while (result.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                
                //To be sure that uid and name are present for mysql
                final String userName = result.getString("GRANTEE").trim();                
                bld.setUid(new Uid(userName));
                bld.setName(userName);
                //No other attributes are now supported.
                //Password can be encoded and it is not provided as an attribute
                
                // only deals w/ accounts..
                bld.setObjectClass(ObjectClass.ACCOUNT);
                
                // create the connector object..
                ConnectorObject ret = bld.build();
                if (!handler.handle(ret)) {
                    break;
                }
            }
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
	}
	
	public Uid create(ObjectClass oclass, Set<Attribute> attrs,OperationOptions options) {
        if ( oclass == null || !oclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Name user = AttributeUtil.getNameFromAttributes(attrs);
        if (user == null || StringUtil.isBlank(user.getNameValue())) {
            throw new IllegalArgumentException("The Name attribute cannot be null or empty.");
        }
        // Password is Operational
        GuardedString password = AttributeUtil.getPasswordValue(attrs);
        if (password == null) {
            throw new IllegalArgumentException("The Password attribute cannot be null.");
        }
        checkDB2Validity(user.getNameValue(),password);
        updateAuthority(user.getNameValue(),attrs);
		return new Uid(user.getNameValue());
	}
	
	
	/**
     *  Applies resources grants and revokes to the passed user.  Updates
     *  occur in a transaction.  Assumes connection is already open.
	 * @param password 
     */
    private void updateAuthority(String user,Set<Attribute> attrs)   {
        checkAdminConnection();

        Collection<String> grants = null;
        Attribute wsAttr = AttributeUtil.find(USER_AUTH_GRANTS, attrs);
        if (wsAttr != null) {
            String delimitedGrants = AttributeUtil.getStringValue(wsAttr);
            if (delimitedGrants != null) {
                // yuck, this utility method should be somewhere central...
                grants = DB2Specifics.divideString(delimitedGrants, ',', true);
            }
            else if (cfg.isRemoveAllGrants()) {
                    throw new IllegalStateException("When DB2 RA 'removeForeignGrants' = 1, " +
                                                "at least 1 grant is required.");
            }
        }
        else if (cfg.isRemoveAllGrants()) {
                throw new IllegalStateException("When DB2 RA 'removeForeignGrants' = 1, " +
                                            "at least 1 grant is required.");
        }
        try {
            if (grants != null) {
                if (cfg.isRemoveAllGrants()) {
                    revokeAllGrants(user);
                }
                executeGrants("", grants, "", user);
                adminConn.commit();
            }
        }
        catch (Exception e) {
        	SQLUtil.rollbackQuietly(adminConn);
        	throw ConnectorException.wrap(e);
        }
    }
    
    
    private void checkAdminConnection() {
		if(adminConn == null){
			throw new IllegalStateException("No admin connection present");
		}
	}

	/**
     *  Checks a given account id and password to make sure they follow DB2
     *  rules for validity.  The rules are given in the DB2 SQL Reference
     *  Manual.  They include length limits, forbidden prefixes, and forbidden
     *  keywords.  Throws and exception if the name or password are invalid.
     */
    private void checkDB2Validity(String accountID, GuardedString password)  {
        if (accountID.length() > maxNameSize) {
        	throw new IllegalArgumentException("Name to short");
        }
        if (DB2Specifics.containsIllegalDB2Chars(accountID.toCharArray())) {
        	throw new IllegalArgumentException("Name contains illegal characters");
        }
        if (!DB2Specifics.isValidName(accountID.toUpperCase())) {
            throw new IllegalArgumentException("Name is reserved keyword or its substring");
        }

        if (password != null) {
        	password.access(new Accessor(){
				public void access(char[] clearChars) {
			        if (DB2Specifics.containsIllegalDB2Chars(clearChars)) {
			        	throw new IllegalArgumentException("Password contains illegal characters");
			        }
			        if (!DB2Specifics.isValidName(new String(clearChars).toUpperCase())) {
			            throw new IllegalArgumentException("Password is reserved keyword or its substring");
			        }
				}
        	});
        }
    }
    
    /**
     *  Removes all grants for a user on the resource.  Effectively
     *  deletes them from the resource.
     */
    private void revokeAllGrants(String user) throws SQLException {
        checkDB2Validity(user,null);
        Collection<DB2Authority> allAuthorities = new DB2AuthorityReader(adminConn).readAllAuthorities(user);
        revokeGrants(allAuthorities);
    }

    
    
    /**
     *  For a given grant type and user, revokes the passed collection
     *  of grant objects from the resource.
     */
    private void revokeGrants(Collection<DB2Authority> db2AuthoritiesToRevoke)   throws  SQLException {
        for(DB2Authority auth : db2AuthoritiesToRevoke){
            DB2AuthorityTable authTable = (DB2AuthorityTable)DB2Specifics.authType2DB2AuthorityTable(auth.authorityType);
            String revokeSQL = authTable.generateRevokeSQL(auth);
            executeSQL(revokeSQL);
        }
    }
    
    
    private void executeSQL(String sql) throws SQLException {
        checkAdminConnection();
        Statement statement = null;
        try {
            statement = adminConn.createStatement();
            statement.execute(sql);
        }
        finally {
            SQLUtil.closeQuietly(statement);
        }
    }
    
    
    /**
     *  Executes a set of sql GRANT statements built using an sql
     *  prefix, a collection of grant objects, a postfix, and a user.
     *  Throws if anything goes wrong.
     */
    private void executeGrants(String prefix, Collection<String> grants,String postfix, String user)  throws SQLException{
        for(String grant : grants){
            String sql = "GRANT " + prefix + " " + grant  + " " + postfix + " TO USER " + user.toUpperCase() ;
            executeSQL(sql);
        }
    }

	public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        if ( objClass == null || !objClass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
		try {
			revokeAllGrants(uid.getName());
		} catch (SQLException e) {
			throw new ConnectorException("Error revoking user grants",e);
		}
	}
	
	

	public Uid update(ObjectClass objclass, Set<Attribute> attrs, OperationOptions options) {
        if ( objclass == null || !objclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Name user = AttributeUtil.getNameFromAttributes(attrs);
        if (user == null || StringUtil.isBlank(user.getNameValue())) {
            throw new IllegalArgumentException("The Name attribute cannot be null or empty.");
        }
		updateAuthority(user.getName(), attrs);
		return new Uid(user.getName());
	}
    
    
    
    
  
}
