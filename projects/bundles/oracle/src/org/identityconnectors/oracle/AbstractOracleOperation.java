package org.identityconnectors.oracle;

import java.sql.Connection;

import org.identityconnectors.common.logging.Log;

/** Abstract operation for OracleConnector like Create,Update.
 *  We will keep operations in separate classes.
 * @author kitko
 *
 */
abstract class AbstractOracleOperation {
    protected Connection adminConn;
    protected Log log;
    protected OracleConfiguration cfg;
    
    AbstractOracleOperation(OracleConfiguration cfg,Connection adminConn, Log log) {
        super();
        this.cfg = OracleConnectorHelper.assertNotNull(cfg, "cfg");
        this.adminConn = OracleConnectorHelper.assertNotNull(adminConn, "adminConn");
        this.log = OracleConnectorHelper.assertNotNull(log, "log");
    }
    
    
    

}
