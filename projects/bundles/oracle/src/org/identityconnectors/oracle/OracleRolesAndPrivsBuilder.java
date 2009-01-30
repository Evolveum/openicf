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
    
    List<String> buildCreate(String userName,List<String> roles,List<String> privileges){
        List<String> statements = new ArrayList<String>();
        StringBuilder builder = new StringBuilder(30);
        for(String grant : roles){
            appendGrant(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        for(String grant : privileges){
            appendGrant(builder,userName,grant);
            statements.add(builder.toString());
            builder.delete(0,builder.length());
        }
        return Collections.emptyList();
    }
    
    private void appendGrant(StringBuilder builder,String userName,String grant){
        builder.append("grant ").append(grant).append(" to ").append('\"').append(userName).append('\"');
    }
    
    List<String> buildAlter(String userName,List<String> roles,List<String> privileges){
        return Collections.emptyList();
    }
}
