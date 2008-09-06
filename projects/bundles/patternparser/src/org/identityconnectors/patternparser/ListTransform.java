/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.patternparser;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

/**
 * A ListTransform specifies a Transform to be applied to each element of a List.
 * <p>
 * Represented in XML by
 * <pre>
 * &lt;ListTransform&gt;
 *   another Transform
 * &lt;/ListTransform&gt;
 * </pre>
 */
public class ListTransform extends Transform {
    private Transform _transform;
    
    protected String getChildren(int indent) {
        return _transform.toXml(indent+2);
    }

    /**
     * Create a ListTransform from an Element.
     * @param element
     */
    public ListTransform(Element element) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this(newTransform(getFirstChildElement(element)));
    }
    
    /**
     * Create a ListTransform from an Element.
     * @param transform -- the Transform to be applied to each List element
     */
    public ListTransform(Transform transform) {
        _transform = transform;
    }

    /**
     * Apply the <b>transform</b> to each element of <b>input</b>, which is expected to be a List, 
     * and return the resulting List.
     * 
     * @param input -- the List to be transformed
     * @return a List containing the transformed elements
     */
    public Object transform(Object input) throws Exception {
        List<Object> result = new ArrayList<Object>();
        if (input instanceof List) {
            for (Object item : (List<?>)input) {
                result.add(_transform.transform(item));
            }
        }
        return result;
    }
}
