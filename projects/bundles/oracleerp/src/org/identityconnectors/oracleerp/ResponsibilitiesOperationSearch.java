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

import static org.identityconnectors.oracleerp.OracleERPUtil.*;
import static org.identityconnectors.oracleerp.OracleERPUtil.DIRECT_RESP_OC;
import static org.identityconnectors.oracleerp.OracleERPUtil.INDIRECT_RESP_OC;
import static org.identityconnectors.oracleerp.OracleERPUtil.RESPS_DIRECT_VIEW;
import static org.identityconnectors.oracleerp.OracleERPUtil.RESPS_INDIRECT_VIEW;

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
    }

}
