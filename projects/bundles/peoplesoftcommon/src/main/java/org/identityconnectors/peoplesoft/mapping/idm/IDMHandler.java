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

package org.identityconnectors.peoplesoft.mapping.idm;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

final class IDMHandler extends DefaultHandler{
    private Stack<State> stateStack = new Stack<State>();
    private Stack<Object> objectStack = new Stack<Object>();


    XmlObject run(InputStream stream) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(stream, this);
        XmlObject root = (XmlObject) objectStack.pop();
        return root;
    }
    
    XmlObject getRoot(){
        XmlObject root = (XmlObject) objectStack.peek();
        return root;
    }

    @Override
    public void startDocument() throws SAXException {
        stateStack.push(State.START);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (stateStack.peek()) {
        case START:
            if (qName.equals("Waveset")) {
                stateStack.push(State.WAVESET);
            } else {
                throw new SAXException();
            }
            break;
        case WAVESET:
            if (qName.equals("Configuration")) {
                stateStack.push(State.CONFIGURATION);
            } else {
                throw new SAXException();
            }
            break;
        case CONFIGURATION:
            if (qName.equals("Extension")) {
                stateStack.push(State.EXTENSION);
            } else {
                throw new SAXException();
            }
            break;
        case EXTENSION:
            if (qName.equals("Object")) {
                stateStack.push(State.OBJECT);
                objectStack.push(newXmlObject(attributes));
            } else {
                throw new SAXException();
            }
            break;
        case OBJECT:
            if (qName.equals("Attribute")) {
                stateStack.push(State.ATTRIBUTE);
                XmlAttribute attribute = newXmlAttribute(attributes);
                ((XmlObject) objectStack.peek()).attributes.put(attribute.name, attribute);
                objectStack.push(attribute);
            } else {
                throw new SAXException();
            }
            break;
        case ATTRIBUTE:
            if (qName.equals("Object")) {
                stateStack.push(State.OBJECT);
                XmlObject object = newXmlObject(attributes);
                ((XmlAttribute) objectStack.peek()).value = object;
                objectStack.push(object);
            } else if (qName.equals("List")) {
                stateStack.push(State.LIST);
                XmlList list = new XmlList();
                ((XmlAttribute) objectStack.peek()).value = list;
                objectStack.push(list);
            } else {
                throw new SAXException();
            }
            break;
        case LIST:
            if (qName.equals("Object")) {
                stateStack.push(State.OBJECT);
                XmlObject object = newXmlObject(attributes);
                ((XmlList) objectStack.peek()).objects.add(object);
                objectStack.push(object);
            } else {
                throw new SAXException();
            }
            break;
        default:
            throw new AssertionError();
        }
    }

    private XmlObject newXmlObject(Attributes attributes) {
        XmlObject result = new XmlObject();
        result.name = attributes.getValue("name");
        return result;
    }

    private XmlAttribute newXmlAttribute(Attributes attributes) {
        XmlAttribute result = new XmlAttribute();
        result.name = attributes.getValue("name");
        result.value = attributes.getValue("value");
        return result;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        stateStack.pop();
        if (objectStack.size() > 1) {
            objectStack.pop();
        }
    }

    static final class XmlObject {
        private String name;
        private Map<String, XmlAttribute> attributes = new LinkedHashMap<String, XmlAttribute>();
        String getName(){
            return name;
        }
        Collection<String> getAttributeNames(){
            return Collections.unmodifiableCollection(attributes.keySet());
        }
        XmlAttribute getAttribute(String name){
            return attributes.get(name);
        }
        Collection<XmlAttribute> getAttributes(){
            return Collections.unmodifiableCollection(attributes.values());
        }
        Collection<Map.Entry<String, XmlAttribute>> getEntries(){
            return Collections.unmodifiableCollection(attributes.entrySet());
        }
        int getAttributesCount(){
            return attributes.size();
        }
    }

    static final class XmlAttribute {
        private String name;
        private Object value;
        String getName() {
            return name;
        }
        Object getValue() {
            return value;
        }
    }

    static final class XmlList {
        private List<XmlObject> objects = new ArrayList<XmlObject>();
        List<XmlObject> getObjects(){
            return Collections.unmodifiableList(objects);
        }
    }

    private enum State {
        START,
        WAVESET,
        CONFIGURATION,
        EXTENSION,
        OBJECT,
        ATTRIBUTE,
        LIST;
    } 
}
