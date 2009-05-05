package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.*;

import java.sql.Connection;
import java.util.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.CreateOp;

/**
 * Oracle create operation. 
 * It builds create sql using OracleCreateOrAlterStBuilder and builds grants using OracleRolesAndPrivsBuilder 
 * @author kitko
 *
 */
final class OracleOperationCreate extends AbstractOracleOperation implements CreateOp{
    
    
    OracleOperationCreate(OracleConfiguration cfg,Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        OracleConnector.checkObjectClass(oclass, cfg.getConnectorMessages());
        Map<String, Attribute> map = AttributeUtil.toMap(attrs);
        String userName = OracleConnectorHelper.getStringValue(map, Name.NAME);
        new LocalizedAssert(cfg.getConnectorMessages()).assertNotBlank(userName,Name.NAME);
        checkCreateAttributes(map);
        checkUserNotExist(userName);
        OracleUserAttributes.Builder builder = new OracleUserAttributes.Builder();
        builder.setUserName(userName);
        new OracleAttributesReader(cfg.getConnectorMessages()).readCreateAttributes(map, builder);
        OracleUserAttributes caAttributes = builder.build();
        String createSQL = new OracleCreateOrAlterStBuilder(cfg.getCSSetup(),cfg.getConnectorMessages()).buildCreateUserSt(caAttributes).toString();
        if(createSQL == null){
        	//This should not happen, but be more deffensive
        	throw new ConnectorException("No create SQL generated, probably not enough attributes");
        }
        Attribute roles = AttributeUtil.find(ORACLE_ROLES_ATTR_NAME, attrs);
        Attribute privileges = AttributeUtil.find(ORACLE_PRIVS_ATTR_NAME, attrs);
        List<String> rolesSQL = new OracleRolesAndPrivsBuilder(cfg.getCSSetup())
                .buildGrantRolesSQL(userName, OracleConnectorHelper.castList(
                        roles, String.class)); 
        List<String> privilegesSQL = new OracleRolesAndPrivsBuilder(cfg.getCSSetup())
        .buildGrantPrivilegesSQL(userName, OracleConnectorHelper.castList(
                privileges, String.class)); 
        try {
            //Now execute create and grant statements
            SQLUtil.executeUpdateStatement(adminConn, createSQL);
            for(String sql : rolesSQL){
                SQLUtil.executeUpdateStatement(adminConn, sql);
            }
            for(String sql : privilegesSQL){
                SQLUtil.executeUpdateStatement(adminConn, sql);
            }
            adminConn.commit();
            log.info("User created : [{0}]", userName);
        } catch (Exception e) {
            SQLUtil.rollbackQuietly(adminConn);
            throw ConnectorException.wrap(e);
        }
        return new Uid(userName);
    }

    
    private void checkCreateAttributes(Map<String, Attribute> map) {
		for(Attribute attr : map.values()){
			if(attr.is(Name.NAME)){
			}
			else if(attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)){
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

	private void checkUserNotExist(String user) {
        boolean userExist = new OracleUserReader(adminConn).userExist(user);
        if(userExist){
            throw new AlreadyExistsException("User " + user + " already exists");
        }
    }

}
