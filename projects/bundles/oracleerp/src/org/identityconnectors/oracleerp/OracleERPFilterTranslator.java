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

import java.util.Set;

import org.identityconnectors.dbcommon.DatabaseFilterTranslator;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete representation
 * of which filters can be applied at the connector level (natively). If the 
 * OracleErp doesn't support a certain expression type, that factory
 * method should return null. This level of filtering is present only to allow any
 * native contructs that may be available to help reduce the result set for the framework,
 * which will (strictly) reapply all filters specified after the connector does the initial
 * filtering.<p><p>Note: The generic query type is most commonly a String, but does not have to be.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class OracleERPFilterTranslator extends DatabaseFilterTranslator {

    ColumnNameResolver cnr = null;
    private Set<String> columnNames = null;
    
    
    /**
     * @param oclass
     * @param options
     * @param columnNames 
     * @param cnr 
     */
    public OracleERPFilterTranslator(ObjectClass oclass, OperationOptions options, Set<String> columnNames, ColumnNameResolver cnr) {
        super(oclass, options);
        this.cnr = cnr;
        this.columnNames  = columnNames;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.dbcommon.DatabaseFilterTranslator#getDatabaseColumnType(org.identityconnectors.framework.common.objects.Attribute, org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    @Override
    protected SQLParam getSQLParam(Attribute attribute, ObjectClass oclass, OperationOptions options) {
        final String attributeName = attribute.getName();
        final String columnName = cnr.getColumnName(attributeName);
        if ( columnNames.contains(columnName)) {
            return new SQLParam( columnName, AttributeUtil.getSingleValue(attribute));
        }
        return null;
    }
}
