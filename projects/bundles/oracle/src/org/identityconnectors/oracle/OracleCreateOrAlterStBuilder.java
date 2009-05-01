package org.identityconnectors.oracle;

import java.util.Arrays;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

import static org.identityconnectors.oracle.OracleUserAttributeCS.*;

/**
 * Builds create or alter user sql statement.
 * Class uses {@link OracleCaseSensitivitySetup} to format user attributes. 
 * It expects that passed userAttributes are already normalized, so it does not normalize tokens anymore 
 * See BSF syntax at :
 * Create : http://download.oracle.com/docs/cd/B12037_01/server.101/b10759/statements_8003.htm
 * Alter  : http://www.stanford.edu/dept/itss/docs/oracle/10g/server.101/b10759/statements_4003.htm
 * 
 * It also tries to perform additional check logic especially that no passed attribute is silently skip 
 * @author kitko
 *
 */
final class OracleCreateOrAlterStBuilder {
    private OracleCaseSensitivitySetup cs;
    @SuppressWarnings("unused")
	private ConnectorMessages cm;
    
    OracleCreateOrAlterStBuilder(OracleCaseSensitivitySetup cs,ConnectorMessages cm) {
        this.cs = OracleConnectorHelper.assertNotNull(cs, "cs");
        this.cm = OracleConnectorHelper.assertNotNull(cm, "cm");
    }
    
    /**
     * Builds create user sql statement
     * @param userAttributes
     * @return
     */
    String buildCreateUserSt(OracleUserAttributes userAttributes){
        userAttributes.operation = Operation.CREATE;
        if(userAttributes.userName == null){
            throw new IllegalArgumentException("User not specified");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("create user ").append(cs.formatToken(USER_NAME, userAttributes.userName));
        int length = builder.length();
        appendCreateOrAlterSt(builder,userAttributes,null);
        return builder.length() == length ? null : builder.toString();
    }
    
    String buildAlterUserSt(OracleUserAttributes userAttributes,UserRecord userRecord){
        userAttributes.operation = Operation.ALTER;
        if(userAttributes.userName == null){
            throw new IllegalArgumentException("User not specified");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("alter user ").append(cs.formatToken(USER_NAME, userAttributes.userName));
        int length = builder.length();
        appendCreateOrAlterSt(builder,userAttributes,userRecord);
        return builder.length() == length ? null : builder.toString();
    }

    private void appendCreateOrAlterSt(StringBuilder builder, OracleUserAttributes userAttributes, UserRecord userRecord) {
        appendAuth(builder, userAttributes, userRecord);
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
        if(userAttributes.expirePassword != null){
        	if(userAttributes.expirePassword){
	            appendExpirePassword(builder,userAttributes);
        	}
        	else{
        		//We must have password, otherwise we would silently skip expirePassword attribute
        		if(Operation.ALTER.equals(userAttributes.operation) && userAttributes.password == null ){
        			throw new IllegalArgumentException("Cannot reset password, no password provided");
        		}
        	}
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

    private void appendAuth(final StringBuilder builder, OracleUserAttributes userAttributes, UserRecord userRecord) {
    	OracleAuthentication auth = userAttributes.auth;
    	if(auth == null){
    		if(Operation.CREATE.equals(userAttributes.operation)){
    			auth = OracleAuthentication.LOCAL;
    		}
    		else{
    			if(userAttributes.password != null){
    				//we have update of password, so set auth to local
    				auth = OracleAuthentication.LOCAL;
    			}
    		}
    	}
    	if(userAttributes.globalName != null && !OracleAuthentication.GLOBAL.equals(auth)){
    		throw new IllegalArgumentException("Globalname cannot be set for not global authentication");
    	}
    	if(userAttributes.password != null && !OracleAuthentication.LOCAL.equals(auth)){
    		throw new IllegalArgumentException("Password cannot be set for not local authentication");
    	}
    	if(auth == null){
    		return;
    	}
    	builder.append(" identified");
        if(OracleAuthentication.LOCAL.equals(auth)){
            builder.append(" by ");
            GuardedString password = userAttributes.password;
            if(password == null){
            	//Can we set password same as username ? , adapter did so
            	if(Operation.CREATE.equals(userAttributes.operation)){
	                password = new GuardedString(userAttributes.userName.toCharArray());
            	}
            	else{
            		//no password for update and local authentication
            		//some application can send update of authentication to local and will not send password at the update
            		//In this case we will rather set password to user name and set (password_expired=true)
            		//Other option would be to throw exception, but some application could noty have 
            		//possibility to send password 
            		password = new GuardedString(userAttributes.userName.toCharArray());
            		userAttributes.expirePassword = true;
            	}
            }
            password.access(new GuardedString.Accessor(){
                public void access(char[] clearChars) {
                    builder.append(cs.formatToken(PASSWORD, clearChars));
                    Arrays.fill(clearChars, (char)0);
                }
            });
        }
        else if(OracleAuthentication.EXTERNAL.equals(auth)){
            builder.append(" externally");
        }
        else if(OracleAuthentication.GLOBAL.equals(auth)){
            if(userAttributes.globalName == null){
                throw new IllegalArgumentException("GlobalName not specified for global authentication");
            }
            builder.append(" globally as ");
            builder.append(cs.formatToken(OracleUserAttributeCS.GLOBAL_NAME,userAttributes.globalName));
        }
        
        
    }

}
