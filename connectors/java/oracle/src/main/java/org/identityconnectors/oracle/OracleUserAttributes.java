package org.identityconnectors.oracle;

import org.identityconnectors.common.security.GuardedString;

/**
 * Helper structure for creating/altering user.
 * It reflects the record from DBA_USERS and holds attribute values from SPI operations.
 * The structure is filled by {@link OracleAttributesReader}
 * 
 * @author kitko
 *
 */
final class OracleUserAttributes {
    private final String userName;
    private final OracleAuthentication auth;
    private final GuardedString password;
    private final String globalName;
    private final Boolean expirePassword;
    private final Boolean enable;
    private final String defaultTableSpace;
    private final String tempTableSpace;
    private final String profile;
    private final String defaultTSQuota;
    private final String tempTSQuota;
    
    private OracleUserAttributes(Builder builder){
    	this.auth = builder.getAuth();
    	this.defaultTableSpace = builder.getDefaultTableSpace();
    	this.defaultTSQuota = builder.getDefaultTSQuota();
    	this.enable = builder.getEnable();
    	this.expirePassword = builder.getExpirePassword();
    	this.globalName = builder.getGlobalName();
    	this.password = builder.getPassword();
    	this.profile = builder.getProfile();
    	this.tempTableSpace = builder.getTempTableSpace();
    	this.tempTSQuota = builder.getTempTSQuota();
    	this.userName = builder.getUserName();
    }

	String getUserName() {
		return userName;
	}
	
	OracleAuthentication getAuth() {
		return auth;
	}

	GuardedString getPassword() {
		return password;
	}

	String getGlobalName() {
		return globalName;
	}

	Boolean getExpirePassword() {
		return expirePassword;
	}

	Boolean getEnable() {
		return enable;
	}

	String getDefaultTableSpace() {
		return defaultTableSpace;
	}

	String getTempTableSpace() {
		return tempTableSpace;
	}

	String getProfile() {
		return profile;
	}

	String getDefaultTSQuota() {
		return defaultTSQuota;
	}

	String getTempTSQuota() {
		return tempTSQuota;
	}


	final static class Builder {
        private String userName;
        private OracleAuthentication auth;
        private GuardedString password;
        private String globalName;
        private Boolean expirePassword;
        private Boolean enable;
        private String defaultTableSpace;
        private String tempTableSpace;
        private String profile;
        private String defaultTSQuota;
        private String tempTSQuota;
        
		String getUserName() {
			return userName;
		}
		Builder setUserName(String userName) {
			this.userName = userName;
			return this;
		}
		OracleAuthentication getAuth() {
			return auth;
		}
		Builder setAuth(OracleAuthentication auth) {
			this.auth = auth;
			return this;
		}
		GuardedString getPassword() {
			return password;
		}
		Builder setPassword(GuardedString password) {
			this.password = password;
			return this;
		}
		String getGlobalName() {
			return globalName;
		}
		Builder setGlobalName(String globalName) {
			this.globalName = globalName;
			return this;
		}
		Boolean getExpirePassword() {
			return expirePassword;
		}
		Builder setExpirePassword(Boolean expirePassword) {
			this.expirePassword = expirePassword;
			return this;
		}
		Boolean getEnable() {
			return enable;
		}
		Builder setEnable(Boolean enable) {
			this.enable = enable;
			return this;
		}
		String getDefaultTableSpace() {
			return defaultTableSpace;
		}
		Builder setDefaultTableSpace(String defaultTableSpace) {
			this.defaultTableSpace = defaultTableSpace;
			return this;
		}
		String getTempTableSpace() {
			return tempTableSpace;
		}
		Builder setTempTableSpace(String tempTableSpace) {
			this.tempTableSpace = tempTableSpace;
			return this;
		}
		String getProfile() {
			return profile;
		}
		Builder setProfile(String profile) {
			this.profile = profile;
			return this;
		}
		String getDefaultTSQuota() {
			return defaultTSQuota;
		}
		Builder setDefaultTSQuota(String defaultTSQuota) {
			this.defaultTSQuota = defaultTSQuota;
			return this;
		}
		String getTempTSQuota() {
			return tempTSQuota;
		}
		Builder setTempTSQuota(String tempTSQuota) {
			this.tempTSQuota = tempTSQuota;
			return this;
		}
    	
		OracleUserAttributes build(){
			return new OracleUserAttributes(this);
		}
		
    }
    
}
