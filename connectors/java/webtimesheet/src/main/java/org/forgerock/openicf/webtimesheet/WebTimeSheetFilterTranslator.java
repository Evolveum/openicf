/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openicf.webtimesheet;

import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete
 * representation of which filters can be applied at the connector level
 * (natively). If the WebTimeSheet doesn't support a certain expression type,
 * that factory method should return null. This level of filtering is present
 * only to allow any native constructs that may be available to help reduce the
 * result set for the framework, which will (strictly) reapply all filters
 * specified after the connector does the initial filtering.<p><p>Note: The
 * generic query type is most commonly a String, but does not have to be.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class WebTimeSheetFilterTranslator extends AbstractFilterTranslator<String> {

    /**
     * Setup logging for the {@link WebTimeSheetConnector}.
     */
    private static final Log log = Log.getLog(WebTimeSheetFilterTranslator.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createContainsExpression(ContainsFilter filter, boolean not) {
        /* 
         * Example implementation:
         * You may define the format of the queries for your connector, but
         * you must make sure that the executeQuery() (if you implemented Search) 
         * method handles it appropriately.
         */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createEndsWithExpression(EndsWithFilter filter, boolean not) {
        log.info("createEndsWithExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createStartsWithExpression(StartsWithFilter filter, boolean not) {
        log.info("createStartsWithExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        log.info("createEqualsExpression");
        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        log.info("Filter Attribute: {0} Value: {1}", name, value);
        if (StringUtil.isBlank(value)) {
            return null;
        } else if (not) {
            //create an expression that means "not contains" or "doesn't contain" if possible
            throw new IllegalArgumentException("Unsupported filter: NOT EQUALS");
        } else {
            if (name.equalsIgnoreCase("__NAME__")) {
                JSONObject query = new JSONObject();
                try {
                    query.put("Action", "Query");
                    query.put("DomainType", "Replicon.Domain.User");
                    query.put("QueryType", "UserByLoginName");
                    query.put("Args", new JSONArray().put(value));
                }
                catch (JSONException ex) {
                    log.error("Unable to prepare JSON query", ex);
                }
                return query.toString();
            }
            if (name.equalsIgnoreCase("__UID__")) {
                JSONObject query = new JSONObject();
                int uid = Integer.parseInt(value);
                JSONArray uidArray = new JSONArray().put(uid);
                try {
                    query.put("Action", "Query");
                    query.put("DomainType", "Replicon.Domain.User");
                    query.put("QueryType", "UserById");
                    query.put("Args", new JSONArray().put(uidArray));
                }
                catch (JSONException ex) {
                    log.error("Unable to prepare JSON query", ex);
                }
                return query.toString();
            } else {
                throw new IllegalArgumentException("Unsupported filter Attribute: " + name);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createAndExpression(String leftExpression, String rightExpression) {
        log.info("createAndExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createOrExpression(String leftExpression, String rightExpression) {
        log.info("createOrExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        log.info("createGreaterThanExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        log.info("createGreaterThanOrEqualExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createLessThanExpression(LessThanFilter filter, boolean not) {
        log.info("createLessThanExpression");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        log.info("createLessThanOrEqualExpression");
        return null;
    }
}
