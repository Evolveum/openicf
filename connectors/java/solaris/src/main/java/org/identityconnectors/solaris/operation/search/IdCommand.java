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

import static org.identityconnectors.solaris.attr.NativeAttribute.GROUPS_SEC;
import static org.identityconnectors.solaris.attr.NativeAttribute.GROUP_PRIM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Radovan Semancik
 *
 */
public final class IdCommand {

    private IdCommand() {
    }

    public static SolarisEntry getEntry(String line, String username) {
        final SolarisEntry.Builder bldr = new SolarisEntry.Builder(username);

        final String[] tokens = line.split(" ", -1);
        final Iterator<String> tokenIt = Arrays.asList(tokens).iterator();

        // First entry is a primary group
        bldr.addAttr(GROUP_PRIM, tokenIt.next());

        // All the next entries are secondary groups
        final List<String> secondaryGroupNames = new ArrayList<String>();
        while (tokenIt.hasNext()) {
            secondaryGroupNames.add(tokenIt.next());
        }
        bldr.addAttr(GROUPS_SEC, secondaryGroupNames);

        return bldr.build();
    }

}
