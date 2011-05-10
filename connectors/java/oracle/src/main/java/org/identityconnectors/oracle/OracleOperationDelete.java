/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import static org.identityconnectors.oracle.OracleMessages.*;

/**
 * Oracle delete operation drops oracle user
 * @author kitko
 *
 */
final class OracleOperationDelete extends AbstractOracleOperation implements DeleteOp{
    private final static Log log = Log.getLog(OracleOperationDelete.class);
    
    OracleOperationDelete(OracleConfiguration cfg, Connection adminConn) {
        super(cfg, adminConn);
    }


    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        OracleConnectorHelper.checkObjectClass(objClass, cfg.getConnectorMessages());
        if(cfg.isDropCascade()){
        	log.info("Deleting user cascade: [{0}]", uid.getUidValue());
        }
        else{
        	log.info("Deleting user noncascade: [{0}]", uid.getUidValue());
        }
        String userName = uid.getUidValue();
        String sql = "drop user " + cfg.getCSSetup().formatToken(OracleUserAttribute.USER,userName);
        if(cfg.isDropCascade()){
        	sql = sql + " CASCADE";
        }
        Statement st = null;
        try{
            st = adminConn.createStatement();
            st.executeUpdate(sql);
            adminConn.commit();
            log.info("User deleted : [{0}]", uid.getUidValue());
        }
        catch(SQLException e){
            SQLUtil.rollbackQuietly(adminConn);
            if("42000".equals(e.getSQLState()) && 1918==e.getErrorCode()){
                throw new UnknownUidException(uid,ObjectClass.ACCOUNT);
            }
            throw new ConnectorException(cfg.getConnectorMessages().format(MSG_DELETE_OF_USER_FAILED,null,uid.getUidValue()),e);
        }
        finally{
            SQLUtil.closeQuietly(st);
        }
    }

}
