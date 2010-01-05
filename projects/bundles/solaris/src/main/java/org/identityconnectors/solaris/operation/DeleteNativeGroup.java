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
        
        ErrorHandler defaultErrHandler = new ErrorHandler() {
            public void handle(String buffer) {
                throw new ConnectorException("ERROR: buffer content: <" + buffer + ">");
            }
        };
        ErrorHandler unknownUidHandler = new ErrorHandler() {
            public void handle(String buffer) {
                throw new UnknownUidException("Unknown group: '" + groupName + "'. Buffer: <" + buffer + ">"); // TODO might erase buffer output for security reasons.
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

        conn.doSudoStart();
        try {
            conn.executeCommand(groupdelCmd + " '" + groupName + "'", rejectsMap, Collections.<String>emptySet());
        } finally {
            conn.doSudoReset();
        }
    }

}
