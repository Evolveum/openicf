package org.identityconnectors.oracle;

import java.sql.*;
import java.util.*;

import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/** Reads roles and privileges for user */
final class OracleRolePrivReader {
    
    private final Connection conn;
    
    OracleRolePrivReader(Connection conn) {
        super();
        this.conn = conn;
    }
    
    /**
     * Reads roles for user using DBA_ROLE_PRIVS table
     * @param userName
     * @return list of associated user roles, not recursive
     */
    List<String> readRoles(String userName){
        List<String> roles = new ArrayList<String>();
        try {
            final SQLParam userNameParam =  new SQLParam("Grantee", userName);
            final List<Object[]> selectRows = SQLUtil.selectRows(conn, "select GRANTED_ROLE from DBA_ROLE_PRIVS where Grantee = ?", userNameParam);
            for(Object[] row : selectRows){
                roles.add((String) row[0]);
            }
            return roles;
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    /**
     * Reads system and table privileges for user
     * @param userName
     * @return
     */
    List<String> readAllPrivileges(String userName){
        List<String> privileges = new ArrayList<String>();
        privileges.addAll(readSystemPrivileges(userName));
        privileges.addAll(readObjectPrivileges(userName));
        return privileges;
    }
    
    /**
     * Reads system privileges for user
     * @param userName
     * @return list of system privileges
     */
    List<String> readSystemPrivileges(String userName){
        List<String> privileges = new ArrayList<String>();
        try {
            final SQLParam userNameParam =  new SQLParam("Grantee", userName);
            List<Object[]> selectRows = SQLUtil.selectRows(conn, "select PRIVILEGE from DBA_SYS_PRIVS where Grantee = ?", userNameParam);
            for(Object[] row : selectRows){
                privileges.add((String) row[0]);
            }
            return privileges;
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    /**
     * Reads object privileges for user
     * @param userName
     * @return list of object privileges
     */
    List<String> readObjectPrivileges(String userName){
        List<String> privileges = new ArrayList<String>();
        try {
            final SQLParam userNameParam =  new SQLParam("Grantee", userName);
            List<Object[]> selectRows = SQLUtil.selectRows(conn, "select PRIVILEGE,OWNER,TABLE_NAME from DBA_TAB_PRIVS where Grantee = ?", userNameParam);
            for(Object[] row : selectRows){
                String privilege = row[0] + " ON " + row[1] + "." + row[2];
                privileges.add(privilege);
            }
            return privileges;
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    

}
