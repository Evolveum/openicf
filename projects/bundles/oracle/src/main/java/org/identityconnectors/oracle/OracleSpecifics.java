package org.identityconnectors.oracle;

import java.sql.*;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.List;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import static org.identityconnectors.oracle.OracleMessages.*;

/** Oracle specifics related to JDBC, error codes, constants 
 *  More info on syntax of url can be find at :
 *  <a href="http://www.oracle.com/technology/tech/java/sqlj_jdbc/htdocs/jdbc_faq.html#05_03">URL syntax</a>
 * */
final class OracleSpecifics {
    static final String THIN_AND_OCI_DRIVER_CLASSNAME = "oracle.jdbc.driver.OracleDriver";
    static final String OCI_DRIVER = "oci";
    static final String THIN_DRIVER = "thin";
    static final String LISTENER_DEFAULT_PORT = "1521";
    

    private OracleSpecifics() {
    }
    
    /** Tests whether connection is still valid.
     *  It tries to execute <code>select 1 from dual</code> and checks possible error codes on exception
     *  @param connection
     */
    static void testConnection(Connection connection){
        //We will execute very simple " select 1 from dual "
        PreparedStatement st = null;
        ResultSet rs = null;
        try{
            st = connection.prepareStatement("select 1 from dual");
            rs = st.executeQuery();
        }
        catch(SQLException e){
            if("61000".equals(e.getSQLState()) && 28 == e.getErrorCode()){
                throw new IllegalStateException("Oracle connection was killed", e);
            }
            else{
                throw new IllegalStateException("Unknown Oracle error while testing connection", e); 
            }
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    /** 
     * Creates Connection using thin driver url syntax and its propertie
     * @param connInfo
     * @return
     */
    static Connection createThinDriverConnection(OracleDriverConnectionInfo connInfo, ConnectorMessages cm){
        String url = connInfo.getUrl();
        if(url == null){
            //Build this syntax : jdbc:oracle:thin:@//myhost:1521/orcl
            //We will pass user and password in properties
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("jdbc:oracle:thin:@");
            if(connInfo.getHost() != null){
                urlBuilder.append("//").append(connInfo.getHost());
                if(connInfo.getPort() != null){
                    urlBuilder.append(":").append(connInfo.getPort());
                }
            }
            urlBuilder.append("/").append(connInfo.getDatabase());
            url = urlBuilder.toString();
        }
        try{
        	return SQLUtil.getDriverMangerConnection(THIN_AND_OCI_DRIVER_CLASSNAME, url, connInfo.getUser(), connInfo.getPassword());
        }
        catch(RuntimeException e){
        	throw new ConnectorException(cm.format(MSG_THIN_CONNECTION_ERROR, null, getCauseMessage(e)), getCause(e));
        }
    }
    
    private static Throwable getCause(Exception e){
    	return e.getCause() != null ? e.getCause() : e;
    }
    
    private static String getCauseMessage(Exception e){
    	return getCause(e).toString();
    }
    
    
    /**
     * Creates Connection using oci driver syntax
     * @param connInfo
     * @return
     */
    static Connection createOciDriverConnection(OracleDriverConnectionInfo connInfo, ConnectorMessages cm){
        String url = connInfo.getUrl();
        if(url == null){
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("jdbc:oracle:oci:@");
            if(connInfo.getHost() != null){
                urlBuilder.append("//").append(connInfo.getHost());
                if(connInfo.getPort() != null){
                    urlBuilder.append(":").append(connInfo.getPort());
                }
                urlBuilder.append("/").append(connInfo.getDatabase());
            }
            else{
                urlBuilder.append(connInfo.getDatabase());
            }
            url = urlBuilder.toString();
        }
        try{
        	return SQLUtil.getDriverMangerConnection(THIN_AND_OCI_DRIVER_CLASSNAME, url, connInfo.getUser(), connInfo.getPassword());
        }
        catch(RuntimeException e){
        	throw new ConnectorException(cm.format(MSG_OCI_CONNECTION_ERROR, null, getCauseMessage(e)), getCause(e));
        }
    }
    
    /**
     * Creates connection using custom driver and url 
     * @param connInfo
     * @return
     */
    static Connection createCustomDriverConnection(OracleDriverConnectionInfo connInfo, ConnectorMessages cm){
    	try{
    		return SQLUtil.getDriverMangerConnection(connInfo.getDriver(), connInfo.getUrl(), connInfo.getUser(), connInfo.getPassword());
    	}
    	catch(RuntimeException e){
    		throw new ConnectorException(cm.format(MSG_CUSTOM_CONNECTION_ERROR, null, getCauseMessage(e)), getCause(e));
    	}
    }
    
    /**
     * Creates connection using datasource retrieved from JNDI.
     * Environment entries are used to create initial context.
     * @param dsName
     * @param env
     * @return
     */
    static Connection createDataSourceConnection(String dsName,Hashtable<?,?> env, ConnectorMessages cm){
    	try{
    		return SQLUtil.getDatasourceConnection(dsName,env);
    	}
    	catch(RuntimeException e){
    		throw new ConnectorException(cm.format(MSG_DATASOURCE_CONNECTION_ERROR, null, getCauseMessage(e)), getCause(e));
    	}
    }
    
    static Connection createDataSourceConnection(String dsName,String user,GuardedString password,Hashtable<?,?> env, ConnectorMessages cm){
        try{
        	return SQLUtil.getDatasourceConnection(dsName,user,password,env);
        }
    	catch(RuntimeException e){
    		throw new ConnectorException(cm.format(MSG_DATASOURCE_CONNECTION_ERROR, null, getCauseMessage(e)), getCause(e));
    	}
    }

	static void killConnection(Connection systemConn, Connection connToKill) throws SQLException{
	    Object sid = SQLUtil.selectSingleValue(connToKill, "SELECT USERENV('SID') FROM DUAL");
	    Object serialNumber = SQLUtil.selectSingleValue(systemConn, "select serial# from v$session where SID  = " +  sid);
	    String killSql = MessageFormat.format("ALTER SYSTEM KILL SESSION {0} ", "'" + sid + "," + serialNumber + "'");
	    SQLUtil.executeUpdateStatement(systemConn, killSql);
	}
	
	static void execBatchStatemts(Connection conn, List<String> sqls) throws SQLException{
	    if(sqls.isEmpty()){
	        return;
	    }
	    Statement st = null;
	    try{
    	    st = conn.createStatement();
    	    for(String sql : sqls){
    	        st.addBatch(sql);
    	    }
    	    st.executeBatch();
	    }
	    finally{
	        SQLUtil.closeQuietly(st);
	    }
	}
    
    
    

}
