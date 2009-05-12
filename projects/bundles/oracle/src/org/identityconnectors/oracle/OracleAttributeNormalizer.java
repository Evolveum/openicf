/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.AttributeNormalizer;

/**
 * Normalizer of oracle attributes
 * @author kitko
 *
 */
final class OracleAttributeNormalizer implements AttributeNormalizer {
    private static final Map<String,OracleUserAttributeCS> attributeMapping = new HashMap<String, OracleUserAttributeCS>();
    static {
        attributeMapping.put(Name.NAME, OracleUserAttributeCS.USER);
        attributeMapping.put(Uid.NAME, OracleUserAttributeCS.USER);
        attributeMapping.put(OracleConstants.ORACLE_GLOBAL_ATTR_NAME, OracleUserAttributeCS.GLOBAL_NAME);
        attributeMapping.put(OracleConstants.ORACLE_ROLES_ATTR_NAME, OracleUserAttributeCS.ROLE);
        attributeMapping.put(OracleConstants.ORACLE_PRIVS_ATTR_NAME, OracleUserAttributeCS.PRIVILEGE);
        attributeMapping.put(OracleConstants.ORACLE_PROFILE_ATTR_NAME, OracleUserAttributeCS.PROFILE);
        attributeMapping.put(OracleConstants.ORACLE_DEF_TS_ATTR_NAME, OracleUserAttributeCS.DEF_TABLESPACE);
        attributeMapping.put(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, OracleUserAttributeCS.TEMP_TABLESPACE);
    }
    
    private final OracleConfiguration cfg;
    
    OracleAttributeNormalizer(OracleConfiguration cfg){
    	this.cfg = cfg;
    }
	
	public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
    	if(attribute == null){
    		return null;
    	}
        String name = attribute.getName();
        final OracleUserAttributeCS oracleUserAttribute = attributeMapping.get(name);
        if(oracleUserAttribute == null){
            return attribute;
        }
        List<Object> values = new ArrayList<Object>();
        if(attribute.getValue() == null){
        	return attribute;
        }
        for(Object o : attribute.getValue()){
            if(o instanceof String){
                o = cfg.getCSSetup().normalizeToken(oracleUserAttribute, (String) o);
            }
            else if(o instanceof GuardedString){
            	o = cfg.getCSSetup().normalizeToken(oracleUserAttribute, (GuardedString) o);
            }
            values.add(o);
        }
        return AttributeBuilder.build(name,values);
	}

}
