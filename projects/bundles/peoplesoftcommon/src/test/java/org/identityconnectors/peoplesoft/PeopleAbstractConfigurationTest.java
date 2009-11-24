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

package org.identityconnectors.peoplesoft;

import static org.junit.Assert.fail;

import org.identityconnectors.common.security.*;
import org.identityconnectors.test.common.*;
import org.junit.*;

import psft.pt8.joa.*;

/**
 * @author kitko
 *
 */
public class PeopleAbstractConfigurationTest {
    @Test
    public void testCreateAdminConn(){
        PropertyBag bag = TestHelpers.getProperties(PeopleSoftAbstractConfiguration.class);
        String host = bag.getStringProperty("host");
        String port = bag.getStringProperty("port");
        String user = bag.getStringProperty("user");
        GuardedString password = bag.getProperty("password", GuardedString.class);
        PeopleSoftAbstractConfiguration cfg = new PeoplesoftTestConfiguration();
        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setUser(user);
        cfg.setPassword(password);
        ISession conn = cfg.craeteAdminConnection();
        Assert.assertNotNull("Admin connection cannot be null", conn);
        conn.disconnect();
        cfg.setHost("unknown");
        try{
            conn = cfg.craeteAdminConnection();
            fail("Must fail on invalid host");
        }
        catch(RuntimeException e){
        }
    }
}
