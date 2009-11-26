/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.SPIOperation;

/**
 * Full normalizer normalizes both input and ouput attributes
 * @author kitko
 *
 */
final class OracleFullNormalizer implements OracleAttributeNormalizer {
	
    private static final Map<String,OracleUserAttribute> mapping = new TreeMap<String, OracleUserAttribute>(OracleConnectorHelper.getAttributeNamesComparator());
    static {
        mapping.put(Name.NAME, OracleUserAttribute.USER);
        mapping.put(Uid.NAME, OracleUserAttribute.USER);
        mapping.put(OracleConstants.ORACLE_GLOBAL_ATTR_NAME, OracleUserAttribute.GLOBAL_NAME);
        mapping.put(OracleConstants.ORACLE_ROLES_ATTR_NAME, OracleUserAttribute.ROLE);
        mapping.put(OracleConstants.ORACLE_PRIVS_ATTR_NAME, OracleUserAttribute.PRIVILEGE);
        mapping.put(OracleConstants.ORACLE_PROFILE_ATTR_NAME, OracleUserAttribute.PROFILE);
        mapping.put(OracleConstants.ORACLE_DEF_TS_ATTR_NAME, OracleUserAttribute.DEF_TABLESPACE);
        mapping.put(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, OracleUserAttribute.TEMP_TABLESPACE);
        
    }
    
    private final OracleCaseSensitivitySetup cs;
    
    OracleFullNormalizer(OracleCaseSensitivitySetup cs){
    	this.cs = cs;
    }
    
    public Set<Attribute> normalizeAttributes(ObjectClass objectClass,Class<? extends SPIOperation> op, Set<Attribute> attrs){
    	return attrs;
    }

	public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        if(attribute == null){
        	return null;
        }
		String name = attribute.getName();
        final OracleUserAttribute oracleUserAttribute = mapping.get(name);
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
	
	public Pair<String,GuardedString> normalizeAuthenticateEntry(String username, GuardedString password){
		String nusername = cs.normalizeToken(OracleUserAttribute.USER, username);
		GuardedString npassword = cs.normalizeToken(OracleUserAttribute.PASSWORD, password);
		return new Pair<String, GuardedString>(nusername, npassword);
	}


}
