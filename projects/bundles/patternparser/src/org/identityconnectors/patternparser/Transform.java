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

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Transforms may be applied to the result of a pattern match
 * <p>
 * Transforms have completely arbitrary input and output; they can simply modify
 * an input string, or change it into a different object, such as an array, a
 * Date, or a List.
 * <p>
 * Transforms may be applied to the output of other Transforms, creating a chain
 * of transforms.
 * <p>
 * Predefined transforms each have their own XML representation. If additional Transforms
 * are needed, it is possible to create new subclasses of Transform. If you do this, you must implement the methods:
 * <ul>
 * <li><b>transform</b> -- perform the transform.</li>
 * <li><b>getChildren</b> -- if your Transform has child Transforms, this must produce an XML string representing the children.</li>
 * <li><b>getAttributes</b> -- if your Transform has attributes, this must produce a string representing the attributes.</li>
 * 
 * </ul>
 * Represented in XML by
 * <pre>
 * &lt;Transform class='<b>class</b>' ...attributes...&gt;
 *   ...children...
 * &lt;/Transform&gt;
 * </pre>
 * where
 * <ul>
 * <li><b>class</b> -- the fully-qualified name of the Transform subclass
 * </ul>
 */

public abstract class Transform {
    protected static final String CLASS = "class";
    
    protected static Element getFirstChildElement(Element element) {
        NodeList nodes = element.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++)
            if (nodes.item(i) instanceof Element)
                return (Element)nodes.item(i);
        return null;
    }

    protected static String attributeToString(String name, String value) {
        return " " + name + "='" + value.replaceAll("'", "&apos;") + "'";
    }

    protected String getAttributes() {
        if (getClass().getPackage().equals(Transform.class.getPackage()))
            return "";
        else
            return attributeToString(CLASS, getClass().getName());
    }

    protected String getChildren(int indent) {
        return "";
    }

    public String toXml(int indent) {
        String children = getChildren(indent);
        StringBuffer pad = new StringBuffer();
        String className;
        if (getClass().getPackage().equals(Transform.class.getPackage()))
            className = getClass().getSimpleName();
        else
            className = "Transform";
        for (int i = 0; i < indent; i++)
            pad.append(" ");
        if (children.length() == 0)
            return pad + "<" + className + getAttributes() + "/>\n";
        else
            return pad + "<" + className + getAttributes() + ">\n" + children +
                   pad + "</" + className + ">\n";
    }

    /**
     * Transform an input object.
     * 
     * @param input -- the object to be transformed
     * @return the transformed object
     * @throws Exception
     */
    public abstract Object transform(Object input) throws Exception;

    /**
     * Factory method to create a Transform from an Element representing the transform.
     * 
     * @param element
     * @return
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static Transform newTransform(Element element) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, ClassNotFoundException {
        String className = element.getAttribute(CLASS);
        if (className.length()==0)
            className = Transform.class.getPackage().getName()+"."+element.getTagName();
        Class<? extends Transform> clazz = (Class<? extends Transform>) Class.forName(className);
        Constructor<? extends Transform> constructor = clazz.getConstructor(Element.class);
        return constructor.newInstance(element);
    }
    
    public static Transform newTransform(String string) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader(string)));
        NodeList elements = document.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
            if (elements.item(i) instanceof Element) {
                return Transform.newTransform((Element) elements.item(i));
            }
        return null;
    }
};
