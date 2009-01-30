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
import com.google.gdata.client.appsforyourdomain.NicknameService;
import com.google.gdata.client.appsforyourdomain.UserService;
import com.google.gdata.data.Link;
import com.google.gdata.data.appsforyourdomain.AppsForYourDomainException;
import com.google.gdata.data.appsforyourdomain.Login;
import com.google.gdata.data.appsforyourdomain.Name;
import com.google.gdata.data.appsforyourdomain.Nickname;
import com.google.gdata.data.appsforyourdomain.Quota;
import com.google.gdata.data.appsforyourdomain.provisioning.NicknameEntry;
import com.google.gdata.data.appsforyourdomain.provisioning.NicknameFeed;
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry;
import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

    protected static final String SERVICE_VERSION = "2.0";
  
    protected NicknameService nicknameService;
    protected UserService userService;
    private String domainUrlBase;
    private String  admin;

    /**
     * Constructs a google apps connection object for the given domain using the
     * given admin credentials.
     *
     * @param adminEmail An admin user's email address such as admin@domain.com
     * @param adminPassword The admin's password
     * @param domainURL  The domain to administer - including the google https prefix 
     *      example: https://www.google.com/a/feeds/mycompany.com
     */
    public GoogleAppsClient(String adminEmail, String adminPassword,
            String domainURL) throws Exception {

        this.domainUrlBase = domainURL;
        if (!domainUrlBase.endsWith("/")) {
            domainUrlBase += "/";
        }

        admin = adminEmail.substring(0, adminEmail.indexOf('@'));
        
        // Configure all of the different Provisioning services
        userService = new UserService(
                "gdata-sample-AppsForYourDomain-UserService");
        userService.setUserCredentials(adminEmail, adminPassword);

        nicknameService = new NicknameService(
                "gdata-sample-AppsForYourDomain-NicknameService");
        nicknameService.setUserCredentials(adminEmail, adminPassword);
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
    public UserEntry getUserEntry(String username)
            throws AppsForYourDomainException, ServiceException, IOException {

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return (UserEntry) userService.getEntry(retrieveUrl, UserEntry.class);
    }

    /**
     * Test the connection. 
     * 
     * todo: We need to find a less expensive call to test the connection
     * 
     */
    public void testConnection()   {

        // fetch the admin account to test the connection.
        // as long as the connection is valid we should not throw an exception
   

        try {
            URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + admin);
            UserEntry ue =  userService.getEntry(retrieveUrl, UserEntry.class);
        }
        catch(Exception e) {
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
    public NicknameFeed getNicknameFeed(String username)
            throws AppsForYourDomainException, ServiceException, IOException {

        URL feedUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION);
        AppsForYourDomainQuery query = new AppsForYourDomainQuery(feedUrl);
        query.setUsername(username);
        return (NicknameFeed) nicknameService.query(query, NicknameFeed.class);
    }

    public List<String> getNicknamesAsList(String username) throws AppsForYourDomainException, ServiceException, IOException {

        NicknameFeed nicknames = getNicknameFeed(username);

        List<String> nnlist = new ArrayList<String>();
        if (nicknames != null) {
            for (Iterator i = nicknames.getEntries().iterator(); i.hasNext();) {
                NicknameEntry ne = (NicknameEntry) i.next();
                Nickname nickname = ne.getNickname();
                nnlist.add(nickname.getName());

            }
        }
        return nnlist;
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
    public NicknameEntry createNickname(String username, String nickname)
            throws AppsForYourDomainException, ServiceException, IOException {


        //System.out.println("Add nickname for " + username + " nick=" + nickname);
        NicknameEntry entry = new NicknameEntry();
        Nickname nicknameExtension = new Nickname();
        nicknameExtension.setName(nickname);
        entry.addExtension(nicknameExtension);

        Login login = new Login();
        login.setUserName(username);
        entry.addExtension(login);


        URL insertUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION);
        return (NicknameEntry) nicknameService.insert(insertUrl, entry);
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
    public void deleteNickname(String nickname)
            throws AppsForYourDomainException, ServiceException, IOException {

        URL deleteUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION + "/" + nickname);
        nicknameService.delete(deleteUrl);
    }

    /**
     * Creates a new user with an email account.
     *
     * @param entry - A user entry structure that represents the user account.
     *
     * @return A UserEntry object of the newly created user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData
     * service.
     */
    public UserEntry createUser(UserEntry entry)
            throws AppsForYourDomainException, ServiceException, IOException {

        URL insertUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION);
        return (UserEntry) userService.insert(insertUrl, entry);
    }

    /**
     * Update a google apps account.
     *
     * 
     * 
     * @param username - user (accountId) to update
     * @param userEntry - the new values to update
     * @return the modified UserEntry
     * @throws com.google.gdata.data.appsforyourdomain.AppsForYourDomainException
     * @throws com.google.gdata.util.ServiceException
     * @throws java.io.IOException
     */
    public UserEntry updateUser(String username, UserEntry userEntry)
            throws AppsForYourDomainException, ServiceException, IOException {

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return (UserEntry) userService.update(updateUrl, userEntry);
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
    public void deleteUser(String username)
            throws AppsForYourDomainException, ServiceException, IOException {

        URL deleteUrl = new URL(
                domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        userService.delete(deleteUrl);

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
        
        if( ue != null ) {
            Login login = ue.getLogin();
            if( login != null )
                sb.append( "Login=" + login.getUserName());
            Name n = ue.getName();
            if( n != null )
                sb.append(" Name=" + n.getGivenName() + " " + n.getFamilyName());
            Quota q = ue.getQuota();
            if( q != null )
                sb.append(" quota=" + q.getLimit());
        }
        return sb.toString();
        
    }
}
