package org.identityconnectors.oracle;

import org.identityconnectors.framework.spi.operations.SPIOperation;

/** Policy settings of user attributes */

final class DefFormatting {
    static final boolean DEF_TO_UPPER = true; //by default set token to upper
    static final String DEF_QUATES = "\""; //by default surround token with quotes
    static final Formatting DEF_FORMATTING = new Formatting(DEF_TO_UPPER, DEF_QUATES);
}

enum OracleUserAttribute {
    USER,
    PASSWORD(){
		@Override
		Formatting getFormatting() {
			return new Formatting(false, DefFormatting.DEF_QUATES);
		}
    },
    SCHEMA,
    ROLE,
    PRIVILEGE(){
		@Override
		Formatting getFormatting() {
			return new Formatting(DefFormatting.DEF_TO_UPPER, "");
		}
    },
    PROFILE,
    DEF_TABLESPACE,
    TEMP_TABLESPACE,
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
