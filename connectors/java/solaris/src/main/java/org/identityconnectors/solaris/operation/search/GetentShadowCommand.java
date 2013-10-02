/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 *
 * Portions Copyright 2008-2009 Sun Microsystems
 */
package org.identityconnectors.solaris.operation.search;

import java.util.Arrays;
import java.util.Iterator;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry.Builder;

/**
 * @author Radovan Semancik
 *
 */
public final class GetentShadowCommand {

    private static final Log logger = Log.getLog(GetentShadowCommand.class);

    private GetentShadowCommand() {
    }

    public static SolarisEntry getEntry(String line, String username) {
        final SolarisEntry.Builder bldr = new SolarisEntry.Builder(username);

        final String[] tokens = line.split(":", -1);
        final Iterator<String> tokenIt = Arrays.asList(tokens).iterator();

        // Username
        final String foundUser = tokenIt.next();
        if (!username.equals(foundUser)) {
            logger.warn("The fetched username differs from what was expected (in getent shadow): fetched = '"
                    + foundUser + "', expected = '" + username + "'.");
            return null;
        }
        bldr.addAttr(NativeAttribute.NAME, username);

        // Hashed password. Skip.
        tokenIt.next();

        // last password change
        addAttrLong(bldr, NativeAttribute.LAST_PASSWORD_CHANGE, tokenIt.next());

        // days until change allowed
        addAttrLong(bldr, NativeAttribute.MIN_DAYS_BETWEEN_CHNG, tokenIt.next());
        
        // days before change required
        addAttrLong(bldr, NativeAttribute.MAX_DAYS_BETWEEN_CHNG, tokenIt.next());
        
        // days warning for expiration
        addAttrLong(bldr, NativeAttribute.DAYS_BEFORE_TO_WARN, tokenIt.next());
        
        // days before account inactive
        addAttrLong(bldr, NativeAttribute.USER_INACTIVE, tokenIt.next());

        // date when account expires
        addAttrLong(bldr, NativeAttribute.USER_EXPIRE, tokenIt.next());

        return bldr.build();
    }

	private static void addAttrLong(Builder bldr,
			NativeAttribute nativeAttr, String stringVal) {
		if (StringUtil.isBlank(stringVal)) {
			return;
		}
		bldr.addAttr(nativeAttr, Long.valueOf(stringVal));
		
	}

}
