/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.*;
import static org.identityconnectors.oracle.OracleUserAttributeCS.*;

/**
 * Builds grant and revoke sql
 * http://www.stanford.edu/dept/itss/docs/oracle/10g/server.101/b10759/statements_9020.htm 
 * @author kitko
 *
 */
class OracleRolesAndPrivsBuilder {
    private OracleCaseSensitivitySetup cs;
    
    OracleRolesAndPrivsBuilder(OracleCaseSensitivitySetup cs){
        this.cs = OracleConnectorHelper.assertNotNull(cs, "cs");
        
    }
    /**
     * Builds statements that grants roles to user
     * @param userName
     * @param roles
     * @param privileges
     * @return list of sql statements
     */
    List<String> buildGrantRolesSQL(String userName,List<String> roles){
        List<String> statements = new ArrayList<String>();
        StringBuilder builder = new StringBuilder(30);
        for(String grant : roles){
            appendGrantRole(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        return statements;
    }
    
    /**
     * Builds statements that grants privileges to user
     * @param userName
     * @param roles
     * @param privileges
     * @return list of sql statements
     */
    List<String> buildGrantPrivilegesSQL(String userName,List<String> privileges){
        List<String> statements = new ArrayList<String>();
        StringBuilder builder = new StringBuilder(30);
        for(String grant : privileges){
            appendGrantPrivilege(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        return statements;
    }
    
    
    //Maybe better would be to have it in one statement
    private void appendGrantRole(StringBuilder builder,String userName,String role){
        builder.append("grant ").append(cs.formatToken(ROLE, role)).append(" to ").append(cs.formatToken(USER_NAME, userName));
    }
    
  //Maybe better would be to have it in one statement
    private void appendGrantPrivilege(StringBuilder builder,String userName,String privilege){
        builder.append("grant ").append(cs.formatToken(PRIVILEGE, privilege)).append(" to ").append(cs.formatToken(USER_NAME, userName));
    }
    
    List<String> buildRevokeRoles(String userName,List<String> roles){
        List<String> statements = new ArrayList<String>();
        StringBuilder builder = new StringBuilder(30);
        for(String grant : roles){
            appendRevokeRole(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        return statements;
    }
    
    
    
    List<String> buildRevokePrivileges(String userName,List<String> privileges){
        List<String> statements = new ArrayList<String>();
        StringBuilder builder = new StringBuilder(30);
        for(String grant : privileges){
            appendRevokePrivilege(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        return statements;
    }
    
	private void appendRevokeRole(StringBuilder builder, String userName, String role) {
		builder.append("revoke ").append(cs.formatToken(ROLE, role)).append(" from ").append(cs.formatToken(USER_NAME, userName));
	}
	
	private void appendRevokePrivilege(StringBuilder builder, String userName, String privilege) {
		builder.append("revoke ").append(cs.formatToken(PRIVILEGE, privilege)).append(" from ").append(cs.formatToken(USER_NAME, userName));
		
	}
    
    
}
