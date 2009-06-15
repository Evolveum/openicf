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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;


/**
 * Main implementation of the OracleErp Connector
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class ResponsibilityNames {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(ResponsibilityNames.class);

    /**
     * The get Instance method
     * 
     * @param connector
     *            parent
     * @return the account
     */
    public static ResponsibilityNames getInstance(OracleERPConnector connector) {
        return new ResponsibilityNames(connector);
    }

    /**
     * The parent connector
     */
    private OracleERPConnector co;

    /**
     * If 11.5.10, determine if description field exists in responsibility views. Default to true
     */
    private boolean descrExists = true;

    /**
     * Check to see which responsibility account attribute is sent. Version 11.5.9 only supports responsibilities, and
     * 11.5.10 only supports directResponsibilities and indirectResponsibilities Default to false If 11.5.10, determine
     * if description field exists in responsibility views.
     */
    private boolean newResponsibilityViews = false;

    /**
     * Accessor for the descrExists property
     * 
     * @return the descrExists
     */
    public boolean isDescrExists() {
        return descrExists;
    }

    /**
     * Accessor for the newResponsibilityViews property
     * 
     * @return the newResponsibilityViews
     */
    public boolean isNewResponsibilityViews() {
        return newResponsibilityViews;
    }

    /**
     * Responsibility Application Id
     */
    private String respApplId = "";

    /**
     * Responsibility Id
     */
    private String respId = "";

    /**
     * The ResponsibilityNames
     * 
     * @param connector
     *            parent
     */
    private ResponsibilityNames(OracleERPConnector connector) {
        this.co = connector;
    }

    /**
     * @param bld
     * @param columnValues
     * @param columnNames
     */
    public void buildResponsibilitiesToAccountObject(ConnectorObjectBuilder bld, Map<String, SQLParam> columnValues,
            Set<String> columnNames) {
        final String id = getStringParamValue(columnValues, USER_ID);
        if (columnNames.contains(RESPS) && !isNewResponsibilityViews()) {
            //add responsibilities
            final List<String> responsibilities = getResponsibilities(id, RESPS_TABLE, false);
            bld.addAttribute(RESPS, responsibilities);

            //add resps list
            final List<String> resps = getResps(responsibilities, RESP_FMT_KEYS);
            bld.addAttribute(RESPKEYS, resps);
        } else if (columnNames.contains(DIRECT_RESPS)) {
            final List<String> responsibilities = getResponsibilities(id, RESPS_DIRECT_VIEW, false);
            bld.addAttribute(DIRECT_RESPS, responsibilities);

            //add resps list
            final List<String> resps = getResps(responsibilities, RESP_FMT_KEYS);
            bld.addAttribute(RESPKEYS, resps);
        }

        if (columnNames.contains(INDIRECT_RESPS)) {
            //add responsibilities
            final List<String> responsibilities = getResponsibilities(id, RESPS_INDIRECT_VIEW, false);
            bld.addAttribute(INDIRECT_RESPS, responsibilities);
        }
    }

    /**
     * The New responsibility format there
     * 
     * @return true/false
     */
    public boolean getNewResponsibilityViews() {
        final String sql = "select * from " + co.app()
                + "fnd_views where VIEW_NAME = 'FND_USER_RESP_GROUPS_DIRECT' and APPLICATION_ID = '0'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = co.getConn().prepareStatement(sql);
            res = ps.executeQuery();
            log.ok(sql);
            if (res != null && res.next()) {
                log.ok("ResponsibilityViews exists");
                return true;
            }
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        log.ok("ResponsibilityViews does not exists");
        return false;
    }

    /**
     * bug#13889 : Added method to create a responsibility string with dates normalized. respFmt: RESP_FMT_KEYS: get
     * responsibility keys (resp_name, app_name, sec_group) RESP_FMT_NORMALIZE_DATES: get responsibility string
     * (resp_name, app_name, sec_group, description, start_date, end_date) start_date, end_date (no time data, allow
     * nulls)
     * 
     * @param strResp
     * @param respFmt
     * @return normalized resps string
     */
    private String getResp(String strResp, int respFmt) {
        final String method = "getResp(String, int)";
        log.info(method + "respFmt=" + respFmt);
        String strRespRet = null;
        StringTokenizer tok = new StringTokenizer(strResp, "||", false);
        if (tok != null && tok.countTokens() > 2) {
            StringBuffer key = new StringBuffer();
            key.append(tok.nextToken()); // responsiblity name
            key.append("||");
            key.append(tok.nextToken()); // application name
            key.append("||");
            key.append(tok.nextToken()); // security group name
            if (respFmt != RESP_FMT_KEYS) {
                key.append("||");
                // descr possibly not available in ui version 11.5.10
                if (!isNewResponsibilityViews() || isDescrExists()) {
                    key.append(tok.nextToken()); // description
                }
                key.append("||");
                key.append(normalizeStrDate(tok.nextToken())); // start_date
                key.append("||");
                key.append(normalizeStrDate(tok.nextToken())); // end_date
            }
            strRespRet = key.toString();
        }
        log.ok(method);
        return strRespRet;
    } // getRespWithNormalizeDates()  

    /**
     * Accessor for the respApplId property
     * 
     * @return the respApplId
     */
    public String getRespApplId() {
        return respApplId;
    }

    /**
     * Accessor for the respId property
     * 
     * @return the respId
     */
    public String getRespId() {
        return respId;
    }

    /**
     * Init the responsibilities
     * 
     * @param configUserId
     *            configUserId
     */
    public void initResponsibilities(final String configUserId) {
        this.newResponsibilityViews = getNewResponsibilityViews();

        if (isNewResponsibilityViews()) {
            this.descrExists = getDescriptionExiests();
        }

        // three pieces of data need for apps_initialize()
        final String auditResponsibility = co.getCfg().getAuditResponsibility();

        if (StringUtil.isNotBlank(auditResponsibility)) {
            if (StringUtil.isNotBlank(configUserId)) {
                co.getSecAttrs().initAdminUserId(configUserId);
            }

            final String view = co.app()
                    + ((isNewResponsibilityViews()) ? OracleERPUtil.RESPS_ALL_VIEW : OracleERPUtil.RESPS_TABLE);
            final String sql = "select responsibility_id, responsibility_application_id from "
                    + view
                    + " where user_id = ? and "
                    + "(responsibility_id,responsibility_application_id) = (select responsibility_id,application_id from "
                    + "{0}fnd_responsibility_vl where responsibility_name = ?)";

            final String msg = "Oracle ERP SQL: {0} returned: RESP_ID = {1}, RESP_APPL_ID = {2}";

            ArrayList<SQLParam> params = new ArrayList<SQLParam>();
            params.add(new SQLParam(configUserId));
            params.add(new SQLParam(auditResponsibility));
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                log.info("Select responsibility for user_id: {0}, and audit responsibility {1}", configUserId,
                        auditResponsibility);
                ps = co.getConn().prepareStatement(sql, params);
                rs = ps.executeQuery();
                if (rs != null) {
                    if (rs.next()) {
                        respId = rs.getString(1);
                        respApplId = rs.getString(2);
                    }
                }

                log.ok(msg, sql, respId, respApplId);
            } catch (SQLException e) {
                log.error(e, msg, sql, respId, respApplId);
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(rs);
                rs = null;
                SQLUtil.closeQuietly(ps);
                ps = null;
            }
        }
    }

    /**
     * getResponsibilities
     * 
     * @param id
     *            user id
     * @param respLocation
     *            The responsibilities table
     * @param activeOnly
     *            select active only
     * @return list of strings of multivalued attribute
     */
    private List<String> getResponsibilities(String id, String respLocation, boolean activeOnly) {

        final String method = "getResponsibilities";
        log.info(method);

        StringBuffer b = new StringBuffer();

        b.append("SELECT fndappvl.application_name, fndrespvl.responsibility_name, ");
        b.append("fndsecgvl.Security_group_name ");
        // descr may not be available in view or in native ui with new resp views
        // bug#15492 - do not include user tables in query if id not specified, does not return allr responsibilities
        if (id != null) {
            if (!isNewResponsibilityViews() || (isDescrExists() && respLocation.equalsIgnoreCase(RESPS_DIRECT_VIEW))) {
                b.append(", fnduserg.DESCRIPTION");
            }
            b.append(", fnduserg.START_DATE, fnduserg.END_DATE ");
        }
        b.append("FROM " + co.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(co.app() + "fnd_application_vl fndappvl, ");
        // bug#15492 - don't include this join if no id is specified.
        if (id != null) {
            b.append(co.app() + "fnd_user fnduser, ");
            b.append(co.app() + respLocation + " fnduserg, ");
        }
        b.append(co.app() + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        // bug#15492 - don't include this join if no id is specified.
        if (id != null) {
            b.append("AND fnduser.user_id = fnduserg.user_id ");
            b.append("AND fndrespvl.RESPONSIBILITY_ID = fnduserg.RESPONSIBILITY_ID ");
            b.append("AND fndrespvl.APPLICATION_ID = fnduserg.RESPONSIBILITY_APPLICATION_ID ");
            b.append("AND fnduser.USER_NAME = ? ");
            b.append("AND fndsecgvl.security_group_id = fnduserg.security_group_id ");
        }
        if (activeOnly) {
            if (id != null) {
                b.append(" AND fnduserg.START_DATE - SYSDATE <= 0 "
                        + "AND (fnduserg.END_DATE IS NULL OR fnduserg.END_DATE - SysDate > 0)");
            }
        }

        PreparedStatement st = null;
        ResultSet res = null;
        List<String> arrayList = new ArrayList<String>();
        final String sql = b.toString();
        try {
            log.info("sql select {0}", sql);
            st = co.getConn().prepareStatement(sql);
            if (id != null) {
                st.setString(1, id.toUpperCase());
            }
            res = st.executeQuery();
            while (res.next()) {

                // six columns with old resp table, 5 with new views - 
                // no description available
                StringBuffer sb = new StringBuffer();
                String s = getColumn(res, 2); // fndrespvl.responsibility_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 1); // fndappvl.application_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 3); // fndsecgvl.Security_group_name
                sb.append(s);
                sb.append("||");
                if (id != null) {
                    s = getColumn(res, 4); // fnduserg.DESCRIPTION or fnduserg.START_DATE
                    sb.append(s);
                }
                sb.append("||");
                if (id != null) {
                    s = getColumn(res, 5); // fnduserg.START_DATE or fnduserg.END_DATE
                    sb.append(s);
                }
                if (!isNewResponsibilityViews()
                        || (isDescrExists() && respLocation.equalsIgnoreCase(RESPS_DIRECT_VIEW))) {
                    sb.append("||");
                    if (id != null) {
                        s = getColumn(res, 6); // fnduserg.END_DATE
                        sb.append(s);
                    }
                }

                arrayList.add(sb.toString());
            }
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }

        log.ok(method);
        return arrayList;
    }

    /**
     * bug#13889 : Added method to create a responsibilities list with dates normalized. RESP_FMT_KEYS: get
     * responsibility keys (resp_name, app_name, sec_group) RESP_FMT_NORMALIZE_DATES: get responsibility keys
     * (resp_name, app_name, sec_group, description, start_date, end_date)
     * 
     * @param resps
     * @param respFmt
     * @return list of Sting
     */
    private List<String> getResps(List<String> resps, int respFmt) {
        final String method = "getResps(ArrayList, int)";
        log.info(method + " respFmt=" + respFmt);
        List<String> respKeys = null;
        if (resps != null) {
            respKeys = new ArrayList<String>();
            for (String strResp : resps) {
                String strRespReformatted = getResp(strResp, respFmt);
                log.info(method + " strResp='" + strResp + "', strRespReformatted='" + strRespReformatted + "'");
                respKeys.add(strRespReformatted);
            }
        }
        log.ok(method);
        return respKeys;
    } // getResps()  

    /**
     * 
     * @param attr
     *            resp attribute
     * @param identity
     * @param result
     * @throws WavesetException
     */
    public void updateUserResponsibilities(final Attribute attr, final String identity) {
        final String method = "updateUserResponsibilities";
        log.info(method);

        final List<String> errors = new ArrayList<String>();
        final List<String> respList = new ArrayList<String>();
        for (Object obj : attr.getValue()) {
            respList.add(obj.toString());
        }

        // get Users Current Responsibilties
        List<String> oldResp = null;
        if (!isNewResponsibilityViews()) {
            oldResp = getResponsibilities(identity, RESPS_TABLE, false);
        } else {
            // can only update directly assigned resps; indirect resps are readonly
            // thru ui            
            oldResp = getResponsibilities(identity, RESPS_DIRECT_VIEW, false);
        }
        //preserve the previous behavior where oldResp is never null.
        if (oldResp == null) {
            oldResp = new ArrayList<String>();
        }
        List<String> oldRespKeys = getResps(oldResp, RESP_FMT_KEYS);
        List<String> newRespKeys = getResps(respList, RESP_FMT_KEYS);
        // bug#13889
        // create responsibilities list with dates normalized i.e., with no time data.
        // We ignore the time data due to potential time differences between the Oracle DB environment and the IDM client.
        // start and end dates are specified as date only from the Oracle Application GUI.
        List<String> oldRespsWithNormalizedDates = getResps(oldResp, RESP_FMT_NORMALIZE_DATES);
        // if old key is not in new list, delete it
        if (oldRespKeys != null) {
            int index = 0;
            for (String resp : oldRespKeys) {
                if (!newRespKeys.contains(resp)) {
                    // bug#9637 check to see if resp is already 
                    // endDated (disabled), if so, ignore, if not,
                    // delete resp from User
                    java.util.Date curDate = getCurrentDate();
                    java.sql.Date endDate = null;
                    boolean delResp = false;
                    String respStr = oldResp.get(index);
                    StringTokenizer tok = new StringTokenizer(respStr, "||", false);
                    if (tok != null) {
                        String endDateStr = null;
                        while (tok.hasMoreTokens()) {
                            endDateStr = tok.nextToken();
                        }
                        if (endDateStr != null && !endDateStr.equalsIgnoreCase("null")) {
                            // format date input
                            int i = endDateStr.indexOf(" ");
                            endDate = java.sql.Date.valueOf(endDateStr.substring(0, i));
                            delResp = endDate.after(curDate);
                        } else {
                            delResp = true;
                        }
                    }
                    if (delResp) {
                        deleteUserResponsibility(identity, resp, errors);
                        log.error("deleted, (end_dated), responsibility: '" + resp + "' for " + identity);
                    }
                }
                index++;
            }
        }
        // if new key is not in old list add it and remove from respList
        // after adding
        if (respList != null) {
            // make copy of array to itereate through because we will be
            // modifying the respList
            List<String> resps = new ArrayList<String>(respList);
            for (String resp : resps) {
                String respKey = getResp(resp, RESP_FMT_KEYS);
                if (!resp.equalsIgnoreCase("") && !oldRespKeys.contains(respKey)) {
                    addUserResponsibility(identity, resp, errors);
                    respList.remove(resp);
                    log.info("added responsibility: '" + resp + "' for " + identity);
                }
            }
        }//end-if
        // if new key is both lists, update it
        if (respList != null) {
            String respWithNormalizedDates = null;
            for (String resp : respList) {
                // bug#13889 -  do not update all responsibilities
                //              only update the ones that changed.
                //              Updating all responsibilities every time masks the audit records.
                //              Added check to see if oldResp list 
                //              contains the current entire responsibility
                //              string.
                if (resp != null) {
                    log.info("checking if update required for responsibility: '" + resp + "' for " + identity);
                } else {
                    log.warn(" resp=NULL while processing updates");
                }
                // Add/Update resp to user
                if (resp != null && !resp.equalsIgnoreCase("")) {
                    // normalize the date string to only contain the date, no time information.
                    respWithNormalizedDates = getResp(resp, RESP_FMT_NORMALIZE_DATES);

                    if (respWithNormalizedDates != null) {
                        log.info("respWithNormalizedDates='" + respWithNormalizedDates + "'");
                    } else {
                        log.warn("respWithNormalizedDates=null while processing updates");
                    }

                    // Add/update resp to user if the date normalized responsibility string is not in the old date normalized list.
                    if ((oldRespsWithNormalizedDates != null) && respWithNormalizedDates != null
                            && !respWithNormalizedDates.equalsIgnoreCase("")
                            && !oldRespsWithNormalizedDates.contains(respWithNormalizedDates)) {
                        updateUserResponsibility(identity, resp, errors);

                        String msg = "updated responsibility: '" + resp + "' for " + identity;
                        log.info(msg);
                    }
                }
            }
        }//end-if

        // bug#16656: delayed error handling for missing responsibilities
        if (!errors.isEmpty()) {
            StringBuilder error = new StringBuilder();
            for (String msg : errors) {
                error.append(msg);
                error.append(";");
            }
            log.error(error.toString());
            throw new ConnectorException(error.toString());
        }

        log.ok(method);

    }

    private void addUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "addUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;
        String description = null;
        String fromDate = null;
        String toDate = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        int count = tok.countTokens();
        if (tok != null && count > 4) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
            // descr optionable in 11.5.10 - check if sent
            if (count > 5) {
                description = tok.nextToken();
            }
            fromDate = tok.nextToken();
            toDate = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        // descr null conversion
        if (description != null && !description.equalsIgnoreCase("null")) {
            description = "'" + description + "'";
        } else {
            description = null;
        }
        // date field convert - start_date cannot be null, set to sysdate
        if ((fromDate == null) || fromDate.equalsIgnoreCase("null")) {
            fromDate = "sysdate";
        } else if (fromDate.length() == 10) {
            fromDate = "to_date('" + fromDate + "', 'yyyy-mm-dd')";
        } else if (fromDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            fromDate = "to_date('" + fromDate.substring(0, fromDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        if ((toDate == null) || toDate.equalsIgnoreCase("null")) {
            toDate = null;
        } else if (toDate.length() == 10) {
            toDate = "to_date('" + toDate + "', 'yyyy-mm-dd')";
        } else if (toDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            toDate = "to_date('" + toDate.substring(0, toDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        StringBuffer b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, true /* doing an insert */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            log.info("execute statement ''{0}''", sql);
            st = co.getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            //
            // 19057: check whether this is a "no data found" error;
            // if so, then perhaps the responsibility we seek doesn't
            // have a valid app name.  We'll retry the query without
            // specifying the app name.
            //
            if (e.getErrorCode() == ORA_01403) {
                doRetryWithoutAppname = true;
            } else {
                final String msg = "Can not execute the sql " + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }

        if (doRetryWithoutAppname) {
            //
            // 19057: without the responsibility's application name, must
            // fall back to using just the responsibility name to identify
            // the desired responsibility
            //
            b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                    null /* respAppName is not valid */, fromDate, toDate, description, true);

            sql = b.toString();
            try {
                log.info("execute statement ''{0}''", sql);
                st = co.getConn().prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    errors.add("Failed to add '" + resp + "' responsibility:" + e.getMessage());
                } else {
                    final String msg = "Can not execute the sql " + sql;
                    log.error(e, msg);
                    throw new ConnectorException(msg, e);
                }
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.ok(method);
    }

    /**
     * This method is shared by addUserResponsibility and updateUserResponsibility to build their PL/SQL statements.
     */
    private StringBuffer buildUserRespStatement(String user, String secGroup, String respName, String respAppName,
            String fromDate, String toDate, String description, boolean doInsert) {

        StringBuffer b = new StringBuffer();
        b.append("DECLARE user varchar2(300); security_group varchar2(300); ");
        b.append("responsibility_long_name varchar2(300); ");
        if (respAppName != null) {
            b.append("responsibility_app_name varchar2(300); ");
        }
        b.append("sec_group_id Number; user_id_num Number; resp_id varchar2(300); app_id Number; sec_id Number; ");
        b.append("description varchar2(300); resp_sec_g_key varchar2(300); ");
        b.append("BEGIN user := ");
        addQuoted(b, user.toUpperCase());
        b.append("; security_group := ");
        addQuoted(b, secGroup.toUpperCase());
        b.append("; responsibility_long_name := ");
        addQuoted(b, respName);
        if (respAppName != null) {
            b.append("; responsibility_app_name := ");
            addQuoted(b, respAppName);
        }
        b.append("; ");
        b.append("SELECT responsibility_id, application_id INTO resp_id, app_id ");
        b.append("FROM " + co.app() + "fnd_responsibility_vl ");
        b.append("WHERE responsibility_name = responsibility_long_name");
        if (respAppName != null) {
            b.append(" AND application_id = ");
            b.append("(SELECT application_id FROM " + co.app() + "fnd_application_vl ");
            b.append("WHERE application_name = responsibility_app_name)");
        }
        b.append("; ");
        b.append("SELECT user_id INTO user_id_num ");
        b.append("FROM " + co.app() + "fnd_user ");
        b.append("WHERE USER_NAME = user; ");
        b.append("SELECT security_group_id INTO sec_group_id ");
        b.append("FROM " + co.app() + "fnd_security_groups_vl ");
        b.append("WHERE SECURITY_GROUP_KEY = security_group; ");

        b.append(co.app());
        if (doInsert) {
            b.append("fnd_user_resp_groups_api.Insert_Assignment (user_id_num, resp_id, app_id, sec_group_id, ");
        } else {
            b.append("fnd_user_resp_groups_api.Update_Assignment (user_id_num, resp_id, app_id, sec_group_id, ");
        }
        b.append(fromDate);
        b.append(", ");
        b.append(toDate);
        b.append(", ");
        b.append(description);
        b.append("); COMMIT; END;");

        return b;
    }

    private void deleteUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "deleteUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        StringBuffer b = new StringBuffer();

        b.append("DECLARE user_id varchar2(300); security_group varchar2(300); ");
        b.append("responsibility_long_name varchar2(300); responsibility_app_name ");
        b.append("varchar2(300); resp_app varchar2(300); resp_key varchar2(300); ");
        b.append("description varchar2(300); resp_sec_g_key varchar2(300); ");

        b.append("BEGIN user_id := ");
        addQuoted(b, identity.toUpperCase());
        b.append("; security_group := ");
        addQuoted(b, securityGroup);
        b.append("; responsibility_long_name := ");
        addQuoted(b, respName);
        b.append("; responsibility_app_name := ");
        addQuoted(b, respAppName);
        b.append("; SELECT  fndsecg.security_group_key INTO resp_sec_g_key ");
        b.append("FROM " + co.app() + "fnd_security_groups fndsecg, " + co.app() + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndsecg.security_group_id = fndsecgvl.security_group_id ");
        b.append("AND fndsecgvl.security_group_name = security_group; ");
        b.append("SELECT fndapp.application_short_name, fndresp.responsibility_key, ");
        b.append("fndrespvl.description INTO resp_app, resp_key, description ");
        b.append("FROM " + co.app() + "fnd_responsibility_vl fndrespvl, " + co.app() + "fnd_responsibility fndresp, ");
        b.append(co.app() + "fnd_application_vl fndappvl, " + co.app() + "fnd_application fndapp ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        b.append("AND fndappvl.APPLICATION_ID = fndapp.APPLICATION_ID ");
        b.append("AND fndappvl.APPLICATION_NAME = responsibility_app_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_NAME = responsibility_long_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_ID = fndresp.RESPONSIBILITY_ID ");
        b.append("AND fndrespvl.APPLICATION_ID = fndresp.APPLICATION_ID; ");
        b.append(co.app() + "fnd_user_pkg.DelResp (user_id, resp_app, resp_key, resp_sec_g_key); ");
        b.append("COMMIT; END;");

        final String sql = b.toString();
        try {
            log.info("execute statement ''{0}''", sql);
            st = co.getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_01403) {
                // bug#16656: delay error handling for missing responsibilities
                errors.add("Failed to delete '" + resp + "' responsibility:" + e.getMessage());
            } else {
                final String msg = "Can not execute the sql " + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
    }

    /**
     * @return
     */
    private boolean getDescriptionExiests() {
        final String sql = "select user_id, description from " + co.app()
                + "fnd_user_resp_groups_direct where USER_ID = '9999999999'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = co.getConn().prepareStatement(sql);
            res = ps.executeQuery();
            log.ok("description exists");
            return true;
        } catch (SQLException e) {
            //log.error(e, sql);
            log.ok("description does not exists");
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        return false;
    }

    private void updateUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "updateUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;
        String description = null;
        String fromDate = null;
        String toDate = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        int count = tok.countTokens();
        if ((tok != null) && (count > 4)) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
            // descr optionable in 11.5.10 - check if sent
            if (count > 5) {
                description = tok.nextToken();
            }
            fromDate = tok.nextToken();
            toDate = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        // descr null conversion
        if (description != null && !description.equalsIgnoreCase("null")) {
            description = "'" + description + "'";
        } else {
            description = null;
        }
        // date field convert - start_date cannot be null, set to sysdate
        if ((fromDate == null) || fromDate.equalsIgnoreCase("null")) {
            fromDate = "sysdate";
        } else if (fromDate.length() == 10) {
            fromDate = "to_date('" + fromDate + "', 'yyyy-mm-dd')";
        } else if (fromDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            fromDate = "to_date('" + fromDate.substring(0, fromDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        if ((toDate == null) || toDate.equalsIgnoreCase("null")) {
            toDate = null;
        } else if (toDate.equalsIgnoreCase(SYSDATE)) {
            toDate = "sysdate";
        } else if (toDate.length() == 10) {
            toDate = "to_date('" + toDate + "', 'yyyy-mm-dd')";
        } else if (toDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            toDate = "to_date('" + toDate.substring(0, toDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        StringBuffer b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, false /* not doing an insert, doing an update */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            log.info("sql select {0}", sql);
            st = co.getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            //
            // 19057: check whether this is a "no data found" error;
            // if so, then perhaps the responsibility we seek doesn't
            // have a valid app name.  We'll retry the query without
            // specifying the app name.
            //
            if (e.getErrorCode() == ORA_01403) {
                doRetryWithoutAppname = true;
            } else {
                final String msg = "Error in sql :" + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }

        if (doRetryWithoutAppname) {
            //
            // 19057: without the responsibility's application name, must
            // fall back to using just the responsibility name to identify
            // the desired responsibility
            //
            b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                    null /* respAppName is not valid */, fromDate, toDate, description, false);

            sql = b.toString();
            try {
                log.info("sql select {0}", sql);
                st = co.getConn().prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    errors.add("Failed to update '" + resp + "' responsibility:" + e.getMessage());
                } else {
                    final String msg = "Can not execute the sql " + sql;
                    log.error(e, msg);
                    throw new ConnectorException(msg, e);
                }
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.ok(method);
    }

    /**
     * The ResponsibilityNames
     * @param where 
     * @param handler 
     * @param options 
     */
    private void getResponsibilityNames(FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {
        final String method = "getResponsibilityNames";
        log.info( method);

        PreparedStatement st = null;
        ResultSet res = null;
        StringBuffer b = new StringBuffer();

        b.append("SELECT distinct fndrespvl.responsibility_name ");
        b.append("FROM " + co.app()+ "fnd_responsibility_vl fndrespvl, ");
        b.append(co.app() + "fnd_application_vl fndappvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");

        try {
            st = co.getConn().prepareStatement(b.toString());
            res = st.executeQuery();
            while (res.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(RESP_NAMES_OC);

                String s = getColumn(res, 1);
                bld.addAttribute(Name.NAME, s);
                bld.addAttribute(NAME, s);
                
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
        }
        catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
    }
    
    
    /**
     * @param oclass
     * @param where
     * @param handler
     * @param options
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        if (oclass.equals(RESP_NAMES_OC)) {
            getResponsibilityNames(where, handler,options);
            return;
        } else if (oclass.equals(RESP_OC)) { //OK
            getResponsibilityNames(where, handler,options);
            return;
        } else if (oclass.equals(DIRECT_RESP_OC)) { //OK
            getResponsibilityNames(where, handler,options);
            return;
        } else if (oclass.equals(INDIRECT_RESP_OC)) { //OK
            getResponsibilityNames(where, handler,options);
            return;
        } else if (oclass.equals(APPS_OC)) {            
            getApplications(where, handler, options);
            return;
        } else if (oclass.equals(AUDITOR_RESPS_OC)) { // ok
            getAuditorResponsibilities(where, handler,options);
            return;
        }
        throw new IllegalArgumentException(co.getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }
    


    /**
     * Get applications for a argument 
     * @param where
     * @param handler
     * @param options
     */
    private void getApplications(FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {
        final String method = "getApplications";
        log.info( method);

        PreparedStatement st = null;
        ResultSet res = null;
        StringBuffer b = new StringBuffer();
        String respName = null;
        if(options != null && options.getOptions() != null) {
            respName = (String) options.getOptions().get(RESP_NAME);
        } else {
            //TODO do I support where there?
            return;
        }
        
        b.append("SELECT distinct fndappvl.application_name ");
        b.append("FROM " + co.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(co.app() + "fnd_application_vl fndappvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        b.append("AND fndrespvl.responsibility_name = ?");

        try {
            st = co.getConn().prepareStatement(b.toString());
            st.setString(1, respName);
            res = st.executeQuery();
            while (res.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(APPS_OC);

                String s = getColumn(res, 1);
                bld.addAttribute(Name.NAME, s);
                bld.addAttribute(NAME, s);
                
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
        }
        catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
}


    /**
     * @param schemaBld
     */
    public void schema(SchemaBuilder schemaBld) {
        final EnumSet<Flags> STD_RNA = EnumSet.of(Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE);
        
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        oc.setType(RESP_NAMES_OC.getObjectClassValue());

        // The Name is supported attribute
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        // name='userMenuNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, STD_RNA));
        // name='menuIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, STD_RNA));
        // name='userFunctionNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, STD_RNA));
        // name='functionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, STD_RNA));
        // name='formIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, STD_RNA));
        // name='formNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, STD_RNA));
        // name='functionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, STD_RNA));
        // name='userFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, STD_RNA));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, STD_RNA));
        // name='readOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTION_NAMES, String.class, STD_RNA));
        // name='readOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_USER_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFunctionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTIONS_IDS, String.class, STD_RNA));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FORM_NAMES, String.class, STD_RNA));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_USER_FORM_NAMES));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_NAMES, String.class, STD_RNA));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_IDS, String.class, STD_RNA));
        //Define object class
        schemaBld.defineObjectClass(oc.build());

        //Auditor responsibilities
        oc = new ObjectClassInfoBuilder();
        oc.setType(AUDITOR_RESPS_OC.getObjectClassValue());

        // The Name is supported attribute
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        // name='userMenuNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, STD_RNA));
        // name='menuIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, STD_RNA));
        // name='userFunctionNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, STD_RNA));
        // name='functionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, STD_RNA));
        // name='formIds' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, STD_RNA));
        // name='formNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, STD_RNA));
        // name='functionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, STD_RNA));
        // name='userFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, STD_RNA));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, STD_RNA));
        // name='readOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTION_NAMES, String.class, STD_RNA));
        // name='readOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_USER_FORM_NAMES, String.class, STD_RNA));
        // name='readOnlyFunctionIds' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTIONS_IDS, String.class, STD_RNA));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FORM_NAMES, String.class, STD_RNA));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_USER_FORM_NAMES));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_NAMES, String.class, STD_RNA));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        oc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_IDS, String.class, STD_RNA));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        
        //Resp object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(RESP_OC.getObjectClassValue()); 
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        
        //Resp object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(DIRECT_RESP_OC.getObjectClassValue()); 
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        
        //directResponsibilities object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(INDIRECT_RESP_OC.getObjectClassValue()); 
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        
        //directResponsibilities object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(APPS_OC.getObjectClassValue()); 
        oc.setContainer(true);
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, STD_RNA));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
    }
    
    /**
     * @param where
     * @param handler
     * @param options
     */
    private void getAuditorResponsibilities(FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {
        String id = null;
        if (options != null && options.getOptions() != null) {
            id = (String) options.getOptions().get("id");
        }

        String respLocation = RESPS_TABLE;
        if (co.getCfg().isActiveAccountsOnly()) {
            respLocation = RESPS_ALL_VIEW;
        }

        List<String> auditorRespList = getResponsibilities(id, respLocation, co.getCfg().isActiveAccountsOnly());
        for (String respName : auditorRespList) {
            ConnectorObject auditorData = getAuditorDataObject(respName);
            if (!handler.handle(auditorData)) {
                break;
            }
        }
    }    
    

    /**
     * 
     * Return Object of Auditor Data
     * 
     * List auditorResps (GO) userMenuNames menuIds userFunctionNames functionIds formIds formNames userFormNames
     * readOnlyFormIds readWriteOnlyFormIds readOnlyFunctionIds readWriteOnlyFunctionIds readOnlyFormNames
     * readOnlyUserFormNames readWriteOnlyFormNames readWriteOnlyUserFormNames
     * @param resp 
     * @return an audit object
     * @throws SQLException 
     * 
     */
     private ConnectorObject getAuditorDataObject(String respName) {
         final String method = "getAuditorDataObject";
         log.info(method);
         // Profile Options used w/SOB and Organization
         String sobOption = "GL Set of Books ID";
         String ouOption = "MO: Operating Unit";

         ConnectorObjectBuilder bld = new ConnectorObjectBuilder();

        String curResp = respName;
        String resp = null;
        String app = null;
        if (curResp != null) {
            StringTokenizer tok = new StringTokenizer(curResp, "||", false);
            if (tok != null && tok.countTokens() > 1) {
                resp = tok.nextToken();
                app = tok.nextToken();
            }
        }
        StringBuffer b = new StringBuffer();

        //one query 
        b.append("SELECT DISTINCT 'N/A' userMenuName, 0 menuID, fffv.function_id,");
        b.append("fffv.user_function_name , ffv.form_id, ffv.form_name, ffv.user_form_name, ");
        b.append("fffv.function_name, ");
        b.append("fffv.parameters  FROM fnd_form_functions_vl fffv, ");
        b.append("fnd_form_vl ffv WHERE fffv.form_id=ffv.form_id(+) ");
        b.append("AND fffv.function_id NOT IN (SELECT action_id FROM fnd_resp_functions frf1 ");
        b.append("WHERE responsibility_id=(SELECT frv.responsibility_id ");
        b.append("FROM fnd_responsibility_vl frv , fnd_application_vl fa WHERE ");
        b.append("frv.application_id=fa.application_id AND  frv.responsibility_name=? ");
        b.append("AND fa.application_name=?) AND rule_type='F') ");
        b.append("AND function_id IN (SELECT function_id FROM fnd_menu_entries fme ");
        b.append("WHERE menu_id NOT IN (SELECT action_id FROM fnd_resp_functions ");
        b.append("WHERE responsibility_id=(SELECT frv.responsibility_id FROM fnd_responsibility_vl frv ");
        b.append(", fnd_application_vl fa WHERE frv.application_id=fa.application_id ");
        b.append("AND  frv.responsibility_name=? ");
        b.append("AND fa.application_name=?) AND rule_type='M')");
        b.append("START WITH menu_id=(SELECT frv.menu_id FROM fnd_responsibility_vl frv ");
        b.append(", fnd_application_vl fa WHERE frv.application_id=fa.application_id ");
        b.append("AND  frv.responsibility_name=? ");
        b.append("AND fa.application_name=?) CONNECT BY prior sub_menu_id=menu_id) ");
        b.append("UNION SELECT DISTINCT user_menu_name userMenuName, menu_id MenuID, ");
        b.append("0 function_id, 'N/A' user_function_name, 0 form_id, 'N/A' form_name, 'N/A' user_form_name, ");
        b.append(" 'N/A' function_name, ");
        b.append("'N/A' parameters  FROM fnd_menus_vl fmv WHERE menu_id IN (");
        b.append("SELECT menu_id FROM fnd_menu_entries fme WHERE menu_id NOT IN (");
        b.append("SELECT action_id FROM fnd_resp_functions WHERE responsibility_id=(");
        b.append("SELECT frv.responsibility_id FROM fnd_responsibility_vl frv, fnd_application_vl fa ");
        b.append("WHERE frv.application_id=fa.application_id AND frv.responsibility_name=? ");
        b.append("AND fa.application_name=?) ");
        b.append("AND rule_type='M') START WITH menu_id=(SELECT frv.menu_id ");
        b.append("FROM fnd_responsibility_vl frv , fnd_application_vl fa WHERE ");
        b.append("frv.application_id=fa.application_id AND  frv.responsibility_name=? ");
        b.append("AND fa.application_name=?) ");
        b.append("CONNECT BY prior sub_menu_id=menu_id) ORDER BY 2,4");
        // one query
        log.info(method + ": SQL statement: " + b.toString());
        log.ok(method + ": Resp: " + curResp);

        PreparedStatement st = null;
        ResultSet res = null;

        List<String> menuIds = new ArrayList<String>();
        List<String> menuNames = new ArrayList<String>();
        List<String> functionIds = new ArrayList<String>();
        List<String> userFunctionNames = new ArrayList<String>();
        List<String> roFormIds = new ArrayList<String>();
        List<String> rwFormIds = new ArrayList<String>();
        List<String> roFormNames = new ArrayList<String>();
        List<String> rwFormNames = new ArrayList<String>();
        List<String> roUserFormNames = new ArrayList<String>();
        List<String> rwUserFormNames = new ArrayList<String>();
        List<String> roFunctionNames = new ArrayList<String>();
        List<String> rwFunctionNames = new ArrayList<String>();
        List<String> roFunctionIds = new ArrayList<String>();
        List<String> rwFunctionIds = new ArrayList<String>();

        // objects to collect all read/write functions and related info
        // which is used later for false positive fix-up
        Map<String, Map<String, Object>> functionIdMap = new HashMap<String, Map<String, Object>>();
        Map<String, Object> attrMap = new HashMap<String, Object>();

        try {

            st = co.getConn().prepareStatement(b.toString());
            st.setString(1, resp);
            st.setString(2, app);
            st.setString(3, resp);
            st.setString(4, app);
            st.setString(5, resp);
            st.setString(6, app);
            st.setString(7, resp);
            st.setString(8, app);
            st.setString(9, resp);
            st.setString(10, app);
            res = st.executeQuery();

            while (res != null && res.next()) {

                String menuName = getColumn(res, 1);
                if (menuName != null && !menuName.equals("N/A")) {
                    menuNames.add(menuName);
                }
                String menuId = getColumn(res, 2);
                if (menuId != null && !menuId.equals("0")) {
                    menuIds.add(menuId);
                }
                String funId = getColumn(res, 3);
                if (funId != null && !funId.equals("0")) {
                    functionIds.add(funId);
                }
                String funName = getColumn(res, 4);
                if (funName != null && !funName.equals("N/A")) {
                    userFunctionNames.add(funName);
                }
                String param = getColumn(res, 9);// column added for parameters
                boolean qo = false;
                if (param != null) {
                    // pattern can be QUERY_ONLY=YES, QUERY_ONLY = YES, QUERY_ONLY="YES",
                    // QUERY_ONLY=Y, etc..
                    Pattern pattern = Pattern.compile("\\s*QUERY_ONLY\\s*=\\s*\"*Y");
                    Matcher matcher = pattern.matcher(param.toUpperCase());
                    if (matcher.find()) {
                        qo = true;
                    }
                }
                if (qo) {
                    String ROfunId = getColumn(res, 3);
                    if (ROfunId != null && !ROfunId.equals("0")) {
                        roFunctionIds.add(ROfunId);
                    }
                    String ROfunctionName = getColumn(res, 8);
                    if (ROfunctionName != null && !ROfunctionName.equals("N/A")) {
                        roFunctionNames.add(ROfunctionName);
                    }
                    String ROformId = getColumn(res, 5);
                    if (ROformId != null && !ROformId.equals("0")) {
                        roFormIds.add(ROformId);
                    }
                    String ROformName = getColumn(res, 6);
                    if (ROformName != null && !ROformName.equals("N/A")) {
                        roFormNames.add(ROformName);
                    }
                    String ROuserFormName = getColumn(res, 7);
                    if (ROuserFormName != null && !ROuserFormName.equals("N/A")) {
                        roUserFormNames.add(ROuserFormName);
                    }
                } else {
                    String RWfunId = getColumn(res, 3);
                    if (RWfunId != null && !RWfunId.equals("0")) {
                        rwFunctionIds.add(RWfunId);
                    }
                    String RWfunctionName = getColumn(res, 8);
                    if (RWfunctionName != null && !RWfunctionName.equals("N/A")) {
                        rwFunctionNames.add(RWfunctionName);
                        attrMap.put("rwFunctionName", RWfunctionName);
                    }
                    String RWformId = getColumn(res, 5);
                    if (RWformId != null && !RWformId.equals("0")) {
                        rwFormIds.add(RWformId);
                        attrMap.put("rwFormId", RWformId);
                    }
                    String RWformName = getColumn(res, 6);
                    if (RWformName != null && !RWformName.equals("N/A")) {
                        rwFormNames.add(RWformName);
                        attrMap.put("rwFormName", RWformName);
                    }
                    String RWuserFormName = getColumn(res, 7);
                    if (RWuserFormName != null && !RWuserFormName.equals("N/A")) {
                        rwUserFormNames.add(RWuserFormName);
                        attrMap.put("rwUserFormName", RWuserFormName);
                    }
                    if (!attrMap.isEmpty()) {
                        functionIdMap.put(RWfunId, new HashMap<String, Object>(attrMap));
                        attrMap.clear();
                    }
                }// end-if (qo)
            }// end-while
            // no catch, just use finally to ensure closes happen
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }

        // Post Process Results looking for false-positive (misidentified rw objects) only if 
        // there are any read only functions (roFunctionIds != null)
        // The results of this query are additional roFunctionIds by following logic
        // in bug#13405.
        if (roFunctionIds != null && roFunctionIds.size() > 0) {
            b = new StringBuffer();
            b.append("SELECT function_id from fnd_compiled_menu_functions ");
            b.append("WHERE menu_id IN ");
            b.append("( SELECT sub_menu_id from fnd_menu_entries ");
            b.append("WHERE function_id IN (");
            b.append(listToCommaDelimitedString(roFunctionIds));
            b.append(") AND sub_menu_id > 0 AND grant_flag = 'Y' ");
            b.append("AND sub_menu_id IN (");
            b.append(listToCommaDelimitedString(menuIds));
            b.append(") )");
            log.info(method + ", SQL statement (Post Processing): " + b.toString());
            try {
                st = co.getConn().prepareStatement(b.toString());
                res = st.executeQuery();
                while (res != null && res.next()) {
                    // get each functionId and use as key to find associated rw objects
                    // remove from rw bucket and place in ro bucket
                    String functionId = getColumn(res, 1);
                    if (functionId != null) {
                        Map<String, Object> idObj = functionIdMap.get(functionId);
                        if (idObj != null) {
                            if (rwFunctionIds.contains(functionId)) {
                                rwFunctionIds.remove(functionId);
                                roFunctionIds.add(functionId);
                            }
                            String rwFunctionName = (String) idObj.get("rwFunctionName");
                            if (rwFunctionNames.contains(rwFunctionName)) {
                                rwFunctionNames.remove(rwFunctionName);
                                roFunctionNames.add(rwFunctionName);
                            }
                            String rwFormId = (String) idObj.get("rwFormId");
                            if (rwFormIds.contains(rwFormId)) {
                                rwFormIds.remove(rwFormId);
                                roFormIds.add(rwFormId);
                            }
                            String rwFormName = (String) idObj.get("rwFormName");
                            if (rwFormNames.contains(rwFormName)) {
                                rwFormNames.remove(rwFormName);
                                roFormNames.add(rwFormName);
                            }
                            String rwUserFormName = (String) idObj.get("rwUserFormName");
                            if (rwUserFormNames.contains(rwUserFormName)) {
                                rwUserFormNames.remove(rwUserFormName);
                                roUserFormNames.add(rwUserFormName);
                            }
                        }// if idObj ! null
                    }// if functionId != null                    
                }// end while

                // no catch, just use finally to ensure closes happen
            } catch (SQLException e) {
                log.error(e, method);
                throw ConnectorException.wrap(e);
            } finally {
                SQLUtil.closeQuietly(res);
                res = null;
                SQLUtil.closeQuietly(st);
                st = null;
            }
        } // end-if roFunctionIds has contents              

        // create objects and load auditor data
        List<String> userFormNameList = new ArrayList<String>(roUserFormNames);
        userFormNameList.addAll(rwUserFormNames);
        List<String> formNameList = new ArrayList<String>(roFormNames);
        formNameList.addAll(rwFormNames);
        List<String> formIdList = new ArrayList<String>(roFormIds);
        formIdList.addAll(rwFormIds);
        List<String> functionNameList = new ArrayList<String>(roFunctionNames);
        functionNameList.addAll(rwFunctionNames);
        List<String> functionIdsList = new ArrayList<String>(roFunctionIds);
        functionIdsList.addAll(rwFunctionIds);

        bld.addAttribute(USER_MENU_NAMES, menuNames);
        bld.addAttribute(MENU_IDS, menuIds);
        bld.addAttribute(USER_FUNCTION_NAMES, userFunctionNames);
        bld.addAttribute(FUNCTION_IDS, functionIdsList);
        bld.addAttribute(READ_ONLY_FUNCTIONS_IDS, roFunctionIds);
        bld.addAttribute(READ_WRITE_ONLY_FUNCTION_IDS, rwFunctionIds);
        bld.addAttribute(FORM_IDS, formIdList);
        bld.addAttribute(READ_ONLY_FORM_IDS, roFormIds);
        bld.addAttribute(READ_WRITE_ONLY_FORM_IDS, rwFormIds);
        bld.addAttribute(FORM_NAMES, formNameList);
        bld.addAttribute(READ_ONLY_FORM_NAMES, roFormNames);
        bld.addAttribute(READ_WRITE_ONLY_FORM_NAMES, rwFormNames);
        bld.addAttribute(USER_FORM_NAMES, userFormNameList);
        bld.addAttribute(READ_ONLY_USER_FORM_NAMES, roUserFormNames);
        bld.addAttribute(READ_WRITE_ONLY_USER_FORM_NAMES, rwUserFormNames);
        bld.addAttribute(FUNCTION_NAMES, functionNameList);
        bld.addAttribute(READ_ONLY_FUNCTION_NAMES, roFunctionNames);
        bld.addAttribute(READ_WRITE_ONLY_FUNCTION_NAMES, rwFunctionNames);
        bld.addAttribute(RESP_NAMES, resp + "||" + app);

        // check to see if SOB/ORGANIZATION is required
        if (co.getCfg().isReturnSobOrgAttrs()) {
            b = new StringBuffer();
            // query for SOB / Organization
            b.append("Select distinct ");
            b.append("decode(fpo1.user_profile_option_name, '");
            b.append(sobOption + "', fpo1.user_profile_option_name||'||'||gsob.name||'||'||gsob.set_of_books_id, '");
            b.append(ouOption + "', fpo1.user_profile_option_name||'||'||hou1.name||'||'||hou1.organization_id)");
            b.append(" from " + co.app() + "fnd_responsibility_vl fr, " + co.app() + "fnd_profile_option_values fpov, "
                    + co.app() + "fnd_profile_options fpo");
            b.append(" , " + co.app() + "fnd_profile_options_vl fpo1, " + co.app() + "hr_organization_units hou1, "
                    + co.app() + "gl_sets_of_books gsob");
            b
                    .append(" where fr.responsibility_id = fpov.level_value and gsob.set_of_books_id = fpov.profile_option_value");
            b
                    .append(" and  fpo.profile_option_name = fpo1.profile_option_name and fpo.profile_option_id = fpov.profile_option_id");
            b
                    .append(" and  fpo.application_id = fpov.application_id and   fpov.profile_option_value = to_char(hou1.organization_id(+))");
            b.append(" and  fpov.profile_option_value = to_char(gsob.set_of_books_id(+)) and   fpov.level_id = 10003");
            b.append(" and  fr.responsibility_name = ?");
            b.append(" order by 1");

            log.info(method + ",SQL statement: " + b.toString());
            log.info(method + ", Resp: " + curResp);

            try {
                st = co.getConn().prepareStatement(b.toString());
                st.setString(1, resp);
                res = st.executeQuery();

                while (res != null && res.next()) {
                    String option = getColumn(res, 1);
                    if (option != null && option.startsWith(sobOption)) {
                        List<String> values = Arrays.asList(option.split("||"));
                        if (values != null && values.size() == 3) {
                            bld.addAttribute(SOB_NAME, values.get(1));
                            bld.addAttribute(SOB_ID, values.get(2));
                        }
                    } else if (option != null && option.startsWith(ouOption)) {
                        List<String> values = Arrays.asList(option.split("||"));
                        if (values != null && values.size() == 3) {
                            bld.addAttribute(OU_NAME, values.get(1));
                            bld.addAttribute(OU_ID, values.get(2));
                        }
                    }
                }
            } catch (SQLException e) {
                log.error(e, method);
                throw ConnectorException.wrap(e);
            } finally {
                SQLUtil.closeQuietly(res);
                res = null;
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }

        if (menuNames != null) {
            Collections.sort(menuNames);
            log.ok(method + "USER_MENU_NAMES " + menuNames.toString());
        }
        if (menuIds != null) {
            Collections.sort(menuIds);
            log.ok(method + "MENU_IDS " + menuIds.toString());
        }
        if (userFunctionNames != null) {
            Collections.sort(userFunctionNames);
            log.ok(method + "USER_FUNCTION_NAMES " + userFunctionNames.toString());
        }
        if (functionIdsList != null) {
            Collections.sort(functionIdsList);
            log.ok(method + "FUNCTION_IDS " + functionIdsList.toString());
        }
        if (roFunctionIds != null) {
            Collections.sort(roFunctionIds);
            log.ok(method + "RO_FUNCTION_IDS " + roFunctionIds.toString());
        }
        if (rwFunctionIds != null) {
            Collections.sort(rwFunctionIds);
            log.ok(method + "RW_FUNCTION_IDS " + rwFunctionIds.toString());
        }
        if (formIdList != null) {
            Collections.sort(formIdList);
            log.ok(method + "APP_ID_FORM_IDS " + formIdList.toString());
        }
        if (roFormIds != null) {
            Collections.sort(roFormIds);
            log.ok(method + "RO_APP_ID_FORM_IDS " + roFormIds.toString());
        }
        if (rwFormIds != null) {
            Collections.sort(rwFormIds);
            log.ok(method + "RW_APP_ID_FORM_IDS " + rwFormIds.toString());
        }
        if (formNameList != null) {
            Collections.sort(formNameList);
            log.ok(method + "FORM_NAMES " + formNameList.toString());
        }
        if (roFormNames != null) {
            Collections.sort(roFormNames);
            log.ok(method + "RO_FORM_NAMES " + roFormNames.toString());
        }
        if (rwFormNames != null) {
            Collections.sort(rwFormNames);
            log.ok(method + "RW_FORM_NAMES " + rwFormNames.toString());
        }
        if (userFormNameList != null) {
            Collections.sort(userFormNameList);
            log.ok(method + "USER_FORM_NAMES " + userFormNameList.toString());
        }
        if (roUserFormNames != null) {
            Collections.sort(roUserFormNames);
            log.ok(method + "RO_USER_FORM_NAMES " + roUserFormNames.toString());
        }
        if (rwUserFormNames != null) {
            Collections.sort(rwUserFormNames);
            log.ok(method + "RW_USER_FORM_NAMES " + rwUserFormNames.toString());
        }
        if (functionNameList != null) {
            Collections.sort(functionNameList);
            log.ok(method + "FUNCTION_NAMES " + functionNameList.toString());
        }
        if (roFunctionNames != null) {
            Collections.sort(roFunctionNames);
            log.ok(method + "RO_FUNCTION_NAMES " + roFunctionNames.toString());
        }
        if (rwFunctionNames != null) {
            Collections.sort(rwFunctionNames);
            log.ok(method + "RW_FUNCTION_NAMES " + rwFunctionNames.toString());
        }
         log.ok(method);
         return bld.build();
     }    
}
