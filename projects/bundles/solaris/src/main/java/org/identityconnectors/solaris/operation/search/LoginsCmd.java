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

package org.identityconnectors.solaris.operation.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.command.CommandBuilder;

class LoginsCmd implements Command {

    public static SolarisEntry getAttributesFor(String username, SolarisConnection conn, CommandBuilder bldr) {
        SolarisEntry entry = null;
        try {
            final String cmd = bldr.build("logins -oxma -l ", username);
            String out = conn.executeCommand(cmd);
            
            if (out.endsWith("was not found")) {
                throw new UnknownUidException("Unknown username: " + username);
            }
            
            entry = getEntry(out, username);
        } catch (Exception e) {
            ConnectorException.wrap(e);
        }
        return entry;
    }

    private static SolarisEntry getEntry(String out, String username) {
        final SolarisEntry.Builder bldr = new SolarisEntry.Builder(username);
        
        final String[] tokens = out.split(":");
        final Iterator<String> tokenIt = Arrays.asList(tokens).iterator();
        
        /*
         *  The logins result is colon delimited and looks like this:
         *
         *  name:uid:group_1:groupnum_1:comment:
         *  [group_i:groupnum_i:]*
         *  dir:shell:pwstat:pwlastchange:
         *  mindaysbetweenchange:maxdaysbetweenchange:daysbeforetowarn
         *
         *  the []* part can be repeated 0 or more times if the user has
         *  additional groups.
         */
        
        /* NAME */
        final String foundUser = tokenIt.next();
        if (foundUser == null || !username.equals(foundUser)) {
            String msg = String.format("the logins command returned a different user than expected. Expecting: '%s', Returned: '%s'", username, foundUser);
            throw new RuntimeException(msg);
        }
        bldr.addAttr(NativeAttribute.NAME, username);
        /* USER UID */
        bldr.addAttr(NativeAttribute.UID, tokenIt.next());
        
        /* PRIMARY GROUP NAME */
        bldr.addAttr(NativeAttribute.GROUP_PRIM, tokenIt.next());
        /* PRIMARY GROUP GID - skip */
        tokenIt.next();
        
        bldr.addAttr(NativeAttribute.COMMENT, tokenIt.next());
        
        
        
        /* SECONDARY GROUPS */
        /** minimal number of tokens in the output of logins command */
        final int MIN_TOKENS = 14;
        final int totalTokens = tokens.length;
        if (totalTokens < MIN_TOKENS) {
            throw new RuntimeException("Error: Missing tokens in output for user '" + username + "'" + ", output: <" + out + ">");
        }
        
        final int numSecondaryGroups = (totalTokens-MIN_TOKENS)/2;
        final List<String> secondaryGroupNames = new ArrayList<String>(numSecondaryGroups);
        
        for (int i = 0; i < numSecondaryGroups; i++) {
            // store secondary group name
            secondaryGroupNames.add(tokenIt.next());
            // ignore secondary group GID
            tokenIt.next();
        }
        
        
        
        bldr.addAttr(NativeAttribute.GROUPS_SEC, secondaryGroupNames);
        bldr.addAttr(NativeAttribute.DIR, tokenIt.next());
        bldr.addAttr(NativeAttribute.SHELL, tokenIt.next());
        
        
        
        /* PWSTAT + PASSWD_LOCK */
        final String pwstat = tokenIt.next();
        if ("PS".equals(pwstat)) {
            bldr.addAttr(NativeAttribute.PWSTAT, Boolean.TRUE.toString());
        }
        // TODO shouldn't it return false otherwise (see SVIDRA#buildUser(String, WSUser))?
        if ("LK".equals(pwstat)) {
            bldr.addAttr(NativeAttribute.LOCK, Boolean.TRUE.toString());
        }
        // TODO shouldn't it return false otherwise (see SVIDRA#buildUser(String, WSUser))?
        
        /* PASSWD CHANGE - skip */
        tokenIt.next();
        
        
        
        bldr.addAttr(NativeAttribute.MIN_DAYS_BETWEEN_CHNG, tokenIt.next());
        bldr.addAttr(NativeAttribute.MAX_DAYS_BETWEEN_CHNG, tokenIt.next());
        bldr.addAttr(NativeAttribute.DAYS_BEFORE_TO_WARN, tokenIt.next());
        
        /* USER INACTIVE */
        String userInactive = tokenIt.next();
        if (userInactive.equals("-1")) {
            // This is set to not expire and security modules may
            // not even be installed on the host so reset this to null.
            userInactive = null;
        }
        bldr.addAttr(NativeAttribute.USER_INACTIVE, userInactive);
        
        /* USER EXPIRE */
        String userExpire = tokenIt.next();
        if (userExpire.equals("0") || userExpire.equals("000000")) {
            // This is set to not expire and security modules may
            // not even be installed on the host so reset this to null.
            userExpire = null;
        }
        bldr.addAttr(NativeAttribute.USER_EXPIRE, userExpire);
        
        return bldr.build();
    }
}
