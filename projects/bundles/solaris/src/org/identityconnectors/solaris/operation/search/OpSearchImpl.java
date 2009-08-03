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
package org.identityconnectors.solaris.operation.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisFilter;
import org.identityconnectors.solaris.command.CommandBuilder;
import org.identityconnectors.solaris.constants.AccountAttributes;
import org.identityconnectors.solaris.operation.AbstractOp;


public class OpSearchImpl extends AbstractOp {
    
    private OperationOptions options;
    private ObjectClass oclass;
    
    public OpSearchImpl(Log log, SolarisConnector conn) {
        super(log, conn);
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
        this.options = options;
        this.oclass = oclass;
        
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
     * filters the output using various constraints (query, escapeStrings...).
     * By default return RETURNED_BY_DEFAULT attributes
     * 
     * @param output
     *            the output of list user command that should be filtered. It
     *            contains command line prompt + prompt for the next command
     *            that should be erased.
     * @param handler
     *            it is called when a result is returned
     * @param escapeStrings
     *            these items should not appear in the result
     * @param query
     *            these filters are applied on the result
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
    private void notifyHandler(ResultsHandler handler, String uid) {
        // TODO this could be more complicated, when other than Name attributes arrive.
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .addAttribute(AttributeBuilder.build(Name.NAME, uid))
                .addAttribute(new Uid(uid));
        ConnectorObject co = builder.build();
        
        /*
         * return RETURNED_BY_DEFAULT attributes + attrsToGet
         */
        /** attributes to get */
        String[] attrsToGet = options.getAttributesToGet();
        if (attrsToGet == null) {
            attrsToGet = getReturnedByDefaultAttrs(getSchema());
        }
        
        for (String attrName : attrsToGet) {
            if (attrName.equals(AccountAttributes.INACTIVE.getName())) {
                try {
                    getConnection().send(getCmdBuilder().build("logins", "-oxma", "-l", ));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            // TODO fill the other attributes
        }
        
        handler.handle(co);
    }

    /**
     * TODO this can be done in two ways: 
     * either you have a register of returned by default attributes in the connector, 
     * or you find it in the schema.
     * 
     * @param schema
     * @return set of attribute names that are returned by default
     */
    private String[] getReturnedByDefaultAttrs(Schema schema) {
        List<String> result = new ArrayList<String>();
        
        ObjectClassInfo ocinfo = schema.findObjectClassInfo(oclass.getObjectClassValue());
        Set<AttributeInfo> attrInfo = ocinfo.getAttributeInfo();
        for (AttributeInfo attributeInfo : attrInfo) {
            if (attributeInfo.isReturnedByDefault()) {
                result.add(attributeInfo.getName());
            }
        }
        
        return result.toArray(new String[0]);
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
