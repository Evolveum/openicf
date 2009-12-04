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

class DeleteNativeUserCommand {
    /**
     * implementation of the Native Delete operation.
     * 
     * Compare with NIS implementation: {@see OpDeleteImpl#invokeNISDelete(String)} 
     */
    static void delete(final String accountId, SolarisConnection conn) {
        // USERDEL accountId
        final String command = conn.buildCommand("userdel", ((conn.getConfiguration().isDelHomeDir()) ? "-r" : ""), accountId);

        Map<String, ErrorHandler> rejectMap = initRejectsMap(accountId);
        conn.executeCommand(command, rejectMap, Collections.<String> emptySet());

        final String output = conn.executeCommand("echo $?");
        if (!output.equals("0")) {
            throw new UnknownUidException("Error deleting user: " + accountId);
        }
    }

    private static Map<String, ErrorHandler> initRejectsMap(final String accountId) {
        // initialize error handler
        final String messageErrorDelete = "Error deleting user: ";
        final ErrorHandler unknownUidHandler = new ErrorHandler() {
            public void handle(String buffer) {
                throw new UnknownUidException(messageErrorDelete + accountId);
            }
        };
        
        // initialize rejects map
        final Map<String, ErrorHandler> result = new LinkedHashMap<String, ErrorHandler>();
        
        final String rejectDoesNotExist = "does not exist";
        result.put(rejectDoesNotExist, unknownUidHandler);
        
        final String rejectUnknownUser = "nknown user";
        result.put(rejectUnknownUser, unknownUidHandler);
        
        result.put("ERROR", new ErrorHandler() {
            public void handle(String buffer) {
                /*
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
                if (buffer.contains(rejectDoesNotExist) || buffer.contains(rejectDoesNotExist)) {
                    throw new UnknownUidException(messageErrorDelete + accountId);
                } else {
                    throw new ConnectorException(messageErrorDelete + accountId);
                }
            }
        });
        
        return result;
    }
}
