package org.identityconnectors.oracle;

import java.text.MessageFormat;
import java.util.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.*;

/** Helper static methods related to Oracle connector */
abstract class OracleConnectorHelper {
	private OracleConnectorHelper(){}
    static String getRequiredStringValue(Map<String, Attribute> attrs, String name){
        Attribute attr = attrs.get(name);
        if(attr == null){
            throw new IllegalArgumentException("No attribute with name  [" + name + "] found in set");
        }
        return AttributeUtil.getStringValue(attr);
    }

    static String getNotEmptyStringValue(Map<String, Attribute> attrs, String name){
        String value = getRequiredStringValue(attrs, name);
        if(StringUtil.isEmpty(value)){
            throw new IllegalArgumentException("Attribute with name [" + name + "] is empty");
        }
        return value;
         
    }

    static String getStringValue(Map<String, Attribute> attrs, String name){
        Attribute attr = attrs.get(name);
        return attr != null ? AttributeUtil.getStringValue(attr) : null;
    }
    
    
    static String getNotNullAttributeNotEmptyStringValue(Map<String, Attribute> attrs, String name){
    	Attribute attr = attrs.get(name);
    	if(attr == null){
    		return null;
    	}
        String value = AttributeUtil.getStringValue(attr);
		if(StringUtil.isEmpty(value)){
            throw new IllegalArgumentException("Attribute with name [" + name + "] is empty");
        }
		return value;
    }

    
    static <O,T> List<T> castList(List<O> source,Class<T> resultType){
        if(source == null){
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<T>(source.size());
        for(Object o : source){
            result.add(resultType.cast(o));
        }
        return result;
    }
    
    static <O,T> List<T> castList(Attribute attr,Class<T> resultType){
        if(attr == null){
            return Collections.emptyList();
        }
        return castList(attr.getValue(),resultType);
    }

    
    static Boolean getNotNullAttributeBooleanValue(Map<String, Attribute> attrs, String name){
        Attribute attr = attrs.get(name);
        if(attr == null){
            return null;
        }
        Object value = AttributeUtil.getSingleValue(attr);
        if(value instanceof Boolean){
            return (Boolean) value;
        }
        throw new IllegalArgumentException(MessageFormat.format("Boolean attribute [{0}] has invalid value [{1}]",name,value));
    }
    
    static <T> T assertNotNull(T t,String argument){
        if(t == null){
            throw new IllegalArgumentException("Passed argument [" + argument + "] is null");
        }
        return t;
    }
    
    static Attribute buildSingleAttribute(String name,Object value){
    	if(value != null){
    		return AttributeBuilder.build(name, value);
    	}
    	else{
    		return AttributeBuilder.build(name);
    	}
    }
    
    
    
    

}
