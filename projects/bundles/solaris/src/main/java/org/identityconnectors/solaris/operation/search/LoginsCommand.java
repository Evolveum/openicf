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

import static org.identityconnectors.solaris.attr.NativeAttribute.COMMENT;
import static org.identityconnectors.solaris.attr.NativeAttribute.DAYS_BEFORE_TO_WARN;
import static org.identityconnectors.solaris.attr.NativeAttribute.DIR;
import static org.identityconnectors.solaris.attr.NativeAttribute.GROUPS_SEC;
import static org.identityconnectors.solaris.attr.NativeAttribute.GROUP_PRIM;
import static org.identityconnectors.solaris.attr.NativeAttribute.ID;
import static org.identityconnectors.solaris.attr.NativeAttribute.LOCK;
import static org.identityconnectors.solaris.attr.NativeAttribute.MAX_DAYS_BETWEEN_CHNG;
import static org.identityconnectors.solaris.attr.NativeAttribute.MIN_DAYS_BETWEEN_CHNG;
import static org.identityconnectors.solaris.attr.NativeAttribute.NAME;
import static org.identityconnectors.solaris.attr.NativeAttribute.PWSTAT;
import static org.identityconnectors.solaris.attr.NativeAttribute.SHELL;
import static org.identityconnectors.solaris.attr.NativeAttribute.USER_EXPIRE;
import static org.identityconnectors.solaris.attr.NativeAttribute.USER_INACTIVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

class LoginsCommand {

    /** a hard-coded set of constants used provided by Logins command. DO NOT CHANGE */
    private static final Set<NativeAttribute> set;
    static {
        set = EnumSet.of(COMMENT, DAYS_BEFORE_TO_WARN, DIR, GROUPS_SEC,
                GROUP_PRIM, LOCK, MAX_DAYS_BETWEEN_CHNG, MIN_DAYS_BETWEEN_CHNG, 
                PWSTAT, SHELL, ID, USER_EXPIRE, USER_INACTIVE);
        /*
         * NativeAttribute.NAME is left out from the 'set' on purpose. The
         * reason is that the name is already known to the issuer of logins
         * command.
         */
    }

    public static SolarisEntry getAttributesFor(String username, SolarisConnection conn) {
        SolarisEntry entry = null;
        try {
            final String cmd = conn.buildCommand("logins -oxma -l ", username);
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

    /*
     * IMPLEMENTATION NOTE:
     * the logins command provides a fixed set of {@link NativeAttribute}-s. If
     * the implementation is changed, don't forget to update the list of
     * acquired attributes: {@link LoginsCmd#set}.
     */
    public static SolarisEntry getEntry(String accountLine, String username) {
        final SolarisEntry.Builder bldr = new SolarisEntry.Builder(username);
        
        /* tokens delimited by ":" */
        final String[] tokens = accountLine.split(":");
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
        bldr.addAttr(NAME, username);
        /* USER UID */
        bldr.addAttr(ID, tokenIt.next());
        
        /* PRIMARY GROUP NAME */
        bldr.addAttr(GROUP_PRIM, tokenIt.next());
        /* PRIMARY GROUP GID - skip */
        tokenIt.next();
        
        bldr.addAttr(COMMENT, tokenIt.next());
        
        
        
        /* SECONDARY GROUPS */
        /** minimal number of tokens in the output of logins command */
        final int MIN_TOKENS = 14;
        final int totalTokens = tokens.length;
        if (totalTokens < MIN_TOKENS) {
            throw new RuntimeException("Error: Missing tokens in output for user '" + username + "'" + ", accountLine: <" + accountLine + ">");
        }
        
        final int numSecondaryGroups = (totalTokens-MIN_TOKENS)/2;
        final List<Object> secondaryGroupNames = new ArrayList<Object>(numSecondaryGroups);
        
        for (int i = 0; i < numSecondaryGroups; i++) {
            // store secondary group name
            secondaryGroupNames.add(tokenIt.next());
            // ignore secondary group GID
            tokenIt.next();
        }
        
        
        
        bldr.addAttr(GROUPS_SEC, secondaryGroupNames);
        bldr.addAttr(DIR, tokenIt.next());
        bldr.addAttr(SHELL, tokenIt.next());
        
        
        
        /* PWSTAT + PASSWD_LOCK */
        final String pwstat = tokenIt.next();
        if ("PS".equals(pwstat)) {
            bldr.addAttr(PWSTAT, Boolean.TRUE.toString());
        } else if ("LK".equals(pwstat)) {
            bldr.addAttr(LOCK, Boolean.TRUE.toString());
        } else {
            bldr.addAttr(LOCK, Collections.emptyList());
            bldr.addAttr(PWSTAT, Collections.emptyList());
        }
        
        /* PASSWD CHANGE - skip */
        tokenIt.next();
        
        
        
        bldr.addAttr(MIN_DAYS_BETWEEN_CHNG, tokenIt.next());
        bldr.addAttr(MAX_DAYS_BETWEEN_CHNG, tokenIt.next());
        bldr.addAttr(DAYS_BEFORE_TO_WARN, tokenIt.next());
        
        /* USER INACTIVE */
        String userInactive = tokenIt.next();
        if (userInactive.equals("-1")) {
            // This is set to not expire and security modules may
            // not even be installed on the host so reset this to null.
            userInactive = null;
        }
        bldr.addAttr(USER_INACTIVE, userInactive);
        
        /* USER EXPIRE */
        String userExpire = tokenIt.next();
        if (userExpire.equals("0") || userExpire.equals("000000")) {
            // This is set to not expire and security modules may
            // not even be installed on the host so reset this to null.
            userExpire = null;
        }
        bldr.addAttr(USER_EXPIRE, userExpire);
        
        return bldr.build();
    }
    
    /**
     * @param attr the attribute in question.
     * @return true if the attribute is provided by {@link LoginsCommand}.
     */
    private static boolean isProvided(NativeAttribute attr) {
        return set.contains(attr);
    }
    
    /**
     * evaluate if logins command is required for retrieval of given attributes
     * 
     * @param attrs
     *            attributes
     * @return true if {@link LoginsCommand} is required to be called, as it
     *         yields at least one of attributes on the list.
     */
    public static boolean isLoginsRequired(Set<NativeAttribute> attrs) {
        for (NativeAttribute nativeAttribute : attrs) {
            if (LoginsCommand.isProvided(nativeAttribute)) {
                return true;
            }
        }
        return false;
    }
}
