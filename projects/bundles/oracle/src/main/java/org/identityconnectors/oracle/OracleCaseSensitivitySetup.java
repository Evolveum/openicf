package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.AttributeNormalizer;

/** OracleCaseSensitivity is responsible for normalizing and formatting oracle user tokens (users,schema...).
 *  Maybe we do not need such granularity , and have just one settings for all objects, but using this scenario
 *  we should cover all corner cases.
 *  For user we will provide some simplification how to define this caseSensitivy setup.
 *  This setup is used for formatting and normalizing of user attributes.
 *  
 *  We distinguish between normalizing and formatting of attributes.
 *  Normalizing is called from framework by {@link AttributeNormalizer} while delegating to normalizeToken.
 *  
 *  Formating is called when building sql for create/alter where we just e.g append quotes 
 */
interface OracleCaseSensitivitySetup {
	/**
	 * Normalize token, e.g make it uppercase 
	 * @param attr
	 * @param token
	 * @return normalized token
	 */
    public String normalizeToken(OracleUserAttribute attr,String token);
    
    /**
     * Normalizes array token (e.g make password upper case )
     * @param attr
     * @param token
     * @return normalized token
     */
    public char[] normalizeToken(OracleUserAttribute attr,char[] token);
    /**
     * Normalizes GuardedString token (e.g make password upper case )
     * @param attr
     * @param token
     * @return normalized token
     */
    public GuardedString normalizeToken(OracleUserAttribute attr,GuardedString token);
    /**
     * Just format token, e.g append quotes
     * @param attr
     * @param token
     * @return
     */
    public String formatToken(OracleUserAttribute attr,String token);
    /**
     * Formats char array token , useful for password to not convert to String
     * @param attr
     * @param token
     * @return formatted password
     */
    public char[] formatToken(OracleUserAttribute attr,char[] token);
    /**
     * Formats GuardedString , useful for password to not convert to String
     * @param attr
     * @param token
     * @return formatted password
     */
    public GuardedString formatToken(OracleUserAttribute attr,GuardedString token);
    
    
    /**
     * Formats SQL column, eq surround with UPPER sql function
     * @param attr
     * @param sqlColumn
     * @return formatted column
     */
    public String formatSQLColumn(OracleUserAttribute attr, String sqlColumn);
    
    
    /**
     * Compound operation that normalizes and then formats string token
     * @param attr
     * @param token
     * @return normalized and formatted token
     */
    public String normalizeAndFormatToken(OracleUserAttribute attr,String token);
    /**
     * Compound operation that normalizes and then formats char[] token
     * @param attr
     * @param token
     * @return normalized and formatted token
     */
    public char[] normalizeAndFormatToken(OracleUserAttribute attr,char[] token);
    /**
     * Compound operation that normalizes and then formats GuardedString token
     * @param attr
     * @param token
     * @return normalized and formatted token
     */
    public GuardedString normalizeAndFormatToken(OracleUserAttribute attr,GuardedString token);
    /**
     * Gets normalizer by attribute
     * @param attribute
     * @throws  IllegalArgumentException when normalizer by attribute is not found
     * @return CSTokenFormatter by attribute
     */
    public CSAttributeFormatterAndNormalizer getAttributeFormatterAndNormalizer(OracleUserAttribute attribute) throws IllegalArgumentException;
    
}

/** Normalizes and formats token(user attribute like tablespace, profile) for user . 
 *  Normalizing is done using {@link AttributeNormalizer} , while formatting is done when building SQL statement.
 * @author kitko
 *
 */
final class CSAttributeFormatterAndNormalizer{
    private final OracleUserAttribute attribute;
    
    private final Formatting formatting;
    

    OracleUserAttribute getAttribute() {
        return attribute;
    }
    String getQuatesChar() {
		return formatting.getQuatesChar();
	}
    
    boolean isToUpper(){
    	return formatting.isToUpper();
    }
    
    Formatting getFormatting(){
    	return formatting;
    }
    
	String normalizeToken(String token){
        if(token == null || token.length() == 0){
            return token;
        }
        return formatting.isToUpper() ? token.toUpperCase() : token;
    }

    char[] normalizeToken(char[] token){
        if(token == null || token.length == 0){
            return token;
        }
        char[] newToken = new char[token.length];
        for(int i = 0;i < token.length;i++){
        	newToken[i] = formatting.isToUpper() ? Character.toUpperCase(token[i]) : token[i];
        }
        return newToken;
    }
    
    char[] formatToken(char[] token){
        if(token == null){
            return token;
        }
        StringBuilder builder = new StringBuilder(token.length + 2);
        builder.append(formatting.getQuatesChar());
        builder.append(token);
        builder.append(formatting.getQuatesChar());
        char[] dst = new char[builder.length()];
        builder.getChars(0, builder.length() , dst, 0);
        builder.delete(0, builder.length());
        return dst;
    }
    
    String formatToken(String token){
        if(token == null){
            return token;
        }
        return new String(formatToken(token.toCharArray()));
    }
    
    String formatSQLColumn(String sqlColumn){
    	//Never use UPPER function, we would not be ever completely consistent for natively(externally to connector) created accounts   
    	if(formatting.isToUpper()){
    		return sqlColumn;
    	}
    	return sqlColumn;
    }
    
    
    private CSAttributeFormatterAndNormalizer(Builder builder){
    	this.attribute = builder.attribute;
    	boolean toUpper = builder.toUpper != null ? builder.toUpper : attribute.getFormatting().isToUpper();
    	String quatesChar = builder.quatesChar != null ? builder.quatesChar : attribute.getFormatting().getQuatesChar();
    	this.formatting = new Formatting(toUpper, quatesChar);
    }
    
    final static class Builder{
        private OracleUserAttribute attribute;
        private Boolean toUpper;
        private String quatesChar ;
        private final ConnectorMessages cm;
        
        Builder(ConnectorMessages cm){
        	this.cm = cm;
        }
        
        Builder setAttribute(OracleUserAttribute attribute){
            this.attribute = attribute;
            return this;
        }
        Builder setToUpper(boolean toUpper){
            this.toUpper = toUpper;
            return this;
        }
        Builder setQuatesChar(String quatesChar){
            this.quatesChar = quatesChar;
            return this;
        }
        Builder setValues(Map<String, Object> aMap){
            Map<String, Object> map = new HashMap<String, Object>(aMap);
            String toUpper = (String) map.remove("upper");
            if(toUpper != null){
                this.toUpper = Boolean.valueOf(toUpper);
            }
            String quates = (String) map.remove("quates");
            if(quates != null){
                this.quatesChar = quates;
            }
            if(!map.isEmpty()){
                throw new RuntimeException(cm.format(MSG_ELEMENTS_FOR_NORMALIZER_NOT_RECOGNIZED, null, map));
            }
            return this;
        }
        CSAttributeFormatterAndNormalizer build(){
            if(this.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            return new CSAttributeFormatterAndNormalizer(this);
        }
        CSAttributeFormatterAndNormalizer buildWithDefaultValues(OracleUserAttribute attribute){
            this.attribute = attribute;
            this.toUpper = attribute.getFormatting().isToUpper();
            this.quatesChar = attribute.getFormatting().getQuatesChar();
            return build();
        }
    }
}


/** Builder of OracleCaseSensitivity using formatters for user attributes */
final class OracleCaseSensitivityBuilder{
    private final Map<OracleUserAttribute,CSAttributeFormatterAndNormalizer> normalizers = new LinkedHashMap<OracleUserAttribute, CSAttributeFormatterAndNormalizer>(6);
    private final ConnectorMessages cm;
    
    OracleCaseSensitivityBuilder(ConnectorMessages cm){
    	this.cm = cm;
    }
    
    OracleCaseSensitivityBuilder defineFormattersAndNormalizers(CSAttributeFormatterAndNormalizer... normalizers){
        for(CSAttributeFormatterAndNormalizer element : normalizers){
            this.normalizers.put(element.getAttribute(),element);
        }
        return this;
    }
    
    OracleCaseSensitivityBuilder clearFormattersAndNormalizers(){
    	this.normalizers.clear();
    	return this;
    }
    
    
    @SuppressWarnings("unchecked")
    OracleCaseSensitivityBuilder parseMap(String format){
        //If we set string default, all formatters will have its default value
        if("default".equalsIgnoreCase(format)){
            return this;
        }
        final Map<String, Object> map = MapParser.parseMap(format,cm);
        for(Iterator<Map.Entry<String, Object>> i = map.entrySet().iterator();i.hasNext();){
        	Entry<String, Object> entry = i.next();
        	String attributeName = entry.getKey();
        	Map<String, Object> elementMap = (Map<String, Object>) entry.getValue();
        	parseAttributeMap(attributeName, elementMap);
        	i.remove();
        }
        if(!map.isEmpty()){
            throw new IllegalArgumentException(cm.format(MSG_ELEMENTS_FOR_CSBUILDER_NOT_RECOGNIZED, null, map));
        }
        return this;
    }
    
    private void parseAttributeMap(String attributeName, Map<String, Object> elementMap){
        if("ALL".equalsIgnoreCase(attributeName)){
            for(OracleUserAttribute attribute : OracleUserAttribute.values()){
            	if(!this.normalizers.containsKey(attribute)){
            		this.normalizers.put(attribute, new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(attribute).setValues(elementMap).build());
            	}
            }
            return;
        }
        OracleUserAttribute attribute = OracleUserAttribute.valueOf(attributeName);
        CSAttributeFormatterAndNormalizer element = new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(attribute).setValues(elementMap).build(); 
        this.normalizers.put(element.getAttribute(),element);    
    }
    
    @SuppressWarnings("unchecked")
	OracleCaseSensitivityBuilder parseArray(String[] array){
    	if(array == null){
    		return this;
    	}
    	for(String element : array){
            final Map<String, Object> map = MapParser.parseMap(element,cm);
            if(map.size() != 1){
            	throw new IllegalArgumentException(cm.format(MSG_CS_MUST_SPECIFY_ONE_ARRAY_ELEMENT, null));
            }
            String attributeName = map.keySet().iterator().next();
            Map<String, Object> elementMap = (Map<String, Object>) map.values().iterator().next();
            parseAttributeMap(attributeName, elementMap);
    	}
    	return this;
    }
    
    
    OracleCaseSensitivitySetup build(){
        Map<OracleUserAttribute,CSAttributeFormatterAndNormalizer> normalizers = new HashMap<OracleUserAttribute, CSAttributeFormatterAndNormalizer>(this.normalizers);
        //If any elements is not defined in specified map set default value
        for(OracleUserAttribute attribute : OracleUserAttribute.values()){
            CSAttributeFormatterAndNormalizer normalizer = normalizers.get(attribute);
            if(normalizer == null){
                normalizer = new CSAttributeFormatterAndNormalizer.Builder(cm).buildWithDefaultValues(attribute);
                normalizers.put(attribute,normalizer);
            }
        }
        return new OracleCaseSensitivityImpl(normalizers);
    }
    
}

/**
 * Implementation of OracleCaseSensitivitySetup that will use map of
 * formatters and normalizers for each {@link OracleUserAttribute}
 * @author kitko
 *
 */
final class OracleCaseSensitivityImpl implements OracleCaseSensitivitySetup{
    private final Map<OracleUserAttribute,CSAttributeFormatterAndNormalizer> normalizers;    
    
    OracleCaseSensitivityImpl(Map<OracleUserAttribute,CSAttributeFormatterAndNormalizer> normalizers){
        this.normalizers = new LinkedHashMap<OracleUserAttribute, CSAttributeFormatterAndNormalizer>(normalizers);
    }
    
    public CSAttributeFormatterAndNormalizer getAttributeFormatterAndNormalizer(OracleUserAttribute attribute){
        final CSAttributeFormatterAndNormalizer normalizer = normalizers.get(attribute);
        if(normalizer == null){
        	//This should not happen, builder should set all normalizers, so no need to localize
            throw new IllegalArgumentException("No normalizer defined for attribute " + attribute);
        }
        return normalizer;
    }
    
    public String formatToken(OracleUserAttribute attr, String token) {
        return getAttributeFormatterAndNormalizer(attr).formatToken(token);
    }
    public String normalizeToken(OracleUserAttribute attr, String token) {
        return getAttributeFormatterAndNormalizer(attr).normalizeToken(token);
    }
    public String normalizeAndFormatToken(OracleUserAttribute attr, String token) {
        token = normalizeToken(attr, token);
        token = formatToken(attr, token);
        return token;
    }
	public GuardedString formatToken(OracleUserAttribute attr, GuardedString token) {
		if(token == null){
			return null;
		}
		final GuardedString[] holder = new GuardedString[1];
		final CSAttributeFormatterAndNormalizer formatter = getAttributeFormatterAndNormalizer(attr);
		token.access(new GuardedString.Accessor(){
			public void access(char[] clearChars) {
				holder[0] = new GuardedString(formatter.formatToken(clearChars));
			}
		});
		return holder[0];
	}
	public GuardedString normalizeAndFormatToken(OracleUserAttribute attr, GuardedString token) {
		token = normalizeToken(attr, token);
		token = formatToken(attr, token);
		return token;
	}
	public GuardedString normalizeToken(OracleUserAttribute attr, GuardedString token) {
		if(token == null){
			return null;
		}
		final GuardedString[] holder = new GuardedString[1];
		final CSAttributeFormatterAndNormalizer normalizer = getAttributeFormatterAndNormalizer(attr);
		token.access(new GuardedString.Accessor(){
			public void access(char[] clearChars) {
				holder[0] = new GuardedString(normalizer.normalizeToken(clearChars));
			}
		});
		return holder[0];
	}
	public char[] formatToken(OracleUserAttribute attr, char[] token) {
        return getAttributeFormatterAndNormalizer(attr).formatToken(token);
	}
	public char[] normalizeAndFormatToken(OracleUserAttribute attr,char[] token) {
        token = normalizeToken(attr, token);
        token = formatToken(attr, token);
        return token;
	}
	public char[] normalizeToken(OracleUserAttribute attr, char[] token) {
		return getAttributeFormatterAndNormalizer(attr).normalizeToken(token);
	}

	public String formatSQLColumn(OracleUserAttribute attr, String sqlColumn) {
		return getAttributeFormatterAndNormalizer(attr).formatSQLColumn(sqlColumn);
	}
    
}



 
