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

package org.identityconnectors.solaris.operation.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Used for iterations blockwise on a list of given accounts, or the iterator
 * can fetch the accounts list on its own. Note: does not support NIS accounts,
 * use {@link AccountIterator} instead.
 *
 * @author David Adam
 * @author Radovan Semancik
 */
public class BlockAccountIterator implements Iterator<SolarisEntry> {

    /** list of *all* usernames on the resource. */
    private List<String> accounts;

    /** of last time of user login is required. */
    private boolean isLast;

    /** iterates through the full list of usernames. */
    private ListIterator<String> usernameIter;
    /** iterates through block of accounts. */
    private Iterator<SolarisEntry> entryIter;
    private SolarisConnection conn;

    /** size of the blocks that the accounts are iterated. */
    private final int blockSize;
    private int blockCount = -1;

    private static final Log logger = Log.getLog(BlockAccountIterator.class);

    BlockAccountIterator(Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        this(Collections.<String> emptyList(), attrsToGet, conn);
    }

    BlockAccountIterator(List<String> usernames, Set<NativeAttribute> attrsToGet,
            SolarisConnection conn) {
        this(usernames, attrsToGet, conn.getConfiguration().getBlockSize(), conn);
    }

    BlockAccountIterator(List<String> usernames, Set<NativeAttribute> attrsToGet, int blockSize,
            SolarisConnection conn) {
        if (conn.isNis()) {
            throw new UnsupportedOperationException(
                    "internal error: BlockAccountIterator does not support NIS accounts, use AccountIterator instead.");
        }
        this.conn = conn;
        this.blockSize = blockSize;

        if (CollectionUtil.isEmpty(usernames)) {
            // fetch usernames
            conn.doSudoStart();
            String command =
                    conn.buildCommand(false, "cut -d: -f1 /etc/passwd | grep -v \"^[+-]\"");
            String usernamesNewLineSeparated = conn.executeCommand(command);
            String[] usernamesList = usernamesNewLineSeparated.split("\n");
            usernames = CollectionUtil.<String> newList();
            for (String string : usernamesList) {
                usernames.add(string.trim());
            }
        }
        accounts = usernames;
        usernameIter = accounts.listIterator();
        entryIter = initNextBlockOfAccounts();

        boolean isProfiles = attrsToGet.contains(NativeAttribute.PROFILES);
        boolean isAuths = attrsToGet.contains(NativeAttribute.AUTHS);
        isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);
        boolean isRoles = attrsToGet.contains(NativeAttribute.ROLES);

        // todo: nice to have feature: any combination of attributes could be
        // accepted., but this involves a complicated changes in the scripting.
        if (isRoles || isAuths || isProfiles) {
            throw new UnsupportedOperationException(
                    "Internal Error: roles, auths, profiles are not supported for batch retrieval of accounts. Use individual retrieval. Use AccountIterator.");
        }
    }

    private Iterator<SolarisEntry> initNextBlockOfAccounts() {
        blockCount++;

        List<String> blockUserNames = new ArrayList<String>(blockSize);
        for (int i = 0; usernameIter.hasNext() && i < blockSize; i++) {
            blockUserNames.add(usernameIter.next());
        }

        List<SolarisEntry> blockEntries =
                conn.getModeDriver().buildAccountEntries(blockUserNames, isLast);

        if (logger.isInfo()) {
            for (SolarisEntry entry : blockEntries) {
                logger.info("Entry: {0}", entry);
            }
        }

        return blockEntries.iterator();
    }

    public boolean hasNext() {
        while ((entryIter == null || !entryIter.hasNext()) && usernameIter.hasNext()) {
            entryIter = initNextBlockOfAccounts();
        }
        return entryIter != null && entryIter.hasNext();
    }

    public SolarisEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return entryIter.next();
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "Internal error: AccountIterators do not allow remove().");
    }
}
