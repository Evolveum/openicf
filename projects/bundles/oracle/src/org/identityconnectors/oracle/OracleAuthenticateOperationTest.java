/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OracleAuthenticateOperationTest extends OracleConnectorAbstractTest {
    
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
        try{
            facade.authenticate(ObjectClass.GROUP, user, new GuardedString(password.toCharArray()), null);
            fail("Authenticate must fail for invalid object class");
        }
        catch(IllegalArgumentException e){
        }
        
    }
    
    

}
