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
package org.identityconnectors.patternparser;

import java.util.regex.Pattern;

import org.w3c.dom.Element;

/**
 * A SubstituteTransform specifies a pair of strings to be used in a match/replace.
 * The equivalent of the perl:
 * <pre>
 * value ~= s/pattern/substitute/g;
 * </pre>
 * Represented in XML by
 * <pre>
 * &lt;SubstituteTransform pattern='<b>pattern</b> substitute='<b>substitute</b>'/&gt;
 * </pre>
 */
public class SubstituteTransform extends Transform {
    private static final String PATTERN    = "pattern";
    private static final String SUBSTITUTE = "substitute";
    private Pattern _pattern;
    private String _substitute;

    protected String getAttributes() {
        return super.getAttributes()+
            attributeToString(PATTERN, _pattern.pattern())+
            attributeToString(SUBSTITUTE, _substitute);
    }

    /**
     * Create a SubstituteTransform from an Element.
     * @param element
     */
    public SubstituteTransform(Element element) {
        this(element.getAttribute(PATTERN), element.getAttribute(SUBSTITUTE));
    }
    
    /**
     * Create a SubstituteTransform using the <b>pattern<b> and <b>replacement</b>.
     * 
     * @param pattern -- a pattern to be replaced.
     * @param substitute -- the replacement string.
     */
    public SubstituteTransform(String pattern, String substitute) {    
        _pattern = Pattern.compile(pattern);    
        _substitute = substitute;
    }

    /**
     * Convert <b>input</b> into a String, and replace all occurrences of the <b>pattern</b>
     * with <b>replacement</b>.
     * 
     * @param input -- the object to be transformed
     * @return a String with the substitutions made
     */
    public Object transform(Object input) throws Exception {
        return _pattern.matcher(input.toString()).replaceAll(_substitute);
    }
}
