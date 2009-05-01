package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_GLOBAL_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_PROFILE_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_TEMP_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_TEMP_TS_QUOTA_ATTR_NAME;

import java.util.Map;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

/** Transforms attributes from Set<Attribute> attrs to {@link OracleUserAttributes}.
 *  It checks just nullability of attributes and makes some possible value checks.
 *  It does not do any additional logical checks, it is mainly reader of attributes to the helper structure.
 * */
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
        
        Attribute defaultTSQuota = map.get(ORACLE_DEF_TS_QUOTA_ATTR_NAME);
        if(defaultTSQuota != null){
        	String val = AttributeUtil.getStringValue(defaultTSQuota);
        	if(StringUtil.isBlank(val)){
        		//when updating to null, actuall we want to drop quouta information and this will
        		//be done altering to 0
        		caAttributes.defaultTSQuota = "0";
        	}
        	else{
        		caAttributes.defaultTSQuota = val;
        	}
        }
        
        Attribute tempTSQuota = map.get(ORACLE_TEMP_TS_QUOTA_ATTR_NAME);
        if(tempTSQuota != null){
        	String val = AttributeUtil.getStringValue(tempTSQuota);
        	if(StringUtil.isBlank(val)){
        		//when updating to null, actuall we want to drop quouta information and this will
        		//be done altering to 0
        		caAttributes.tempTSQuota = "0";
        	}
        	else{
        		caAttributes.tempTSQuota = val;
        	}
        }
    }

    private void readAuthAttributes(Map<String, Attribute> map, OracleUserAttributes caAttributes) {
        String authentication =  OracleConnectorHelper.getStringValue(map, ORACLE_AUTHENTICATION_ATTR_NAME);
        Attribute passwordAttribute = map.get(OperationalAttributes.PASSWORD_NAME);
        //Set globalname to not silently skip it
        caAttributes.globalName = OracleConnectorHelper.getStringValue(map, ORACLE_GLOBAL_ATTR_NAME);
        caAttributes.password = passwordAttribute != null ? AttributeUtil.getGuardedStringValue(passwordAttribute) : null;
        if(authentication != null){
	        try{
	        	caAttributes.auth = OracleAuthentication.valueOf(authentication);
	        }
	        catch(IllegalArgumentException e){
	        	throw new IllegalArgumentException(messages.format(OracleMessages.INVALID_AUTH, OracleMessages.INVALID_AUTH, authentication));
	        }
	        switch(caAttributes.auth){
		        case LOCAL :
		        	//We will set default password in sql builder
		            break;
		        case EXTERNAL : break;
		        case GLOBAL : 
		        	//Now globalname is required
		        	caAttributes.globalName = OracleConnectorHelper.getNotEmptyStringValue(map, ORACLE_GLOBAL_ATTR_NAME);
		        	break;
	        }
        }
    }

}
