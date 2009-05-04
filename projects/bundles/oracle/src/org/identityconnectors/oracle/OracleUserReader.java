package org.identityconnectors.oracle;

import java.math.BigDecimal;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;

import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/** Reads records from DBA_USERS table */
final class OracleUserReader {
    private Connection adminConn;
    
    OracleUserReader(Connection adminConn){
        this.adminConn = OracleConnectorHelper.assertNotNull(adminConn, "adminConn");
    }
    
    /** Test whether user exists, looking at DBA_USERS table */
    boolean userExist(String user){
        //Cannot use PreparedStatement, JVM is crashing !
        StringBuilder query = new StringBuilder("select USERNAME from DBA_USERS where USERNAME = ");
        query.append('\'').append(user).append('\'');
        Statement st = null;
        ResultSet rs = null;
        try{
            st = adminConn.createStatement();
            rs = st.executeQuery(query.toString());
            return rs.next(); 
        }
        catch(SQLException e){
            throw new ConnectorException("Cannot test whether user exist",e);
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    /**
     * Reads records from DBA_USERS matching usernames
     * @param userNames
     * @return collection of {@link UserRecord} records
     */
    Collection<UserRecord> readUserRecords(Collection<String> userNames) throws SQLException{
        StringBuilder query = new StringBuilder("select * from DBA_USERS where USERNAME in(");
        int i = 0, length = userNames.size();
        for(String userName : userNames){
            query.append('\'').append(userName).append('\'');
            if(i != length -1){
                query.append(',');
            }
            i++;
        }
        query.append(')');
        ResultSet rs = null;
        Statement st = null;
        List<UserRecord> result = new ArrayList<UserRecord>(userNames.size());
        try{
            st = adminConn.createStatement();
            rs = st.executeQuery(query.toString());
            while(rs.next()){
                UserRecord record = translateRowToUserRecord(rs);
                result.add(record);
            }
            return result;
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    /**
     * Transform collection of {@link UserRecord} to map with username as key
     * @param records
     * @return
     */
    static Map<String,UserRecord> createUserRecordMap(Collection<UserRecord> records){
        Map<String,UserRecord> map = new HashMap<String, UserRecord>(records.size());
        for(UserRecord record : records){
            map.put(record.getUserName(), record);
        }
        return map;
    }
    
    /**
     * Reads one {@link UserRecord} record for user
     * @param username
     * @return user record or null if no user with username exists
     * @throws SQLException 
     */
    UserRecord readUserRecord(String username) throws SQLException{
        Collection<UserRecord> records = readUserRecords(Collections.singletonList(username));
        if(!records.isEmpty()){
            return records.iterator().next();
        }
        return null;
    }
    

    private UserRecord translateRowToUserRecord(ResultSet rs) throws SQLException {
    	UserRecord.Builder builder = new UserRecord.Builder();
        builder.setCreatedDate(rs.getTimestamp("CREATED"));
        builder.setDefaultTableSpace(rs.getString("DEFAULT_TABLESPACE"));
        builder.setExpireDate(rs.getTimestamp("EXPIRY_DATE"));
        builder.setExternalName(rs.getString("EXTERNAL_NAME"));
        builder.setLockDate(rs.getTimestamp("LOCK_DATE"));
        builder.setProfile(rs.getString("PROFILE"));
        builder.setStatus(rs.getString("ACCOUNT_STATUS"));
        builder.setTemporaryTableSpace(rs.getString("TEMPORARY_TABLESPACE"));
        builder.setUserId(rs.getLong("USER_ID"));
        builder.setUserName(rs.getString("USERNAME"));
        builder.setPassword(rs.getString("PASSWORD"));
        UserRecord record = builder.build();
        return record;
    }
    
    Long readUserTSQuota(String userName, String tableSpace) throws SQLException{
    	BigDecimal bytes = (BigDecimal) SQLUtil.selectSingleValue(adminConn, "select max_bytes from dba_ts_quotas where USERNAME = '" + userName + "'" + " AND TABLESPACE_NAME = '" + tableSpace + "'" );
    	return bytes == null ? null : bytes.longValue();
    }
    
    Long readUserDefTSQuota(String userName) throws SQLException{
    	UserRecord record = readUserRecord(userName);
    	if(record == null){
    		throw new IllegalArgumentException(MessageFormat.format("No user record found for user [{0}]",userName));
    	}
    	return readUserTSQuota(userName, record.getDefaultTableSpace());
    }
    
    Long readUserTempTSQuota(String userName) throws SQLException{
    	UserRecord record = readUserRecord(userName);
    	if(record == null){
    		throw new IllegalArgumentException(MessageFormat.format("No user record found for user [{0}]",userName));
    	}
    	return readUserTSQuota(userName, record.getTemporaryTableSpace());
    }
    
    static OracleAuthentication resolveAuthentication(UserRecord record){
    	if("EXTERNAL".equals(record.getPassword())){
    		return OracleAuthentication.EXTERNAL;
    	}
    	if(record.getExternalName() != null){
    		return OracleAuthentication.GLOBAL;
    	}
    	return OracleAuthentication.LOCAL;
    }

}
