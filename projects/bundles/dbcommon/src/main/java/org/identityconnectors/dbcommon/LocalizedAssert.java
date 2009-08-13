package org.identityconnectors.dbcommon;

import org.identityconnectors.framework.common.objects.ConnectorMessages;

/**
 * Localized asserts is a set of localized asserts utility method that throws localized
 * exception when assert is not true.
 * Argument names passed into assert methods can also be localized 
 * 
 * @author kitko
 *
 */
public class LocalizedAssert {

    private ConnectorMessages cm;
    private boolean localizeArguments;

    /**
     * Creates asserts with messages
     * @param cm
     * @throws IllegalArgumentException if cm param is null
     */
    public LocalizedAssert(ConnectorMessages cm){
        if(cm == null){
            throw new IllegalArgumentException("ConnectorMessages argument is null");
        }
        this.cm = cm;
    }
    
    /**
     * Creates asserts with messages with flag whether to localize argument names
     * @param cm
     * @param localizeArguments the arg
     * @throws IllegalArgumentException if cm param is null
     */
    public LocalizedAssert(ConnectorMessages cm, boolean localizeArguments){
        if(cm == null){
            throw new IllegalArgumentException("ConnectorMessages argument is null");
        }
        this.cm = cm;
        this.localizeArguments = localizeArguments;
    }
    
    
    private void throwException(String locKey,String argument){
    	if(localizeArguments){
    		argument = cm.format(argument, argument);
    	}
        String msg = cm.format(locKey, null, argument);
        throw new IllegalArgumentException(msg);
    }
    
    /**
     * Asserts the argument is not null. If argument is null, throws localized
     * IllegalArgumentException
     * @param <T>
     * @param o
     * @param argument
     * @return o param
     */
    public <T> T assertNotNull(T o, String argument) {
        if (o == null) {
            throwException(DBMessages.ASSERT_NOT_NULL,argument);
        }
        return o;
    }
    
    /**
     * Asserts the argument is null. If argument is not null, throws localized
     * IllegalArgumentException
     * @param <T>
     * @param o
     * @param argument
     * @return o
     */
    public <T> T assertNull(T o,String argument){
        if(o != null){
            throwException(DBMessages.ASSERT_NULL,argument);
        }
        return o;
    }
    
    /**
     * Asserts that argument string is not blank. If argument is null or blank, throws localized
     * IllegalArgumentException
     * @param o
     * @param argument
     * @return same string
     */
    public String assertNotBlank(String o, String argument) {
        if (o == null || o.length() == 0) {
            throwException(DBMessages.ASSERT_NOT_BLANK,argument);
        }
        return o;
    }
    
    /**
     * Asserts that argument string is blank. If argument is not blank, throws localized
     * IllegalArgumentException
     * @param s
     * @param argument
     * @return same string
     */
    public String assertBlank(String s,String argument){
        if(s != null && s.length() > 0){
            throwException(DBMessages.ASSERT_BLANK,argument);
        }
        return s;
    }
    
    
    
}
