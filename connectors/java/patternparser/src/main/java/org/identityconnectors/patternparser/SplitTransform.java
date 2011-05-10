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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

/**
 * A SplitTransform specifies a pattern to be used to split the value into a List&lt;String&gt;.
 * <p>
 * Represented in XML by
 * <pre>
 * &lt;SplitTransform splitPattern='&lt;pattern&gt;'/&gt;
 * </pre>
 */
public class SplitTransform extends Transform {
    private static final String SPLIT_PATTERN = "splitPattern";
    private Pattern _splitPattern;
    
    protected String getAttributes() {
        return super.getAttributes()+attributeToString(SPLIT_PATTERN, _splitPattern.pattern());
    }

    /**
     * Create a SplitTransform from an Element.
     * @param element
     */
    public SplitTransform(Element element) {
        this(element.getAttribute(SPLIT_PATTERN));
    }
    
    /**
     * Create a SplitTransform using the <b>splitPattern</b>.
     * 
     * @param splitPattern -- a pattern to be used to split strings into subStrings.
     */
    public SplitTransform(String splitPattern) {
        _splitPattern = Pattern.compile(splitPattern);
    }

    /**
     * Convert <b>input</b> into a String, and split the string into separate strings, based
     * on the <b>splitPattern</b> specified in the constructor.
     * 
     * @param input -- the object to be transformed
     * @return a List&lt;String&gt; containing the substrings split from the input
     */
    public Object transform(Object input) throws Exception {
        List<String> list = new ArrayList<String>();
        String inputString = input.toString();
        if (inputString.length()==0)
            return list;
        String[] split = _splitPattern.split(inputString);
        for (String string : split)
            list.add(string);
        return list;
    }
}
