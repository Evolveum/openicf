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
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.patternparser;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

/**
 * A ListTransform specifies a Transform to be applied to each element of a
 * List.
 * <p>
 * Represented in XML by
 *
 * <pre>
 * &lt;ListTransform&gt;
 *   another Transform
 * &lt;/ListTransform&gt;
 * </pre>
 */
public class ListTransform extends Transform<List<Object>> {
    private final Transform<?> transform;

    protected String getChildren(int indent) {
        return transform.toXml(indent + 2);
    }

    /**
     * Create a ListTransform from an Element.
     *
     * @param element
     */
    public ListTransform(Element element) throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        this(newTransform(getFirstChildElement(element)));
    }

    /**
     * Create a ListTransform from an Element.
     *
     * @param transform
     *            the Transform to be applied to each List element
     */
    public ListTransform(final Transform<?> transform) {
        this.transform = transform;
    }

    /**
     * Apply the <b>transform</b> to each element of <b>input</b>, which is
     * expected to be a List, and return the resulting List.
     *
     * @param input
     *            the List to be transformed
     * @return a List containing the transformed elements
     */
    @Override
    public List<Object> transform(Object input) throws Exception {
        final List<Object> result = new ArrayList<Object>();
        if (input instanceof List) {
            for (Object item : (List<?>) input) {
                result.add(transform.transform(item));
            }
        }
        return result;
    }
}
