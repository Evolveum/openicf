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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 *
 * Portions Copyrighted 2012 Evolveum, Radovan Semancik
 */
package org.identityconnectors.solaris.operation;

import java.util.Collections;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * @author David Adam
 *
 */
final class PasswdCommand extends CommandSwitches {

    private static final Set<String> NEW_PASSWORD_MATCHES = CollectionUtil.newSet("ew Password:",
    		"ew UNIX password:", "password again:");

    private final static Set<String> PASSWD_REJECTS = CollectionUtil.newSet("Permission denied",
            "command not found", "not allowed to execute");

    private PasswdCommand() {
    }

    public static void configureUserPassword(SolarisEntry entry, GuardedString password,
            SolarisConnection conn) {
        try {
            if (password == null) {
                return;
            }

            String command = conn.getModeDriver().buildPasswdCommand(entry.getName());
            conn.executeCommand(command, PASSWD_REJECTS, NEW_PASSWORD_MATCHES);

            conn.sendPassword(password, Collections.<String> emptySet(), NEW_PASSWORD_MATCHES);

//            conn.sendPassword(password, Collections.<String> emptySet(), NEW_PASSWORD_MATCHES);
            conn.sendPassword(password, Collections.<String> emptySet(), Collections.<String> emptySet());
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    public static void configurePasswordProperties(SolarisEntry entry, SolarisConnection conn) {
        conn.getModeDriver().configurePasswordProperties(entry, conn);
    }
}
