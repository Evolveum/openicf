/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hamcrest.core.IsEqual;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
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
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorImpl#checkAlive()}.
     */
    @Test
    public void testCheckAlive() {
        OracleConnectorImpl oc = createTestConnector();
        oc.checkAlive();
        oc.dispose();
        
        OracleConnectorImpl con = new OracleConnectorImpl();
        try{
	        con.checkAlive();
	        fail("Must fail for not initialized");
        }
        catch(RuntimeException e){}
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorImpl#getConfiguration()}.
     */
    @Test
    public void testGetConfiguration() {
        OracleConnectorImpl oc = createTestConnector();
        OracleConfiguration cfg2 = oc.getConfiguration();
        assertSame(testConf,cfg2);
        oc.dispose();
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorImpl#init(org.identityconnectors.framework.spi.Configuration)}.
     */
    @Test
    public void testInit() {
        OracleConnectorImpl oc = createTestConnector();
        oc.dispose();
        
        oc = new OracleConnectorImpl();
        OracleConfiguration cfg = new OracleConfiguration();
        try{
            oc.init(cfg);
            fail("Init should fail for uncomplete cfg");
        }
        catch(RuntimeException e){
        }
    }
    
    @Test
    public void testSchema(){
    	Schema schema = facade.schema();
    	assertNotNull(schema);
    	ObjectClassInfo account = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME);
    	assertNotNull(account);
    	Set<String> attributeNames = new HashSet<String>(OracleConstants.ALL_ATTRIBUTE_NAMES);
    	for(AttributeInfo info : account.getAttributeInfo()){
    		for(Iterator<String> i = attributeNames.iterator();i.hasNext();){
    			if(info.is(i.next())){
    				i.remove();
    			}
    		}
    	}
    	Assert.assertThat("All attributes must be present in schema",attributeNames, new IsEqual<Set<String>>(Collections.<String>emptySet()));
    }
    
    @Test
    public void testTest(){
    	OracleConnectorImpl c = new OracleConnectorImpl();
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
    	final OracleConnectorImpl c = new OracleConnectorImpl();
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
    	final OracleConnectorImpl c = new OracleConnectorImpl();
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
    
    
    
    
    private void runSimpleContract(OracleConnectorImpl connector, Runnable after) throws SQLException{
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
