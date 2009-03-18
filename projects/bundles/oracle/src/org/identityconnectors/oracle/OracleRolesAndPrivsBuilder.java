/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.*;
import static org.identityconnectors.oracle.OracleUserAttribute.*;

/**
 * Builds grant and revoke sql
 * @author kitko
 *
 */
class OracleRolesAndPrivsBuilder {
    private OracleCaseSensitivitySetup cs;
    
    OracleRolesAndPrivsBuilder(OracleCaseSensitivitySetup cs){
        this.cs = OracleConnectorHelper.assertNotNull(cs, "cs");
        
    }
    /**
     * Builds statements that grants roles and privilehes to user
     * @param userName
     * @param roles
     * @param privileges
     * @return list of sql statements
     */
    List<String> buildCreateSQL(String userName,List<String> roles,List<String> privileges){
        List<String> statements = new ArrayList<String>();
        StringBuilder builder = new StringBuilder(30);
        for(String grant : roles){
            appendGrantRole(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        for(String grant : privileges){
            appendGrantPrivilege(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        return statements;
    }
    
    private void appendGrantRole(StringBuilder builder,String userName,String role){
        builder.append("grant ").append(cs.formatToken(ROLE, role)).append(" to ").append(cs.formatToken(USER_NAME, userName));
    }
    
    private void appendGrantPrivilege(StringBuilder builder,String userName,String privilege){
        builder.append("grant ").append(cs.formatToken(PRIVILEGE, privilege)).append(" to ").append(cs.formatToken(USER_NAME, userName));
    }
    
    
    List<String> buildAlter(String userName,List<String> roles,List<String> privileges){
        //TODO 
        return Collections.emptyList();
    }
}
