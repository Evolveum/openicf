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
 * Portions Copyrighted 2013-2022 Evolveum
 */
package org.identityconnectors.databasetable;

import static org.identityconnectors.databasetable.DatabaseTableConstants.*;

import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.databasetable.config.JsonConfigHandler;
import org.identityconnectors.databasetable.config.SqlHandler;
import org.identityconnectors.databasetable.config.UniversalObjectClassHandler;
import org.identityconnectors.databasetable.mapping.MappingStrategy;
import org.identityconnectors.databasetable.mapping.misc.SQLColumnTypeInfo;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder.OrderBy;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

/**
 * The database table {@link DatabaseTableConnector} is a basic, but easy to use
 * {@link DatabaseTableConnector} for accounts in a relational database.
 * <p>
 * It supports create, update, search, and delete operations. It can also be
 * used for pass-thru authentication, although it assumes the password is in
 * clear text in the database.
 * <p>
 * This connector assumes that all account data is stored in a single database
 * table. The delete action is implemented to simply remove the row from the
 * table.
 * <p>
 *
 * @author Will Droste
 * @author Keith Yarbrough
 * @version $Revision $
 * @since 1.0
 */
@ConnectorClass(
        displayNameKey = "DBTABLE_CONNECTOR",
        configurationClass = DatabaseTableConfiguration.class)
public class DatabaseTableConnector implements PoolableConnector, CreateOp, SearchOp<FilterWhereBuilder>,
        DeleteOp, UpdateOp, SchemaOp, TestOp, AuthenticateOp, SyncOp, ResolveUsernameOp, DiscoverConfigurationOp {

    /**
     * Setup logging for the {@link DatabaseTableConnector}.
     */
    static Log log = Log.getLog(DatabaseTableConnector.class);

    //    /**
//     * Place holder for the {@link Connection} passed into the callback
//     * {@link ConnectionFactory#setConnection(Connection)}.
//     */
    private DatabaseTableConnection conn;

    /**
     * Place holder for the {@link Configuration} passed into the callback
     * {@link DatabaseTableConnector#init(Configuration)}.
     */
    private DatabaseTableConfiguration config;

    /**
     * Schema cache is used. The schema creation need a jdbc query.
     */
    private Schema schema;

    /**
     * Default attributes to get, created and cached from the schema
     */
    private Map<String, Set<String>> defaultAttributesToGet;

    /**
     * Same of the data types must be converted
     */
    private Map<String, SQLColumnTypeInfo> columnSQLTypes;

    /**
     * Stores values from different tables
     */
    private  Map<String, Map<String, SQLColumnTypeInfo>> columnSQLTypesCollection;

    /**
     * Cached value for required columns
     */
    private Set<String> stringColumnRequired;

    // =======================================================================
    // Initialize/dispose methods..
    // =======================================================================

    /**
     * {@inheritDoc}
     */
    public DatabaseTableConfiguration getConfiguration() {
        return this.config;
    }

    private Map<String, UniversalObjectClassHandler> universalObjectClassHandlers;
    private SqlHandler sqlHandler;

    /**
     * Init the connector
     * {@inheritDoc}
     */
    public void init(Configuration cfg) {
        log.info("init DatabaseTable connector");
        this.config = (DatabaseTableConfiguration) cfg;

        // Creates and sets config classes according to connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")) {
            JsonConfigHandler jsonConfigHandler = new JsonConfigHandler(this.config);
            this.universalObjectClassHandlers = jsonConfigHandler.getUniversalObjectClassHandlers();
        } else if (Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
            this.sqlHandler = new SqlHandler(this.config);
            this.config.setTable(this.sqlHandler.getTable());
        }

        this.schema = null;
        this.defaultAttributesToGet = null;
        this.columnSQLTypes = null;
        this.columnSQLTypesCollection = null;
        log.ok("init DatabaseTable connector ok, connection is valid");
    }


    /**
     * {@inheritDoc}
     */
    public void checkAlive() {
        log.info("checkAlive DatabaseTable connector");
        try {
            if (StringUtil.isNotBlank(config.getDatasource())) {
                openConnection();
            } else {
                this.config.setValidationOnlyConnection();
                getConn().test();
                this.config.setValidationFull();
                commit();
            }
        } catch (SQLException e) {
            evaluateAndHandleException(e, true, true, true, "error in checkAlive");
        }
        //Check alive will not close the connection, the next API call is expected
        log.ok("checkAlive DatabaseTable connector ok");
    }

    /**
     * The connector connection access method
     *
     * @return connection
     */
    DatabaseTableConnection getConn() {
        //Lazy initialize the connection
        if (conn == null) {
            this.config.validate();
            //Validate first to minimize wrong resource access
            this.conn = DatabaseTableConnection.createDBTableConnection(this.config);
        }
        return conn;
    }

    /**
     * Disposes of the {@link DatabaseTableConnector}'s resources.
     * {@inheritDoc}
     */
    public void dispose() {
        log.info("dispose DatabaseTable connector");
        if (conn != null) {
            conn.dispose();
            conn = null;
        }
        this.defaultAttributesToGet = null;
        this.schema = null;
        this.columnSQLTypes = null;
        this.columnSQLTypesCollection = null;
    }

    /**
     * Creates a row in the database representing an account.
     * {@inheritDoc}
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")){
            throw new UnsupportedOperationException("Create operation is not supported in 'Custom' connector mode");
        }

        log.info("create account, check the ObjectClass");
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }

        if (attrs == null || attrs.isEmpty()) {
            throw new IllegalArgumentException(this.config.getMessage(MSG_INVALID_ATTRIBUTE_SET));
        }
        log.ok("Attribute set is not empty");

        //Name must be present in attribute set or must be generated UID set on
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        if (name == null) {
            throw new IllegalArgumentException(this.config.getMessage(MSG_NAME_BLANK));
        }

        final String accountName = name.getNameValue();
        log.ok("Required Name attribure value {0} for create", accountName);

        // Sets table name and isEnableEmptyString from config specified by connector mode
        String tblname;
        boolean isEnableEmptyString;
        if(Objects.equals(this.config.getConnectorMode(), "Json")){
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
            isEnableEmptyString = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).isEnableEmptyString();
        } else {
            tblname = this.config.getTable();
            isEnableEmptyString = this.config.isEnableEmptyString();
        }

        // start the insert statement
        final InsertIntoBuilder bld = new InsertIntoBuilder();

        log.info("Creating account: {0}", accountName);
        Set<String> missingRequiredColumns = CollectionUtil.newCaseInsensitiveSet();
        if (isEnableEmptyString) {
            final Set<String> mrc = getStringColumnReguired();
            log.info("Empty String is enabled, add missing required columns {0}", mrc);
            missingRequiredColumns.addAll(mrc);
        }
        log.info("process and check the Attribute Set");
        //All attribute names should be in create columns statement
        for (Attribute attr : attrs) {
            if (attr == null) {
                throw new IllegalArgumentException("Attribute cannot be null");
            } else {
                // quoted column name
                final String columnName = getColumnName(oclass, attr.getName());
                Object value = AttributeUtil.getSingleValue(attr);
                //Empty String
                if (isToBeEmpty(columnName, value)) {
                    log.info("create account, attribute for a column {0} is null and should be empty", columnName);
                    value = EMPTY_STR;
                }
                final SQLColumnTypeInfo sqlColumnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), columnName);
                log.info("attribute {0} fit column {1} and sql type {2}", attr.getName(), columnName, sqlColumnTypeInfo);
                bld.addBind(new SQLParam(quoteName(columnName), value, sqlColumnTypeInfo.getTypeCode(), sqlColumnTypeInfo.getTypeName()));
                missingRequiredColumns.remove(columnName);
                log.ok("attribute {0} was added to insert", attr.getName());
            }
        }

        // Bind empty string for not-null columns which are not in attribute set list
        if (isEnableEmptyString) {
            log.info("there are columns not matched in attribute set which should be empty");
            for (String mCol : missingRequiredColumns) {
                SQLColumnTypeInfo typeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), mCol);
                bld.addBind(new SQLParam(quoteName(mCol), EMPTY_STR, typeInfo.getTypeCode(), typeInfo.getTypeName()));
                log.ok("Required empty value to column {0} added", mCol);
            }
        }

        final String SQL_INSERT = "INSERT INTO {0} ( {1} ) VALUES ( {2} )";
        // create the prepared statement..
        final String sql = MessageFormat.format(SQL_INSERT, tblname, bld.getInto(), bld.getValues());

        PreparedStatement pstmt = null;
        try {
            openConnection();
            pstmt = getConn().prepareStatement(sql, bld.getParams());
            // execute the SQL statement
            pstmt.execute();
            log.info("Create account {0} commit", accountName);
            commit();
        } catch (SQLException e) {
            evaluateAndHandleException(e, true, true, false, MSG_CAN_NOT_CREATE, accountName);
        } finally {
            IOUtil.quietClose(pstmt);
            closeConnection();
        }
        log.ok("Account {0} created");
        // create and return the uid..
        return new Uid(accountName);

    }


    /**
     * Test to throw the exception
     *
     * @param errorCode exception
     */
    private boolean throwIt(int errorCode) {
        return config.isRethrowAllSQLExceptions() || errorCode != 0;
    }

    /**
     * Test is value is null and must be empty
     *
     * @param columnName the column name
     * @param value the value to tests
     * @return true/false
     */
    private boolean isToBeEmpty(final String columnName, Object value) {
        return config.isEnableEmptyString() && getStringColumnReguired().contains(columnName) && value == null;
    }

    /**
     * Deletes a row from the table.
     * {@inheritDoc}
     */
    public void delete(final ObjectClass oclass, final Uid uid, final OperationOptions options) {
        if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")){
            throw new UnsupportedOperationException("Delete operation is not supported in 'Custom' connector mode");
        }

        log.info("delete account, check the ObjectClass");

        final String SQL_DELETE = "DELETE FROM {0} WHERE {1} = ?";
        PreparedStatement stmt = null;
        // create the SQL string..

        if (oclass == null ) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        } else {
            log.ok("The ObjectClass is ok");

            if (uid == null) {
                throw new IllegalArgumentException("Uid cannot be null");
            } else {
                if (uid.getUidValue() == null) {
                    throw new IllegalArgumentException(this.config.getMessage(MSG_UID_BLANK));
                } else {
                    final String accountUid = uid.getUidValue();
                    log.ok("The Uid is present");

                    String tblname;
                    String keycol;
                    // Sets table name and key column from config specified by connector mode
                    if(Objects.equals(this.config.getConnectorMode(), "Json")){
                        tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
                        keycol = quoteName(this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn());
                    } else {
                        tblname = this.config.getTable();
                        keycol = this.config.getKeyColumn();
                    }

                    final String sql = MessageFormat.format(SQL_DELETE, tblname, keycol);
                    try {
                        log.info("delete account SQL {0}", sql);
                        openConnection();
                        // create a prepared call..
                        stmt = getConn().getConnection().prepareStatement(sql);
                        // set object to delete..
                        final SQLColumnTypeInfo sqlColumnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), keycol);
                        SQLUtil.setSQLParam(stmt, 1, new SQLParam(quoteName(keycol), accountUid, sqlColumnTypeInfo.getTypeCode(), sqlColumnTypeInfo.getTypeName()));
                        // stmt.setString(1, accountUid);
                        // uid to delete..
                        log.info("Deleting account Uid: {0}", accountUid);
                        final int dr = stmt.executeUpdate();
                        if (dr < 1) {
                            SQLUtil.rollbackQuietly(getConn());
                            // TODO: Before we throw this error we should recheck that the account is really not there with select.
                            // This may happen when the "table" is view and update returns UPDATED 0.
                            // The consequences of killing the shadow are quite annoying, so we should double-check.
                            // Of course, the previous behaviour quietly ignoring the problem was not good either.
                            handleUnknownUid(MSG_EXP_UNKNOWN_UID, accountUid);
                        }
                        if (dr > 1) {
                            SQLUtil.rollbackQuietly(getConn());
                            // TODO: This one is even stranger - again, should it make the shadow dead?
                            // It likely should not be ignored like before, I agree with that.
                            handleUnknownUid(MSG_EXP_TOO_MANY_UID, accountUid);
                        }
                        log.info("Delete account {0} commit", accountUid);
                        commit();
                    } catch (SQLException e) {
                        SQLUtil.rollbackQuietly(getConn());
                        evaluateAndHandleException(e, false, true, false, MSG_CAN_NOT_DELETE, accountUid);
                    } finally {
                        IOUtil.quietClose(stmt);
                        closeConnection();
                    }
                    log.ok("Account Uid {0} deleted", accountUid);

                }
            }
        }
    }

    /**
     * Update the database row with the data provided.
     * {@inheritDoc}
     */
    public Uid update(ObjectClass oclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")){
            throw new UnsupportedOperationException("Update operation is not supported in 'Custom' connector mode");
        }

        log.info("update account, check the ObjectClass");

        final String sqlTemplate = "UPDATE {0} SET {1} WHERE {2} = ?";
        // create the sql statement..

        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }
        log.ok("The ObjectClass is ok");

        if (attrs == null || attrs.isEmpty()) {
            throw new IllegalArgumentException(this.config.getMessage(MSG_INVALID_ATTRIBUTE_SET));
        }
        log.ok("Attribute set is not empty");

        final String accountUid = uid.getUidValue();
        Assertions.nullCheck(accountUid, "accountUid");
        log.ok("Account uid {0} is present", accountUid);

        Uid ret = uid;
        // The update is changing name. The oldUid is a key and the name will become new uid.
        final Name name = AttributeUtil.getNameFromAttributes(attrs);
        String accountName = accountUid;
        if (name != null && !accountUid.equals(name.getNameValue())) {
            accountName = name.getNameValue();
            Assertions.nullCheck(accountName, "accountName");
            log.info("Account name {0} is present and is not the same as uid", accountName);
            ret = new Uid(accountName);
            log.ok("Renaming account uid {0} to name {1}", accountUid, accountName);
        }

        log.info("process and check the Attribute Set");
        UpdateSetBuilder updateSet = new UpdateSetBuilder();
        for (Attribute attribute : attrs) {
            // All attributes needs to be updated except the UID
            if (!attribute.is(Uid.NAME)) {
                final String attributeName = attribute.getName();
                final String columnName = getColumnName(oclass, attributeName);
                Object value = AttributeUtil.getSingleValue(attribute);
                // Handle the empty string values
                if (isToBeEmpty(columnName, value)) {
                    log.info("Append empty attribute {0} for required columnName {1}", attributeName, columnName);
                    value = DatabaseTableConstants.EMPTY_STR;
                }
                final SQLColumnTypeInfo sqlColumnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), columnName);
                final SQLParam param = new SQLParam(quoteName(columnName), value, sqlColumnTypeInfo.getTypeCode(), sqlColumnTypeInfo.getTypeName());
                updateSet.addBind(param);
                log.ok("Appended to update statement the attribute {0} for columnName {1} and sql type code {2}", attributeName, columnName, sqlColumnTypeInfo.getTypeCode());
            }
        }
        log.info("Update account {0}", accountName);

        String tblname;
        String keycol;

        // Sets table name and key column from config specified by connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")){
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
            keycol = quoteName(this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn());
        } else {
            tblname = this.config.getTable();
            keycol = this.config.getKeyColumn();
        }

        // Format the update query
        SQLColumnTypeInfo columnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), keycol);
        updateSet.addValue(new SQLParam(keycol, accountUid, columnTypeInfo.getTypeCode(), columnTypeInfo.getTypeName()));
        final String sql = MessageFormat.format(sqlTemplate, tblname, updateSet.getSQL(), keycol);
        PreparedStatement stmt = null;
        try {
            openConnection();
            // create the prepared statement..
            stmt = getConn().prepareStatement(sql, updateSet.getParams());

            log.info("The prepared statement in case of update {0}", stmt.toString());

            int retCode = stmt.executeUpdate();
            log.info("The return code for the statement: {0}", retCode);
            // commit changes

            if (retCode == 0) {
                String uidValue = uid.getUidValue();
                // TODO: Before we throw this error we should recheck that the account is really not there with select.
                // This may happen when the "table" is view and update returns UPDATED 0.
                // The consequences of killing the shadow are quite annoying, so we should double-check.
                // Of course, the previous behaviour quietly ignoring the problem was not good either.
                log.error("Account with the uid {0} not found during the update operation.", uidValue);
                handleUnknownUid(MSG_OP_UPDATE_UNKNOWN, uidValue);
            }
            log.info("Update account {0} commit", accountName);
            commit();
        } catch (SQLException e) {
            evaluateAndHandleException(e, true, true, false, MSG_CAN_NOT_UPDATE, accountName);
        } finally {
            IOUtil.quietClose(stmt);
            closeConnection();
        }
        log.ok("Account {0} updated", accountName);
        return ret;
    }


    /**
     * Creates a Database Table filter translator.
     * {@inheritDoc}
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        log.info("check the ObjectClass");
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }
        log.ok("The ObjectClass is ok");
        return new DatabaseTableFilterTranslator(this, oclass, options);
    }

    /**
     * Search for rows
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        log.info("check the ObjectClass and result handler");
        // Contract tests
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }

        if (handler == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_RESULT_HANDLER_NULL));
        }
        log.ok("The ObjectClass and result handler is ok");

        //Names
        String tblname;
        // Sets table name from config specified by connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")){
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
        } else {
            tblname = config.getTable();
        }

        if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
            executeSqlQuery(oclass, where, handler);
        } else {
            final Set<String> columnNamesToGet = resolveColumnNamesToGet(oclass, options);
            log.ok("Column Names {0} To Get", columnNamesToGet);
            // For all account query there is no need to replace or quote anything
            ResultSet result = null;
            PreparedStatement statement = null;
            final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, columnNamesToGet);
            query.setWhere(where);

            try {
                openConnection();
                statement = getConn().prepareStatement(query);
                result = statement.executeQuery();
                log.ok("executeQuery {0} on {1}", query.getSQL(), oclass);
                while (result.next()) {
                    final Map<String, SQLParam> columnValues = getConn().getColumnValues(result);
                    log.ok("Column values {0} from result set ", columnValues);
                    // create the connector object
                    final ConnectorObjectBuilder bld = buildConnectorObject(oclass,columnValues);
                    if (!handler.handle(bld.build())) {
                        log.ok("Stop processing of the result set");
                        break;
                    }
                }
                // commit changes
                log.info("commit executeQuery");
                commit();
            } catch (SQLException e) {
                log.error(e, "Query {0} on {1} error", query.getSQL(), oclass);

                SQLUtil.rollbackQuietly(getConn());
                evaluateAndHandleException(e, true, true, false, MSG_CAN_NOT_READ, tblname);
            } finally {
                IOUtil.quietClose(result);
                IOUtil.quietClose(statement);
                closeConnection();
            }
            log.ok("Query Account committed");
        }
    }

    private String buildSqlQuery(FilterWhereBuilder where) {
        String whereCondition = "";
        String sqlQuery = this.sqlHandler.getSqlQuery();

        if(where != null) {
            whereCondition = where.getWhereClause();
            int minIndex = sqlQuery.length();

            int whereIndex = this.sqlHandler.getWhereIndex();
            if (whereIndex == -1 || whereIndex > minIndex) {
                return sqlQuery + " WHERE " + whereCondition;
            }

            if(this.sqlHandler.getWhereClause()!=null) {
                return sqlQuery.substring(0,whereIndex) + "WHERE " + whereCondition + " AND " + sqlQuery.substring(whereIndex+6);
            }

            return sqlQuery.substring(0,whereIndex) + "WHERE " + whereCondition + " " + sqlQuery.substring(whereIndex);
        }
        return sqlQuery;
    }

    private boolean isNumericType(int sqlType) {
        switch (sqlType) {
            case -7:  // BIT
            case -6:  // TINYINT
            case -5:  // BIGINT
            case 2:   // NUMERIC
            case 3:   // DECIMAL
            case 4:   // INTEGER
            case 5:   // SMALLINT
            case 6:   // FLOAT
            case 7:   // REAL
            case 8:   // DOUBLE
                return true;
            default:
                return false;
        }
    }

    private static String detectOperatorFor(String whereSql, String sourceName, Map<String, String> aliasBySource) {
        if (whereSql == null) {
            whereSql = "";
        }
        if (sourceName == null || sourceName.trim().isEmpty()) {
            return "=";
        }


        String alias = (aliasBySource != null) ? aliasBySource.get(sourceName) : null;

        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (alias != null && !alias.isEmpty()) {
            candidates.add(alias);
        }
        candidates.add(sourceName);

        for (String cand : candidates) {
            String patName = java.util.regex.Pattern.quote(cand);
            java.util.regex.Pattern pat = java.util.regex.Pattern.compile("(?i)" + patName + "\\s*(=|like|equals)\\b");
            java.util.regex.Matcher m = pat.matcher(whereSql);
            if (m.find()) {
                String op = m.group(1);
                if ("equals".equalsIgnoreCase(op)) {
                    return "=";
                }
                if ("like".equalsIgnoreCase(op)) {
                    return "LIKE";
                }
                return "=";
            }
        }

        return "=";
    }

    private String getSqlTypeName(int sqlType) {
        switch (sqlType) {
            case -7: return "BIT";
            case -6: return "TINYINT";
            case -5: return "BIGINT";
            case -4: return "LONGVARBINARY";
            case -3: return "VARBINARY";
            case -2: return "BINARY";
            case -1: return "LONGVARCHAR";
            case 1: return "CHAR";
            case 2: return "NUMERIC";
            case 3: return "DECIMAL";
            case 4: return "INTEGER";
            case 5: return "SMALLINT";
            case 6: return "FLOAT";
            case 7: return "REAL";
            case 8: return "DOUBLE";
            case 12: return "VARCHAR";
            case 16: return "BOOLEAN";
            case 91: return "DATE";
            case 92: return "TIME";
            case 93: return "TIMESTAMP";
            case 2004: return "BLOB";
            case 2005: return "CLOB";
            default: return "SQL Type:" + sqlType;
        }
    }

    private Object convertToNumericType(String value, int sqlType) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimmedValue = value.trim();

        switch (sqlType) {
            case -7:  // BIT
            case 16:  // BOOLEAN
                return Boolean.parseBoolean(trimmedValue);

            case -6:  // TINYINT
                return Byte.parseByte(trimmedValue);

            case 5:   // SMALLINT
                return Short.parseShort(trimmedValue);

            case 4:   // INTEGER
                return Integer.parseInt(trimmedValue);

            case -5:  // BIGINT
                return Long.parseLong(trimmedValue);

            case 6:   // FLOAT
            case 7:   // REAL
                return Float.parseFloat(trimmedValue);

            case 8:   // DOUBLE
                return Double.parseDouble(trimmedValue);

            case 2:   // NUMERIC
            case 3:   // DECIMAL
                return new java.math.BigDecimal(trimmedValue);

            default:
                return value;
        }
    }

    private void executeSqlQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler) {
        List<SQLParam> params = new ArrayList<>();
        if (where != null) {
            params = where.getParams();

            String originalWhereSql = where.getWhere() != null ? where.getWhere().toString() : "";

            List<SQLParam> replacedParams = new ArrayList<>();
            if (!this.sqlHandler.getColumnAliasBySource().isEmpty()) {

                ListIterator<SQLParam> it = params.listIterator();
                while (it.hasNext()) {
                    SQLParam p = it.next();
                    String oldName = p.getName();
                    String newName = this.sqlHandler.getSourceByColumnAlias().get(oldName);
                    Object value = p.getValue();

                    if(isNumericType(p.getSqlType())) {
                        if (value instanceof String) {
                            String strValue = (String) value;
                            String originalValue = strValue;

                            if (strValue.startsWith("%")) {
                                strValue = strValue.substring(1);
                            }

                            if (strValue.endsWith("%")) {
                                strValue = strValue.substring(0, strValue.length() - 1);
                            }

                            if (!originalValue.equals(strValue)) {
                                log.info("Removed leading/trailing % from value ''{0}'' -> ''{1}'' for column {2}",
                                        originalValue, strValue, p.getName());
                                value = strValue;
                            }

                            if (isNumericType(p.getSqlType())) {
                                if (strValue.contains("%") || strValue.contains("_")) {
                                    String cleanValue = strValue.replace("%", "").replace("_", "");

                                    log.warn("Column {0} is numeric type {1} but received wildcard value ''{2}''. " +
                                                    "Removing all wildcards and using exact match with value ''{3}''",
                                            p.getName(), getSqlTypeName(p.getSqlType()), strValue, cleanValue);

                                    strValue = cleanValue;
                                }

                                try {
                                    value = convertToNumericType(strValue, p.getSqlType());
                                    log.ok("Converted string ''{0}'' to numeric type {1} for column {2}",
                                            strValue, getSqlTypeName(p.getSqlType()), p.getName());
                                } catch (NumberFormatException e) {
                                    log.error("Cannot convert ''{0}'' to numeric type for column {1}. Using string value.",
                                            strValue, p.getName());
                                    value = strValue;
                                }
                            }
                        }
                    }

                    if (newName != null && !newName.equals(oldName)) {
                        replacedParams.add(new SQLParam(newName, value, p.getSqlType(), p.getSqlTypeName()));
                    } else {
                        replacedParams.add(p);
                    }
                }
                params = replacedParams;

                where = new FilterWhereBuilder();
                boolean first = true;
                for (SQLParam p : params) {
                    if (!first) {
                        where.getWhere().append(" AND ");
                    }

                    String operator = detectOperatorFor(originalWhereSql, p.getName(), this.sqlHandler.getColumnAliasBySource());
                    if(isNumericType(p.getSqlType())) {operator = "=";}

                    Object v = p.getValue();
                    where.addBind(
                            new SQLParam(p.getName(), v, p.getSqlType(), p.getSqlTypeName()),
                            operator
                    );

                    first = false;
                }
            }
        }

        String sqlQuery = buildSqlQuery(where);

        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = getConn().prepareStatement(sqlQuery, params);
            result = stmt.executeQuery();
            log.ok("executeSqlQuery {0} on {1}", sqlQuery, oclass);
            while (result.next()) {
                final Map<String, SQLParam> columnValues = getConn().getColumnValues(result);
                log.ok("Column values {0} from result set ", columnValues);
                final ConnectorObjectBuilder bld = buildConnectorObject(oclass,columnValues);
                if (!handler.handle(bld.build())) {
                    log.ok("Stop processing of the result set");
                    break;
                }
            }
            log.info("commit executeSqlQuery");
            commit();

        } catch (SQLException e) {
            log.error(e, "sqlQuery {0} on {1} error", sqlQuery, oclass);

            SQLUtil.rollbackQuietly(getConn());
            evaluateAndHandleException(e, true, true, false, MSG_CAN_NOT_READ, this.sqlHandler.getColumns());
        } finally {
            IOUtil.quietClose(result);
            IOUtil.quietClose(stmt);
            closeConnection();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sync(ObjectClass oclass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
        log.info("check the ObjectClass and result handler");
        // Contract tests    
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }
        log.ok("The object class is ok");
        if (handler == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_RESULT_HANDLER_NULL));
        }
        log.ok("The result handles is not null");
        //check if password column is defined in the config

        // Names
        String tblname;
        String changeLogColumn;
        // Sets table name and change log column from config specified by connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")) {
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
            changeLogColumn = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getChangeLogColumn();
        } else if (Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
            tblname = this.sqlHandler.getTable();
            changeLogColumn = config.getChangeLogColumn();
        } else {
            tblname = config.getTable();
            changeLogColumn = config.getChangeLogColumn();
        }

        if (StringUtil.isBlank(changeLogColumn)) {
            throw new IllegalArgumentException(config.getMessage(MSG_CHANGELOG_COLUMN_BLANK));
        }
        log.ok("The change log column is ok");

        log.ok("Change log attribute {0} map to column name {1}", changeLogColumn, quoteName(changeLogColumn));

        final Set<String> columnNames = resolveColumnNamesToGet(oclass, options);
        log.ok("Column Names {0} To Get", columnNames);

        final List<OrderBy> orderBy = new ArrayList<>();
        //Add also the token column        
        columnNames.add(quoteName(changeLogColumn));

        //Set ORDER BY on Sync Data
        String syncOrderByColumnName = quoteName(changeLogColumn);
        if (StringUtil.isNotBlank(config.getSyncOrderColumn())) {
            syncOrderByColumnName = config.getSyncOrderColumn();
        } else if (Objects.equals(this.config.getConnectorMode(), "Json")) {
            if(StringUtil.isNotBlank(this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getSyncOrderColumn())) {
                syncOrderByColumnName = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getSyncOrderColumn();
            }
        }

        Boolean syncOrderByAsc = true;
        // Sets sync order by ascending from config specified by connector mode if it is not empty
        if (config.getSyncOrderAsc() != null) {
            syncOrderByAsc = config.getSyncOrderAsc();
        } else if(Objects.equals(this.config.getConnectorMode(), "Json")) {
            if(this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getSyncOrderAsc() != null) {
                syncOrderByAsc = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getSyncOrderAsc();
            }
        }

        orderBy.add(new OrderBy(syncOrderByColumnName, syncOrderByAsc));
        log.ok("OrderBy {0}", orderBy);

        // The first token is not null set the FilterWhereBuilder
        final FilterWhereBuilder where = new FilterWhereBuilder();
        if (token != null && token.getValue() != null) {
            final Object tokenVal = token.getValue();
            log.info("Sync token is {0}", tokenVal);
            final SQLColumnTypeInfo sqlColumnTypeinfo = getColumnTypeInfo(oclass.getObjectClassValue(), changeLogColumn);
            where.addBind(new SQLParam(quoteName(changeLogColumn), tokenVal, sqlColumnTypeinfo.getTypeCode(), sqlColumnTypeinfo.getTypeName()), ">");
        }

        DatabaseQueryBuilder query;
        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            openConnection();

            if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
                String sqlQuery = buildSqlQuery(where);
                statement = getConn().prepareStatement(sqlQuery, where.getParams());
                log.info("execute sync query {0} on {1}", sqlQuery, oclass);
            } else {
                query = new DatabaseQueryBuilder(tblname, columnNames);
                query.setWhere(where);
                query.setOrderBy(orderBy);
                statement = getConn().prepareStatement(query);
                log.info("execute sync query {0} on {1}", query.getSQL(), oclass);
            }

            result = statement.executeQuery();

            while (result.next()) {
                final Map<String, SQLParam> columnValues = getConn().getColumnValues(result);
                log.ok("Column values {0} from sync result set ", columnValues);

                // create the connector object..
                final SyncDeltaBuilder sdb = buildSyncDelta(oclass,columnValues);
                if (!handler.handle(sdb.build())) {
                    log.ok("Stop processing of the sync result set");
                    break;
                }
            }
            // commit changes
            log.info("commit sync account");
            commit();
        } catch (SQLException e) {

            SQLUtil.rollbackQuietly(getConn());
            evaluateAndHandleException(e, false, true, false, MSG_CAN_NOT_READ, tblname);
        } finally {
            IOUtil.quietClose(result);
            IOUtil.quietClose(statement);

            closeConnection();
        }
        log.ok("Sync Account committed");
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass oclass) {
        log.info("check the ObjectClass");
        final String SQL_SELECT = "SELECT MAX( {0} ) FROM {1}";
        // Contract tests    
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }
        log.ok("The object class is ok");

        // Format the update query
        String tblname;
        String chlogName;

        //check if password column is defined in the config and sets table name and isEnableEmptyString from config specified
        // by connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")) {
            if (StringUtil.isBlank(this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getChangeLogColumn())) {
                throw new IllegalArgumentException(config.getMessage(MSG_CHANGELOG_COLUMN_BLANK));
            }
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
            chlogName = quoteName(this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getChangeLogColumn());
        } else if (Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
            if (StringUtil.isBlank(config.getChangeLogColumn())) {
                throw new IllegalArgumentException(config.getMessage(MSG_CHANGELOG_COLUMN_BLANK));
            }
            tblname = this.sqlHandler.getTable();
            chlogName = quoteName(config.getChangeLogColumn());
        } else {
            if (StringUtil.isBlank(config.getChangeLogColumn())) {
                throw new IllegalArgumentException(config.getMessage(MSG_CHANGELOG_COLUMN_BLANK));
            }
            tblname = config.getTable();
            chlogName = quoteName(config.getChangeLogColumn());
        }
        log.ok("The change log column is ok");

        final String sql = MessageFormat.format(SQL_SELECT, chlogName, tblname);
        SyncToken ret = null;

        log.info("getLatestSyncToken on {0}", oclass);
        PreparedStatement stmt = null;
        ResultSet rset = null;
        try {
            openConnection();
            // create the prepared statement..
            stmt = getConn().getConnection().prepareStatement(sql);
            rset = stmt.executeQuery();
            log.ok("The statement {0} executed", sql);
            if (rset.next()) {
                Object value = rset.getObject(1);
                if (value != null) {
                    log.ok("New token value {0}", value);
                    ret = new SyncToken(SQLUtil.jdbc2AttributeValue(value));
                }
            }
            if (ret == null) {
                SQLColumnTypeInfo columnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), chlogName);
                ret = new SyncToken(SQLUtil.jdbc2AttributeValue(SQLUtil.getCurrentJdbcTime(columnTypeInfo.getTypeCode())));
            }
            log.ok("getLatestSyncToken", ret);
            // commit changes
            log.info("commit getLatestSyncToken");
            commit();
        } catch (SQLException e) {

            SQLUtil.rollbackQuietly(getConn());
            evaluateAndHandleException(e, false, true, false, MSG_CAN_NOT_READ, tblname);
        } finally {
            IOUtil.quietClose(rset);
            IOUtil.quietClose(stmt);

            closeConnection();
        }

        log.ok("getLatestSyncToken committed");
        return ret;
    }

    // =======================================================================
    // Schema..
    // =======================================================================

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        try {
            openConnection();
            if (schema == null) {
                log.info("cache schema");
                cacheSchema();
            }
            assert schema != null;
            commit();
        } catch (SQLException e) {

            evaluateAndHandleException(e, true, true, true, "error in schema");

        } finally {
            closeConnection();
        }
        log.ok("schema");
        return schema;
    }

    /**
     * Test the configuration and connection
     * {@inheritDoc}
     */
    public void test() {
        log.info("test");
        try {
            openConnection();
            DatabaseTableConnection connection = getConn();
            connection.test();
            commit();
        } catch (SQLException e) {
            log.error(e, "error in test");
            evaluateAndHandleException(e, true, true, true, "error in test");
        } finally {
            closeConnection();
        }
        log.ok("connector test ok");
    }

    private void closeConnection() {
        getConn().closeConnection();
    }

    private void openConnection() throws SQLException {
        getConn().openConnection();
    }

    private void commit() throws SQLException {
        getConn().getConnection().commit();
    }

    /**
     * Attempts to authenticate the given username combination
     * {@inheritDoc}
     */
    public Uid authenticate(ObjectClass oclass, String username, GuardedString password,
            OperationOptions options) {

        final String SQL_AUTH_QUERY = "SELECT {0} FROM {1} WHERE ( {0} = ? ) AND ( {2} = ? )";

        log.info("check the ObjectClass");
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }
        log.ok("The object class is ok");
        if (StringUtil.isBlank(config.getPasswordColumn())) {
            throw new UnsupportedOperationException(config.getMessage(MSG_AUTHENTICATE_OP_NOT_SUPPORTED));
        }
        log.ok("The Password Column is ok");
        // determine if you can get a connection to the database..
        if (StringUtil.isBlank(username)) {
            throw new IllegalArgumentException(config.getMessage(MSG_USER_BLANK));
        }
        log.ok("The username is ok");
        // check that there is a pwd to query..
        if (password == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_PASSWORD_BLANK));
        }
        log.ok("The password is ok");

        String keyCol;
        final String passwordColumnName = quoteName(config.getPasswordColumn());
        String tblname;
        // Sets table name and key column from config specified by connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")) {
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
            keyCol = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn();
        } else if (Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
            tblname = this.sqlHandler.getTable();
            keyCol = config.getKeyColumn();
        } else {
            tblname = config.getTable();
            keyCol = config.getKeyColumn();
        }

        String sql = MessageFormat.format(SQL_AUTH_QUERY, quoteName(keyCol), tblname, passwordColumnName);
        SQLColumnTypeInfo columnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), keyCol);
        final List<SQLParam> values = new ArrayList<>();
        values.add(new SQLParam(quoteName(keyCol), username, columnTypeInfo.getTypeCode(), columnTypeInfo.getTypeName())); // real username
        values.add(new SQLParam(passwordColumnName, password)); // real password

        PreparedStatement stmt = null;
        ResultSet result = null;
        Uid uid = null;
        //No passwordExpired capability
        try {
            // replace the ? in the SQL_AUTH statement with real data
            log.info("authenticate Account: {0}", username);
            openConnection();

            stmt = getConn().prepareStatement(sql, values);
            result = stmt.executeQuery();
            log.ok("authenticate query for account {0} executed ", username);
            //No PasswordExpired capability
            if (!result.next()) {
                log.error("authenticate query for account {0} has no result ", username);
                throw new InvalidCredentialException(config.getMessage(MSG_AUTH_FAILED, username));
            }
            uid = new Uid(result.getString(1));
            // commit changes
            log.info("commit authenticate");
            commit();
        } catch (SQLException e) {

            SQLUtil.rollbackQuietly(getConn());
            evaluateAndHandleException(e, false, true, false, MSG_CAN_NOT_READ, tblname);
        } finally {
            IOUtil.quietClose(result);
            IOUtil.quietClose(stmt);

            closeConnection();
        }
        log.info("Account: {0} authenticated ", username);
        return uid;
    }

    /**
     * Attempts to resolve the given username
     * {@inheritDoc}
     */
    public Uid resolveUsername(ObjectClass oclass, String username, OperationOptions options) {
        final String SQL_AUTH_QUERY = "SELECT {0} FROM {1} WHERE ( {0} = ? )";

        log.info("check the ObjectClass");
        if (oclass == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_ACCOUNT_OBJECT_CLASS_REQUIRED));
        }
        log.ok("The object class is ok");
        if (StringUtil.isBlank(config.getPasswordColumn())) {
            throw new UnsupportedOperationException(config.getMessage(MSG_AUTHENTICATE_OP_NOT_SUPPORTED));
        }
        log.ok("The Password Column is ok");
        // determine if you can get a connection to the database..
        if (StringUtil.isBlank(username)) {
            throw new IllegalArgumentException(config.getMessage(MSG_USER_BLANK));
        }
        log.ok("The username is ok");

        final String passwordColumnName = quoteName(config.getPasswordColumn());
        String keyCol;
        String tblname;

        // Sets table name and key column from config specified by connector mode
        if(Objects.equals(this.config.getConnectorMode(), "Json")) {
            tblname = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getTable();
            keyCol = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn();
        } else if (Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
            tblname = this.sqlHandler.getTable();
            keyCol = config.getKeyColumn();
        } else {
            tblname = config.getTable();
            keyCol = config.getKeyColumn();
        }

        String sql = MessageFormat.format(SQL_AUTH_QUERY, quoteName(keyCol), tblname, passwordColumnName);

        final List<SQLParam> values = new ArrayList<>();
        SQLColumnTypeInfo columnTypeInfo = getColumnTypeInfo(oclass.getObjectClassValue(), keyCol);
        values.add(new SQLParam(quoteName(keyCol), username, columnTypeInfo.getTypeCode(), columnTypeInfo.getTypeName())); // real username

        PreparedStatement stmt = null;
        ResultSet result = null;
        Uid uid = null;
        //No passwordExpired capability
        try {
            // replace the ? in the SQL_AUTH statement with real data
            log.info("authenticate Account: {0}", username);
            openConnection();

            stmt = getConn().prepareStatement(sql, values);
            result = stmt.executeQuery();
            log.ok("authenticate query for account {0} executed ", username);
            //No PasswordExpired capability
            if (!result.next()) {
                log.error("authenticate query for account {0} has no result ", username);
                throw new InvalidCredentialException(config.getMessage(MSG_AUTH_FAILED, username));
            }
            uid = new Uid(result.getString(1));
            // commit changes
            log.info("commit authenticate");
            commit();
        } catch (SQLException e) {

            SQLUtil.rollbackQuietly(getConn());
            evaluateAndHandleException(e, false, true, false, MSG_CAN_NOT_READ, tblname);
        } finally {
            IOUtil.quietClose(result);
            IOUtil.quietClose(stmt);

            closeConnection();
        }
        log.info("Account: {0} authenticated ", username);
        return uid;
    }

    /**
     * Used to escape the table or column name.
     *
     * @param value Value to be quoted
     * @return the quoted column name
     */
    public String quoteName(String value) {
        return DatabaseTableSQLUtil.quoteName(config.getQuoting(), value);
    }

    /**
     * The required type is cached
     *
     * @param columnName the column name
     * @return the cached column type
     */
    public SQLColumnTypeInfo getColumnTypeInfo(String objectClass, String columnName) {
        if (columnSQLTypes == null) {
            cacheSchema();
        } else if (Objects.equals(this.config.getConnectorMode(), "Json")) {
            if(columnSQLTypesCollection.size() < this.universalObjectClassHandlers.size()) {
                cacheSchema();
            }
        }
        // no null here :)
        assert columnSQLTypesCollection != null;
        assert columnSQLTypesCollection.get(objectClass) != null;

        return columnSQLTypesCollection.get(objectClass).get(columnName);
    }

    /**
     * Convert the attribute name to resource specific columnName
     *
     * @return the Column Name value
     */
    public String getColumnName(ObjectClass oclass, String attributeName) {
        if (Name.NAME.equalsIgnoreCase(attributeName)) {
            log.ok("attribute name {0} map to key column", attributeName);
            if(Objects.equals(this.config.getConnectorMode(), "Json")) {
                return this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn();
            } else {
                return config.getKeyColumn();
            }
        }
        if (Uid.NAME.equalsIgnoreCase(attributeName)) {
            log.ok("attribute name {0} map to key column", attributeName);
            if(Objects.equals(this.config.getConnectorMode(), "Json")) {
                return this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn();
            } else {
                return config.getKeyColumn();
            }
        }
        if (!StringUtil.isBlank(config.getPasswordColumn())
                && OperationalAttributes.PASSWORD_NAME.equalsIgnoreCase(attributeName)) {
            log.ok("attribute name {0} map to password column", attributeName);
            return config.getPasswordColumn();
        }
        return attributeName;
    }

    /**
     * Cache schema, defaultAtributesToGet, columnClassNamens
     */
    private void cacheSchema() {
        final SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        defaultAttributesToGet = new HashMap<>();
        if (universalObjectClassHandlers != null) {
            for (String oClass : universalObjectClassHandlers.keySet()) {
                final Set<AttributeInfo> attrInfoSet = buildSelectBasedAttributeInfos(oClass, universalObjectClassHandlers.get(oClass).getTable(), universalObjectClassHandlers.get(oClass).getKeyColumn());

                log.info("cacheSchemaNew");
                // Cache the attributes to get

                Set<String> attrs = new HashSet<>();
                for (AttributeInfo info : attrInfoSet) {
                    if (info.isReturnedByDefault()) {
                        attrs.add(info.getName());
                    }
                }
                defaultAttributesToGet.put(oClass, attrs);

                /*
                 * Add any other operational attributes to the attrInfoSet
                 */
                // attrInfoSet.add(OperationalAttributeInfos.ENABLE);

                /*
                 * Use SchemaBuilder to build the schema. Currently, only ACCOUNT type is supported.
                 */

                final ObjectClassInfoBuilder ociB = new ObjectClassInfoBuilder();
                ociB.setType(oClass);
                ociB.addAllAttributeInfo(attrInfoSet);

                final ObjectClassInfo oci = ociB.build();
                schemaBld.defineObjectClass(oci);

                /*
                 * Note: AuthenticateOp, and all the 'SPIOperation'-s are by default added by Reflection API to the Schema.
                 *
                 * See for details: SchemaBuilder.defineObjectClass() --> FrameworkUtil.getDefaultSupportedOperations()
                 * ReflectionUtil.getAllInterfaces(connector); is the line that *does* acquire the implemented interfaces by the
                 * connector class.
                 */
                if (StringUtil.isBlank(config.getPasswordColumn())) { // remove the AuthenticateOp
                    log.info("no password column, remove the AuthenticateOp");
                    schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oci);
                }

                if (StringUtil.isBlank(config.getChangeLogColumn())) { // remove the SyncOp
                    log.info("no changeLog column, remove the SyncOp");
                    schemaBld.removeSupportedObjectClass(SyncOp.class, oci);
                }
            }
        } else {
            /*
             * First, compute the account attributes based on the database schema
             */
            String table = config.getTable();
            String keyCol = config.getKeyColumn();

            Set<AttributeInfo> attrInfoSet;

            if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
                attrInfoSet = buildSQLSelectBasedAttributeInfos(ObjectClass.ACCOUNT_NAME,
                        this.sqlHandler.getColumnSchemaFormat(),
                        this.sqlHandler.getSqlQuery());
            } else {
                attrInfoSet = buildSelectBasedAttributeInfos(ObjectClass.ACCOUNT_NAME, table,keyCol);
            }

            log.info("cacheSchema");
            // Cache the attributes to get
            Set<String> attrs = new HashSet<>();
            for (AttributeInfo info : attrInfoSet) {
                if (info.isReturnedByDefault()) {
                    attrs.add(info.getName());
                }
            }
            defaultAttributesToGet.put(ObjectClass.ACCOUNT_NAME,attrs);

            /*
             * Add any other operational attributes to the attrInfoSet
             */
            // attrInfoSet.add(OperationalAttributeInfos.ENABLE);


            final ObjectClassInfoBuilder ociB = new ObjectClassInfoBuilder();
            ociB.setType(ObjectClass.ACCOUNT_NAME);
            ociB.addAllAttributeInfo(attrInfoSet);

            final ObjectClassInfo oci = ociB.build();
            schemaBld.defineObjectClass(oci);

            /*
             * Note: AuthenticateOp, and all the 'SPIOperation'-s are by default added by Reflection API to the Schema.
             *
             * See for details: SchemaBuilder.defineObjectClass() --> FrameworkUtil.getDefaultSupportedOperations()
             * ReflectionUtil.getAllInterfaces(connector); is the line that *does* acquire the implemented interfaces by the
             * connector class.
             */
            if (StringUtil.isBlank(config.getPasswordColumn())) { // remove the AuthenticateOp
                log.info("no password column, remove the AuthenticateOp");
                schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oci);
            }

            if (StringUtil.isBlank(config.getChangeLogColumn())) { // remove the SyncOp
                log.info("no changeLog column, remove the SyncOp");
                schemaBld.removeSupportedObjectClass(SyncOp.class, oci);
            }
        }
        schema = schemaBld.build();
        log.ok("schema builded");
    }

    private Set<AttributeInfo> buildSQLSelectBasedAttributeInfos(String oClass,
                                                                 Map<String, String> columnSchemaFormat,
                                                                 String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("sql must not be null");
        }
        if (columnSchemaFormat == null || columnSchemaFormat.isEmpty()) {
            throw new IllegalArgumentException("columnSchemaFormat must not be null or empty (alias -> table.column)");
        }

        String core = sql.trim();
        if (core.endsWith(";")) {
            core = core.substring(0, core.length() - 1);
        }

        StringBuilder projection = new StringBuilder();
        int i = 0;
        for (String alias : columnSchemaFormat.keySet()) {
            if (alias == null || alias.isEmpty()) {
                continue;
            }
            if (i++ > 0) {
                projection.append(", ");
            }
            projection.append("__src.").append(quoteName(alias))
                    .append(" AS ").append(quoteName(alias));
        }

        if (projection.length() == 0) {
            throw new IllegalArgumentException("columnSchemaFormat has no usable aliases.");
        }

        String wrapped = "SELECT " + projection + " FROM (" + core + ") AS __src WHERE 1=0";

        Set<AttributeInfo> attrInfo;
        ResultSet rset = null;
        Statement stmt = null;
        try {
            stmt = getConn().getConnection().createStatement();
            log.info("executeQuery ''{0}''", wrapped);
            rset = stmt.executeQuery(wrapped);
            log.ok("query executed");
            attrInfo = buildAttributeInfoSet(oClass, rset);
            log.info("commit get schema");
            commit();
        } catch (SQLException ex) {
            log.error(ex, "buildSelectBasedAttributeInfo in SQL: ''{0}''", wrapped);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(config.getMessage(MSG_CAN_NOT_READ, config.getTable()), ex);
        } finally {
            IOUtil.quietClose(rset);
            IOUtil.quietClose(stmt);
        }
        log.ok("schema created");
        return attrInfo;
    }


    /**
     * Get the schema using a SELECT query.
     *
     * @return Schema based on a empty SELECT query.
     */
    private Set<AttributeInfo> buildSelectBasedAttributeInfos(String oClass, String table, String keyColumn) {
        // Template for a empty query to get the columns of the table.
        final String schemaQuery = "SELECT * FROM {0} WHERE {1} IS NULL";

        log.info("get schema from the table");
        Set<AttributeInfo> attrInfo;
        String sql = MessageFormat.format(schemaQuery, table, quoteName(keyColumn));

        // check out the result etc..
        ResultSet rset = null;
        Statement stmt = null;
        try {
            // create the query..
            stmt = getConn().getConnection().createStatement();

            log.info("executeQuery ''{0}''", sql);
            rset = stmt.executeQuery(sql);
            log.ok("query executed");
            // get the results queued..
            attrInfo = buildAttributeInfoSet(oClass, rset);
            // commit changes
            log.info("commit get schema");
            commit();
        } catch (SQLException ex) {
            log.error(ex, "buildSelectBasedAttributeInfo in SQL: ''{0}''", sql);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(config.getMessage(MSG_CAN_NOT_READ, config.getTable()), ex);
        } finally {
            IOUtil.quietClose(rset);
            IOUtil.quietClose(stmt);
        }
        log.ok("schema created");
        return attrInfo;
    }

    /**
     * Return the set of AttributeInfo based on the database query meta-data.
     */
    private Set<AttributeInfo> buildAttributeInfoSet(String oClass, ResultSet rset) throws SQLException {
        log.info("build AttributeInfoSet");

        if(columnSQLTypesCollection == null) {
            columnSQLTypesCollection = CollectionUtil.newCaseInsensitiveMap();
        }

        Set<AttributeInfo> attrInfo = new HashSet<>();
        columnSQLTypes = CollectionUtil.newCaseInsensitiveMap();
        stringColumnRequired = CollectionUtil.newCaseInsensitiveSet();
        ResultSetMetaData meta = rset.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            final String name = meta.getColumnName(i);

            log.ok("Name of the parameter being evaluated : {0}", name);

            AttributeInfoBuilder attrBld = new AttributeInfoBuilder();
            Integer columnType = meta.getColumnType(i);
            String columnTypeName = meta.getColumnTypeName(i);

            if(meta.getColumnType(i) == 5) {
                columnType = 4;
                columnTypeName = "int";
            }

            boolean attributeFound = false;
            columnSQLTypes.put(name, new SQLColumnTypeInfo(columnTypeName, columnType));
            MappingStrategy msTmp;

            if(Objects.equals(this.config.getConnectorMode(), "Json")) {
                if (name.equalsIgnoreCase(this.universalObjectClassHandlers.get(oClass).getKeyColumn())) {
                    // name attribute
                    attrBld.setName(Name.NAME);
                    //The generate UID make the Name attribute is nor required
                    attrBld.setRequired(true);
                    attrInfo.add(attrBld.build());
                    attrBld = new AttributeInfoBuilder();
                    attrBld.setName(name);
                    attrInfo.add(attrBld.build());
                    log.ok("key column in name attribute in the schema");
                    attributeFound = true;
                } else if (name.equalsIgnoreCase(this.universalObjectClassHandlers.get(oClass).getPasswordColumn())) {
                    // Password attribute
                    attrInfo.add(OperationalAttributeInfos.PASSWORD);
                    log.ok("password column in password attribute in the schema");
                    attributeFound = true;
                } else if (name.equalsIgnoreCase(this.universalObjectClassHandlers.get(oClass).getChangeLogColumn())) {
                    // skip changelog column from the schema. It is not part of the contract
                    log.ok("skip changelog column from the schema");
                }
                msTmp = conn.createMappingStrategy(conn.getConnection(), this.universalObjectClassHandlers.get(oClass));
            } else {
                if (name.equalsIgnoreCase(config.getKeyColumn())) {
                    // name attribute
                    attrBld.setName(Name.NAME);
                    //The generate UID make the Name attribute is nor required
                    attrBld.setRequired(true);
                    if (Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
                        attrBld.setUpdateable(false);
                        attrBld.setCreateable(false);
                    }
                    attrInfo.add(attrBld.build());
                    attrBld = new AttributeInfoBuilder();
                    if (this.sqlHandler.getColumnAliasBySource().containsKey(name)){
                        String newName = this.sqlHandler.getColumnAliasBySource().get(name);
                        attrBld.setName(newName);
                    }else {
                        attrBld.setName(name);
                    }
                    attrBld.setReadable(false);
                    attrBld.setReturnedByDefault(false);
                    attrInfo.add(attrBld.build());
                    log.ok("key column in name attribute in the schema");
                    attributeFound = true;
                } else if (name.equalsIgnoreCase(config.getPasswordColumn())) {
                    // Password attribute
                    attrInfo.add(OperationalAttributeInfos.PASSWORD);
                    log.ok("password column in password attribute in the schema");
                    attributeFound = true;
                } else if (name.equalsIgnoreCase(config.getChangeLogColumn())) {
                    // skip changelog column from the schema. It is not part of the contract
                    log.ok("skip changelog column from the schema");
                }
                msTmp = getConn().getSms();
            }

            if (!attributeFound) {
                // All other attributed taken from the table
                log.ok("Building attribute info set for standard attribute. ");

                log.ok("Datatype fetch finished, used strategy : {0}", msTmp.getClass());
                Class<?> dataType = msTmp.getSQLAttributeType(columnType, columnTypeName);

                log.ok("Datatype to be used : {0}", dataType);

                if (!UUID.class.equals(dataType)) {

                    log.ok("UUID datatype handled in schema builder as String.class for schema definition purposes ", name);
                    attrBld.setType(dataType);
                } else {

                    attrBld.setType(String.class);
                }

                attrBld.setName(name);
                final boolean required = meta.isNullable(i) == ResultSetMetaData.columnNoNulls;
                attrBld.setRequired(required);

                if (required && dataType.isAssignableFrom(String.class)) {
                    log.ok("the column name {0} is string type and required", name);
                    stringColumnRequired.add(name);
                }

                if(Objects.equals(this.config.getConnectorMode(), "Custom_SQL_Query")) {
                    attrBld.setUpdateable(false);
                    attrBld.setCreateable(false);
                }

                attrBld.setReturnedByDefault(isReturnedByDefault(dataType));
                attrInfo.add(attrBld.build());
                log.ok("the column name {0} has data type {1}", name, dataType);
            }
        }
        columnSQLTypesCollection.put(oClass, columnSQLTypes);
        log.ok("the Attribute InfoSet is done");
        return attrInfo;
    }

    /**
     * Decide if it should be returned by default.
     * Generally all byte arrays are not returned by default.
     *
     * @param dataType the type of the attribute type
     */
    private boolean isReturnedByDefault(final Class<?> dataType) {
        return !(byte[].class.equals(dataType));
    }

    /**
     * Construct a connector object
     * <p>Taking care about special attributes</p>
     *
     * @param columnValues from the result set
     * @return ConnectorObjectBuilder object
     */
    private ConnectorObjectBuilder buildConnectorObject(ObjectClass oclass, Map<String, SQLParam> columnValues) {
        log.info("build ConnectorObject");
        String uidValue = null;
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        for (Map.Entry<String, SQLParam> colValue : columnValues.entrySet()) {
            String columnName = colValue.getKey();
            final SQLParam param = colValue.getValue();

            // Map the special
            String keyCol;
            String changeLogCol;
            // Sets table name and change log column from config specified by connector mode
            if(Objects.equals(this.config.getConnectorMode(), "Json")) {
                keyCol = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getKeyColumn();
                changeLogCol = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getChangeLogColumn();
            } else {
                keyCol = this.config.getKeyColumn();
                changeLogCol = this.config.getChangeLogColumn();
            }
            if(!this.sqlHandler.getColumnAliasBySource().isEmpty() && this.sqlHandler.getColumnAliasBySource().containsKey(columnName)) {
                columnName = this.sqlHandler.getColumnAliasBySource().get(columnName);
            }
            if (columnName.equalsIgnoreCase(keyCol)) {
                if (param == null || param.getValue() == null) {
                    log.error("Name cannot be null.");
                    String msg = "Name cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                uidValue = param.getValue().toString();
                bld.setName(uidValue);
            } else if (columnName.equalsIgnoreCase(config.getPasswordColumn())) {
                if (config.getSuppressPassword()) {
                    // No Password in the result object
                    log.ok("Password is suppressed in the result object");
                } else {
                    GuardedString passwordValue = null;
                    if (param != null && param.getValue() != null) {
                        passwordValue = new GuardedString(((String) param.getValue()).toCharArray());
                    }
                    if (passwordValue != null) {
                        bld.addAttribute(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, passwordValue));
                    } else {
                        bld.addAttribute(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME));
                    }
                }
            } else if (columnName.equalsIgnoreCase(changeLogCol)) {
                //No changelogcolumn attribute in the results
                log.ok("changelogcolumn attribute in the result");
            } else {
                if (param != null && param.getValue() != null) {
                    Object paramValue = param.getValue();

                    if (!(paramValue instanceof UUID)) {

                        bld.addAttribute(AttributeBuilder.build(columnName, paramValue));
                    } else {

                        log.ok("Column with the name: {0} with the UUID datatype will be handled as a String.class type" +
                                "Object for the purpose of Connector object translation", columnName);
                        bld.addAttribute(AttributeBuilder.build(columnName, paramValue.toString()));
                    }

                } else {
                    bld.addAttribute(AttributeBuilder.build(columnName));
                }
            }
        }

        // To be sure that uid and name are present for mysql
        if (uidValue == null) {
            final String msg = "The uid value is missing in query.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        // Add Uid attribute to object
        bld.setUid(new Uid(uidValue));
        // only deals w/ accounts..
        bld.setObjectClass(oclass);
        log.ok("ConnectorObject is builded");
        return bld;
    }

    /**
     * Construct a SyncDeltaBuilder the sync builder
     * <p>Taking care about special attributes</p>
     *
     * @param oclass
     * @param columnValues from the resultSet
     * @return SyncDeltaBuilder the sync builder
     */
    private SyncDeltaBuilder buildSyncDelta(ObjectClass oclass, Map<String, SQLParam> columnValues) {
        log.info("buildSyncDelta");
        SyncDeltaBuilder bld = new SyncDeltaBuilder();
        // Find a token
        String changeLogCol;
        if(Objects.equals(config.getConnectorMode(), "Json")) {
            changeLogCol = this.universalObjectClassHandlers.get(oclass.getObjectClassValue()).getChangeLogColumn();
        } else {
            changeLogCol = config.getChangeLogColumn();
        }
        SQLParam tokenParam = columnValues.get(changeLogCol);

        if (tokenParam == null) {
            throw new IllegalArgumentException(config.getMessage(MSG_INVALID_SYNC_TOKEN_VALUE));
        }
        Object token = tokenParam.getValue();
        // Null token, set some acceptable value
        if (token == null) {
            log.ok("token value is null, replacing to 0L");
            token = 0L;
        }

        // To be sure that sync token is present
        bld.setToken(new SyncToken(token));
        bld.setObject(buildConnectorObject(oclass,columnValues).build());

        // only deals w/ updates
        bld.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
        log.ok("SyncDeltaBuilder is ok");
        return bld;
    }

    private Set<String> resolveColumnNamesToGet(ObjectClass oclass, OperationOptions options) {
        Set<String> attributesToGet = getDefaultAttributesToGet(oclass);
        if (options != null && options.getAttributesToGet() != null) {
            attributesToGet = CollectionUtil.newSet(options.getAttributesToGet());
            attributesToGet.add(Uid.NAME); // Ensure the Uid colum is there
        }
        // Replace attributes to quoted columnNames
        Set<String> columnNamesToGet = new HashSet<>();
        for (String attributeName : attributesToGet) {
            final String columnName = getColumnName(oclass, attributeName);
            columnNamesToGet.add(quoteName(columnName));
        }
        return columnNamesToGet;
    }

    /**
     * Get the default Attributes to get
     *
     * @return the Set of default attribute names
     */
    private Set<String> getDefaultAttributesToGet(ObjectClass oclass) {
        if (defaultAttributesToGet.get(oclass.getObjectClassValue()) == null) {
            cacheSchema();
        }
        assert defaultAttributesToGet != null;
        return defaultAttributesToGet.get(oclass.getObjectClassValue());
    }

    /**
     * Get the default Attributes to get
     *
     * @return the Set of default attribute names
     */
    private Set<String> getStringColumnReguired() {
        if (stringColumnRequired == null) {
            cacheSchema();
        }
        assert stringColumnRequired != null;
        return stringColumnRequired;
    }

    /**
     * Analyses the provided exception to evaluate if special handling is needed and throws
     * an respective exception
     * {@inheritDoc}
     */
    private void evaluateAndHandleException(Exception e, Boolean checkIfRethrow, Boolean logErr, Boolean wrap,
            String message, Object... messageParameters) {
        boolean throwDefault = true;

        if (e instanceof SQLException) {
            throwDefault = false;

            if (config.getSQLStateExceptionHandling()) {

                handleBasedOnSQLState((SQLException) e, logErr, checkIfRethrow, wrap, message, messageParameters);

            } else if (isConfiguredAlreadyExistsException((SQLException) e)) {

                log.ok("Default exception handling for AlreadyExistsException in case of SQLException.");
                evaluateAndThrow(new AlreadyExistsException(e), (SQLException) e, checkIfRethrow, logErr,
                        config.getMessage(MSG_OP_ALREADY_EXISTS, messageParameters));
            }

            if (!wrap) {

                log.ok("Default exception handling for ConnectorException in case of SQLException.");
                evaluateAndThrow(new ConnectorException(e), (SQLException) e, checkIfRethrow, logErr,
                        config.getMessage(MSG_EXP_DEFAULT, messageParameters));
            } else {

                log.ok("Default exception handling for ConnectorException in case of SQLException, wrapping original sqlError.");
                evaluateAndThrow(ConnectorException.wrap(e), (SQLException) e, checkIfRethrow, logErr,
                        config.getMessage(MSG_EXP_DEFAULT, messageParameters));
            }
        }

        if (throwDefault) {

            log.ok("Default exception handling for ConnectorException.");
            log.error(config.getMessage(message, messageParameters));
            throw new ConnectorException(e);
        }
    }

    /**
     * Exception evaluated based on a set of well known SQLState codes
     * {@inheritDoc}
     */
    private void handleBasedOnSQLState(SQLException e, Boolean logErr, Boolean checkIfRethrow, Boolean wrap,
            String message, Object... messageParameters) {
        String sqlState = e.getSQLState();

        String[] connectionFailedSqlStates = config.getSQLStateConnectionFailed();
        String[] alreadyExistsSqlStates = config.getSQLStateAlreadyExists();
        String[] invalidAttributeValueSqlStates = config.getSQLStateInvalidAttributeValue();
        String[] configurationExceptionSqlStates = config.getSQLStateConfigurationException();

        if (sqlState != null) {

            log.info("The SQLSTATE code of the processed SQL exception: {0}", sqlState);

            if (connectionFailedSqlStates != null && Arrays.asList(connectionFailedSqlStates).contains(sqlState)) {

                log.ok("sqlState exception handling for ConnectionFailedException.");
                evaluateAndThrow(new ConnectionFailedException(e), e, checkIfRethrow,
                        logErr, config.getMessage(message, messageParameters));
                return;
            } else if (alreadyExistsSqlStates != null && Arrays.asList(alreadyExistsSqlStates).contains(sqlState)) {

                log.ok("sqlState exception handling for AlreadyExistsException.");
                evaluateAndThrow(new AlreadyExistsException(e), e, checkIfRethrow,
                        logErr, config.getMessage(MSG_OP_ALREADY_EXISTS, messageParameters));
                return;
            } else if (invalidAttributeValueSqlStates != null && Arrays.asList(invalidAttributeValueSqlStates)
                    .contains(sqlState)) {

                log.ok("sqlState exception handling for InvalidAttributeValueException.");
                evaluateAndThrow(new InvalidAttributeValueException(e), e, checkIfRethrow,
                        logErr, config.getMessage(message, messageParameters));
                return;
            } else if (configurationExceptionSqlStates != null && Arrays.asList(configurationExceptionSqlStates)
                    .contains(sqlState)) {

                log.ok("sqlState exception handling for ConfigurationException.");
                evaluateAndThrow(new ConfigurationException(e), e, checkIfRethrow,
                        logErr, config.getMessage(message, messageParameters));
                return;
            }
        }

        // DEFAULT
        log.warn("Sql state code either null or not matched. Executing default exception handling.");

        if (!wrap) {

            log.ok("Default sqlState exception handling for ConnectorException");
            evaluateAndThrow(new ConnectorException(e), e, checkIfRethrow, logErr,
                    config.getMessage(MSG_EXP_DEFAULT, messageParameters));
        } else {

            log.ok("Default sqlState exception handling for ConnectorException, wrapping original sqlError.");
            evaluateAndThrow(ConnectorException.wrap(e), e, checkIfRethrow, logErr,
                    config.getMessage(MSG_EXP_DEFAULT, messageParameters));
        }

    }

    private boolean isConfiguredAlreadyExistsException(SQLException ex) {
        if (ex == null || ex.getMessage() == null || config.getAlreadyExistMessages() == null) {
            return false;
        }
        String[] messages = config.getAlreadyExistMessages().split(",");
        for (String msg : messages) {
            if (ex.getMessage().contains(msg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * UnknownUid exception handling
     * {@inheritDoc}
     */
    private void handleUnknownUid(String message, Object... attributes) {

        log.error(config.getMessage(MSG_CAN_NOT_UPDATE, attributes));
        throw new UnknownUidException(config.getMessage(message, attributes));
    }

    private Boolean evaluateAndThrow(RuntimeException exceptionToThrow, SQLException exceptionToConsume, Boolean checkIfRethrow,
            Boolean logErr, String errMessage) {

        if (!logErr) {

            log.ok(exceptionToConsume, errMessage);

        } else {
            log.error(exceptionToConsume, errMessage);
        }

        if (checkIfRethrow) {
            if (throwIt(exceptionToConsume.getErrorCode())) {
                SQLUtil.rollbackQuietly(getConn());
                throw exceptionToThrow;
            }
        } else {

            throw exceptionToThrow;
        }

        return true;
    }

    @Override
    public void testPartialConfiguration() {
        log.info("test partial configuration");
        DatabaseTableConnection connection = null;
        try {
            this.config.setValidationOnlyConnection();
            this.config.validate();
            this.config.setValidationFull();
            connection = DatabaseTableConnection.createDBTableConnection(this.config);
            connection.openConnection();
            connection.testByDriver();
            connection.commit();
        } catch (SQLException e) {
            log.error(e, "error in test partial configuration");
            evaluateAndHandleException(e, true, true, true, "error in test partial configuration");
        } finally {
            if (connection != null) {
                connection.closeConnection();
            }
        }
        log.ok("test partial configuration ok");
    }

    @Override
    public Map<String, SuggestedValues> discoverConfiguration() {
        List<String> dbTableNameSuggestions = new ArrayList<>();
        List<String> nameColumnSuggestions = new ArrayList<>();

        this.config.setValidationOnlyConnection();
        this.config.validate();
        this.config.setValidationFull();

        DatabaseTableConnection connection = null;
        try {
            connection = DatabaseTableConnection.createDBTableConnection(this.config);

            List<String> tableNames = getTableNamesContainingWord(Arrays.asList("account", "user"), connection);

            for (String tableName : tableNames) {
                selectTableWithPossibleSuggestions(
                        connection,
                        tableName,
                        dbTableNameSuggestions,
                        nameColumnSuggestions);
            }
        } finally {
            if (connection != null) {
                connection.closeConnection();
            }
        }

        Map<String, SuggestedValues> suggestions = new HashMap<>();

        createSuggestions(suggestions, "table", dbTableNameSuggestions);

        createSuggestions(suggestions, "keyColumn", nameColumnSuggestions);

        createSuggestions(suggestions, "passwordColumn", nameColumnSuggestions);

        return suggestions;
    }

    private List<String> getTableNamesContainingWord(List<String> expectedNames, DatabaseConnection connection) {
        List<String> tableNames = new ArrayList<>();
        ResultSet tables = null;
        try {
            String[] types = { "TABLE" };

            DatabaseMetaData meta = connection.getConnection().getMetaData();
            tables = meta.getTables(null, null, "%", types);

            List<String> allTableNames = new ArrayList<>();

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                allTableNames.add(tableName);
            }
            if (allTableNames.size() > 1) {
                allTableNames.forEach(tableName -> {
                    for (String expectedName : expectedNames) {
                        if (tableName.toLowerCase().contains(expectedName)) {
                            tableNames.add(tableName);
                            break;
                        }
                    }
                });
            } else {
                tableNames.addAll(allTableNames);
            }

            // commit changes
            log.info("commit get suggestions");
            connection.commit();
        } catch (SQLException ex) {
            SQLUtil.rollbackQuietly(connection);
        } finally {
            IOUtil.quietClose(tables);
        }
        return tableNames;
    }

    private void createSuggestions(Map<String, SuggestedValues> suggestions, String suggestionName, List<String> suggestionsValue) {
        if (!suggestionsValue.isEmpty()) {
            suggestions.put(suggestionName, SuggestedValuesBuilder.buildOpen(suggestionsValue.toArray()));
        }
    }

    private void selectTableWithPossibleSuggestions(DatabaseTableConnection connection,
                                                    String dbTableName,
                                                    List<String> dbTableNameSuggestions,
                                                    List<String> nameColumnSuggestions) {
        final String SCHEMA_QUERY = "SELECT * FROM {0} WHERE 0 = 1";

        log.info("get Suggestions from the table {0}", dbTableName);
        String sql = MessageFormat.format(SCHEMA_QUERY, dbTableName);
        // check out the result etc.
        ResultSet rset = null;
        Statement stmt = null;
        try {
            // create the query.
            connection = DatabaseTableConnection.createDBTableConnection(this.config);
            stmt = connection.getConnection().createStatement();

            log.info("executeQuery ''{0}''", sql);
            rset = stmt.executeQuery(sql);
            log.ok("query executed");
            // get the results queued.

            dbTableNameSuggestions.add(dbTableName);

            ResultSetMetaData meta = rset.getMetaData();
            int count = meta.getColumnCount();
            for (int i = 1; i <= count; i++) {
                final String name = meta.getColumnName(i);
                if (!nameColumnSuggestions.contains(name)) {
                    nameColumnSuggestions.add(name);
                }
            }

            // commit changes
            log.info("commit get suggestions");
            connection.commit();
        } catch (SQLException ex) {
            SQLUtil.rollbackQuietly(connection);
        } finally {
            IOUtil.quietClose(rset);
            IOUtil.quietClose(stmt);
        }
        log.ok("suggestions created");
    }
}
