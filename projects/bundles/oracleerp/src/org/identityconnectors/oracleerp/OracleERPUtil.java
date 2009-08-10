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

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
public class OracleERPUtil {


    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(OracleERPUtil.class);

    static final String MSG = "Oracle ERP: ";    
    
    private OracleERPUtil() {
        throw new AssertionError();
    }



    // The predefined attribute names
    static final String USER_ID = "user_id";
    static final String USER_NAME = "user_name";
    static final String OWNER = "owner";
    static final String UNENCRYPT_PWD = "unencrypted_password";
    static final String SESS_NUM = "session_number";
    static final String START_DATE = "start_date";
    static final String END_DATE = "end_date";
    static final String LAST_LOGON_DATE = "last_logon_date";
    static final String DESCR = "description";
    static final String PWD_DATE = "password_date";
    static final String PWD_ACCESSES_LEFT = "password_accesses_left";
    static final String PWD_LIFESPAN_ACCESSES = "password_lifespan_accesses";
    static final String PWD_LIFESPAN_DAYS = "password_lifespan_days";
    static final String EMP_ID = "employee_id";
    static final String EMAIL = "email_address";
    static final String FAX = "fax";
    static final String CUST_ID = "customer_id";
    static final String SUPP_ID = "supplier_id";

    // The supported operations
    static final String CREATE_FNC = "CreateUser";
    static final String UPDATE_FNC = "UpdateUser";

    // Unrelated account attribute names
    static final String EMP_NUM = "employee_number";
    static final String PERSON_FULLNAME = "person_fullname";
    static final String FULL_NAME = "full_name";
    static final String NPW_NUM = "npw_number";
    static final String PERSON_ID = "person_id";    

    //Special attributes

    static final String PERSON_PARTY_ID = "person_party_id";
    static final String EXP_PWD = "expirePassword";

    // The container attributes 

    static final String RESPKEYS = "responsibilityKeys";
    
    // static variable on update to set dates to local server time
    static final String SYSDATE = "sysdate";
    static final String NULL_DATE = "FND_USER_PKG.null_date";
    static final String NULL_CHAR = "FND_USER_PKG.null_char";
    static final String NULL_NUMBER = "FND_USER_PKG.null_number";
    static final String Q = "?";    


    static final String USER_MENU_NAMES = "userMenuNames";
    static final String MENU_IDS = "menuIds";
    static final String USER_FUNCTION_NAMES = "userFunctionNames";
    static final String FUNCTION_IDS = "functionIds";
    static final String FORM_IDS = "formIds";
    static final String FORM_NAMES = "formNames";
    static final String FUNCTION_NAMES = "functionNames";
    static final String USER_FORM_NAMES = "userFormNames";
    static final String RO_FORM_IDS = "readOnlyFormIds";
    static final String RW_ONLY_FORM_IDS = "readWriteOnlyFormIds";
    static final String RO_FORM_NAMES = "readOnlyFormNames";
    static final String RO_FUNCTION_NAMES = "readOnlyFunctionNames";
    static final String RO_USER_FORM_NAMES = "readOnlyUserFormNames";
    static final String RO_FUNCTIONS_IDS = "readOnlyFunctionIds";
    static final String RW_FORM_NAMES = "readWriteOnlyFormNames";
    static final String RW_USER_FORM_NAMES = "readWriteOnlyUserFormNames";
    static final String RW_FUNCTION_NAMES = "readWriteOnlyFunctionNames";
    static final String RW_FUNCTION_IDS = "readWriteOnlyFunctionIds";

    /** The search pattern operation options key */
    static final String PATTERN = "searchPattern";
    
    /** Default OracleRP driver */
    static final String DEFAULT_DRIVER="oracle.jdbc.driver.OracleDriver";
    
    /** Default OracleRP admin user */ 
    static final String DEFAULT_USER_NAME = "APPS"; // Default user name           
      
    /**
     * Name attribute, old name attribute
     */
    static final String NAME = "name";

    /**
     * Timeout
     */
    public static final int ORACLE_TIMEOUT = 1800;
    
    /**
     * Default customer
     */
    public static final String CUST = "CUST";
    
    // Auditor Data Object
    static final String AUDITOR_RESPS = "auditorResps";
    static final String AUDITOR_RESP = "auditorResp";
    static final String AUDITOR_OBJECT = "auditorObject";

    
    // format flags for processing responsibilities strings, used by getResp() and getResps() methods
    static final int RESP_FMT_KEYS = 0; // return responsibilities with keys only.
    static final int RESP_FMT_NORMALIZE_DATES = 1; // return whole responsibilities with time data removed from date columns.
    static final int ORA_01403 = 1403;
    
    // Validate messages constants
    static final String MSG_USER_BLANK = "msg.user.blank";
    static final String MSG_PASSWORD_BLANK = "msg.password.blank";
    static final String MSG_HOST_BLANK = "msg.host.blank";
    static final String MSG_PORT_BLANK = "msg.port.blank";
    static final String MSG_DATABASE_BLANK = "msg.database.blank";
    static final String MSG_DRIVER_BLANK = "msg.driver.blank";
    static final String MSG_DRIVER_NOT_FOUND = "msg.jdbc.driver.not.found";
    static final String MSG_UNKNOWN_OPERATION_TYPE = "msg.unknown.operation.type";
    static final String MSG_HR_LINKING_ERROR="msg.hr.linking.error";
    static final String MSG_USER_NOT_FOUND="msg.user.not.found";
    static final String MSG_COULD_NOT_ENABLE_USER="msg.could.not.enable.user";
    static final String MSG_COULD_NOT_DISABLE_USER="msg.could.not.disable.user";
    static final String MSG_COULD_NOT_RENAME_USER="msg.could.not.rename.user";
    static final String MSG_ACCOUNT_NOT_UPDATE="msg.account.not.update";
    static final String MSG_ACCOUNT_NAME_REQUIRED="msg.acount.name.required";
    static final String MSG_ACCOUNT_UID_REQUIRED="msg.acount.uid.required";
    static final String MSG_COULD_NOT_READ = "msg.could.not.read";
    static final String MSG_ACCOUNT_NOT_CREATE="msg.account.not.create";    
    static final String MSG_ACCOUNT_NOT_DELETE="msg.account.not.delete";
    static final String MSG_ACCOUNT_NOT_READ="msg.account.not.read";
    static final String MSG_ACCOUNT_OBJECT_CLASS_REQUIRED = "msg.acount.object.class.required";
    static final String MSG_AUTH_FAILED = "msg.auth.op.failed";
    static final String MSG_INVALID_RESPONSIBILITY = "msg.invalid.responsibility";
    static final String MSG_COULD_NOT_EXECUTE = "msg.could.not.execute";
    static final String MSG_FAILED_ADD_RESP = "msg.failed.add.responsibility";
    static final String MSG_FAILED_DELETE_RESP = "msg.failed.delete.responsibility";
    static final String MSG_FAILED_UPDATE_RESP = "msg.failed.update.responsibility";
    static final String MSG_INVALID_SECURING_ATTRIBUTE = "msg.invalid.securing.attribute";

    
    
    /**
     * object class name definitions
     * responsibilities, responsibilityNames, applications, securityGroups, auditorResps
     */
    
    static final String RESPS = "responsibilities";
    /** Object Class name */
    public static final ObjectClass RESP_OC = new ObjectClass(RESPS);    

    static final String DIRECT_RESPS = "directResponsibilities";
    /** Object Class name */
    public static final ObjectClass DIRECT_RESP_OC = new ObjectClass(DIRECT_RESPS);    

    static final String INDIRECT_RESPS = "indirectResponsibilities";
    /** Object Class name */
    public static final ObjectClass INDIRECT_RESP_OC = new ObjectClass(INDIRECT_RESPS); 

    static final String RESP_NAMES = "responsibilityNames";
    static final String RESP_NAME = "responsibility";
    /** Object Class name */
    public static final ObjectClass RESP_NAMES_OC = new ObjectClass(RESP_NAMES);        
    
    static final String APPS = "applications";
    static final String APP = "application";
    /** Object Class name */
    public static final ObjectClass APPS_OC = new ObjectClass(APPS);    
    
    static final String SEC_GROUPS = "securityGroups";
    //static final String SEC_GROUP = "securityGroup";
    /** Object Class name */
    public static final ObjectClass SEC_GROUPS_OC = new ObjectClass(SEC_GROUPS); 
    
    static final String SEC_ATTRS = "securingAttrs";
    /** Object Class name */
    public static final ObjectClass SEC_ATTRS_OC = new ObjectClass(SEC_ATTRS); 
    

    /**
     * Auditor responsibilities has menus, forms, functions, 
     * Auditor attributes: activeRespsOnly
     */
    public static final ObjectClass AUDITOR_RESPS_OC =  new ObjectClass(AUDITOR_RESPS);
    
    static final String ACTIVE_RESPS_ONLY = "activeRespsOnly";
    static final String SOB_NAME = "setOfBooksName";
    static final String SOB_ID = "setOfBooksId";
    static final String OU_NAME = "organizationalUnitName";
    static final String OU_ID = "organizationalUnitId";

    // new version 11.5.10 does not use responsibility table, it uses 2 new views
    static final String RESPS_TABLE = "fnd_user_resp_groups";    
    static final String RESPS_DIRECT_VIEW = "fnd_user_resp_groups_direct";
    static final String RESPS_INDIRECT_VIEW = "fnd_user_resp_groups_indirect";
    static final String RESPS_ALL_VIEW = "fnd_user_resp_groups_all";    


    /**
     * @param conn connection 
     * @param cfg configuration
     * @param userName
     * @return The UserId string value
     * @throws SQLException
     */
    public static String getUserId(OracleERPConnection conn, OracleERPConfiguration cfg, String userName) {
        final String msg = "getUserId ''{0}'' -> ''{1}''";
        String userId = null;
        log.ok("get UserId for {0}", userName);
        final String sql = "select " + USER_ID + " from " + cfg.app() + "FND_USER where upper(user_name) = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
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
            final String emsg = cfg.getMessage(MSG_USER_NOT_FOUND, userName);
            log.error(emsg);
            throw new IllegalStateException(emsg);
        }    
        // pstmt closed in finally below
        log.ok(msg, userName, userId);
        return userId;
    }    

    /**
     * @param con conector 
     * @param attrs attributes
     * @return the identity
     */
    public static String getId(Set<Attribute> attrs) {
        final String method = "getId";
        log.ok(method);
        final Uid uid = AttributeUtil.getUidAttribute(attrs);
        return uid.getUidValue();
    }        
    

    
    /**
     * Get default attributes to get from schema
     * @param ais attribute info set
     * @return set of the 
     */
    public static Set<String> getDefaultAttributesToGet(Set<AttributeInfo> ais) {
        Set<String> ret = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : ais) {
            if (ai.isReturnedByDefault()) {
                ret.add(ai.getName());
            }
        }
        return ret;
    }

    /**
     * @param schema
     * @param type
     * @return The attribute info set
     */
    public static Set<AttributeInfo> getAttributeInfos(Schema schema, String type) {
        ObjectClassInfo oci = schema.findObjectClassInfo(type);
        if  ( oci == null) {
            throw new IllegalStateException("Unknown object type");
        }
        return oci.getAttributeInfo();
    }    
    
    /**
     * Get default attributes to get from schema
     * @param ais attribute info set
     * @return set of the 
     */
    public static Set<String> getCreatableAttributes(Set<AttributeInfo> ais) {
        Set<String> ret = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : ais) {
            if (ai.isCreateable()) {
                ret.add(ai.getName());
            }
        }
        return ret;
    }    
    
    /**
     * Get default attributes to get from schema
     * @param ais attribute info set
     * @return set of the 
     */
    public static Set<String> getRequiredAttributes(Set<AttributeInfo> ais) {
        Set<String> ret = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : ais) {
            if (ai.isRequired()) {
                ret.add(ai.getName());
            }
        }
        return ret;
    }
    
    /**
     * Get default attributes to get from schema
     * @param ais attribute info set
     * @return set of the 
     */
    public static Set<String> getUpdatableAttributes(Set<AttributeInfo> ais) {
        Set<String> ret = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : ais) {
            if (ai.isUpdateable()) {
                ret.add(ai.getName());
            }
        }
        return ret;
    }
    
    
    /**
     * Get default attributes to get from schema
     * @param ais attribute info set
     * @return set of the 
     */
    public static Set<String> getReadableAttributes(Set<AttributeInfo> ais) {
        Set<String> ret = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : ais) {
            if (ai.isReadable()) {
                ret.add(ai.getName());
            }
        }
        return ret;
    }
        
    /**
     * Get default attributes to get from schema
     * @param ais attribute info set
     * @return set of the 
     */
    public static Set<String> getAllAttributes(Set<AttributeInfo> ais) {
        Set<String> ret = CollectionUtil.newCaseInsensitiveSet();
        for (AttributeInfo ai : ais) {
            ret.add(ai.getName());
        }
        return ret;
    }    
    /**
     * Get attributes to get list
     * @param options
     * @param ais Attribute info set 
     * @return set of attribute names to get or empty set, when not defined
     */
    public static Set<String> getAttributesToGet(OperationOptions options, Set<AttributeInfo> ais) {
        final String msg = "getAttributesToGet";
        Set<String> allAttributes = getAllAttributes(ais);
        Set<String> _attrToGet;
        if(options != null && options.getAttributesToGet() != null) {
            _attrToGet = CollectionUtil.newCaseInsensitiveSet();
            for (String toGet : options.getAttributesToGet()) {
                if(allAttributes.contains(toGet) ) {
                _attrToGet.add(toGet);
                } else {
                    throw new IllegalArgumentException("Invalid attribute to get"+toGet);
                }
            }
        } else {
            _attrToGet = getDefaultAttributesToGet(ais); 
        }
        //Make sure, that the name and uid are always present
        _attrToGet.add(Name.NAME);
        _attrToGet.add(Uid.NAME);
        log.ok(msg);
        return _attrToGet;
    }    
    
    /**
     * Get The personId from employeNumber or NPW number
     * @param name user identity
     * @param conn connector
     * @param cfg configuration
     * @param attrs attributes 
     * @return personid the id of the person
     */
    public static Integer getPersonId(String name, OracleERPConnection conn, OracleERPConfiguration cfg, Set<Attribute> attrs) {  
        log.ok("getPersonId for userId: ''{0}''", name);
        Integer ret = null;
        int num = 0;
        String columnName = null;
        final Attribute empAttr = AttributeUtil.find(EMP_NUM, attrs);
        final Attribute npwAttr = AttributeUtil.find(NPW_NUM, attrs);
        if ( empAttr != null ) {
            num = AttributeUtil.getIntegerValue(empAttr);
            columnName = EMP_NUM;   
            log.ok("{0} present with value ''{1}''",columnName, num);
        } else if ( npwAttr != null ){
            num = AttributeUtil.getIntegerValue(npwAttr);
            columnName = NPW_NUM;            
            log.ok("{0} present with value ''{1}''",columnName, num);
        } else {
            log.ok("neither {0} not {1} attributes for personId are present", EMP_NUM, NPW_NUM);
            return null;
        }
        
        log.ok("clomunName ''{0}''", columnName);
        final String sql = "select "+PERSON_ID+" from "+cfg.app()+"PER_PEOPLE_F where "+columnName+" = ?";
        ResultSet rs = null; // SQL query on person_id
        PreparedStatement ps = null; // statement that generates the query
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, num);
            ps.setQueryTimeout(OracleERPUtil.ORACLE_TIMEOUT);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            log.ok("Oracle ERP: PERSON_ID return from {0} = {1}", sql, ret);
            
            if( ret == null ) {
                final String msg =  cfg.getMessage(MSG_HR_LINKING_ERROR, num, name);
                log.error(msg);
                throw new ConnectorException(msg);
            }

            log.ok("getPersonId for userId: ''{0}'' -> ''{1}''", name, ret);
            return ret;
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(rs);
            rs = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
    }
    
        
    
    /**
     * @param dateString
     * @return the timestamp
     */
    public static Timestamp stringToTimestamp(final String dateString) {
        Timestamp tms;
        try {
            tms = Timestamp.valueOf(dateString);
        } catch (IllegalArgumentException expected) {
            try {
                tms = new Timestamp(new Long(dateString));
            } catch (Exception e) {
                tms = new Timestamp(stringToDate(dateString).getTime());
            }
        }
        return tms;
    }
    
    /**
     * @param dateString
     * @return the timestamp
     */
    public static Date stringToDate(final String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;

        try {
            date = sdf.parse(dateString);
        } catch (ParseException e1) {
            try {
                date = Timestamp.valueOf(dateString);
            } catch (IllegalArgumentException expected) {
                try {
                    date = DateFormat.getDateInstance().parse(dateString);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return date;
    }
    
    /**
     * @param sqlSelect 
     * @param whereAnd
     * @return and string
     */
    public static String whereAnd(String sqlSelect, String whereAnd) {
        int iofw = sqlSelect.toUpperCase().indexOf("WHERE");
        return (iofw == -1) ? sqlSelect + " WHERE " + whereAnd : sqlSelect.substring(0, iofw) + "WHERE ( "+sqlSelect.substring(iofw + 5) +" ) AND ( " + whereAnd + " )";
    }
    
    /**
     * Only accounts where clause
     */
    public static final String ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE =
        "(START_DATE - SYSDATE <= 0) AND ((END_DATE IS NULL) OR (END_DATE - SYSDATE > 0))";
    

    /**
     * Read one row from database result set and convert a columns to attribute set.  
     * @param resultSet database data
     * @return The transformed attribute set
     * @throws SQLException 
     */
    public static Map<String, SQLParam> getColumnValues(ResultSet resultSet) throws SQLException {
        Assertions.nullCheck(resultSet,"resultSet");
        Map<String, SQLParam> ret = CollectionUtil.<SQLParam>newCaseInsensitiveMap();
        final ResultSetMetaData meta = resultSet.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            final String name = meta.getColumnName(i);
            SQLParam param = null;
            int sqlType = meta.getColumnType(i);
            if ( name.toLowerCase().endsWith("date")) {
                //All dates needs to be fetched as a dates
                param = SQLUtil.getSQLParam(resultSet, i, name, Types.TIMESTAMP);
            } else {
                param = SQLUtil.getSQLParam(resultSet, i, name, sqlType);                            
            }
            
            
            ret.put(name, param);
        }
        return ret;
    }    
    
    /**
     * Get a string from a result set, trimming trailing blanks.
     * 
     * @param result
     * @param col
     * @return the resulted string
     * @throws java.sql.SQLException
     */
    public static String getColumn(ResultSet result, int col) throws java.sql.SQLException {
        String s = result.getString(col);
        if (s != null)
            s = s.trim();
        return s;
    }
    
    /**
     * @param name
     * @param columnValues
     * @return long  value
     */
    public static Long extractLong(String name, Map<String, SQLParam> columnValues) {
        //enable date
        final SQLParam param = columnValues.get(name);
        if (param == null) {
            return null;
        }
        final Object value = param.getValue();
        if (value == null) {
            return null;
        }
        Long ret = null;
        if ( value instanceof String) {
            ret = new Long((String) value);
        } else if ( value instanceof Long) {
            ret = (Long) value;
        } else if ( value instanceof BigInteger) {
            ret = ((BigInteger) value).longValue();
        } else {
            try {
                ret = new Long(value.toString());
            } catch (NumberFormatException e) {
                // expected
            }
        }
        return ret;
    }  
    
    /**
     * @param name
     * @param columnValues
     * @return date value
     */
    public static Date extractDate(String name, Map<String, SQLParam> columnValues) {
        //enable date
        final SQLParam param = columnValues.get(name);
        if (param == null) {
            return null;
        }
        final Object value = param.getValue();
        if (value == null) {
            return null;
        }
        Date ret = null;
        if (value instanceof String) {
           ret = stringToDate((String) value);
        } else if (value instanceof Date) {
           ret = (Date) value; 
        }
        return ret;
    }  
  
    
    /**
     * Takes a date in string format and returns a normalized version of the date i.e., removing time data. null dates
     * are returned as string "null".
     * @param strDate string date
     * @return normalized date
     */
    public static String normalizeStrDate(String strDate) {
        final String method = "normalizeStrDate ''{0}'' -> ''{1}''";      
        String retDate = strDate;
        if ((strDate == null) || strDate.equalsIgnoreCase("null")) {
            retDate = null;
        } else if (strDate.length() == 10) {
            retDate = strDate;
        } else if (strDate.length() > 10) {
            retDate = strDate.substring(0, 10); // truncate to only date i.e.,yyyy-mm-dd 
        }
        log.ok(method, strDate, retDate);
        return retDate;
    } // normalizeStrDate()
    
    /**
     * @return sql date type
     * 
     */
    public static java.sql.Date getCurrentDate() {
        final String method = "getCurrentDate ''{0}''";      
        Calendar rightNow = Calendar.getInstance();
        java.util.Date utilDate = rightNow.getTime();
        log.ok(method, utilDate);  
        return new java.sql.Date(utilDate.getTime());
    }
    

    /**
     * Add a quoted string to a SQL statement we're building in a buffer. If the attribute might be an integer, then
     * call addAttributeValue() instead, which factors in the syntax of the attribute when determining whether or not to
     * quote the value.
     * @param b buffer
     * @param s string to be quoted
     */
    public static void addQuoted(StringBuilder b, String s) {
        final String method = "addQuoted ''{0}''";
        log.ok(method, s);
        b.append("'");
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\'')
                    b.append("''");
                else
                    b.append(ch);
            }
        }
        b.append("'");
    }

    /**
     * @param userParamMap map
     * @param paramName param Name
     * @return the String parameter
     */
    public static String getStringParamValue(final Map<String, SQLParam> userParamMap, final String paramName) {
        final String method = "getStringParamValue ''{0}''";
        log.ok(method, paramName);
        final SQLParam param = userParamMap.get(paramName);
        if (param == null || !(param.getValue() instanceof String))
            return null;
        final String ret = (String) param.getValue();
        return ret;
    }
    

    /**
     * Convert to strings
     * @param from 
     * @return list of strings
     */
    public static List<String> convertToListString(List<Object> from) {
        final String method = "convertToListString";
        log.ok(method);
        //Convert to list of Strings
        final List<String> ret = new ArrayList<String>();
        for (Object obj : from) {
            ret.add(obj.toString());
        }    
        return ret;                    
    }    
    
    /**
     * @param list
     * @return The string value
     */
    public static String listToCommaDelimitedString(List<String> list) {
        final String method = "listToCommaDelimitedString";
        log.ok(method);
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (String val : list) {
            if(!first) ret.append(",");
            ret.append(val);
            first = false;
        }
        return ret.toString();        
    }
    
    /**
     * @param where
     * @return the id from the current statement 
     */
    public static String getFilterId(FilterWhereBuilder where) {
        String filterId = null;
        if (where != null) {
            for (SQLParam sqlp : where.getParams()) {
                if (sqlp.getName().equalsIgnoreCase(USER_NAME)) {
                    filterId = sqlp.getValue().toString();
                }
            }
        }
        return filterId;
    }    

    // The SQL call update function SQL template
    private static final String FND_USER_CALL = "fnd_user_pkg.";

    /**
     * @param schemaId
     * @param fn
     * @param body
     * @return The SQL string
     */
    public static String createCallSQL(String schemaId, final String fn, StringBuilder body) {
        return "{ call " + schemaId + FND_USER_CALL + fn + " ( "+ body.toString() + " ) }";
    }


}
