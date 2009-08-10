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
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;


/**
 * The Account User Responsibilities Update
 *  
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class ResponsibilitiesOperations extends Operation {

    /**
     * The column names to get
     */
    public static final Set<String> AUDITOR_ATTRIBUTE_NAMES = CollectionUtil.newCaseInsensitiveSet();

    /**
     * Initialization of the map
     */
    static {       
        AUDITOR_ATTRIBUTE_NAMES.add(USER_MENU_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(MENU_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(USER_FUNCTION_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(FUNCTION_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(RO_FUNCTIONS_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(RW_FUNCTION_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(FORM_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(RO_FORM_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(RW_ONLY_FORM_IDS);
        AUDITOR_ATTRIBUTE_NAMES.add(FORM_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RO_FORM_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RW_FORM_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(USER_FORM_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RO_USER_FORM_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RW_USER_FORM_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(FUNCTION_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RO_FUNCTION_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RW_FUNCTION_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(RESP_NAMES);
        AUDITOR_ATTRIBUTE_NAMES.add(AUDITOR_RESPS);    
    }

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(ResponsibilitiesOperations.class);
    
    /**
     * @param conn
     * @param cfg
     */
    protected ResponsibilitiesOperations(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }

    /**
     * @return the resp table location
     */
    public String getRespLocation() {
        String respLocation = RESPS_TABLE;
        if (cfg.isNewResponsibilityViews()) {
            respLocation = RESPS_ALL_VIEW;
        }
        return respLocation;
    }
    
    
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
        if (!cfg.isNewResponsibilityViews()) {
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
                        log.ok("deleted, (end_dated), responsibility: '" + resp + "' for " + identity);
                    }
                }
                index++;
            }
        }
        // if new key is not in old list add it and remove from respList
        // after adding

            // make copy of array to itereate through because we will be
        // modifying the respList
        List<String> resps = new ArrayList<String>(respList);
        for (String resp : resps) {
            String respKey = getResp(resp, RESP_FMT_KEYS);
            if (!resp.equalsIgnoreCase("") && !oldRespKeys.contains(respKey)) {
                addUserResponsibility(identity, resp, errors);
                respList.remove(resp);
                log.ok("added responsibility: '" + resp + "' for " + identity);
            }
        }

        // if new key is both lists, update it
        String respWithNormalizedDates = null;
        for (String resp : respList) {
            // bug#13889 -  do not update all responsibilities
            //              only update the ones that changed.
            //              Updating all responsibilities every time masks the audit records.
            //              Added check to see if oldResp list 
            //              contains the current entire responsibility
            //              string.
            if (resp != null) {
                log.ok("checking if update required for responsibility: '" + resp + "' for " + identity);
            } else {
                log.ok(" resp=NULL while processing updates");
            }
            // Add/Update resp to user
            if (resp != null && !resp.equalsIgnoreCase("")) {
                // normalize the date string to only contain the date, no time information.
                respWithNormalizedDates = getResp(resp, RESP_FMT_NORMALIZE_DATES);

                if (respWithNormalizedDates != null) {
                    log.ok("respWithNormalizedDates='" + respWithNormalizedDates + "'");
                } else {
                    log.ok("respWithNormalizedDates=null while processing updates");
                }

                // Add/update resp to user if the date normalized responsibility string is not in the old date normalized list.
                if ((oldRespsWithNormalizedDates != null) && respWithNormalizedDates != null
                        && !respWithNormalizedDates.equalsIgnoreCase("")
                        && !oldRespsWithNormalizedDates.contains(respWithNormalizedDates)) {
                    updateUserResponsibility(identity, resp, errors);

                    String msg = "updated responsibility: '" + resp + "' for " + identity;
                    log.ok(msg);
                }
            }
        }

        // bug#16656: delayed error handling for missing responsibilities
        if (!errors.isEmpty()) {
            final String msg = cfg.getMessage(MSG_COULD_NOT_READ);
            StringBuilder iaexceptions = new StringBuilder();
            for (String txt : errors) {
                iaexceptions.append(txt);
                iaexceptions.append(";");
            }
            IllegalArgumentException iae = new IllegalArgumentException(iaexceptions.toString());
            log.error(iae, msg);
            throw new ConnectorException(msg, iae);
        }
        log.info(method + "done");
    }
    
    /**
     * getResponsibilities
     * 
     * @param userName
     *            user id
     * @param respLocation
     *            The responsibilities table
     * @param activeOnly
     *            select active only
     * @return list of strings of multivalued attribute
     */
    public List<String> getResponsibilities(String userName, String respLocation, boolean activeOnly) {

        final String method = "getResponsibilities";
        log.info(method);

        StringBuilder b = new StringBuilder();

        b.append("SELECT fndrespvl.responsibility_name, fndappvl.application_name, fndsecgvl.Security_group_name ");
        // descr may not be available in view or in native ui with new resp views
        // bug#15492 - do not include user tables in query if id not specified, does not return allr responsibilities
        final boolean isDescription = !cfg.isNewResponsibilityViews() || (cfg.isDescrExists() && respLocation.equalsIgnoreCase(RESPS_DIRECT_VIEW));
        if (userName != null) {
            if (isDescription) {
                b.append(", fnduserg.DESCRIPTION");
            }
            b.append(", fnduserg.START_DATE, fnduserg.END_DATE ");
        }
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(cfg.app() + "fnd_application_vl fndappvl, ");
        // bug#15492 - don't include this join if no id is specified.
        if (userName != null) {
            b.append(cfg.app() + "fnd_user fnduser, ");
            b.append(cfg.app() + respLocation + " fnduserg, ");
        }
        b.append(cfg.app() + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        // bug#15492 - don't include this join if no id is specified.
        if (userName != null) {
            b.append("AND fnduser.user_id = fnduserg.user_id ");
            b.append("AND fndrespvl.RESPONSIBILITY_ID = fnduserg.RESPONSIBILITY_ID ");
            b.append("AND fndrespvl.APPLICATION_ID = fnduserg.RESPONSIBILITY_APPLICATION_ID ");
            b.append("AND fnduser.USER_NAME = ? ");
            b.append("AND fndsecgvl.security_group_id = fnduserg.security_group_id ");
        }
        if (activeOnly) {
            if (userName != null) {
                b.append(" AND fnduserg.START_DATE - SYSDATE <= 0 "
                        + "AND (fnduserg.END_DATE IS NULL OR fnduserg.END_DATE - SysDate > 0)");
            }
        }

        PreparedStatement st = null;
        ResultSet res = null;
        List<String> arrayList = new ArrayList<String>();
        final String sql = b.toString();
        try {
            st = conn.prepareStatement(sql);
            if (userName != null) {
                st.setString(1, userName.toUpperCase());
            }
            res = st.executeQuery();
            while (res.next()) {

                // six columns with old resp table, 5 with new views - 
                // no description available
                StringBuilder sb = new StringBuilder();
                String s = getColumn(res, 1); // fndrespvl.responsibility_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 2); // fndappvl.application_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 3); // fndsecgvl.Security_group_name
                sb.append(s);
                if (userName != null) {
                    sb.append("||");
                    if (isDescription) {
                        s = getColumn(res, 4); // fnduserg.DESCRIPTION
                        sb.append(s);
                        sb.append("||");
                        s = normalizeStrDate(getColumn(res, 5)); // fnduserg.START_DATE
                        sb.append(s);
                        sb.append("||");
                        s = normalizeStrDate(getColumn(res, 6)); // fnduserg.END_DATE
                        sb.append(s);
                    } else {
                        s = normalizeStrDate(getColumn(res, 4)); // fnduserg.START_DATE
                        sb.append(s);
                        sb.append("||");
                        s = normalizeStrDate(getColumn(res, 5)); // fnduserg.END_DATE
                        sb.append(s);
                    }
                }
                arrayList.add(sb.toString());
            }
        } catch (Exception e) {
            final String msg = cfg.getMessage(MSG_COULD_NOT_READ);
            log.error(e, msg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }

        log.info(method + " done");
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
    public List<String> getResps(List<String> resps, int respFmt) {
        final String method = "getResps";
        log.info(method + " respFmt=" + respFmt);
        List<String> respKeys = null;
        if (resps != null) {
            respKeys = new ArrayList<String>();
            for (String strResp : resps) {
                String strRespReformatted = getResp(strResp, respFmt);
                respKeys.add(strRespReformatted);
            }
        }
        log.info(method + " done");
        return respKeys;
    } // getResps()  
    

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

        final int count = tok.countTokens();
        if (tok != null && count > 2) {
            StringBuilder key = new StringBuilder();
            key.append(tok.nextToken()); // responsiblity name
            key.append("||");
            key.append(tok.nextToken()); // application name
            key.append("||");
            key.append(tok.nextToken()); // security group name
            if (respFmt != RESP_FMT_KEYS) {
                key.append("||");
                // descr possibly not available in ui version 11.5.10
                if (count > 5) {
                    key.append(tok.nextToken()); // description
                }
                key.append("||");
                key.append(tok.nextToken()); // start_date
                key.append("||");
                key.append(tok.nextToken()); // end_date
            }
            strRespRet = key.toString();
        }
        log.info(method + " done");
        return strRespRet;
    } // getRespWithNormalizeDates()  
    
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
            final String msg = cfg.getMessage(MSG_INVALID_RESPONSIBILITY, resp);
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

        StringBuilder b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, true /* doing an insert */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            st = conn.prepareStatement(sql);
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
                final String msg = cfg.getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
                log.error(e, msg);
                SQLUtil.rollbackQuietly(conn);
                throw new ConnectorException(msg, e);
            }
        } catch (Exception ex) {
            final String msg1 = cfg.getMessage(MSG_COULD_NOT_EXECUTE, ex.getMessage());
            log.error(ex, msg1);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg1, ex);
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
                st = conn.prepareStatement(sql);
                st.execute();

            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    final String msg = cfg.getMessage(MSG_FAILED_ADD_RESP, resp, e.getMessage());
                    errors.add(msg);
                } else {
                    final String msg1 = cfg.getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
                    log.error(e, msg1);
                    SQLUtil.rollbackQuietly(conn);
                    throw new ConnectorException(msg1, e);
                }
            } catch (Exception ex) {
                final String msg1 = cfg.getMessage(MSG_COULD_NOT_EXECUTE, ex.getMessage());
                log.error(ex, msg1);
                SQLUtil.rollbackQuietly(conn);
                throw new ConnectorException(msg1, ex);
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        conn.commit();
        log.info(method + " done");
    }

    /**
     * This method is shared by addUserResponsibility and updateUserResponsibility to build their PL/SQL statements.
     */
    private StringBuilder buildUserRespStatement(String user, String secGroup, String respName, String respAppName,
            String fromDate, String toDate, String description, boolean doInsert) {

        StringBuilder b = new StringBuilder();
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
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl ");
        b.append("WHERE responsibility_name = responsibility_long_name");
        if (respAppName != null) {
            b.append(" AND application_id = ");
            b.append("(SELECT application_id FROM " + cfg.app() + "fnd_application_vl ");
            b.append("WHERE application_name = responsibility_app_name)");
        }
        b.append("; ");
        b.append("SELECT user_id INTO user_id_num ");
        b.append("FROM " + cfg.app() + "fnd_user ");
        b.append("WHERE USER_NAME = user; ");
        b.append("SELECT security_group_id INTO sec_group_id ");
        b.append("FROM " + cfg.app() + "fnd_security_groups_vl ");
        b.append("WHERE SECURITY_GROUP_KEY = security_group; ");

        b.append(cfg.app());
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
            final String msg = cfg.getMessage(MSG_INVALID_RESPONSIBILITY, resp);
            log.error(msg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg);
        }

        StringBuilder b = new StringBuilder();

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
        b.append("FROM " + cfg.app() + "fnd_security_groups fndsecg, " + cfg.app() + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndsecg.security_group_id = fndsecgvl.security_group_id ");
        b.append("AND fndsecgvl.security_group_name = security_group; ");
        b.append("SELECT fndapp.application_short_name, fndresp.responsibility_key, ");
        b.append("fndrespvl.description INTO resp_app, resp_key, description ");
        b.append("FROM " + cfg.app() + "fnd_responsibility_vl fndrespvl, " + cfg.app() + "fnd_responsibility fndresp, ");
        b.append(cfg.app() + "fnd_application_vl fndappvl, " + cfg.app() + "fnd_application fndapp ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        b.append("AND fndappvl.APPLICATION_ID = fndapp.APPLICATION_ID ");
        b.append("AND fndappvl.APPLICATION_NAME = responsibility_app_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_NAME = responsibility_long_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_ID = fndresp.RESPONSIBILITY_ID ");
        b.append("AND fndrespvl.APPLICATION_ID = fndresp.APPLICATION_ID; ");
        b.append(cfg.app() + "fnd_user_pkg.DelResp (user_id, resp_app, resp_key, resp_sec_g_key); ");
        b.append("COMMIT; END;");

        final String sql = b.toString();
        try {
            st = conn.prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_01403) {
                // bug#16656: delay error handling for missing responsibilities
                final String msg = cfg.getMessage(MSG_FAILED_DELETE_RESP, resp, e.getMessage());
                errors.add(msg);                
            } else {
                final String msg = cfg.getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
                log.error(e, msg);
                SQLUtil.rollbackQuietly(conn);
                throw new ConnectorException(msg, e);
            }
        } catch (Exception ex) {
            final String msg1 = cfg.getMessage(MSG_COULD_NOT_EXECUTE, ex.getMessage());
            log.error(ex, msg1);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg1, ex);
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.info(method + " done");
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
            final String msg = cfg.getMessage(MSG_INVALID_RESPONSIBILITY, resp);
            log.error(msg);
            SQLUtil.rollbackQuietly(conn);
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

        StringBuilder b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, false /* not doing an insert, doing an update */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            st = conn.prepareStatement(sql);
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
                final String msg = cfg.getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
                log.error(e, msg);
                SQLUtil.rollbackQuietly(conn);
                throw new ConnectorException(msg, e);
            }
        } catch (Exception ex) {
            final String msg1 = cfg.getMessage(MSG_COULD_NOT_EXECUTE, ex.getMessage());
            log.error(ex, msg1);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg1, ex);
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
                st = conn.prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    final String msg = cfg.getMessage(MSG_FAILED_UPDATE_RESP, resp, e.getMessage());
                    errors.add(msg);                            
                } else {
                    final String msg = cfg.getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
                    log.error(e, msg);
                    SQLUtil.rollbackQuietly(conn);
                    throw new ConnectorException(msg, e);
                }
            } catch (Exception ex) {
                final String msg1 = cfg.getMessage(MSG_COULD_NOT_EXECUTE, ex.getMessage());
                log.error(ex, msg1);
                SQLUtil.rollbackQuietly(conn);
                throw new ConnectorException(msg1, ex);
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.info(method + " done");
    }    
    
    /**
     * @param oclass
     * @param where
     * @param handler
     * @param options
     */
    public void responsibilityRes(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {

        final boolean activeRespsOnly = isActiveRespOnly(options);
        final String id = getOptionId(options);

        String respLocation = null;
        if (oclass.is(DIRECT_RESPS)) { //OK
            respLocation = RESPS_DIRECT_VIEW;
        } else if (oclass.is(INDIRECT_RESPS)) { //OK
            respLocation = RESPS_INDIRECT_VIEW;
        } else {
            respLocation = getRespLocation();
        }

        List<String> objectList = getResponsibilities(id, respLocation, activeRespsOnly);

        for (String respName : objectList) {
            ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
            bld.setObjectClass(oclass);
            bld.setName(respName);
            bld.setUid(respName);
            if (!handler.handle(bld.build())) {
                break;
            }
        }
    }    


    /**
     * @param options
     * @return boolean true/false is active
     */
    public boolean isActiveRespOnly(OperationOptions options) {
        boolean activeRespsOnly = false;
        if ( options != null && options.getOptions() != null) {
            activeRespsOnly = Boolean.TRUE.equals(options.getOptions().get(ACTIVE_RESPS_ONLY)) ? true : false;
        }
        return activeRespsOnly;
    }    
    
    
    /**
     * @param options
     * @return String id from options 
     */
    public String getOptionId(OperationOptions options) {
        String id = null;
        if (options != null && options.getOptions() != null) {
            id = (String) options.getOptions().get("id");
        }
        return id;
    }
}
