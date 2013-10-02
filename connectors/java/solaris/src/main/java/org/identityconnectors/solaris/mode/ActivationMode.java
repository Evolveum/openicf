package org.identityconnectors.solaris.mode;

public enum ActivationMode {
	
	NONE("none"),
	
	EXPIRATION("expiration"),
	
	LOCKING("locking");

	private String configString;
	
	private ActivationMode(String configString) {
		this.configString = configString;
	}

	public String getConfigString() {
		return configString;
	}
	
}
