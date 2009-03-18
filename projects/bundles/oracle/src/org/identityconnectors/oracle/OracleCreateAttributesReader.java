package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.*;

import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;

/** Transforms attributes from Set<Attribute> attrs to {@link OracleUserAttributes} */
class OracleCreateAttributesReader {
     ConnectorMessages messages;
     
     OracleCreateAttributesReader(ConnectorMessages messages){
         this.messages = OracleConnectorHelper.assertNotNull(messages, "messages");
     }
    
     void readCreateRestAttributes(Set<Attribute> attrs, OracleUserAttributes caAttributes) {
        caAttributes.expirePassword = OracleConnectorHelper.getBooleanValue(attrs, OperationalAttributes.PASSWORD_EXPIRED_NAME);
        caAttributes.defaultTableSpace = OracleConnectorHelper.getStringValue(attrs, ORACLE_DEF_TS_ATTR_NAME);
        caAttributes.tempTableSpace = OracleConnectorHelper.getStringValue(attrs, ORACLE_TEMP_TS_ATTR_NAME);
        caAttributes.enable = OracleConnectorHelper.getBooleanValue(attrs, OperationalAttributes.ENABLE_NAME);
        caAttributes.profile = OracleConnectorHelper.getStringValue(attrs, ORACLE_PROFILE_ATTR_NAME);
        Attribute defaultTSQuota = AttributeUtil.find(ORACLE_DEF_TS_QUOTA_ATTR_NAME, attrs);
        if(defaultTSQuota != null){
            caAttributes.defaultTSQuota = new Quota(AttributeUtil.getStringValue(defaultTSQuota));
        }
        Attribute tempTSQuota = AttributeUtil.find(ORACLE_TEMP_TS_QUOTA_ATTR_NAME, attrs);
        if(tempTSQuota != null){
            caAttributes.tempTSQuota = new Quota(AttributeUtil.getStringValue(tempTSQuota));
        }
    }

    void readCreateAuthAttributes(Set<Attribute> attrs, OracleUserAttributes caAttributes) {
        String authentication =  OracleConnectorHelper.getStringValue(attrs, ORACLE_AUTHENTICATION_ATTR_NAME);
        if(authentication == null){
            authentication = ORACLE_AUTH_LOCAL; 
        }
        if(ORACLE_AUTH_LOCAL.equals(authentication)){
            caAttributes.auth = OracleAuthentication.LOCAL;
            GuardedString password = AttributeUtil.getPasswordValue(attrs);
            if(password == null){
                if(caAttributes.userName == null){
                    throw new IllegalArgumentException("Cannot choose default passoword, username is null");
                }
                password = new GuardedString(caAttributes.userName.toCharArray());
            }
            caAttributes.password = password;
        }
        else if(ORACLE_AUTH_EXTERNAL.equals(authentication)){
            caAttributes.auth = OracleAuthentication.EXTERNAL;
        }
        else if(ORACLE_AUTH_GLOBAL.equals(authentication)){
            caAttributes.auth = OracleAuthentication.GLOBAL;
            caAttributes.globalName = OracleConnectorHelper.getNotEmptyStringValue(attrs, ORACLE_GLOBAL_ATTR_NAME);
        }
        else{
            throw new IllegalArgumentException(messages.format(OracleMessages.INVALID_AUTH, OracleMessages.INVALID_AUTH, authentication));
        }
    }

}
