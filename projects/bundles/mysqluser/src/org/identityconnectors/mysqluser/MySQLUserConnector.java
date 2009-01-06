/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.mysqluser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;


/**
 * The MySQLUser connector works with accounts in a mysql database.
 * <p>
 * It supports create, update, search, and delete operations, authentication. 
 * </p>
 * <p>
 * This connector assumes that all account data is stored default in MySQL database. The create,update,delete actions
 * are implemented to call appropriate SQL statements on the database.
 * </p>
 * 
 * @version $Revision $
 * @since 1.0
 */
@ConnectorClass(
        displayNameKey = "MYSQL_CONNECTOR_DISPLAY",
        configurationClass = MySQLUserConfiguration.class) 
public class MySQLUserConnector implements PoolableConnector, CreateOp, SearchOp<FilterWhereBuilder>, DeleteOp, UpdateOp,
        SchemaOp, TestOp, AuthenticateOp {


    /**
     * Setup logging for the {@link MySQLUserConnector}.
     */
    private Log log = Log.getLog(MySQLUserConnector.class);
    
    /**
     * Place holder for the {@link Configuration} passed into the callback {@link MySQLUserConnector#init(Configuration)}.
     */
    private MySQLUserConfiguration config;

    /**
     * Place holder for the Connection
     */
    private MySQLUserConnection conn;
    

    /**
     * Gets the Configuration context for this connector.
     * 
     * @see org.identityconnectors.framework.spi.PoolableConnector#getConfiguration()
     */
    public Configuration getConfiguration() {
        return this.config;
    }
    
   
    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see org.identityconnectors.framework.spi.PoolableConnector#init(Configuration)
     */
    public void init(Configuration cfg) {
        this.config = (MySQLUserConfiguration) cfg;
        this.conn = MySQLUserConnection.getConnection(this.config);
    }

    
    /**
     * Method to receive check the connector is valid.
     * 
     * @see org.identityconnectors.framework.spi.PoolableConnector#checkAlive()
     */
    public void checkAlive() {
        conn.test();
    }    


    /**
     * The connector connection access method
     * @return connection
     */
    MySQLUserConnection getConnection() {
        return conn;
    }
  
    /**
     * Disposes of the {@link MySQLUserConnector}'s resources.
     * 
     * @see org.identityconnectors.framework.spi.PoolableConnector#dispose()
     */
    public void dispose() {
        if ( this.conn != null ) {
            this.conn.dispose();
            this.conn = null;
        }
        this.config = null;
    }
    
    /**
     * Create a new mysql user using model-user rights.
     * 
     * @param oclass the {@link ObjectClass} type (must be ACCOUNT )
     * @param attrs attributes. Required attributes are name and password
     * @param options additional options. Additional options are not supported in this operation.  
     * 
     * @return the value that represents the account  {@link Uid} for the new user. 
     * @throws AlreadyExistsException if the user or group already exists on the target resource
     * @throws ConnectorException if an invalid attribute is specified
     * 
     * @see org.identityconnectors.framework.spi.operations.CreateOp#create(ObjectClass, Set, OperationOptions)
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        // Get the needed attributes
        if ( oclass == null || !oclass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Name user = AttributeUtil.getNameFromAttributes(attrs);
        if (user == null || StringUtil.isBlank(user.getNameValue())) {
            throw new IllegalArgumentException("The Name attribute cannot be null or empty.");
        }
        // Password is Operational
        GuardedString password = AttributeUtil.getPasswordValue(attrs);
        if (password == null) {
            throw new IllegalArgumentException("The Password attribute cannot be null.");
        }
        // Create the user
        log.info("Creating user: {0}", user.getNameValue());
        createUser(user.getNameValue(), password);
        // Don't forget to create Uid
        Uid uid = newUid(user.getNameValue());

        // Model name is needed to set rights for the new user
        String modelUserName = config.getUsermodel();
        log.info("Reading the modeluser: {0}", modelUserName);
        List<String> grants = readGrantsForModelUser(modelUserName);

        // Replace modelUser to newUser for the GRANT statement
        List<String> newGrants = replaceModelUserInGrants(user.getNameValue(), modelUserName, grants);

        // Granting rights for the new user
        log.info("Granting rights for user: {0}", user.getNameValue());
        grantingRights(newGrants, user.getNameValue());

        // commit all
        conn.commit();
        log.ok("Created user: {0}", user.getNameValue());

        return uid;
    }
    
    /**
     * Deletes a mysql user using drop statement
     * @param objClass the type of object to delete. Only ACCOUNT is supported.
     * @param uid the {@link Uid} of the user to delete
     * @param options additional options. Additional options are not supported in this operation. 
     * 
     * @throws UnknownUidException if the specified Uid does not exist on the target resource
     * @throws ConnectorException if a problem occurs with the connection
     * 
     * @see DeleteOp#delete(ObjectClass, Uid, OperationOptions)
     */
    public void delete(final ObjectClass objClass, final Uid uid, final OperationOptions options) {
        if (objClass == null || (!objClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Delete operation received a wrong ObjectClass.");
        }

        if (uid == null || (uid.getUidValue() == null)) {
            throw new IllegalArgumentException("Delete operation received a null ObjectClass.");
        }
        
        log.info("Delete user uid: {0}", uid.getName());
        //Delete the user
        deleteUser(uid);
        //Commit delete
        conn.commit();
        log.ok("Deleted user uid: {0}", uid.getName());
    }
    
    /**
     * Update the database row w/ the data provided.
     * @param objClass the {@link ObjectClass} type (must be ACCOUNT )
     * @param attrs attributes. Required attributes are name and password
     * @param options additional options. Additional options are not supported in this operation.  
     * 
     * @see UpdateOp#update(ConnectorObject, Set, OperationOptions )
     */
    public Uid update(final ObjectClass objClass, Uid oldUid, final Set<Attribute> attrs, final OperationOptions options) {
        final String SQL_UPDATE = "UPDATE mysql.user SET {0} WHERE user=?";
        final String SQL_SET_USER = "user = ?";
        final String SQL_SET_PASSWORD = "password = password(?)";

        if (objClass == null || (!ObjectClass.ACCOUNT.equals(objClass))) {
            throw new IllegalArgumentException("Invalid objectclass '" + objClass + "'");
        }
        
        if(attrs == null || attrs.size() == 0) {
            throw new IllegalArgumentException("Invalid attributes provided to a update operation.");
        }                

        // init the return value for old Uid
        Uid ret = oldUid;
        String updateSet = "";
        // Bind values
        final List<Object> values = new ArrayList<Object>();
        //The update is changing name. The oldUid is a key and the name will be new uid for mysql.
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        // is there a change in name?
        if(name != null && oldUid.getUidValue() != name.getNameValue()) {
            log.info("Update user {0} to (1)", oldUid.getUidValue(), name.getNameValue());
            updateSet = SQL_SET_USER;
            values.add(name.getNameValue());
            // update the return value to new Uid
            ret = newUid(name.getNameValue());
        }
        
        //Password change
        GuardedString password = AttributeUtil.getPasswordValue(attrs);
        if(password != null) {
            if(updateSet.length() != 0) {
                updateSet = updateSet + ", ";
            }
            updateSet = updateSet + SQL_SET_PASSWORD;
            values.add(password);
        }

        // Finalize update
        String sql = MessageFormat.format(SQL_UPDATE, updateSet);
        values.add(oldUid.getUidValue());
        
        // Update the user, insert bind values into the statement
        updateUser(sql, values);
        
        // Commit changes
        conn.commit();
        log.ok("User name: {0} updated", name);
        return ret;
    }
    


    /**
     * Creates a MySQL filter translator.
     * 
     * @see FilterTranslator#createFilterTranslator(ObjectClass, OperationOptions )
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new MySQLUserFilterTranslator(oclass, options);
    }

    /**
     * Runs a query generated by the MySQLUserFilterTranslator.
     * This will be called once for each native query produced
     * by the FilterTranslator. 
     * @param oclass The object class for the search. Will never be null.
     * @param where The native query to run. A value of null means 'return everything for the given object class'.
     * @param handler Results should be returned to this handler
     * @param options this can be null.
     * 
     * @see SearchOp#executeQuery(ObjectClass, Object, ResultsHandler, OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {
        /**
         * The query to get the user columns
         * The Password <> '' mean we want to avoid reading duplicates
         * Every base user has password set up
         */
        final String ALL_USER_QUERY = "SELECT User FROM mysql.user WHERE Password <> ''";

        if (oclass == null || !ObjectClass.ACCOUNT.equals(oclass)) {
            throw new IllegalArgumentException("Unsupported objectclass '" + oclass + "'");
        }
        // Database query builder will create SQL query.
        // if where == null then all users are returned
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(ALL_USER_QUERY);
        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                
                //To be sure that uid and name are present for mysql
                final String userName = result.getObject(MySQLUserConfiguration.MYSQL_USER).toString();                
                bld.setUid(newUid(userName));
                bld.setName(userName);
                //No other attributes are now supported.
                //Password can be encoded and it is not provided as an attribute
                
                // only deals w/ accounts..
                bld.setObjectClass(ObjectClass.ACCOUNT);
                
                // create the connector object..
                ConnectorObject ret = bld.build();
                if (!handler.handle(ret)) {
                    break;
                }
            }
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
    }


    /**
     * {@inheritDoc}
     * 
     * @see SchemaOp#schema()
     */
    public Schema schema() {
        //The Name is supported attribute
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(Name.INFO);
        //Password is operationalAttribute 
        attrInfoSet.add(OperationalAttributeInfos.PASSWORD);

        // Use SchemaBuilder to build the schema. Currently, only ACCOUNT type is supported.
        SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        return schemaBld.build();
    } 


    /**
     * Test the configuration and connection
     * @see org.identityconnectors.framework.spi.operations.TestOp#test()
     */
    public void test() {
        config.validate();
        conn.test();        
    }
    

    /** 
     * Attempts to authenticate the given user/password combination.
     * 
     * @param username the username of the user
     * @param password the user's password
     * @throws InvalidCredentialException if the user is not authenticated
     *  
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(java.lang.String, java.lang.String, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        final String AUTH_SELECT="SELECT user FROM mysql.user WHERE user = ? AND password = password(?)";
        
        log.info("authenticate user: {0}", username);

        // Get the needed attributes
        if ( objectClass == null || !objectClass.equals(ObjectClass.ACCOUNT)) {
            throw new IllegalArgumentException(
                    "Create operation requires an 'ObjectClass' attribute of type 'Account'.");
        }
        Assertions.blankCheck(username, "username");
        Assertions.nullCheck(password, "password");
        
        List<Object> values = new ArrayList<Object>();
        values.add(username);
        values.add(password);
        
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = conn.prepareStatement(AUTH_SELECT, values);
            result = stmt.executeQuery();
            //No PasswordExpired capability
            if (!result.next()) {
                throw new InvalidCredentialException("user: "+username+" authentication failed");
            }            
            final Uid uid = new Uid( result.getString(1));
            log.info("user: {0} authenticated ", username);
            return uid;
        } catch (SQLException e) {
            log.error(e, "user: {0} authentication failed ", username);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(stmt);            
        }
    }
    
    /**
     * creates a new user, also sets the user's password
     * @param name
     * @param password
     */
    private void createUser(String name, GuardedString password) {
        final String SQL_CREATE_TEMPLATE = "CREATE USER ? IDENTIFIED BY ?";
        PreparedStatement c1 = null;
        try {
            // Create the user
            List<Object> values = new ArrayList<Object>();
            values.add(name);
            values.add(password);
            c1 = conn.prepareStatement(SQL_CREATE_TEMPLATE, values);
            c1.execute();
        } catch (SQLException e) {
            log.error(e, "Create user {0} error", name);
            SQLUtil.rollbackQuietly(conn);
            throw new AlreadyExistsException(e);
        } finally {
            SQLUtil.closeQuietly(c1);
        }
    }

    /**
     * Read the rights for the user model of mysql
     * @param modelUser name
     * @return list of the rights
     */
    private List<String> readGrantsForModelUser(String modelUser) {
        final String SQL_SHOW_GRANTS = "SHOW GRANTS FOR ?";
        PreparedStatement c2 = null;
        List<String> grants = new ArrayList<String>();
        try {
            // created, read the model user grants
            c2 = conn.getConnection().prepareStatement(SQL_SHOW_GRANTS);
            c2.setString(1, modelUser);
            ResultSet grantRs = c2.executeQuery();
            while (grantRs.next()) {
                String grant = grantRs.getString(1);
                grants.add(grant);
            }
        } catch (SQLException e) {
            log.error(e, "Error read GRANTS for model user {0}", modelUser);
            //No error when the modelUser does not exist
            //SQLUtil.rollbackQuietly(getConnection());
            //throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(c2);
        }
        return grants;
    }


    /**
     * Replace Model User in GRANT SQL statements
     * @param userName the new user
     * @param modelUser is defined in mysql
     * @param grants the list of rights to set
     * @return the updated GRANT statement
     */
    private List<String> replaceModelUserInGrants(String userName, String modelUser, List<String> grants) {
        List<String> newGrants = new ArrayList<String>();
        for (String grant : grants) {
            String newGrant = grant.replaceAll("'" + modelUser + "'", "'" + userName + "'");
            // Remove password key is if present
            newGrant = newGrant.replaceAll("IDENTIFIED.*", "");
            newGrants.add(newGrant);
        }
        return newGrants;
    }    
    
    /**
     * Execute the GRANT statement for the given userName and rights
     * @param grants rights for the new user
     * @param userName ID of the new user
     */
    private void grantingRights(List<String> grants, String userName) {
        for (String grant : grants) {
            PreparedStatement c3 = null;
            try {
                c3 = conn.getConnection().prepareStatement(grant);
                log.info("Granting rights {0} for user: {1}", userName, grant);
                c3.execute();
            } catch (SQLException e) {
                log.error(e, "Error granting rights {0} for user: {1}", userName, grant);
                SQLUtil.rollbackQuietly(conn);
                throw ConnectorException.wrap(e);
            } finally {
                SQLUtil.closeQuietly(c3);
            }
        }
    }

    /**
     * Delete The User
     * 
     * @param uid
     *            the uid of the user
     * @param connection
     */
    private void deleteUser(final Uid uid) {
        final String SQL_DELETE_TEMPLATE = "DROP USER ?";

        PreparedStatement stmt = null;
        try {
            // create a prepared call..
            stmt = conn.getConnection().prepareStatement(SQL_DELETE_TEMPLATE);
            // set object to delete..
            stmt.setString(1, uid.getUidValue());
            // uid to delete..
            log.info("Deleting Uid: {0}", uid.getUidValue());
            stmt.execute();
            log.ok("Deleted Uid: {0}", uid.getUidValue());
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(conn);
            log.error(e, "SQL: " + SQL_DELETE_TEMPLATE);
            throw new UnknownUidException(e);
        } finally {
            // clean up..
            SQLUtil.closeQuietly(stmt);
        }
    }


    /**
     * Update the user identified by uid using the update string and list of bind values
     * 
     * @param updstr
     *            the SQL update string
     * @param values
     *            the object values to be bind
     */
    private void updateUser(String updstr, List<Object> values) {
        // create the sql statement..
        final String FLUSH_PRIVILEGES = "FLUSH PRIVILEGES";
        PreparedStatement stmt = null;
        try {
            // create the prepared statement..
            stmt = conn.getConnection().prepareStatement(updstr);
            SQLUtil.setParams(stmt, values);            
            stmt.execute();
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(conn);
            log.error(e, "SQL: " + updstr);
            throw ConnectorException.wrap(e);
        } finally {
            // clean up..
            SQLUtil.closeQuietly(stmt);
        }
        // Flush privileges
        PreparedStatement cstmt = null;
        try {
            // create the prepared statement..
            cstmt = conn.getConnection().prepareStatement(FLUSH_PRIVILEGES);
            cstmt.execute();
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(conn);
            log.error(e, "SQL: " + FLUSH_PRIVILEGES);
            throw ConnectorException.wrap(e);
        } finally {
            // clean up..
            SQLUtil.closeQuietly(cstmt);
        }
    }    

    /**
     * Create new Uid
     * @param userName
     * @return
     */
    private Uid newUid(String userName) {
        return new Uid(userName);
    }    
}
