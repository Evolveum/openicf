package org.identityconnectors.oracle;

import java.sql.Connection;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.UpdateOp;

class OracleOperationUpdate extends AbstractOracleOperation implements UpdateOp {

    OracleOperationUpdate(OracleConfiguration cfg, Connection adminConn, Log log) {
        super(cfg, adminConn, log);
    }

    public Uid update(ObjectClass objclass, Uid uid,  Set<Attribute> attrs, OperationOptions options) {
        checkUserExist(uid.getUidValue());
        CreateAlterAttributes caAttributes = new CreateAlterAttributes();
        caAttributes.userName = uid.getUidValue();
        new OracleCreateAttributesReader(cfg.getConnectorMessages()).readCreateAuthAttributes(attrs, caAttributes);
        new OracleCreateAttributesReader(cfg.getConnectorMessages()).readCreateRestAttributes(attrs, caAttributes);
        try{
            UserRecord userRecord = new OracleUserReader(adminConn).readUserRecord(caAttributes.userName);
            String alterSQL = new OracleCreateOrAlterStBuilder(cfg.getCaseSensitivity()).buildAlterUserSt(caAttributes, userRecord);
            SQLUtil.executeUpdateStatement(adminConn, alterSQL);
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
    

}


