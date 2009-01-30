package org.identityconnectors.oracle;

import java.sql.*;
import java.util.*;

import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

class OracleRolePrivReader {
    
    private Connection conn;
    OracleRolePrivReader(Connection conn) {
        super();
        this.conn = conn;
    }
    
    List<String> readRoles(String userName){
        List<String> roles = new ArrayList<String>();
        try {
            final List<Object[]> selectRows = SQLUtil.selectRows(conn, "select GRANTED_ROLE from DBA_ROLE_PRIVS where Grantee = '" + userName + "'");
            for(Object[] row : selectRows){
                roles.add((String) row[0]);
            }
            return roles;
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    List<String> readPrivileges(String userName){
        List<String> privileges = new ArrayList<String>();
        try {
            List<Object[]> selectRows = SQLUtil.selectRows(conn, "select PRIVILEGE from DBA_SYS_PRIVS where Grantee = \"" + userName + "\"");
            for(Object[] row : selectRows){
                privileges.add((String) row[0]);
            }
            selectRows = SQLUtil.selectRows(conn, "select PRIVILEGE,TABLE_NAME from USER_TAB_PRIVS where Grantee = \"" + userName + "\"");
            for(Object[] row : selectRows){
                String privilege = row[0] + " ON " + row[1];
                privileges.add(privilege);
            }
            return privileges;
        } catch (SQLException e) {
            throw ConnectorException.wrap(e);
        }
    }

}
