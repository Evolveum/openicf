package org.identityconnectors.oracle;

import java.sql.Timestamp;

/** User record represents one record from DBA_USERS table */
final class UserRecord {
	
	private final String userName;
	private final String profile;
	private final String defaultTableSpace;
	private final String temporaryTableSpace;
	private final String externalName;
	private final Long userId;
	private final String status;
	private final Timestamp createdDate;
	private final Timestamp lockDate;
	private final Timestamp expireDate;
    //We read this just for testing for external authentication
	private final String password;
	
    private UserRecord(Builder builder) {
		this.createdDate = builder.getCreatedDate();
		this.defaultTableSpace = builder.getDefaultTableSpace();
		this.expireDate = builder.getExpireDate();
		this.externalName = builder.getExternalName();
		this.lockDate = builder.getLockDate();
		this.password = builder.getPassword();
		this.profile = builder.getProfile();
		this.status = builder.getStatus();
		this.temporaryTableSpace = builder.getTemporaryTableSpace();
		this.userId = builder.getUserId();
		this.userName = builder.getUserName();
	}
	
	
    String getUserName() {
		return userName;
	}

	String getProfile() {
		return profile;
	}

	String getDefaultTableSpace() {
		return defaultTableSpace;
	}

	String getTemporaryTableSpace() {
		return temporaryTableSpace;
	}

	String getExternalName() {
		return externalName;
	}

	Long getUserId() {
		return userId;
	}

	String getStatus() {
		return status;
	}

	Timestamp getCreatedDate() {
		return createdDate;
	}

	Timestamp getLockDate() {
		return lockDate;
	}

	Timestamp getExpireDate() {
		return expireDate;
	}

	String getPassword() {
		return password;
	}

	static final class Builder {
        private String userName;
        private String profile;
        private String defaultTableSpace;
        private String temporaryTableSpace;
        private String externalName;
        private Long userId;
        private String status;
        private Timestamp createdDate;
        private Timestamp lockDate;
        private Timestamp expireDate;
        private String password;
        
		String getUserName() {
			return userName;
		}
		Builder setUserName(String userName) {
			this.userName = userName;
			return this;
		}
		String getProfile() {
			return profile;
		}
		Builder setProfile(String profile) {
			this.profile = profile;
			return this;
		}
		String getDefaultTableSpace() {
			return defaultTableSpace;
		}
		Builder setDefaultTableSpace(String defaultTableSpace) {
			this.defaultTableSpace = defaultTableSpace;
			return this;
		}
		String getTemporaryTableSpace() {
			return temporaryTableSpace;
		}
		Builder setTemporaryTableSpace(String temporaryTableSpace) {
			this.temporaryTableSpace = temporaryTableSpace;
			return this;
		}
		String getExternalName() {
			return externalName;
		}
		Builder setExternalName(String externalName) {
			this.externalName = externalName;
			return this;
		}
		Long getUserId() {
			return userId;
		}
		Builder setUserId(Long userId) {
			this.userId = userId;
			return this;
		}
		String getStatus() {
			return status;
		}
		Builder setStatus(String status) {
			this.status = status;
			return this;
		}
		Timestamp getCreatedDate() {
			return createdDate;
		}
		Builder setCreatedDate(Timestamp createdDate) {
			this.createdDate = createdDate;
			return this;
		}
		Timestamp getLockDate() {
			return lockDate;
		}
		Builder setLockDate(Timestamp lockDate) {
			this.lockDate = lockDate;
			return this;
		}
		Timestamp getExpireDate() {
			return expireDate;
		}
		Builder setExpireDate(Timestamp expireDate) {
			this.expireDate = expireDate;
			return this;
		}
		String getPassword() {
			return password;
		}
		Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		UserRecord build(){
			return new UserRecord(this); 
		}
		
        
    }
}
