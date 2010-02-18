/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.test;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.Assert;
import org.junit.Test;

public class SolarisAuthenticateTest extends SolarisTestBase {
    
    @Test
    public void testPositiveAuth() {
        String username = getUsername(0);
        /*
         * This is an additional workaround for Solaris Trusted extension configuration.
         */
        if (isTrustedExtensions()) {
            String command = "usermod -K min_label=ADMIN_LOW -K clearance=ADMIN_HIGH " + username; 
            getConnection().executeCommand(command);
        }
        
        GuardedString passwd = new GuardedString(SAMPLE_PASSWD.toCharArray());
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, username, passwd, null);
        } catch (Exception ex) {
            Assert.fail("Unexpected exception thrown during authenticate. Exception message:" + ex.getMessage());
        }
    }
   
    @Test
    public void testAuthenticateApiOp() {
        GuardedString password = getConfiguration().getPassword();
        String username = getConfiguration().getLoginUser();
        getFacade().authenticate(ObjectClass.ACCOUNT, username, password, null);
    }
    
    /**
     * test to authenticate with invalid credentials.
     */
    @Test (expected=ConnectorException.class)
    public void testAuthenticateApiOpInvalidCredentials() {
        GuardedString password = new GuardedString(
                "WRONG_PASSWORD_FOOBAR2135465".toCharArray());
        String username = getConfiguration().getLoginUser();
        getFacade().authenticate(ObjectClass.ACCOUNT, username, password, null);
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void unknownObjectClass() {
        GuardedString password = getConfiguration().getPassword();
        String username = getConfiguration().getLoginUser();
        getFacade().authenticate(new ObjectClass("NONEXISTING_OBJECTCLASS"), username, password, null);
    }
    
    @Test (expected=RuntimeException.class)
    public void unknownUid() {
        GuardedString password = getConfiguration().getPassword();
        getFacade().authenticate(ObjectClass.ACCOUNT, "NONEXISTING_UID___", password, null);
    }
    
    /**
     * after unsuccessful authenticate the connection must recover and be in a usable state.
     */
    @Test
    public void testTwiceAuthWithFailure() {
        try {
            getFacade().authenticate(ObjectClass.ACCOUNT, "NONEXISTING_UID__", getConfiguration().getPassword(), null);
        } catch (ConnectorException ex) {
            //OK
        }
        getFacade().authenticate(ObjectClass.ACCOUNT, getConfiguration().getLoginUser(), getConfiguration().getPassword(), null);
    }
    
    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 1;
    }
}
