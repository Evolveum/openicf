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

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.command.ClosureFactory;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

import expect4j.matches.Match;


/**
 * @author David Adam
 *
 */
class PasswdCommand extends CommandSwitches {
    private final static Match[] errorsPasswd;
    static {
        MatchBuilder builder = new MatchBuilder();
        builder.addCaseInsensitiveRegExpMatch("Permission denied", ClosureFactory.newConnectorException("Permission denied when executing 'passwd'"));
        builder.addCaseInsensitiveRegExpMatch("command not found", ClosureFactory.newConnectorException("'passwd' command not found"));
        builder.addCaseInsensitiveRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("current user is not allowed to execute 'passwd' command"));
        errorsPasswd = builder.build();
    }
    
    public PasswdCommand() {
        // empty intentionally
    }
    
    public void configureUserPassword(SolarisEntry entry, GuardedString password, SolarisConnection conn) {
        try {
            String command = String.format("passwd -r files %s", entry.getName());
            conn.send(command);

            Match[] matches = prepareMatches("New Password", errorsPasswd);
            conn.expect(matches);
            SolarisUtil.sendPassword(password, conn);

            matches = prepareMatches("Re-enter new Password:", errorsPasswd);
            conn.expect(matches);
            SolarisUtil.sendPassword(password, conn);

            conn.waitFor(String.format("passwd: password successfully changed for %s", entry.getName()));
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }
}
