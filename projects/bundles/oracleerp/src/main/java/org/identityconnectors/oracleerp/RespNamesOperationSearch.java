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
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.AttributeInfo;
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
public class RespNamesOperationSearch extends Operation implements SearchOp<FilterWhereBuilder> {
    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(RespNamesOperationSearch.class);

    /** Audit Operations */
    private AuditorOperations auditOps;

    /**
     * @param conn
     * @param cfg
     */
    public RespNamesOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
        auditOps = new AuditorOperations(conn, cfg);
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
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        final String method = "executeQuery";
        log.info(method);

        final Set<AttributeInfo> ais = getAttributeInfos(cfg.getSchema(), RESP_NAMES);
        final Set<String> atg = getAttributesToGet(options, ais);

        PreparedStatement st = null;
        ResultSet res = null;
        StringBuilder b = new StringBuilder();

        b.append("SELECT distinct fndrespvl.responsibility_name ");
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(cfg.app() + "fnd_application_vl fndappvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");

        // Query support
        if (where != null && where.getParams().size() == 1) {
            b.append("and fndrespvl.responsibility_name = ?");
        } else {
            where = new FilterWhereBuilder();
        }

        try {
            st = conn.prepareStatement(b.toString(), where.getParams());
            res = st.executeQuery();
            while (res.next()) {

                String respName = getColumn(res, 1);
                AttributeMergeBuilder amb = new AttributeMergeBuilder(atg);
                amb.addAttribute(NAME, respName);

                if (where.getParams().size() == 1) {
                    auditOps.updateAuditorData(amb, respName);
                }

                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(RESP_NAMES_OC);
                bld.addAttributes(amb.build());
                bld.setName(respName);
                bld.setUid(respName);
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
