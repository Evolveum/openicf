package org.identityconnectors.oracle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

/** Helper static methods related to Oracle connector */
abstract class OracleConnectorHelper {
	private OracleConnectorHelper(){}
	
    static String getRequiredStringValue(Map<String, Attribute> attrs, String name, ConnectorMessages cm){
        Attribute attr = attrs.get(name);
        if(attr == null){
            throw new IllegalArgumentException(cm.format("oracle.attribute.is.missing", null, name));
        }
        return AttributeUtil.getStringValue(attr);
    }

    static String getNotEmptyStringValue(Map<String, Attribute> attrs, String name, ConnectorMessages cm){
        String value = getRequiredStringValue(attrs, name, cm);
        if(StringUtil.isEmpty(value)){
            throw new IllegalArgumentException(cm.format("oracle.attribute.is.empty", null, name));
        }
        return value;
         
    }

    static String getStringValue(Map<String, Attribute> attrs, String name, ConnectorMessages cm){
        Attribute attr = attrs.get(name);
        return attr != null ? AttributeUtil.getStringValue(attr) : null;
    }
    
    
    static String getNotNullAttributeNotEmptyStringValue(Map<String, Attribute> attrs, String name, ConnectorMessages cm){
    	Attribute attr = attrs.get(name);
    	if(attr == null){
    		return null;
    	}
    	return getNotEmptyStringValue(attrs, name, cm);
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

    
    static Boolean getNotNullAttributeBooleanValue(Map<String, Attribute> attrs, String name, ConnectorMessages cm){
        Attribute attr = attrs.get(name);
        if(attr == null){
            return null;
        }
        Object value = AttributeUtil.getSingleValue(attr);
        if(value instanceof Boolean){
            return (Boolean) value;
        }
        throw new IllegalArgumentException(cm.format("oracle.boolean.attribute.has.invalid.value", null, name, value));
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
    
    /** Later switch to framework implementation */ 
    static class AttributeComparator implements Comparator<String>{
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
    }
    
    static Comparator<String> getAttributeNamesComparator(){
    	return new AttributeComparator();
    }
    

}
