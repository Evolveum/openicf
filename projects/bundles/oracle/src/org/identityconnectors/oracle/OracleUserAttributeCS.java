package org.identityconnectors.oracle;

/** Case sensitivity settings of user tokens */
enum OracleUserAttributeCS {
    USER_NAME,
    PASSWORD,
    SCHEMA,
    ROLE,
    PRIVILEGE(""),
    PROFILE,
    DEF_TABLESPACE,
    TEMP_TABLESPACE,
    GLOBAL_NAME(false,"'");
    
    OracleUserAttributeCS(){}
    OracleUserAttributeCS(String defQuatesChar){
        this.defQuatesChar = defQuatesChar;
    }
    OracleUserAttributeCS(boolean defToUpper,String defQuatesChar){
        this.defToUpper = defToUpper;
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
