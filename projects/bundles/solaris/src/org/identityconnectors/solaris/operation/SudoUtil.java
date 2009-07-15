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

import java.io.IOException;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.command.closure.ErrorClosure;
import org.identityconnectors.solaris.command.closure.NullClosure;

class SudoUtil {
    private static final String SUDO_START_COMMAND = "sudo -v";
    private static final String SUDO_RESET_COMMAND = "sudo -k";
    
    // purely based on RA, TODO test 
    public static void doSudoStart(SolarisConfiguration config, final SolarisConnection conn) {
        if (config.isSudoAuth()) {
            try {
                // 1) send sudo reset command
                conn.send(SUDO_RESET_COMMAND); // TODO CommandBuilder might be user for this
                conn.expect(MatchBuilder.buildRegExpMatch("not found", new ErrorClosure("Sudo command is not found")));

                // 2) send sudo start command
                conn.send(SUDO_START_COMMAND); // TODO CommandBuilder might be user for this
                conn.waitForCaseInsensitive("assword:");
                // TODO evaluate which password should be used:
                GuardedString passwd = config.getPassword();
                passwd.access(new GuardedString.Accessor() {
                    public void access(char[] clearChars) {
                        try {
                            conn.send(new String(clearChars));
                        } catch (IOException e) {
                            ConnectorException.wrap(e);
                        }
                    }
                });
                
                // 3) wait for the end of sudo operation
                MatchBuilder builder = new MatchBuilder();
                builder.addRegExpMatch(config.getRootShellPrompt(), new NullClosure());// TODO possibly replace NullClosure with null.
                // signs of password reject:
                builder.addRegExpMatch("may not run", new ErrorClosure("Not sufficient permissions")); // TODO improve error msg
                builder.addRegExpMatch("not allowed to execute", new ErrorClosure("Not sufficient permissions"));// TODO improve error msg
                conn.expect(builder.build());
            } catch (Exception e) {
                ConnectorException.wrap(e);
            }
        }
    }
    
    // purely based on RA, TODO test 
    public static void doSudoReset(SolarisConfiguration config, SolarisConnection conn) {
        if (config.isSudoAuth()) {
            // 1) send sudo reset command
            try {
                conn.send(SUDO_RESET_COMMAND);
                conn.waitFor(config.getRootShellPrompt());
            } catch (Exception e) {
                ConnectorException.wrap(e);
            }
        }
    }
}
