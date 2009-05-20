/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OracleOperationDeleteTest extends OracleConnectorAbstractTest {

    /** 
     * Test method for {@link org.identityconnectors.oracle.OracleConnectorImpl#delete(ObjectClass, Uid, OperationOptions)}
     */
    @Test
    public void testDelete(){
        String newUser = "newUser";
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
            try{
                facade.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
                fail("Delete should fail for unexistent user");
            }
            catch(UnknownUidException e){}
        }
        catch(UnknownUidException e){}
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), null);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        try{
            facade.delete(ObjectClass.ACCOUNT, uid, null);
            fail("Delete should fail for unexistent user, previous delete was not successful");
        }
        catch(UnknownUidException e){}
        try{
            facade.delete(ObjectClass.GROUP, new Uid(newUser), null);
            fail("Delete must fail for invalid object class");
        }
        catch(IllegalArgumentException e){
        }
    }
    
	/** Test that delete will fail for sql error, e.g killed connection 
	 * @throws SQLException */ 
	@Test
	public void testDeleteFail() throws SQLException{
		OracleConnectorImpl testConnector = createTestConnector();
		String newUser = "newUser";
        try{
        	testConnector.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
        }
        catch(UnknownUidException e){}
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
        Uid uid = testConnector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), null);
        testConnector.delete(ObjectClass.ACCOUNT, uid, null);
        uid = testConnector.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), null);
        OracleSpecificsTest.killConnection(connector.getAdminConnection(), testConnector.getAdminConnection());
        try{
        	testConnector.delete(ObjectClass.GROUP, uid, null);
        	fail("Delete must fail for killed connection");
        }catch(UnknownUidException e){
        	fail("Should not throw UnknownUidException for killed connection");
        }
        catch(RuntimeException e){
        }
        connector.delete(ObjectClass.ACCOUNT, uid, null);
		testConnector.dispose();
	}


}
