/**
 * 
 */
package org.identityconnectors.oracle;

import java.sql.*;
import java.util.*;

import org.identityconnectors.common.StringUtil;
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
    static final String ORACLE_EXPIRE_PASSWORD = "expirePassword";



    
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
    
    private String getRequiredStringValue(Set<Attribute> attrs, String name){
        Attribute attr = AttributeUtil.find(name, attrs);
        if(attr == null){
            throw new IllegalArgumentException("No attribute with name  [" + name + "] found in set");
        }
        return AttributeUtil.getStringValue(attr);
    }
    
    
    private String getNotEmptyStringValue(Set<Attribute> attrs, String name){
        String value = getRequiredStringValue(attrs, name);
        if(StringUtil.isEmpty(value)){
            throw new IllegalArgumentException("Attribute with name [" + name + "] is empty");
        }
        return value;
         
    }
    
    private String getStringValue(Set<Attribute> attrs, String name){
        Attribute attr = AttributeUtil.find(name, attrs);
        return attr != null ? AttributeUtil.getStringValue(attr) : null;
    }
    

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        if ( oclass == null || !oclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        String userName = getNotEmptyStringValue(attrs, Name.NAME);
        checkUserNotExist(userName);
        String authentication =  getStringValue(attrs, ORACLE_AUTHENTICATION_ATTR_NAME);
        if(authentication == null){
            authentication = ORACLE_AUTH_LOCAL; 
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("create user \"").append(userName).append("\" identified");
        if(ORACLE_AUTH_LOCAL.equals(authentication)){
            builder.append(" by ");
            GuardedString password = AttributeUtil.getPasswordValue(attrs);
            if(password == null){
                password = new GuardedString(userName.toCharArray());
            }
            password.access(new GuardedString.Accessor(){
                public void access(char[] clearChars) {
                    builder.append("\"").append(clearChars).append("\"");
                }
            });
            Attribute expirePassword = AttributeUtil.find(ORACLE_EXPIRE_PASSWORD, attrs);
            if(expirePassword != null && AttributeUtil.getBooleanValue(expirePassword)){
                builder.append(" password expire");
            }
        }
        else if(ORACLE_AUTH_EXTERNAL.equals(authentication)){
            builder.append(" externally ");
        }
        else if(ORACLE_AUTH_GLOBAL.equals(authentication)){
            builder.append(" globally as ");
            String globalName = getNotEmptyStringValue(attrs, ORACLE_GLOBAL_ATTR_NAME); 
            builder.append('\'').append(globalName).append('\'');
        }
        else{
            throw new IllegalArgumentException("Invalid value of [" + ORACLE_AUTHENTICATION_ATTR_NAME + "] = " + authentication);
        }
        try {
            SQLUtil.executeUpdateStatement(adminConn, builder.toString());
            adminConn.commit();
            log.info("User created : {0}", userName);
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(adminConn);
            throw ConnectorException.wrap(e);
        }
        return new Uid(userName);
    }
    
    private void checkUserNotExist(String user) {
        boolean userExist = userExist(user);
        if(userExist){
            throw new AlreadyExistsException("User " + user + " already exists");
        }
    }
    
    private boolean userExist(String user){
        //Cannot user PreparedStatement, JVM is crashing !
        StringBuilder query = new StringBuilder("select USERNAME from DBA_USERS where USERNAME = ");
        query.append('\'').append(user).append('\'');
        Statement st = null;
        ResultSet rs = null;
        try{
            st = adminConn.createStatement();
            rs = st.executeQuery(query.toString());
            return rs.next(); 
        }
        catch(SQLException e){
            throw new ConnectorException("Cannot test whether user exist",e);
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    List<UserRecord> readUserRecords(List<String> userNames){
        StringBuilder query = new StringBuilder("select * from DBA_USERS where USERNAME in(");
        for(String userName : userNames){
            query.append("\"").append(userName).append("\"");
        }
        query.append(')');
        ResultSet rs = null;
        Statement st = null;
        try{
            st = adminConn.createStatement();
            rs = st.executeQuery(query.toString());
            return null;
        }
        catch(SQLException e){
            throw ConnectorException.wrap(e);
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }

    
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        //TODO add cascade option
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
    
    
    

}
