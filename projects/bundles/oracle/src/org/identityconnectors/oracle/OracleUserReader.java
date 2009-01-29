package org.identityconnectors.oracle;

import java.sql.*;
import java.util.*;

import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/** Reads record from DBA_USERS table */
class OracleUserReader {
    private Connection adminConn;
    
    OracleUserReader(Connection adminConn){
        this.adminConn = adminConn;
    }
    
    
    boolean userExist(String user){
        //Cannot user PreparedStatement, JVM is crashing !
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
    
    Collection<UserRecord> readUserRecords(Collection<String> userNames){
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
        catch(SQLException e){
            throw ConnectorException.wrap(e);
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    static Map<String,UserRecord> createUserRecordMap(Collection<UserRecord> records){
        Map<String,UserRecord> map = new HashMap<String, UserRecord>(records.size());
        for(UserRecord record : records){
            map.put(record.userName, record);
        }
        return map;
    }
    
    UserRecord readUserRecord(String username){
        Collection<UserRecord> records = readUserRecords(Collections.singletonList(username));
        if(!records.isEmpty()){
            return records.iterator().next();
        }
        return null;
    }
    

    private UserRecord translateRowToUserRecord(ResultSet rs) throws SQLException {
        UserRecord record = new UserRecord();
        record.createdDate = rs.getDate("CREATED");
        record.defaultTableSpace = rs.getString("DEFAULT_TABLESPACE");
        record.expireDate = rs.getDate("EXPIRY_DATE");
        record.externalName = rs.getString("EXTERNAL_NAME");
        record.lockDate = rs.getDate("LOCK_DATE");
        record.profile = rs.getString("PROFILE");
        record.status = rs.getString("ACCOUNT_STATUS");
        record.temporaryTableSpace = rs.getString("TEMPORARY_TABLESPACE");
        record.userId = rs.getLong("USER_ID");
        record.userName = rs.getString("USERNAME");
        return record;
    }

}
