/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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

import java.sql.Types;

import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.objects.*;

/**
 * DB2 filter translator
 * @version $Revision 1.0$
 * @since 1.0
 */
class DB2FilterTranslator extends DatabaseFilterTranslator {
    /**
     * The filter translator constructor
     * @param oclass object class
     * @param options operation options
     */
    public DB2FilterTranslator(ObjectClass oclass, OperationOptions options) {
        super(oclass, options);
    }

    @Override
    protected String getDatabaseColumnName(Attribute attribute, ObjectClass oclass, OperationOptions options) {
        if(attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
            return "UPPER(TRIM(GRANTEE))";
        }
        //Password or other are invalid columns for query, 
        //There could be an exception,but null value would disable this filter 
        return null;
    }
    
	@Override
	protected FilterWhereBuilder createBuilder() {
		return new FilterWhereBuilder();
	}

	@Override
	protected boolean validateSearchAttribute(Attribute attribute) {
		if(DB2Connector.USER_AUTH_GRANTS.equals(attribute.getName())){
			return false;
		}
		return super.validateSearchAttribute(attribute);
	}

    /* (non-Javadoc)
     * @see org.identityconnectors.dbcommon.DatabaseFilterTranslator#getDatabaseColumnType(org.identityconnectors.framework.common.objects.Attribute, org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    @Override
    protected Integer getDatabaseColumnType(Attribute attribute, ObjectClass oclass, OperationOptions options) {
        return Types.NULL;
    }
	
	
    
    
 
}
