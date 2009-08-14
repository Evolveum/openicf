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

import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
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
public class ResponsibilitiesOperationSearch extends Operation implements SearchOp<FilterWhereBuilder> {
    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(ResponsibilitiesOperationSearch.class);

    /** Resp operations*/
    private ResponsibilitiesOperations respOps;

    /**
     * @param conn
     * @param cfg
     */
    protected ResponsibilitiesOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
        respOps = new ResponsibilitiesOperations(conn, cfg);
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

        final boolean activeRespsOnly = respOps.isActiveRespOnly(options);
        final String id = respOps.getOptionId(options);

        String respLocation = null;
        if (oclass.equals(DIRECT_RESP_OC)) { //OK
            respLocation = RESPS_DIRECT_VIEW;
        } else if (oclass.equals(INDIRECT_RESP_OC)) { //OK
            respLocation = RESPS_INDIRECT_VIEW;
        } else {
            respLocation = respOps.getRespLocation();
        }

        List<String> objectList = respOps.getResponsibilities(id, respLocation, activeRespsOnly);

        for (String respName : objectList) {
            ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
            bld.setObjectClass(oclass);
            bld.setName(respName);
            bld.setUid(respName);
            bld.addAttribute(AttributeBuilder.build(NAME, respName));
            if (!handler.handle(bld.build())) {
                break;
            }
        }
        conn.commit();
        log.info(method + " done");
    }

}
