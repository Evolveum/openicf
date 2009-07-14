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
package org.identityconnectors.solaris;

import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.operation.AbstractOp;


public class OpSearchImpl extends AbstractOp {
    
    public OpSearchImpl(SolarisConfiguration configuration,
            SolarisConnection connection, Log log) {
        super(configuration, connection, log);
    }
    
    /**
     * Search operation
     * 
     * @param query
     *            can contain AND, OR ("&", "|") that represents and-ing and
     *            or-ing the result of matches. AND has priority over OR.
     */
    public void executeQuery(ObjectClass oclass, SolarisFilter query,
            ResultsHandler handler, OperationOptions options) {
        // if i run the tests separately, the login info is in the expect4j's buffer
        // otherwise (when tests are run in batch), there is empty buffer, so this waitfor will timeout.
        try {
            getConnection().waitFor(
                    getConfiguration().getRootShellPrompt(),
                    SolarisConnection.WAIT);
        } catch (Exception ex) {
            // OK
        }
        
        //evaluate if the user exists
        final String command = getCmdBuilder().build("cut", "-d: -f1 /etc/passwd | grep -v \"^[+-]\"");
        String output = executeCommand(command);
        
        List<String> escapeStrings = CollectionUtil.newList(getConfiguration().getRootShellPrompt(), "@"/* TODO */, "#"/* TODO */, "$"/* TODO */, command);
        /* TODO: replace hard-coded constants, see the previous line. */
        
        filterResult(output, handler, escapeStrings, query);
    }

    /**
     * filters the output using various constraints (query, escapeStrings
     * @param output the output of list user command that should be filtered. It contains 
     * command line prompt + prompt for the next command that should be erased.
     * @param handler it is called when a result is returned
     * @param escapeStrings these items should not appear in the result
     * @param query these filters are applied on the result
     */
    private void filterResult(String output, ResultsHandler handler,
            List<String> escapeStrings, SolarisFilter query) {
        
        // TODO
        if (query != null && !query.getName().equals(Name.NAME)) {
            throw new UnsupportedOperationException("Only filtering by __NAME__ attribute is supported for now.");
        }

        final String[] lines = output.split("\n");
        
        for (String currentLine : lines) {
            /** token can contain username or other data in single column. */
            String token = currentLine.trim();
            if (!isEscaped(token, escapeStrings)) {
                if (query != null) {
                    if (token.matches(query.getRegExp())) {
                        notifyHandler(handler, token);
                    }
                } else {
                    // Null query, return all results
                    notifyHandler(handler, token);
                }
            }
        }//for
    }
    
    /** calls handler with given string */
    private void notifyHandler(ResultsHandler handler, String string) {
        // TODO this could be more complicated, when other than Name attributes arrive.
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .addAttribute(AttributeBuilder.build(Name.NAME, string))
                .addAttribute(new Uid(string));
        ConnectorObject co = builder.build();
        handler.handle(co);
    }

    /** checks if given 'string' is on the list of items to escape. */
    private boolean isEscaped(String string, List<String> escapeStrings) {
        for (String str : escapeStrings) {
            if (string.contains(str))
                return true;
        }
        return false;
    }
}
