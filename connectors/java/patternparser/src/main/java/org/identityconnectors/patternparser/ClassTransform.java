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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * A ClassTransform specifies a Class with a constructor taking a single argument
 * of the type to which the transform is applied (determined at run-time).
 * <p>
 * Represented in XML by
 * <pre>
 * &lt;ClassTransform transform='<b>className</b>'/&gt;
 * </pre>
 */

public class ClassTransform extends Transform {
    private static final String TRANSFORM = "transform";
    private Class<? extends Object> _class;
    private transient Map<Class<? extends Object>, Constructor<?>> _map;

    protected String getAttributes() {
        return super.getAttributes()+attributeToString(TRANSFORM, _class.getName());
    }

    /**
     * Create a ClassTransform from an Element.
     * @param element
     */
    public ClassTransform(Element element) throws ClassNotFoundException {
        this(Class.forName(element.getAttribute(TRANSFORM)));
    }
    
    /**
     * Create a ClassTransform from an Element.
     * @param clazz -- the Class to instantiate as part of the transform
     */
    public ClassTransform(Class<? extends Object> clazz) {
        _map = new HashMap<Class<? extends Object>, Constructor<?>>();
        _class = clazz;
    }

    /**
     * Create a new object by executing
     * <pre><b>transform</b>.getConstructor(<b>input</b>.getClass()).newInstance(<b>input</b>)</pre>
     * 
     * @param input -- the object to be transformed
     * @return an instance of Class <b>transform</b>
     */
    public Object transform(Object input) throws Exception {
        Class<? extends Object> clazz = input.getClass();
        if (!_map.containsKey(clazz)) {
            _map.put(clazz, _class.getConstructor(clazz));
        }
        return _map.get(clazz).newInstance(input);
    }
}
