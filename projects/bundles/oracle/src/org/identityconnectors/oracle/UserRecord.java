package org.identityconnectors.oracle;

import java.sql.Date;

class UserRecord {
    String userName;
    String profile;
    String defaultTableSpace;
    String temporaryTableSpace;
    String externalName;
    long userId;
    String status;
    Date createdDate;
    Date lockDate;
    Date expireDate;
}
