package org.identityconnectors.oracle;

/** Case sensitivity settings of user attributes */
enum OracleUserAttributeCS {
    USER(false),
    PASSWORD(false),
    SCHEMA,
    ROLE,
    PRIVILEGE(""),
    PROFILE,
    DEF_TABLESPACE,
    TEMP_TABLESPACE,
    GLOBAL_NAME(false,"'"),
    SYSTEM_USER(false,""), 
    SYSTEM_PASSWORD(false);  
    
    OracleUserAttributeCS(){}
    OracleUserAttributeCS(String defQuatesChar){
        this.defQuatesChar = defQuatesChar;
    }
    OracleUserAttributeCS(boolean defToUpper){
        this.defToUpper = defToUpper;
    }
    OracleUserAttributeCS(boolean defToUpper,String defQuatesChar){
        this.defToUpper = defToUpper;
        this.defQuatesChar = defQuatesChar;
    }
    private boolean defToUpper = true; //by default set token to upper
    private String defQuatesChar = "\""; //by default surround token with quotes
    
    boolean isDefToUpper() {
        return defToUpper;
    }
    String getDefQuatesChar() {
        return defQuatesChar;
    }
}
