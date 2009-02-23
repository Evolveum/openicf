package org.identityconnectors.oracle;

import java.sql.Connection;

import org.identityconnectors.common.logging.Log;

class AbstractOracleOperation {
    protected Connection adminConn;
    protected Log log;
    protected OracleConfiguration cfg;
    
    AbstractOracleOperation(OracleConfiguration cfg,Connection adminConn, Log log) {
        super();
        this.cfg = cfg;
        this.adminConn = adminConn;
        this.log = log;
    }
    
    AbstractOracleOperation(OracleConfiguration cfg, Log log) {
        super();
        this.cfg = cfg;
        this.log = log;
    }
    
    
    
    

}
