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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;


/**
 * The Account User Responsibilities Update
 *
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class SecuringAttributesOperations extends Operation {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(SecuringAttributesOperations.class);

    /**
     * @param conn
     * @param cfg
     */
    SecuringAttributesOperations(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super(conn, cfg);
    }

    /**
     *
     * @param secAttr
     * @param userName
     *
     *             Interesting thing here is that a user can have exact duplicate securing attributes, as crazy as that
     *             sounds, they just show up multiple times in the native gui.
     *             Since there is no available key, we will delete all and add all new ones
     *
     */
    public void updateUserSecuringAttrs(final Attribute secAttr, String userName) {
        final String method = "updateUserSecuringAttrs";
        log.ok(method);

        final String userId=getUserId(userName);

        //Convert to list of Strings
        final List<String> secAttrList = convertToListString(secAttr.getValue());

        // get Users Securing Attrs
        List<String> oldSecAttrs = getSecuringAttrs(userName);

        // add new attrs
        for (String secAttribute : secAttrList) {
            // Only add if not there already (including value)
            // , otherwise delete from old list
            if (oldSecAttrs.contains(secAttribute)) {
                oldSecAttrs.remove(secAttribute);
            } else {
                addSecuringAttr(userId, secAttribute);
            }
        }

        // delete old attrs
        if (oldSecAttrs != null) {
            for (String secAttribute : oldSecAttrs) {
                deleteSecuringAttr(userId, secAttribute);
            }
        }

        log.ok(method + " done");
    }

    /**
     *  // PROCEDURE CREATE_USER_SEC_ATTR
        // Argument Name           Type            In/Out Default?
        // ------------------------------ ----------------------- ------ --------
        P_API_VERSION_NUMBER        NUMBER          IN
        P_INIT_MSG_LIST     VARCHAR2        IN     DEFAULT
        P_SIMULATE          VARCHAR2        IN     DEFAULT
        P_COMMIT            VARCHAR2        IN     DEFAULT
        P_VALIDATION_LEVEL      NUMBER          IN     DEFAULT
        P_RETURN_STATUS     VARCHAR2        OUT
        P_MSG_COUNT         NUMBER          OUT
        P_MSG_DATA          VARCHAR2        OUT
        P_WEB_USER_ID           NUMBER          IN
        P_ATTRIBUTE_CODE        VARCHAR2        IN
        P_ATTRIBUTE_APPL_ID     NUMBER          IN
        P_VARCHAR2_VALUE        VARCHAR2        IN
        P_DATE_VALUE            DATE            IN
        P_NUMBER_VALUE      NUMBER          IN
        P_CREATED_BY            NUMBER          IN
        P_CREATION_DATE     DATE            IN
        P_LAST_UPDATED_BY       NUMBER          IN
        P_LAST_UPDATE_DATE      DATE            IN
        P_LAST_UPDATE_LOGIN     NUMBER          IN
    */
    private void addSecuringAttr(String userId, String secAttr) {
        final String method = "addUserSecuringAttrs";
        log.ok(method);
        String attributeName = null;
        String applicationName = null;
        String value = null;

        StringTokenizer tok = new StringTokenizer(secAttr, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            attributeName = tok.nextToken();
            if (attributeName != null) {
                attributeName = attributeName.trim();
            }
            applicationName = tok.nextToken();
            if (applicationName != null) {
                applicationName = applicationName.trim();
            }
            value = tok.nextToken();
            if (value != null) {
                value = value.trim();
            }
        } else {
            final String msg1 = getCfg().getMessage(MSG_INVALID_SECURING_ATTRIBUTE, secAttr);
            log.error(msg1);
            throw new ConnectorException(msg1);
        }

        int intUserId = new Integer(userId).intValue();
        ResultSet rs = null; // SQL query on all users, result
        PreparedStatement pstmt = null; // statement that generates the query
        CallableStatement cstmt1 = null;
        try {
            // get attribute_code and attribute_appl_id
            // also need to get type of data value
            String attributeCode = null;
            String strAttrApplId = null;
            String dataType = null;
            String sqlSelect = "select distinct akattr.ATTRIBUTE_APPLICATION_ID,"
                    + " akattr.ATTRIBUTE_CODE, akattr.DATA_TYPE FROM FND_APPLICATION_VL fndapplvl,"
                    + " AK_ATTRIBUTES_VL akattrvl, AK_ATTRIBUTES akattr WHERE akattrvl.NAME = ?"
                    + " AND fndapplvl.application_name = ? AND akattrvl.attribute_code = akattr.attribute_code "
                    + " AND akattr.ATTRIBUTE_APPLICATION_ID = fndapplvl.application_id";

            pstmt = getConn().prepareStatement(sqlSelect);
            pstmt.setString(1, attributeName);
            pstmt.setString(2, applicationName);
            rs = pstmt.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    strAttrApplId = rs.getString(1);
                    attributeCode = rs.getString(2);
                    dataType = rs.getString(3);
                }
                // rs closed in finally below
            }
            // pstmt closed in finally below

            final String sqlCall = "{ call " + getCfg().app()
                    + "icx_user_sec_attr_pub.create_user_sec_attr(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
            cstmt1 = getConn().prepareCall(sqlCall);

            cstmt1.setInt(1, 1);
            cstmt1.setNull(2, java.sql.Types.VARCHAR);
            cstmt1.setNull(3, java.sql.Types.VARCHAR);
            cstmt1.setNull(4, java.sql.Types.VARCHAR);
            cstmt1.setNull(5, java.sql.Types.NUMERIC);

            // return_status
            cstmt1.registerOutParameter(6, java.sql.Types.VARCHAR);
            //msg_count
            cstmt1.registerOutParameter(7, java.sql.Types.NUMERIC);
            //msg_data
            cstmt1.registerOutParameter(8, java.sql.Types.VARCHAR);

            cstmt1.setInt(9, intUserId);
            cstmt1.setString(10, attributeCode);

            int attrApplId = 0;
            if (strAttrApplId != null) {
                attrApplId = new Integer(strAttrApplId).intValue();
            }
            cstmt1.setInt(11, attrApplId);

            if ("VARCHAR2".equalsIgnoreCase(dataType)) {
                cstmt1.setString(12, value);

            } else {
                cstmt1.setNull(12, Types.VARCHAR);
            }

            if ("DATE".equalsIgnoreCase(dataType)) {
                cstmt1.setTimestamp(13, java.sql.Timestamp.valueOf(value));
            } else {
                cstmt1.setNull(13, java.sql.Types.DATE);
            }
            if ("NUMBER".equalsIgnoreCase(dataType)) {
                if (value != null) {
                    int intValue = new Integer(value).intValue();
                    cstmt1.setInt(14, intValue);
                }
            } else {
                cstmt1.setNull(14, java.sql.Types.NUMERIC);
            }
            cstmt1.setInt(15, getCfg().getAdminUserId());
            java.sql.Date sqlDate = getCurrentDate();
            cstmt1.setDate(16, sqlDate);
            cstmt1.setInt(17, getCfg().getAdminUserId());
            cstmt1.setDate(18, sqlDate);
            cstmt1.setInt(19, getCfg().getAdminUserId());


            cstmt1.execute();
            // cstmt1 closed in finally below
            log.ok(method + " done");

        } catch (Exception ex) {
            final String msg1 = getCfg().getMessage(MSG_COULD_NOT_EXECUTE, ex.getMessage());
            log.error(ex, msg1);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(msg1, ex);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(pstmt);
            pstmt = null;
            SQLUtil.closeQuietly(cstmt1);
            cstmt1 = null;
        }
    } // addSecuringAttr()

    /**    PROCEDURE DELETE_USER_SEC_ATTR
    Argument Name           Type            In/Out Default?
    ------------------------------ ----------------------- ------ --------
    P_API_VERSION_NUMBER        NUMBER          IN
    P_INIT_MSG_LIST     VARCHAR2        IN     DEFAULT
    P_SIMULATE          VARCHAR2        IN     DEFAULT
    P_COMMIT            VARCHAR2        IN     DEFAULT
    P_VALIDATION_LEVEL      NUMBER          IN     DEFAULT
    P_RETURN_STATUS     VARCHAR2        OUT
    P_MSG_COUNT         NUMBER          OUT
    P_MSG_DATA          VARCHAR2        OUT
    P_WEB_USER_ID           NUMBER          IN
    P_ATTRIBUTE_CODE        VARCHAR2        IN
    P_ATTRIBUTE_APPL_ID     NUMBER          IN
    P_VARCHAR2_VALUE        VARCHAR2        IN
    P_DATE_VALUE            DATE            IN
    P_NUMBER_VALUE      NUMBER          IN
    */
    private void deleteSecuringAttr(String userId, String secAttr) {
        final String method = "deleteSecuringAttr";
        log.ok(method);
        String attributeName = null;
        String applicationName = null;
        String value = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(secAttr, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            attributeName = tok.nextToken();
            if (attributeName != null) {
                attributeName = attributeName.trim();
            }
            applicationName = tok.nextToken();
            if (applicationName != null) {
                applicationName = applicationName.trim();
            }
            value = tok.nextToken();
            if (value != null) {
                value = value.trim();
            }
        } else {
            final String msg1 = getCfg().getMessage(MSG_INVALID_SECURING_ATTRIBUTE, secAttr);
            log.error(msg1);
            throw new ConnectorException(msg1);
        }
        int intUserId = new Integer(userId).intValue();
        ResultSet rs = null; // SQL query on all users, result
        PreparedStatement pstmt = null; // statement that generates the query
        CallableStatement cstmt1 = null;
        try {
            // get attribute_code and attribute_appl_id
            // also need to get type of data value
            String attributeCode = null;
            String strAttrApplId = null;
            String dataType = null;
            final String sqlSelect = "select distinct akattr.ATTRIBUTE_APPLICATION_ID,"
                    + " akattr.ATTRIBUTE_CODE, akattr.DATA_TYPE FROM FND_APPLICATION_VL fndapplvl,"
                    + " AK_ATTRIBUTES_VL akattrvl, AK_ATTRIBUTES akattr WHERE akattrvl.NAME = ?"
                    + " AND fndapplvl.application_name = ? AND akattrvl.attribute_code = akattr.attribute_code "
                    + " AND akattr.ATTRIBUTE_APPLICATION_ID = fndapplvl.application_id";

            pstmt = getConn().prepareStatement(sqlSelect);
            pstmt.setString(1, attributeName);
            pstmt.setString(2, applicationName);
            rs = pstmt.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    strAttrApplId = rs.getString(1);
                    attributeCode = rs.getString(2);
                    dataType = rs.getString(3);
                }
                // rs closed in finally below
            }
            // pstmt closed in finally below
            final String sqlCall = "{ call " + getCfg().app() + "icx_user_sec_attr_pub.delete_user_sec_attr(?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
            cstmt1 = getConn().prepareCall(sqlCall);

            cstmt1.setInt(1, 1);
            cstmt1.setNull(2, java.sql.Types.VARCHAR);
            cstmt1.setNull(3, java.sql.Types.VARCHAR);
            cstmt1.setNull(4, java.sql.Types.VARCHAR);
            cstmt1.setNull(5, java.sql.Types.NUMERIC);
            // return_status
            cstmt1.registerOutParameter(6, java.sql.Types.VARCHAR);
            //msg_count
            cstmt1.registerOutParameter(7, java.sql.Types.NUMERIC);
            //msg_data
            cstmt1.registerOutParameter(8, java.sql.Types.VARCHAR);
            cstmt1.setInt(9, intUserId);
            cstmt1.setString(10, attributeCode);

            int attrApplId = 0;
            if (strAttrApplId != null) {
                attrApplId = new Integer(strAttrApplId).intValue();
            }
            cstmt1.setInt(11, attrApplId);
            if ("VARCHAR2".equalsIgnoreCase(dataType)) {
                cstmt1.setString(12, value);
            } else {
                cstmt1.setNull(12, Types.VARCHAR);
            }

            if ("DATE".equalsIgnoreCase(dataType)) {
                cstmt1.setTimestamp(13, java.sql.Timestamp.valueOf(value));

            } else {
                cstmt1.setNull(13, java.sql.Types.DATE);
            }
            if ("NUMBER".equalsIgnoreCase(dataType)) {
                if (value != null) {
                    int intValue = new Integer(value).intValue();
                    cstmt1.setInt(14, intValue);
                }
            } else {
                cstmt1.setNull(14, java.sql.Types.NUMERIC);
            }
            cstmt1.execute();
            // cstmt1 closed in finally below

        } catch (Exception e) {
            final String msg1 = getCfg().getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
            log.error(e, msg1);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(msg1, e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(pstmt);
            pstmt = null;
            SQLUtil.closeQuietly(cstmt1);
            cstmt1 = null;
        }
        log.ok(method + " done");
    }

    /**
     * Get Securing Attributes
     * @param userName
     * @return list of strings
     */
    public List<String> getSecuringAttrs(String userName) {
        final String method = "getSecAttrs";
        log.ok(method);
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuilder b = new StringBuilder();
        //default value
        String pattern = "%";
        b.append("SELECT distinct akattrvl.NAME, fndappvl.APPLICATION_NAME ");
        if (userName != null) {
            b.append(", akwebsecattr.VARCHAR2_VALUE, akwebsecattr.DATE_VALUE, akwebsecattr.NUMBER_VALUE ");
        }
        b.append("FROM " + getCfg().app() + "AK_ATTRIBUTES_VL akattrvl, " + getCfg().app()
                + "FND_APPLICATION_VL fndappvl ");
        // conditionalize including AK_WEB_USER_SEC_ATTR_VALUES in the FROM
        // list, has significant performance impact when present but not
        // referenced.
        if (userName !=  null) {
            b.append(", " + getCfg().app() + "AK_WEB_USER_SEC_ATTR_VALUES akwebsecattr, ");
            b.append(getCfg().app() + "FND_USER fnduser ");
        }

        b.append("WHERE akattrvl.ATTRIBUTE_APPLICATION_ID = fndappvl.APPLICATION_ID ");

        if (userName != null) {
            b.append("AND akwebsecattr.WEB_USER_ID = fnduser.USER_ID ");
            b.append("AND akattrvl.ATTRIBUTE_APPLICATION_ID = akwebsecattr.ATTRIBUTE_APPLICATION_ID ");
            b.append("AND akattrvl.ATTRIBUTE_CODE = akwebsecattr.ATTRIBUTE_CODE ");
            b.append("AND fnduser.USER_NAME = ?");
        }
        b.append(" AND akattrvl.NAME LIKE '");
        b.append(pattern);
        b.append("' ");
        b.append("ORDER BY akattrvl.NAME");

        List<String> arrayList = new ArrayList<String>();
        final String sql = b.toString();
        try {
            st = getConn().prepareStatement(sql);
            if ( userName != null) {
                st.setString(1, userName.toUpperCase());
            }
            res = st.executeQuery();
            while (res.next()) {

                StringBuilder sb = new StringBuilder();
                sb.append(getColumn(res, 1));
                sb.append("||");
                sb.append(getColumn(res, 2));
                // get one of three values (one column per type) if id is specified
                // value can be type varchar2, date, number
                if (getColumn(res, 3) != null) {
                    sb.append("||");
                    sb.append(getColumn(res, 3));
                }
                if (getColumn(res, 4) != null) {
                    sb.append("||");
                    sb.append(getColumn(res, 4));
                }
                if (getColumn(res, 5) != null) {
                    sb.append("||");
                    sb.append(getColumn(res, 5));
                }
                arrayList.add(sb.toString());
            }
        } catch (Exception e) {
            final String msg1 = getCfg().getMessage(MSG_COULD_NOT_EXECUTE, e.getMessage());
            log.error(e, msg1);
            SQLUtil.rollbackQuietly(getConn());
            throw new ConnectorException(msg1, e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method + " done");
        return arrayList;
    }
    
    /**
     * Get user id from the user name
     * @param userName
     * @return The UserId string value
     */
     String getUserId(String userName) {
        final String msg = "getUserId ''{0}'' -> ''{1}''";
        String userId = "0";
        log.ok("get UserId for {0}", userName);
        final String sql = "select " + USER_ID + " from " + getCfg().app() + "FND_USER where upper(user_name) = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = getConn().prepareStatement(sql);
            ps.setString(1, userName.toUpperCase());
            rs = ps.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    userId = rs.getString(1);
                }
                // rs closed in finally below
            }
        } catch (Exception e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(ps);
        }
        if (userId == null || userId == "") {
            final String emsg = getCfg().getMessage(MSG_USER_NOT_FOUND, userName);
            log.error(emsg);
        }
        // pstmt closed in finally below
        log.ok(msg, userName, userId);
        return userId;
    }    


}
