/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
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
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#delete(ObjectClass, Uid, OperationOptions)}
     * @throws SQLException 
     */
    @Test
    public void testDelete() throws SQLException{
        String newUser = "newUser";
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(testConf.getCSSetup().normalizeToken(OracleUserAttribute.USER, newUser)), null);
            try{
            	facade.delete(ObjectClass.ACCOUNT, new Uid(testConf.getCSSetup().normalizeToken(OracleUserAttribute.USER, newUser)), null);
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
            facade.delete(ObjectClass.GROUP, uid, null);
            fail("Delete must fail for invalid object class");
        }
        catch(IllegalArgumentException e){
        }
        
        //try cascade
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION","CREATE VIEW");
        uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password,privileges), null);
        OracleConfiguration cfg = OracleConfigurationTest.createSystemConfiguration();
        cfg.setDropCascade(false);
        Connection conn = cfg.createUserConnection(uid.getUidValue(), AttributeUtil.getGuardedStringValue(password));
        SQLUtil.executeUpdateStatement(conn, "create view mydual as (select * from dual)");
        conn.close();
        ConnectorFacade testFacade = createFacade(cfg);
        try{
        	testFacade.delete(ObjectClass.ACCOUNT, uid, null);
        	fail("Delete must fail withou cascade");
        }catch(RuntimeException e){}
        cfg.setDropCascade(true);
        testFacade = createFacade(cfg);
        testFacade.delete(ObjectClass.ACCOUNT, uid, null);
    }
    
	/** Test that delete will fail for sql error, e.g killed connection 
	 * @throws SQLException */ 
	@Test
	public void testDeleteFail() throws SQLException{
		OracleConnector testConnector = createTestConnector();
		
		
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
        OracleSpecifics.killConnection(connector.getOrCreateAdminConnection(), testConnector.getOrCreateAdminConnection());
        try{
        	testConnector.delete(ObjectClass.ACCOUNT, uid, null);
        	fail("Delete must fail for killed connection");
        }catch(UnknownUidException e){
        	fail("Should not throw UnknownUidException for killed connection");
        }
        catch(RuntimeException e){
        }
        testConnector.dispose();
        connector.delete(ObjectClass.ACCOUNT, uid, null);
        
        //delete group must fail
        try{
        	connector.delete(ObjectClass.GROUP, uid, null);
        	fail("Delete must fail group");
        }catch(UnknownUidException e){
        	fail("Should not throw UnknownUidException for group");
        }
        catch(RuntimeException e){
        }
        
        
        
		
		
	}


}
