/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.test.TestHelpers;
import org.junit.*;

/**
 * @author kitko
 *
 */
public class OracleConnectorTest {
    
    private static OracleConfiguration testConf;
    private static ConnectorFacade facade;
    
    private OracleConnector createTestConnector(){
        OracleConnector oc = new OracleConnector();
        oc.init(testConf);
        return oc;
    }
    

    /**
     * Setup for all tests
     */
    @BeforeClass
    public static void setupClass(){
        testConf = OracleConfigurationTest.createSystemConfiguration();
        facade = createFacade(testConf);
    }
    
    private static ConnectorFacade createFacade(OracleConfiguration conf) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration apiCfg = TestHelpers.createTestConfiguration(OracleConnector.class, conf);
        return factory.newInstance(apiCfg);
    }
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#checkAlive()}.
     */
    @Test
    public void testCheckAlive() {
        OracleConnector oc = createTestConnector();
        oc.checkAlive();
        oc.dispose();
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
        catch(IllegalArgumentException e){
        }
        
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#test()}.
     */
    @Test
    public void testTest() {
        OracleConnector oc = createTestConnector();
        oc.test();
    }

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#authenticate(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)}.
     */
    @Test
    public void testAuthenticate() {
        String user = TestHelpers.getProperty("authenticate.user", null);
        String password = TestHelpers.getProperty("authenticate.password", null);
        final Uid uid = facade.authenticate(ObjectClass.ACCOUNT, user, new GuardedString(password.toCharArray()), null);
        assertNotNull(uid);
        
        try{
            facade.authenticate(ObjectClass.ACCOUNT , user, new GuardedString("wrongPassword".toCharArray()), null);
            fail("Authenticate must fail for invalid user/password");
        }
        catch(InvalidCredentialException e){}
        
    }
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#create(ObjectClass, java.util.Set, OperationOptions)}
     */
    @Test
    public void testCreate(){
        String newUser = "newUser";
        try{
            facade.delete(ObjectClass.ACCOUNT, new Uid(newUser), null);
            fail("Delete should fail for unexistent user");
        }
        catch(UnknownUidException e){}
        //Test local authentication
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), null);
        
        
    }
    
    /** 
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#delete(ObjectClass, Uid, OperationOptions)}
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
        Attribute authentication = AttributeBuilder.build(OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConnector.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(newUser);
        Attribute password = AttributeBuilder.buildPassword("password".toCharArray());
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,password), null);
        facade.delete(ObjectClass.ACCOUNT, uid, null);
        try{
            facade.delete(ObjectClass.ACCOUNT, uid, null);
            fail("Delete should fail for unexistent user, previous delete was not succesful");
        }
        catch(UnknownUidException e){}
    }

}
