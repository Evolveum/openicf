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
package org.identityconnectors.flatfile;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import org.testng.Assert;

import java.io.File;
import java.nio.charset.Charset;

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
        Assert.assertEquals(null, config.getFile());
        Assert.assertEquals(Charset.defaultCharset().name(), config.getEncoding());
        Assert.assertEquals('"', config.getTextQualifier());
        Assert.assertEquals(',', config.getFieldDelimiter());
        // set a unique attribute so there's not a runtime exception..
        config.setUniqueAttributeName("uid");
        
        // test the file..
        File f = new File("test/test.csv");
        // simple property test..
        config.setFile(f);
        Assert.assertEquals(f, config.getFile());
        // try the validate..
        try {
            config.validate();
            AssertJUnit.fail();
        } catch (RuntimeException e) {
            // expected because the file doesn't exist..
        }
        // create a temp file
        f = File.createTempFile("connector", "suffix");
        f.deleteOnExit();
        // attempt validate again..
        config.setFile(f);
        Assert.assertEquals(f, config.getFile());
        // this should work..
        config.validate();

        // test the encoding property..
        config.setEncoding(null);
        try {
            config.validate();
            AssertJUnit.fail();
        } catch (IllegalArgumentException ex) {
            // should go here..
        }
        // check encoding..
        config.setEncoding(Charset.forName("UTF-8").name());
        Assert.assertEquals(Charset.forName("UTF-8").name(), config.getEncoding());

        // test stupid problem..
        config.setFieldDelimiter('"');
        try {
            config.validate();
            AssertJUnit.fail();
        } catch (IllegalStateException ex) {
            // should go here..
        }
        // fix field delimiter..
        config.setFieldDelimiter(',');
        
        // test blank unique attribute..
        try {
            config.setUniqueAttributeName("");
            config.validate();
            AssertJUnit.fail();
        } catch (IllegalArgumentException ex) {
            // should throw..
        }
    }
}
