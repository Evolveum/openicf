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
package org.identityconnectors.mysqluser;

import static org.identityconnectors.mysqluser.MySQLUserConstants.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
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
import org.identityconnectors.framework.common.objects.OperationalAttributes;
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
    private static final Log log = Log.getLog(MySQLUserConnector.class);
    
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
        if ( StringUtil.isNotBlank(config.getDatasource())) {
            try {
                conn.openConnection();
            } catch (SQLException e) {
                log.error(e, "error in checkAlive");
                throw ConnectorException.wrap(e);
            }
        } else {
            conn.test();
            conn.commit();
        }
        // will not close connection, it is expected to be used in following api op
        log.ok("checkAlive");
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
        log.ok("dispose");
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

        checkAttributes(oclass, attrs);  

        Name user = AttributeUtil.getNameFromAttributes(attrs);
        if (user == null || StringUtil.isBlank(user.getNameValue())) {
            throw new IllegalArgumentException(config.getMessage(MSG_NAME_BLANK));
        }
        // Password is Operational
        GuardedString password = AttributeUtil.getPasswordValue(attrs);
        if (password == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_PWD_BLANK));
        }
        try {
            // Create the user
            log.info("Creating user: {0}", user.getNameValue());
            conn.openConnection();
            createUser(user.getNameValue(), password);
            // Don't forget to create Uid
            final Uid uid = newUid(user.getNameValue());

            // Model name is needed to set rights for the new user
            String modelUserName = config.getUsermodel();
            log.info("Reading the modeluser: {0}", modelUserName);
            List<String> grants = readGrantsForModelUser(modelUserName);

            // Replace modelUser to newUser for the GRANT statement
            List<String> newGrants = replaceModelUserInGrants(user.getNameValue(), modelUserName, grants);

            // Granting rights for the new user
            log.info("Granting rights for user: {0}", user.getNameValue());
            grantingRights(newGrants, user.getNameValue(), password);

            // commit all
            conn.commit();
            log.ok("Created user: {0}", user.getNameValue());
            return uid;
        } catch (SQLException ex) {
            log.error(ex, "error in create");
            throw ConnectorException.wrap(ex);
        } finally {
            conn.closeConnection();
        }
    }
    
    /**
     * Deletes a mysql user using drop statement
     * @param oclass the type of object to delete. Only ACCOUNT is supported.
     * @param uid the {@link Uid} of the user to delete
     * @param options additional options. Additional options are not supported in this operation. 
     * 
     * @throws UnknownUidException if the specified Uid does not exist on the target resource
     * @throws ConnectorException if a problem occurs with the connection
     * 
     * @see DeleteOp#delete(ObjectClass, Uid, OperationOptions)
     */
    public void delete(final ObjectClass oclass, final Uid uid, final OperationOptions options) {
        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }

        if (uid == null || (uid.getUidValue() == null)) {
            throw new IllegalArgumentException(config.getMessage(MSG_UID_BLANK));
        }
        try {
            log.info("Delete user uid: {0}", uid.getName());
            conn.openConnection();
            //Delete the user
            deleteUser(uid);
            //Commit delete
            conn.commit();
            log.ok("Deleted user uid: {0}", uid.getName());
        } catch (SQLException ex) {
            log.error(ex, "error in delete");
            throw ConnectorException.wrap(ex);
        } finally {
            conn.closeConnection();
        }
    }
    
    /**
     * Update the database row w/ the data provided.
     * @param oclass the {@link ObjectClass} type (must be ACCOUNT )
     * @param attrs attributes. Required attributes are name and password
     * @param options additional options. Additional options are not supported in this operation.  
     * 
     * @see UpdateOp#update(ConnectorObject, Set, OperationOptions )
     */
    public Uid update(final ObjectClass oclass, Uid oldUid, final Set<Attribute> attrs, final OperationOptions options) {
        final String SQL_UPDATE = "UPDATE mysql.user SET {0} WHERE user=?";
        final String SQL_SET_USER = "user = ?";
        final String SQL_SET_PASSWORD = "password = password(?)";

        checkAttributes(oclass, attrs);
        
        // init the return value for old Uid
        Uid ret = oldUid;
        String updateSet = "";
        // Bind values
        final List<SQLParam> values = new ArrayList<SQLParam>();
        //The update is changing name. The oldUid is a key and the name will be new uid for mysql.
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        // is there a change in name?
        if(name != null && oldUid.getUidValue() != name.getNameValue()) {
            log.info("Update user {0} to (1)", oldUid.getUidValue(), name.getNameValue());
            updateSet = SQL_SET_USER;
            values.add(new SQLParam(name.getNameValue(), Types.VARCHAR));
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
            values.add(new SQLParam(password));
        }

        // Finalize update
        String sql = MessageFormat.format(SQL_UPDATE, updateSet);
        values.add(new SQLParam(oldUid.getUidValue(), Types.CHAR));
        
        try {
            conn.openConnection();
            // Update the user, insert bind values into the statement
            updateUser(sql, values);            
            // Commit changes
            conn.commit();
        } catch (SQLException ex) {
            log.error(ex, "error in update");
            throw ConnectorException.wrap(ex);
        } finally {
            conn.closeConnection();
        }
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
        final String ALL_USER_QUERY = "SELECT DISTINCT User FROM mysql.user";

        // Get the needed attributes
        if(oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED)); 
        }
        
        // Database query builder will create SQL query.
        // if where == null then all users are returned
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(ALL_USER_QUERY);
        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            conn.openConnection();
            
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                
                //To be sure that uid and name are present for mysql
                final String userName = result.getString(1);
                //ISSUE, cloud be en empty string for anonymous public user
                if(userName == null || userName.length()==0) {
                    //Skip this line
                    continue;
                }
                
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
            //Commit finally
            conn.commit();
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(conn);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
            
            conn.closeConnection();
        }
        
        log.ok("executeQuery");
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
        
        log.ok("schema");        
        return schemaBld.build();
    } 


    /**
     * Test the configuration and connection
     * @see org.identityconnectors.framework.spi.operations.TestOp#test()
     */
    public void test() {
        try {
            conn.openConnection();
            conn.test();
            if (!findUser(config.getUsermodel())) {
                SQLUtil.rollbackQuietly(conn);
                throw new IllegalArgumentException(config.getMessage(MSG_USER_MODEL_NOT_FOUND, config.getUsermodel()));
            }
            conn.commit();
        } catch (SQLException e) {
            log.error(e, "Error in test");
            //No rolback, the error could be just in the openConnection
            throw ConnectorException.wrap(e);
        } finally {
            conn.closeConnection();
        }
        log.ok("test");
    }
    

    /** 
     * Attempts to authenticate the given user/password combination.
     * 
     * @param user the username of the user
     * @param password the user's password
     * @throws InvalidCredentialException if the user is not authenticated
     *  
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(java.lang.String, java.lang.String, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass oclass, String user, GuardedString password, OperationOptions options) {
        final String AUTH_SELECT="SELECT DISTINCT user FROM mysql.user WHERE user = ? AND password = password(?)";
        
        log.info("authenticate user: {0}", user);

        // Get the needed attributes
        if(oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED)); 
        }         

        if (user == null || StringUtil.isBlank(user)) {
            throw new IllegalArgumentException(config.getMessage(MSG_NAME_BLANK));
        }

        if (password == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_PWD_BLANK));
        }
        
        List<SQLParam> values = new ArrayList<SQLParam>();
        values.add(new SQLParam(user, Types.VARCHAR));
        values.add(new SQLParam(password));
        
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            conn.openConnection();
            
            stmt = conn.prepareStatement(AUTH_SELECT, values);
            result = stmt.executeQuery();
            //No PasswordExpired capability
            if (!result.next()) {
                throw new InvalidCredentialException(config.getMessage(MSG_AUTH_FAILED, user));
            }            
            final Uid uid = new Uid( result.getString(1));
            
            conn.commit();

            log.info("user: {0} authenticated ", user);
            return uid;
        } catch (SQLException e) {
            log.error(e, "user: {0} authentication failed ", user);
            SQLUtil.rollbackQuietly(conn);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(stmt);
            
            conn.closeConnection();
        }
    }

    /**
     * Only supported attributes
     * @param oclass the only one supported class
     * @param attrs the set of attributes
     */
    private void checkAttributes(final ObjectClass oclass, final Set<Attribute> attrs) {
        // Get the needed attributes
        if(oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED)); 
        }
        
        if(attrs == null || attrs.size() == 0) {
            throw new IllegalArgumentException(config.getMessage(MSG_INVALID_ATTRIBUTE_SET)); 
        }               
        
            // Check for known attributes
        for (Attribute attribute : attrs) {
            if (attribute == null)
                throw new IllegalArgumentException(config.getMessage(MSG_INVALID_ATTRIBUTE_SET));
            if (attribute.is(Name.NAME))
                continue;
            if (attribute.is(Uid.NAME))
                continue;
            if (attribute.is(OperationalAttributes.PASSWORD_NAME))
                continue;
            throw new IllegalArgumentException(config.getMessage(MSG_INVALID_ATTRIBUTE_SET));
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
            List<SQLParam> values = new ArrayList<SQLParam>();
            values.add(new SQLParam(name, Types.VARCHAR));
            values.add(new SQLParam(password));
            c1 = conn.prepareStatement(SQL_CREATE_TEMPLATE, values);
            c1.execute();
            log.ok("User {0} created", name);
        } catch (SQLException e) {
            if(findUser(name)) {
                log.error(e, "Already Exists user {0}", name);
                SQLUtil.rollbackQuietly(conn);
                throw new AlreadyExistsException(e);
            } else {
                grantUssage(name, password);
            }
        } finally {
            SQLUtil.closeQuietly(c1);
        }
    }
    
    /**
     * creates a new user for MySQL4.1 resource
     * @param name
     * @param password
     */
    private void grantUssage(String name, GuardedString password) {
        final String SQL_GRANT_TEMPLATE = "GRANT USAGE ON *.* TO ?@'localhost' IDENTIFIED BY ?";
        PreparedStatement c1 = null;
        try {
            // Create the user
            List<SQLParam> values = new ArrayList<SQLParam>();
            values.add(new SQLParam(name, Types.VARCHAR));
            values.add(new SQLParam(password));
            c1 = conn.prepareStatement(SQL_GRANT_TEMPLATE, values);
            c1.execute();
        } catch (SQLException e) {
            log.error(e, "Grant user {0} exception", name);
            SQLUtil.rollbackQuietly(conn);
            throw new IllegalStateException(e);
        } finally {
            SQLUtil.closeQuietly(c1);
        }
    }    
    
    /**
     * Find the user
     * @param userName
     */
    private boolean findUser(String userName) {
        PreparedStatement ps = null;
        ResultSet result = null;
        final List<SQLParam> values = new ArrayList<SQLParam>();
        values.add(new SQLParam(userName, Types.VARCHAR));
        final String SQL_SELECT = "SELECT DISTINCT user FROM mysql.user WHERE user = ?";
        log.info("findt User {0}", userName);
        try {
            ps = conn.prepareStatement(SQL_SELECT, values);
            result = ps.executeQuery();
            if(result.next()) {
                return true;
            }
        } catch (SQLException ex) {
            log.error(ex,"find User {0} ", userName);
            SQLUtil.rollbackQuietly(conn);
            throw new IllegalStateException(ex);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
        }
        return false;
    }      
  
    /**
     * Read the rights for the user model of mysql
     * @param modelUser name
     * @return list of the rights
     */
    private List<String> readGrantsForModelUser(String modelUser) {
        final String SQL_SHOW_MODEL_USERS="SELECT host FROM mysql.user WHERE user = ?";
        final String SQL_SHOW_GRANTS = "SHOW GRANTS FOR ?";
        PreparedStatement c1 = null;
        PreparedStatement c2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;
        List<String> grants = new ArrayList<String>();
        try {
            // created, read the model user grants
            c1 = conn.getConnection().prepareStatement(SQL_SHOW_MODEL_USERS);
            c1.setString(1, modelUser);
            rs1 = c1.executeQuery();
            while (rs1.next()) {
                final StringBuilder query = new StringBuilder(SQL_SHOW_GRANTS);
                final String host = rs1.getString(1);
                log.ok("readGrantsFor host:{0}, user:{1}", host, modelUser);
                //The host specification must be added when defined
                if ((host != null) && !host.equals("") && !host.equals("%")) {
                    query.append("@"+host);
                }             
                c2 = conn.getConnection().prepareStatement(query.toString());
                c2.setString(1, modelUser);
                rs2 = c2.executeQuery();
                while (rs2.next()) {
                    String grant = rs2.getString(1);
                    grants.add(grant);
                }
            }
        } catch (SQLException e) {
            log.error(e, "Error read GRANTS for model user {0}", modelUser);
            //No error when the modelUser does not exist
            SQLUtil.rollbackQuietly(conn);
            //throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(rs1);
            SQLUtil.closeQuietly(rs2);
            SQLUtil.closeQuietly(c1);
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
            newGrant = newGrant.replaceAll("IDENTIFIED BY PASSWORD '.*'", "IDENTIFIED BY ?");
            newGrants.add(newGrant);
        }
        return newGrants;
    }    
    
    /**
     * Execute the GRANT statement for the given userName and rights
     * @param grants rights for the new user
     * @param userName ID of the new user
     */
    private void grantingRights(List<String> grants, String userName, GuardedString password) {
        for (String grant : grants) {
            final PreparedStatement[] psa = new PreparedStatement[1];
            try {
                psa[0] = conn.getConnection().prepareStatement(grant);
                log.info("Granting rights {0} for user: {1}", userName, grant);
                if(grant.indexOf("IDENTIFIED BY ?")>0) {
                    password.access(new GuardedString.Accessor() {
                        public void access(char[] clearChars) {
                            try {
                                psa[0].setObject(1, new String(clearChars));
                            } catch (SQLException e) {
                                // checked exception are not allowed in the access method 
                                // Lets use the exception softening pattern
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
                psa[0].execute();
            } catch (SQLException e) {
                log.error(e, "Error granting rights {0} for user: {1}", userName, grant);
                SQLUtil.rollbackQuietly(conn);
                throw ConnectorException.wrap(e);
            } finally {
                SQLUtil.closeQuietly(psa[0]);
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
        final String SQL_SHOW_MODEL_USERS="SELECT host FROM mysql.user WHERE user = ?";
        final String SQL_DELETE_TEMPLATE = "DROP USER ?";

        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs1 = null;
        try {
            // created, read the model user grants
            ps1 = conn.getConnection().prepareStatement(SQL_SHOW_MODEL_USERS);
            ps1.setString(1, uid.getUidValue());
            rs1 = ps1.executeQuery();
            boolean unknown = true;
            while (rs1.next()) {
                unknown  = false;
                final StringBuilder query = new StringBuilder(SQL_DELETE_TEMPLATE);
                final String host = rs1.getString(1);
                //The host specification must be added when defined
                if ((host != null) && !host.equals("") && !host.equals("%")) {
                    query.append("@"+host);
                }
                // create a prepared call..
                ps2 = conn.getConnection().prepareStatement(query.toString());                                
                // set object to delete..
                ps2.setString(1, uid.getUidValue());
                // uid to delete..
                log.info("Deleting Uid: {0}, host:{1}", uid.getUidValue(), host);
                ps2.execute();
            }
            if( unknown ) {
               throw new UnknownUidException(uid, ObjectClass.ACCOUNT);
            }
        } catch (SQLException e) {
            deleteUser41(uid);
        } finally {
            // clean up..
            SQLUtil.closeQuietly(rs1);
            SQLUtil.closeQuietly(ps1);
            SQLUtil.closeQuietly(ps2);
        }
        log.ok("Deleted Uid: {0}", uid.getUidValue());
    }

    /**
     * Delete The User on 41 resource
     * 
     * @param uid
     *            the uid of the user
     * @param connection
     */
    private void deleteUser41(final Uid uid) {
        final String SQL_DELETE_USERS="DELETE FROM user WHERE User=?";
        final String SQL_DELETE_DB="DELETE FROM db WHERE User=?";
        final String SQL_DELETE_TABLES="DELETE FROM tables_priv WHERE User=?";
        final String SQL_DELETE_COLUMNS="DELETE FROM columns_priv WHERE User=?";


        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        try {
            // created, read the model user grants
            ps1 = conn.getConnection().prepareStatement(SQL_DELETE_USERS);
            ps1.setString(1, uid.getUidValue());
            ps1.execute();
            ps2 = conn.getConnection().prepareStatement(SQL_DELETE_DB);
            ps2.setString(1, uid.getUidValue());
            ps2.execute();
            ps3 = conn.getConnection().prepareStatement(SQL_DELETE_TABLES);
            ps3.setString(1, uid.getUidValue());
            ps3.execute();
            ps4 = conn.getConnection().prepareStatement(SQL_DELETE_COLUMNS);
            ps4.setString(1, uid.getUidValue());
            ps4.execute();
            flushPriviledges();
        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(conn);
            log.error(e, "delete user 41");
            throw new IllegalStateException(e); 
        } finally {
            // clean up..
            SQLUtil.closeQuietly(ps1);
            SQLUtil.closeQuietly(ps2);
            SQLUtil.closeQuietly(ps3);
            SQLUtil.closeQuietly(ps4);
        }
        log.ok("Deleted Uid: {0}", uid.getUidValue());
    }    
    

    /**
     * Update the user identified by uid using the update string and list of bind values
     * 
     * @param updstr
     *            the SQL update string
     * @param values
     *            the object values to be bind
     */
    private void updateUser(String updstr, List<SQLParam> values) {
        // create the sql statement..
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
        flushPriviledges();
    }


    /**
     * Make all privilege changes the most actual on resource
     */
    private void flushPriviledges() {
        // Flush privileges
        final String FLUSH_PRIVILEGES = "FLUSH PRIVILEGES";
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
