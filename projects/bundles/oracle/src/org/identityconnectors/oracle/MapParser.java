package org.identityconnectors.oracle;

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

/** Very simple parser, that can parse String in format like this <br/>
 *  <code>name=Tom,surname=Scott,address={town=London,street=Some street,number={n1=10,n2=1234}}} </code>
 *  <br/>
 *  No escape characters are supported, it is only internal class for OracleCaseSensitivity building
 * @author kitko
 *
 */
class MapParser {
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
    
    
    public static Map<String,Object> parseMap(String string){
        if(string == null || string.length() == 0){
            return new HashMap<String, Object>(0);
        }
        if(string.charAt(0) == '{'){
            if(string.charAt(string.length() - 1) != '}'){
                throw new RuntimeException(MessageFormat.format("String [{0}] not properly bracket", string));
            }
            string = string.substring(1,string.length() - 1);
        }
        String []entries = parseEntries(string);
        Map<String,Object> map = new HashMap<String, Object>();
        for(String entry : entries){
            Map.Entry<String, Object> mEntry = parseEntry(entry);
            map.put(mEntry.getKey(),mEntry.getValue());
        }
        return map;
    }
    
    
    private static String[] parseEntries(String string) {
        StringBuilder currentEntry = new StringBuilder();
        List<String> entries = new ArrayList<String>();
        for(int i = 0,length=string.length(); i < length; i++){
            char c = string.charAt(i);
            if(c == ','){
                entries.add(currentEntry.toString());
                currentEntry.delete(0, currentEntry.length());
            }
            else if(c == '{'){
                int rightBracketIndex = getRightBracketIndex(string,i);
                currentEntry.append(string.substring(i,rightBracketIndex + 1));
                i = rightBracketIndex;
            }
            else if(c == '}'){
                throw new RuntimeException(MessageFormat.format("Illegal right bracket at index [{0}] for [{1}]",i,string));
            }
            else{
                currentEntry.append(c);
            }
        }
        entries.add(currentEntry.toString());
        return entries.toArray(new String[entries.size()]);
    }


    private static Entry<String, Object> parseEntry(String entry) {
        int eqIndex = entry.indexOf('=');
        if(eqIndex == -1){
            throw new RuntimeException(MessageFormat.format("No '=' character in entry [{0}]",entry));
        }
        if(eqIndex == 0){
            throw new RuntimeException(MessageFormat.format("Character '=' is at the start of entry [{0}]",entry));
        }
        String key = entry.substring(0,eqIndex);
        String valueString = (eqIndex == entry.length() -1) ? null : entry.substring(eqIndex + 1, entry.length());
        Object value = parseValue(valueString);
        return new MyEntry(key, value);
    }


    private static Object parseValue(String value){
        if(value == null || value.length() == 0){
            return value;
        }
        if(value.startsWith("{")){
            if(value.charAt(value.length() - 1) != '}'){
                throw new RuntimeException(MessageFormat.format("String [{0}] not properly bracket", value));
            }
            return parseMap(value);
        }
        return value;
    }
    
    private static int getRightBracketIndex(String string,int leftBracketIndex){
        Stack<Character> stack = new Stack<Character>();
        if(string.charAt(leftBracketIndex) != '{'){
            throw new RuntimeException(MessageFormat.format("No bracket at [{0}] at index [{1}]",string,leftBracketIndex));
        }
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
        throw new RuntimeException(MessageFormat.format("String [{0}] not properly bracket", string));
    }
}
