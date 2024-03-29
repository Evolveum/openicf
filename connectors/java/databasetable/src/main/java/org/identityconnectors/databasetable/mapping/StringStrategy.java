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
package org.identityconnectors.databasetable.mapping;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;


/**
 * The SQL get/set strategy class implementation
 * All types expected to be String is write as a string and the conversion is left on database driver
 *
 * @version $Revision 1.0$
 * @since 1.0
 */
public class StringStrategy implements MappingStrategy {

    MappingStrategy delegate;
    static Log log = Log.getLog(StringStrategy.class);

    /**
     * The SQL get/set strategy class implementation write as a string all types mapped as a String.
     * Final sql mapping
     *
     * @param delegate
     */
    public StringStrategy(MappingStrategy delegate) {
        Assertions.nullCheck(delegate, "MappingStrategy delegate");
        this.delegate = delegate;
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.databasetable.MappingStrategy#getSQLParam(java.sql.ResultSet, int, int)
     */
    public SQLParam getSQLParam(ResultSet resultSet, int i, String name, final int sqlType, String sqlAttributeTypeName) throws SQLException {
        //Is it expected to be string, read as a string.
        if (delegate.getSQLAttributeType(sqlType, sqlAttributeTypeName).isAssignableFrom(String.class)) {
            return SQLUtil.getSQLParam(resultSet, i, name, Types.VARCHAR);
        }
        //Default processing otherwise
        return delegate.getSQLParam(resultSet, i, name, sqlType, sqlAttributeTypeName);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.databasetable.MappingStrategy#getSQLAttributeType(int)
     */
    public Class<?> getSQLAttributeType(int sqlType, String sqlAttributeTypeName) {
        return delegate.getSQLAttributeType(sqlType, sqlAttributeTypeName);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.databasetable.MappingStrategy#setSQLParam(java.sql.PreparedStatement, int, org.identityconnectors.dbcommon.SQLParam)
     */
    public void setSQLParam(final PreparedStatement stmt, final int idx, SQLParam parm) throws SQLException {
        // Write all internal string as a string and left conversion to the database

        Class sqlType = delegate.getSQLAttributeType(parm.getSqlType(), parm.getSqlTypeName());

        if (sqlType.isAssignableFrom(String.class)) {
            // Force convert to string
            SQLUtil.setSQLParam(stmt, idx, new SQLParam(parm.getName(), parm.getValue(), Types.VARCHAR));
        } else {
            // Default otherwise
            delegate.setSQLParam(stmt, idx, parm);
        }
    }
}

