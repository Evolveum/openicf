package org.identityconnectors.oracle;

import org.identityconnectors.framework.spi.operations.SPIOperation;

interface ExtraAttributesPolicySetup {
	public ExtraAttributesPolicy getPolicy(OracleUserAttribute attribute, Class<? extends SPIOperation> operation);
}
