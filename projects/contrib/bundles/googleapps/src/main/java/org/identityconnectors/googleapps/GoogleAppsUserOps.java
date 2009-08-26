/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.identityconnectors.googleapps;

import static org.identityconnectors.common.CollectionUtil.isEmpty;


import com.google.gdata.data.appsforyourdomain.Quota;
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;

/**
 *
 * @author warrenstrange
 */
public class GoogleAppsUserOps {

    GoogleAppsConnection gc;
    Log log = Log.getLog(GoogleAppsUserOps.class);

    GoogleAppsUserOps(GoogleAppsConnection gc) {
        this.gc = gc;
    }

    /**
     * Create a user 
     * @param name
     * @param a
     * @return
     */
    Uid createUser(Name name, AttributesAccessor a) {

        final String accountId = a.getName().getNameValue();

        final GoogleAppsClient g = gc.getConnection();

        //log.info("Extracting Attrs {0}", attrs);

        // todo: this craters if the attribute does not exist (NPE)
        // we should throw a better error
        final String givenName = a.findString(GoogleAppsConnector.ATTR_GIVEN_NAME);
        final String familyName = a.findString(GoogleAppsConnector.ATTR_FAMILY_NAME);
        final Integer quota = a.findInteger(GoogleAppsConnector.ATTR_QUOTA);
        // Password is Operational

        GuardedString password = a.getPassword();


        if (password == null) {
            throw new IllegalArgumentException("The Password attribute cannot be null.");
        }

        final boolean suspended = !a.getEnabled(true);

        String clearPw = getPlainPassword(password);

        UserEntry ue = g.setUserEntry(null, accountId, clearPw, givenName, familyName, suspended, quota);

        log.info("Creating User {0} givenName: {1} familyName {2} ", accountId, givenName, familyName);
        ue = g.createUser(ue);
        
        List<String> nicks = a.findStringList(GoogleAppsConnector.ATTR_NICKNAME_LIST);

        if (nicks != null) {
            for (String nickname : nicks) {
                log.info("Adding nickname {0} to account {1}", nickname, accountId);
                g.createNickname(accountId, nickname);
            }
        }

        List<String> groups = a.findStringList(GoogleAppsConnector.ATTR_GROUP_LIST);
        if( ! isEmpty(groups)) {
            for(String group: groups) {
                g.addGroupMember(group, accountId);
            }
        }

        List<String> owners = a.findStringList(GoogleAppsConnector.ATTR_OWNER_LIST);
        if( ! isEmpty(owners)) {
            for(String owner: owners) {
                g.addGroupMember(owner, accountId);
            }
        }

        return new Uid(accountId);
    }

    /**
     * Utility method to unguard the password.
     * @param password - Guarded password
     * @return plain text password
     */
    public static String getPlainPassword(GuardedString password) {
        if (password == null) {
            return null;
        }

        final StringBuffer buf = new StringBuffer();

        password.access(new GuardedString.Accessor() {

            public void access(char[] clearChars) {
                buf.append(clearChars);
            }
        });
        return buf.toString();
    }

    void delete(String id) {
        GoogleAppsClient g = gc.getConnection();
        g.deleteUser(id);
    }

    void query(String query, ResultsHandler handler, OperationOptions ops) {
        GoogleAppsClient g = gc.getConnection();
        boolean fetchNicknames = false; // by default nicknames and groups are not fetched
        boolean fetchGroups = false;
        //boolean fetchOwners = false;

        if (ops != null) {
            String attrs[] = ops.getAttributesToGet();

            if (attrs != null) {
                List<String> alist = Arrays.asList(attrs);
                log.info("Query options {0} ", alist);
                if (alist.contains(GoogleAppsConnector.ATTR_NICKNAME_LIST)) 
                    fetchNicknames = true;
                else if(alist.contains(GoogleAppsConnector.ATTR_GROUP_LIST))
                    fetchGroups = true;  
            }
        }

        if (query == null) { // return all users;
            log.info("Fetching All Users");
            Iterator i = g.getIterator();
            while (i.hasNext()) {
                UserEntry ue = (UserEntry) i.next();
                List<String> nicknames = new ArrayList<String>();
                List<String> groups = new ArrayList<String>();
                String accountId = ue.getLogin().getUserName();
                if (fetchNicknames) {
                    nicknames = g.getNicknamesAsList(accountId);
                }
                if( fetchGroups )
                    groups = g.getGroupMembershipsForUser(accountId);

                handler.handle(makeConnectorObject(ue, nicknames, groups));
            }
        } else {  // get a single user
            ConnectorObject obj = get(query, fetchNicknames, fetchGroups);
            log.info("ConnectorObj {0}", obj);
            if (obj != null) {
                handler.handle(obj);

            }
        }
    }

    /**
     * Given a google apps user entry and associated nicknames, create
     * a ConnectorObject.
     *
     * @param ue google apps UesrEntry object
     * @param nicknames list of nicknames
     * @return a connectorOject
     */
    private ConnectorObject makeConnectorObject(UserEntry ue, List<String> nicknames, List<String> groups) {

        if (ue == null) 
            return null; // must have a userentry object

        ObjectClass objectClass = ObjectClass.ACCOUNT;

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

        Uid uid = new Uid(ue.getLogin().getUserName());

        builder.setUid(uid);
        builder.setName(uid.getUidValue());

        String familyName = ue.getName().getFamilyName();
        String givenName = ue.getName().getGivenName();
        boolean suspended = ue.getLogin().getSuspended().booleanValue();

        builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_FAMILY_NAME, familyName));
        builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_GIVEN_NAME, givenName));
        builder.addAttribute(AttributeBuilder.buildEnabled(!suspended));

        if (nicknames.size() > 0) {
            builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_NICKNAME_LIST, nicknames));
        }

        if( groups.size() > 0 ) {
            builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_GROUP_LIST, groups));
        }

        Quota quota = ue.getQuota();

        if (quota != null) {
            builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_QUOTA, quota.getLimit()));
        }

        log.info("Make object uid={0} fn={1} gn={2} nicks={3} groups={4}", uid, familyName, givenName, nicknames, groups);

        return builder.build();
    }

    private final List<String> emptyList = new ArrayList<String>(1);
    /**
     * Retrive the user with the given uid
     *
     * @param id - the user id for the account
     * @param fetchNicknames  - if true we should also fetch associated nicknames. This can be expensive
     * @return The user object if it exists, null otherwise
     */
    private ConnectorObject get(String id, boolean fetchNicknames, boolean fetchGroups) {
        UserEntry ue = null;
        GoogleAppsClient g = gc.getConnection();

        log.info("Fetching google apps user {0}", id);
        ue = g.getUserEntry(id);

        log.info("UserEntry = {0}", GoogleAppsClient.userEntrytoString(ue));
        if (ue != null) {
            List<String> nicks = (fetchNicknames ? g.getNicknamesAsList(id) :emptyList );
            List<String> groups = (fetchGroups ? g.getGroupMembershipsForUser(id) : emptyList);
            return makeConnectorObject(ue, nicks, groups);
        }

        return null;
    }

    /**
     * Update the user entry - including associated nicknames (aliases)
     * @param uid
     * @param replaceAttrs
     * @param options
     * @return
     */
    Uid updateUser(Uid uid, Set<Attribute> replaceAttrs, OperationOptions options) {
        AttributesAccessor a = new AttributesAccessor(replaceAttrs);
        final String accountId = uid.getUidValue();

        final GoogleAppsClient g = gc.getConnection();
        // this an optimization in case the update only includes nicknames
        // we can skip the update on the other attributes
        boolean justDoNicknames = replaceAttrs.size() == 1 && a.findStringList(GoogleAppsConnector.ATTR_NICKNAME_LIST) != null;


        if (!justDoNicknames) {
            // fetch the complete object so we can merge in the changes
            final UserEntry userEntry = g.getUserEntry(accountId);


            if (userEntry == null) {
                throw new ConnectorException("Update failed. Could not read current state for user " + accountId);
            }
            // set the updated values
            log.info("Rerieved UserEntry {0}", GoogleAppsClient.userEntrytoString(userEntry));

            String password = getPlainPassword(a.getPassword());
            final String first = a.findString(GoogleAppsConnector.ATTR_FAMILY_NAME);
            final String given = a.findString(GoogleAppsConnector.ATTR_GIVEN_NAME);

            // note google apps uses suspend instead of enable - so we have to
            // negate the result
            final boolean suspended = !a.getEnabled(true);

            // merge in replace attributes
            UserEntry ue = g.setUserEntry(userEntry, accountId, password, given, first, suspended, null);
            // update the user. Throws an exception if it fails.
            ue = g.updateUser(accountId, ue);

        }
        // Update the nicknames
        List<String> nicknamesToUpdate = a.findStringList(GoogleAppsConnector.ATTR_NICKNAME_LIST);

        if (nicknamesToUpdate != null) {
            // we don't get a list of the deltas - so we need to
            // read the current list to compare what we need to add/delete
            // google apps does not have a "delete all" nicknames request -
            // so we don't have the option to delete all and then add in the update list

            List<String> currentNames = g.getNicknamesAsList(accountId);

            log.info("Existing nickname for account {0} are: {1}", accountId, currentNames);
            // we don't get a list of the deltas - so we need to
            // read the current list to compare what we need to add/delete
            // google apps does not have a "delete all" nicknames request -
            // so we don't have the option to delete all and then add in the update list
            //ChangeSet changes = new ChangeSet(currentNames, nicknamesToUpdate);
            ChangeSetExecutor changeSetExecutor = new ChangeSetExecutor(currentNames, nicknamesToUpdate) {

                @Override
                public void doAdd(String nickname) {
                    log.info("Adding nickname ${0} to user {1}", nickname, accountId);
                    g.createNickname(accountId, nickname);

                }

                @Override
                public void doRemove(String nickname) {
                    log.info("Removing nickname {0}", nickname);
                    g.deleteNickname(nickname);

                }
            };
        }

        return new Uid((accountId));
    }
}
