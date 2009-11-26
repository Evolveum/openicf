package org.identityconnectors.oracle;

/**
 * Policy applied when extra attributes are passed to SPI operation.
 * By default we would fail, but some application cannot avoid sending extra attributes in some operation and we must just agree such attributes 
 * @author kitko
 *
 */
enum ExtraAttributesPolicy {
	IGNORE,
	FAIL;
}
