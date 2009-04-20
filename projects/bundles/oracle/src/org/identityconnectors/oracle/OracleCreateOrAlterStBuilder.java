package org.identityconnectors.oracle;

import java.util.Arrays;
import org.identityconnectors.common.security.GuardedString;
import static org.identityconnectors.oracle.OracleUserAttribute.*;

/**
 * Builds create or alter user sql statement.
 * See BSF syntax at :
 * Create : http://download.oracle.com/docs/cd/B12037_01/server.101/b10759/statements_8003.htm
 * Alter  : http://www.stanford.edu/dept/itss/docs/oracle/10g/server.101/b10759/statements_4003.htm
 * @author kitko
 *
 */
class OracleCreateOrAlterStBuilder {
    private OracleCaseSensitivitySetup cs;
    
    public OracleCreateOrAlterStBuilder(OracleCaseSensitivitySetup cs) {
        this.cs = OracleConnectorHelper.assertNotNull(cs, "cs");
    }
    
    /**
     * Builds create user sql statement
     * @param userAttributes
     * @return
     */
    String buildCreateUserSt(OracleUserAttributes userAttributes){
        StringBuilder builder = new StringBuilder();
        userAttributes.operation = Operation.CREATE;
        if(userAttributes.userName == null){
            throw new IllegalArgumentException("User not specified");
        }
        builder.append("create user ").append(cs.formatToken(USER_NAME, userAttributes.userName));
        if(userAttributes.auth == null){
            throw new IllegalArgumentException("Authentication not specified");
        }
        appendCreateOrAlterSt(builder,userAttributes,null);
        return builder.toString();
    }
    
    String buildAlterUserSt(OracleUserAttributes userAttributes,UserRecord userRecord){
        StringBuilder builder = new StringBuilder();
        userAttributes.operation = Operation.ALTER;
        if(userAttributes.userName == null){
            throw new IllegalArgumentException("User not specified");
        }
        builder.append("alter user ").append(cs.formatToken(USER_NAME, userAttributes.userName));
        appendCreateOrAlterSt(builder,userAttributes,userRecord);
        return builder.toString();
    }

    private void appendCreateOrAlterSt(StringBuilder builder, OracleUserAttributes userAttributes, UserRecord userRecord) {
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

    private void appendProfile(StringBuilder builder,OracleUserAttributes userAttributes) {
        builder.append(" profile ").append(cs.formatToken(PROFILE,userAttributes.profile));
        
    }

    private void appendEnabled(StringBuilder builder, OracleUserAttributes userAttributes) {
        if(userAttributes.enable){
            builder.append(" account unlock");
        }
        else{
            builder.append(" account lock");
        }
        
    }

    private void appendExpirePassword(StringBuilder builder, OracleUserAttributes userAttributes) {
        builder.append(" password expire");
    }

    private void appendDefaultTSQuota(StringBuilder builder, OracleUserAttributes userAttributes, UserRecord userRecord) {
        builder.append(" quota");
        if("-1".equals(userAttributes.defaultTSQuota)){
            builder.append(" unlimited");
        }
        else{
            builder.append(' ').append(userAttributes.defaultTSQuota);
        }
        builder.append(" on");
        String defaultTableSpace = userAttributes.defaultTableSpace; 
        if(defaultTableSpace == null){
            if(userRecord == null || userRecord.defaultTableSpace == null){
                throw new IllegalArgumentException("Default tablespace not specified");
            }
            defaultTableSpace = userRecord.defaultTableSpace;
        }
        builder.append(' ').append(cs.formatToken(DEF_TABLESPACE, defaultTableSpace));
    }

    private void appendTempTSQuota(StringBuilder builder, OracleUserAttributes userAttributes, UserRecord userRecord) {
        builder.append(" quota");
        if("-1".equals(userAttributes.tempTSQuota)){
            builder.append(" unlimited");
        }
        else{
            builder.append(' ').append(userAttributes.tempTSQuota);
        }
        builder.append(" on");
        String tempTableSpace = userAttributes.tempTableSpace; 
        if(tempTableSpace == null){
            if(userRecord == null || userRecord.temporaryTableSpace == null){
                throw new IllegalArgumentException("Temporary tablespace not specified");
            }
            tempTableSpace = userRecord.temporaryTableSpace;
        }
        builder.append(' ').append(cs.formatToken(TEMP_TABLESPACE, tempTableSpace));

    }
    
    private void appendTemporaryTableSpace(StringBuilder builder, OracleUserAttributes userAttributes) {
        builder.append(" temporary tablespace ").append(cs.formatToken(TEMP_TABLESPACE, userAttributes.tempTableSpace));
        
    }

    private void appendDefaultTableSpace(StringBuilder builder, OracleUserAttributes userAttributes) {
        builder.append(" default tablespace ").append(cs.formatToken(DEF_TABLESPACE, userAttributes.defaultTableSpace));
    }

    private void appendAuth(final StringBuilder builder, OracleUserAttributes userAttributes) {
        builder.append(" identified");
        if(OracleAuthentication.LOCAL.equals(userAttributes.auth)){
            builder.append(" by ");
            GuardedString password = userAttributes.password;
            if(password == null){
                password = new GuardedString(userAttributes.userName.toCharArray());
            }
            password.access(new GuardedString.Accessor(){
                public void access(char[] clearChars) {
                    builder.append(cs.formatToken(PASSWORD, clearChars));
                    Arrays.fill(clearChars, (char)0);
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
            builder.append(cs.formatToken(OracleUserAttribute.GLOBAL_NAME,userAttributes.globalName));
        }
        
        
    }

}
