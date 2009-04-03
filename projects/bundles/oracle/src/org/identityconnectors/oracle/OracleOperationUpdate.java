package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.ORACLE_PRIVS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_ROLES_ATTR_NAME;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.*;

class OracleOperationUpdate extends AbstractOracleOperation implements UpdateOp,UpdateAttributeValuesOp {

    OracleOperationUpdate(OracleConfiguration cfg, Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid update(ObjectClass objclass, Uid uid,  Set<Attribute> attrs, OperationOptions options) {
        checkUserExist(uid.getUidValue());
        OracleUserAttributes caAttributes = new OracleUserAttributes();
        caAttributes.userName = uid.getUidValue();
        new OracleCreateAttributesReader(cfg.getConnectorMessages()).readCreateAuthAttributes(attrs, caAttributes);
        new OracleCreateAttributesReader(cfg.getConnectorMessages()).readCreateRestAttributes(attrs, caAttributes);
        try{
            UserRecord userRecord = new OracleUserReader(adminConn).readUserRecord(caAttributes.userName);
            String alterSQL = new OracleCreateOrAlterStBuilder(cfg.getCSSetup()).buildAlterUserSt(caAttributes, userRecord);
            
            List<String> grantRevokeSQL = new ArrayList<String>();
            List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, attrs), String.class);
            if(!roles.isEmpty()){
            	List<String> currentRoles = new OracleRolePrivReader(adminConn).readRoles(caAttributes.userName);
            	List<String> revokeRoles = new ArrayList<String>(currentRoles);
            	revokeRoles.removeAll(roles);
            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(caAttributes.userName, revokeRoles));
            	roles.removeAll(currentRoles);
            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantRolesSQL(caAttributes.userName, roles));
            }
            List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, attrs), String.class);
            if(!privileges.isEmpty()){
                List<String> currentPrivileges = new OracleRolePrivReader(adminConn).readPrivileges(caAttributes.userName);
                List<String> revokePrivileges = new ArrayList<String>(currentPrivileges);
                revokePrivileges.removeAll(privileges);
                grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokePrivileges(caAttributes.userName, revokePrivileges));
                privileges.removeAll(currentPrivileges);
            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantPrivilegesSQL(caAttributes.userName, privileges));
            }
            
            SQLUtil.executeUpdateStatement(adminConn, alterSQL);
            for(String sql : grantRevokeSQL){
            	SQLUtil.executeUpdateStatement(adminConn, sql);
            }
            adminConn.commit();
            return uid;
        }catch(Exception e){
            SQLUtil.rollbackQuietly(adminConn);
            throw ConnectorException.wrap(e);
        }
    }
    
    private void checkUserExist(String user) {
        boolean userExist = new OracleUserReader(adminConn).userExist(user);
        if(!userExist){
            throw new UnknownUidException(new Uid(user),ObjectClass.ACCOUNT);
        }
    }

    //It makes sense to add roles and privileges only
    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        checkUserExist(uid.getUidValue());
        List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, valuesToAdd), String.class);
        List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, valuesToAdd), String.class);
        List<String> grantRolesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantRolesSQL(uid.getUidValue(), roles);
        List<String> grantPrivilegesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantRolesSQL(uid.getUidValue(), privileges);
        try{
	        for(String grant : grantRolesStatements){
	        	SQLUtil.executeUpdateStatement(adminConn, grant);
	        }
	        for(String grant : grantPrivilegesStatements){
	        	SQLUtil.executeUpdateStatement(adminConn, grant);
	        }
        }
        catch(SQLException e){
            SQLUtil.rollbackQuietly(adminConn);
            throw ConnectorException.wrap(e);
        }
        return uid;
    }

    //It makes sense to remove roles and privileges only
    //It is error to revoke not existing role/privilege from user
    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        checkUserExist(uid.getUidValue());
        List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, valuesToRemove), String.class);
        List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, valuesToRemove), String.class);
        List<String> revokeRolesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(uid.getUidValue(), roles);
        List<String> revokePrivilegesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(uid.getUidValue(), privileges);
        try{
	        for(String revoke : revokeRolesStatements){
	        	SQLUtil.executeUpdateStatement(adminConn, revoke);
	        }
	        for(String revoke : revokePrivilegesStatements){
	        	SQLUtil.executeUpdateStatement(adminConn, revoke);
	        }
        }
        catch(SQLException e){
            SQLUtil.rollbackQuietly(adminConn);
            throw ConnectorException.wrap(e);
        }
        return uid;
    }
    

}


