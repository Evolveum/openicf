package org.identityconnectors.db2;


/**
 *  Utility class that generates SQL snippets for operating on
 *  DB2 authorization tables.
 */
class DB2AuthorityTable {

    /**
     *  Constructor.
     */
    DB2AuthorityTable(String sqlRevokeFunctionObjectConnector)
    {
        this.sqlRevokeFunctionObjectConnector
            = sqlRevokeFunctionObjectConnector;
    }

    /**
     *  Generates sql to revoke a given DB2Authority
     */
    String generateRevokeSQL(DB2Authority auth) {
        return "REVOKE " + auth.authorityFunction + " "
            + sqlRevokeFunctionObjectConnector + " "
            + auth.authorityObject + " FROM USER "
            + auth.userName ;
    }

    /**
     *
     */
    String generateGrant(DB2Authority auth) {
        return (auth.authorityFunction + " "
            + sqlRevokeFunctionObjectConnector + " "
            + auth.authorityObject).trim();
    }

    /**
     *
     */
    String generateGrantSQL(DB2Authority auth) {
        return "GRANT " + generateGrant(auth) + " TO USER "
            + auth.userName ;
    }

    public final String sqlRevokeFunctionObjectConnector;
}
