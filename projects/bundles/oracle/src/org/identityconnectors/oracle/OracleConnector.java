/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.Connection;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;

/**
 * @author kitko
 *
 */
@ConnectorClass(configurationClass=OracleConfiguration.class, displayNameKey = "oracle.connector")
public class OracleConnector implements PoolableConnector, AuthenticateOp {
    private Connection adminConn;
    private OracleConfiguration cfg;
    
    public void checkAlive() {
        
    }

    public void dispose() {
        SQLUtil.closeQuietly(adminConn);
    }

    public OracleConfiguration getConfiguration() {
        return cfg;
    }

    public void init(Configuration cfg) {
        this.cfg = (OracleConfiguration) cfg;
        this.adminConn = createAdminConnection();
    }
    
    /**
     * Test of configuration and validity of connection
     */
    public void test() {
        cfg.validate();
        OracleSpecifics.testConnection(adminConn);
    }
    

    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        return null;
    }
    
    private Connection createAdminConnection(){
        return createConnection(cfg.getUser(),cfg.getPassword());
    }

    private Connection createConnection(String user, GuardedString password) {
        // TODO Auto-generated method stub
        return null;
    }
    

}
