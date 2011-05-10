package org.identityconnectors.oracle;

import org.identityconnectors.framework.spi.operations.SPIOperation;

/** Policy settings of user attributes.
 *  Each oracle user attribute can have different policy for formating and for policy that is applied when attribute is not applicable in operation.
 *  */

final class DefFormatting {
    static final boolean DEF_TO_UPPER = true; //by default set token to upper
    static final String DEF_QUATES = "\""; //by default surround token with quotes
    static final Formatting DEF_FORMATTING = new Formatting(DEF_TO_UPPER, DEF_QUATES);
}

enum OracleUserAttribute {
    /** Formating user name */
    USER,
    /** Formatting password */
    PASSWORD(){
		@Override
		Formatting getFormatting() {
			return new Formatting(false, DefFormatting.DEF_QUATES);
		}
    },
    /** Formatting one user role */
    ROLE,
    /** Formatting the privilege */
    PRIVILEGE(){
		@Override
		Formatting getFormatting() {
			return new Formatting(DefFormatting.DEF_TO_UPPER, "");
		}
    },
    /** Formatting name of profile user is associated with */
    PROFILE,
    /** Formatting name of default tablespace where user's objects are created in */
    DEF_TABLESPACE,
    /** Formatting name of temporary tablespace where user's temp objeccts are created in */
    TEMP_TABLESPACE,
    /** Global name when using global authentication */
    GLOBAL_NAME(){
		@Override
		Formatting getFormatting() {
			return new Formatting(false,"'");
		}
    },
    SYSTEM_USER(){
		@Override
		Formatting getFormatting() {
			return new Formatting(false,"");
		}
    },
    SYSTEM_PASSWORD(){
		@Override
		Formatting getFormatting() {
			return new Formatting(false,DefFormatting.DEF_QUATES);
		}
    },
    PASSWORD_EXPIRE;  
    
    

    Formatting getFormatting(){
    	return DefFormatting.DEF_FORMATTING;
    }
    
    ExtraAttributesPolicy getExtraAttributesPolicy(Class<? extends SPIOperation> operation){
    	return ExtraAttributesPolicy.FAIL;
    }
}
