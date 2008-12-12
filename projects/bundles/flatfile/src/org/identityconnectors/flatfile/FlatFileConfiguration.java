/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.spi.AbstractConfiguration;


/**
 * Configuration information required for the Connector to attach to a file.
 */
public class FlatFileConfiguration extends AbstractConfiguration {

    /**
     * Location to the CSV file to parse.
     */
    private File file;

    /**
     * Basic encoding of the file the default valid in the default character set
     * of the OS.
     */
    private String encoding = Charset.defaultCharset().name();

    /**
     * Delimiter to determine begin and end of text in value.
     */
    private char textQualifier = '"';

    /**
     * Delimiter for the separate each file in a row.
     */
    private char fieldDelimeter = ',';

    /**
     * Particular attribute the represent the unique value of each account.
     */
    private String uniqueAttribute;
    

    // =======================================================================
    // File Property
    // =======================================================================
    /**
     * File to parse from the file system. An exception will throw in the
     * {@link #validate()} method if the file does not exist.
     * 
     * @param value
     *            Valid CSV file that exists.
     */
    public void setFile(File value) {
        this.file = value;
    }

    /**
     * Get the currently set file.
     * 
     * @return the reference object to the CSV file.
     */
    public File getFile() {
        return this.file;
    }

    // =======================================================================
    // Encoding Property
    // =======================================================================
    /**
     * Set the encoding to use when reading the CSV file. The default value is
     * the character set of the OS.
     * 
     * @param value
     *            encoding to reading the file in (recommended to set to UTF-8).
     */
    public void setEncoding(String value) {
        this.encoding = value;
    }

    public String getEncoding() {
        return this.encoding;
    }

    // =======================================================================
    // Text Qualifier Property
    // =======================================================================
    public void setTextQualifier(char value) {
        this.textQualifier = value;
    }

    public char getTextQualifier() {
        return this.textQualifier;
    }

    // =======================================================================
    // Field Delimiter Property
    // =======================================================================
    public void setFieldDelimiter(char value) {
        this.fieldDelimeter = value;
    }

    public char getFieldDelimiter() {
        return this.fieldDelimeter;
    }

    // =======================================================================
    // Unique Attribute Property
    // =======================================================================
    public void setUniqueAttributeName(String value) {
        this.uniqueAttribute = value;
    }

    public String getUniqueAttributeName() {
        return this.uniqueAttribute;
    }

    /**
     * Determine if all the values are valid.
     * 
     * @throws IllegalArgumentException
     *             iff the unique identifier attribute is blank or encoding is
     *             set to null
     * @throws IllegalStateException
     *             iff the text qualifier and field delimiter are the same.
     * @throws RuntimeException
     *             iff the file is not found.
     * @throws IllegalCharsetNameException
     *             iff the character set name is invalid
     * @see org.identityconnectors.framework.Configuration#validate()
     */
    public void validate() {
        // make sure the encoding is set..
        if (this.encoding == null) {
            final String msg = "File encoding must not be null!";
            throw new IllegalArgumentException(msg);
        }
        //make sure it's a valid charset
        Charset.forName(this.encoding);
        // make sure the delimiter and the text qualifier are not the same..
        if (this.textQualifier == this.fieldDelimeter) {
            final String msg = "Field delimiter and text qualifier can not be equal!";
            throw new IllegalStateException(msg);
        }
        // make sure the unique identifier is set..
        if (StringUtil.isBlank(this.uniqueAttribute)) {
            final String msg = "Unique identifier must not be blank!";
            throw new IllegalArgumentException(msg);
        }
        // check to make sure the file is valid..
        if (!getFile().exists()) {
            final String msg = "File not found: " + getFile().toString();
            throw new IllegalArgumentException(new FileNotFoundException(msg));
        }
        if (!getFile().canRead()) {
            throw new IllegalArgumentException("File not accessible!");
        }
    }

    // =======================================================================
    // Helper methods (must be package protected)
    // =======================================================================
    /**
     * Creates a new buffered reader based on the parameters provided in the
     * config.
     */
    BufferedReader newFileReader() throws IOException {
        BufferedReader rdr = null;
        FileInputStream fis = new FileInputStream(getFile());
        InputStreamReader ins = new InputStreamReader(fis, encoding);
        rdr = new BufferedReader(ins);
        return rdr;
    }

}
