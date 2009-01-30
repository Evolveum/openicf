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
    
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        if ( oclass == null || !oclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        String userName = OracleConnectorHelper.getNotEmptyStringValue(attrs, Name.NAME);
        checkUserNotExist(userName);
        CreateAlterAttributes caAttributes = new CreateAlterAttributes();
        caAttributes.userName = userName;
        setCreateAuthAttributes(attrs, caAttributes);
        setCreateRestAttributes(attrs, caAttributes);
        try {
            String createSQL = new OracleCreateOrAlterStBuilder().buildCreateUserSt(caAttributes).toString();
            Attribute roles = AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, attrs);
            Attribute privileges = AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, attrs);
            List<String> privAndRolesSQL = new OracleRolesAndPrivsBuilder()
                    .buildCreate(userName, OracleConnectorHelper.castList(
                            roles, String.class), OracleConnectorHelper
                            .castList(privileges, String.class)); 
            //Now execute create and grant statements
            SQLUtil.executeUpdateStatement(adminConn, createSQL);
            for(String privSQL : privAndRolesSQL){
                SQLUtil.executeUpdateStatement(adminConn, privSQL);
            }
            adminConn.commit();
            log.info("User created : {0}", userName);
        } catch (Exception e) {
            SQLUtil.rollbackQuietly(adminConn);
            throw ConnectorException.wrap(e);
        }
        return new Uid(userName);
    }

    private void setCreateRestAttributes(Set<Attribute> attrs, CreateAlterAttributes caAttributes) {
        caAttributes.expirePassword = OracleConnectorHelper.getBooleanValue(attrs, OperationalAttributes.PASSWORD_EXPIRED_NAME);
        caAttributes.defaultTableSpace = OracleConnectorHelper.getStringValue(attrs, ORACLE_DEF_TS_ATTR_NAME);
        caAttributes.tempTableSpace = OracleConnectorHelper.getStringValue(attrs, ORACLE_TEMP_TS_ATTR_NAME);
        caAttributes.enable = OracleConnectorHelper.getBooleanValue(attrs, OperationalAttributes.ENABLE_NAME);
        caAttributes.profile = OracleConnectorHelper.getStringValue(attrs, ORACLE_PROFILE_ATTR_NAME);
        Attribute defaultTSQuota = AttributeUtil.find(ORACLE_DEF_TS_QUOTA_ATTR_NAME, attrs);
        if(defaultTSQuota != null){
            caAttributes.defaultTSQuota = new Quota(AttributeUtil.getStringValue(defaultTSQuota));
        }
        Attribute tempTSQuota = AttributeUtil.find(ORACLE_TEMP_TS_QUOTA_ATTR_NAME, attrs);
        if(tempTSQuota != null){
            caAttributes.tempTSQuota = new Quota(AttributeUtil.getStringValue(tempTSQuota));
        }
    }

    private void setCreateAuthAttributes(Set<Attribute> attrs, CreateAlterAttributes caAttributes) {
        String authentication =  OracleConnectorHelper.getStringValue(attrs, ORACLE_AUTHENTICATION_ATTR_NAME);
        if(authentication == null){
            authentication = ORACLE_AUTH_LOCAL; 
        }
        if(ORACLE_AUTH_LOCAL.equals(authentication)){
            caAttributes.auth = OracleAuthentication.LOCAL;
            GuardedString password = AttributeUtil.getPasswordValue(attrs);
            if(password == null){
                password = new GuardedString(caAttributes.userName.toCharArray());
            }
            caAttributes.password = password;
        }
        else if(ORACLE_AUTH_EXTERNAL.equals(authentication)){
            caAttributes.auth = OracleAuthentication.EXTERNAL;
        }
        else if(ORACLE_AUTH_GLOBAL.equals(authentication)){
            caAttributes.auth = OracleAuthentication.GLOBAL;
            caAttributes.globalName = OracleConnectorHelper.getNotEmptyStringValue(attrs, ORACLE_GLOBAL_ATTR_NAME);
        }
        else{
            throw new IllegalArgumentException("Invalid value of [" + ORACLE_AUTHENTICATION_ATTR_NAME + "] = " + authentication);
        }
    }
    
    private void checkUserNotExist(String user) {
        boolean userExist = new OracleUserReader(adminConn).userExist(user);
        if(userExist){
            throw new AlreadyExistsException("User " + user + " already exists");
        }
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
    
    

}
