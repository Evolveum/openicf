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
package org.identityconnectors.patternparser.test;

import org.identityconnectors.patternparser.Transform;
import org.w3c.dom.Element;


public class SubstringTransform extends Transform {
    private static final String START = "start";
    private static final String END = "end";
    
    private int start;
    private int end;
    
    public SubstringTransform(Element element) {
        this(Integer.parseInt(element.getAttribute(START)), Integer.parseInt(element.getAttribute(END)));
    }

    public SubstringTransform(int start, int length) {
        this.start = start;
        this.end = length;
    }

    @Override
    protected String getAttributes() {
        return super.getAttributes()
        +attributeToString(START, start+"")
        +attributeToString(END, end+"");
    }

    @Override
    public Object transform(Object input) throws Exception {
        return ((String)input).substring(start, end);
    }
    
}
