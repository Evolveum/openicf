/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */

package org.forgerock.openicf.salesforce.query;

import java.beans.IntrospectionException;
import java.util.Iterator;

import org.forgerock.openicf.salesforce.meta.MetaResource;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

import org.forgerock.openicf.salesforce.annotation.Custom;
import org.forgerock.openicf.salesforce.annotation.ICAttribute;
import org.forgerock.openicf.salesforce.annotation.ICResource;

public class QueryBuilder {

    private IQuery query;
    private String selectPart;
    private String fromPart;
    private String joinPart;
    private String wherePart;
    private ICResource mainResource;

    public QueryBuilder(IQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Input query cannot be null");
        }

        this.query = query;
        this.selectPart = "";
        this.fromPart = "";
        this.joinPart = "";
        this.wherePart = "";

        processQuery();
    }


    private void processQuery() {
        mainResource = query.getResource();
        Iterator<IPart> iter = query.iterator();
        while (iter.hasNext()) {
            IPart part = iter.next();
            if (part instanceof IQueryPart) {
                try {
                    processQueryPart((IQueryPart) part);
                } catch (IntrospectionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                wherePart += part.toString();
            }
        }
    }

    /**
     * @param part
     * @throws IntrospectionException
     */
    private void processQueryPart(IQueryPart part) throws IntrospectionException {

    }


    @Override
    public String toString() {
        if (selectPart.length() == 0) {
            //String mainTableName = MetaResourceHelper.getTableNameForClass(mainResource.type());
            String mainTableName = "User";
            selectPart = mainTableName + ".*";
        }

        // combine joins and where parts
        String where = "";
        if (joinPart.length() > 0) {
            where = joinPart;
            if (wherePart.length() > 0) {
                where += " AND " + wherePart;
            }
        } else if (wherePart.length() > 0) {
            where = wherePart;
        }

        // handle extra where clause
        String extraWhere = mainResource.extraWhere();
        if (extraWhere.length() > 0) {
            if (where.length() > 0) {
                where = extraWhere + " AND " + where;
            } else {
                where = extraWhere;
            }
        }

        if (where.length() > 0) {
            where = "WHERE " + where;
        }

        return String.format("SELECT %s FROM %s %s", selectPart, fromPart, where);
    }
}
