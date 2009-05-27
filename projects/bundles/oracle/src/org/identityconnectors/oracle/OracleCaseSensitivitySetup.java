package org.identityconnectors.oracle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import static org.identityconnectors.oracle.OracleMessages.*;

/** OracleCaseSensitivity is responsible for normalizing and formatting oracle objects tokens (users,schema...).
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
    public String normalizeToken(OracleUserAttributeCS attr,String token);
    
    /**
     * Normalizes array token (e.g make password upper case )
     * @param attr
     * @param token
     * @return normalized token
     */
    public char[] normalizeToken(OracleUserAttributeCS attr,char[] token);
    /**
     * Normalizes GuardedString token (e.g make password upper case )
     * @param attr
     * @param token
     * @return normalized token
     */
    public GuardedString normalizeToken(OracleUserAttributeCS attr,GuardedString token);
    /**
     * Just format token, e.g append quotes
     * @param attr
     * @param token
     * @return
     */
    public String formatToken(OracleUserAttributeCS attr,String token);
    /**
     * Formats char array token , useful for password to not convert to String
     * @param attr
     * @param token
     * @return formatted password
     */
    public char[] formatToken(OracleUserAttributeCS attr,char[] token);
    /**
     * Formats GuardedString , useful for password to not convert to String
     * @param attr
     * @param token
     * @return formatted password
     */
    public GuardedString formatToken(OracleUserAttributeCS attr,GuardedString token);
    
    
    /**
     * Formats SQL column, eq surround with UPPER sql function
     * @param attr
     * @param sqlColumn
     * @return formatted column
     */
    public String formatSQLColumn(OracleUserAttributeCS attr, String sqlColumn);
    
    
    /**
     * Compound operation that normalizes and then formats string token
     * @param attr
     * @param token
     * @return normalized and formatted token
     */
    public String normalizeAndFormatToken(OracleUserAttributeCS attr,String token);
    /**
     * Compound operation that normalizes and then formats char[] token
     * @param attr
     * @param token
     * @return normalized and formatted token
     */
    public char[] normalizeAndFormatToken(OracleUserAttributeCS attr,char[] token);
    /**
     * Compound operation that normalizes and then formats GuardedString token
     * @param attr
     * @param token
     * @return normalized and formatted token
     */
    public GuardedString normalizeAndFormatToken(OracleUserAttributeCS attr,GuardedString token);
    /**
     * Gets normalizer by attribue
     * @param attribute
     * @throws  IllegalArgumentException when normalizer by attribute is not found
     * @return CSTokenFormatter by attribute
     */
    public CSAttributeFormatterAndNormalizer getAttributeFormatterAndNormalizer(OracleUserAttributeCS attribute) throws IllegalArgumentException;
    
}

/** Normalizes and formats token(user attribute like tablespace, profile) for user . 
 *  Normalizing is done using {@link AttributeNormalizer} , while formatting is done when building SQL statement.
 * @author kitko
 *
 */
final class CSAttributeFormatterAndNormalizer{
    private final OracleUserAttributeCS attribute;
    private final boolean toUpper;
    private final String quatesChar;

    OracleUserAttributeCS getAttribute() {
        return attribute;
    }
    boolean isToUpper() {
        return toUpper;
    }
    
    String getQuatesChar() {
		return quatesChar;
	}
    
	String normalizeToken(String token){
        if(token == null || token.length() == 0){
            return token;
        }
        return toUpper ? token.toUpperCase() : token;
    }

    char[] normalizeToken(char[] token){
        if(token == null || token.length == 0){
            return token;
        }
        char[] newToken = new char[token.length];
        for(int i = 0;i < token.length;i++){
        	newToken[i] = toUpper ? Character.toUpperCase(token[i]) : token[i];
        }
        return newToken;
    }
    
    char[] formatToken(char[] token){
        if(token == null){
            return token;
        }
        StringBuilder builder = new StringBuilder(token.length + 2);
        builder.append(quatesChar);
        builder.append(token);
        builder.append(quatesChar);
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
    	if(toUpper){
    		return sqlColumn;
    	}
    	return sqlColumn;
    }
    
    
    private CSAttributeFormatterAndNormalizer(Builder builder){
    	this.attribute = builder.attribute;
    	this.toUpper = builder.toUpper != null ? builder.toUpper : attribute.isDefToUpper();
    	this.quatesChar = builder.quatesChar != null ? builder.quatesChar : attribute.getDefQuatesChar();
    }
    
    final static class Builder{
        private OracleUserAttributeCS attribute;
        private Boolean toUpper;
        private String quatesChar ;
        private final ConnectorMessages cm;
        
        Builder(ConnectorMessages cm){
        	this.cm = cm;
        }
        
        Builder setAttribute(OracleUserAttributeCS attribute){
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
        CSAttributeFormatterAndNormalizer buildWithDefaultValues(OracleUserAttributeCS attribute){
            this.attribute = attribute;
            this.toUpper = attribute.isDefToUpper();
            this.quatesChar = attribute.getDefQuatesChar();
            return build();
        }
    }
}


/** Builder of OracleCaseSensitivity using formatters for user attributes */
final class OracleCaseSensitivityBuilder{
    private final Map<OracleUserAttributeCS,CSAttributeFormatterAndNormalizer> normalizers = new HashMap<OracleUserAttributeCS, CSAttributeFormatterAndNormalizer>(6);
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
    
    
    @SuppressWarnings("unchecked")
    OracleCaseSensitivityBuilder parseMap(String format){
        //If we set string default, all formatters will have its default value
        if("default".equalsIgnoreCase(format)){
            return this;
        }
        final Map<String, Object> map = MapParser.parseMap(format,cm);
        for(Iterator<Map.Entry<String, Object>> i = map.entrySet().iterator();i.hasNext();){
        	Entry<String, Object> entry = i.next();
        	i.remove();
        	String attributeName = entry.getKey();
        	Map<String, Object> elementMap = (Map<String, Object>) entry.getValue();
            if("ALL".equalsIgnoreCase(attributeName)){
                for(OracleUserAttributeCS attribute : OracleUserAttributeCS.values()){
                	if(!this.normalizers.containsKey(attribute)){
                		this.normalizers.put(attribute, new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(attribute).setValues(elementMap).build());
                	}
                }
                continue;
            }
            OracleUserAttributeCS attribute = OracleUserAttributeCS.valueOf(attributeName);
            CSAttributeFormatterAndNormalizer element = new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(attribute).setValues(elementMap).build(); 
            this.normalizers.put(element.getAttribute(),element);    
        }
        if(!map.isEmpty()){
            throw new IllegalArgumentException(cm.format(MSG_ELEMENTS_FOR_CSBUILDER_NOT_RECOGNIZED, null, map));
        }
        return this;
    }
    
    OracleCaseSensitivitySetup build(){
        Map<OracleUserAttributeCS,CSAttributeFormatterAndNormalizer> normalizers = new HashMap<OracleUserAttributeCS, CSAttributeFormatterAndNormalizer>(this.normalizers);
        //If any elements is not defined in specified map set default value
        for(OracleUserAttributeCS attribute : OracleUserAttributeCS.values()){
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
 * formatters and normalizers for each {@link OracleUserAttributeCS}
 * @author kitko
 *
 */
final class OracleCaseSensitivityImpl implements OracleCaseSensitivitySetup{
    private final Map<OracleUserAttributeCS,CSAttributeFormatterAndNormalizer> normalizers;    
    
    OracleCaseSensitivityImpl(Map<OracleUserAttributeCS,CSAttributeFormatterAndNormalizer> normalizers){
        this.normalizers = new HashMap<OracleUserAttributeCS, CSAttributeFormatterAndNormalizer>(normalizers);
    }
    
    public CSAttributeFormatterAndNormalizer getAttributeFormatterAndNormalizer(OracleUserAttributeCS attribute){
        final CSAttributeFormatterAndNormalizer normalizer = normalizers.get(attribute);
        if(normalizer == null){
            throw new IllegalArgumentException("No normalizer defined for attribute " + attribute);
        }
        return normalizer;
    }
    
    public String formatToken(OracleUserAttributeCS attr, String token) {
        return getAttributeFormatterAndNormalizer(attr).formatToken(token);
    }
    public String normalizeToken(OracleUserAttributeCS attr, String token) {
        return getAttributeFormatterAndNormalizer(attr).normalizeToken(token);
    }
    public String normalizeAndFormatToken(OracleUserAttributeCS attr, String token) {
        token = normalizeToken(attr, token);
        token = formatToken(attr, token);
        return token;
    }
	public GuardedString formatToken(OracleUserAttributeCS attr, GuardedString token) {
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
	public GuardedString normalizeAndFormatToken(OracleUserAttributeCS attr, GuardedString token) {
		token = normalizeToken(attr, token);
		token = formatToken(attr, token);
		return token;
	}
	public GuardedString normalizeToken(OracleUserAttributeCS attr, GuardedString token) {
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
	public char[] formatToken(OracleUserAttributeCS attr, char[] token) {
        return getAttributeFormatterAndNormalizer(attr).formatToken(token);
	}
	public char[] normalizeAndFormatToken(OracleUserAttributeCS attr,char[] token) {
        token = normalizeToken(attr, token);
        token = formatToken(attr, token);
        return token;
	}
	public char[] normalizeToken(OracleUserAttributeCS attr, char[] token) {
		return getAttributeFormatterAndNormalizer(attr).normalizeToken(token);
	}

	public String formatSQLColumn(OracleUserAttributeCS attr, String sqlColumn) {
		return getAttributeFormatterAndNormalizer(attr).formatSQLColumn(sqlColumn);
	}
    
}



 
