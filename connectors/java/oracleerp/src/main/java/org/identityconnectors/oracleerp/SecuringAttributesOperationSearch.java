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
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.MSG_COULD_NOT_EXECUTE;
import static org.identityconnectors.oracleerp.OracleERPUtil.NAME;
import static org.identityconnectors.oracleerp.OracleERPUtil.PATTERN;
import static org.identityconnectors.oracleerp.OracleERPUtil.SEC_ATTRS_OC;
import static org.identityconnectors.oracleerp.OracleERPUtil.getColumn;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.SearchOp;

/**
 * @author Petr Jung
 * @since 1.0
 */
final class SecuringAttributesOperationSearch extends Operation implements
        SearchOp<FilterWhereBuilder> {

    private static final Log LOG = Log.getLog(SecuringAttributesOperationSearch.class);

    /**
     * @param conn
     * @param cfg
     */
    SecuringAttributesOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.identityconnectors.framework.spi.operations.SearchOp#
     * createFilterTranslator
     * (org.identityconnectors.framework.common.objects.ObjectClass,
     * org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass,
            OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, CollectionUtil
                .newSet(new String[]{OracleERPUtil.NAME}), new BasicNameResolver());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.identityconnectors.framework.spi.operations.SearchOp#executeQuery
     * (org.identityconnectors.framework.common.objects.ObjectClass,
     * java.lang.Object,
     * org.identityconnectors.framework.common.objects.ResultsHandler,
     * org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder query, ResultsHandler handler,
            OperationOptions options) {
        final String method = "executeQuery";
        LOG.ok(method);
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuilder b = new StringBuilder();
        // default value
        String pattern = "%";
        if (options != null && options.getOptions() != null) {
            Object patternOption = options.getOptions().get(PATTERN);
            pattern = patternOption == null ? pattern : (String) patternOption;
        }
        b.append("SELECT distinct akattrvl.NAME, fndappvl.APPLICATION_NAME ");
        b.append("FROM " + getCfg().app() + "AK_ATTRIBUTES_VL akattrvl, " + getCfg().app()
                + "FND_APPLICATION_VL fndappvl ");
        b.append("WHERE akattrvl.ATTRIBUTE_APPLICATION_ID = fndappvl.APPLICATION_ID ");
        b.append(" AND akattrvl.NAME LIKE '");
        b.append(pattern);
        b.append("' ");
        b.append("ORDER BY akattrvl.NAME");
        final String sql = b.toString();
        try {
            LOG.ok("execute sql {0}", sql);
            st = getConn().prepareStatement(sql);
            res = st.executeQuery();
            while (res.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append(getColumn(res, 1));
                sb.append("||");
                sb.append(getColumn(res, 2));
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(SEC_ATTRS_OC);
                bld.setName(sb.toString());
                bld.setUid(sb.toString());
                bld.addAttribute(AttributeBuilder.build(NAME, sb.toString()));
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
        } catch (Exception e) {
            final String msg1 = getCfg().getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
            LOG.error(e, msg1);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(msg1, e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        getConn().commit();
        LOG.ok(method + " done");
    }
}
