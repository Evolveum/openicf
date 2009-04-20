package org.identityconnectors.oracle;

import java.sql.Date;

/** User record represents one record from DBA_USERS table */
class UserRecord {
    String userName;
    String profile;
    String defaultTableSpace;
    String temporaryTableSpace;
    String externalName;
    Long userId;
    String status;
    Date createdDate;
    Date lockDate;
    Date expireDate;
    //We read this just for testing for external authentication
    String password;
}
