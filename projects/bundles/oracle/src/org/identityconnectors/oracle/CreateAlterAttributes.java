package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/**
 * Helper structure for creating/altering user
 * @author kitko
 *
 */
class CreateAlterAttributes implements Cloneable{
    Operation operation;
    String userName;
    OracleAuthentication auth;
    GuardedString password;
    String globalName;
    Boolean expirePassword;
    Boolean enable;
    String defaultTableSpace;
    String tempTableSpace;
    String profile;
    Quota defaultTSQuota;
    Quota tempTSQuota;
    
    protected CreateAlterAttributes clone(){
        try {
            return (CreateAlterAttributes) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ConnectorException("Cannot clone CreateAlterAttributes",e);
        }
    }
    
}

class Quota{
    String size;

    Quota(String size) {
        super();
        this.size = size;
    }
    Quota(){}
}

enum OracleAuthentication {
    LOCAL,
    EXTERNAL,
    GLOBAL;
}

enum Operation{
    CREATE,
    ALTER;
}