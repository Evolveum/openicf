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

import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * The Account Name Resolver tries to convert Account column names to attribute names.
 *
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountNameResolver extends BasicNameResolver {

    /**
     * Map column name to attribute name, special attributes are handed separated
     *
     * @param columnName
     * @return the columnName
     */
    @Override
    public String getAttributeName(String columnName) {
        if (FULL_NAME.equalsIgnoreCase(columnName)) {
            return PERSON_FULLNAME;
        }
        return super.getAttributeName(columnName);
    }

    /**
     * Map the attribute name to column name, including the special attributes
     *
     * @param attributeName
     * @return the columnName
     */
    @Override
    public String getColumnName(String attributeName) {
        if (Name.NAME.equalsIgnoreCase(attributeName)) {
            return USER_NAME;
        } else if (Uid.NAME.equalsIgnoreCase(attributeName)) {
            return USER_NAME;
        } else if (PERSON_FULLNAME.equalsIgnoreCase(attributeName)) {
            return FULL_NAME;
        }
        return super.getColumnName(attributeName);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.AttributeNormalizer#normalizeAttribute(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Attribute)
     */
    @Override
    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        if (attribute instanceof Name) {
            return new Name(((Name) attribute).getNameValue().toUpperCase());
        } else if (attribute instanceof Uid) {
            return new Uid(((Uid) attribute).getUidValue().toUpperCase());
        } else if (USER_NAME.equalsIgnoreCase(attribute.getName())) {
            return AttributeBuilder.build(USER_NAME, AttributeUtil.getAsStringValue(attribute).toUpperCase());
        } else if (NAME.equalsIgnoreCase(attribute.getName())) {
            return AttributeBuilder.build(NAME, AttributeUtil.getAsStringValue(attribute).toUpperCase());
        }
        return super.normalizeAttribute(oclass, attribute);
    }

}
