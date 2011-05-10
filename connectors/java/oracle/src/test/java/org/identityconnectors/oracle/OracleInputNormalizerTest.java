/**
 * 
 */
package org.identityconnectors.oracle;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.UpdateOp;


/**
 * Tests for OracleAttributeNormalizer
 * @author kitko
 *
 */
public class OracleInputNormalizerTest {

	@Test
	public void testNormalizeAttribute(){
		OracleInputNormalizer normalizer = new OracleInputNormalizer(OracleConfigurationTest.createSystemConfiguration().getCSSetup());
		ObjectClass objectClass = ObjectClass.ACCOUNT;
		
		Class<? extends SPIOperation> op = UpdateOp.class;
		
		AssertJUnit.assertNull(normalizer.normalizeAttribute(objectClass, op, null));
		
		Attribute attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build("dummy","dummyValue"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("dummyValue", AttributeUtil.getSingleValue(attr));
		
		//User is by default case sensitive/insensitive, depends on OracleUserAttributeCS
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Name.NAME,"myName"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals(OracleUserAttribute.USER.getFormatting().isToUpper() ?  "myName".toUpperCase() : "myName", AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Name.NAME.toLowerCase(),"myName"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals(OracleUserAttribute.USER.getFormatting().isToUpper() ?  "myName".toUpperCase() : "myName", AttributeUtil.getSingleValue(attr));
		
		//We do not normalize by UID
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Uid.NAME,"myUid"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myUid", AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Uid.NAME.toLowerCase(),"myUid"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myUid", AttributeUtil.getSingleValue(attr));
		
		// By default we do not uppercase globalname
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"myGlobalName"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myGlobalName", AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME,"myProfile"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myProfile".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME.toLowerCase(),"myProfile"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myProfile".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass,  op, AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,"myDefTs"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myDefTs".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME.toLowerCase(),"myDefTs"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myDefTs".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,"myTempTs"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myTempTs".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME.toLowerCase(),"myTempTs"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myTempTs".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"myPriv"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myPriv".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME.toLowerCase(),"myPriv"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myPriv".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"myRole"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME.toLowerCase(),"myRole"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME.toUpperCase(),"myRole"));
		AssertJUnit.assertNotNull(attr);
		AssertJUnit.assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
	}
}
