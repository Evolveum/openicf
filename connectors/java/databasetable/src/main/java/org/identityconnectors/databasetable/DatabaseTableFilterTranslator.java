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
package org.identityconnectors.databasetable;

import org.identityconnectors.databasetable.mapping.misc.SQLColumnTypeInfo;
import org.identityconnectors.dbcommon.DatabaseFilterTranslator;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;

/**
 * Database table filter translator
 *
 * @version $Revision 1.0$
 * @since 1.0
 */
public class DatabaseTableFilterTranslator extends DatabaseFilterTranslator {

    DatabaseTableConnector connector;

    /**
     * @param connector the database table connector
     * @param oclass
     * @param options
     */
    public DatabaseTableFilterTranslator(DatabaseTableConnector connector, ObjectClass oclass, OperationOptions options) {
        super(oclass, options);
        this.connector = connector;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.dbcommon.DatabaseFilterTranslator#getDatabaseColumnType(org.identityconnectors.framework.common.objects.Attribute, org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    @Override //TODO
    protected SQLParam getSQLParam(Attribute attribute, ObjectClass oclass, OperationOptions options) {
        final Object value = AttributeUtil.getSingleValue(attribute);
        String columnName = connector.quoteName(connector.getColumnName(attribute.getName()));
        SQLColumnTypeInfo columnTypeInfo = connector.getColumnTypeInfo(columnName);

        if (columnTypeInfo != null) {
        } else {
            String quoting = connector.getConfiguration().getQuoting();
            if (quoting != null && !quoting.isEmpty() && !quoting.equalsIgnoreCase(Quoting.NONE.getType())) {

                if (columnName != null && columnName.length() >= 2) {
                    String firstChar = String.valueOf(columnName.charAt(0));
                    String lastChar = String.valueOf(columnName.charAt(columnName.length() - 1));

                    String quoteValue = Quoting.compareAndFetch(quoting).getValue();
                    if (quoteValue != null) {
                        if (quoteValue.contains(firstChar) && quoteValue.contains(lastChar)) {
                            String trimmedColumnName = columnName.substring(1, columnName.length() - 1);
                            //2nd attempt to find the column type information, removing double quotes
                            columnTypeInfo = connector.getColumnTypeInfo(trimmedColumnName);
                        }
                    } else {

                        throw new ConnectorException("The 'Quoting' configuration parameter is set to an unknown value," +
                                " please use either of the following: " + Quoting.printAll());
                    }
                } else {

                    throw new ConnectorException("Column with an empty name, not possible to process such column.");
                }
            }
        }

        if (columnTypeInfo != null) {

            return new SQLParam(columnName, value, columnTypeInfo.getTypeCode(), columnTypeInfo.getTypeName());
        } else {

            throw new ConnectorException("Column with the name '" + columnName + "' not found amongst the generated " +
                    "'type data set' and probably not present in resource schema.");
        }

    }

}
