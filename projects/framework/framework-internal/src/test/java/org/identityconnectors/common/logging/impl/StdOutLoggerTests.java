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
package org.identityconnectors.common.logging.impl;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.StringPrintWriter;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.logging.LogSpi;
import org.identityconnectors.common.logging.impl.StdOutLogger;
import org.junit.Assert;
import org.junit.Test;



/**
 * Simple tests of the STDOUT logger.
 */
public class StdOutLoggerTests {
    
    @Test
    public void checkIsLogger() {
        LogSpi logSpi = new StdOutLogger();
        for (Log.Level level : Log.Level.values()) {
            Assert.assertTrue(logSpi.isLoggable(String.class, level));
        }
    }
    
    @Test
    public void checkLogFormat() throws Exception {
        String MSG_EXP = "Expected message from logger attempt";
        PrintStream tmp = System.out;
        Exception EXCEPTION_EXP = new Exception();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pstrm = new PrintStream(bout, true, "UTF-8");
        try {
            // replace print stream..
            System.setOut(pstrm);
            // write something to the log..
            LogSpi logSpi = new StdOutLogger();
            logSpi.log(String.class, "checkLogFormtat", Log.Level.OK, MSG_EXP, EXCEPTION_EXP);
            pstrm.flush();
        }
        finally {
            // no matter what put it back..
            System.setOut(tmp);
        }
        // okay check the results..
        String logRecord = new String(bout.toByteArray(), "UTF-8");
        BufferedReader rdr = new BufferedReader(new StringReader(logRecord));
        String records[] = rdr.readLine().split("\t");
        Map<String, String> map = new HashMap<String, String>(); 
        for (String record : records) {
            String frag[] = record.split(":");
            map.put(frag[0].trim(), frag[1].trim());
        }
        assertEquals(MSG_EXP, map.get("Message"));
        assertEquals(String.class.getName(), map.get("Class"));
        assertEquals(Log.Level.OK.toString(), map.get("Level"));
        assertEquals(Long.toString(Thread.currentThread().getId()), map.get("Thread Id"));
        // read construct the rest for the exception..
        StringPrintWriter actual = new StringPrintWriter();
        for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
            actual.println(line);
        }
        StringPrintWriter expected = new StringPrintWriter();
        EXCEPTION_EXP.printStackTrace(expected);
        assertEquals(expected.getString(), actual.getString());
    }

}
