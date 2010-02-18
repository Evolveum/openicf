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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnection.ErrorHandler;

public class DeleteNativeGroup {

    public static void delete(final String groupName, SolarisConnection conn) {
        
        String groupdelCmd = conn.buildCommand("groupdel");
        
        Map<String, ErrorHandler> rejectsMap = initRejectsMap(groupName);

        conn.doSudoStart();
        try {
            conn.executeCommand(groupdelCmd + " '" + groupName + "'", rejectsMap, Collections.<String>emptySet());
        } finally {
            conn.doSudoReset();
        }
    }

    private static Map<String, ErrorHandler> initRejectsMap(final String groupName) {
        ErrorHandler defaultErrHandler = new ErrorHandler() {
            public void handle(String buffer) {
                /*
                 * contract tests workaround: (issue is not right type of exception)
                 * 
                 * Initial state: originally we were throwing just ConnectorException here, because we 
                 * assumed that if Expect4j matches, "ERROR", this means that none of other more specific
                 * messages have been in the response. This assumption turned out to be wrong, thus we 
                 * introduced the following change: 
                 * 
                 * Change description: we need to do an extra check of the buffer, if the buffer 
                 * doesn't contain reject strings for more specific exceptions, that deserve 
                 * UnknownUidException to be thrown.
                 * 
                 * Original motivation of this extra check:
                 * Random errors in contract tests can occur because the wrong exception type is thrown.
                 * 
                 * How the error happens:
                 * Expect4J reads the response from the resource partially (for instance just the start 
                 * of the error message, containing "ERROR"), so will invoke the Closure for generic error
                 * message, instead of the specific one. 
                 * After Expect invokes the closure, the SolarisConnection marks, which error message was matched.
                 * If an error messages is matched, SolarisConnection will try to read as much of the response, as it can.
                 * This is were the connection comes to know the remaining part of the error message, 
                 * which can contain the specific information we miss.
                 * So even though Expect4j matched the generic message, based on full buffer content 
                 * we need to throw a specific error message. 
                 */
                if (buffer.contains("does not exist")) {
                    throw new UnknownUidException("Unknown group: '" + groupName + "'. Buffer: <" + buffer + ">");
                } else {
                    throw new ConnectorException("ERROR: buffer content: <" + buffer + ">");
                }
            }
        };
        ErrorHandler unknownUidHandler = new ErrorHandler() {
            public void handle(String buffer) {
                throw new UnknownUidException("Unknown group: '" + groupName + "'. Buffer: <" + buffer + ">");
            }
        };
        
        Map<String, ErrorHandler> rejectsMap = new LinkedHashMap<String, ErrorHandler>();
        /*
         * This is a workaround to throw UnknownUidException. 
         * Unfortunately Expect4j doesn't match the input as expected.
         */
        rejectsMap.put("UX.*ERROR.*does not exist", unknownUidHandler);
        rejectsMap.put("groupdel:.*ERROR.*does not exist", unknownUidHandler);
        rejectsMap.put("ERROR.*does not exist", unknownUidHandler);
        
        rejectsMap.put("does not exist", unknownUidHandler); // HP-UX error
        rejectsMap.put("command not found", defaultErrHandler); // HP-UX error
        rejectsMap.put("not allowed to execute", defaultErrHandler); // sudo 
        rejectsMap.put("annot remove", defaultErrHandler); // sudo
        rejectsMap.put("ERROR", defaultErrHandler);
        return rejectsMap;
    }

}
