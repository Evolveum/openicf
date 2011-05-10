package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.MSG_CUSTOM_CONNECTION_ERROR;
import static org.identityconnectors.oracle.OracleMessages.MSG_DATASOURCE_CONNECTION_ERROR;
import static org.identityconnectors.oracle.OracleMessages.MSG_OCI_CONNECTION_ERROR;
import static org.identityconnectors.oracle.OracleMessages.MSG_ORACLE_PCI_CANNOT_GET_INFO;
import static org.identityconnectors.oracle.OracleMessages.MSG_THIN_CONNECTION_ERROR;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

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
    
    static Connection createDriverConnection(OracleDriverConnectionInfo connInfo, ConnectorMessages cm){
        if("thin".equalsIgnoreCase(connInfo.getProtocol())){
            return createThinDriverConnection(connInfo, cm);
        }
        else if("oci".equalsIgnoreCase(connInfo.getProtocol())){
            return createOciDriverConnection(connInfo, cm);
        }
        return createCustomDriverConnection(connInfo, cm);
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
	
	/**
	 * Here try to get url/host/port from connection.
	 * @param conn
	 * @param connectorMessages 
	 * @return OracleDriverConnectionInfo
	 * @throws ConnectorException when any error occurs 
	 */
	static OracleDriverConnectionInfo parseConnectionInfo(Connection conn, ConnectorMessages cm) throws ConnectorException{
	    OracleDriverConnectionInfo.Builder builder = new OracleDriverConnectionInfo.Builder();
	    try{
	        DatabaseMetaData metaData = conn.getMetaData();
            String url = metaData.getURL();
            String user = metaData.getUserName();
	        builder.setUrl(url);
	        builder.setUser(user);
	        //Now we need to find out driver classname
	        //First try to use oracle driver
	        Class<?> oraClazz = null;
	        final String oracleConnectionClassName = "oracle.jdbc.OracleConnection";
	        try{
	            //Oracle driver must be on classpath also when using datasource on app server 
	            oraClazz = Class.forName(oracleConnectionClassName);
	        }
	        catch(ClassNotFoundException e){
	            //It means driver is not on path, ok other driver is used
	        }
	        if(oraClazz!= null && oraClazz.isInstance(conn)){
	            builder.setDriver(THIN_AND_OCI_DRIVER_CLASSNAME);
	        }
	        if(builder.getDriver() == null){
    	        //We need to find driver classname
    	        List<Driver> drivers = Collections.list(DriverManager.getDrivers());
    	        for(Driver driver : drivers){
    	            if(driver.acceptsURL(url)){
    	                if(builder.getDriver() != null){
    	                    //Here we have problem, we cannot determine which driver to use
    	                    throw new ConnectorException(MessageFormat.format("We cannot determine driver from connection. These two drivers [{0}] accept connection url [{1}]",Arrays.asList(builder.getDriver(), driver.getClass().getName()),url));
    	                }
    	                builder.setDriver(driver.getClass().getName());
    	            }
    	        }
	        }
	        if(builder.getDriver() == null){
	            //This should not happen, but it is better to fail here like to throw NPE in DriverManager.getConnection 
	            throw new ConnectorException(MessageFormat.format("Cannot determine driver classname from connection. No driver accepts url [{0}]", url));
	        }
	        return builder.build();
	    }
	    catch(SQLException e){
	        throw new ConnectorException(cm.format(MSG_ORACLE_PCI_CANNOT_GET_INFO, null),e);
	    }
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
    
    static void execStatemts(Connection conn, List<String> sqls) throws SQLException{
        if(sqls.isEmpty()){
            return;
        }
        Statement st = null;
        try{
            st = conn.createStatement();
            for(String sql : sqls){
                st.executeUpdate(sql);
            }
        }
        finally{
            SQLUtil.closeQuietly(st);
        }
    }
    
    private static Throwable getCause(Exception e){
        return e.getCause() != null ? e.getCause() : e;
    }
    
    private static String getCauseMessage(Exception e){
        return getCause(e).toString();
    }
    

}
