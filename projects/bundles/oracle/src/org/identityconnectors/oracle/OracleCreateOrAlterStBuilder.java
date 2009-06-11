package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.MSG_CANNOT_EXPIRE_PASSWORD_FOR_NOT_LOCAL_AUTHENTICATION;
import static org.identityconnectors.oracle.OracleMessages.MSG_CANNOT_SET_GLOBALNAME_FOR_NOT_GLOBAL_AUTHENTICATION;
import static org.identityconnectors.oracle.OracleMessages.MSG_CANNOT_SET_PASSWORD_FOR_NOT_LOCAL_AUTHENTICATION;
import static org.identityconnectors.oracle.OracleMessages.MSG_MISSING_DEFAULT_TABLESPACE_FOR_QUOTA;
import static org.identityconnectors.oracle.OracleMessages.MSG_MISSING_GLOBALNAME_FOR_GLOBAL_AUTHENTICATION;
import static org.identityconnectors.oracle.OracleMessages.MSG_MISSING_TEMPORARY_TABLESPACE_FOR_QUOTA;
import static org.identityconnectors.oracle.OracleMessages.MSG_MUST_SPECIFY_PASSWORD_FOR_UNEXPIRE;
import static org.identityconnectors.oracle.OracleUserAttribute.DEF_TABLESPACE;
import static org.identityconnectors.oracle.OracleUserAttribute.PASSWORD;
import static org.identityconnectors.oracle.OracleUserAttribute.PASSWORD_EXPIRE;
import static org.identityconnectors.oracle.OracleUserAttribute.PROFILE;
import static org.identityconnectors.oracle.OracleUserAttribute.TEMP_TABLESPACE;
import static org.identityconnectors.oracle.OracleUserAttribute.USER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.UpdateOp;

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
	private final ExtraAttributesPolicySetup extraAttributesPolicySetup;
	
	private final static Log log = Log.getLog(OracleCreateOrAlterStBuilder.class);
    
    /** Helper status where we store real set attributes */
    private static class BuilderStatus{
        // Real password set
        private GuardedString passwordSet;
        //Force to expire password even if no attribute was specified by user
        private boolean forceExpirePassword;
        //Real set authentication
        OracleAuthentication currentAuth;
        private final List<Pair<OracleUserAttribute,RuntimeException>> ignoredAttributes = new ArrayList<Pair<OracleUserAttribute,RuntimeException>>(2);
        private BuilderStatus addIgnoredAttribute(OracleUserAttribute oua,RuntimeException e){
        	ignoredAttributes.add(new Pair<OracleUserAttribute,RuntimeException>(oua,e));
        	return this;
        }
    }
    
    OracleCreateOrAlterStBuilder(OracleCaseSensitivitySetup cs, ConnectorMessages cm, ExtraAttributesPolicySetup extraAttributesPolicySetup) {
        this.cs = OracleConnectorHelper.assertNotNull(cs, "cs");
        this.cm = OracleConnectorHelper.assertNotNull(cm, "cm");
        this.extraAttributesPolicySetup = OracleConnectorHelper.assertNotNull(extraAttributesPolicySetup, "extraAttributesPolicySetup");
    }
    
    OracleCreateOrAlterStBuilder(OracleConfiguration cfg) {
    	this(cfg.getCSSetup(), cfg.getConnectorMessages(), cfg.getExtraAttributesPolicySetup());
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
        String ddl = generateDDL(userAttributes,CreateOp.class,null);
        if(ddl == null || ddl.length() == 0){
        	return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("create user ").append(cs.formatToken(USER, userAttributes.getUserName()));
        builder.append(ddl);
        return builder.toString();
    }
    
    String buildAlterUserSt(OracleUserAttributes userAttributes,UserRecord userRecord){
        if(userAttributes.getUserName() == null){
            throw new IllegalArgumentException("User not specified"); //Internal error
        }
        if(userRecord == null){
        	throw new IllegalArgumentException("Must set userRecord for update"); //Internal error
        }
        String ddl = generateDDL(userAttributes,UpdateOp.class,userRecord);
        if(ddl == null || ddl.length() == 0){
        	return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("alter user ").append(cs.formatToken(USER, userAttributes.getUserName()));
        builder.append(ddl);
        return builder.toString();
    }

    private String generateDDL(OracleUserAttributes userAttributes, Class<? extends SPIOperation> operation, UserRecord userRecord) {
    	StringBuilder builder = new StringBuilder();
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
        		throw new IllegalArgumentException(cm.format(MSG_MUST_SPECIFY_PASSWORD_FOR_UNEXPIRE, null));
        	}
        }
        if(status.forceExpirePassword || Boolean.TRUE.equals(userAttributes.getExpirePassword())){
        	//We can expire password only for LOCAL authentication
        	if(OracleAuthentication.LOCAL.equals(status.currentAuth)){
        		appendExpirePassword(builder,userAttributes);
        	}
        	else{
        		IllegalArgumentException e = new IllegalArgumentException(cm.format(MSG_CANNOT_EXPIRE_PASSWORD_FOR_NOT_LOCAL_AUTHENTICATION, null)); 
        		if(ExtraAttributesPolicy.FAIL.equals(extraAttributesPolicySetup.getPolicy(PASSWORD_EXPIRE, operation))){
        			throw e;
        		}
        		else{
        			status.addIgnoredAttribute(PASSWORD_EXPIRE, e);        			
        			log.info("Ignoring extra password_expire attribute in operation [{0}]", operation);
        		}
        	}
        }
        if(userAttributes.getEnable() != null){
            appendEnabled(builder,userAttributes);
        }
        if(userAttributes.getProfile() != null){
            appendProfile(builder,userAttributes);
        }
        if(builder.length() == 0 && !status.ignoredAttributes.isEmpty()){
        	//throw the fisrt exception
        	throw status.ignoredAttributes.get(0).getSecond();
        }
        return builder.toString();
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
                throw new IllegalArgumentException(cm.format(MSG_MISSING_DEFAULT_TABLESPACE_FOR_QUOTA, null)); 
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
            	throw new IllegalArgumentException(cm.format(MSG_MISSING_TEMPORARY_TABLESPACE_FOR_QUOTA, null));
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

    private void appendAuth(final StringBuilder builder, OracleUserAttributes userAttributes, Class<? extends SPIOperation> operation, BuilderStatus status,UserRecord userRecord) {
    	status.currentAuth = userAttributes.getAuth();
    	if(status.currentAuth == null){
    		if(CreateOp.class.equals(operation)){
    			status.currentAuth = OracleAuthentication.LOCAL;
    		}
    		else {
    			status.currentAuth = OracleUserReader.resolveAuthentication(userRecord);
    		}
    	}
    	boolean appendIdentified = CreateOp.class.equals(operation)
				|| userAttributes.getAuth() != null
				|| userAttributes.getPassword() != null
				|| userAttributes.getGlobalName() != null;
    	if(!appendIdentified){
    		return;
    	}
    	if(userAttributes.getPassword() != null && !OracleAuthentication.LOCAL.equals(status.currentAuth)){
    		//Apply the extra attribute policy 
    		IllegalArgumentException e = new IllegalArgumentException(cm.format(MSG_CANNOT_SET_PASSWORD_FOR_NOT_LOCAL_AUTHENTICATION, null));
    		if(ExtraAttributesPolicy.FAIL.equals(extraAttributesPolicySetup.getPolicy(PASSWORD, operation))){
    			throw e;
    		}
    		else{
    			log.info("Ignoring extra password attribute in operation [{0}]", operation);
    			status.addIgnoredAttribute(PASSWORD, e);
    			//If only password was set, return
    			if(userAttributes.getAuth() == null && userAttributes.getGlobalName() == null && UpdateOp.class.equals(operation)){
    				appendIdentified = false;
    			}
    		}
    	}
    	if(userAttributes.getGlobalName() != null && !OracleAuthentication.GLOBAL.equals(status.currentAuth)){
    		throw new IllegalArgumentException(cm.format(MSG_CANNOT_SET_GLOBALNAME_FOR_NOT_GLOBAL_AUTHENTICATION, null));
    	}
    	if(!appendIdentified){
    		return;
    	}
    	builder.append(" identified");
        if(OracleAuthentication.LOCAL.equals(status.currentAuth)){
            builder.append(" by ");
            status.passwordSet = userAttributes.getPassword();
            if(status.passwordSet == null){
            	//Can we set password same as username ? , adapter did so
            	if(CreateOp.class.equals(operation)){
            		//Set password to userName, it is already normalized
            		status.passwordSet = new GuardedString(userAttributes.getUserName().toCharArray());
            	}
            	else{
            		//no password for update and local authentication
            		//some application can send update of authentication to local and will not send password at the update
            		//In this case we will rather set password to user name and set (password_expired=true)
            		//Other option would be to throw exception, but some application could not have 
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
        else if(OracleAuthentication.EXTERNAL.equals(status.currentAuth)){
            builder.append(" externally");
        }
        else if(OracleAuthentication.GLOBAL.equals(status.currentAuth)){
            if(StringUtil.isBlank(userAttributes.getGlobalName())){
                throw new IllegalArgumentException(cm.format(MSG_MISSING_GLOBALNAME_FOR_GLOBAL_AUTHENTICATION, null));
            }
            builder.append(" globally as ");
            builder.append(cs.formatToken(OracleUserAttribute.GLOBAL_NAME,userAttributes.getGlobalName()));
        }
    }

}
