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

import static org.identityconnectors.solaris.attr.NativeAttribute.DAYS_BEFORE_TO_WARN;
import static org.identityconnectors.solaris.attr.NativeAttribute.LOCK;
import static org.identityconnectors.solaris.attr.NativeAttribute.MAX_DAYS_BETWEEN_CHNG;
import static org.identityconnectors.solaris.attr.NativeAttribute.MIN_DAYS_BETWEEN_CHNG;
import static org.identityconnectors.solaris.attr.NativeAttribute.NAME;
import static org.identityconnectors.solaris.attr.NativeAttribute.PWSTAT;
import static org.identityconnectors.solaris.attr.NativeAttribute.USER_INACTIVE;

import java.util.Arrays;
import java.util.Iterator;

import org.identityconnectors.common.logging.Log;

/**
 * @author Radovan Semancik
 *
 */
public final class PasswdCommand {

    private static final Log logger = Log.getLog(PasswdCommand.class);

    private PasswdCommand() {
    }

    public static SolarisEntry getEntry(String line, String username) {
        final SolarisEntry.Builder bldr = new SolarisEntry.Builder(username);

        final String[] tokens = line.split(" ", -1);
        final Iterator<String> tokenIt = Arrays.asList(tokens).iterator();

        // Username
        final String foundUser = tokenIt.next();
        if (!username.equals(foundUser)) {
            logger.warn("The fetched username differs from what was expected (in passwd -S): fetched = '"
                    + foundUser + "', expected = '" + username + "'.");
            return null;
        }
        bldr.addAttr(NAME, username);

        if (tokenIt.hasNext()) {
	        /* PWSTAT + PASSWD_LOCK */
	        final String pwstat = tokenIt.next();
	        boolean isPwStat = false;
	        boolean isLock = false;
	        if ("PS".equals(pwstat)) {
	            isPwStat = true;
	        }
	        if ("LK".equals(pwstat)) {
	            isLock = true;
	        }
	        bldr.addAttr(PWSTAT, isPwStat);
	        bldr.addAttr(LOCK, isLock);
	
	        if (tokenIt.hasNext()) {
		        /* PASSWD CHANGE - skip */
		        tokenIt.next();
		        
		        if (tokenIt.hasNext()) {
			        bldr.addAttr(MIN_DAYS_BETWEEN_CHNG, Integer.valueOf(tokenIt.next()));
			        
			        if (tokenIt.hasNext()) {
				        bldr.addAttr(MAX_DAYS_BETWEEN_CHNG, Integer.valueOf(tokenIt.next()));
				        
				        if (tokenIt.hasNext()) {
				        	bldr.addAttr(DAYS_BEFORE_TO_WARN, Integer.valueOf(tokenIt.next()));
				        	
				        	if (tokenIt.hasNext()) {
						        /* USER INACTIVE */
						        Integer userInactive = Integer.valueOf(tokenIt.next());
						        if (userInactive.equals(-1)) {
						            // This is set to not expire and security modules may
						            // not even be installed on the host so reset this to null.
						            userInactive = null;
						        }
						        bldr.addAttr(USER_INACTIVE, userInactive);
				        	}
	
				        }
			        }
		        }
		
	        }
        }

        return bldr.build();
    }

}
