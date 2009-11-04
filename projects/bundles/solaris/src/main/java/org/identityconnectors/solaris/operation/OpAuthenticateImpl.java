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
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;

public class OpAuthenticateImpl extends AbstractOp {

    private static final Log _log = Log.getLog(OpAuthenticateImpl.class);
    
    private static final String MSG = "authenticateMessage";
    final ObjectClass[] acceptOC = {ObjectClass.ACCOUNT};
    
    public OpAuthenticateImpl(SolarisConnector conn) {
        super(conn);
    }

    public Uid authenticate(ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options) {
        SolarisUtil.controlObjectClassValidity(objectClass, acceptOC, getClass());
        _log.info("authenticate (user: '{0}')", username);
        
        final Map<String, SolarisConnection.ErrorHandler> rejectsMap = initRejectsMap(username);
        final String command = "exec login " + username + " TERM=vt00";
        
        getConnection().executeCommand(command, Collections.<String>emptySet(), CollectionUtil.newSet("assword:"));
        SolarisConnection.sendPassword(password, getConnection());
        getConnection().executeCommand("echo '" + MSG + "'", rejectsMap, CollectionUtil.newSet(MSG));
        _log.info("authenticate successful for user: '{0}'", username);
        
        return new Uid(username);
        /*
            // since we have to exec login from the lowest shell, we have to exit
    // here to the login shell from the root shell before we can
    // authenticate the user.
    if (!sudoAuthorization() && !loginUser.equals(rootUser)) {
        script.addToken(new ScriptToken.Send("exit"));
        script.addToken(new ScriptToken.WaitFor(loginShellPrompt, getWaitFor().getTimeout()));
    }

        // By specifying the TERM variable, we prevent a ttytype command
        //   from running out of the default /etc/profile on HP-UX.  The
        //   ttytype command is bad because it "eats" input.  Since we
        //   are not waiting on a shell prompt (we don't know what the
        //   prompt would look like), we immediately send the echo
        //   command.  When ttytype "eats" it, it is never executed and
        //   therefore our script will fail.  This is obviously fragile,
        //   but I guess no more fragile than the entire concept of
        //   scripting.
        // It doesn't hurt to use it on other platforms since we shouldn't
        //   be doing anything that requires a specific term type.
    script.addToken(new ScriptToken.Send("exec login " + accountID +
                                  " TERM=vt100"));
    script.addToken(new ScriptToken.WaitForIgnoreCase("assword:", getWaitForIgnoreCase().getTimeout()));
    script.addToken(new ScriptToken.SendPassword(password));
    script.addToken(
        new ScriptToken.Send("echo '" + UNIQUE_MESSAGE + "'"));
    script.addToken(new ScriptToken.WaitFor(UNIQUE_MESSAGE, new String[]{"incorrect", "lowest level \"shell\""},
            getWaitFor().getTimeout()));

    return script;
    }
         */
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
