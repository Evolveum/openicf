/**
 *
 */
package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.MSG_DELETE_OF_USER_FAILED;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.DeleteOp;

/**
 * Oracle delete operation drops oracle user.
 *
 * @author kitko
 *
 */
final class OracleOperationDelete extends AbstractOracleOperation implements DeleteOp {
    private final static Log LOG = Log.getLog(OracleOperationDelete.class);

    OracleOperationDelete(OracleConfiguration cfg, Connection adminConn) {
        super(cfg, adminConn);
    }

    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        OracleConnectorHelper.checkObjectClass(objClass, cfg.getConnectorMessages());
        if (cfg.isDropCascade()) {
            LOG.info("Deleting user cascade: [{0}]", uid.getUidValue());
        } else {
            LOG.info("Deleting user noncascade: [{0}]", uid.getUidValue());
        }
        String userName = uid.getUidValue();
        String sql =
                "drop user " + cfg.getCSSetup().formatToken(OracleUserAttribute.USER, userName);
        if (cfg.isDropCascade()) {
            sql = sql + " CASCADE";
        }
        Statement st = null;
        try {
            st = adminConn.createStatement();
            st.executeUpdate(sql);
            adminConn.commit();
            LOG.info("User deleted : [{0}]", uid.getUidValue());
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(adminConn);
            if ("42000".equals(e.getSQLState()) && 1918 == e.getErrorCode()) {
                throw new UnknownUidException(uid, ObjectClass.ACCOUNT);
            }
            throw new ConnectorException(cfg.getConnectorMessages().format(
                    MSG_DELETE_OF_USER_FAILED, null, uid.getUidValue()), e);
        } finally {
            SQLUtil.closeQuietly(st);
        }
    }

}
