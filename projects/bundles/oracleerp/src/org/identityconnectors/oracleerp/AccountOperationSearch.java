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
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.SearchOp;

/**
 * The Account CreateOp implementation of the SPI Select attributes from fnd_user table, add person details from
 * PER_PEOPLE_F table add responsibility names add auditor data add securing attributes all filtered according
 * attributes to get
 * 
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountOperationSearch extends Operation implements SearchOp<FilterWhereBuilder> {

    /** Setup logging. */
    static final Log log = Log.getLog(AccountOperationSearch.class);

    /** Name translation */
    private NameResolver nr = new AccountNameResolver();

    
    /** ResOps */
    private ResponsibilitiesOperations respOps;

    /** Audit Operations */
    private AuditorOperations auditOps;
    
    /** Securing Attributes Operations */
    private SecuringAttributesOperations secAttrOps;
    
    /**
     * @param conn
     * @param cfg
     */
    protected AccountOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
        respOps = new ResponsibilitiesOperations(conn, cfg);
        auditOps = new AuditorOperations(conn, cfg);
        secAttrOps = new SecuringAttributesOperations(conn, cfg);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, AccountOperations.FND_USER_COLS, nr);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        final String method = "executeQuery";

        final String tblname = cfg.app() + "fnd_user";
        final Set<AttributeInfo> ais = getAttributeInfos(cfg.getSchema(), ObjectClass.ACCOUNT_NAME);
        final Set<String> attributesToGet = getAttributesToGet(options, ais);
        final Set<String> fndUserColumnNames = getColumnNamesToGet(attributesToGet);
        //We always wont to have user id and user name
        fndUserColumnNames.add(USER_NAME); //User id
        fndUserColumnNames.add(START_DATE); //Enable Date
        fndUserColumnNames.add(END_DATE); // Disable date
        fndUserColumnNames.add(PWD_ACCESSES_LEFT); // Disable date
        fndUserColumnNames.add(PWD_DATE); // Last logon date
        fndUserColumnNames.add(PWD_LIFESPAN_DAYS); // Alloved days from last logon
        
        final Set<String> perPeopleColumnNames = CollectionUtil.newSet(fndUserColumnNames);
        final String filterId = getFilterId(where);

        fndUserColumnNames.retainAll(AccountOperations.FND_USER_COLS);
        perPeopleColumnNames.retainAll(AccountOperations.PER_PEOPLE_COLS);

        // For all user query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, fndUserColumnNames);
        String sqlSelect = query.getSQL();

        if (StringUtil.isNotBlank(cfg.getAccountsIncluded())) {
            sqlSelect += whereAnd(sqlSelect, cfg.getAccountsIncluded());
        } else if (cfg.isActiveAccountsOnly()) {
            sqlSelect += whereAnd(sqlSelect, ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE);
        }

        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                AttributeMergeBuilder amb = new AttributeMergeBuilder(attributesToGet);
                final Map<String, SQLParam> columnValues = SQLUtil.getColumnValues(result);
                final SQLParam userNameParm = columnValues.get(USER_NAME);
                final String userName = (String) userNameParm.getValue();
                final boolean getAuditorData = userNameParm.getValue().toString().equals(filterId);
                // get users account attributes
                merrgeAllAttributes(amb, columnValues);

                // if person_id not null and employee_number in schema, return employee_number
                buildPersonDetails(amb, columnValues, perPeopleColumnNames);

                // get users responsibilities only if if resp || direct_resp in account attribute
                buildResponsibilitiesToAccountObject(amb, userName);
                // get user's securing attributes
                buildSecuringAttributesToAccountObject(amb, userName);

                //Auditor data for get user only
                log.info("get auditor data: {0}", getAuditorData);
                if (getAuditorData) {
                    buildAuditorDataObject(amb, userName);
                }
                //build special attributes
                buildSpecialAttributes(amb, columnValues);
                
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                
                // add all special attributes to account
                bld.setObjectClass(ObjectClass.ACCOUNT);
                bld.addAttributes(amb.build());
                bld.setName(userName);
                bld.setUid(userName);
                

                
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
        conn.commit();
        log.ok(method);        
    }

    /**
     * Transform the columns to special attributes
     * @param amb
     * @param columnValues
     */
    public void buildSpecialAttributes(final AttributeMergeBuilder amb, final Map<String, SQLParam> columnValues) {
        // create the connector object..
        final Date dateNow = new Date(System.currentTimeMillis());
        final SQLParam end_date_param = columnValues.get(END_DATE);
        Date end_date = null;
        //disable date
        if (end_date_param != null) {
            end_date = (Date) end_date_param.getValue();
            if( end_date != null ) {
                amb.addAttribute(AttributeBuilder.buildDisableDate(end_date));
            }
        }

        //enable date
        final SQLParam start_date_param = columnValues.get(START_DATE);
        Date start_date = null;
        if (start_date_param != null) {
            start_date = (Date) start_date_param.getValue();
            if ( start_date != null) {
                amb.addAttribute(AttributeBuilder.buildEnableDate(start_date));
            }
        }

        //enable
        if (end_date != null && start_date != null) {
            boolean enable = dateNow.compareTo(end_date) <= 0 && dateNow.compareTo(start_date) > 0;
            amb.addAttribute(AttributeBuilder.buildEnabled(enable));
        } else if (end_date != null) {
            boolean enable = dateNow.compareTo(end_date) <= 0;
            amb.addAttribute(AttributeBuilder.buildEnabled(enable));
        } else if (start_date != null) {
            boolean enable = dateNow.compareTo(start_date) > 0;
            amb.addAttribute(AttributeBuilder.buildEnabled(enable));
        } else {
            //bld.addAttribute(AttributeBuilder.buildEnabled(false));                        
        }
        
        final SQLParam lastLogonDateParam = columnValues.get(LAST_LOGON_DATE);
        if (lastLogonDateParam != null) {
            final Date lastLogonDate = (Date) lastLogonDateParam.getValue();   
            if ( lastLogonDate != null ) {
                amb.addAttribute(AttributeBuilder.buildLastLoginDate(lastLogonDate));
            }
        }        

        // password expired
        final SQLParam pwdDateParam = columnValues.get(PWD_DATE);
        Date pwdDate = null;
        if (pwdDateParam != null) {
            pwdDate = (Date) columnValues.get(PWD_DATE).getValue();
            if( pwdDate != null ) {
                amb.addAttribute(AttributeBuilder.buildLastPasswordChangeDate(pwdDate));
            }
        }
        
        final SQLParam access_left_param = columnValues.get(PWD_ACCESSES_LEFT);
        BigDecimal access_left = null;
        if( access_left_param != null ) {
            access_left = (BigDecimal) columnValues.get(PWD_ACCESSES_LEFT).getValue();
        }
        final SQLParam lifespan_days_param = columnValues.get(PWD_LIFESPAN_DAYS);
        BigDecimal lifespan_days = null;
        if ( lifespan_days_param != null ) {
            lifespan_days = (BigDecimal) columnValues.get(PWD_LIFESPAN_DAYS).getValue();
        }        
        if (access_left != null && access_left.intValue() <= 0) {
            amb.addAttribute(AttributeBuilder.buildPasswordExpired(true));
        } else if (lifespan_days != null && pwdDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pwdDate);
            cal.add(Calendar.DAY_OF_MONTH, lifespan_days.intValue());
            boolean expired = cal.after(dateNow);
            amb.addAttribute(AttributeBuilder.buildPasswordExpired(expired));
        } else if ( pwdDate == null){
            amb.addAttribute(AttributeBuilder.buildPasswordExpired(true));
        } else {
            amb.addAttribute(AttributeBuilder.buildPasswordExpired(false));
        }
    }
    
    /**
     * @param attributesToGet
     *            from application
     * @return the set of the column names
     */
    private Set<String> getColumnNamesToGet(Set<String> attributesToGet) {
        Set<String> columnNamesToGet = CollectionUtil.newCaseInsensitiveSet();

        // Replace attributes to quoted columnNames
        for (String attributeName : attributesToGet) {
            final String columnName = nr.getColumnName(attributeName);
            if (columnName != null) {
                columnNamesToGet.add(columnName);
            }
        }      

        log.ok("columnNamesToGet {0}", columnNamesToGet);
        return columnNamesToGet;
    }

    /**
     * @param bld
     * @param columnValues
     * @param columnNames
     */
    private void buildPersonDetails(AttributeMergeBuilder bld, final Map<String, SQLParam> columnValues,
            Set<String> personColumns) {

        if (columnValues == null || columnValues.get(EMP_ID) == null) {
            // No personId(employId)
            log.ok("buildPersonDetails: No personId(employId)");
            return;
        }
        final BigDecimal personId = (BigDecimal) columnValues.get(EMP_ID).getValue();
        if (personId == null) {
            log.ok("buildPersonDetails: Null personId(employId)");
            return;
        }
        log.info("buildPersonDetails for personId: {0}", personId);

        //Names to get filter
        final String tblname = cfg.app() + "PER_PEOPLE_F";

        if (personColumns.isEmpty()) {
            // No persons column required
            log.ok("No persons column To Get");
            return;
        }
        log.ok("personColumns {0} To Get", personColumns);

        // For all account query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, personColumns);
        final FilterWhereBuilder where = new FilterWhereBuilder();
        where.addBind(new SQLParam(PERSON_ID, personId, Types.DECIMAL), "=");
        query.setWhere(where);

        final String sql = query.getSQL();
        String msg = "Oracle ERP: sql = ''{0}''";
        log.info(msg, sql);

        ResultSet result = null; // SQL query on person_id
        PreparedStatement statement = null; // statement that generates the query
        try {
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            if (result != null) {
                log.ok("executeQuery {0}", query.getSQL());
                if (result.next()) {
                    final Map<String, SQLParam> personValues = SQLUtil.getColumnValues(result);
                    // get users account attributes
                    this.merrgeAllAttributes(bld, personValues);
                    log.ok("Person values {0} from result set ", personValues);
                }
            }

        } catch (SQLException e) {
            String emsg = e.getMessage();
            msg = "Caught SQLException when executing: ''{0}'': {1}";
            log.error(msg, sql, emsg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(result);
            result = null;
            SQLUtil.closeQuietly(statement);
            statement = null;
        }
    }

  
    /**
     * Merge all attribute . Translate column name to attribute name and convert the value
     * Add only readable attributes
     * @param amb the attribute merger
     * @param columnValues the column value map
     * @throws SQLException
     */
    private void merrgeAllAttributes(AttributeMergeBuilder amb, Map<String, SQLParam> columnValues) throws SQLException {
        final String method = "buildAccountObject";
        log.info(method);
        for (Map.Entry<String, SQLParam> val : columnValues.entrySet()) {
            mergeAttribute(amb, val.getValue());
        }
    }

    /**
     * @param amb
     *            AttributeMergeBuilder
     * @param param
     *            SQLParam
     * @throws SQLException
     *             if something wrong
     */
    private void mergeAttribute(AttributeMergeBuilder amb, final SQLParam param) throws SQLException {
        final String columnName = param.getName();
        //Convert the data type and create attribute from it.
        final String attributeName = nr.getAttributeName(columnName);
        final Set<String> readable = getReadableAttributes(getAttributeInfos(cfg.getSchema(), ObjectClass.ACCOUNT_NAME));
        //  Add only readable attributes
        if (readable.contains(attributeName)) {
            final Object value = SQLUtil.jdbc2AttributeValue(param.getValue());
            amb.addAttribute(attributeName, value);
        }
    }
    
    /**
     * @param amb builder
     * @param userName of the user
     */
    private void buildResponsibilitiesToAccountObject(AttributeMergeBuilder amb, final String userName) {
         
        if (!cfg.isNewResponsibilityViews() && amb.isInAttributesToGet(RESPS)) {
            //add responsibilities
            final List<String> responsibilities = respOps.getResponsibilities(userName, RESPS_TABLE, false);
            amb.addAttribute(RESPS, responsibilities);

            //add resps list
            final List<String> resps = respOps.getResps(responsibilities, RESP_FMT_KEYS);
            amb.addAttribute(RESPKEYS, resps);
        } else if (amb.isInAttributesToGet(DIRECT_RESPS)) {
            final List<String> responsibilities = respOps.getResponsibilities(userName, RESPS_DIRECT_VIEW, false);
            amb.addAttribute(DIRECT_RESPS, responsibilities);

            //add resps list
            final List<String> resps = respOps.getResps(responsibilities, RESP_FMT_KEYS);
            amb.addAttribute(RESPKEYS, resps);
        }

        if (amb.isInAttributesToGet(INDIRECT_RESPS)) {
            //add responsibilities
            final List<String> responsibilities = respOps.getResponsibilities(userName, RESPS_INDIRECT_VIEW, false);
            amb.addAttribute(INDIRECT_RESPS, responsibilities);
        }
    }
    
    
    /**
     * @param amb
     * @param userName 
     */
    private void buildSecuringAttributesToAccountObject(AttributeMergeBuilder amb, final String userName) {
        if (!cfg.isManageSecuringAttrs()) {
            return;
        }

        if ( amb.isInAttributesToGet(SEC_ATTRS) ) {
            List<String> secAttrs = secAttrOps.getSecuringAttrs(userName);
            if (secAttrs != null) {
                amb.addAttribute(SEC_ATTRS, secAttrs);
            }
        }
    }
    
    /**
     * @param amb
     *            builder
     * @param userName
     *            id of the responsibility
     */
    public void buildAuditorDataObject(AttributeMergeBuilder amb, String userName) {
        log.info("buildAuditorDataObject for uid: {0}", userName);
        List<String> activeRespList = respOps.getResponsibilities(userName, respOps.getRespLocation(), false);
        for (String activeRespName : activeRespList) {
            auditOps.updateAuditorData(amb, activeRespName);
        }
    }   
}
