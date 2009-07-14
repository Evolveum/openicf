package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.MSG_ERROR_TEST_USER_EXISTENCE;
import static org.identityconnectors.oracle.OracleMessages.MSG_USER_RECORD_NOT_FOUND;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

/** Reads records from DBA_USERS table */
final class OracleUserReader {
    private final Connection adminConn;
    private final ConnectorMessages cm;
    
    OracleUserReader(Connection adminConn,ConnectorMessages cm){
        this.adminConn = OracleConnectorHelper.assertNotNull(adminConn, "adminConn");
        this.cm = OracleConnectorHelper.assertNotNull(cm, "cm");
    }
    
    /** Test whether user exists, looking at DBA_USERS table */
    boolean userExist(String user){
        //Cannot use PreparedStatement, JVM is crashing !
        String query = "select 1 from DBA_USERS where USERNAME = ?";
        PreparedStatement st = null;
        ResultSet rs = null;
        try{
            st = adminConn.prepareStatement(query);
            st.setString(1, user);
            rs = st.executeQuery();
            return rs.next(); 
        }
        catch(SQLException e){
            throw new ConnectorException(cm.format(MSG_ERROR_TEST_USER_EXISTENCE,null),e);
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    
    /**
     * Reads one {@link UserRecord} record for user
     * @param username
     * @return user record or null if no user with username exists
     * @throws SQLException 
     */
    UserRecord readUserRecord(String username) throws SQLException{
        String query = "select * from DBA_USERS where USERNAME = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try{
	        ps = adminConn.prepareStatement(query);
	        ps.setString(1, username);
	        rs = ps.executeQuery();
	        if(rs.next()){
	        	UserRecord userRecord = translateRowToUserRecord(rs);
	        	return userRecord;
	        }
	        return null;
        }
        finally{
        	SQLUtil.closeQuietly(rs);
        	SQLUtil.closeQuietly(ps);
        }
    }
    

    static UserRecord translateRowToUserRecord(ResultSet rs) throws SQLException {
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
    	String query =  "select max_bytes from dba_ts_quotas where USERNAME = ? and TABLESPACE_NAME = ?";
        final SQLParam userNameParm = new SQLParam("USERNAME", userName, Types.VARCHAR);
        final SQLParam tableSpaceParm = new SQLParam("TABLESPACE_NAME", tableSpace, Types.VARCHAR);
        BigDecimal bytes = (BigDecimal) SQLUtil.selectSingleValue(adminConn, query, userNameParm, tableSpaceParm); 
    	return bytes == null ? null : bytes.longValue();
    }
    
    Long readUserDefTSQuota(String userName) throws SQLException{
    	UserRecord record = readUserRecord(userName);
    	if(record == null){
    		throw new IllegalArgumentException(cm.format(MSG_USER_RECORD_NOT_FOUND, null, userName));
    	}
    	return readUserTSQuota(userName, record.getDefaultTableSpace());
    }
    
    Long readUserTempTSQuota(String userName) throws SQLException{
    	UserRecord record = readUserRecord(userName);
    	if(record == null){
    		throw new IllegalArgumentException(cm.format(MSG_USER_RECORD_NOT_FOUND, null, userName));
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
    
    static boolean isPasswordExpired(UserRecord record){
    	return record.getStatus() != null && record.getStatus().contains("EXPIRED");
    }
    
    static boolean isUserLocked(UserRecord record){
    	return record.getStatus() != null && record.getStatus().contains("LOCKED");
    }

}
