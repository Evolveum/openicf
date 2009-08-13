/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class ExtraAttributesPolicySetupBuilderTest {

	/**
	 * Test method for {@link org.identityconnectors.oracle.ExtraAttributesPolicySetupBuilder#parseArray(java.lang.String[])}.
	 */
	@Test
	public void testParseArray() {
		ExtraAttributesPolicySetupBuilder builder = new ExtraAttributesPolicySetupBuilder(TestHelpers.createDummyMessages());
		builder.parseArray(null);
		String[] array = new String[] {"PASSWORD={create=IGNORE,update=FAIL}","PASSWORD_EXPIRE={update=IGNORE}"};
		ExtraAttributesPolicySetup setup = builder.parseArray(array).build();
		
		assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(OracleUserAttribute.PASSWORD, CreateOp.class));
		assertEquals(ExtraAttributesPolicy.FAIL, setup.getPolicy(OracleUserAttribute.PASSWORD, UpdateOp.class));
		assertEquals(ExtraAttributesPolicy.FAIL, setup.getPolicy(OracleUserAttribute.PASSWORD_EXPIRE, CreateOp.class));
		assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(OracleUserAttribute.PASSWORD_EXPIRE, UpdateOp.class));
		
		builder.clearPolicies();
		setup = builder.parseArray(new String[]{"ALL={create=IGNORE,update=IGNORE}","USER={update=FAIL}"}).build();
		for(OracleUserAttribute oua : OracleUserAttribute.values()){
			for(Class<? extends SPIOperation> op : FrameworkUtil.allSPIOperations()){
				if(OracleUserAttribute.USER.equals(oua) && UpdateOp.class.equals(op)) {
					Assert.assertEquals(ExtraAttributesPolicy.FAIL, setup.getPolicy(oua, op));
					continue;
				}
				if(CreateOp.class.equals(op) || UpdateOp.class.equals(op)){
					Assert.assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(oua, op));
					continue;
				}
				Assert.assertEquals(oua.getExtraAttributesPolicy(op), setup.getPolicy(oua, op));
			}
		}
		
		try{
			builder.parseArray(new String[]{"dummy"});
			fail("Must fail for invalid array");
		}
		catch(RuntimeException e){}
		
		try{
			builder.parseArray(new String[]{"dummy={a=b}"});
			fail("Must fail for invalid array");
		}
		catch(RuntimeException e){}
		
		try{
			builder.parseArray(new String[]{"PASSWORD={a=b}"});
			fail("Must fail for invalid array");
		}
		catch(RuntimeException e){}

		try{
			builder.parseArray(new String[]{"PASSWORD={invalid=FAIL}"});
			fail("Must fail for invalid array");
		}
		catch(RuntimeException e){}
		
		try{
			builder.parseArray(new String[]{"PASSWORD={create=invalid}"});
			fail("Must fail for invalid array");
		}
		catch(RuntimeException e){}
		

		
	}

	/**
	 * Test method for {@link org.identityconnectors.oracle.ExtraAttributesPolicySetupBuilder#parseMap(java.lang.String)}.
	 */
	@Test
	public void testParseMap() {
		ExtraAttributesPolicySetupBuilder builder = new ExtraAttributesPolicySetupBuilder(TestHelpers.createDummyMessages());
		builder.parseMap(null);
		builder.parseMap("");
		String map = "PASSWORD={create=IGNORE,update=FAIL},PASSWORD_EXPIRE={update=IGNORE}";
		ExtraAttributesPolicySetup setup = builder.parseMap(map).build();
		
		assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(OracleUserAttribute.PASSWORD, CreateOp.class));
		assertEquals(ExtraAttributesPolicy.FAIL, setup.getPolicy(OracleUserAttribute.PASSWORD, UpdateOp.class));
		assertEquals(ExtraAttributesPolicy.FAIL, setup.getPolicy(OracleUserAttribute.PASSWORD_EXPIRE, CreateOp.class));
		assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(OracleUserAttribute.PASSWORD_EXPIRE, UpdateOp.class));
		
		builder.clearPolicies();
		setup = builder.parseMap("default").build();
		//All must have default values
		for(OracleUserAttribute oua : OracleUserAttribute.values()){
			for(Class<? extends SPIOperation> op : FrameworkUtil.allSPIOperations()){
				Assert.assertEquals(oua.getExtraAttributesPolicy(op), setup.getPolicy(oua, op));
			}
		}
		
		builder.clearPolicies();
		setup = builder.parseMap("ALL={create=IGNORE,update=IGNORE},USER={update=FAIL}").build();
		for(OracleUserAttribute oua : OracleUserAttribute.values()){
			for(Class<? extends SPIOperation> op : FrameworkUtil.allSPIOperations()){
				if(OracleUserAttribute.USER.equals(oua) && UpdateOp.class.equals(op)) {
					Assert.assertEquals(ExtraAttributesPolicy.FAIL, setup.getPolicy(oua, op));
					continue;
				}
				if(CreateOp.class.equals(op) || UpdateOp.class.equals(op)){
					Assert.assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(oua, op));
					continue;
				}
				Assert.assertEquals(oua.getExtraAttributesPolicy(op), setup.getPolicy(oua, op));
			}
		}
		
		
		try{
			builder.parseMap("dummy");
			fail("Must fail for invalid map");
		}
		catch(RuntimeException e){}
		
		try{
			builder.parseMap("dummy={a=b}");
			fail("Must fail for invalid map");
		}
		catch(RuntimeException e){}
		
		try{
			builder.parseMap("PASSWORD={a=b}");
			fail("Must fail for invalid map");
		}
		catch(RuntimeException e){}

		try{
			builder.parseMap("PASSWORD={invalid=FAIL}");
			fail("Must fail for invalid map");
		}
		catch(RuntimeException e){}
		
		try{
			builder.parseMap("PASSWORD={create=invalid}");
			fail("Must fail for invalid map");
		}
		catch(RuntimeException e){}
		

	}
	
	@Test
	public void testBuild(){
		//Test that after build all policies will be default from Attribute
		ExtraAttributesPolicySetupBuilder builder = new ExtraAttributesPolicySetupBuilder(TestHelpers.createDummyMessages());
		ExtraAttributesPolicySetup setup = builder.build();
		for(OracleUserAttribute oua : OracleUserAttribute.values()){
			for(Class<? extends SPIOperation> op : FrameworkUtil.allSPIOperations()){
				Assert.assertEquals(oua.getExtraAttributesPolicy(op), setup.getPolicy(oua, op));
			}
		}
	}

	/**
	 * Test method for {@link org.identityconnectors.oracle.ExtraAttributesPolicySetupBuilder#definePolicy(org.identityconnectors.oracle.OracleUserAttribute, java.lang.Class, org.identityconnectors.oracle.ExtraAttributesPolicy)}.
	 */
	@Test
	public void testDefinePolicy() {
		ExtraAttributesPolicySetupBuilder builder = new ExtraAttributesPolicySetupBuilder(TestHelpers.createDummyMessages());
		builder.definePolicy(OracleUserAttribute.PASSWORD, CreateOp.class, ExtraAttributesPolicy.IGNORE);
		builder.definePolicy(OracleUserAttribute.PASSWORD_EXPIRE, CreateOp.class, ExtraAttributesPolicy.IGNORE);
		ExtraAttributesPolicySetup setup = builder.build();
		for(OracleUserAttribute oua : OracleUserAttribute.values()){
			for(Class<? extends SPIOperation> op : FrameworkUtil.allSPIOperations()){
				Assert.assertNotNull("ExtraAttributesPolicy not defined", setup.getPolicy(oua, op));
			}
		}
		assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(OracleUserAttribute.PASSWORD, CreateOp.class));
		assertEquals(ExtraAttributesPolicy.IGNORE, setup.getPolicy(OracleUserAttribute.PASSWORD_EXPIRE, CreateOp.class));
		
	}

}
