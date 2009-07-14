package org.identityconnectors.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.oracle.OracleConfiguration.ConnectionType;

/** Authenticate operation.
 *  It just tries to create new jdbc connection with passed user/password
 * @author kitko
 *
 */
final class OracleOperationAuthenticate extends AbstractOracleOperation implements AuthenticateOp{
    
    
    OracleOperationAuthenticate(OracleConfiguration cfg,Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        OracleConnectorHelper.checkObjectClass(objectClass, cfg.getConnectorMessages());
        new LocalizedAssert(cfg.getConnectorMessages()).assertNotBlank(username, "username");
        new LocalizedAssert(cfg.getConnectorMessages()).assertNotNull(password, "password");
        if(options != null && Boolean.TRUE.equals(options.getOptions().get("returnUidOnly"))){
        	return findUserByName(username);
        }
        log.info("Authenticate user: [{0}]", username);
        Connection conn = null;
        try{
            conn = cfg.createUserConnection(username, password);
        }
        catch(RuntimeException e){
        	log.info("Authentication of user [{0}] failed", username);
            if(e.getCause() instanceof SQLException){
                SQLException sqlE = (SQLException) e.getCause();
                if(StringUtil.isBlank(sqlE.getSQLState())){
                	handleNotCompletedSQLEXception(e, sqlE, username, password);
                }
                else{
                	handleSQLException(e, sqlE, username, password);
                }
            }
            throw e;
        }
        //When we get connection from DS, test the connection
        try{
	        if(ConnectionType.DATASOURCE.equals(cfg.getConnType())){
	        	doExtraConnectionTest(username, conn);
	        	killDSConnection(conn);
	        }
        }
       	finally{
       		SQLUtil.closeQuietly(conn);
        }
        log.info("User authenticated : [{0}]",username);
        return new Uid(username);
    }

	private void killDSConnection(Connection conn) {
		try {
			//Here we will kill the session  on oracle to not pool the connection
			OracleSpecifics.killConnection(adminConn, conn);
		} catch (SQLException e) {
			throw new IllegalStateException("Cannot kill the getConnection retrieved from DS", e);
		}
		//And now force the usage of the connection, which should hint the app server to discard the connection from pool
		try{
			OracleSpecifics.testConnection(conn);
			//If we get here, we could have security hole, because next connection from DS will not check password (will return cached connection)
			//throwing the exception we will force the admin to fix the problem
			throw new IllegalArgumentException("Connection from DS not killed");
		}
		catch(Exception e){
		    //Expected, the connection test should not succeed  
		}
	}

	private Uid findUserByName(String username) {
		if(new OracleUserReader(adminConn, cfg.getConnectorMessages()).userExist(username)){
			return new Uid(username);
		}
		throw new InvalidCredentialException(cfg.getConnectorMessages().format(OracleMessages.MSG_CANNOT_FIND_USER, null, username));
	}

	/** Maybe we do not need this method, because now ds connection are killed.
	 *  But there could be some other reason why the connections can remain/be initialized in the pool  
	 */
	private void doExtraConnectionTest(String username, Connection conn) {
		try{
        	OracleSpecifics.testConnection(conn);
        }
        catch(RuntimeException e){
        	//This should not happen
        	throw new ConnectorException("Error testing connection after succesufull authenticate", e);
        }
        //Now imagine the case we get connection from pool (key is user/password), but the user is already expired or locked.
        //Then we will get connection, that is open and we cannot find out that user is locked
        //The solution would be , that datasource pool would not cache connections retrieved by ds.getConnection(user, password)
        //But this is not configurable
        //so we will look at DBA_USERS view to find the state of user
        try {
			UserRecord userRecord = new OracleUserReader(adminConn, cfg.getConnectorMessages()).readUserRecord(username);
			if(userRecord == null){
				throw new ConnectorException(MessageFormat.format("Cannot find userRecord for user [{0}] at authenticate, probably user is deleted",username));
			}
			if(StringUtil.isBlank(userRecord.getStatus())){
				//should not happen
				throw new ConnectorException(MessageFormat.format("userRecord.getStatus() is blank for user [{0}]", username));
			}
			if(OracleUserReader.isUserLocked(userRecord)){
	        	throw new InvalidCredentialException("User account is locked");
			}
			else if(OracleUserReader.isPasswordExpired(userRecord)){
	        	PasswordExpiredException passwordExpiredException = new PasswordExpiredException("Password expired");
	        	passwordExpiredException.initUid(new Uid(username));
				throw passwordExpiredException;
			}
			//http://www.dbforums.com/oracle/1617629-account_status-dba_users.html
			//We will not look at other values 
		} catch (SQLException e) {
			throw new ConnectorException(MessageFormat.format("Cannot find userRecord for user [{0}] at authenticate",username) , e);
		}
	}

	private void handleSQLException(RuntimeException e, SQLException sqlE, String username, GuardedString password) {
        if("72000".equals(sqlE.getSQLState()) && 1017 == sqlE.getErrorCode()){
        	//By contract we must throw PasswordExpiredException when account is expired
            //Wrong user or password, log it here and rethrow
            log.info(sqlE, "Oracle.authenticate : Invalid user/passord for user: {0}", username);
            throw new InvalidCredentialException("Oracle.authenticate :  Invalid user/password", sqlE);
        }
        else if("99999".equals(sqlE.getSQLState()) && 28000==sqlE.getErrorCode()){
        	InvalidCredentialException icException = new InvalidCredentialException("User account is locked", sqlE);
			throw icException;
        }
        else if("99999".equals(sqlE.getSQLState()) && 28001==sqlE.getErrorCode()){
        	PasswordExpiredException passwordExpiredException = new PasswordExpiredException("Password expired", sqlE);
        	passwordExpiredException.initUid(new Uid(username));
			throw passwordExpiredException;
        }
        throw e;
	}

	private void handleNotCompletedSQLEXception(RuntimeException e, SQLException sqlE, String username, GuardedString password) {
    	//If we get exception without sql state, we must try to look at the message of exception.
		//Status of user in DBA_USERS view could also help, but the real cause of exception can be absolutely different
    	String msg = sqlE.getMessage();
    	if(StringUtil.isBlank(msg)){
    		//here we cannot do anything to determine the cause, just throw the original wrapper
    		throw e;
    	}
    	if(msg.contains("ORA-01017")){
            log.info(sqlE, "Oracle.authenticate : Invalid user/passord for user: {0}", username);
            throw new InvalidCredentialException("Oracle.authenticate :  Invalid user/password", sqlE);
    	}
        else if(msg.contains("ORA-28000")){
        	InvalidCredentialException icException = new InvalidCredentialException("User account is locked", sqlE);
			throw icException;
        }
    	else if(msg.contains("ORA-28001")){
        	PasswordExpiredException passwordExpiredException = new PasswordExpiredException("Password expired", sqlE);
        	passwordExpiredException.initUid(new Uid(username));
			throw passwordExpiredException;
    	}
    	throw e;
	}

}
