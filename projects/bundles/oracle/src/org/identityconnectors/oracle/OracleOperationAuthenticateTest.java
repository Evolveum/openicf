/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;

/**
 * @author kitko
 *
 */
public class OracleOperationAuthenticateTest extends OracleConnectorAbstractTest {
    
    /**
     * Test method for {@link org.identityconnectors.oracle.OracleConnector#authenticate(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)}.
     */
    @Test
    public void testAuthenticate() {
    	String user = "TESTUSER";
    	GuardedString password = new GuardedString("TEST".toCharArray());
    	try{
    		facade.delete(ObjectClass.ACCOUNT, new Uid(user),null);
    	}catch(UnknownUidException e){}
        Attribute authentication = AttributeBuilder.build(OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_AUTH_LOCAL);
        Attribute name = new Name(user);
        Attribute passwordAttribute = AttributeBuilder.buildPassword(password);
        Attribute privileges = AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"CREATE SESSION");
        Uid uid = facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(authentication,name,passwordAttribute,privileges), null);
    	
        uid = facade.authenticate(ObjectClass.ACCOUNT, user, password, null);
        assertNotNull(uid);
        
        //Invalid password
        try{
            facade.authenticate(ObjectClass.ACCOUNT , user, new GuardedString("wrongPassword".toCharArray()), null);
            fail("Authenticate must fail for invalid user/password");
        }
        catch(PasswordExpiredException e){
        	fail("Invalid password cannot throw PasswordExpiredException");
        }
        catch(InvalidCredentialException e){
        }
        
        //Invalid object class
        try{
            facade.authenticate(ObjectClass.GROUP, user, password, null);
            fail("Authenticate must fail for invalid object class");
        }
        catch(IllegalArgumentException e){
        }
        
        //Null user name
        try{
            facade.authenticate(ObjectClass.ACCOUNT, null, password, null);
            fail("Must fail for null user");
        }
        catch(Exception e){}
        
        //Expired password must throw PasswordExpiredException
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildPasswordExpired(true)), null);
        try{
        	uid = facade.authenticate(ObjectClass.ACCOUNT, user, password, null);
        	fail("Should not Authenticate with expired password");
        }catch(PasswordExpiredException e){
        }
        catch(RuntimeException e){
        	fail("Authenticate with expired accoun must throw PasswordExpiredException only :" + e);
        }
        
        //Lock account
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildPassword(password)), null);
        facade.authenticate(ObjectClass.ACCOUNT, user, password, null);
        facade.update(ObjectClass.ACCOUNT, uid, Collections.singleton(AttributeBuilder.buildEnabled(false)), null);
        try{
        	facade.authenticate(ObjectClass.ACCOUNT, user, password, null);
        }
        catch(PasswordExpiredException e){
        	fail("Must not throw PasswordExpiredException on locked account");
        }
        catch(ConnectorSecurityException e){
        	Assert.assertThat(e.getMessage(), JUnitMatchers.containsString("account is locked"));
        }
        
        facade.delete(ObjectClass.ACCOUNT, new Uid(user),null);
        
        

        
    }
    
    

}
