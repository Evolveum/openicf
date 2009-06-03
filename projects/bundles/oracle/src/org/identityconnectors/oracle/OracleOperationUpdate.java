package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.MSG_ADDATTRIBUTEVALUES_ATTRIBUTE_NOT_SUPPORTED;
import static org.identityconnectors.oracle.OracleMessages.MSG_ADDATTRIBUTEVALUES_FOR_USER_FAILED;
import static org.identityconnectors.oracle.OracleMessages.MSG_MUST_SPECIFY_PASSWORD_FOR_UNEXPIRE;
import static org.identityconnectors.oracle.OracleMessages.MSG_REMOVEATTRIBUTEVALUES_ATTRIBUTE_NOT_SUPPORTED;
import static org.identityconnectors.oracle.OracleMessages.MSG_REMOVEATTRIBUTEVALUES_FOR_USER_FAILED;
import static org.identityconnectors.oracle.OracleMessages.MSG_UPDATE_ATTRIBUTE_NOT_SUPPORTED;
import static org.identityconnectors.oracle.OracleMessages.MSG_UPDATE_NO_ATTRIBUTES;
import static org.identityconnectors.oracle.OracleMessages.MSG_UPDATE_OF_USER_FAILED;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
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

	private static final Collection<String> VALID_UPDATE_ATTRIBUTES;
	
	static {
		SortedSet<String> tmp = new TreeSet<String>(OracleConnectorHelper.getAttributeNamesComparator());
		tmp.addAll(OracleConstants.ALL_ATTRIBUTE_NAMES);
		tmp.removeAll(Arrays.asList(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,OperationalAttributes.DISABLE_DATE_NAME));
		VALID_UPDATE_ATTRIBUTES = Collections.unmodifiableCollection(tmp);
	}
	
	
    OracleOperationUpdate(OracleConfiguration cfg, Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid update(ObjectClass objclass, Uid uid,  Set<Attribute> attrs, OperationOptions options) {
        Map<String, Attribute> map = AttributeUtil.toMap(attrs);
    	checkUpdateAttributes(map);
        checkUserExist(uid.getUidValue());
        log.info("Update user : [{0}]", uid.getUidValue());
        OracleUserAttributes.Builder builder = new OracleUserAttributes.Builder();
        builder.setUserName(uid.getUidValue());
        new OracleAttributesReader(cfg.getConnectorMessages()).readAlterAttributes(map, builder);
        OracleUserAttributes caAttributes = builder.build();
        try{
            UserRecord userRecord = new OracleUserReader(adminConn,cfg.getConnectorMessages()).readUserRecord(caAttributes.getUserName());
            String alterSQL = new OracleCreateOrAlterStBuilder(cfg).buildAlterUserSt(caAttributes, userRecord);
            List<String> grantRevokeSQL = new ArrayList<String>();
            Attribute aRoles = AttributeUtil.find(OracleConstants.ORACLE_ROLES_ATTR_NAME, attrs);
            //If we have null or empty roles attribute, revoke all roles
            if(aRoles != null){
				List<String> roles = OracleConnectorHelper.castList(aRoles, String.class);
	            if(!roles.isEmpty()){
	            	List<String> currentRoles = new OracleRolePrivReader(adminConn).readRoles(caAttributes.getUserName());
	            	List<String> revokeRoles = new ArrayList<String>(currentRoles);
	            	revokeRoles.removeAll(roles);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(caAttributes.getUserName(), revokeRoles));
	            	roles.removeAll(currentRoles);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantRolesSQL(caAttributes.getUserName(), roles));
	            }
	            else{
	            	List<String> currentRoles = new OracleRolePrivReader(adminConn).readRoles(caAttributes.getUserName());
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokeRoles(caAttributes.getUserName(), currentRoles));
	            }
            }
            
            Attribute aPrivileges = AttributeUtil.find(OracleConstants.ORACLE_PRIVS_ATTR_NAME, attrs);
            //If we have null or empty privileges attribute, revoke all privileges
            if(aPrivileges != null){
				List<String> privileges = OracleConnectorHelper.castList(aPrivileges, String.class);
	            if(!privileges.isEmpty()){
	                List<String> currentPrivileges = new OracleRolePrivReader(adminConn).readPrivileges(caAttributes.getUserName());
	                List<String> revokePrivileges = new ArrayList<String>(currentPrivileges);
	                revokePrivileges.removeAll(privileges);
	                grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokePrivileges(caAttributes.getUserName(), revokePrivileges));
	                privileges.removeAll(currentPrivileges);
	            	grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildGrantPrivilegesSQL(caAttributes.getUserName(), privileges));
	            }
	            else{
	                List<String> currentPrivileges = new OracleRolePrivReader(adminConn).readPrivileges(caAttributes.getUserName());
	                grantRevokeSQL.addAll(new OracleRolesAndPrivsBuilder(cfg.getCSSetup()).buildRevokePrivileges(caAttributes.getUserName(), currentPrivileges));
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
            log.info("User updated : [{0}]", uid.getUidValue());
            return uid;
        }catch(Exception e){
            SQLUtil.rollbackQuietly(adminConn);
            throw new ConnectorException(cfg.getConnectorMessages().format(MSG_UPDATE_OF_USER_FAILED, null, uid.getUidValue()), e);
        }
    }
    
    private void checkUserExist(String user) {
        boolean userExist = new OracleUserReader(adminConn,cfg.getConnectorMessages()).userExist(user);
        if(!userExist){
            throw new UnknownUidException(new Uid(user),ObjectClass.ACCOUNT);
        }
    }

    //It makes sense to add roles and privileges only
    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
    	Map<String, Attribute> map = AttributeUtil.toMap(valuesToAdd);
        checkAddAttributes(map);
    	checkUserExist(uid.getUidValue());
        List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(OracleConstants.ORACLE_ROLES_ATTR_NAME, valuesToAdd), String.class);
        List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(OracleConstants.ORACLE_PRIVS_ATTR_NAME, valuesToAdd), String.class);
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
        catch(Exception e){
            SQLUtil.rollbackQuietly(adminConn);
            throw new ConnectorException(cfg.getConnectorMessages().format(MSG_ADDATTRIBUTEVALUES_FOR_USER_FAILED, null, uid.getUidValue()), e);
        }
        return uid;
    }

    //It makes sense to remove roles and privileges only
    //It is error to revoke not existing role/privilege from user
    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
    	Map<String, Attribute> map = AttributeUtil.toMap(valuesToRemove);
    	checkRemoveAttributes(map);
        checkUserExist(uid.getUidValue());
        List<String> roles = OracleConnectorHelper.castList(AttributeUtil.find(OracleConstants.ORACLE_ROLES_ATTR_NAME, valuesToRemove), String.class);
        List<String> privileges = OracleConnectorHelper.castList(AttributeUtil.find(OracleConstants.ORACLE_PRIVS_ATTR_NAME, valuesToRemove), String.class);
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
        catch(Exception e){
            SQLUtil.rollbackQuietly(adminConn);
            throw new ConnectorException(cfg.getConnectorMessages().format(MSG_REMOVEATTRIBUTEVALUES_FOR_USER_FAILED, null, uid.getUidValue()), e);
        }
        return uid;
    }
    
    private void checkUpdateAttributes(Map<String, Attribute> map) {
    	checkNoAttributes(map);
    	LocalizedAssert la = new LocalizedAssert(cfg.getConnectorMessages());
		for(Attribute attr : map.values()){
			//No need to call Attribute.is, set is caseinsensitive  
			if(!VALID_UPDATE_ATTRIBUTES.contains(attr.getName())){
				throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_UPDATE_ATTRIBUTE_NOT_SUPPORTED, null, attr.getName()));
			}
			if(attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)){
				la.assertNotNull(AttributeUtil.getBooleanValue(attr), OperationalAttributes.PASSWORD_EXPIRED_NAME);
				//we can 'unexpire' password only if new password is provided
				//We cannot use password equals to name
				if(Boolean.FALSE.equals(AttributeUtil.getSingleValue(attr))){
					Attribute password = map.get(OperationalAttributes.PASSWORD_NAME);
					if(password == null || AttributeUtil.getGuardedStringValue(password) == null){
						throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_MUST_SPECIFY_PASSWORD_FOR_UNEXPIRE,null));
					}
				}
			}
		}
	}

    private void checkNoAttributes(Map<String, Attribute> map) {
		//Is it really error to call update with no attributes ? Probably application should not issue dummy calls
		if(map.isEmpty()){
			throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_UPDATE_NO_ATTRIBUTES, null));
		}
	}

	private void checkAddAttributes(Map<String, Attribute> map) {
		checkNoAttributes(map);
		for(Attribute attr : map.values()){
			if(attr.is(OracleConstants.ORACLE_PRIVS_ATTR_NAME)){
			}
			else if(attr.is(OracleConstants.ORACLE_ROLES_ATTR_NAME)){
			}
			else{
				throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_ADDATTRIBUTEVALUES_ATTRIBUTE_NOT_SUPPORTED, null, attr.getName()));
			}
		}
	}
    
    private void checkRemoveAttributes(Map<String, Attribute> map) {
    	checkNoAttributes(map);
		for(Attribute attr : map.values()){
			if(attr.is(OracleConstants.ORACLE_PRIVS_ATTR_NAME)){
			}
			else if(attr.is(OracleConstants.ORACLE_ROLES_ATTR_NAME)){
			}
			else{
				throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_REMOVEATTRIBUTEVALUES_ATTRIBUTE_NOT_SUPPORTED, null, attr.getName()));
			}
		}
	}

}


