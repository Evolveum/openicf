/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.*;
import java.util.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;

/**
 * @author kitko
 *
 */
@ConnectorClass(configurationClass=OracleConfiguration.class, displayNameKey = "oracle.connector")
public class OracleConnector implements PoolableConnector, AuthenticateOp,CreateOp,DeleteOp {
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
        try{
            final Connection conn = createConnection(username, password);
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
    
    private Connection createAdminConnection(){
        return createConnection(cfg.getUser(),cfg.getPassword());
    }

    private Connection createConnection(String user, GuardedString password) {
        return cfg.createConnection(user, password);
    }
    

    
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        //Currently IDM pass null for options parameter. So there is no way how to decide
        //whether we will do cascade or noCascade delete
        String userName = uid.getUidValue();
        String sql = "drop user \"" + userName + "\"";
        Statement st = null;
        try{
            st = adminConn.createStatement();
            st.executeUpdate(sql);
            adminConn.commit();
        }
        catch(SQLException e){
            SQLUtil.rollbackQuietly(adminConn);
            if("42000".equals(e.getSQLState())){
                throw new UnknownUidException(uid,ObjectClass.ACCOUNT);
            }
        }
        finally{
            SQLUtil.closeQuietly(st);
        }
    }
    
    Connection getAdminConnection(){
        return adminConn;
    }
    
    static Log getLog(){
        return log;
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        return new OracleCreateOperation(adminConn, log).create(oclass, attrs, options);
    }
    
    

}
