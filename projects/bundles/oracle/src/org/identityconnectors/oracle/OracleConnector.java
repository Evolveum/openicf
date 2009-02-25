/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.Connection;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;

/**
 * @author kitko
 *
 */
@ConnectorClass(configurationClass=OracleConfiguration.class,
        displayNameKey = "oracle.connector",
        messageCatalogPaths={"org/identityconnectors/dbcommon/Messages","org/identityconnectors/oracle/Messages"})
public class OracleConnector implements PoolableConnector, AuthenticateOp,CreateOp,DeleteOp,UpdateOp {
    private Connection adminConn;
    private OracleConfiguration cfg;
    private final static Log log = Log.getLog(OracleConnector.class);
    
    static final String ORACLE_AUTHENTICATION_ATTR_NAME = "oracleAuthentication";
    static final String ORACLE_AUTH_LOCAL = "LOCAL";
    static final String ORACLE_AUTH_EXTERNAL = "EXTERNAL";
    static final String ORACLE_AUTH_GLOBAL = "GLOBAL";
    static final String NO_CASCADE = "noCascade";
    static final String ORACLE_GLOBAL_ATTR_NAME = "oracleGlobalName";
    static final String ORACLE_ROLES_ATTR_NAME = "oracleRoles";
    static final String ORACLE_PRIVS_ATTR_NAME = "oraclePrivs";
    static final String ORACLE_PROFILE_ATTR_NAME = "oracleProfile";
    static final String ORACLE_DEF_TS_ATTR_NAME = "oracleDefaultTS";
    static final String ORACLE_TEMP_TS_ATTR_NAME = "oracleTempTS";
    static final String ORACLE_DEF_TS_QUOTA_ATTR_NAME = "oracleDefaultTSQuota";
    static final String ORACLE_TEMP_TS_QUOTA_ATTR_NAME = "oracleTempTSQuota";
    
    
    
    public void checkAlive() {
        OracleSpecifics.testConnection(adminConn);
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
        return new OracleOperationAuthenticate(cfg, log).authenticate(objectClass, username, password, options);
    }
    
    private Connection createAdminConnection(){
        return cfg.createConnection(cfg.getUser(), cfg.getPassword());
    }

    
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        new OracleOperationDelete(cfg, adminConn, log).delete(objClass, uid, options);
    }
    
    Connection getAdminConnection(){
        return adminConn;
    }
    
    static Log getLog(){
        return log;
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        return new OracleOperationCreate(cfg, adminConn, log).create(oclass, attrs, options);
    }
    
    static void checkObjectClass(ObjectClass objectClass,ConnectorMessages messages){
        if(!ObjectClass.ACCOUNT.equals(objectClass)){
            throw new IllegalArgumentException("Invalid obejct class");
        }
    }

    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        return new OracleOperationUpdate(cfg, adminConn, log).update(objclass, uid, attrs, options);
    }
    
    

}
