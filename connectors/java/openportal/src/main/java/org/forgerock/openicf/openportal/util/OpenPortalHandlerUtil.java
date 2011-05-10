/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.forgerock.openicf.openportal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;


/**
 *
 * @author admin
 */
public class OpenPortalHandlerUtil {

    public static List<String> findAttributeValue(Attribute attr, AttributeInfo attrInfo){
        String javaClass = attrInfo.getType().getName();
        List<String> results = new ArrayList<String>();
        String stringValue = null;

        if(attr != null && attr.getValue() != null && attrInfo != null){
            for(Object value : attr.getValue()){
                Class clazz;
                try{
                    if (attrInfo.getType().isPrimitive())
                        javaClass = convertPrimitiveToWrapper(attrInfo.getType().getName()).getName();

                    clazz = Class.forName(javaClass);

                    if(!clazz.isInstance(value)){
                        throw new IllegalArgumentException(attrInfo.getName() + " contains invalid type. Value(s) should be of type " + clazz.getName());
                    }
                }catch(ClassNotFoundException ex){
                    throw ConnectorException.wrap(ex);
                }
                if(javaClass.equals("org.identityconnectors.common.security.GuardedString")){
                    GuardedStringAccessor accessor = new GuardedStringAccessor();
                    GuardedString gs = AttributeUtil.getGuardedStringValue(attr);
                    gs.access(accessor);
                    stringValue = String.valueOf(accessor.getArray());

                }else if(javaClass.equals("org.identityconnectors.common.security.GuardedByteArray")){
                    GuardedByteArrayAccessor accessor = new GuardedByteArrayAccessor();
                    GuardedByteArray gba = (GuardedByteArray)attr.getValue().get(0);
                    gba.access(accessor);
                    stringValue = new String(accessor.getArray());
                }else{
                    stringValue = value.toString();
                }
                results.add(stringValue);
            }
        }
        return results;
    }
    private static final Map<String, Class<?>> primitiveMap = new HashMap<String, Class<?>>();
    static{
        primitiveMap.put("boolean", Boolean.class);
        primitiveMap.put("byte", Byte.class);
        primitiveMap.put("short", Short.class);
        primitiveMap.put("char", Character.class);
        primitiveMap.put("int", Integer.class);
        primitiveMap.put("long", Long.class);
        primitiveMap.put("float", Float.class);
        primitiveMap.put("double", Double.class);
    }
    private static Class convertPrimitiveToWrapper(String name) {
        return primitiveMap.get(name);
    }
    
}
