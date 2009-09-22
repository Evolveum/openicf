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
package org.identityconnectors.common.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ScriptTests {

    @Test
    public void testBasic() {
        Script s1 = new Script("Groovy", "print 'foo'");
        assertEquals("Groovy", s1.getScriptLanguage());
        assertEquals("print 'foo'", s1.getScriptText());
        Script s2 = new ScriptBuilder().setScriptLanguage("Groovy").setScriptText("print 'foo'").build();
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testLanguageNotBlank() {
        try {
            new ScriptBuilder().setScriptText("print 'foo'").build();
            fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }

        try {
            new ScriptBuilder().setScriptText("print 'foo'").setScriptLanguage("").build();
            fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }

        try {
            new ScriptBuilder().setScriptText("print 'foo'").setScriptLanguage(" ").build();
            fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }
    }

    @Test
    public void testTextNotNull() {
        try {
            new ScriptBuilder().setScriptLanguage("Groovy").build();
            fail();
        } catch (NullPointerException e) {
            // OK.
        }

        // The text can be empty.
        new ScriptBuilder().setScriptLanguage("Groovy").setScriptText("").build();
    }
}
