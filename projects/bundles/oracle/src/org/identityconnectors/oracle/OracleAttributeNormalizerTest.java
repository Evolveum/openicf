/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Test;


/**
 * Tests for OracleAttributeNormalizer
 * @author kitko
 *
 */
public class OracleAttributeNormalizerTest {

	@Test
	public void testNormalizeAttribute(){
		OracleAttributeNormalizer normalizer = new OracleAttributeNormalizer(OracleConfigurationTest.createSystemConfiguration());
		ObjectClass objectClass = ObjectClass.ACCOUNT;
		
		assertNull(normalizer.normalizeAttribute(objectClass, null));
		
		Attribute attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build("dummy","dummyValue"));
		assertNotNull(attr);
		assertEquals("dummyValue", AttributeUtil.getSingleValue(attr));
		
		//User is by default case sensitive
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(Name.NAME,"myName"));
		assertNotNull(attr);
		assertEquals("myName", AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(Name.NAME.toLowerCase(),"myName"));
		assertNotNull(attr);
		assertEquals("myName", AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(Uid.NAME,"myUid"));
		assertNotNull(attr);
		assertEquals("myUid", AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(Uid.NAME.toLowerCase(),"myUid"));
		assertNotNull(attr);
		assertEquals("myUid", AttributeUtil.getSingleValue(attr));
		
		// By default we do not uppercase globalname
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"myGlobalName"));
		assertNotNull(attr);
		assertEquals("myGlobalName", AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME,"myProfile"));
		assertNotNull(attr);
		assertEquals("myProfile".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME.toLowerCase(),"myProfile"));
		assertNotNull(attr);
		assertEquals("myProfile".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,"myDefTs"));
		assertNotNull(attr);
		assertEquals("myDefTs".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME.toLowerCase(),"myDefTs"));
		assertNotNull(attr);
		assertEquals("myDefTs".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,"myTempTs"));
		assertNotNull(attr);
		assertEquals("myTempTs".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME.toLowerCase(),"myTempTs"));
		assertNotNull(attr);
		assertEquals("myTempTs".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"myPriv"));
		assertNotNull(attr);
		assertEquals("myPriv".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME.toLowerCase(),"myPriv"));
		assertNotNull(attr);
		assertEquals("myPriv".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"myRole"));
		assertNotNull(attr);
		assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME.toLowerCase(),"myRole"));
		assertNotNull(attr);
		assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME.toUpperCase(),"myRole"));
		assertNotNull(attr);
		assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
	}
}
