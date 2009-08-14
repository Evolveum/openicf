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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;


/**
 * The schema implementation of the SPI
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class OracleERPOperationRunScriptOnConnector extends Operation implements ScriptOnConnectorOp {

    /**
     * @param conn
     * @param cfg
     */
    protected OracleERPOperationRunScriptOnConnector(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        final ClassLoader loader = getClass().getClassLoader();

        /*
         * Build the actionContext to pass it to the script according the documentation
         */
        final Map<String, Object> actionContext = new HashMap<String, Object>();
        final Map<String, Object> scriptArguments = request.getScriptArguments();
        Assertions.nullCheck(scriptArguments, "scriptArguments");

        //Name
        final Object nameArg = scriptArguments.get(Name.NAME);
        Assertions.nullCheck(nameArg, Name.NAME);
        final String nameValue = ((Name) nameArg).getNameValue();

        //Password
        final Object pwdArg = scriptArguments.get(OperationalAttributes.PASSWORD_NAME);
        Assertions.nullCheck(pwdArg, OperationalAttributes.PASSWORD_NAME);
        final GuardedString password = ((GuardedString) pwdArg);

        //Connection
        actionContext.put("conn", conn.getConnection()); //The real connection
        final Object action = scriptArguments.get("operation");
        actionContext.put("action", action); // The action is the operation name createUser/updateUser/deleteUser/disableUser/enableUser
        final Object timing = scriptArguments.get("timing");
        actionContext.put("timing", timing); // The timming before / after
        final Object attributes = scriptArguments.get("attributes");
        actionContext.put("attributes", attributes); // The attributes
        actionContext.put("id", nameValue); // The user name
        if (password != null) {
            password.access(new GuardedString.Accessor() {
                public void access(char[] clearChars) {
                    actionContext.put("password", new String(clearChars)); //The password
                }
            });
        }
        actionContext.put("trace", log); //The loging
        List<String> errorList = new ArrayList<String>();
        actionContext.put("errors", errorList); // The error list

        Map<String, Object> inputMap = new HashMap<String, Object>();
        inputMap.put("actionContext", actionContext);


        /*
         * Build the script executor and run the script
         */
        final String scriptLanguage = request.getScriptLanguage();
        final ScriptExecutorFactory scriptExFact = ScriptExecutorFactory.newInstance(scriptLanguage);
        final ScriptExecutor scripEx = scriptExFact.newScriptExecutor(loader, request.getScriptText(), true);
        Object ret;
        try {
            ret = scripEx.execute(inputMap);
        } catch (Exception e) {
            log.error(e, "error in script");
            SQLUtil.rollbackQuietly(conn);
            throw ConnectorException.wrap(e);
        }
        conn.commit();
        return ret;
    }



}
