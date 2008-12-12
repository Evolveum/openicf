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

import org.w3c.dom.Element;

/**
 * A BooleanTransform converts the value to a Boolean, if possible.
 * <p>
 * Represented in XML by
 * <pre>
 * &lt;BooleanTransform/&gt;
 * </pre>
 */
public class BooleanTransform extends Transform {
    /**
     * Create a BooleanTransform from an Element.
     * @param element
     */
    public BooleanTransform(Element element) {
        this();
    }
    /**
     * Create a BooleanTransform.
     */
    public BooleanTransform() {
    }
    
    /**
     * Convert <b>input</b> into a String, and if the String equals (ignoring case) "true" return a Boolean.TRUE.
     * If the String equals (ignoring case) "false" return a Boolean.FALSE.
     * Otherwise, just return the input object
     * 
     * @param input -- the object to be transformed
     * @return a Boolean, or the input object
     */
    @Override
    public Object transform(Object input) throws Exception {
        String value = input.toString().trim();
        if ("true".equalsIgnoreCase(value))
            return Boolean.TRUE;
        else if ("false".equalsIgnoreCase(value))
            return Boolean.FALSE;
        else 
            return input;
    }
};
