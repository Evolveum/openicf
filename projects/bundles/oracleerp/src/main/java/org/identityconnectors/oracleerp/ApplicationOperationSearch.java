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
 * @version $Revision 1.0$
 * @since 1.0
 */
public class ApplicationOperationSearch extends Operation implements SearchOp<FilterWhereBuilder> {
    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(ApplicationOperationSearch.class);
    /**
     * @param conn
     * @param cfg
     */
    protected ApplicationOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, CollectionUtil
        .newSet(new String[] { OracleERPUtil.NAME }), new BasicNameResolver());
    }
    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder query, ResultsHandler handler,
    OperationOptions options) {
        final String method = "executeQuery";
        log.info(method);
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuilder b = new StringBuilder();
        String respName = null;
        if (options != null && options.getOptions() != null) {
            respName = (String) options.getOptions().get(RESP_NAME);
        } else {
            //TODO add the query support for applications
            return;
        }
        b.append("SELECT distinct fndappvl.application_name ");
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(cfg.app() + "fnd_application_vl fndappvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        b.append("AND fndrespvl.responsibility_name = ?");
        try {
            st = conn.prepareStatement(b.toString());
            st.setString(1, respName);
            res = st.executeQuery();
            while (res.next()) {
                String s = getColumn(res, 1);
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(APPS_OC);
                bld.addAttribute(AttributeBuilder.build(NAME, s));
                bld.setName(s);
                bld.setUid(s);
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
        } catch (Exception e) {
            final String msg = cfg.getMessage(MSG_COULD_NOT_READ);
            log.error(e, msg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        conn.commit();
        log.info(method + " ok");
    }
}
