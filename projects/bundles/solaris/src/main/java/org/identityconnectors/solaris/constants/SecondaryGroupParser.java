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
     */
    public Pair<Uid, String> getUidAndAttr(String loginsCommandResult,
            Pattern pattern) {
        // /*
        // * the implementation is base on
        // SVIDResrouceAdapter#buildUser(loginsResult, targetUser) method.
        // */
        final int minTokens = AccountAttributes.CommandConstants.Logins.COL_COUNT;
        Collection<String> loginsTokens = Arrays
                .asList(loginsCommandResult
                        .split(AccountAttributes.CommandConstants.Logins.DEFAULT_OUTPUT_DELIMITER));

        if (loginsTokens.size() < minTokens) {
            throw new ConnectorException(
                    "ERROR: too little tokens retrieved from 'logins' command.");
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

        /*
         * XXXXXXXXXXXXXx
         */
        String accountId = tokenIt.next();

        /*
         * XXXXXXXXXXXXXX
         */
        String groupName = tokenIt.next();
        /*
         * XXXXXXXXXXXXXX
         */
        tokenIt.next(); // skip group id
        
        /*
         * XXXXXXXXXXXXXX
         */
        String userComment = tokenIt.next();

        final int numSecondaryGroups = (totalTokens - minTokens) / 2;
        /*
         * XXXXXXXXXXXXXX
         */
        StringBuilder secondaryGroupNames = new StringBuilder();
//        StringBuilder secondaryGroupIds = new StringBuilder();
        
        for (int i = 0; i < numSecondaryGroups; i++) {
            secondaryGroupNames.append(tokenIt.next());
            /*secondaryGroupIds.append((String) */tokenIt.next()/*)*/;
            if (i < numSecondaryGroups - 1) {
                secondaryGroupNames.append(',');
                /*secondaryGroupIds.append(',');*/
            }
        }

        /*
         * XXXXXXXXXXXXXX
         */
        String userDir = tokenIt.next();
        /*
         * XXXXXXXXXXXXXX
         */
        String userShell = tokenIt.next();

        /*
         * XXXXXXXXXXXXXX
         */
        String pwstat = tokenIt.next();
        boolean PASSWD_FORCE_CHANGE = false;
        if ("PS".equals(pwstat))
            PASSWD_FORCE_CHANGE = true;

        boolean disabled = false; // PASSWD_LOCK
        if ("LK".equals(pwstat)) {
            disabled = true;
        }
        
        /*
         * XXXXXXXXXXXXXX
         */
        tokenIt.next(); // skip password change
        /*
         * XXXXXXXXXXXXXX
         */
        String PASSWD_MIN = tokenIt.next();
        /*
         * XXXXXXXXXXXXXX
         */
        String PASSWD_MAX = tokenIt.next();
        /*
         * XXXXXXXXXXXXXX
         */
        String PASSWD_WARN = tokenIt.next();

        /*
         * XXXXXXXXXXXXXX
         */
        String userInactive = tokenIt.next(); //USER_INACTIVE
        if (userInactive.equals("-1")) {
            // This is set to not expire and security modules may
            // not even be installed on the host so reset this to null.
            userInactive = null;
        }

        String userExpire = tokenIt.next(); //USER_EXPIRE
        if (userExpire.equals("0") || userExpire.equals("000000")) {
            // This is set to not expire and security modules may
            // not even be installed on the host so reset this to null.
            userExpire = null;
        }

        // //////////////////////////
        return null;
    }

}