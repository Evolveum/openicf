package org.identityconnectors.oracle;

import java.sql.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;

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
        OracleConnectorImpl.checkObjectClass(objectClass, cfg.getConnectorMessages());
        new LocalizedAssert(cfg.getConnectorMessages()).assertNotBlank(username, "username");
        new LocalizedAssert(cfg.getConnectorMessages()).assertNotNull(password, "password");
        log.info("Authenticate user: [{0}]", username);
        try{
            final Connection conn = cfg.createUserConnection(username, password);
            log.info("User authenticated : [{0}]",username);
            SQLUtil.closeQuietly(conn);
            return new Uid(username);
        }
        catch(RuntimeException e){
        	log.info("Authentication of user [{0}] failed",username);
            if(e.getCause() instanceof SQLException){
                SQLException sqlE = (SQLException) e.getCause();
                if("72000".equals(sqlE.getSQLState()) && 1017 == sqlE.getErrorCode()){
                	//By contract we must throw PasswordExpiredException when account is expired
                	
                    //Wrong user or password, log it here and rethrow
                    log.info(e,"Oracle.authenticate : Invalid user/passord for user: {0}",username);
                    throw new InvalidCredentialException("Oracle.authenticate :  Invalid user/password",e.getCause());
                }
                else if("99999".equals(sqlE.getSQLState()) && 28000==sqlE.getErrorCode()){
                	InvalidCredentialException icException = new InvalidCredentialException("User account is locked",e);
					throw icException;
                }
                else if("99999".equals(sqlE.getSQLState()) && 28001==sqlE.getErrorCode()){
                	PasswordExpiredException passwordExpiredException = new PasswordExpiredException("Password expired");
                	passwordExpiredException.initUid(new Uid(username));
					throw passwordExpiredException;
                }
            }
            throw e;
        }
    }

}
