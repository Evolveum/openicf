package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_AUTH_LOCAL;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_GLOBAL_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_PROFILE_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_TEMP_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_TEMP_TS_QUOTA_ATTR_NAME;

import java.util.Map;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

/** Transforms attributes from Set<Attribute> attrs to {@link OracleUserAttributes} */
final class OracleAttributesReader {
     ConnectorMessages messages;
     
     OracleAttributesReader(ConnectorMessages messages){
         this.messages = OracleConnectorHelper.assertNotNull(messages, "messages");
     }
    
     void readCreateAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes){
    	 caAttributes.operation = Operation.CREATE;
    	 readAuthAttributes(map, caAttributes);
    	 readRestAttributes(map, caAttributes);
     }
     
     void readAlterAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes){
    	 caAttributes.operation = Operation.ALTER;
    	 readAuthAttributes(map, caAttributes);
    	 readRestAttributes(map, caAttributes);
     }
     
     
     
     private void readRestAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes) {
        caAttributes.expirePassword = OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.PASSWORD_EXPIRED_NAME);
        caAttributes.defaultTableSpace = OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_DEF_TS_ATTR_NAME);
        caAttributes.tempTableSpace = OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_TEMP_TS_ATTR_NAME);
        caAttributes.enable = OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.ENABLE_NAME);
        caAttributes.profile = OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_PROFILE_ATTR_NAME);
        caAttributes.defaultTSQuota = OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_DEF_TS_QUOTA_ATTR_NAME);
        caAttributes.tempTSQuota = OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_TEMP_TS_QUOTA_ATTR_NAME);
    }

    private void readAuthAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes) {
        String authentication =  OracleConnectorHelper.getStringValue(map, ORACLE_AUTHENTICATION_ATTR_NAME);
        Attribute passwordAttribute = map.get(OperationalAttributes.PASSWORD_NAME);
        if(authentication == null){
        	//For alter and null passwordAttribute, do not set any authentication nor password
        	if(Operation.ALTER.equals(caAttributes.operation) && passwordAttribute == null){
       			return;
        	}
            authentication = ORACLE_AUTH_LOCAL; 
        }
        try{
        	caAttributes.auth = OracleAuthentication.valueOf(authentication);
        }
        catch(IllegalArgumentException e){
        	throw new IllegalArgumentException(messages.format(OracleMessages.INVALID_AUTH, OracleMessages.INVALID_AUTH, authentication));
        }
        switch(caAttributes.auth){
	        case LOCAL :
	            GuardedString password = passwordAttribute != null ? AttributeUtil.getGuardedStringValue(passwordAttribute) : null;
	            caAttributes.password = password;
	            break;
	        case EXTERNAL : break;
	        case GLOBAL : 
	        	caAttributes.globalName = OracleConnectorHelper.getNotEmptyStringValue(map, ORACLE_GLOBAL_ATTR_NAME);
	        	break;
        }
    }

}
