package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.*;

import java.util.Map;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/** Transforms attributes from Set<Attribute> attrs to {@link OracleUserAttributes}.
 *  It checks just nullability of attributes and makes some possible value checks.
 *  It does not do any additional logical checks, it is mainly reader of attributes to the helper structure.
 * */
final class OracleAttributesReader {
     private final ConnectorMessages cm;
     
     OracleAttributesReader(ConnectorMessages messages){
         this.cm = OracleConnectorHelper.assertNotNull(messages, "messages");
     }
    
     void readCreateAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes){
    	 readAuthAttributes(map, caAttributes, CreateOp.class);
    	 readRestAttributes(map, caAttributes, CreateOp.class);
     }
     
     void readAlterAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes){
    	 readAuthAttributes(map, caAttributes, UpdateOp.class);
    	 readRestAttributes(map, caAttributes, UpdateOp.class);
     }
     
     
     
     private void readRestAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes, Class<? extends SPIOperation> operation) {
        caAttributes.setExpirePassword(OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.PASSWORD_EXPIRED_NAME, cm));
        caAttributes.setDefaultTableSpace(OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, OracleConstants.ORACLE_DEF_TS_ATTR_NAME, cm));
        caAttributes.setTempTableSpace(OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, cm));
        Boolean enabled = OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.ENABLE_NAME, cm);
        Boolean lockOut = OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.LOCK_OUT_NAME, cm);
        if(enabled != null && lockOut != null){
        	//enable and lock must have different values , throw separate message for each case
        	if(enabled && lockOut){
        		throw new IllegalArgumentException(cm.format(MSG_ENABLE_LOCK_ATTR_VALUE_CONFLICT_TRUE, null));
        	}
        	if(!enabled && !lockOut){
        		throw new IllegalArgumentException(cm.format(MSG_ENABLE_LOCK_ATTR_VALUE_CONFLICT_FALSE, null));
        	}
        }
        
        if(enabled != null){
        	caAttributes.setEnable(enabled);
        }
        if(lockOut != null){
        	caAttributes.setEnable(!lockOut);
        }
        caAttributes.setProfile(OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, OracleConstants.ORACLE_PROFILE_ATTR_NAME, cm));
        
        Attribute defaultTSQuota = map.get(OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME);
        if(defaultTSQuota != null){
        	String val = AttributeUtil.getStringValue(defaultTSQuota);
        	if(StringUtil.isBlank(val)){
        		//when updating to null, actuall we want to drop quouta information and this will
        		//be done altering to 0
        		caAttributes.setDefaultTSQuota("0");
        	}
        	else{
        		caAttributes.setDefaultTSQuota(val);
        	}
        }
        
        Attribute tempTSQuota = map.get(OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME);
        if(tempTSQuota != null){
        	String val = AttributeUtil.getStringValue(tempTSQuota);
        	if(StringUtil.isBlank(val)){
        		//when updating to null, actuall we want to drop quouta information and this will
        		//be done altering to 0
        		caAttributes.setTempTSQuota("0");
        	}
        	else{
        		caAttributes.setTempTSQuota(val);
        	}
        }
    }

    private void readAuthAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes, Class<? extends SPIOperation> operation) {
        String authentication = null;
        if(CreateOp.class.equals(operation)){
        	authentication = OracleConnectorHelper.getStringValue(map, OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, cm);
        }
        else{
        	authentication = OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, cm);
        }
        Attribute passwordAttribute = map.get(OperationalAttributes.PASSWORD_NAME);
        //Set globalname to not silently skip it
        caAttributes.setGlobalName(OracleConnectorHelper.getStringValue(map, OracleConstants.ORACLE_GLOBAL_ATTR_NAME, cm));
        caAttributes.setPassword(passwordAttribute != null ? AttributeUtil.getGuardedStringValue(passwordAttribute) : null);
        if(authentication != null){
	        try{
	        	caAttributes.setAuth(OracleAuthentication.valueOf(authentication));
	        }
	        catch(IllegalArgumentException e){
	        	throw new IllegalArgumentException(cm.format(MSG_INVALID_AUTH, null, authentication));
	        }
	        switch(caAttributes.getAuth()){
		        case LOCAL :
		        	//We will set default password in sql builder
		            break;
		        case EXTERNAL : break;
		        case GLOBAL : 
		        	//Now globalname is required
		        	caAttributes.setGlobalName(OracleConnectorHelper.getNotEmptyStringValue(map, OracleConstants.ORACLE_GLOBAL_ATTR_NAME, cm));
		        	break;
	        }
        }
    }

}
