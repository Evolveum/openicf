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

import com.google.gdata.data.appsforyourdomain.AppsForYourDomainException;
import com.google.gdata.data.appsforyourdomain.Quota;
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry;
import com.google.gdata.util.ServiceException;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;


import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * <p> A connector for Google Apps for your domain.
 * This connector can manage accounts on Google Apps. It also 
 * manages nicknames (email aliases) associated with users.
 * <p>
 *
 * Notes
 *
 * According to this thread: <br/>
 * http://groups.google.com/group/google-apps-apis/browse_thread/thread/d68b2458b4e84777
 * <br/>
 * The API does not support a rename operation on an account. You have to delete
 * and receate the account.
 * <br/>
 * Quota can be read - but it appears to ignore it on a create. This is the
 * same behavior as creating through the Google Admin Web GUI (it doesnt
 * even let you set it). I believe this is due to the type of google apps
 * account I have used for testing. The default is to allow the user to
 * have the maxium quota - which is what most organizations will want anyways.
 *
 * 
 * @author Warren Strange
 * @version $Revision $
 * @since 1.0
 */
@ConnectorClass(configurationClass = GoogleAppsConfiguration.class, displayNameKey = "googleapps.connector.display")
public class GoogleAppsConnector implements
        PoolableConnector, CreateOp, SearchOp<String>, DeleteOp, SchemaOp, UpdateOp, TestOp {

    public static final String ATTR_FAMILY_NAME = "familyName";
    public static final String ATTR_GIVEN_NAME = "givenName";
    public static final String ATTR_QUOTA = "quota";
    public static final String ATTR_NICKNAME_LIST = "nicknames";
    /**
     * Setup logging for the {@link GoogleAppsConnector}.
     */
    Log log = Log.getLog(GoogleAppsConnector.class);
    /**
     * Place holder for the {@link Connection} passed into the callback
     * {@link ConnectionFactory#setConnection(Connection)}.
     */
    private GoogleAppsConnection gc;
    /**
     * Place holder for the {@link Configuration} passed into the callback
     * {@link GoogleAppsConnector#init(Configuration)}.
     */
    private GoogleAppsConfiguration config;

    // =======================================================================
    // Initialize/dispose methods..
    // =======================================================================
    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see org.identityconnectors.framework.spi.Connector#init(Configuration)
     */
    public void init(Configuration cfg) {
        this.config = (GoogleAppsConfiguration) cfg;
        this.gc = newConnection();
    }

    /**
     * Disposes of the {@link GoogleAppsConnector}'s resources.
     *
     * Nothing really to do here as the google apps adapter does not
     * consume client resources.
     * 
     * @see Connector#dispose()
     */
    public void dispose() {
    }

    /**
     * 
     * {@inheritDoc}
     * @see SchemaOp#schema()
     */
    public Schema schema() {
        if (config == null) {
            throw new IllegalStateException("Configuration object has not been set.");
        }
        SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();

        // Required Attributes
        //
        AttributeInfoBuilder nb = new AttributeInfoBuilder();
        nb.setCreateable(true);
        nb.setUpdateable(false);
        nb.setName(Name.NAME);

        attributes.add(nb.build());
        // regular attributes
        attributes.add(AttributeInfoBuilder.build(ATTR_FAMILY_NAME));
        attributes.add(AttributeInfoBuilder.build(ATTR_GIVEN_NAME));

        // quota can be set on create, but can not be updated after account creation
        AttributeInfoBuilder q = new AttributeInfoBuilder();
        q.setCreateable(true);
        q.setName(ATTR_QUOTA);
        q.setType(Integer.class);
        q.setUpdateable(false);
        q.setReadable(true);

        attributes.add(q.build());
        // Multi-valued attributes
        attributes.add(buildMultivaluedAttribute(ATTR_NICKNAME_LIST, String.class, false, false));

        // Operational Attributes - password and enable/disable status
        //
        attributes.add(OperationalAttributeInfos.PASSWORD);
        attributes.add(OperationalAttributeInfos.ENABLE);

        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);

        return schemaBuilder.build();

    }
    // =======================================================================
    // Connection Pooling..
    // =======================================================================

    /**
     * Creates a new {@link Connection} based on the {@link Configuration}
     * passed into {@link GoogleAppsConnector#init(Configuration)}.
     * 
     * @see ConnectionFactory#newConnection()
     */
    public GoogleAppsConnection newConnection() {
        return new GoogleAppsConnection(config);
    }

    /**
     * Call-back to receive a tested {@link Connection} from the pool.
     * 
     * @see ConnectionFactory#setConnection(Connection)
     */
    public void setConnection(GoogleAppsConnection conn) {
        this.gc = conn;
    }

    /**
     * Creates a a google apps user 
     * 
     * @param attrs
     *            Various attributes the correlate to a google apps user
     * @return the value that represents the google aps account id for the user. 
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions arg2) {
        Name name = AttributeUtil.getNameFromAttributes(attrs);


        log.info("Create User {0}", name);
        AttributesAccessor a = new AttributesAccessor(attrs);


        if (name != null) {

            final String accountId = a.getName().getNameValue();

            final GoogleAppsClient g = gc.getConnection();

            log.info("Extracting Attrs {0}", attrs);

            // todo: this craters if the attribute does not exist (NPE)
            // we should throw a better error
            final String givenName = a.findString(ATTR_GIVEN_NAME);
            final String familyName = a.findString(ATTR_FAMILY_NAME);
            final Integer quota = a.findInteger(ATTR_QUOTA);
            // Password is Operational

            GuardedString password = a.getPassword();


            if (password == null) {
                throw new IllegalArgumentException("The Password attribute cannot be null.");
            }

            final boolean suspended = !a.getEnabled(true);


            String clearPw = null;

            password.access(new GuardedString.Accessor() {

                public void access(char[] clearChars) {
                    UserEntry ue = g.setUserEntry(null, accountId, new String(clearChars), givenName, familyName, suspended, quota);
                    try {
                        ue = g.createUser(ue);
                    } catch (Exception e) {
                        log.error(e, "Google Apps Create Error");
                        throw new RuntimeException(e);
                    }
                }
            });
            List<String> nicks = a.findStringList(ATTR_NICKNAME_LIST);



            if (nicks != null) {
                for (String nickname : nicks) {
                    log.info("Adding nickname {0} to account {1}", nickname, accountId);
                    try {
                        g.createNickname(accountId, nickname);
                    } catch (Exception e) {
                        log.error(e, "Google Apps Error while trying to update nicknames");
                        throw new RuntimeException(e);
                    }
                }
            }
            return new Uid(accountId);

        }
        return null;
    }

    /**
     * Delete a google apps account and all associated nicknames.
     *
     * Note that once an account is deleted, the account id can not be reused
     * for 5 days (according to google).
     * 
     * @see DeleteOp#delete(Uid)
     */
    public void delete(final ObjectClass objClass, final Uid uid, OperationOptions options) {
        try {
            String id = uid.getUidValue();
            log.info("Deleting User {0}", id);
            GoogleAppsClient g = gc.getConnection();
            g.deleteUser(id);
        } catch (Exception e) {
            log.error(e, "Delete");
            throw ConnectorException.wrap(e);
        }
    }

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Utility to build a multi valued attribute.
     * 
     * @param name
     * @param clazz
     * @param required
     * @return
     */
    private AttributeInfo buildMultivaluedAttribute(String name, Class<?> clazz, boolean required, boolean returnByDefault) {
        AttributeInfoBuilder builder = new AttributeInfoBuilder();
        builder.setName(name);
        builder.setType(clazz);
        builder.setRequired(required);
        builder.setMultiValued(true);
        builder.setReturnedByDefault(returnByDefault);
        return builder.build();
    }

    public FilterTranslator<String> createFilterTranslator(ObjectClass arg0, OperationOptions arg1) {
        return new GoogleAppsFilterTranslator();
    }

    /**
     * Execute the search operation.
     *
     * This adapter only supports searching by account name
     *
     *
     *
     * @param oclass Object class to search for. We only support ACCOUNT
     * @param query - Query string. Must be the account name or NULL for all accounts
     * @param handler - Results handler
     * @param ops - search options. By default, the nicknames for a user are not returned
     */
    public void executeQuery(ObjectClass oclass, String query, ResultsHandler handler, OperationOptions ops) {
        if (!ObjectClass.ACCOUNT.equals(oclass)) {
            throw new IllegalArgumentException("Unsupported objectclass '" + oclass + "'");
        }

        GoogleAppsClient g = gc.getConnection();

        log.info("query string = {0}", query);

        boolean fetchNicknames = false;

        if (ops != null) {
            String attrs[] = ops.getAttributesToGet();

            if (attrs != null) {
                List<String> alist = Arrays.asList(attrs);
                log.info("Query options {0} ", alist);
                if (alist.contains(ATTR_NICKNAME_LIST)) {
                    fetchNicknames = true;
                }
            }
        }

        if (query == null) { // return all users;
            log.info("Fetching All Users");
            Iterator i = g.getIterator();
            while (i.hasNext()) {
                try {
                    UserEntry ue = (UserEntry) i.next();
                    List<String> nicknames = new ArrayList<String>();
                    if (fetchNicknames) {
                        nicknames = g.getNicknamesAsList(ue.getLogin().getUserName());
                    }
                    handler.handle(makeConnectorObject(ue, nicknames));
                } catch (AppsForYourDomainException ex) {
                    ConnectorException.wrap(ex);
                } catch (ServiceException ex) {
                    ConnectorException.wrap(ex);
                } catch (IOException ex) {
                    ConnectorIOException.wrap(ex);
                }
            }
        } else {  // get a single user
            ConnectorObject obj = get(query, fetchNicknames);
            log.info("ConnectorObj {0}", obj);
            if (obj != null) {
                handler.handle(obj);

            }
        }
        log.info("Exit");
    }

    /**
     * Given a google apps user entry and associated nicknames, create
     * a ConnectorObject.
     *
     * @param ue google apps UesrEntry object
     * @param nicknames list of nicknames
     * @return a connectorOject
     */
    private ConnectorObject makeConnectorObject(UserEntry ue, List<String> nicknames) {

        if (ue == null) {
            return null;
        }
        if (nicknames == null) // should not be the case, but...
        {
            nicknames = new ArrayList<String>();
        }

        ObjectClass objectClass = ObjectClass.ACCOUNT;

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

        Uid uid = new Uid(ue.getLogin().getUserName());

        builder.setUid(uid);
        builder.setName(uid.getUidValue());

        String familyName = ue.getName().getFamilyName();
        String givenName = ue.getName().getGivenName();
        boolean suspended = ue.getLogin().getSuspended().booleanValue();

        builder.addAttribute(AttributeBuilder.build(ATTR_FAMILY_NAME, familyName));
        builder.addAttribute(AttributeBuilder.build(ATTR_GIVEN_NAME, givenName));
        builder.addAttribute(AttributeBuilder.buildEnabled(!suspended));

        if (nicknames.size() > 0) {
            builder.addAttribute(AttributeBuilder.build(ATTR_NICKNAME_LIST, nicknames));
        }

        Quota quota = ue.getQuota();

        if (quota != null) {
            builder.addAttribute(AttributeBuilder.build(ATTR_QUOTA, quota.getLimit()));
        }

        log.info("Make object uid={0} fn={1} gn={2} nicks={3}", uid, familyName, givenName, nicknames);

        return builder.build();

    }

    /**
     * Retrive the user with the given uid
     *
     * @param id - the user id for the account
     * @param fetchNicknames  - if true we should also fetch associated nicknames. This can be expensive
     * @return The user object if it exists, null otherwise
     */
    private ConnectorObject get(String id, boolean fetchNicknames) {
        UserEntry ue = null;

        GoogleAppsClient g = gc.getConnection();

        try {

            log.info("Fetching google apps user {0}", id);
            ue = g.getUserEntry(id);

            log.info("UserEntry = {0}", GoogleAppsClient.userEntrytoString(ue));
            if (ue != null) {
                List<String> nicks = (fetchNicknames ? g.getNicknamesAsList(id) : null);

                return makeConnectorObject(ue, nicks);
            }
        } catch (Exception e) {
            ConnectorException.wrap(e);
        }
        return null;
    }

    /**
     * Update a googleapps user account
     *
     * Note that we do not support account rename operations
     * Only the name (given,family), nicknames,
     * and the password can be updated
     * Quota can not be changed after a create
     *
     *
     * 
     * Nickname list updates are rather inefficent - but the number
     * of nickname for a user should be small.
     *
     * 
     * @param objclass - Object class - we only support ACCOUNT
     * @param replaceAttrs - attribute set to replace in update set
     * @param options - update options
     * @return the Uid of the updated object
     */
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttrs, OperationOptions options) {

        log.info("Update {0}", replaceAttrs);

        if (objclass == null || !ObjectClass.ACCOUNT.equals(objclass)) {
            throw new IllegalArgumentException("Unsupported objectclass '" + objclass + "'");
        }

        if (replaceAttrs == null || replaceAttrs.size() == 0) {
            throw new IllegalArgumentException("Invalid attributes provided to a update operation.");
        }

        AttributesAccessor a = new AttributesAccessor(replaceAttrs);

        if (uid == null) {
            throw new ConnectorException("Uid attribute is missing!");
        }

        final String accountId = uid.getUidValue();

        final GoogleAppsClient g = gc.getConnection();


        // this an optimization in case the update only includes nicknames
        // we can skip the update on the other attributes
        boolean justDoNicknames = replaceAttrs.size() == 1 && a.findStringList(ATTR_NICKNAME_LIST) != null;


        try {
         
            if (! justDoNicknames) {
                 // fetch the complete object so we can merge in the changes
                final UserEntry userEntry = g.getUserEntry(accountId);


                if (userEntry == null) {
                    throw new ConnectorException("Update failed. Could not read current state for user " + accountId);
                }
                // set the updated values
                log.info("Rerieved UserEntry {0}", GoogleAppsClient.userEntrytoString(userEntry));


                String password = getPlainPassword(a.getPassword());
                final String first = a.findString(ATTR_FAMILY_NAME);
                final String given = a.findString(ATTR_GIVEN_NAME);

                // note google apps uses suspend instead of enable - so we have to
                // negate the result
                final boolean suspended = !a.getEnabled(true);

                // merge in replace attributes
                UserEntry ue = g.setUserEntry(userEntry, accountId, password, given, first, suspended, null);
                // update the user. Throws an exception if it fails.
                ue = g.updateUser(accountId, ue);

            }

            // Update the nicknames
            List<String> nl = a.findStringList(ATTR_NICKNAME_LIST);

            if (nl != null) {
                // we don't get a list of the deltas - so we need to
                // read the current list to compare what we need to add/delete
                // google apps does not have a "delete all" nicknames request -
                // so we don't have the option to delete all and then add in the update list

                List<String> currentNames = g.getNicknamesAsList(accountId);

                log.info("Existing nickname for account {0} are: {1}", accountId, currentNames);

                // toadd is a copy of the nicknames to update
                List<String> toAdd = new ArrayList<String>(nl);

                // remove all the nicknames from toAdd that already exist 
                //  (no need to add them again);

                toAdd.removeAll(currentNames);
                // toAdd now contains all the net new names we need to add
                // add the new names
                for (String n : toAdd) {
                    // todo: May want to validate the nickname before trying to add it
                    log.info("Adding nickname ${0} to user {1}", n, accountId);
                    g.createNickname(accountId, n);
                }

                // If a name currently exists, and is NOT 
                // on the update list (list of to be names)
                // then we need to remove it 
                // subtract the to-be list from the current names
                currentNames.removeAll(nl);
                for (String n : currentNames) {
                    log.info("Removing nickname {0}", n);
                    g.deleteNickname(n);
                }
            }
        } catch (AppsForYourDomainException ex) {
            ConnectorException.wrap(ex);
        } catch (ServiceException ex) {
            ConnectorException.wrap(ex);
        } catch (IOException ex) {
            ConnectorIOException.wrap(ex);
        }

        return new Uid((accountId));
    }

    public void test() {
        log.info("test connection");
        GoogleAppsClient g = gc.getConnection();
        g.testConnection();
    }

    /**
     * Check the google apps connection is alive
     */
    public void checkAlive() {
        log.info("checkAlive");
        test();
    }

    private String getPlainPassword(GuardedString password) {
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
}
