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
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SchemaBuilder;

/**
 * Main implementation of the OracleErp Connector
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class UserSecuringAttrs  {
    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(UserSecuringAttrs.class);
    


    /**
     * The get Instance method
     * @param connector parent
     * @return the account
     */
    public static UserSecuringAttrs getInstance(OracleERPConnector connector) {
       return new UserSecuringAttrs(connector);
    }

    /**
     * used for adminUserId for calling storing procedures
     */
    private int adminUserId = 0;

   
    /**
     * The parent connector
     */
    private OracleERPConnector co;
    
    /**
     * The account
     * @param connector parent
     */
    private UserSecuringAttrs(OracleERPConnector connector) {
        this.co = connector;
    }
    
    /**
     * @param amb
     * @param userName 
     */
    public void buildSecuringAttributesToAccountObject(AttributeMergeBuilder amb, final String userName) {
        if (!co.getCfg().isManageSecuringAttrs()) {
            return;
        }

        List<String> secAttrs = getSecuringAttrs(userName);
        if (secAttrs != null) {
            amb.addAttribute(SEC_ATTRS, secAttrs);
        }
    }
    
    /**
     * Accessor for the adminUserId property
     * @return the adminUserId
     */
    public int getAdminUserId() {
        return adminUserId;
    }

    /**
     * Get the Account Object Class Info
     * @param schemaBld 
     */
    public void schema(SchemaBuilder schemaBld) {
        /*
         Seems to be hidden object class, no contract tests 
         
        //securityGroups object class
        ObjectClassInfoBuilder oc = new ObjectClassInfoBuilder();
        oc.setType(SEC_GROUPS_OC.getObjectClassValue()); 
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());  
        
        //securingAttrs object class
        oc = new ObjectClassInfoBuilder();
        oc.setType(SEC_ATTRS_OC.getObjectClassValue()); 
        oc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class));
        //Define object class
        schemaBld.defineObjectClass(oc.build());
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(DeleteOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(CreateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(UpdateOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(SchemaOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnConnectorOp.class, oc.build());
        schemaBld.removeSupportedObjectClass(ScriptOnResourceOp.class, oc.build());
        */  
    }



    /**
     * @param userId
     */
    public void initAdminUserId(String userId) {
        try {
            adminUserId = new Integer(userId).intValue();
            log.ok("The adminUserId is : {0} ", userId);
        } catch (Exception ex) {
            log.error(ex, "The User Id String {0} is not a number", userId);
        }
    }    
    

    /**
     * 
     * 
     * @param secAttr
     * @param identity
     * @param userId 
     * @throws WavesetException
     * 
     *             Interesting thing here is that a user can have exact duplicate securing attributes, as crazy as that
     *             sounds, they just show up multiple times in the native gui.
     * 
     *             Since there is no available key, we will delete all and add all new ones
     * 
     */
    public void updateUserSecuringAttrs(final Attribute secAttr, String identity) {
        final String method = "updateUserSecuringAttrs";
        log.info(method);

        final String userId=getUserId(co, identity);

        //Convert to list of Strings
        final List<String> secAttrList = convertToListString(secAttr.getValue());

        // get Users Securing Attrs
        List<String> oldSecAttrs = getSecuringAttrs(identity);

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

        log.ok(method);
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
        log.info(method);
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
            final String msg = "Invalid Securing Attribute: " + secAttr;
            log.error(msg);
            throw new ConnectorException(msg);
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
            String sql = "select distinct akattr.ATTRIBUTE_APPLICATION_ID,"
                    + " akattr.ATTRIBUTE_CODE, akattr.DATA_TYPE FROM FND_APPLICATION_VL fndapplvl,"
                    + " AK_ATTRIBUTES_VL akattrvl, AK_ATTRIBUTES akattr WHERE akattrvl.NAME = ?"
                    + " AND fndapplvl.application_name = ? AND akattrvl.attribute_code = akattr.attribute_code "
                    + " AND akattr.ATTRIBUTE_APPLICATION_ID = fndapplvl.application_id";
            
            String msg = "Oracle ERP: sql = ''{0}''";
            log.info(msg, sql);
            

            pstmt = co.getConn().prepareStatement(sql);
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

            sql = "{ call " + co.app()
                    + "icx_user_sec_attr_pub.create_user_sec_attr(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
            cstmt1 = co.getConn().prepareCall(sql);

            msg = "Oracle ERP: api_version_number = " + 1;
            log.ok(msg);
            cstmt1.setInt(1, 1);
            
            msg = "Oracle ERP: init_msg_list = NULL";
            log.ok(msg);
            cstmt1.setNull(2, java.sql.Types.VARCHAR);
            
            msg = "Oracle ERP: simulate = NULL";
            log.ok(msg);
            cstmt1.setNull(3, java.sql.Types.VARCHAR);
            
            msg = "Oracle ERP: commit = NULL";
            log.ok(msg);
            cstmt1.setNull(4, java.sql.Types.VARCHAR);
            
            msg = "Oracle ERP: validation_level = NULL";
            log.ok(msg);
            cstmt1.setNull(5, java.sql.Types.NUMERIC);
            
            // return_status
            cstmt1.registerOutParameter(6, java.sql.Types.VARCHAR);
            //msg_count
            cstmt1.registerOutParameter(7, java.sql.Types.NUMERIC);
            //msg_data
            cstmt1.registerOutParameter(8, java.sql.Types.VARCHAR);

            msg = "Oracle ERP: web_user_id = " + intUserId;
            log.ok(msg);
            cstmt1.setInt(9, intUserId);
            
            msg = "Oracle ERP: attribute_code = " + attributeCode;
            log.ok(msg);
            cstmt1.setString(10, attributeCode);
            
            int attrApplId = 0;
            if (strAttrApplId != null) {
                attrApplId = new Integer(strAttrApplId).intValue();
            }
            msg = "Oracle ERP: attribute_appl_id = " + strAttrApplId;
            log.ok(msg);
            cstmt1.setInt(11, attrApplId);
            
            if (dataType.equalsIgnoreCase("VARCHAR2")) {
                msg = "Oracle ERP: varchar2_value = " + value;
                log.ok(msg);
                cstmt1.setString(12, value);
                
            } else {
                msg = "Oracle ERP: varchar2_value = null";
                log.ok(msg);
                cstmt1.setNull(12, Types.VARCHAR);                
            }

            if (dataType.equalsIgnoreCase("DATE")) {
                msg = "Oracle ERP: date_value = " + value;
                log.ok(msg);
                cstmt1.setTimestamp(13, java.sql.Timestamp.valueOf(value));               
            } else {
                msg = "Oracle ERP: date_value = NULL";
                log.ok(msg);
                cstmt1.setNull(13, java.sql.Types.DATE);                
            }
            if (dataType.equalsIgnoreCase("NUMBER")) {
                if (value != null) {
                    int intValue = new Integer(value).intValue();
                    msg = "Oracle ERP: number_value = " + intValue;
                    log.ok(msg);
                    cstmt1.setInt(14, intValue);                   
                }
            } else {
                msg = "Oracle ERP: number_value = null";
                log.ok(msg);
                cstmt1.setNull(14, java.sql.Types.NUMERIC);
            }
            msg = "Oracle ERP: created_by = " + getAdminUserId();
            log.ok(msg);
            cstmt1.setInt(15, getAdminUserId());
            
            java.sql.Date sqlDate = getCurrentDate();
            msg = "Oracle ERP: creation_date = sysdate";
            log.ok(msg);
            cstmt1.setDate(16, sqlDate);
            
            msg = "Oracle ERP: last_updated_by = " + getAdminUserId();
            log.ok(msg);
            cstmt1.setInt(17, getAdminUserId());
            
            msg = "Oracle ERP: last_updated_date = sysdate";
            log.ok(msg);
            cstmt1.setDate(18, sqlDate);
            
            msg = "Oracle ERP: last_update_login = " + getAdminUserId();
            log.ok(msg);
            cstmt1.setInt(19, getAdminUserId());
            

            cstmt1.execute();
            // cstmt1 closed in finally below
            log.ok(method);

        } catch (SQLException e) {
            final String msg = "SQL Exception:" + e.getMessage();
            log.error(e, msg);
            SQLUtil.rollbackQuietly(co.getConn());
            throw new ConnectorException(msg, e);
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
        log.info(method);
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
            final String msg = "Invalid Securing Attribute: " + secAttr;
            log.error(msg);
            throw new ConnectorException(msg);
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
            String sql = "select distinct akattr.ATTRIBUTE_APPLICATION_ID,"
                    + " akattr.ATTRIBUTE_CODE, akattr.DATA_TYPE FROM FND_APPLICATION_VL fndapplvl,"
                    + " AK_ATTRIBUTES_VL akattrvl, AK_ATTRIBUTES akattr WHERE akattrvl.NAME = ?"
                    + " AND fndapplvl.application_name = ? AND akattrvl.attribute_code = akattr.attribute_code "
                    + " AND akattr.ATTRIBUTE_APPLICATION_ID = fndapplvl.application_id";

            String msg = "execute sql = ''{0}''";
            log.info(msg, sql);

            pstmt = co.getConn().prepareStatement(sql);
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
            sql = "{ call " + co.app() + "icx_user_sec_attr_pub.delete_user_sec_attr(?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
            cstmt1 = co.getConn().prepareCall(sql);

            cstmt1.setInt(1, 1);
            msg = "Oracle ERP: api_version_number = " + 1;
            log.ok(msg);

            cstmt1.setNull(2, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: init_msg_list = NULL";
            log.ok(msg);

            cstmt1.setNull(3, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: simulate = NULL";
            log.ok(msg);

            cstmt1.setNull(4, java.sql.Types.VARCHAR);
            msg = "Oracle ERP: commit = NULL";
            log.ok(msg);

            cstmt1.setNull(5, java.sql.Types.NUMERIC);
            msg = "Oracle ERP: validation_level = NULL";
            log.ok(msg);

            // return_status
            cstmt1.registerOutParameter(6, java.sql.Types.VARCHAR);
            //msg_count
            cstmt1.registerOutParameter(7, java.sql.Types.NUMERIC);
            //msg_data
            cstmt1.registerOutParameter(8, java.sql.Types.VARCHAR);

            cstmt1.setInt(9, intUserId);
            msg = "Oracle ERP: web_user_id = {0}";
            log.ok(msg, intUserId);

            cstmt1.setString(10, attributeCode);
            msg = "Oracle ERP: attribute_code = {0}";
            log.ok(msg, attributeCode);

            int attrApplId = 0;
            if (strAttrApplId != null) {
                attrApplId = new Integer(strAttrApplId).intValue();
            }
            cstmt1.setInt(11, attrApplId);
            msg = "Oracle ERP: attribute_appl_id = {0}";
            log.ok(msg, strAttrApplId);

            if (dataType.equalsIgnoreCase("VARCHAR2")) {
                cstmt1.setString(12, value);
                msg = "Oracle ERP: varchar2_value  = {0}";
                log.ok(msg, value);

            } else {
                cstmt1.setNull(12, Types.VARCHAR);
                msg = "Oracle ERP: varchar2_value = null";
                log.ok(msg);

            }

            if (dataType.equalsIgnoreCase("DATE")) {
                cstmt1.setTimestamp(13, java.sql.Timestamp.valueOf(value));
                msg = "Oracle ERP: date_value  = {0}";
                log.ok(msg, value);

            } else {
                cstmt1.setNull(13, java.sql.Types.DATE);
                msg = "Oracle ERP: date_value = NULL";
                log.ok(msg);
            }
            if (dataType.equalsIgnoreCase("NUMBER")) {
                if (value != null) {
                    int intValue = new Integer(value).intValue();
                    cstmt1.setInt(14, intValue);
                    msg = "Oracle ERP: number_value = " + intValue;
                    log.ok(msg, value);
                }
            } else {
                cstmt1.setNull(14, java.sql.Types.NUMERIC);
                msg = "Oracle ERP: number_value = null";
                log.ok(msg);
            }
            cstmt1.execute();
            // cstmt1 closed in finally below

        } catch (SQLException e) {
            SQLUtil.rollbackQuietly(co.getConn());

            final String msg = "error in statement";
            log.error(e, msg);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(pstmt);
            pstmt = null;
            SQLUtil.closeQuietly(cstmt1);
            cstmt1 = null;
        }
        log.ok(method);
    }

    /**
     * Get Securing Attributes
     * @param userName 
     * @param options 
     * @return list of strings
     */
    private List<String> getSecuringAttrs(String userName) {
        final String method = "getSecAttrs";
        log.info(method);
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuffer b = new StringBuffer();
        //default value
        String pattern = "%";
        b.append("SELECT distinct akattrvl.NAME, fndappvl.APPLICATION_NAME ");
        if (userName != null) {
            b.append(", akwebsecattr.VARCHAR2_VALUE, akwebsecattr.DATE_VALUE, akwebsecattr.NUMBER_VALUE ");
        }
        b.append("FROM " + co.app() + "AK_ATTRIBUTES_VL akattrvl, " + co.app()
                + "FND_APPLICATION_VL fndappvl ");
        // conditionalize including AK_WEB_USER_SEC_ATTR_VALUES in the FROM
        // list, has significant performance impact when present but not
        // referenced.
        b.append(", " + co.app() + "AK_WEB_USER_SEC_ATTR_VALUES akwebsecattr, ");
        b.append(co.app() + "FND_USER fnduser ");

        b.append("WHERE akattrvl.ATTRIBUTE_APPLICATION_ID = fndappvl.APPLICATION_ID ");

        b.append("AND akwebsecattr.WEB_USER_ID = fnduser.USER_ID ");
        b.append("AND akattrvl.ATTRIBUTE_APPLICATION_ID = akwebsecattr.ATTRIBUTE_APPLICATION_ID ");
        b.append("AND akattrvl.ATTRIBUTE_CODE = akwebsecattr.ATTRIBUTE_CODE ");
        b.append("AND fnduser.USER_NAME = ?");
        b.append(" AND akattrvl.NAME LIKE '");
        b.append(pattern);
        b.append("' ");
        b.append("ORDER BY akattrvl.NAME");

        List<String> arrayList = new ArrayList<String>();
        final String sql = b.toString();
        try {
            log.info("execute sql {0}", sql);
            st = co.getConn().prepareStatement(sql);
            st.setString(1, userName.toUpperCase());
            res = st.executeQuery();
            while (res.next()) {

                StringBuffer sb = new StringBuffer();
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
        } catch (SQLException e) {
            final String msg = "could not get Securing attributes";
            log.error(e, msg);
            throw new ConnectorException(msg, e);
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
     * Get Securing Attributes for list of all results
     * @param where 
     * @param handler 
     * @param options 
     */
    public void getSecuringAttrsResult(FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {
        final String method = "getSecAttrs";
        log.info(method);
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuffer b = new StringBuffer();
        //default value
        String pattern = "%";
        if (options != null && options.getOptions() != null) {
            pattern = (String) options.getOptions().get(PATTERN);
        }
        b.append("SELECT distinct akattrvl.NAME, fndappvl.APPLICATION_NAME ");

        b.append("FROM " + co.app() + "AK_ATTRIBUTES_VL akattrvl, " + co.app()
                + "FND_APPLICATION_VL fndappvl ");


        b.append("WHERE akattrvl.ATTRIBUTE_APPLICATION_ID = fndappvl.APPLICATION_ID ");

        b.append(" AND akattrvl.NAME LIKE '");
        b.append(pattern);
        b.append("' ");
        b.append("ORDER BY akattrvl.NAME");

        final String sql = b.toString();
        try {
            log.info("execute sql {0}", sql);
            st = co.getConn().prepareStatement(sql);
            res = st.executeQuery();
            while (res.next()) {

                StringBuffer sb = new StringBuffer();
                sb.append(getColumn(res, 1));
                sb.append("||");
                sb.append(getColumn(res, 2));
                
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(SEC_ATTRS_OC);

                bld.setName(sb.toString());
                bld.addAttribute(NAME, sb.toString());
                
                if (!handler.handle(bld.build())) {
                    break;
                }                
            }
        } catch (SQLException e) {
            final String msg = "could not get Securing attributes";
            log.error(e, msg);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
    }
    
    /**
     * Get securing Groups for appName 
     * @param where 
     * @param handler 
     * @param options 
     * @param appName
     */
    public void getSecGroups(FilterWhereBuilder where, ResultsHandler handler, OperationOptions options) {
        final String method = "getSecGroups";
        log.info( method);
        
        PreparedStatement st = null;
        ResultSet res = null;
        StringBuffer b = new StringBuffer();

        b.append("SELECT distinct fndsecgvl.security_group_name ");
        b.append("FROM " + co.app() + "fnd_security_groups_vl fndsecgvl ");

        try {
            st = co.getConn().prepareStatement(b.toString());
            res = st.executeQuery();
            while (res.next()) {
                
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(SEC_GROUPS_OC);

                String s = getColumn(res, 1);
                bld.setName(s);
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
        if (oclass.equals(OracleERPUtil.SEC_GROUPS_OC)) {
            getSecGroups(where, handler, options);
            return;
        } else if (oclass.equals(OracleERPUtil.SEC_ATTRS_OC)) { //OK
            getSecuringAttrsResult(where, handler,options);
            return;
        } 
        throw new IllegalArgumentException(co.getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
        
    }    
}
