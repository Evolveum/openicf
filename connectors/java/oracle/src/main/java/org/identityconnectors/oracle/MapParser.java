package org.identityconnectors.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import static org.identityconnectors.oracle.OracleMessages.*;
import org.identityconnectors.framework.common.objects.ConnectorMessages;

/** Very simple parser, that can parse String in format like this <br/>
 *  <code>name=Tom,surname=Scott,address={town=London,street=Some street,number={n1=10,n2=1234}}} </code>
 *  <br/>
 *  No escape characters are supported, it is only internal class for OracleCaseSensitivity building
 * @author kitko
 *
 */
final class MapParser {
    private static class MyEntry implements Map.Entry<String, Object>{
        String key;
        Object value;
        MyEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }
        public String getKey() {
            return key;
        }
        public Object getValue() {
            return value;
        }
        public Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }
    
    public static Map<String,Object> parseMap(String string, ConnectorMessages cm){
    	return parseMapInternal(string, cm);
    }

    
    private static Map<String,Object> parseMapInternal(String string, ConnectorMessages cm){
        if(string == null || string.length() == 0){
            return new HashMap<String, Object>(0);
        }
        if(string.charAt(0) == '{'){
            if(string.charAt(string.length() - 1) != '}'){
                throw new IllegalArgumentException(cm.format(MSG_INVALID_BRACKET, null,string));
            }
            string = string.substring(1,string.length() - 1);
        }
        String []entries = parseEntries(string, cm);
        Map<String,Object> map = new HashMap<String, Object>();
        for(String entry : entries){
            Map.Entry<String, Object> mEntry = parseEntry(entry, cm);
            map.put(mEntry.getKey(),mEntry.getValue());
        }
        return map;
    }
    
    
    
    private static String[] parseEntries(String string, ConnectorMessages cm) {
        StringBuilder currentEntry = new StringBuilder();
        List<String> entries = new ArrayList<String>();
        for(int i = 0,length=string.length(); i < length; i++){
            char c = string.charAt(i);
            if(c == ','){
                entries.add(currentEntry.toString());
                currentEntry.delete(0, currentEntry.length());
            }
            else if(c == '{'){
                int rightBracketIndex = getRightBracketIndex(string,i, cm);
                currentEntry.append(string.substring(i,rightBracketIndex + 1));
                i = rightBracketIndex;
            }
            else if(c == '}'){
                throw new IllegalArgumentException(cm.format(MSG_ILLEGAL_RIGHT_BRACKET, null,i,string));
            }
            else{
                currentEntry.append(c);
            }
        }
        entries.add(currentEntry.toString());
        return entries.toArray(new String[entries.size()]);
    }


    private static Entry<String, Object> parseEntry(String entry, ConnectorMessages cm) {
        int eqIndex = entry.indexOf('=');
        if(eqIndex == -1){
            throw new IllegalArgumentException(cm.format(MSG_NO_EQ_CHAR_IN_ENTRY, null,entry));
        }
        if(eqIndex == 0){
            throw new IllegalArgumentException(cm.format(MSG_CHAR_EQ_IS_AT_START_OF_ENTRY,null,entry));
        }
        String key = entry.substring(0,eqIndex);
        String valueString = (eqIndex == entry.length() -1) ? null : entry.substring(eqIndex + 1, entry.length());
        Object value = parseValue(valueString, cm);
        return new MyEntry(key, value);
    }


    private static Object parseValue(String value, ConnectorMessages cm){
        if(value == null || value.length() == 0){
            return value;
        }
        if(value.startsWith("{")){
            if(value.charAt(value.length() - 1) != '}'){
                throw new IllegalArgumentException(cm.format(MSG_INVALID_BRACKET,null, value));
            }
            return parseMapInternal(value, cm);
        }
        return value;
    }
    
    private static int getRightBracketIndex(String string,int leftBracketIndex, ConnectorMessages cm){
        Stack<Character> stack = new Stack<Character>();
        for(int i = leftBracketIndex,length = string.length();i < length;i++){
            char c = string.charAt(i);
            if(c == '{'){
                stack.push('{');
            }
            else if(c == '}'){
                stack.pop();
            }
            if(stack.isEmpty()){
                return i;
            }
        }
        throw new IllegalArgumentException(cm.format(MSG_INVALID_BRACKET,null, string));
    }
}
