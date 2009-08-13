package org.identityconnectors.oracle;

class Formatting {
    private final boolean toUpper;
    private final String quatesChar;
    
	Formatting(boolean toUpper,String quatesChar) {
		super();
		this.quatesChar = quatesChar;
		this.toUpper = toUpper;
	}

	/**
	 * @return the toUpper
	 */
	boolean isToUpper() {
		return toUpper;
	}

	/**
	 * @return the quatesChar
	 */
	String getQuatesChar() {
		return quatesChar;
	}
	
	
    
}
