/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for OracleConnector except tests for concrete SPI operation
 * @author kitko
 *
 */
public class OracleConnectorImplTest extends OracleConnectorAbstractTest{
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#checkAlive()}.
     */
    @Test
    public void testCheckAlive() {
        OracleConnector oc = createTestConnector();
        oc.checkAlive();
        oc.dispose();
        
        OracleConnector con = new OracleConnector();
        try{
	        con.checkAlive();
	        fail("Must fail for not initialized");
        }
        catch(RuntimeException e){}
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#getConfiguration()}.
     */
    @Test
    public void testGetConfiguration() {
        OracleConnector oc = createTestConnector();
        OracleConfiguration cfg2 = oc.getConfiguration();
        assertSame(testConf,cfg2);
        oc.dispose();
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    @Test
    public void testInit() {
        OracleConnector oc = createTestConnector();
        oc.dispose();
        
        oc = new OracleConnector();
        OracleConfiguration cfg = new OracleConfiguration();
        try{
            oc.init(cfg);
            fail("Init should fail for uncomplete cfg");
        }
        catch(RuntimeException e){
        }
    }
    
    @Test
    public void testTest(){
    	OracleConnector c = new OracleConnector();
    	try{
    		c.test();
    		fail("Test must fail if init was not called");
    	}
    	catch(RuntimeException e){
    	}
    	c.init(testConf);
    	c.test();
    	c.dispose();
    }
    
    /** Test that connection is kept open when using driver */
    @Test
    public void testConnectionDirectDriver() throws SQLException{
    	final OracleConnector c = new OracleConnector();
    	//First use driver connection
    	c.init(OracleConfigurationTest.createThinConfiguration());
    	Assert.assertNull("Admin connection should be initialized lazy", c.getAdminConnection());
    	c.test();
    	Assert.assertNotNull("Admin connection should not be null after SPI OP", c.getAdminConnection());
    	//We should be able to use the connection
    	c.getAdminConnection().createStatement().close();
    	//Try to run contract
    	runSimpleContract(c,
    			new Runnable(){
					public void run() {
						Assert.assertNotNull(c.getAdminConnection());
						try {
							Assert.assertFalse(c.getAdminConnection().isClosed());
						} catch (SQLException e) {
							throw ConnectorException.wrap(e);
						}
					}
    			});
    	c.dispose();
    }
    
    /** Test that connection is kept open when using driver */
    @Test
    public void testConnectionDataSource() throws SQLException{
    	final OracleConnector c = new OracleConnector();
    	//First use driver connection
    	c.init(OracleConfigurationTest.createDataSourceConfiguration());
    	Assert.assertNull("Admin connection should be initialized lazy", c.getAdminConnection());
    	c.test();
    	Assert.assertNull("Admin connection should be null after SPI OP", c.getAdminConnection());
    	//Try to run contract
    	runSimpleContract(c,
    			new Runnable(){
					public void run() {
						Assert.assertNull(c.getAdminConnection());
					}
    			});
    	c.dispose();
    	//Here close the thread local connection
    	OracleConfigurationTest.tearDownClass();
    }
    
    
    
    
    private void runSimpleContract(OracleConnector connector, Runnable after) throws SQLException{
    	//First create the user
    	Uid uid = new Uid("testUser");
    	try{
    		connector.delete(ObjectClass.ACCOUNT, uid,null);
    	}
    	catch(UnknownUidException e){}
    	uid = connector.create(ObjectClass.ACCOUNT, CollectionUtil.<Attribute>newSet(new Name(uid.getUidValue())), null);
    	after.run();
    	//Update the user
    	connector.update(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.buildPassword("newPassword".toCharArray())), null);
    	after.run();
    	//Add privilege to authenticate
    	connector.addAttributeValues(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"create session")), null);
    	after.run();
    	//authenticate
    	connector.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), new GuardedString("newPassword".toCharArray()), null);
    	after.run();
    	//Remove privilege
    	connector.removeAttributeValues(ObjectClass.ACCOUNT, uid, CollectionUtil.newSet(AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"create session")), null);
    	after.run();
    	//Delete the user
    	connector.delete(ObjectClass.ACCOUNT, uid,null);
    	after.run();
    	//Create schema
    	Assert.assertNotNull(connector.schema());
    	after.run();

    }
    

}
