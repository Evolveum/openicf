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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

/**
 * A GroupsTransform specifies a pattern to be used to break up the stringified value into a List.
 * <p>
 * Each element of the list corresponds to a group from the pattern.
 * <p>
 * Represented in XML by
 * <pre>
 * &lt;GroupsTransform mapPattern='<b>pattern</b>'/&gt;
 * </pre>
 */

public class GroupsTransform extends Transform {
    private static final String MAP_PATTERN = "mapPattern";
    private Pattern _mapPattern;
    
    protected String getAttributes() {
        return super.getAttributes()+attributeToString(MAP_PATTERN, _mapPattern.pattern());
    }

    /**
     * Create a GroupsTransform from an Element.
     * @param element
     */
    public GroupsTransform(Element element) {
        this(element.getAttribute(MAP_PATTERN));
    }
    
    /**
     * Create a GroupsTransform, given a <b>mapPattern</b>.
     * @param mapPattern -- the pattern to be used to split the input
     */
    public GroupsTransform(String mapPattern) {
        _mapPattern = Pattern.compile(mapPattern);
    }

    /**
     * Convert <b>input</b> into a String, and apply the <b>mapPattern</b>, returning all 
     * groups in the pattern as a List&lt;String&gt;
     * 
     * @param input -- the object to be transformed
     * @return a List&lt;String&gt; containing all the groups recognized in the <b>mapPattern</b>
     */
    public Object transform(Object input) throws Exception {
        Matcher matcher = _mapPattern.matcher(input.toString());
        if (matcher.matches()) {
            List<Object> output = new LinkedList<Object>();
            for (int i=1; i<=matcher.groupCount(); i++) {
                output.add(matcher.group(i));
            }
            return output;
        } else {
            return input;
        }
    }
}
