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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.Pair;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.constants.SolarisAttribute;
import org.identityconnectors.solaris.operation.AbstractOp;

/**
 * This class is used to perform search for various attributes during one run of
 * search command.
 * 
 * Note: This object *should* *not* be used or cached within two separate
 * searches.
 * 
 * @author David Adam
 */
public class SearchPerformer {
    /** first string denotes the command, the second denotes the output. */
    private Map<String, String[]> cachedCommands;
    private SolarisConnection connection;
    private SolarisConfiguration configuration;
    private AbstractOp operation;

    /** constructor for unit tests only */
    SearchPerformer(SolarisConfiguration configuration, SolarisConnection connection) {
        cachedCommands = new HashMap<String, String[]>();
        this.connection = connection;
        this.configuration = configuration;
    }
    
    /**
     * construct a new search performer
     * @param configuration
     * @param connection
     * @param opSearchImpl the operation that is source of information turned on sudo command
     */
    public SearchPerformer(SolarisConfiguration configuration,
            SolarisConnection connection, AbstractOp opSearchImpl) {
        this(configuration, connection);
        operation = opSearchImpl;
    }

    public Set<Uid> performSearch(SolarisAttribute attribute, String searchRegExp) {
        return performSearch(attribute, searchRegExp, null);
    }

    /**
     * {@see SearchPerformer#performValueSearchForUid(SolarisAttribute, String, String)}
     * @param callback this interface is called back to filter output of the unix command that acquires the search attribute.
     * @return list of attribute values, that satisfy the criteria.
     */
    private Set<Uid> performSearch(SolarisAttribute attribute, String searchRegExp, String uid, SearchCallback callback) {
        // try to substitute username if needed in the command.
        final String command = buildCommand(attribute, uid);
        final String[] output = cacheRequest(command);
        Pattern p = Pattern.compile(attribute.getRegExpForUidAndAttribute());
        Set<Uid> result = new HashSet<Uid>();
        for (String line : output) {
            // PAIR: Uid + attributeValue for the searched attribute
            Pair<Uid, String> grepResult = null;

            grepResult = callback.getUidAndAttr(line, p); //getUidAndAttr(line, p);

            // in case there's a match with the searched regular expression:
            if (grepResult != null) {
                if (grepResult.second != null
                        && grepResult.second.matches(searchRegExp)) {
                    result.add(grepResult.first);
                } 
            }
        }

        return result;
    }

    private String buildCommand(SolarisAttribute attribute, String uid) {
        String command = ((uid == null) ? attribute.getCommand() : attribute.getCommand(uid));
        if (operation != null) {
            command = operation.getCmdBuilder().build(command);
        }
        return command;
    }
    
    /**
     * search for given attribute, and apply the given regular expression
     * pattern to the values to filter them.
     * 
     * @param searchRegExp
     *            the regular expression, that is used to match the attribute
     *            values. These values should satisfy the criteria of the
     *            search.
     * @param attribute
     *            the attribute that we are looking for
     * @param uid
     *            the uid used to filter the search result (it might be depend
     *            on presence of __placeholders__ in the
     *            {@link SolarisAttribute#getCommand(String...)}
     * @return list of attribute values, that satisfy the criteria.
     */
    public Set<Uid> performSearch(SolarisAttribute attribute, String searchRegExp, String uid) {
         return performSearch(attribute, searchRegExp, uid,
                new SearchCallback() {
                    public Pair<Uid, String> getUidAndAttr(String line, Pattern... pattern) {
                        return getUidAndAttrImpl(line, pattern);
                    }
                });
    }
    
    /**
     * Search the value of given attribute for an Account identified by given Uid.
     * @param attribute searched attribute
     * @param searchRegExp the regular expression to pick the [uid, attributevalue] pair from raw input
     * @param uid that we are interested in.
     * @param callback this interface is called back to filter output of the unix command that acquires the search attribute.
     * @return the value of the attribute.
     */
    public List<String> performValueSearchForUid(SolarisAttribute attribute, String searchRegExp, String uid, SearchCallback callback) {
        // try to substitute username if needed in the command.
        Assertions.nullCheck(uid, "uid");
        final String command = attribute.getCommand(uid);
        if (command == null) {
            return null;
        }
        
        final String[] output = cacheRequest(command);
        Pattern p = Pattern.compile(attribute.getRegExpForUidAndAttribute());
        List<String> result = new ArrayList<String>();
        for (String line : output) {
            Pair<Uid, String> grepResult = null;
            
            grepResult = callback.getUidAndAttr(line, p);
             
            if (grepResult != null && grepResult.first.getUidValue().equals(uid)) {
                result.add(grepResult.second);
                break; // expecting to match only a single line.
            }
        }
        
        return result;
    }
 
    /**
     * A more simple version of search for an attribute based on Uid.
     * {@see SearchPerformer#performValueSearchForUid(SolarisAttribute, String, String)}
     */
    public List<String> performValueSearchForUid(SolarisAttribute attribute, String searchRegExp, String uid) {
        return performValueSearchForUid(attribute, searchRegExp, uid,
                new SearchCallback() {
                    public Pair<Uid, String> getUidAndAttr(String line, Pattern... pattern) {
                        return getUidAndAttrImpl(line, pattern);
                    }
                });
    }

    /** @return null if no match, and a pair otherwise. */
    private Pair<Uid, String> getUidAndAttrImpl(String line, Pattern... pattern) {
        // PAIR: Uid + attributeValue for the searched attribute
        Pair<Uid, String> pair = null;
        
        Matcher matcher = pattern[0].matcher(line); // FIXME do it for rall patterns
        if (matcher.matches()) {
            final int groupCnt = matcher.groupCount();
            switch (groupCnt) {
            case 1:
                pair = new Pair<Uid, String>();
                // assuming that a single column is always Uid.
                pair.first = new Uid(matcher.group(1));
                pair.second = pair.first.getUidValue();// fixme
                break;
            case 2:
                pair = new Pair<Uid, String>();
                pair.first = new Uid(matcher.group(1));
                pair.second = matcher.group(2);
                break;
            }//switch
        } 
        return pair;
    }

    private String[] cacheRequest(String command) {
        String[] output = null;
        final String[] cacheQuery = cachedCommands.get(command);
        // check if the command is cached
        if (cacheQuery == null) {
            output = performCmd(command);
            cachedCommands.put(command, output);
        } else {
            output = cacheQuery;
        }
        return output;
    }

    private String[] performCmd(String command) {
        String output = null;
        try {
            try {
                // this cleans expect4j's buffer from previous output.
                connection.waitFor(configuration.getRootShellPrompt(),
                        SolarisConnection.WAIT);
            } catch (Exception e) {
                // OK
            }

            output = connection.executeCommand(command);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        return output.split("\n");
    }
    
    public interface SearchCallback {
        public Pair<Uid, String> getUidAndAttr(String line, Pattern... pattern);
    }
}
