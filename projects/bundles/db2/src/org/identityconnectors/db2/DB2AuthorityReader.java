package org.identityconnectors.db2;

import static org.identityconnectors.db2.DB2Specifics.*;

import java.sql.*;
import java.util.*;

import javax.sql.rowset.CachedRowSet;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.dbcommon.SQLUtil;

import com.sun.rowset.CachedRowSetImpl;

/**
 * Helper utilities for reading authorities
 * @author kitko
 *
 */
class DB2AuthorityReader {
	private Connection conn;
	
    DB2AuthorityReader(Connection adminConn) {
    	Assertions.nullCheck(adminConn,"adminConn");
		this.conn = adminConn;
	}



	/**
     *  Returns a collection of Database type DB2Authority objects
     *  for the passed user.
     */
    Collection<DB2Authority> readDatabaseAuthorities(String user)  throws SQLException{
        String accountIDUC = user.toUpperCase();

        String sql = "SELECT * FROM SYSIBM.SYSDBAUTH WHERE GRANTEE = '"
            + accountIDUC + "' AND GRANTEETYPE = 'U'";

        Collection<DB2Authority> grants = new ArrayList<DB2Authority>();

        ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            if (rs.getString("DBADMAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "DBADM", "", accountIDUC));
            }
            if (rs.getString("CREATETABAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "CREATETAB", "", accountIDUC));
            }
            if (rs.getString("BINDADDAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "BINDADD", "", accountIDUC));
            }
            if (rs.getString("CONNECTAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "CONNECT", "", accountIDUC));
            }
            if (rs.getString("NOFENCEAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "CREATE_NOT_FENCED", "", accountIDUC));
            }
            if (rs.getString("IMPLSCHEMAAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "IMPLICIT_SCHEMA", "", accountIDUC));
            }
            if (rs.getString("LOADAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_DATABASE,
                    "LOAD", "", accountIDUC));
            }
        }
        return grants;
    }
    
    
    
    private ResultSet executeQuery(String sql) throws SQLException {
			Statement statement = null;
	        ResultSet rs = null;
            try{
	    		statement = conn.createStatement();
	            rs = statement.executeQuery(sql);
	            CachedRowSet cr = new CachedRowSetImpl();
	            cr.populate(rs);
	            return cr;
            }
            finally{
	            SQLUtil.closeQuietly(statement);
	            SQLUtil.closeQuietly(rs);
            }
    }
    
    
    /**
     *  Returns a collection of DB2Authority objects representing
     *  all authorities in the database for the user.
     * @throws SQLException 
     */
    Collection<DB2Authority> readAllAuthorities(String user) throws SQLException  {
        Collection<DB2Authority> allAuths = new ArrayList<DB2Authority>();
        allAuths.addAll(readDatabaseAuthorities(user));
        allAuths.addAll(readIndexAuthorities(user));
        allAuths.addAll(readPackageAuthorities(user));
        allAuths.addAll(readSchemaAuthorities(user));
        allAuths.addAll(readServerAuthorities(user));
        allAuths.addAll(readTableAuthorities(user));
        allAuths.addAll(readTablespaceAuthorities(user));
        return allAuths;
    }
    
    /**
     *  Returns a collection of Index type DB2Authority objects
     *  for the passed user.
     */
    Collection<DB2Authority> readIndexAuthorities(String user)  throws SQLException
    {
        String accountIDUC = user.toUpperCase();

        String sql = "SELECT * FROM SYSIBM.SYSINDEXAUTH WHERE GRANTEE = '"
            + accountIDUC + "' AND GRANTEETYPE = 'U'";
        Collection<DB2Authority> grants = new ArrayList<DB2Authority>();
        ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            if (rs.getString("CONTROLAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_INDEX, "CONTROL",
                rs.getString("CREATOR").trim() + "." +  rs.getString("NAME"),
                    accountIDUC));
            }
        }
        return grants;
    }
    
    Collection<DB2Authority> readPackageAuthorities(String user) throws SQLException{
	    String accountIDUC = user.toUpperCase();
	
	    String sql = "SELECT * FROM SYSIBM.SYSPLANAUTH WHERE GRANTEE = '"
	        + accountIDUC + "' AND GRANTEETYPE = 'U'";
	
	    Collection<DB2Authority> grants = new ArrayList<DB2Authority>();
	
	    ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            if (rs.getString("CONTROLAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_PACKAGE,
                    "CONTROL", rs.getString("NAME"),
                    accountIDUC));
            }
            if (rs.getString("BINDAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_PACKAGE,
                    "BIND", rs.getString("NAME"),
                    accountIDUC));
            }
            if (rs.getString("EXECUTEAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_PACKAGE,
                    "EXECUTE", rs.getString("NAME"),
                    accountIDUC));
            }
        }
	    return grants;
    }
    
    
    /**
     *  Returns a collection of Schema type DB2Authority objects
     *  for the passed user.
     */
    Collection<DB2Authority> readSchemaAuthorities(String user) throws SQLException{
        String accountIDUC = user.toUpperCase();

        String sql = "SELECT * FROM SYSIBM.SYSSCHEMAAUTH WHERE GRANTEE = '"
            + accountIDUC + "' AND GRANTEETYPE = 'U'";

        Collection<DB2Authority> grants = new ArrayList<DB2Authority>();

        ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            if (rs.getString("CREATEINAUTH").equals("Y")
                || rs.getString("CREATEINAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_SCHEMA,
                    "CREATEIN", rs.getString("SCHEMANAME"),
                    accountIDUC));
            }
            if (rs.getString("ALTERINAUTH").equals("Y")
                || rs.getString("ALTERINAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_SCHEMA,
                    "ALTERIN", rs.getString("SCHEMANAME"),
                    accountIDUC));
            }
            if (rs.getString("DROPINAUTH").equals("Y")
                || rs.getString("DROPINAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_SCHEMA,
                    "DROPIN", rs.getString("SCHEMANAME"),
                    accountIDUC));
            }
        }
        return grants;
    }
    
    /**
     *  Returns a collection of Server pass through type
     *  DB2Authority objects for the passed user.
     */
    Collection<DB2Authority> readServerAuthorities(String user) throws SQLException{
        String accountIDUC = user.toUpperCase();
        String sql = "SELECT * FROM SYSIBM.SYSPASSTHRUAUTH"
            + " WHERE GRANTEE = '"
            + accountIDUC + "' AND GRANTEETYPE = 'U'";
        Collection<DB2Authority> grants = new ArrayList<DB2Authority>();
        ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            grants.add(new DB2Authority(AUTH_TYPE_SERVER,
                    "PASSTHRU", rs.getString("SERVERNAME"),
                    accountIDUC));
        }
        return grants;
    }
    
    
    /**
     *  Returns a collection of Table type DB2Authority objects
     *  for the passed user.
     */
    Collection<DB2Authority> readTableAuthorities(String user) throws SQLException{
        String accountIDUC = user.toUpperCase();

        String sql = "SELECT * FROM SYSIBM.SYSTABAUTH WHERE GRANTEE = '"
            + accountIDUC + "' AND GRANTEETYPE = 'U'";

        Collection<DB2Authority> grants = new ArrayList<DB2Authority>();

        ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            if (rs.getString("CONTROLAUTH").equals("Y")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "CONTROL", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("ALTERAUTH").equals("Y")
                || rs.getString("ALTERAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "ALTER", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("DELETEAUTH").equals("Y")
                || rs.getString("DELETEAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "DELETE", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("INDEXAUTH").equals("Y")
                || rs.getString("INDEXAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "INDEX", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("INSERTAUTH").equals("Y")
                || rs.getString("INSERTAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "INSERT", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("SELECTAUTH").equals("Y")
                || rs.getString("SELECTAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "SELECT", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("UPDATEAUTH").equals("Y")
                || rs.getString("UPDATEAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "UPDATE", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
            if (rs.getString("REFAUTH").equals("Y")
                || rs.getString("REFAUTH").equals("G")) {
                grants.add(new DB2Authority(AUTH_TYPE_TABLE,
                    "REFERENCES", rs.getString("TCREATOR").trim() + "." + rs.getString("TTNAME").trim(),
                    accountIDUC));
            }
        }
        return grants;
    }
    
    /**
     *  Returns a collection of Tablespace type DB2Authority objects
     *  for the passed user.
     */
    Collection<DB2Authority> readTablespaceAuthorities(String user) throws SQLException{
        String accountIDUC = user.toUpperCase();
        String sql = "SELECT * FROM SYSIBM.SYSTBSPACEAUTH, SYSIBM.SYSTABLESPACES"
            + " WHERE SYSIBM.SYSTBSPACEAUTH.GRANTEE = '"
            + accountIDUC + "' AND SYSIBM.SYSTBSPACEAUTH.GRANTEETYPE = 'U' "
            + "AND (SYSIBM.SYSTBSPACEAUTH.USEAUTH = 'Y' OR SYSIBM.SYSTBSPACEAUTH.USEAUTH = 'G') "
            + "AND SYSIBM.SYSTBSPACEAUTH.TBSPACEID = SYSIBM.SYSTABLESPACES.TBSPACEID";

        Collection<DB2Authority> grants = new ArrayList<DB2Authority>();
        ResultSet rs = executeQuery(sql);
        while (rs.next()) {
            grants.add(new DB2Authority(AUTH_TYPE_TABLESPACE,
                    "USE", rs.getString("TBSPACE"),
                    accountIDUC));
        }
        return grants;
    }
    
    
    
    
   
    
    
}
