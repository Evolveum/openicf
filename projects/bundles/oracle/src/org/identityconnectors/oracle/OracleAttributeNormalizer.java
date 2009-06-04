/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.operations.SPIOperation;

/**
 * Normalizer of oracle attributes.
 * It is not standard {@link AttributeNormalizer}, it just normalizes attributes before Create and Update op, but it is never called for normalizing output
 * @author kitko
 *
 */
final class OracleAttributeNormalizer  {
    private static final Map<String,OracleUserAttribute> attributeMapping = new TreeMap<String, OracleUserAttribute>(OracleConnectorHelper.getAttributeNamesComparator());
    static {
        attributeMapping.put(Name.NAME, OracleUserAttribute.USER);
        //UID should never be normalized
        //attributeMapping.put(Uid.NAME, OracleUserAttributeCS.USER);
        attributeMapping.put(OracleConstants.ORACLE_GLOBAL_ATTR_NAME, OracleUserAttribute.GLOBAL_NAME);
        attributeMapping.put(OracleConstants.ORACLE_ROLES_ATTR_NAME, OracleUserAttribute.ROLE);
        attributeMapping.put(OracleConstants.ORACLE_PRIVS_ATTR_NAME, OracleUserAttribute.PRIVILEGE);
        attributeMapping.put(OracleConstants.ORACLE_PROFILE_ATTR_NAME, OracleUserAttribute.PROFILE);
        attributeMapping.put(OracleConstants.ORACLE_DEF_TS_ATTR_NAME, OracleUserAttribute.DEF_TABLESPACE);
        attributeMapping.put(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, OracleUserAttribute.TEMP_TABLESPACE);
    }
    
    private final OracleCaseSensitivitySetup cs;
    
    OracleAttributeNormalizer(OracleCaseSensitivitySetup cs){
    	this.cs = cs;
    }
    
    Set<Attribute> normalizeAttributes(ObjectClass objectClass,Class<? extends SPIOperation> op, Set<Attribute> attrs){
    	Set<Attribute> res = new HashSet<Attribute>();
    	for(Attribute attr : attrs){
    		Attribute normalizedAttr = normalizeAttribute(objectClass, op, attr);
    		if(normalizedAttr != null){
    			res.add(normalizedAttr);
    		}
    	}
    	return res;
    }
    
	
	Attribute normalizeAttribute(ObjectClass oclass, Class<? extends SPIOperation> op, Attribute attribute) {
        if(attribute == null){
        	return null;
        }
		String name = attribute.getName();
        final OracleUserAttribute oracleUserAttribute = attributeMapping.get(name);
        if(oracleUserAttribute == null){
            return attribute;
        }
        if(attribute.getValue() == null){
        	return attribute;
        }
        List<Object> values = new ArrayList<Object>();
        for(Object o : attribute.getValue()){
            if(o instanceof String){
                o = cs.normalizeToken(oracleUserAttribute, (String) o);
            }
            else if(o instanceof GuardedString){
            	o = cs.normalizeToken(oracleUserAttribute, (GuardedString) o);
            }
            values.add(o);
        }
        return AttributeBuilder.build(name,values);
	}

}
