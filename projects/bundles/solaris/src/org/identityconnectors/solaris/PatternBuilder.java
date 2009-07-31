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
package org.identityconnectors.solaris;

/**
 * This class builds regular expression patterns for matching one or more 
 * columns in a delimited data string. 
 * 
 * For example we have a line with data:
 * "john:smith:123456789:john.smith@foo.com"
 * So the patt
 * @author David Adam
 */
public class PatternBuilder {
    
    private static final String DEFAULT_DELIMITER = ":";
    private int nrOfColumns;

    private String delimiter;

    public PatternBuilder(int nrOfColumns) {
        this(nrOfColumns, DEFAULT_DELIMITER);
    }
    
    private PatternBuilder(int nrOfColumns, String delimiter) {
        this.nrOfColumns = nrOfColumns; 
        this.delimiter = delimiter;
    }
    
    public String build(int... columnSelector) {
        checkValidity(columnSelector);
        
        StringBuffer sb = new StringBuffer();
        
        String token = String.format("[^%s]*", delimiter, delimiter);
        String groupToken = String.format("(%s)", token);
        
        for (int i = 1; i <= nrOfColumns; i++) {
            boolean found = false;
            for (int currSelector : columnSelector) {
                if (i == currSelector) {
                    sb.append(groupToken);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                sb.append(token);
            }
            if (i != nrOfColumns)
                sb.append(delimiter);
        }
        
        return sb.toString();
    }
    
    private void checkValidity(int[] columnSelector) {
        for (int i : columnSelector) {
            if (i > nrOfColumns || i <= 0) {
                throw new IllegalArgumentException(
                        "ColumnSelector should be between 1 nad number of columns ("
                                + nrOfColumns + ". But actual value is: " + i);
            }
        }
    }

    public static String buildPattern(int nrOfColumns, int... columnSelector) {
        PatternBuilder pb = new PatternBuilder(nrOfColumns);
        return pb.build(columnSelector);
    }
    
    public int getNrOfColumns() {
        return nrOfColumns;
    }
}
