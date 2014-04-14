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
 * Portions Copyrighted 2014 ForgeRock AS.
 */
package org.identityconnectors.common.logging.impl;

import static org.testng.Assert.assertFalse;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.logging.LogSpi;
import org.testng.annotations.Test;

/**
 * Just make sure the class behavior doesn't change..
 */
public class NoOpLoggerTests {

    @Test
    public void falseLogger() {
        LogSpi logSpi = new NoOpLogger();
        for (Log.Level level : Log.Level.values()) {
            assertFalse(logSpi.isLoggable(String.class, level));
        }
        // hopefully this will throw a NPE if someone changes it..
        logSpi.log(null, (String)null, null, null, null);
    }
}
