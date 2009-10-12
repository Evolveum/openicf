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
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;


/**
 * The schema implementation of the SPI
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class OracleERPOperationRunScriptOnConnector extends Operation implements ScriptOnConnectorOp {
    
    private static final Log log = Log.getLog(OracleERPOperationRunScriptOnConnector.class);

    /**
     * @param conn
     * @param cfg
     */
    OracleERPOperationRunScriptOnConnector(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        /*
         * Build the actionContext to pass it to the script according the documentation
         */
        final Map<String, Object> scriptArguments = request.getScriptArguments();
        final Map<String, Object> actionContext = new HashMap<String, Object>();
        final Map<String, Object> inputMap = new HashMap<String, Object>();
        final List<String> errorList = new ArrayList<String>();
        
        Assertions.nullCheck(scriptArguments, "scriptArguments");
        final Map<String, Object> scriptAttributes = OracleERPUtil.getScriptAttributes( scriptArguments.get(ATTRIBUTES) );

        //Name
        Object value = scriptAttributes.get(Uid.NAME);
        if (value == null) {
            value = scriptAttributes.get(Name.NAME);
        }
        Assertions.nullCheck(value, ID);
        final String id = (String) value;

        //Password
        final Object pwdArg = scriptAttributes.get(OperationalAttributes.PASSWORD_NAME);

        //Connection
        actionContext.put(CONN, getConn().getConnection()); //The real connection
        final Object action = scriptArguments.get(ACTION);
        actionContext.put(ACTION, action); // The action is the operation name createUser/updateUser/deleteUser/disableUser/enableUser
        final Object timing = scriptArguments.get(TIMING);
        actionContext.put(TIMING, timing); // The timing before / after
        actionContext.put(ATTRIBUTES, scriptAttributes); // The attributes
        actionContext.put(ID, id); // The user id
        if (pwdArg != null && pwdArg instanceof GuardedString) {
            final GuardedString password = ((GuardedString) pwdArg);
            password.access(new GuardedString.Accessor() {
                public void access(char[] clearChars) {
                    actionContext.put(PASSWORD, new String(clearChars)); //The password
                }
            });
        }
        actionContext.put(TRACE, log); //The loging
        actionContext.put(ERRORS, errorList); // The error list
        log.ok("runScriptOnConnector action: {0}, timing: {1}, ID: {2}, scriptAttributes: {3}", action, timing, id, scriptAttributes);

        inputMap.put(ACTION_CONTEXT, actionContext);


        /*
         * Build the script executor and run the script
         */
        Object ret;
        final String scriptText = request.getScriptText();
        log.ok("runScriptOnConnector execute script: {0}", scriptText);
        try {
            final ClassLoader loader = getClass().getClassLoader();
            final String scriptLanguage = request.getScriptLanguage();
            final ScriptExecutorFactory scriptExFact = ScriptExecutorFactory.newInstance(scriptLanguage);
            final ScriptExecutor scripEx = scriptExFact.newScriptExecutor(loader, scriptText, true);
            ret = scripEx.execute(inputMap);
            
            //Go through the errors and throw first one 
            //TODO implement the warning set return, when possible
            StringBuilder errorBld = new StringBuilder();
            for (String s : errorList) {
                errorBld.append(s);
                errorBld.append("; ");
            }
            //Any errors, warnings?
            if (errorBld.length() != 0) {
                final String msg = errorBld.toString();
                log.error("script errors: {0}", msg);
                throw new ConnectorException(msg);
            }
            //Make sure, the connection is commit
            getConn().commit();            
        } catch (Exception e) {
            final String msg = getCfg().getMessage(MSG_COULD_NOT_EXECUTE, scriptText);
            log.error(e, msg);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(msg, e);
        }
        return ret;
    }

}
