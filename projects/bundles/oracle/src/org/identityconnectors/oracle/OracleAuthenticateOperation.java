package org.identityconnectors.oracle;

import java.sql.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;

class OracleAuthenticateOperation extends AbstractOracleOperation implements AuthenticateOp{
    
    
    OracleAuthenticateOperation(OracleConfiguration cfg, Log log) {
        super(cfg, log);
    }

    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        OracleConnector.checkObjectClass(objectClass, cfg.getConnectorMessages());
        try{
            final Connection conn = cfg.createConnection(username, password);
            SQLUtil.closeQuietly(conn);
            return new Uid(username);
        }
        catch(RuntimeException e){
            if(e.getCause() instanceof SQLException){
                SQLException sqlE = (SQLException) e.getCause();
                if("72000".equals(sqlE.getSQLState())){
                    //Wrong user or password, log it here and rethrow
                    log.info(e,"Oracle.authenticate : Invalid user/passord for user: {0}",username);
                    throw new InvalidCredentialException("Oracle.authenticate :  Invalid user/password",e.getCause());
                }
            }
            throw e;
        }
    }

}
