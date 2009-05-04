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

import org.identityconnectors.framework.common.exceptions.ConnectorException;


/** helper class for Solaris specific operations */
public class SolarisHelper {
    public static final int SHORT_WAIT = 60000;
    
    public static String executeCommand(SolarisConfiguration configuration,
            SolarisConnection connection, String command) {
        connection.resetStandardOutput();
        try {
            connection.send(command);
            connection.waitFor(configuration.getRootShellPrompt(), SHORT_WAIT);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        String output = connection.getStandardOutput();
        int index = output.lastIndexOf(configuration.getRootShellPrompt());
        if (index!=-1)
            output = output.substring(0, index);
        
        String terminator = "\n";
        // trim off starting or ending \n
        //
        if (output.startsWith(terminator)) {
            output = output.substring(terminator.length());
        }
        if (output.endsWith(terminator)) {
            output = output.substring(0, output.length()-terminator.length());
        }
        return output;
    }
}
