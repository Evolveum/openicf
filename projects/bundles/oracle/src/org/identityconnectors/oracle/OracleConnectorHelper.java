package org.identityconnectors.oracle;

import java.util.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.*;

/** Helper static methods related to Oracle DB */
abstract class OracleConnectorHelper {

    static String getRequiredStringValue(Set<Attribute> attrs, String name){
        Attribute attr = AttributeUtil.find(name, attrs);
        if(attr == null){
            throw new IllegalArgumentException("No attribute with name  [" + name + "] found in set");
        }
        return AttributeUtil.getStringValue(attr);
    }

    static String getNotEmptyStringValue(Set<Attribute> attrs, String name){
        String value = getRequiredStringValue(attrs, name);
        if(StringUtil.isEmpty(value)){
            throw new IllegalArgumentException("Attribute with name [" + name + "] is empty");
        }
        return value;
         
    }

    static String getStringValue(Set<Attribute> attrs, String name){
        Attribute attr = AttributeUtil.find(name, attrs);
        return attr != null ? AttributeUtil.getStringValue(attr) : null;
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

    
    static Boolean getBooleanValue(Set<Attribute> attrs, String name){
        Attribute attr = AttributeUtil.find(name, attrs);
        if(attr == null){
            return null;
        }
        Object value = AttributeUtil.getSingleValue(attr);
        if(value instanceof Boolean){
            return (Boolean) value;
        }
        if(value instanceof String){
            return "true".equals(value);
        }
        return null;
    }
    
    

}
