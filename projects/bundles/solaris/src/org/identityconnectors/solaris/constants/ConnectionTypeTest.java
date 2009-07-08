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
package org.identityconnectors.solaris.constants;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionTypeTest {
    @Test
    public void testGoodConversion() {
        Assert.assertTrue(ConnectionType.toConnectionType("telnet").equals(ConnectionType.TELNET));
        Assert.assertTrue(ConnectionType.toConnectionType("TELNET").equals(ConnectionType.TELNET));
        Assert.assertTrue(ConnectionType.toConnectionType("TeLnet").equals(ConnectionType.TELNET));
        Assert.assertTrue(ConnectionType.toConnectionType("sSh").equals(ConnectionType.SSH));
        Assert.assertTrue(ConnectionType.toConnectionType("SSH").equals(ConnectionType.SSH));
        Assert.assertTrue(ConnectionType.toConnectionType("ssh").equals(ConnectionType.SSH));
    }
    
    @Test
    public void testWrongConfiguration() {
        try {
            final String test = "bassh";
            ConnectionType.toConnectionType(test);
            Assert.fail(String.format("Error, accepted: %s", test));
        } catch (RuntimeException ex) {
            //ok
        }
        try {
            final String test = "telnetSSh";
            ConnectionType.toConnectionType(test);
            Assert.fail(String.format("Error, accepted: %s", test));
        } catch (RuntimeException ex) {
            //ok
        }
    }
}
