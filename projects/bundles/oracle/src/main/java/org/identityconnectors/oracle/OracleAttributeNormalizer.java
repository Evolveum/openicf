/**
 * 
 */
package org.identityconnectors.oracle;

import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.operations.SPIOperation;

/**
 * Normalizer of oracle attributes.
 * @author kitko
 *
 */
interface OracleAttributeNormalizer  extends AttributeNormalizer{
    
	Set<Attribute> normalizeAttributes(ObjectClass objectClass,Class<? extends SPIOperation> op, Set<Attribute> attrs);
	
	Pair<String,GuardedString> normalizeAuthenticateEntry(String username, GuardedString password);


}
