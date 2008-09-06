/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.flatfile;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Attempts to test that the configuration options can validate the input given
 * them. It also attempt to make sure the properties are correct.
 */
public class FlatFileConfigurationTests {

    /**
     * Tests setting and validating the parameters provided.
     */
    @Test
    public void testValidate() throws Exception {
        FlatFileConfiguration config = new FlatFileConfiguration();
        // check defaults..
        assertEquals(null, config.getFile());
        assertEquals(Charset.defaultCharset().name(), config.getEncoding());
        assertEquals('"', config.getTextQualifier());
        assertEquals(',', config.getFieldDelimiter());
        // set a unique attribute so there's not a runtime exception..
        config.setUniqueAttributeName("uid");
        
        // test the file..
        File f = new File("test/test.csv");
        // simple property test..
        config.setFile(f);
        assertEquals(f, config.getFile());
        // try the validate..
        try {
            config.validate();
            Assert.fail();
        } catch (RuntimeException e) {
            // expected because the file doesn't exist..
        }
        // create a temp file
        f = File.createTempFile("connector", "suffix");
        f.deleteOnExit();
        // attempt validate again..
        config.setFile(f);
        assertEquals(f, config.getFile());
        // this should work..
        config.validate();

        // test the encoding property..
        config.setEncoding(null);
        try {
            config.validate();
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // should go here..
        }
        // check encoding..
        config.setEncoding(Charset.forName("UTF-8").name());
        assertEquals(Charset.forName("UTF-8").name(), config.getEncoding());

        // test stupid problem..
        config.setFieldDelimiter('"');
        try {
            config.validate();
            Assert.fail();
        } catch (IllegalStateException ex) {
            // should go here..
        }
        // fix field delimiter..
        config.setFieldDelimiter(',');
        
        // test blank unique attribute..
        try {
            config.setUniqueAttributeName("");
            config.validate();
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // should throw..
        }
    }
}
