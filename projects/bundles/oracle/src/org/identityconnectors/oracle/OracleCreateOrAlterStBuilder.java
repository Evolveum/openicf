package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;

/**
 * Builds create or alter user sql statement.
 * See BSF syntax at :
 * Create : http://download.oracle.com/docs/cd/B12037_01/server.101/b10759/statements_8003.htm
 * Alter  : http://www.stanford.edu/dept/itss/docs/oracle/10g/server.101/b10759/statements_4003.htm
 * @author kitko
 *
 */
class OracleCreateOrAlterStBuilder {
    
    String buildCreateUserSt(CreateAlterAttributes userAttributes){
        StringBuilder builder = new StringBuilder();
        userAttributes.operation = Operation.CREATE;
        if(userAttributes.userName == null){
            throw new IllegalArgumentException("User not specified");
        }
        builder.append("create user ").append("\"").append(userAttributes.userName).append("\"");
        if(userAttributes.auth == null){
            throw new IllegalArgumentException("Authentication not specified");
        }
        appendCreateOrAlterSt(builder,userAttributes,null);
        return builder.toString();
    }
    
    String buildAlterUserSt(CreateAlterAttributes userAttributes,UserRecord userRecord){
        StringBuilder builder = new StringBuilder();
        userAttributes.operation = Operation.ALTER;
        if(userAttributes.userName == null){
            throw new IllegalArgumentException("User not specified");
        }
        builder.append("alter user ").append("\"").append(userAttributes.userName).append("\"");
        appendCreateOrAlterSt(builder,userAttributes,userRecord);
        return builder.toString();
    }

    private void appendCreateOrAlterSt(StringBuilder builder, CreateAlterAttributes userAttributes, UserRecord userRecord) {
        if(userAttributes.auth != null){
            appendAuth(builder, userAttributes);
        }
        if(userAttributes.defaultTableSpace != null){
            appendDefaultTableSpace(builder,userAttributes);
        }
        if(userAttributes.tempTableSpace != null){
            appendTemporaryTableSpace(builder,userAttributes);
        }
        if(userAttributes.defaultTSQuota != null){
            appendDefaultTSQuota(builder,userAttributes,userRecord);
        }
        if(userAttributes.tempTSQuota != null){
            appendTempTSQuota(builder,userAttributes,userRecord );
        }
        if(userAttributes.expirePassword != null && userAttributes.expirePassword){
            appendExpirePassword(builder,userAttributes);
        }
        if(userAttributes.enable != null){
            appendEnabled(builder,userAttributes);
        }
        if(userAttributes.profile != null){
            appendProfile(builder,userAttributes);
        }
        
        
        
    }

    private void appendProfile(StringBuilder builder,CreateAlterAttributes userAttributes) {
        builder.append(" profile ").append('\"').append(userAttributes.profile).append('\"');
        
    }

    private void appendEnabled(StringBuilder builder, CreateAlterAttributes userAttributes) {
        if(userAttributes.enable){
            builder.append(" account unlock");
        }
        else{
            builder.append(" account lock");
        }
        
    }

    private void appendExpirePassword(StringBuilder builder, CreateAlterAttributes userAttributes) {
        builder.append(" password expire");
    }

    private void appendDefaultTSQuota(StringBuilder builder, CreateAlterAttributes userAttributes, UserRecord userRecord) {
        builder.append(" quota");
        String size = userAttributes.defaultTSQuota.size;
        if(size == null){
            builder.append(" unlimited");
        }
        else{
            builder.append(' ').append(size);
        }
        builder.append(" on");
        String defaultTableSpace = userAttributes.defaultTableSpace; 
        if(defaultTableSpace == null){
            if(userRecord == null || userRecord.defaultTableSpace == null){
                throw new IllegalArgumentException("Default tablespace not specified");
            }
            defaultTableSpace = userRecord.defaultTableSpace;
        }
        builder.append(' ').append('\"').append(defaultTableSpace).append('\"');
    }

    private void appendTempTSQuota(StringBuilder builder, CreateAlterAttributes userAttributes, UserRecord userRecord) {
        builder.append(" quota");
        String size = userAttributes.tempTSQuota.size;
        if(size == null){
            builder.append(" unlimited");
        }
        else{
            builder.append(' ').append(size);
        }
        builder.append(" on");
        String tempTableSpace = userAttributes.tempTableSpace; 
        if(tempTableSpace == null){
            if(userRecord == null || userRecord.temporaryTableSpace == null){
                throw new IllegalArgumentException("Temporary tablespace not specified");
            }
            tempTableSpace = userRecord.temporaryTableSpace;
        }
        builder.append(' ').append('\"').append(tempTableSpace).append('\"');

    }
    
    private void appendTemporaryTableSpace(StringBuilder builder, CreateAlterAttributes userAttributes) {
        builder.append(" temporary tablespace ").append('\"').append(userAttributes.tempTableSpace).append('\"');
        
    }

    private void appendDefaultTableSpace(StringBuilder builder, CreateAlterAttributes userAttributes) {
        builder.append(" default tablespace ").append('\"').append(userAttributes.defaultTableSpace).append('\"');
    }

    private void appendAuth(final StringBuilder builder, CreateAlterAttributes userAttributes) {
        builder.append(" identified");
        if(OracleAuthentication.LOCAL.equals(userAttributes.auth)){
            builder.append(" by ");
            GuardedString password = userAttributes.password;
            if(password == null){
                password = new GuardedString(userAttributes.userName.toCharArray());
            }
            password.access(new GuardedString.Accessor(){
                public void access(char[] clearChars) {
                    builder.append('\"').append(clearChars).append('\"');
                }
            });
        }
        else if(OracleAuthentication.EXTERNAL.equals(userAttributes.auth)){
            builder.append(" externally");
        }
        else if(OracleAuthentication.GLOBAL.equals(userAttributes.auth)){
            if(userAttributes.globalName == null){
                throw new IllegalArgumentException("GlobalName not specified for global authentication");
            }
            builder.append(" globally as ");
            builder.append('\"').append(userAttributes.globalName).append('\"');
        }
        
        
    }

}
