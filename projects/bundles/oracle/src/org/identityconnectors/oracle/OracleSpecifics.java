package org.identityconnectors.oracle;

import java.sql.*;
import java.util.Hashtable;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;

/** Oracle specifics related to JDBC, error codes, constants */
abstract class OracleSpecifics {
    static final String THIN_AND_OCI_DRIVER_CLASSNAME = "oracle.jdbc.driver.OracleDriver";
    static final String OCI_DRIVER = "oci";
    static final String THIN_DRIVER = "thin";
    static final String LISTENER_DEFAULT_PORT = "1521";
    

    private OracleSpecifics() {
    }
    
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
                throw new IllegalStateException("Oracle connection has been killed",e);
            }
            else{
                throw new IllegalStateException("Unknown Oracle error while testing connection",e); 
            }
        }
        finally{
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(st);
        }
    }
    
    /** See following link for more info about url syntax
     * <a href="http://www.oracle.com/technology/tech/java/sqlj_jdbc/htdocs/jdbc_faq.html#05_03">URL syntax</a>
     * @param connInfo
     * @return
     */
    static Connection createThinDriverConnection(OracleDriverConnectionInfo connInfo){
        String url = connInfo.getUrl();
        if(url == null){
            //Build this syntax : jdbc:oracle:thin:scott/tiger@//myhost:1521/orcl
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
        return SQLUtil.getDriverMangerConnection(THIN_AND_OCI_DRIVER_CLASSNAME, url, connInfo.getUser(), connInfo.getPassword());
    }
    
    
    
    static Connection createOciDriverConnection(OracleDriverConnectionInfo connInfo){
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
        return SQLUtil.getDriverMangerConnection(THIN_AND_OCI_DRIVER_CLASSNAME, url, connInfo.getUser(), connInfo.getPassword());
    }
    
    static Connection createDriverConnection(OracleDriverConnectionInfo connInfo){
        return SQLUtil.getDriverMangerConnection(connInfo.getDriver(), connInfo.getUrl(), connInfo.getUser(), connInfo.getPassword());
    }
    
    
    static Connection createDataSourceConnection(String dsName,Hashtable<?,?> env){
        return SQLUtil.getDatasourceConnection(dsName,env);
    }
    
    static Connection createDataSourceConnection(String dsName,String user,GuardedString password,Hashtable<?,?> env){
        return SQLUtil.getDatasourceConnection(dsName,user,password,env);
    }
    
    
    

}
