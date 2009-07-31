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
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.FULL_NAME;
import static org.identityconnectors.oracleerp.OracleERPUtil.PERSON_FULLNAME;
import static org.identityconnectors.oracleerp.OracleERPUtil.USER_NAME;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * The Account Name Resolver tries to convert Account column names to attribute names.
 * 
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountNameResolver implements NameResolver {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(AccountNameResolver.class);

    /**
     * Map column name to attribute name, special attributes are handed separated
     * 
     * @param columnName
     * @return the columnName
     */
    public String getAttributeName(String columnName) {
        if (FULL_NAME.equalsIgnoreCase(columnName)) {
            return PERSON_FULLNAME;
        }
        return columnName;
    }

    /**
     * Map the attribute name to column name, including the special attributes
     * 
     * @param attributeName
     * @return the columnName
     */
    public String getColumnName(String attributeName) {
        if (Name.NAME.equalsIgnoreCase(attributeName)) {
            return USER_NAME;
        } else if (Uid.NAME.equalsIgnoreCase(attributeName)) {
            return USER_NAME;
        } else if (PERSON_FULLNAME.equalsIgnoreCase(attributeName)) {
            return FULL_NAME;
        }
        return attributeName;
    }

}
