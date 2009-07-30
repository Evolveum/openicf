/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.AUDITOR_RESPS_OC;
import static org.identityconnectors.oracleerp.OracleERPUtil.RESP_NAMES;
import static org.identityconnectors.oracleerp.OracleERPUtil.getAttributeInfos;
import static org.identityconnectors.oracleerp.OracleERPUtil.getAttributesToGet;

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
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
public class AuditorOperationSearch extends Operation implements SearchOp<FilterWhereBuilder> {
    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(AuditorOperationSearch.class);

    /**
     * @param conn
     * @param cfg
     */
    protected AuditorOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg) {
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
        final String id = new ResponsibilitiesOperations(conn, cfg).getOptionId(options);
        final boolean activeRespsOnly = new ResponsibilitiesOperations(conn, cfg).isActiveRespOnly(options);
        final String respLocation = new ResponsibilitiesOperations(conn, cfg).getRespLocation();

        List<String> auditorRespList = new ResponsibilitiesOperations(conn, cfg).getResponsibilities(id, respLocation,
                activeRespsOnly);
        for (String respName : auditorRespList) {
            final Set<AttributeInfo> ais = getAttributeInfos(cfg.getSchema(), RESP_NAMES);
            final AttributeMergeBuilder amb = new AttributeMergeBuilder(getAttributesToGet(options, ais));
            new AuditorOperations(conn, cfg).updateAuditorData(amb, respName);

            ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
            bld.setObjectClass(AUDITOR_RESPS_OC);
            bld.addAttributes(amb.build());
            if (!handler.handle(bld.build())) {
                break;
            }
        }
    }
}
