/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hamcrest.core.IsEqual;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for OracleConnector except tests for concrete SPI operation
 * @author kitko
 *
 */
public class OracleConnectorTest extends OracleConnectorAbstractTest{
    
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
    

}
