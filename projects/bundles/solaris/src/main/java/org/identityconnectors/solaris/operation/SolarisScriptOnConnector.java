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
package org.identityconnectors.solaris.operation;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;

public class SolarisScriptOnConnector {

    private SolarisConnection connection;

    public SolarisScriptOnConnector(SolarisConnector solarisConnector) {
        connection = solarisConnector.getConnection();
    }

    /**
     * Execute a script on the resource. The result will contain everything up to the first rootShellPrompt.
     * @param request contains scriptText, that will be executed.
     * @param options is not used by {@link SolarisConnector}
     * @return the result of the script's execution (the result is the feedback up to the rootShellPrompt).
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        final String scriptLanguage = request.getScriptLanguage();
        final String scriptText = request.getScriptText();
        
        if (StringUtil.isBlank(scriptLanguage))
            throw new IllegalArgumentException("Script language is missing.");
        
        if (!scriptLanguage.equals("bash"))
            throw new IllegalArgumentException("ScriptLanguage is '"+ scriptLanguage + "'. The only accepted script language is 'bash'.");
        
        if (StringUtil.isBlank(scriptText))
            throw new IllegalArgumentException("scriptText is missing");
        
        String out = connection.executeCommand(scriptText);
        
        return out;
    }

}
