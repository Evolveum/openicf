package org.identityconnectors.oracle;

import java.util.*;

import org.identityconnectors.common.security.GuardedString;

/** OracleCaseSensitivity is responsible for formatting oracle objects tokens (users,schema...)
 *  Maybe we do not need such granurality, and have just one settings for all objects, but using this scenarion
 *  we should cover all corner cases.
 *  For user we will provide some simplification how to define this caseSensitivy setup 
 * */
interface OracleCaseSensitivity {
    public String formatUserName(String name);
    public String formatSchemaName(String schema);
    public String formatPasswordString(String password);
    public GuardedString formatPassword(GuardedString password);
    public String formatProfile(String profile);
    public String formatDefaultTableSpace(String defTabSpace);
    public String formatTempTableSpace(String tempTableSpace);
    public String formatGlobalName(String globalName);
    public CSTokenFormatter getAttributeFormatter(OracleUserAttribute attribute);
}

/** Oracle user attribute */
enum OracleUserAttribute {
    USER_NAME,
    PASSWORD,
    SCHEMA,
    PROFILE,
    DEF_TABLESPACE,
    TEMP_TABLESPACE,
    GLOBAL_NAME(true,"'");
    OracleUserAttribute(){}
    OracleUserAttribute(boolean defToUpper,String defQuatesChar){
        this.defToUpper = true;
        this.defQuatesChar = defQuatesChar;
    }
    private boolean defToUpper = true;
    private String defQuatesChar = "\"";
    boolean isDefToUpper() {
        return defToUpper;
    }
    String getDefQuatesChar() {
        return defQuatesChar;
    }
    
}

/** Formatter that formats given token. 
 *  It can make it upper case, append quates etc.
 * @author kitko
 *
 */
final class CSTokenFormatter{
    private OracleUserAttribute attribute;
    private boolean toUpper;
    private String quatesChar = "";
    OracleUserAttribute getAttribute() {
        return attribute;
    }
    boolean isToUpper() {
        return toUpper;
    }
    String getQuatesChar() {
        return quatesChar;
    }
    private char[] formatToken(char []token){
        if(token == null || token.length == 0){
            return token;
        }
        StringBuilder builder = new StringBuilder(token.length + 2);
        builder.append(quatesChar);
        for(char c : token){
            builder.append(toUpper ? Character.toUpperCase(c) : c);
        }
        builder.append(quatesChar);
        final char[] result = new char[builder.length()];
        builder.getChars(0, builder.length(), result, 0);
        return result;
    }
    String formatToken(String token){
        if(token == null || token.length() == 0){
            return token;
        }
        return new String(formatToken(token.toCharArray()));
    }
    
    GuardedString formatToken(GuardedString gs){
        final GuardedString[] result = new GuardedString[1];
        gs.access(new GuardedString.Accessor(){
            public void access(char[] clearChars) {
                char[] newPassword = formatToken(clearChars);
                result[0] = new GuardedString(newPassword);
                Arrays.fill(newPassword, (char)0);
            }
        });
        return result[0];
        
    }
    private CSTokenFormatter(){}
    
    final static class CSTokenFormatterBuilder{
        CSTokenFormatter element = new CSTokenFormatter();
        CSTokenFormatterBuilder setAttribute(OracleUserAttribute attribute){
            element.attribute = attribute;
            return this;
        }
        CSTokenFormatterBuilder setToUpper(boolean toUpper){
            element.toUpper = toUpper;
            return this;
        }
        CSTokenFormatterBuilder setQuatesChar(String quatesChar){
            element.quatesChar = quatesChar;
            return this;
        }
        CSTokenFormatterBuilder setValues(Map<String, Object> aMap){
            Map<String, Object> map = new HashMap<String, Object>(aMap);
            element.toUpper = Boolean.valueOf((String)map.remove("upper"));
            String quates = (String) map.remove("quates");
            if(quates != null){
                element.quatesChar = quates;
            }
            if(!map.isEmpty()){
                throw new RuntimeException("Some elements in aMap not recognized : " + map);
            }
            return this;
        }
        CSTokenFormatterBuilder setDefaultValues(){
            //We can have different defaults for each attribute
            if(element.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            element.quatesChar = element.attribute.getDefQuatesChar();
            element.toUpper = element.attribute.isDefToUpper();
            return this;
        }
        CSTokenFormatter build(){
            if(element.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            return element;
        }
        CSTokenFormatter buildWithDefaultValues(OracleUserAttribute attribute){
            setAttribute(attribute);
            setDefaultValues();
            return build();
        }
    }

}


/** Builder of OracleCaseSensitivity using formatters for user attributes */
final class OracleCaseSensitivityBuilder{
    private Map<OracleUserAttribute,CSTokenFormatter> formatters = new HashMap<OracleUserAttribute, CSTokenFormatter>(6);
    
    OracleCaseSensitivityBuilder defineFormatters(CSTokenFormatter... formatters){
        for(CSTokenFormatter element : formatters){
            this.formatters.put(element.getAttribute(),element);
        }
        return this;
    }
    
    @SuppressWarnings("unchecked")
    OracleCaseSensitivityBuilder defineFormatters(String format){
        //If we set string default, all formatters will have its default value
        if("default".equalsIgnoreCase(format)){
            return this;
        }
        final Map<String, Object> map = MapParser.parseMap(format);
        for(String attributeName : map.keySet()){
            Map<String, Object> elementMap = (Map<String, Object>) map.get(attributeName);
            if("ALL".equalsIgnoreCase(attributeName)){
                for(OracleUserAttribute attribute : OracleUserAttribute.values()){
                    this.formatters.put(attribute, new CSTokenFormatter.CSTokenFormatterBuilder().setAttribute(attribute).setValues(elementMap).build());
                }
                continue;
            }
            OracleUserAttribute attribute = OracleUserAttribute.valueOf(attributeName);
            CSTokenFormatter element = new CSTokenFormatter.CSTokenFormatterBuilder().setAttribute(attribute).setValues(elementMap).build(); 
            this.formatters.put(element.getAttribute(),element);    
        }
        return this;
    }
    
    OracleCaseSensitivity build(){
        Map<OracleUserAttribute,CSTokenFormatter> formatters = new HashMap<OracleUserAttribute, CSTokenFormatter>(this.formatters);
        //If any elements is not defined in specified map set default value
        for(OracleUserAttribute attribute : OracleUserAttribute.values()){
            CSTokenFormatter formatter = formatters.get(attribute);
            if(formatter == null){
                formatter = new CSTokenFormatter.CSTokenFormatterBuilder().buildWithDefaultValues(attribute);
                formatters.put(attribute,formatter);
            }
        }
        return new OracleCaseSensitivityImpl(formatters);
    }
    
}

final class OracleCaseSensitivityImpl implements OracleCaseSensitivity{
    private Map<OracleUserAttribute,CSTokenFormatter> elements;
    
    OracleCaseSensitivityImpl(Map<OracleUserAttribute, CSTokenFormatter> elements) {
        this.elements = new HashMap<OracleUserAttribute, CSTokenFormatter>(elements);
    }
    public CSTokenFormatter getAttributeFormatter(OracleUserAttribute attribute){
        final CSTokenFormatter element = elements.get(attribute);
        if(element == null){
            throw new IllegalArgumentException("No element defined for attribute " + attribute);
        }
        return element;
    }
    public String formatDefaultTableSpace(String defTabSpace) {
        return getAttributeFormatter(OracleUserAttribute.DEF_TABLESPACE).formatToken(defTabSpace);
    }
    public GuardedString formatPassword(GuardedString password) {
        return getAttributeFormatter(OracleUserAttribute.PASSWORD).formatToken(password);
    }

    public String formatPasswordString(String password) {
        return getAttributeFormatter(OracleUserAttribute.PASSWORD).formatToken(password);
    }

    public String formatProfile(String profile) {
        return getAttributeFormatter(OracleUserAttribute.PROFILE).formatToken(profile);
    }

    public String formatSchemaName(String schema) {
        return getAttributeFormatter(OracleUserAttribute.PASSWORD).formatToken(schema);
    }

    public String formatTempTableSpace(String tempTableSpace) {
        return getAttributeFormatter(OracleUserAttribute.PASSWORD).formatToken(tempTableSpace);
    }

    public String formatUserName(String name) {
        return getAttributeFormatter(OracleUserAttribute.USER_NAME).formatToken(name);
    }
    public String formatGlobalName(String globalName) {
        return getAttributeFormatter(OracleUserAttribute.GLOBAL_NAME).formatToken(globalName);
    }
    
}



 
