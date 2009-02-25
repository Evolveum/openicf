package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.*;

import java.sql.Connection;
import java.util.*;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.CreateOp;

class OracleOperationCreate extends AbstractOracleOperation implements CreateOp{
    
    
    OracleOperationCreate(OracleConfiguration cfg,Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        OracleConnector.checkObjectClass(oclass, cfg.getConnectorMessages());
        String userName = OracleConnectorHelper.getStringValue(attrs, Name.NAME);
        new LocalizedAssert(cfg.getConnectorMessages()).assertNotBlank(userName,Name.NAME);
        checkUserNotExist(userName);
        CreateAlterAttributes caAttributes = new CreateAlterAttributes();
        caAttributes.userName = userName;
        new OracleCreateAttributesReader(cfg.getConnectorMessages()).readCreateAuthAttributes(attrs, caAttributes);
        new OracleCreateAttributesReader(cfg.getConnectorMessages()).readCreateRestAttributes(attrs, caAttributes);
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

    
    private void checkUserNotExist(String user) {
        boolean userExist = new OracleUserReader(adminConn).userExist(user);
        if(userExist){
            throw new AlreadyExistsException("User " + user + " already exists");
        }
    }

}
