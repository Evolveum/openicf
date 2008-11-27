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

/**
 * Connector to DB2
 * @author kitko
 *
 */
@ConnectorClass(
        displayNameKey = "db2.connector",
        configurationClass = DB2Configuration.class)
public class DB2Connector implements AuthenticateOp,SchemaOp,CreateOp,SearchOp<FilterWhereBuilder>,DeleteOp,UpdateOp,TestOp,PoolableConnector,AdvancedUpdateOp,AttributeNormalizer {
	
	private final static Log log = Log.getLog(DB2Connector.class);
	private Connection adminConn;
	private DB2Configuration cfg;
    // DB2 limitation on account id size
    private static final int maxNameSize = 30;
    static final String USER_AUTH_GRANTS = "grants";


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
					log.info(e,"DB2.authenticate : Invalid user/passord for user: {0}",username);
					throw new InvalidCredentialException("DB2.authenticate :  Invalid user/password",e.getCause());
				}
			}
			throw e;
		}
		finally{
			SQLUtil.closeQuietly(conn);
		}
		log.info("User {0} authenticated",username);
		return new Uid(username.toUpperCase());
	}
	
	public Schema schema() {
        //The Name is supported attribute
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(Name.NAME,true,true,true,false));
        AttributeInfoBuilder grantsBuilder = new AttributeInfoBuilder();
        grantsBuilder.setName(USER_AUTH_GRANTS).setCreateable(true).
        setUpdateable(true).setRequired(true).setReadable(false);
        attrInfoSet.add(grantsBuilder.build());
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
		final Connection conn = cfg.createUserConnection(user, password);
		//switch off auto commit, but not when connecting using datasource.
		//Probably connection from DS would throw exception  when trying to change autocommit
		if(!DB2Configuration.ConnectionType.DATASOURCE.equals(cfg.getConnType())){
			try {
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				throw new ConnectorException("Cannot switch off autocommit",e);
			}
		}
		return conn;
	}
    
    String buildAuthorityString(String userName){
    	DB2AuthorityReader dB2AuthorityReader = new DB2AuthorityReader(adminConn);
    	Collection<DB2Authority> allAuths = null;
		try {
			allAuths = dB2AuthorityReader.readAllAuthorities(userName);
		} catch (SQLException e) {
			throw new ConnectorException("Error reading db2 authorities",e);
		}
    	StringBuilder buffer = new StringBuilder();
    	for(DB2Authority authority : allAuths){
    		final DB2AuthorityTable authorityTable = DB2Specifics.authType2DB2AuthorityTable(authority.authorityType);
            String grantString = authorityTable.generateGrant(authority);
            buffer.append(grantString).append(',');
    	}
    	if(buffer.length() > 0){
    		buffer.deleteCharAt(buffer.length() - 1);
    	}
    	return buffer.toString();
    }
    
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new DB2FilterTranslator(oclass, options);
    }
	

	public void executeQuery(ObjectClass oclass, FilterWhereBuilder where,ResultsHandler handler, OperationOptions options) {
		//Read users from SYSIBM.SYSDBAUTH table
		//DB2 stores users in UPPERCASE , we must do UPPER(TRIM(GRANTEE)) = upper('john')
        final String ALL_USER_QUERY = "SELECT GRANTEE FROM SYSIBM.SYSDBAUTH WHERE GRANTEETYPE = 'U' AND CONNECTAUTH = 'Y'";

        if (oclass == null || !ObjectClass.ACCOUNT.equals(oclass)) {
            throw new IllegalArgumentException("Unsupported objectclass '" + oclass + "'");
        }
        // Database query builder will create SQL query.
        // if where == null then all users are returned
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(ALL_USER_QUERY);
        query.setWhere(where);
        final String sql = query.getSQL();
        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = adminConn.prepareStatement(sql);
            SQLUtil.setParams(statement, query.getParams());
            result = statement.executeQuery();
            while (result.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                
                final String userName = result.getString("GRANTEE").trim();
                if(options.getAttributesToGet() != null && Arrays.asList(options.getAttributesToGet()).contains(USER_AUTH_GRANTS)){
                	String authString = buildAuthorityString(userName);
                	bld.addAttribute(USER_AUTH_GRANTS,authString);
                }
                
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
        checkUserNotExist(user.getNameValue());
        checkDB2Validity(user.getNameValue(),password);
        try{
        	updateAuthority(user.getNameValue(),attrs,Type.ADD);
        	adminConn.commit();
        }
        catch(SQLException e){
        	throw new ConnectorException("cannot commit create",e);
        }
		return new Uid(user.getNameValue());
	}
	
	
	private void checkUserNotExist(String user) {
		boolean userExist = userExist(user);
		if(userExist){
			throw new AlreadyExistsException("User " + user + " already exists");
		}
	}
	
	private void checkUserExist(String user) {
		boolean userExist = userExist(user);
		if(!userExist){
			throw new UnknownUidException(new Uid(user),ObjectClass.ACCOUNT);
		}
	}
	
	
	private boolean userExist(String user){
		final String ALL_USER_QUERY = "SELECT GRANTEE FROM SYSIBM.SYSDBAUTH WHERE GRANTEETYPE = 'U' AND CONNECTAUTH = 'Y' AND TRIM(GRANTEE) = ?";
		PreparedStatement st = null;
		ResultSet rs = null;
		try{
			st = adminConn.prepareStatement(ALL_USER_QUERY);
			st.setString(1,user.toUpperCase());
			rs = st.executeQuery();
			return rs.next(); 
		}
		catch(SQLException e){
			throw new ConnectorException("Cannot test whether user exist",e);
		}
		finally{
			SQLUtil.closeQuietly(st);
		}
	}

	/**
     *  Applies resources grants and revokes to the passed user.  Updates
     *  occur in a transaction.  Assumes connection is already open.
	 * @param password 
     */
    private void updateAuthority(String user,Set<Attribute> attrs,AdvancedUpdateOp.Type type)   {
        checkAdminConnection();
        Attribute wsAttr = AttributeUtil.find(USER_AUTH_GRANTS, attrs);
        String delimitedGrants = wsAttr != null ? AttributeUtil.getStringValue(wsAttr) : null;
        Collection<String> grants = delimitedGrants != null ? DB2Specifics.divideString(delimitedGrants, ',', true) : new ArrayList<String>();
        try{
	        switch(type){
	        	case ADD : 		{
	        					 addMandatoryConnect(grants);
	        					 executeGrants(grants,user);
	        					 break;
	        	}
	        	case REPLACE :  {
	        					addMandatoryConnect(grants);
	        					revokeAllGrants(user);
	        					executeGrants(grants,user);
	        					break;
	        	}
	        	case DELETE : 	{
	        					removeMandatoryRevoke(grants);
	        					executeRevokes(grants, user);
	        					break;
	        	}
	        }
        }
        catch (Exception e) {
        	SQLUtil.rollbackQuietly(adminConn);
        	throw ConnectorException.wrap(e);
        }
    }
    
    private void addMandatoryConnect(Collection<String> grants){
    	boolean addConnect = true;
    	for(String grant : grants){
    		if(grant.trim().equalsIgnoreCase("CONNECT ON DATABASE")){
    			addConnect = false;
    		}
    	}
    	if(addConnect){
    		grants.add("CONNECT ON DATABASE");
    	}
    }
    
    private void removeMandatoryRevoke(Collection<String> grants){
    	for(Iterator<String> i = grants.iterator();i.hasNext();){
    		if(i.next().trim().equalsIgnoreCase("CONNECT ON DATABASE")){
    			i.remove();
    		}
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
            DB2AuthorityTable authTable = DB2Specifics.authType2DB2AuthorityTable(auth.authorityType);
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
        catch(SQLException e){
        	log.error(e,"Error executing query {0}", sql);
        	throw e;
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
    private void executeGrants(Collection<String> grants,String user)  throws SQLException{
        for(String grant : grants){
            String sql = "GRANT " + grant  + " TO USER " + user.toUpperCase() ;
            executeSQL(sql);
        }
    }
    
    /**
     *  Executes a set of sql REVOKE statements built using an sql
     *  prefix, a collection of grant objects, a postfix, and a user.
     *  Throws if anything goes wrong.
     */
    private void executeRevokes(Collection<String> grants,String user)  throws SQLException{
        for(String grant : grants){
            String sql = "REVOKE " + grant  + " FROM USER " + user.toUpperCase() ;
            executeSQL(sql);
        }
    }
    

	public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        if ( objClass == null || !objClass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
		checkUserExist(uid.getUidValue());
        try {
			revokeAllGrants(uid.getUidValue());
			adminConn.commit();
		} catch (SQLException e) {
		    SQLUtil.rollbackQuietly(adminConn);
			throw new ConnectorException("Error revoking user grants",e);
		}
	}
	

	public Uid update(ObjectClass objclass, Set<Attribute> attrs, OperationOptions options) {
		return update(Type.REPLACE,objclass,attrs,options);
	}

	public void test() {
		cfg.validate();
		DB2Specifics.testConnection(adminConn);
	}

	public Uid update(Type type, ObjectClass objclass, Set<Attribute> attrs,OperationOptions options) {
        if ( objclass == null || !objclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Update operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        if(name != null){
        	throw new IllegalArgumentException("Name attribute is nonUpdatable, cannot appear in update operation");
        }
        
        Uid uid = AttributeUtil.getUidAttribute(attrs);
        if (uid == null || StringUtil.isBlank(uid.getUidValue())){
            throw new IllegalArgumentException("The uid attribute cannot be null or empty.");
        }
        
        try{
        	updateAuthority(uid.getUidValue(), attrs, type);
        	adminConn.commit();
        }
        catch(SQLException e){
        	throw new ConnectorException("Cannot commit update",e);
        }
		return uid;
	}

	public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
		if(attribute.is(Name.NAME)){
			String value = (String) attribute.getValue().get(0);
			return new Name(value.trim().toUpperCase());
		}
		else if(attribute.is(Uid.NAME)){
			String value = (String) attribute.getValue().get(0);
			return new Uid(value.trim().toUpperCase());
		}
		return attribute;
	}
    
    
    
    
  
}
