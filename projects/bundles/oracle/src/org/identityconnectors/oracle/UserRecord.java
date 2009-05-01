package org.identityconnectors.oracle;

import java.sql.Timestamp;

/** User record represents one record from DBA_USERS table */
final class UserRecord {
    String userName;
    String profile;
    String defaultTableSpace;
    String temporaryTableSpace;
    String externalName;
    Long userId;
    String status;
    Timestamp createdDate;
    Timestamp lockDate;
    Timestamp expireDate;
    //We read this just for testing for external authentication
    String password;
}
