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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.mysqluser;

import static org.identityconnectors.mysqluser.MySQLUserConstants.MYSQL_USER;

import org.identityconnectors.dbcommon.DatabaseFilterTranslator;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * My SQL filter translator.
 *
 * @since 1.0
 */
public class MySQLUserFilterTranslator extends DatabaseFilterTranslator {

    /**
     * The filter translator constructor.
     *
     * @param objectClass
     *            object class
     * @param options
     *            operation options
     */
    public MySQLUserFilterTranslator(final ObjectClass objectClass, final OperationOptions options) {
        super(objectClass, options);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.identityconnectors.dbcommon.DatabaseFilterTranslator#
     * getDatabaseColumnType
     * (org.identityconnectors.framework.common.objects.Attribute,
     * org.identityconnectors.framework.common.objects.ObjectClass,
     * org.identityconnectors.framework.common.objects.OperationOptions)
     */
    @Override
    protected SQLParam getSQLParam(Attribute attribute, ObjectClass oclass, OperationOptions options) {
        // check the null values
        if (attribute == null) {
            return null;
        }
        // MySQLUser filter a name or uid attribute
        if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
            return new SQLParam(MYSQL_USER, AttributeUtil.getSingleValue(attribute));
        }
        // Password or other are invalid columns for query,
        // There could be an exception,but null value would disable this filter
        return null;
    }
}
