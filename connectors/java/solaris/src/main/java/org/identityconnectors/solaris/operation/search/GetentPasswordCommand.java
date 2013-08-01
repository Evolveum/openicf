/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 *
 * Portions Copyright 2008-2009 Sun Microsystems
 */
package org.identityconnectors.solaris.operation.search;

import static org.identityconnectors.solaris.attr.NativeAttribute.COMMENT;
import static org.identityconnectors.solaris.attr.NativeAttribute.DIR;
import static org.identityconnectors.solaris.attr.NativeAttribute.ID;
import static org.identityconnectors.solaris.attr.NativeAttribute.NAME;
import static org.identityconnectors.solaris.attr.NativeAttribute.SHELL;

import java.util.Arrays;
import java.util.Iterator;

import org.identityconnectors.common.logging.Log;

/**
 * @author Radovan Semancik
 *
 */
public final class GetentPasswordCommand {

    private static final Log logger = Log.getLog(GetentPasswordCommand.class);

    private GetentPasswordCommand() {
    }

    public static SolarisEntry getEntry(String line, String username) {
        final SolarisEntry.Builder bldr = new SolarisEntry.Builder(username);

        final String[] tokens = line.split(":", -1);
        final Iterator<String> tokenIt = Arrays.asList(tokens).iterator();

        // Username
        final String foundUser = tokenIt.next();
        if (!username.equals(foundUser)) {
            logger.warn("The fetched username differs from what was expected (in getent passwd): fetched = '"
                    + foundUser + "', expected = '" + username + "'.");
            return null;
        }
        bldr.addAttr(NAME, username);

        // Password placeholder. Skip.
        tokenIt.next();

        // UID
        bldr.addAttr(ID, Integer.valueOf(tokenIt.next()));

        // Primary group GID. This is useless now. Skip.
        tokenIt.next();

        // GECOS
        bldr.addAttr(COMMENT, tokenIt.next());

        // Homedir
        bldr.addAttr(DIR, tokenIt.next());

        // Login shell
        bldr.addAttr(SHELL, tokenIt.next());

        return bldr.build();
    }

}
