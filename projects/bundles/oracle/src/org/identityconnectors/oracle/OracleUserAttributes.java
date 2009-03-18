package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/**
 * Helper structure for creating/altering user
 * @author kitko
 *
 */
class OracleUserAttributes implements Cloneable{
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
    
    protected OracleUserAttributes clone(){
        try {
            return (OracleUserAttributes) super.clone();
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