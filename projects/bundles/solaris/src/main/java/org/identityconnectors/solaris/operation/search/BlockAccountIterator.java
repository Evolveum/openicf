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
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.SudoUtil;

public class BlockAccountIterator implements Iterator<SolarisEntry> {

    private static final String SHELL_CONT_CHARS = "> ";
    private static final int CHARS_PER_LINE = 160;
    private static final String TMPFILE  = "/tmp/connloginsError.$$";

    /** list of *all* usernames on the resource. */
    private List<String> accounts;
    
    /** of last time of user login is required */
    private boolean isLast;

    /** iterates through the full list of usernames. */
    private ListIterator<String> usernameIter;
    /** iterates through block of accounts. */
    private Iterator<SolarisEntry> accountIter;
    private SolarisConnection conn;

    /** size of the blocks that the accounts are iterated. */
    private final int blockSize;
    private int blockCount = -1;
    private SolarisConfiguration config;

    public BlockAccountIterator(List<String> usernames, Set<NativeAttribute> attrsToGet, SolarisConnection conn, SolarisConfiguration config, int blockSize) {
        this.conn = conn;
        this.blockSize = blockSize;
        this.config = config;

        accounts = usernames;
        usernameIter = accounts.listIterator();
        accountIter = initNextBlockOfAccounts(usernameIter);
        
        boolean isProfiles = attrsToGet.contains(NativeAttribute.PROFILES);
        boolean isAuths = attrsToGet.contains(NativeAttribute.AUTHS);
        isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);
        boolean isRoles = attrsToGet.contains(NativeAttribute.ROLES);
        
        // todo: nice to have feature: any combination of attributes could be accepted., but this involves a complicated changes in the scripting.
        if (isRoles || isAuths || isProfiles) {
            throw new UnsupportedOperationException("Internal Error: roles, auths, profiles are not supported for batch retrieval of accounts. Use individual retrieval. Use AccountIterator.");
        }
    }
    
    private Iterator<SolarisEntry> initNextBlockOfAccounts(Iterator<String> globalIterator) {
        blockCount++;
        
        List<String> blockUserNames = new ArrayList<String>(blockSize);
        for (int i = 0; globalIterator.hasNext() && i < blockSize; i++) {
            blockUserNames.add(globalIterator.next());
        }
        
        List<SolarisEntry> blockEntries = buildEntries(blockUserNames);
        
        return blockEntries.iterator();
    }
    
    /**
     * get the attributes for given block of users
     * @param blockUserNames
     * @return the SolarisEntry list initialized with the required attributes.
     */
    private List<SolarisEntry> buildEntries(List<String> blockUserNames) {
        SudoUtil.doSudoStart(config, conn);
        conn.executeCommand(conn.buildCommand("rm -f", TMPFILE));
        
        String getUsersScript = buildGetUserScript(blockUserNames);
        final String out = conn.executeCommand(getUsersScript);
        
        conn.executeCommand(conn.buildCommand("rm -f", TMPFILE));
        SudoUtil.doSudoReset(config, conn);
        
        return processOutput(out);
    }

    /** retrieve account info from the output */
    private List<SolarisEntry> processOutput(String out) {
//        SVIDRA# getUsersFromCaptureList(CaptureList captureList, ArrayList users)()
        
        List<String> lines = Arrays.asList(out.split("\n"));
        Iterator<String> it = lines.iterator();
        int captureIndex = 0;
        List<SolarisEntry> result = new ArrayList<SolarisEntry>(blockSize);
        
        while (it.hasNext()) {
            final int accountIndex = captureIndex + (blockCount * blockSize);
            final String currentAccount = accounts.get(accountIndex);
            String token = it.next();
            String lastLoginToken = null;
            
            // Weed out shell continuation chars
            if (token.startsWith(SHELL_CONT_CHARS)) {
                int index = token.lastIndexOf(SHELL_CONT_CHARS);

                token = token.substring(index + SHELL_CONT_CHARS.length());
            }
            
            //TODO clarify meaning of this section.
            if (isLast) {
                if (!it.hasNext()) {
                    throw new ConnectorException(String.format("User '%s' is missing last login time.", currentAccount));
                }

                lastLoginToken = "";

                while (lastLoginToken.length() < 3) {
                    lastLoginToken = it.next();
                }
            }// if (isLast)
            
            SolarisEntry entry = buildUser(currentAccount, token, lastLoginToken);
            if (entry != null) {
                result.add(entry);
            }
            
            captureIndex++;
        }// while (it.hasNext())
        
        return result;
    }

    /**
     * build user based on the content given.
     * @param token
     * @param lastLoginToken
     * @return the build user.
     */
    private SolarisEntry buildUser(String username, String token, String lastLoginToken) {
        if (lastLoginToken == null) {
            return LoginsCmd.getEntry(token, username);
        } else {
            SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(username).addAttr(NativeAttribute.NAME, username);
            // logins
            SolarisEntry entry = LoginsCmd.getEntry(token, username);
            entryBuilder.addAllAttributesFrom(entry);
            
            //last
            Attribute attribute = LastCmd.parseOutput(username, lastLoginToken);
            entryBuilder.addAttr(NativeAttribute.LAST_LOGIN, attribute.getValue());
            
            return entryBuilder.build();
        }
    }

    private String buildGetUserScript(List<String> blockUserNames) {
        // make a list of users, separated by space.
        StringBuilder connUserList = new StringBuilder();
        int charsThisLine = 0;
        for (String user : blockUserNames) {
            final int length = user.length();
            // take care that line meets the limit on 160 chars per line
            if ((charsThisLine + length + 3) > CHARS_PER_LINE) {
                connUserList.append("\n");
                charsThisLine = 0;
            }
            
            connUserList.append(user);
            connUserList.append(" ");
            charsThisLine += length + 1;
        }
        
        StringBuilder getUsersScript = new StringBuilder();
        getUsersScript.append("WSUSERLIST=\"");
        getUsersScript.append(connUserList.toString() + "\n\";");
        getUsersScript.append("for user in $WSUSERLIST; do ");
        
        // FIXME (there's a conditional for USER_TIME_LAST_LOGIN in adapter, why?)
//        if (getAttrTypeFromMapName(USER_TIME_LAST_LOGIN) != null) {
//            String getUserScript =
//                loginsCmd + " -oxma -l $user 2>>" + TMPFILE + "; " +
//                "LASTLOGIN=`" + lastCmd + " -1 $user`; " +
//                "if [ -z \"$LASTLOGIN\" ]; then " +
//                  "echo \"wtmp begins\" ; " +
//                "else " +
//                  "echo $LASTLOGIN; " +
//                "fi; ";
//
//            getUsersScript.append(getUserScript);
//
//        } else {
//            getUsersScript.append(loginsCmd + " -oxma -l $user 2>>" +
//                                  TMPFILE + "; ");
//        }
//
//        getUsersScript.append("done");
        
        String getScript = null;
        if (isLast) {
            getScript = 
                conn.buildCommand("logins") + " -oxma -l $user 2>>" + TMPFILE + "; " +
                "LASTLOGIN=`" + conn.buildCommand("last") + " -1 $user`; " +
                "if [ -z \"$LASTLOGIN\" ]; then " +
                     "echo \"wtmp begins\" ; " +
                "else " +
                     "echo $LASTLOGIN; " +
                "fi; ";
        } else {
            getScript = conn.buildCommand("logins") + " -oxma -l $user 2>>" + TMPFILE + "; ";
        }
        getUsersScript.append(getScript);
        getUsersScript.append("done");
        
        return getUsersScript.toString();
    }

    public boolean hasNext() {
        return accountIter.hasNext() || usernameIter.hasNext();
    }

    public SolarisEntry next() {
        if (accountIter.hasNext()) {
            return accountIter.next();
        } else {
            if (usernameIter.hasNext()) {
                accountIter = initNextBlockOfAccounts(usernameIter);
                return accountIter.next();
            }
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException("Internal error: AccountIterators do not allow remove().");
    }
}
