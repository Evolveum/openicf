/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.*;

/**
 * Builds grant and revoke sql
 * @author kitko
 *
 */
class OracleRolesAndPrivsBuilder {
    
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
    
    private void appendGrantRole(StringBuilder builder,String userName,String grant){
        builder.append("grant ").append('"').append(grant).append('"').append(" to ").append('\"').append(userName).append('\"');
    }
    
    private void appendGrantPrivilege(StringBuilder builder,String userName,String grant){
        builder.append("grant ").append(grant).append(" to ").append('\"').append(userName).append('\"');
    }
    
    
    List<String> buildAlter(String userName,List<String> roles,List<String> privileges){
        //TODO 
        return Collections.emptyList();
    }
}
