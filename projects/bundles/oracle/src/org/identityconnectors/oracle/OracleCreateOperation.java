package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.*;

import java.sql.Connection;
import java.util.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.CreateOp;

class OracleCreateOperation extends AbstractOracleOperation implements CreateOp{
    
    
    OracleCreateOperation(OracleConfiguration cfg,Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        OracleConnector.checkObjectClass(oclass, cfg.getConnectorMessages());
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
                    .buildCreateSQL(userName, OracleConnectorHelper.castList(
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

}
