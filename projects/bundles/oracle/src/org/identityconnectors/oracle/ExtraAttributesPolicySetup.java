package org.identityconnectors.oracle;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import static org.identityconnectors.oracle.OracleMessages.*;

interface ExtraAttributesPolicySetup {
	public ExtraAttributesPolicy getPolicy(OracleUserAttribute attribute, Class<? extends SPIOperation> operation);
}

final class ExtraAttributesPolicySetupBuilder{
	private final Map<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy> policies;
	private final ConnectorMessages cm;
	
	
	ExtraAttributesPolicySetupBuilder(ConnectorMessages cm){
		policies = new LinkedHashMap<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy>();
		this.cm = OracleConnectorHelper.assertNotNull(cm, "cm");
	}
	
	ExtraAttributesPolicySetup build(){
		final Map<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy> policies = new LinkedHashMap<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy>(this.policies);
		for(OracleUserAttribute attribute : OracleUserAttribute.values()){
			for(Class<? extends SPIOperation> operation : FrameworkUtil.allSPIOperations()){
				Pair<OracleUserAttribute,Class<? extends SPIOperation>> pair = new Pair<OracleUserAttribute,Class<? extends SPIOperation>>(attribute, operation);
				ExtraAttributesPolicy policy = policies.get(pair);
				if(policy == null){
					policy = attribute.getExtraAttributesPolicy(operation);
					if(policy == null){
						policy = ExtraAttributesPolicy.FAIL;
					}
					policies.put(pair, policy);
				}
			}
		}
		return new ExtraAttributesPolicySetupImpl(policies);
	}
	
	@SuppressWarnings("unchecked")
	ExtraAttributesPolicySetupBuilder parseArray(String[] policies){
		if(policies == null){
			return this;
		}
		for(String policy : policies){
            final Map<String, Object> map = MapParser.parseMap(policy, cm);
            if(map.size() != 1){
            	throw new IllegalArgumentException(cm.format(MSG_EAP_MUST_SPECIFY_ONE_ARRAY_ELEMENT, null));
            }
            String attributeName = map.keySet().iterator().next();
            Map<String, String> elementMap = (Map<String, String>) map.values().iterator().next();
            parseAttributeMap(attributeName, elementMap);
		}
		return this;
	}
	
	private void parseAttributeMap(String attributeName, Map<String, String> elementMap) {
        if("ALL".equalsIgnoreCase(attributeName)){
            for(OracleUserAttribute attribute : OracleUserAttribute.values()){
            	parseAttribute(attribute, elementMap, false);
            }
            return;
        }
		OracleUserAttribute attribute = OracleUserAttribute.valueOf(attributeName);
		//Keys of the map should be simple names of SPIOperations
		parseAttribute(attribute, elementMap, true);
	}

	private void parseAttribute(OracleUserAttribute attribute,Map<String, String> aElementMap, boolean overwrite) {
		Map<String, String> elementMap = new HashMap<String, String>(aElementMap);
		for(Iterator<Entry<String, String>> i = elementMap.entrySet().iterator();i.hasNext();){
			Entry<String, String> entry = i.next();
			String opString = entry.getKey();
			String policyString = entry.getValue();
			ExtraAttributesPolicy policy = ExtraAttributesPolicy.valueOf(policyString);
			Class<? extends SPIOperation> operation = resolveOperation(opString);
			definePolicyInternal(attribute, operation, policy, overwrite);
			i.remove();
		}
		if(!elementMap.isEmpty()){
			throw new IllegalArgumentException(cm.format(ORACLE_EAP_INVALID_ELEMENTS_IN_MAP, null, elementMap));
		}
	}
	
	
	
	private Class<? extends SPIOperation> resolveOperation(String opString){
		for(Class<? extends SPIOperation> clazz : FrameworkUtil.allSPIOperations()){
			if(clazz.getName().equals(opString)){
				return clazz;
			};
			String clazzSimpleName = clazz.getSimpleName();
			if(clazzSimpleName.equals(opString)){
				return clazz;
			}
			if(clazzSimpleName.endsWith("Op") && clazzSimpleName.substring(0, clazzSimpleName.length() -2).equalsIgnoreCase(opString)){
				return clazz;
			}
		}
		throw new IllegalArgumentException(cm.format(ORACLE_EAP_CANNOT_RESOLVE_SPI_OPERATION, null, opString));
	}

	@SuppressWarnings("unchecked")
	ExtraAttributesPolicySetupBuilder parseMap(String format){
        if("default".equalsIgnoreCase(format)){
            return this;
        }
        final Map<String, Object> map = MapParser.parseMap(format,cm);
        for(Iterator<Map.Entry<String, Object>> i = map.entrySet().iterator();i.hasNext();){
        	Entry<String, Object> entry = i.next();
        	String attributeName = entry.getKey();
        	Map<String, String> elementMap = (Map<String, String>) entry.getValue();
        	parseAttributeMap(attributeName, elementMap);
        	i.remove();
        }
        if(!map.isEmpty()){
        	throw new IllegalArgumentException(cm.format(ORACLE_EAP_INVALID_ELEMENTS_IN_MAP, null, map));
        }
        return this;
	}
	
	ExtraAttributesPolicySetupBuilder definePolicy(OracleUserAttribute attribute, Class<? extends SPIOperation> operation, ExtraAttributesPolicy policy){
		definePolicyInternal(attribute, operation, policy, true);
		return this;
	}
	
	ExtraAttributesPolicySetupBuilder clearPolicies(){
		policies.clear();
		return this;
	}
	
	private void definePolicyInternal(OracleUserAttribute attribute, Class<? extends SPIOperation> operation, ExtraAttributesPolicy policy, boolean overwrite){
		Pair<OracleUserAttribute,Class<? extends SPIOperation>> pair = new Pair<OracleUserAttribute,Class<? extends SPIOperation>>(attribute, operation);
		if(!overwrite){
			if(policies.containsKey(pair)){
				return ;
			}
		}
		policies.put(pair , policy);
	}
	
	
}


final class ExtraAttributesPolicySetupImpl implements ExtraAttributesPolicySetup {
	private final Map<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy> policies;
	
	ExtraAttributesPolicySetupImpl(Map<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy> policies){
		this.policies = new LinkedHashMap<Pair<OracleUserAttribute,Class<? extends SPIOperation>>, ExtraAttributesPolicy>(policies);
	}
	
	public ExtraAttributesPolicy getPolicy(OracleUserAttribute attribute, Class<? extends SPIOperation> operation) {
		ExtraAttributesPolicy policy = policies.get(new Pair<OracleUserAttribute,Class<? extends SPIOperation>>(attribute, operation));
		if(policy == null){
			//Internal error, need not localize
			throw new IllegalArgumentException(MessageFormat.format("No  policy defined for attribute [{0}] and operation [{1}]", attribute, operation));
		}
		return policy;
	}
	
}