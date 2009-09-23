package org.identityconnectors.oracle;

import java.sql.*;

/** Abstract operation for OracleConnector like Create,Update.
 *  We will keep operations in separate classes.
 * @author kitko
 *
 */
abstract class AbstractOracleOperation {
    protected final Connection adminConn;
    protected final OracleConfiguration cfg;
    
    AbstractOracleOperation(OracleConfiguration cfg,Connection adminConn) {
        super();
        this.cfg = OracleConnectorHelper.assertNotNull(cfg, "cfg");
        this.adminConn = OracleConnectorHelper.assertNotNull(adminConn, "adminConn");
    }
    
    
    

}
