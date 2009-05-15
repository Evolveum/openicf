package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleUserAttributeCS.DEF_TABLESPACE;
import static org.identityconnectors.oracle.OracleUserAttributeCS.PASSWORD;
import static org.identityconnectors.oracle.OracleUserAttributeCS.PROFILE;
import static org.identityconnectors.oracle.OracleUserAttributeCS.TEMP_TABLESPACE;
import static org.identityconnectors.oracle.OracleUserAttributeCS.USER;

import java.util.Arrays;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

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
    private final OracleCaseSensitivitySetup cs;
    private final ConnectorMessages cm;
    
    /** Helper status where we store real set attributes */
    private static class BuilderStatus{
        // Real password set
        private GuardedString passwordSet;
        //Force to expire password even if no attribute was specified by user
        private boolean forceExpirePassword;
    }
    
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
        if(userAttributes.getUserName() == null){
            throw new IllegalArgumentException("User not specified"); //Internal error
        }
        StringBuilder builder = new StringBuilder();
        builder.append("create user ").append(cs.formatToken(USER, userAttributes.getUserName()));
        int length = builder.length();
        appendCreateOrAlterSt(builder,userAttributes,Operation.CREATE,null);
        return builder.length() == length ? null : builder.toString();
    }
    
    String buildAlterUserSt(OracleUserAttributes userAttributes,UserRecord userRecord){
        if(userAttributes.getUserName() == null){
            throw new IllegalArgumentException("User not specified"); //Internal error
        }
        if(userRecord == null){
        	throw new IllegalArgumentException("Must set userRecord for update"); //Internal error
        }
        StringBuilder builder = new StringBuilder();
        builder.append("alter user ").append(cs.formatToken(USER, userAttributes.getUserName()));
        int length = builder.length();
        appendCreateOrAlterSt(builder,userAttributes,Operation.ALTER,userRecord);
        return builder.length() == length ? null : builder.toString();
    }

    private void appendCreateOrAlterSt(StringBuilder builder, OracleUserAttributes userAttributes, Operation operation, UserRecord userRecord) {
    	BuilderStatus status = new BuilderStatus();
        appendAuth(builder, userAttributes, operation, status, userRecord);
        if(userAttributes.getDefaultTableSpace() != null){
            appendDefaultTableSpace(builder,userAttributes);
        }
        if(userAttributes.getTempTableSpace() != null){
            appendTemporaryTableSpace(builder,userAttributes);
        }
        if(userAttributes.getDefaultTSQuota() != null){
            appendDefaultTSQuota(builder,userAttributes,userRecord);
        }
        if(userAttributes.getTempTSQuota() != null){
            appendTempTSQuota(builder,userAttributes,userRecord );
        }
        if(Boolean.FALSE.equals(userAttributes.getExpirePassword())){
        	if(status.passwordSet == null){
        		throw new IllegalArgumentException(cm.format("oracle.must.specify.password.for.unexpire", null));
        	}
        }
        if(status.forceExpirePassword || Boolean.TRUE.equals(userAttributes.getExpirePassword())){
        	appendExpirePassword(builder,userAttributes);
        }
        if(userAttributes.getEnable() != null){
            appendEnabled(builder,userAttributes);
        }
        if(userAttributes.getProfile() != null){
            appendProfile(builder,userAttributes);
        }
    }

    private void appendProfile(StringBuilder builder,OracleUserAttributes userAttributes) {
        builder.append(" profile ").append(cs.formatToken(PROFILE,userAttributes.getProfile()));
        
    }

    private void appendEnabled(StringBuilder builder, OracleUserAttributes userAttributes) {
        if(userAttributes.getEnable()){
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
        if("-1".equals(userAttributes.getDefaultTSQuota())){
            builder.append(" unlimited");
        }
        else{
            builder.append(' ').append(userAttributes.getDefaultTSQuota());
        }
        builder.append(" on");
        String defaultTableSpace = userAttributes.getDefaultTableSpace(); 
        if(defaultTableSpace == null){
            if(userRecord == null || userRecord.getDefaultTableSpace() == null){
                throw new IllegalArgumentException(cm.format("oracle.missing.default.tablespace", null)); 
            }
            defaultTableSpace = userRecord.getDefaultTableSpace();
        }
        builder.append(' ').append(cs.formatToken(DEF_TABLESPACE, defaultTableSpace));
    }

    private void appendTempTSQuota(StringBuilder builder, OracleUserAttributes userAttributes, UserRecord userRecord) {
        builder.append(" quota");
        if("-1".equals(userAttributes.getTempTSQuota())){
            builder.append(" unlimited");
        }
        else{
            builder.append(' ').append(userAttributes.getTempTSQuota());
        }
        builder.append(" on");
        String tempTableSpace = userAttributes.getTempTableSpace(); 
        if(tempTableSpace == null){
            if(userRecord == null || userRecord.getTemporaryTableSpace() == null){
            	throw new IllegalArgumentException(cm.format("oracle.missing.temporary.tablespace", null));
            }
            tempTableSpace = userRecord.getTemporaryTableSpace();
        }
        builder.append(' ').append(cs.formatToken(TEMP_TABLESPACE, tempTableSpace));

    }
    
    private void appendTemporaryTableSpace(StringBuilder builder, OracleUserAttributes userAttributes) {
        builder.append(" temporary tablespace ").append(cs.formatToken(TEMP_TABLESPACE, userAttributes.getTempTableSpace()));
        
    }

    private void appendDefaultTableSpace(StringBuilder builder, OracleUserAttributes userAttributes) {
        builder.append(" default tablespace ").append(cs.formatToken(DEF_TABLESPACE, userAttributes.getDefaultTableSpace()));
    }

    private void appendAuth(final StringBuilder builder, OracleUserAttributes userAttributes, Operation operation, BuilderStatus status,UserRecord userRecord) {
    	OracleAuthentication auth = userAttributes.getAuth();
    	if(auth == null){
    		if(Operation.CREATE.equals(operation)){
    			auth = OracleAuthentication.LOCAL;
    		}
    		else{
    			if(userAttributes.getPassword() != null){
    				//we have update of password, so set auth to local
    				auth = OracleAuthentication.LOCAL;
    			}
    		}
    	}
    	if(userAttributes.getGlobalName() != null && !OracleAuthentication.GLOBAL.equals(auth)){
    		throw new IllegalArgumentException(cm.format("oracle.cannot.set.globalname.for.not.global.authentication", null));
    	}
    	if(userAttributes.getPassword() != null && !OracleAuthentication.LOCAL.equals(auth)){
    		throw new IllegalArgumentException(cm.format("oracle.cannot.set.password.for.not.local.authentication", null));
    	}
    	if(auth == null){
    		return;
    	}
    	builder.append(" identified");
        if(OracleAuthentication.LOCAL.equals(auth)){
            builder.append(" by ");
            status.passwordSet = userAttributes.getPassword();
            if(status.passwordSet == null){
            	//Can we set password same as username ? , adapter did so
            	if(Operation.CREATE.equals(operation)){
            		status.passwordSet = new GuardedString(userAttributes.getUserName().toCharArray());
            	}
            	else{
            		//no password for update and local authentication
            		//some application can send update of authentication to local and will not send password at the update
            		//In this case we will rather set password to user name and set (password_expired=true)
            		//Other option would be to throw exception, but some application could noty have 
            		//possibility to send password 
            		status.passwordSet = new GuardedString(userAttributes.getUserName().toCharArray());
            		status.forceExpirePassword = true;
            	}
            }
            status.passwordSet.access(new GuardedString.Accessor(){
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
            if(userAttributes.getGlobalName() == null){
                throw new IllegalArgumentException(cm.format("oracle.missing.globalname.for.global.authentication", null));
            }
            builder.append(" globally as ");
            builder.append(cs.formatToken(OracleUserAttributeCS.GLOBAL_NAME,userAttributes.getGlobalName()));
        }
        
        
    }

}
