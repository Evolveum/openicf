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
package org.identityconnectors.patternparser;

import java.util.regex.Pattern;

import org.w3c.dom.Element;

/**
 * A NullTransform specifies a string to be used in a match.
 * If the input matches the pattern, a null is returned, otherwise
 * the input is returned.
 */
public class NullTransform extends Transform {
    private static final String PATTERN    = "pattern";
    private Pattern _pattern;

    protected String getAttributes() {
        return super.getAttributes()+
            attributeToString(PATTERN, _pattern.pattern());
    }

    /**
     * Create a NullTransform from an Element.
     * @param element
     */
    public NullTransform(Element element) {
        this(element.getAttribute(PATTERN));
    }
    
    /**
     * Create a NullTransform using the <b>pattern<b> .
     * 
     * @param pattern -- a pattern to be replaced.
     */
    public NullTransform(String pattern) {    
        _pattern = Pattern.compile(pattern);
    }

    /**
     * Convert <b>input</b> into a String, and match it against <b>pattern</b>,
     * returning a null if the match is successful.
     * 
     * @param input -- the object to be transformed
     * @return null, or the input
     */
    public Object transform(Object input) throws Exception {
        if (_pattern.matcher(input.toString()).matches()) 
            return null;
        else
            return input;
    }
}
