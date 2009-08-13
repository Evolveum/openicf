/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
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
