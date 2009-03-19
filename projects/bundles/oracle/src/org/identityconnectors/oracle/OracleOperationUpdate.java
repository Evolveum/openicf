package org.identityconnectors.oracle;

import java.sql.Connection;
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
            SQLUtil.executeUpdateStatement(adminConn, alterSQL);
            //TODO alter also schema and privilege 
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
    //If provided same role/privilege user already has, the role/privilege will be skipped
    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        checkUserExist(uid.getUidValue());
        // TODO Auto-generated method stub
        return null;
    }

    //It makes sense to remove roles and privileges only
    //It is error to revoke not existing role/privilege from user
    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        checkUserExist(uid.getUidValue());
        // TODO Auto-generated method stub
        return null;
    }
    

}


