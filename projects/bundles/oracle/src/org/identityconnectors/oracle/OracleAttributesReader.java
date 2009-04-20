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
class OracleAttributesReader {
     ConnectorMessages messages;
     
     OracleAttributesReader(ConnectorMessages messages){
         this.messages = OracleConnectorHelper.assertNotNull(messages, "messages");
     }
    
     void readCreateRestAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes) {
        caAttributes.expirePassword = OracleConnectorHelper.getBooleanValue(map, OperationalAttributes.PASSWORD_EXPIRED_NAME);
        caAttributes.defaultTableSpace = OracleConnectorHelper.getStringValue(map, ORACLE_DEF_TS_ATTR_NAME);
        caAttributes.tempTableSpace = OracleConnectorHelper.getStringValue(map, ORACLE_TEMP_TS_ATTR_NAME);
        caAttributes.enable = OracleConnectorHelper.getBooleanValue(map, OperationalAttributes.ENABLE_NAME);
        caAttributes.profile = OracleConnectorHelper.getStringValue(map, ORACLE_PROFILE_ATTR_NAME);
        Attribute defaultTSQuota = map.get(ORACLE_DEF_TS_QUOTA_ATTR_NAME);
        if(defaultTSQuota != null){
            caAttributes.defaultTSQuota = AttributeUtil.getStringValue(defaultTSQuota);
        }
        Attribute tempTSQuota = map.get(ORACLE_TEMP_TS_QUOTA_ATTR_NAME);
        if(tempTSQuota != null){
            caAttributes.tempTSQuota = AttributeUtil.getStringValue(tempTSQuota);
        }
    }

    void readCreateAuthAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes) {
        String authentication =  OracleConnectorHelper.getStringValue(map, ORACLE_AUTHENTICATION_ATTR_NAME);
        if(authentication == null){
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
	        	Attribute passwordAttribute = map.get(OperationalAttributes.PASSWORD_NAME);
	            GuardedString password = passwordAttribute != null ? AttributeUtil.getGuardedStringValue(passwordAttribute) : null;
	            if(password == null){
	                if(caAttributes.userName == null){
	                    throw new IllegalArgumentException("Cannot choose default passoword, username is null");
	                }
	                password = new GuardedString(caAttributes.userName.toCharArray());
	            }
	            caAttributes.password = password;
	            break;
	        case EXTERNAL : break;
	        case GLOBAL : 
	        	caAttributes.globalName = OracleConnectorHelper.getNotEmptyStringValue(map, ORACLE_GLOBAL_ATTR_NAME);
	        	break;
        }
    }

}
