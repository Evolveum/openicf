package org.identityconnectors.db2;

import org.identityconnectors.framework.common.objects.ConnectorMessages;

class Asserts {

    private ConnectorMessages cm;

    Asserts(ConnectorMessages cm){
        if(cm == null){
            throw new IllegalArgumentException("ConnectorMessages argument is null");
        }
        this.cm = cm;
    }
    
    private void throwException(String locKey,String argument){
        String msg = cm.format(locKey, null, argument);
        throw new IllegalArgumentException(msg);
    }
    
    <T> T notNull(T o, String argument) {
        if (o == null) {
            throwException(DB2Messages.NOT_NULL,argument);
        }
        return o;
    }
    
    <T> T null_(T o,String argument){
        if(o != null){
            throwException(DB2Messages.NULL,argument);
        }
        return o;
    }
    
    String notBlank(String o, String argument) {
        if (o == null || o.length() == 0) {
            throwException(DB2Messages.NOT_BLANK,argument);
        }
        return o;
    }
    
    String blank(String s,String argument){
        if(s != null && s.length() > 0){
            throwException(DB2Messages.BLANK,argument);
        }
        return s;
    }
    
    
    
}
