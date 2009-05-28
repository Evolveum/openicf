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

import static org.identityconnectors.solaris.SolarisMessages.MSG_NOT_SUPPORTED_OBJECTCLASS;

import java.util.List;
import java.util.Map;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;


/** helper class for Solaris specific operations */
public class SolarisHelper {
    //private static final int SHORT_WAIT = 60000;
    //private static final int LONG_WAIT = 120000;
    private static final int VERY_LONG_WAIT = 1200000;
    
    /** set the timeout for waiting on reply. */
    public static final int DEFAULT_WAIT = VERY_LONG_WAIT;
    
    /** 
     * Execute a issue a command on the resource specified by the configuration 
     */
    public static String executeCommand(SolarisConfiguration configuration,
            SolarisConnection connection, String command) {
        connection.resetStandardOutput();
        try {
            connection.send(command);
            connection.waitFor(configuration.getRootShellPrompt(), VERY_LONG_WAIT);
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
    
    /** helper method for getting the password from an attribute map */
    public static GuardedString getPasswordFromMap(Map<String, Attribute> attrMap) {
        String msg; 
        Attribute attrPasswd = attrMap.get(OperationalAttributes.PASSWORD_NAME);
        List<Object> listValues = attrPasswd.getValue();
        
        Object o = null;
        try {
            o = listValues.get(0);
        } catch (ArrayIndexOutOfBoundsException ex) {
            msg = String.format("password attribute is missing from attribute map: %s", attrMap.toString());
            throw new IllegalArgumentException(msg);
        }
        
        if (o instanceof GuardedString) {
            return (GuardedString) listValues.get(0);
        } else {
            msg = String.format("password should be of type GuardedString, inside attribute map: %s", attrMap.toString());
            throw new IllegalArgumentException(msg);
        }
    }
    
    public static void controlObjectClassValidity(ObjectClass oclass) {
        if (!oclass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new IllegalArgumentException(String.format(
                    MSG_NOT_SUPPORTED_OBJECTCLASS, ObjectClass.ACCOUNT_NAME));
        }
    }
}
