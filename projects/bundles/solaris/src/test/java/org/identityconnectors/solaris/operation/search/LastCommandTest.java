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

package org.identityconnectors.solaris.operation.search;

import junit.framework.Assert;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.test.SolarisTestBase;
import org.junit.Test;

public class LastCommandTest extends SolarisTestBase {
    @Test
    public void test() {
        Attribute result = LastCommand.getLastAttributeFor("root", getConnection());
        Assert.assertNotNull(result);
        Assert.assertEquals(NativeAttribute.LAST_LOGIN.getName(), result.getName());
    }
    
    @Override
    public boolean createGroup() {
        return false;
    }

    @Override
    public int getCreateUsersNumber() {
        return 0;
    }
}
