package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.ORACLE_PRIVS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_ROLES_ATTR_NAME;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Alter the user attributes, his roles and privileges
 * @author kitko
 *
 */
final class OracleOperationUpdate extends AbstractOracleOperation implements UpdateOp,UpdateAttributeValuesOp {

    OracleOperationUpdate(OracleConfiguration cfg, Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid update(ObjectClass objclass, Uid uid,  Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> map = AttributeUtil.toMap(attrs);
    	checkUpdateAttributes(map);
        checkUserExist(uid.getUidValue());
        OracleUserAttributes caAttributes = new OracleUserAttributes();
        caAttributes.userName = uid.getUidValue();
        new OracleAttributesReader(cfg.getConnectorMessages()).readAlterAttributes(map, caAttributes);
        try{
            UserRecord userRecord = new OracleUserReader(adminConn).readUserRecord(caAttributes.userName);
            String alterSQL = new OracleCreateOrAlterStBuilder(cfg.getCSSetup(),cfg.getConnectorMessages()).buildAlterUserSt(caAttributes, userRecord);
            
            List<String> grantRevokeSQL = new ArrayList<String>();
            Attribute aRoles = AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, attrs);
            //If we have null or empty roles attribute, revoke all roles
            if(aRoles != null){
				List<String> roles = OracleConnectorHelper.castList(aRoles, String.class);
	            if(!roles.isEmpty()){
	            	List<String> currentRoles = new OracleRolePrivReader(adminConn).readRoles(caAttributes.userName);
	            	List<String> revokeRoles = new ArrayList<String>(currentRoles);
	            	revokeRoles.removeAll(roles);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(caAttributes.userName, revokeRoles));
	            	roles.removeAll(currentRoles);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantRolesSQL(caAttributes.userName, roles));
	            }
	            else{
	            	List<String> currentRoles = new OracleRolePrivReader(adminConn).readRoles(caAttributes.userName);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(caAttributes.userName, currentRoles));
	            }
            }
            
            Attribute aPrivileges = AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, attrs);
            //If we have null or empty privileges attribute, revoke all privileges
            if(aPrivileges != null){
				List<String> privileges = OracleConnectorHelper.castList(aPrivileges, String.class);
	            if(!privileges.isEmpty()){
	                List<String> currentPrivileges = new OracleRolePrivReader(adminConn).readPrivileges(caAttributes.userName);
	                List<String> revokePrivileges = new ArrayList<String>(currentPrivileges);
	                revokePrivileges.removeAll(privileges);
	                grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokePrivileges(caAttributes.userName, revokePrivileges));
	                privileges.removeAll(currentPrivileges);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantPrivilegesSQL(caAttributes.userName, privileges));
	            }
	            else{
	                List<String> currentPrivileges = new OracleRolePrivReader(adminConn).readPrivileges(caAttributes.userName);
	                grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokePrivileges(caAttributes.userName, currentPrivileges));
	            }
            }
            if(alterSQL == null && grantRevokeSQL.isEmpty()){
            	//This is dummy update with not DDL , is it valid ?
            	//yes, if we e.g update roles to same roles, no ddl will be generated
            }
            if(alterSQL != null){
            	SQLUtil.executeUpdateStatement(adminConn, alterSQL);
            }
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
        checkAddAttributes(valuesToAdd);
    	checkUserExist(uid.getUidValue());
        List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, valuesToAdd), String.class);
        List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, valuesToAdd), String.class);
        List<String> grantRolesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantRolesSQL(uid.getUidValue(), roles);
        List<String> grantPrivilegesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantPrivilegesSQL(uid.getUidValue(), privileges);
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
    	checkRemoveAttributes(valuesToRemove);
        checkUserExist(uid.getUidValue());
        List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, valuesToRemove), String.class);
        List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, valuesToRemove), String.class);
        List<String> revokeRolesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(uid.getUidValue(), roles);
        List<String> revokePrivilegesStatements = new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokePrivileges(uid.getUidValue(), privileges);
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
    
    private void checkUpdateAttributes(Map<String, Attribute> map) {
    	LocalizedAssert la = new LocalizedAssert(cfg.getConnectorMessages());
		for(Attribute attr : map.values()){
			if(attr.is(Name.NAME)){
			}
			else if(attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)){
				la.assertNotNull(AttributeUtil.getBooleanValue(attr), OperationalAttributes.PASSWORD_EXPIRED_NAME);
				//we can 'unexpire' password only if new password is provided
				//We cannot use password equals to name
				if(Boolean.FALSE.equals(AttributeUtil.getSingleValue(attr))){
					Attribute password = map.get(OperationalAttributes.PASSWORD_NAME);
					if(password == null || AttributeUtil.getGuardedStringValue(password) == null){
						throw new IllegalArgumentException("No password specified when unexpiring passord");
					}
				}
			}
			else if(attr.is(OperationalAttributes.ENABLE_NAME)){
			}
			else if(attr.is(OperationalAttributes.PASSWORD_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_DEF_TS_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_GLOBAL_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_PROFILE_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_TEMP_TS_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_TEMP_TS_QUOTA_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_PRIVS_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_ROLES_ATTR_NAME)){
			}
			else{
				throw new IllegalArgumentException("Illegal argument " + attr);
			}
		}
	}

    private void checkAddAttributes(Set<Attribute> attrs) {
		for(Attribute attr : attrs){
			if(attr.is(OracleConnector.ORACLE_PRIVS_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_ROLES_ATTR_NAME)){
			}
			else{
				throw new IllegalArgumentException("Illegal argument " + attr);
			}
		}
	}
    
    private void checkRemoveAttributes(Set<Attribute> attrs) {
		for(Attribute attr : attrs){
			if(attr.is(OracleConnector.ORACLE_PRIVS_ATTR_NAME)){
			}
			else if(attr.is(OracleConnector.ORACLE_ROLES_ATTR_NAME)){
			}
			else{
				throw new IllegalArgumentException("Illegal argument " + attr);
			}
		}
	}

}


