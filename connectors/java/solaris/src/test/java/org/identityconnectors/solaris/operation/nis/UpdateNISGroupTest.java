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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.operation.nis;

import java.util.List;

import org.identityconnectors.common.CollectionUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class UpdateNISGroupTest {
    @Test
    public void testListFormatter() {
        List<String> users = CollectionUtil.newList("foo", "bar", "baz");
        String separator = ",";
        String result = UpdateNISGroup.listToString(users, separator);
        AssertJUnit.assertEquals("foo,bar,baz", result.trim());

        users = CollectionUtil.newList("foo");
        result = UpdateNISGroup.listToString(users, separator);
        AssertJUnit.assertEquals("foo", result.trim());
    }
}
