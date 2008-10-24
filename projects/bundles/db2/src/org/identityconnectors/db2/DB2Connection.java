package org.identityconnectors.db2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.DatabaseConnection;
import org.identityconnectors.dbcommon.SQLUtil;

/**
 * Wrapper for jdbc connection to DB2 database.
 * @author kitko
 *
 */
public class DB2Connection extends DatabaseConnection {
	/** Classname of DB2 app driver */
	public final static String APP_DRIVER = "com.ibm.db2.jcc.DB2Driver"; 
	
	public DB2Connection(Connection conn) {
		super(conn);
	}
	
	@Override
	public void test(){
		//We will execute very simple " select 1 from sysibm.dual "
		PreparedStatement st = null;
		ResultSet rs = null;
		try{
			st = getConnection().prepareStatement("select 1 from sysibm.dual");
			rs = st.executeQuery();
		}
		catch(SQLException e){
			if("08001".equals(e.getSQLState()) && -4499 == e.getErrorCode()){
				throw new IllegalStateException("DB2 connection is stale",e);
			}
			else{
				throw new IllegalStateException("Unknown DB2 error while testing connection",e); 
			}
		}
		finally{
			SQLUtil.closeQuietly(rs);
			SQLUtil.closeQuietly(st);
		}
	}
	
	static Connection createDB2Connection(String driver,String host,String port,String subprotocol,String database,String user,GuardedString password){
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("jdbc:").append(subprotocol);
		if(host != null && host.length() > 0){
			urlBuilder.append("://").append(host);
		}
		if(port != null){
			urlBuilder.append(":").append(port);
		}
		urlBuilder.append("/").append(database);
		return SQLUtil.getDriverMangerConnection(driver, urlBuilder.toString(), user, password);
	}
}
