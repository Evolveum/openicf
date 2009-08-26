/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.googleapps;

import com.google.gdata.client.appsforyourdomain.AppsForYourDomainQuery;
import com.google.gdata.client.appsforyourdomain.AppsGroupsService;
import com.google.gdata.client.appsforyourdomain.NicknameService;
import com.google.gdata.client.appsforyourdomain.UserService;
import com.google.gdata.data.Link;
import com.google.gdata.data.appsforyourdomain.AppsForYourDomainException;
import com.google.gdata.data.appsforyourdomain.Login;
import com.google.gdata.data.appsforyourdomain.Name;
import com.google.gdata.data.appsforyourdomain.Nickname;
import com.google.gdata.data.appsforyourdomain.Quota;
import com.google.gdata.data.appsforyourdomain.generic.GenericEntry;
import com.google.gdata.data.appsforyourdomain.generic.GenericFeed;
import com.google.gdata.data.appsforyourdomain.provisioning.NicknameEntry;
import com.google.gdata.data.appsforyourdomain.provisioning.NicknameFeed;
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry;
import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/**
 *
 * Helper class containing the google apps methods to perform CRUD operations on
 * google apps accounts
 * 
 * 
 * Attribution: This is based on the sample application in the google apps java toolkit. 
 * 
 * @author Warren Strange
 */
public class GoogleAppsClient {

    public final static String SERVICENAME = "identityConnectors";
    protected static final String SERVICE_VERSION = "2.0";
    protected NicknameService nicknameService;
    protected UserService userService;
    protected AppsGroupsService groupService;
    private String domainUrlBase;
    private String admin;


    private Log log = Log.getLog(GoogleAppsClient.class);
    /**
     * Constructs a google apps connection object for the given domain using the
     * given admin credentials.
     *
     * @param adminLogin An admin user id WITHOYT the @doman. Example:  admin
     * @param adminPassword The admin's password
     * @param domainURL  The domain to administer - including the google https prefix 
     *      example: https://www.google.com/a/feeds/mycompany.com
     * @param domain - the domain. Example: acme.com
     */
    public GoogleAppsClient(String adminLogin, String adminPassword,
            String domainURL, String domain) throws Exception {

        this.domainUrlBase = domainURL;
        if (!domainUrlBase.endsWith("/")) {
            domainUrlBase += "/";
        }

        admin = adminLogin;
        String adminEmail = admin + "@" + domain;

        // Configure all of the different Provisioning services
        userService = new UserService(SERVICENAME + "-UserService");
        userService.setUserCredentials(adminEmail, adminPassword);

        nicknameService = new NicknameService(SERVICENAME + "-NicknameService");
        nicknameService.setUserCredentials(adminEmail, adminPassword);

        // todo: FIX ME - need to extract domain - 
        groupService = new AppsGroupsService(adminEmail, adminPassword, domain,
                SERVICENAME + "-AppsGroupService");

    }

    /**
     * Retrieves a user.
     * 
     * @param username The user you wish to retrieve.
     * @return A UserEntry object of the retrieved user. 
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    public UserEntry getUserEntry(String username) {
        try {
            URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
            return (UserEntry) userService.getEntry(retrieveUrl, UserEntry.class);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * Test the connection. 
     * 
     * todo: We need to find a less expensive call to test the connection
     * 
     */
    public void testConnection() {
        // fetch the admin account to test the connection.
        // as long as the connection is valid we should not throw an exception

        try {
            getUserEntry(admin);
        } catch (Exception e) {
            throw new ConnectorException("TestConnection failed. Wrapped exception ", e);
        }
    }

    /**
     * Retrieves all users in domain.  This method may be very slow for domains
     * with a large number of users.  Any changes to users, including creations
     * and deletions, which are made after this method is called may or may not be
     * included in the Feed which is returned.
     *
     * @return A UserFeed object of the retrieved users.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    private UserFeed getUserFeed()
            throws AppsForYourDomainException, ServiceException, IOException {

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/");
        UserFeed allUsers = new UserFeed();
        UserFeed currentPage;
        Link nextLink;

        do {
            currentPage = (UserFeed) userService.getFeed(retrieveUrl, UserFeed.class);
            allUsers.getEntries().addAll(currentPage.getEntries());
            nextLink = currentPage.getLink(Link.Rel.NEXT, Link.Type.ATOM);
            if (nextLink != null) {
                retrieveUrl = new URL(nextLink.getHref());
            }
        } while (nextLink != null);

        return allUsers;
    }

    public GoogleAppsAccountIterator getIterator() {
        return new GoogleAppsAccountIterator();
    }

    public void updateGroup(String groupId, String groupName, String description, String permissions) {
        try {
            groupService.updateGroup(groupId, groupName, description, permissions);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    void addGroupOwner(String groupId, String owner) {
        try {
            groupService.addOwnerToGroup(groupId, owner);
        } catch (Exception e) {
            ConnectorException.wrap(e);
        }
    }

    List<String> getGroupMembershipsForUser(String accountId) {
        List<String> l = new ArrayList<String>();
        try {
            GenericFeed f = groupService.retrieveGroups(accountId, true);
            List<GenericEntry> x = f.getEntries();
            for( GenericEntry item:x) {
                String group = item.getProperty(AppsGroupsService.APPS_PROP_GROUP_ID);
                l.add(  item.getProperty(group) );
            }
        }
        catch(Exception e) {
            throw ConnectorException.wrap(e);
        }
        return l;
    }

    /**
     * An iterator over google apps accounts.  This is wrapper around 
     * the google apps iterator mechanism. Google apps returns results
     * one page at at time. This iterator advances the pages as we 
     * consume entries.
     * 
     * Exceptions here are wrapped and converted to RuntimeException - this
     * is done to ease integration into the open connectors framework.
     * 
     */
    public class GoogleAppsAccountIterator implements Iterator {

        URL retrieveUrl;
        UserFeed currentPage;
        Iterator userIterator;

        public GoogleAppsAccountIterator() {

            try {
                retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/");
                currentPage = (UserFeed) userService.getFeed(retrieveUrl, UserFeed.class);
            } catch (Exception ex) {
                // rethrow as a generic IOException 
                throw new RuntimeException("Error creating google apps iterator", ex);
            }
        }

        public UserEntry next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Unexpected end of user list");
            }


            UserEntry ue = (UserEntry) userIterator.next();

            /*
             * 
             * for debug
            Name n = ue.getName();
            Login l = ue.getLogin();
            Quota q = ue.getQuota();
            
            System.out.println("Get entry id=" + l.getUserName() + " name=" + n.getFamilyName() + "," + n.getGivenName() +
            " Email=" + ue.getEmail() +
            " isSuspended=" + l.getSuspended() + " Quota=" + q.getLimit());
            
             */
            return ue;
        }

        public void close() {
            // no -op
        }

        /**
         *  Return true if there are more elements
         * 
         *  As a side effect - Advance the google apps page fetched if required. 
         * @return true if there are more user entry elements to read
         */
        public boolean hasNext() {

            if (currentPage == null) {
                return false;
            }

            if (userIterator == null) {
                userIterator = currentPage.getEntries().iterator();
            }

            if (userIterator.hasNext()) {
                return true;
            }  // no need to advance - we have more entries to return


            // iterator is empty. See if there are more pages to fetch
            Link nextLink = currentPage.getLink(Link.Rel.NEXT, Link.Type.ATOM);

            if (nextLink == null) {
                return false;
            }


            try {
                retrieveUrl = new URL(nextLink.getHref());
                currentPage = (UserFeed) userService.getFeed(retrieveUrl, UserFeed.class);
            } catch (Exception ex) {
                throw new RuntimeException("Error trying to fetch next page of results", ex);
            }

            userIterator = null;

            // todo: danger will robinson. Will this ever recurse forever? It should not...
            return hasNext();
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Retrieves all nicknames for the given username.
     *
     * @param username The user for which you want all nicknames.
     * @return A NicknameFeed object with all the nicknames for the user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    public NicknameFeed getNicknameFeed(String username) {
        try {
            URL feedUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION);
            AppsForYourDomainQuery query = new AppsForYourDomainQuery(feedUrl);
            query.setUsername(username);
            return (NicknameFeed) nicknameService.query(query, NicknameFeed.class);
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    public List<String> getNicknamesAsList(String username) {
        List<String> nnlist = new ArrayList<String>();
        try {
            NicknameFeed nicknames = getNicknameFeed(username);

            if (nicknames != null) {
                for (Iterator i = nicknames.getEntries().iterator(); i.hasNext();) {
                    NicknameEntry ne = (NicknameEntry) i.next();
                    Nickname nickname = ne.getNickname();
                    nnlist.add(nickname.getName());
                }
            }
            return nnlist;
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    /**
     * Creates a nickname for the username.
     *
     * @param username The user for which we want to create a nickname.
     * @param nickname The nickname you wish to create.
     * @return A NicknameEntry object of the newly created nickname. 
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    public void createNickname(String username, String nickname) {

        try {
            //System.out.println("Add nickname for " + username + " nick=" + nickname);
            NicknameEntry entry = new NicknameEntry();
            Nickname nicknameExtension = new Nickname();
            nicknameExtension.setName(nickname);
            entry.addExtension(nicknameExtension);

            Login login = new Login();
            login.setUserName(username);
            entry.addExtension(login);

            URL insertUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION);
            NicknameEntry ne = nicknameService.insert(insertUrl, entry);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }

    }

    /**
     * Deletes a nickname.
     *
     * @param nickname The nickname you wish to delete.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    public void deleteNickname(String nickname) {
        try {
            URL deleteUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION + "/" + nickname);
            nicknameService.delete(deleteUrl);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * Creates a new user with an email account.
     *
     * @param entry - A user entry structure that represents the user account.
     *
     * @return A UserEntry object of the newly created user.
     * service.
     */
    public UserEntry createUser(UserEntry entry) {
        try {
            URL insertUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION);
            return (UserEntry) userService.insert(insertUrl, entry);
        } catch (Exception e) {
            //log.error("Got an exception trying to create a user: {0}", e.getMessage());
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * Update a google apps account.
     *
     * 
     * 
     * @param username - user (accountId) to update
     * @param userEntry - the new values to update
     * @return the modified UserEntry
     */
    public UserEntry updateUser(String username, UserEntry userEntry) {
        try {
            URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
            return (UserEntry) userService.update(updateUrl, userEntry);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    /**
     * Deletes a user.
     *
     * @param username The user you wish to delete.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    public void deleteUser(String username) {
        try {
            URL deleteUrl = new URL(
                    domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
            userService.delete(deleteUrl);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }

    }

    /**
     * This method is used to create a new User Entry object (if userEntry == null), OR update
     * an existing one (if userEntry is non null). 
     * 
     * Non null values are set in the userEntry object
     * 
     * @param userEntry - Existing UserEntry object to be updated. If null a new entry is created
     * @param username - The login identifier
     * @param password  - Password - in the clear. 
     * @param givenName - Persons first (given) name
     * @param familyName = Persons last (family) name
     * @param suspended - set to true if the account is suspended
     * @param quotaLimitInMb - Users email quota limit in MB
     * 
     * @return a new user entry object or an updated copy of the one passed in.
     */
    public UserEntry setUserEntry(UserEntry userEntry, String username, String password,
            String givenName,
            String familyName,
            boolean suspended,
            Integer quotaLimitInMb) {

        if (userEntry == null) {
            userEntry = new UserEntry();
        }


        Login login = userEntry.getLogin();
        if (login == null) {
            login = new Login();
        }
        if (username != null) {
            login.setUserName(username);
        }

        if (password != null) {
            login.setPassword(password);
        }

        login.setSuspended(suspended);

        userEntry.setExtension(login);

        Name name = userEntry.getName();
        if (name == null) {
            name = new Name();
        }

        if (givenName != null) {
            name.setGivenName(givenName);
        }
        if (familyName != null) {
            name.setFamilyName(familyName);
        }
        userEntry.setExtension(name);

        if (quotaLimitInMb != null) {
            Quota quota = new Quota();
            quota.setLimit(quotaLimitInMb);
            userEntry.setExtension(quota);
        }

        return userEntry;
    }

    /**
     * Convenience method to dump a userentry objet to a string for debug
     * @param ue userentry
     * @return String repreneation of the user entry
     */
    public static String userEntrytoString(UserEntry ue) {
        StringBuffer sb = new StringBuffer("UserEntry:");

        if (ue != null) {
            Login login = ue.getLogin();
            if (login != null) {
                sb.append("Login=" + login.getUserName());
            }
            Name n = ue.getName();
            if (n != null) {
                sb.append(" Name=" + n.getGivenName() + " " + n.getFamilyName());
            }
            Quota q = ue.getQuota();
            if (q != null) {
                sb.append(" quota=" + q.getLimit());
            }
        }
        return sb.toString();

    }

    // Group Operations
    public Iterator getGroupIterator() {
        try {
            GenericFeed groupsFeed = groupService.retrieveAllGroups();
            Iterator<GenericEntry> groupIterator = groupsFeed.getEntries().iterator();
            return groupIterator;
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public GenericEntry getGroupEntry(String id) {
        try {
            return groupService.retrieveGroup(id);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public void deleteGroup(String id) {
        try {
            groupService.deleteGroup(id);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public void addGroupMember(String groupId, String memberId) {
        try {
            groupService.addMemberToGroup(groupId, memberId);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public void removeGroupMember(String groupId, String memberId) {

        try {
            groupService.deleteMemberFromGroup(groupId, memberId);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    public void createGroup(String id, String Name, String groupDescription, String emailPermission) {

        try {
            groupService.createGroup(id, Name, groupDescription, emailPermission);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }

    }

    public List<String> getMembersAsList(String id) {
        List<String> members = new ArrayList();
        try {
            GenericFeed groupsFeed = groupService.retrieveAllMembers(id);
            Iterator<GenericEntry> groupsEntryIterator = groupsFeed.getEntries().iterator();
            while (groupsEntryIterator.hasNext()) {
                members.add(groupsEntryIterator.next().getProperty(AppsGroupsService.APPS_PROP_GROUP_MEMBER_ID));
            }
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        return members;
    }

    public List<String> getOwnersAsList(String id) {
        List<String> owners = new ArrayList();
        try {
            GenericFeed groupsFeed = groupService.retreiveGroupOwners(id);
            Iterator<GenericEntry> groupsEntryIterator = groupsFeed.getEntries().iterator();
            while (groupsEntryIterator.hasNext()) {
                owners.add(groupsEntryIterator.next().getProperty(AppsGroupsService.APPS_PROP_GROUP_EMAIL));
            }
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
        return owners;
    }
}
