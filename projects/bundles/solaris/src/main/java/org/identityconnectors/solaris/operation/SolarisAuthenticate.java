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
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;

public class SolarisAuthenticate extends AbstractOp {

    private static final Log log = Log.getLog(SolarisAuthenticate.class);
    
    private SolarisConnection connection;
    
    private static final String MSG = "authenticateMessage";
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT};
    
    public SolarisAuthenticate(SolarisConnector connector) {
        super(connector);
        connection = connector.getConnection();
    }

    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        // The script works by trying to execute another login from
        // within the system, then tries to echo a unique string back
        // but makes sure that the text incorrect didn't come back.
        // N.B. from here on, the SolarisConnection will be logged in
        // as the authenticated user (assuming the authentication succeeded)
        // and NOT as root.  If the connection is reused after the
        // authentication, things may not behave as expected.  The logout
        // that will presumably be called eventually will logout all
        // the way out of the system, including the original root shell.
        
        SolarisUtil.controlObjectClassValidity(objectClass, acceptOC, getClass());
        log.info("authenticate (user: '{0}')", username);
        
        final SolarisConfiguration configuration = connection.getConfiguration();
        final String loginShellPrompt = configuration.getLoginShellPrompt();
        // in case we logged in with different user that the root user, the following applies:
        // since we have to exec login from the lowest shell, we have to exit
        // here to the login shell from the root shell before we can
        // authenticate the user.
        if (!configuration.isSudoAuthorization() && configuration.isSuAuthorization()) {
            connection.executeCommand("exit", Collections.<String>emptySet(), CollectionUtil.newSet(loginShellPrompt));
        }
        
        final Map<String, SolarisConnection.ErrorHandler> rejectsMap = initRejectsMap(username);
        final String command = "exec login " + username + " TERM=vt00";
        
        connection.executeCommand(command, Collections.<String>emptySet(), CollectionUtil.newSet("assword:"));
        SolarisConnection.sendPassword(password, connection);
        connection.executeCommand("echo '" + MSG + "'", rejectsMap, CollectionUtil.newSet(MSG));
        log.info("authenticate successful for user: '{0}'", username);
        
        return new Uid(username);
    }

    private Map<String, SolarisConnection.ErrorHandler> initRejectsMap(final String username) {
        Map<String, SolarisConnection.ErrorHandler> rejectsMap = CollectionUtil.newMap(

                "incorrect", new SolarisConnection.ErrorHandler() {
                    public void handle(String buffer) {
                        throw new InvalidCredentialException("Incorrect authentication for user: " + username + "");
                    }
                },

                "lowest level \"shell\"", new SolarisConnection.ErrorHandler() {
                    public void handle(String buffer) {
                        throw new ConnectorException("ERROR: buffer contents: <" + buffer + ">");
                    }
                }
                );
        return rejectsMap;
    }
}
