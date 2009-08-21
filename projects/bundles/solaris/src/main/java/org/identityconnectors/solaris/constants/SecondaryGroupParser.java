package org.identityconnectors.solaris.constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.identityconnectors.common.Pair;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.operation.search.SearchPerformer.SearchCallback;

/**
 * Handle the output of parsing the extended 'logins -oxma' command
 * {@link AccountAttributes.CommandConstants.Logins#CMD_EXTENDED}.
 * 
 * @author David Adam
 */
class SecondaryGroupParser implements SearchCallback {

    private SecondaryGroupParser() {
    }

    // enforce the singleton pattern
    static class Builder {
        private static SecondaryGroupParser sgp = new SecondaryGroupParser();

        static SecondaryGroupParser getInstance() {
            return sgp;
        }
    }

    /**
     * @param loginsCommandResult the line that is parsed for secondary groups, it is the output line of logins command, {@see AccountAttributes
     * @param pattern this argument is ignored.
     * @return the list of the groupnames separated by comma.
     */
    public Pair<Uid, String> getUidAndAttr(String loginsCommandResult,
            Pattern pattern) {
        // /*
        // * the implementation is base on
        // SVIDResrouceAdapter#buildUser(loginsResult, targetUser) method.
        // */
        final int minTokens = AccountAttributes.CommandConstants.Logins.COL_COUNT;
        Collection<String> loginsTokens = Arrays.asList(loginsCommandResult.split(AccountAttributes.CommandConstants.Logins.DEFAULT_OUTPUT_DELIMITER));

        if (loginsTokens.size() < minTokens) {
            throw new ConnectorException("ERROR: too little tokens retrieved from 'logins' command.");
        }

        /*
         * The logins result is colon delimited and looks like this:
         * 
         * name:uid:group_1:groupnum_1:comment: [group_i:groupnum_i:]
         * dir:shell:pwstat:pwlastchange:
         * mindaysbetweenchange:maxdaysbetweenchange:daysbeforetowarn
         * 
         * the [] part can be repeated 0 or more times if the user has
         * additional groups.
         */

        // handle login info
        int totalTokens = loginsTokens.size();
        Iterator<String> tokenIt = loginsTokens.iterator();

        String accountId = tokenIt.next();
        //skip userId, groupName, groupId, userComment
        for (int i = 2; i <= 5; i++) {
            tokenIt.next();
        }

        final int numSecondaryGroups = (totalTokens - minTokens) / 2;
        StringBuilder secondaryGroupNames = new StringBuilder();
        
        for (int i = 0; i < numSecondaryGroups; i++) {
            secondaryGroupNames.append(tokenIt.next());
            /*secondaryGroupIds.append((String) */tokenIt.next()/*)*/;
            if (i < numSecondaryGroups - 1) {
                secondaryGroupNames.append(',');
            }
        }//for
        
        return new Pair<Uid, String>(new Uid(accountId), secondaryGroupNames.toString());
    }

}